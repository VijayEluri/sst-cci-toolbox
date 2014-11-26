package org.esa.cci.sst.tools;

import org.esa.cci.sst.data.*;
import org.esa.cci.sst.orm.PersistenceManager;
import org.esa.cci.sst.orm.Storage;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.matchup.MatchupIO;
import org.esa.cci.sst.tools.mmdgeneration.DimensionConfigurationInitializer;
import org.esa.cci.sst.tools.samplepoint.DirtySubsceneRemover;
import org.esa.cci.sst.tools.samplepoint.OverlapRemover;
import org.esa.cci.sst.tools.samplepoint.SamplePointImporter;
import org.esa.cci.sst.tools.samplepoint.TimeRange;
import org.esa.cci.sst.util.*;
import org.postgis.PGgeometry;

import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatchupGenerator extends BasicTool {

    private static final DecimalFormat monthFormat = new DecimalFormat("00");
    private String sensorName1;
    private String sensorName2;
    private int subSceneWidth1;
    private int subSceneWidth2;
    private int subSceneHeight1;
    private int subSceneHeight2;
    private double dirtyPixelFraction;
    private boolean overlappingWanted;
    private long referenceSensorPattern;
    private String referenceSensorName;
    private String archiveRootPath;

    public MatchupGenerator() {
        super("matchup-generator", "1.0");
    }

    public static void main(String[] args) {
        final MatchupGenerator tool = new MatchupGenerator();
        try {
            final boolean ok = tool.setCommandLineArgs(args);
            if (!ok) {
                tool.printHelp();
                return;
            }
            tool.initialize();
            tool.run();
        } catch (ToolException e) {
            tool.getErrorHandler().terminate(e);
        } catch (Exception e) {
            tool.getErrorHandler().terminate(new ToolException(e.getMessage(), e, ToolException.UNKNOWN_ERROR));
        } finally {
            tool.getPersistenceManager().close();
        }
    }

    @Override
    public void initialize() {
        super.initialize();

        final Configuration config = getConfig();

        final String[] sensorNames = config.getStringValue(Configuration.KEY_MMS_SAMPLING_SENSOR).split(",", 2);
        sensorName1 = sensorNames[0];
        if (sensorNames.length > 1) {
            sensorName2 = sensorNames[1];
        }
        dirtyPixelFraction = config.getDoubleValue(Configuration.KEY_MMS_SAMPLING_DIRTY_PIXEL_FRACTION, 0.0);
        overlappingWanted = config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_OVERLAPPING_WANTED, false);
        referenceSensorName = config.getStringValue(Configuration.KEY_MMS_SAMPLING_REFERENCE_SENSOR);
        referenceSensorPattern = config.getPattern(referenceSensorName, 0);

        final Set<String> dimensionNames = new TreeSet<>();
        dimensionNames.add(SensorNames.getDimensionNameX(sensorName1));
        dimensionNames.add(SensorNames.getDimensionNameY(sensorName1));
        if (sensorName2 != null) {
            dimensionNames.add(SensorNames.getDimensionNameX(sensorName2));
            dimensionNames.add(SensorNames.getDimensionNameY(sensorName2));
        }
        final Map<String, Integer> map = DimensionConfigurationInitializer.initialize(dimensionNames, getConfig());
        subSceneWidth1 = map.get(SensorNames.getDimensionNameX(sensorName1));
        subSceneHeight1 = map.get(SensorNames.getDimensionNameY(sensorName1));
        if (sensorName2 != null) {
            subSceneWidth2 = map.get(SensorNames.getDimensionNameX(sensorName2));
            subSceneHeight2 = map.get(SensorNames.getDimensionNameY(sensorName2));
        }

        archiveRootPath = config.getStringValue(Configuration.KEY_MMS_ARCHIVE_ROOT);

    }

    private void run() throws IOException {
        cleanupIfRequested();

        final List<SamplingPoint> samples = loadSamplePoints(logger);
        removeDirtySamples(logger, samples, true);

        if (sensorName2 != null) {
            removeDirtySamples(logger, samples, false);
        }

        removeOverlappingSamples(logger, samples);
        createMatchups(logger, samples);
    }

    void createMatchups(List<SamplingPoint> samples, String referenceSensorName, String primarySensorName,
                        String secondarySensorName, long referenceSensorPattern, PersistenceManager pm,
                        Storage storage, Logger logger) {
        if (!samples.isEmpty()) {
            try {
                boolean hasInsitu = false;

                // create reference observations
                logInfo(logger, "Starting creating reference observations...");
                pm.transaction();
                final String sensorShortName = createSensorShortName(referenceSensorName, primarySensorName);
                final List<ReferenceObservation> referenceObservations = createReferenceObservations(samples,
                        sensorShortName,
                        storage);
                pm.commit();
                logInfo(logger, "Finished creating reference observations");

                logInfo(logger, "Starting creating matchup pattern ...");
                final long matchupPattern = defineMatchupPattern(primarySensorName,
                        secondarySensorName,
                        referenceSensorPattern,
                        pm);
                logInfo(logger, MessageFormat.format("Matchup pattern: {0}", Long.toHexString(matchupPattern)));

                // create matchups and coincidences
                logInfo(logger, "Starting creating matchups and coincidences...");
                pm.transaction();

                final List<Matchup> matchups = new ArrayList<>(referenceObservations.size());
                final List<Coincidence> coincidences = new ArrayList<>(samples.size());
                final List<InsituObservation> insituObservations = new ArrayList<>(samples.size());
                for (int i = 0; i < samples.size(); i++) {
                    final SamplingPoint p = samples.get(i);
                    final ReferenceObservation r = referenceObservations.get(i);

                    final Matchup matchup = createMatchup(matchupPattern, r);
                    matchups.add(matchup);

                    final RelatedObservation o1 = storage.getRelatedObservation(p.getReference());

                    final Coincidence coincidence = createPrimaryCoincidence(matchup, o1);
                    coincidences.add(coincidence);

                    if (secondarySensorName != null) {
                        final RelatedObservation o2 = storage.getRelatedObservation(p.getReference2());
                        final Coincidence secondCoincidence = createSecondaryCoincidence(p, matchup, o2);
                        coincidences.add(secondCoincidence);
                    }
                    if (p.getInsituReference() != 0) {
                        final int datafileId = p.getInsituReference();
                        final DataFile insituDatafile = storage.getDatafile(datafileId);
                        final InsituObservation insituObservation = createInsituObservation(p, insituDatafile);
                        insituObservations.add(insituObservation);

                        final Coincidence insituCoincidence = createCoincidence(matchup, insituObservation);
                        insituCoincidence.setTimeDifference(Math.abs(p.getReferenceTime() - p.getTime()) / 1000.0);

                        coincidences.add(insituCoincidence);
                        hasInsitu = true;
                    }
                }
                pm.commit();

                logInfo(logger, "Finished creating matchups and coincidences");

                // persist matchups and coincidences
                logInfo(logger, "Starting writing matchups and coincidences...");

                final String[] sensorNamesArray = createSensorNamesArray(hasInsitu);
                final Month centerMonth = ConfigUtil.getCenterMonth(Configuration.KEY_MMS_SAMPLING_START_TIME,
                        Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                        getConfig());

                final String outputFilePath = createOutputFilePath(archiveRootPath, sensorNamesArray, centerMonth.getYear(), centerMonth.getMonth());
                final File outFile = new File(outputFilePath);
                outFile.createNewFile();    // @todo 2 tb/tb test and eventually create helper method 2014-11-26
                MatchupIO.write(matchups, new FileOutputStream(outFile), getConfig());

                logInfo(logger, "Finished writing matchups and coincidences...");
            } catch (Exception e) {
                throw new ToolException(e.getMessage(), e, ToolException.TOOL_ERROR);
            }
        }
    }

    // package access for testing only tb 2014-04-03
    static Coincidence createSecondaryCoincidence(SamplingPoint p, Matchup matchup, RelatedObservation o2) {
        final Coincidence coincidence = createCoincidence(matchup, o2);
        final Date matchupTime = matchup.getRefObs().getTime();
        final Date relatedTime = new Date(p.getReference2Time());
        coincidence.setTimeDifference(TimeUtil.getTimeDifferenceInSeconds(matchupTime, relatedTime));
        return coincidence;
    }

    // package access for testing only tb 2014-04-03
    static Coincidence createPrimaryCoincidence(Matchup matchup, RelatedObservation o1) {
        final Coincidence coincidence = createCoincidence(matchup, o1);
        coincidence.setTimeDifference(0.0);
        return coincidence;
    }

    // package access for testing only tb 2014-04-03
    static Matchup createMatchup(long matchupPattern, ReferenceObservation r) {
        final Matchup matchup = new Matchup();
        matchup.setId(r.getId());
        matchup.setRefObs(r);
        matchup.setPattern(matchupPattern);
        return matchup;
    }

    // package access for testing only tb 2014-04-03
    static long defineMatchupPattern(String primarySensorName, String secondarySensorName, long referenceSensorPattern,
                                     PersistenceManager pm) {
        long matchupPattern;
        final Storage storage = pm.getStorage();
        pm.transaction();

        final String primaryOrbitName = SensorNames.getOrbitName(primarySensorName);
        final Sensor primarySensor = storage.getSensor(primaryOrbitName);
        if (secondarySensorName != null) {
            final String secondaryOrbitName = SensorNames.getOrbitName(secondarySensorName);
            final Sensor secondarySensor = storage.getSensor(secondaryOrbitName);
            matchupPattern = referenceSensorPattern | primarySensor.getPattern() | secondarySensor.getPattern();
        } else {
            matchupPattern = referenceSensorPattern | primarySensor.getPattern();
        }
        pm.commit();
        return matchupPattern;
    }

    // package access for testing only tb 2014-04-03
    static InsituObservation createInsituObservation(SamplingPoint p, DataFile insituDatafile) {
        final InsituObservation insituObservation = new InsituObservation();
        insituObservation.setName(p.getDatasetName());
        insituObservation.setDatafile(insituDatafile);
        insituObservation.setRecordNo(p.getIndex());
        insituObservation.setSensor(Constants.SENSOR_NAME_HISTORY);
        final PGgeometry insituLocation = GeometryUtil.createPointGeometry(p.getLon(), p.getLat());
        insituObservation.setLocation(insituLocation);
        insituObservation.setTime(new Date(p.getTime()));
        insituObservation.setTimeRadius(Math.abs(p.getReferenceTime() - p.getTime()) / 1000.0);
        return insituObservation;
    }

    static String createSensorShortName(String referenceSensorName, String primarySensorName) {
        return referenceSensorName.substring(0, 3) + "_" + SensorNames.getStandardName(primarySensorName);
    }

    // package access for testing only tb 2014-03-19
    static ReferenceObservation createReferenceObservation(String referenceSensorName, SamplingPoint samplingPoint,
                                                           DataFile datafile) {
        final ReferenceObservation r = new ReferenceObservation();
        r.setName(samplingPoint.getDatasetName());
        r.setSensor(referenceSensorName);

        final PGgeometry location = GeometryUtil.createPointGeometry(samplingPoint.getReferenceLon(),
                samplingPoint.getReferenceLat());
        r.setLocation(location);
        r.setPoint(location);

        final Date time = new Date(samplingPoint.getReferenceTime());
        r.setTime(time);
        if (samplingPoint.isInsitu()) {
            r.setTimeRadius(Math.abs(samplingPoint.getReferenceTime() - samplingPoint.getTime()) / 1000.0);
        } else {
            r.setTimeRadius(0.0);
        }

        r.setDatafile(datafile);
        r.setRecordNo(0);
        r.setDataset(samplingPoint.getInsituDatasetId().getValue());
        r.setReferenceFlag(Constants.MATCHUP_REFERENCE_FLAG_UNDEFINED);
        return r;
    }

    static String createOutputFilePath(String archiveRoot, String[] sensorNames, int year, int month) {
        final StringBuilder stringBuilder = new StringBuilder(256);
        stringBuilder.append(archiveRoot);
        stringBuilder.append("/clean/");

        final StringBuilder sensorTagBuilder = new StringBuilder(64);
        sensorTagBuilder.append(sensorNames[0]);
        for (int i = 1; i < sensorNames.length; i++) {
            sensorTagBuilder.append(',');
            sensorTagBuilder.append(sensorNames[i]);
        }
        final String sensorTag = sensorTagBuilder.toString();

        stringBuilder.append(sensorTag);
        stringBuilder.append('/');
        stringBuilder.append(year);
        stringBuilder.append('/');
        stringBuilder.append(sensorTag);
        stringBuilder.append("-clean-");
        stringBuilder.append(year);
        stringBuilder.append('-');
        stringBuilder.append(monthFormat.format(month));
        stringBuilder.append(".json");
        return stringBuilder.toString();
    }

    private String[] createSensorNamesArray(boolean hasInsitu) {
        final ArrayList<String> sensorNamesList = new ArrayList<>();
        sensorNamesList.add(sensorName1);
        if (sensorName2 != null) {
            sensorNamesList.add(sensorName2);
        }
        if (hasInsitu) {
            sensorNamesList.add("history");
        }

        return sensorNamesList.toArray(new String[sensorNamesList.size()]);
    }

    private static Coincidence createCoincidence(Matchup matchup, RelatedObservation o2) {
        final Coincidence coincidence = new Coincidence();
        coincidence.setMatchup(matchup);
        coincidence.setObservation(o2);
        return coincidence;
    }

    private static void logInfo(Logger logger, String message) {
        if (logger != null && logger.isLoggable(Level.INFO)) {
            logger.info(message);
        }
    }

    private static List<ReferenceObservation> createReferenceObservations(List<SamplingPoint> samples,
                                                                          String referenceSensorName,
                                                                          Storage storage) {
        final List<ReferenceObservation> referenceObservations = new ArrayList<>(samples.size());
        for (final SamplingPoint samplingPoint : samples) {
            final Observation o = storage.getObservation(samplingPoint.getReference());
            final DataFile datafile = o.getDatafile();

            final ReferenceObservation r = createReferenceObservation(referenceSensorName, samplingPoint, datafile);

            referenceObservations.add(r);
        }
        return referenceObservations;
    }

    private OverlapRemover createOverlapRemover() {
        if (overlappingWanted) {
            return new OverlapRemover(1, 1); // only remove samples, which belong to the same pixel
        }
        return new OverlapRemover(subSceneWidth1, subSceneHeight1);
    }

    private void cleanupIfRequested() {
        final Configuration config = getConfig();
        if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP)) {
            cleanup();
        } else if (config.getBooleanValue(Configuration.KEY_MMS_SAMPLING_CLEANUP_INTERVAL)) {
            cleanupInterval();
        }
    }


    private void cleanup() {
        getPersistenceManager().transaction();

        Query delete = getPersistenceManager().createQuery("delete from Coincidence c");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery("delete from Matchup m");
        delete.executeUpdate();
        delete = getPersistenceManager().createQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from Observation o where o.sensor = '" + Constants.SENSOR_NAME_DUMMY + "'");
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    private void cleanupInterval() {
        getPersistenceManager().transaction();

        final TimeRange timeRange = ConfigUtil.getTimeRange(Configuration.KEY_MMS_SAMPLING_START_TIME,
                Configuration.KEY_MMS_SAMPLING_STOP_TIME,
                getConfig());
        final Date startDate = timeRange.getStartDate();
        final Date stopDate = timeRange.getStopDate();
        Query delete = getPersistenceManager().createNativeQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from mm_coincidence c where exists ( select r.id from mm_observation r where c.matchup_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_DUMMY + "')");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from mm_matchup m where exists ( select r from mm_observation r where m.refobs_id = r.id and r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_DUMMY + "')");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();
        delete = getPersistenceManager().createNativeQuery(
                // TODO - check this, because sensor name for reference observations is built according to the pattern referenceSensorName.substring(0, 3) + "_" + SensorNames.ensureStandardName(primarySensorName),
                "delete from mm_observation r where r.time >= ?1 and r.time < ?2 and r.sensor = '" + Constants.SENSOR_NAME_DUMMY + "'");
        delete.setParameter(1, startDate);
        delete.setParameter(2, stopDate);
        delete.executeUpdate();

        getPersistenceManager().commit();
    }

    private void createMatchups(Logger logger, List<SamplingPoint> samples) {
        logInfo(logger, "Starting creating matchups...");
        createMatchups(samples, referenceSensorName, sensorName1, sensorName2, referenceSensorPattern,
                getPersistenceManager(), getStorage(), logger);
        logInfo(logger, "Finished creating matchups...");
    }

    private void removeOverlappingSamples(Logger logger, List<SamplingPoint> samples) {
        logInfo(logger, "Starting removing overlapping samples...");
        final OverlapRemover overlapRemover = createOverlapRemover();
        overlapRemover.removeSamples(samples);
        // TODO - remove duplicated samples (i.e. samples that have the same in-situ measurement and same satellite coordinates)
        logInfo(logger, "Finished removing overlapping samples (" + samples.size() + " samples left)");
    }

    private void removeDirtySamples(Logger logger, List<SamplingPoint> samples, boolean primary) {
        final DirtySubsceneRemover subsceneRemover = new DirtySubsceneRemover();
        if (primary) {
            subsceneRemover.primary(true)
                    .subSceneWidth(subSceneWidth1)
                    .subSceneHeight(subSceneHeight1)
                    .dirtyPixelFraction(dirtyPixelFraction)
                    .config(getConfig())
                    .storage(getStorage())
                    .logger(logger)
                    .removeSamples(samples);
        } else {
            subsceneRemover.primary(false)
                    .subSceneWidth(subSceneWidth2)
                    .subSceneHeight(subSceneHeight2)
                    .dirtyPixelFraction(dirtyPixelFraction)
                    .config(getConfig())
                    .storage(getStorage())
                    .logger(logger)
                    .removeSamples(samples);
        }
    }

    private List<SamplingPoint> loadSamplePoints(Logger logger) throws IOException {
        logInfo(logger, "Starting loading samples...");
        final SamplePointImporter samplePointImporter = new SamplePointImporter(getConfig());
        samplePointImporter.setLogger(logger);
        final List<SamplingPoint> samples = samplePointImporter.load();
        logInfo(logger, "Finished loading samples: (" + samples.size() + " loaded).");
        return samples;
    }
}
