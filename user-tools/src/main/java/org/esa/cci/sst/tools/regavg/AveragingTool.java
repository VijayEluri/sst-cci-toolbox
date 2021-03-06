/*
 * SST_cci Tools
 *
 * Copyright (C) 2011-2013 by Brockmann Consult GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.esa.cci.sst.tools.regavg;

import org.esa.cci.sst.auxiliary.Climatology;
import org.esa.cci.sst.common.ProcessingLevel;
import org.esa.cci.sst.common.SstDepth;
import org.esa.cci.sst.common.TemporalResolution;
import org.esa.cci.sst.file.FileStore;
import org.esa.cci.sst.grid.RegionMask;
import org.esa.cci.sst.grid.RegionMaskList;
import org.esa.cci.sst.log.SstLogging;
import org.esa.cci.sst.product.ProductType;
import org.esa.cci.sst.tool.Configuration;
import org.esa.cci.sst.tool.Parameter;
import org.esa.cci.sst.tool.Tool;
import org.esa.cci.sst.tool.ToolException;
import org.esa.cci.sst.tools.regavg.auxiliary.LUT1;
import org.esa.cci.sst.tools.regavg.auxiliary.LUT2;
import org.esa.cci.sst.util.FileUtil;
import org.esa.cci.sst.util.TimeUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * The SST-CCI Regional-Average tool.
 *
 * @author Norman Fomferra
 * @author Bettina Scholze
 */
public final class AveragingTool extends Tool {

    private static final String FILE_FORMAT_VERSION = "1.1";

    private static final String TOOL_NAME = "org/esa/cci/sst/tools/regavg";
    private static final String TOOL_VERSION = "3.1";
    private static final String TOOL_SYNTAX = TOOL_NAME + " [OPTIONS]";
    private static final String TOOL_HEADER = "\n" +
            "The regavg tool is used to generate regional average time-series from ARC (L2P, L3U) and " +
            "SST_cci (L3U, L3P, L4) product files given a time interval and a list of regions. An output " +
            "NetCDF file will be written for each region.\n" +
            "OPTIONS may be one or more of the following:\n";

    private static final Parameter PARAM_SST_DEPTH = new Parameter("sstDepth", "DEPTH", SstDepth.skin + "",
            "The SST depth. Must be one of " + Arrays.toString(SstDepth.values()) + ".");

    private static final Parameter PARAM_REGION_LIST = new Parameter("regionList", "NAME=REGION[;...]",
            "Global=-180,90,180,-90",
            "A semicolon-separated list of NAME=REGION pairs. "
                    + "REGION may be given as coordinates in the format W,N,E,S "
                    + "or as name of a file that provides a region mask in plain text form. "
                    + "The region mask file contains 72 x 36 5-degree grid cells. "
                    + "Colums correspond to range -180 (first column) to +180 (last column) degrees longitude, "
                    + "while lines correspond to +90 (first line) to -90 (last line) degrees latitude. "
                    + "Cells can be '0' or '1', where "
                    + "a '1' indicates that the region represented by the cell will be considered "
                    + "in the averaging process.");

    private static final Parameter PARAM_START_DATE = new Parameter("startDate", "DATE", "1990-01-01",
            "The start date for the analysis given in the format YYYY-MM-DD");

    private static final Parameter PARAM_END_DATE = new Parameter("endDate", "DATE", "2020-12-31",
            "The end date for the analysis given in the format YYYY-MM-DD");

    private static final Parameter PARAM_CLIMATOLOGY_DIR = new Parameter("climatologyDir", "DIR", "./climatology",
            "The directory path to the reference climatology.");

    private static final Parameter PARAM_TEMPORAL_RES = new Parameter("temporalRes", "NUM",
            TemporalResolution.monthly + "",
            "The temporal resolution. Must be one of " + validTemporalResolutions() + ".");

    private static final Parameter PARAM_PRODUCT_TYPE = new Parameter("productType", "NAME", null,
            "The product type. Must be one of " + Arrays.toString(
                    ProductType.values()) + ".");

    private static final Parameter PARAM_FILENAME_REGEX = new Parameter("filenameRegex", "REGEX", null,
            "The input filename pattern. REGEX is Regular Expression that usually dependends on the parameter " +
                    "'productType'. E.g. the default value for the product type '" + ProductType.ARC_L3U + "' " +
                    "is '" + ProductType.ARC_L3U.getDefaultFilenameRegex() + "'. For example, if you only want " +
                    "to include daily (D) L3 AATSR (ATS) files with night observations only, dual view, 3 channel retrieval, " +
                    "bayes cloud screening (nD3b) you could use the regex \'ATS_AVG_3PAARC\\\\d{8}_D_nD3b[.]nc[.]gz\'.");

    private static final Parameter PARAM_OUTPUT_DIR = new Parameter("outputDir", "DIR", ".", "The output directory.");

    private static final Parameter PARAM_LUT1_FILE = new Parameter("lut1File", "FILE",
            "./auxdata/coverage_uncertainty_parameters.nc",
            "A NetCDF file that provides lookup table 1.");

    // @todo 1 tb/tb adapt default path 2014-11-14
    private static final Parameter PARAM_LUT2_FILE = new Parameter("lut2File", "FILE",
            "./auxdata/RegionalAverage_LUT2.txt",
            "A plain text file that provides lookup table 2.");

    private static final Parameter PARAM_WRITE_TEXT = new Parameter("writeText", null, null,
            "Also writes results to a plain text file 'regavg-output-<date>.txt'.");

    private ProductType productType;

    public static void main(String[] arguments) {
        new AveragingTool().run(arguments);
    }

    // package access for testing only tb 2014-11-14
    static String validTemporalResolutions() {
        final String[] strings = new String[4];

        int i = 0;
        for (TemporalResolution resolution : TemporalResolution.values()) {
            if (!TemporalResolution.weekly5d.equals(resolution) && !TemporalResolution.weekly7d.equals(resolution)) {
                strings[i++] = resolution.name();
            }
        }

        return Arrays.toString(strings);
    }

    @Override
    protected String getName() {
        return TOOL_NAME;
    }

    @Override
    protected String getVersion() {
        return TOOL_VERSION;
    }

    @Override
    protected String getSyntax() {
        return TOOL_SYNTAX;
    }

    @Override
    protected String getHeader() {
        return TOOL_HEADER;
    }

    @Override
    protected String getToolHome() {
        return System.getProperty(TOOL_NAME + ".home", ".");
    }

    @Override
    protected Parameter[] getParameters() {
        ArrayList<Parameter> paramList = new ArrayList<>();
        paramList.addAll(Arrays.asList(
                PARAM_SST_DEPTH,
                PARAM_TEMPORAL_RES,
                PARAM_REGION_LIST,
                PARAM_START_DATE,
                PARAM_END_DATE,
                PARAM_CLIMATOLOGY_DIR,
                PARAM_LUT1_FILE,
                PARAM_LUT2_FILE,
                PARAM_PRODUCT_TYPE,
                PARAM_FILENAME_REGEX,
                PARAM_OUTPUT_DIR,
                PARAM_WRITE_TEXT));
        ProductType[] values = ProductType.values();
        for (ProductType value : values) {
            paramList.add(new Parameter(value.name() + ".dir", "DIR", null,
                    "Directory that hosts the products of type '" + value.name() + "'."));
        }
        return paramList.toArray(new Parameter[paramList.size()]);
    }

    @Override
    protected void run(Configuration configuration, String[] arguments) throws ToolException {
        final String productTypeValue = configuration.getMandatoryStringValue(PARAM_PRODUCT_TYPE.getName(), PARAM_PRODUCT_TYPE.getDefaultValue());
        productType = ProductType.valueOf(productTypeValue);

        final String climatologyDirValue = configuration.getMandatoryStringValue(PARAM_CLIMATOLOGY_DIR.getName(), PARAM_CLIMATOLOGY_DIR.getDefaultValue());
        final String toolHome = configuration.getToolHome();
        final File climatologyDir = FileUtil.getExistingDirectory(climatologyDirValue, toolHome);
        final Climatology climatology = Climatology.create(climatologyDir, productType.getGridDef());

        final String lut_1_path = configuration.getMandatoryStringValue(PARAM_LUT1_FILE.getName(), PARAM_LUT1_FILE.getDefaultValue());
        final File lut1File = FileUtil.getExistingFile(lut_1_path, toolHome);
        final LUT1 lut1 = getLUT1(lut1File);

        final String lut_2_path = configuration.getMandatoryStringValue(PARAM_LUT2_FILE.getName(), PARAM_LUT2_FILE.getDefaultValue());
        final File lut2File = FileUtil.getExistingFile(lut_2_path, toolHome);
        final LUT2 lut2 = getLUT2(lut2File);

        final String productDir = configuration.getMandatoryStringValue(productType + ".dir", null);
        final String filenameRegex = configuration.getStringValue(PARAM_FILENAME_REGEX.getName(), productType.getDefaultFilenameRegex());
        final FileStore fileStore = FileStore.create(productType, filenameRegex, productDir);

        final String sstDepthValue = configuration.getMandatoryStringValue(PARAM_SST_DEPTH.getName(), PARAM_SST_DEPTH.getDefaultValue());
        final SstDepth sstDepth = SstDepth.valueOf(sstDepthValue);

        final String temporalResValue = configuration.getMandatoryStringValue(PARAM_TEMPORAL_RES.getName(), PARAM_TEMPORAL_RES.getDefaultValue());
        final TemporalResolution temporalResolution = TemporalResolution.valueOf(temporalResValue);

        final RegionMaskList regionMaskList = parseRegionList(configuration);
        final Date startDate = configuration.getMandatoryShortUtcDateValue(PARAM_START_DATE.getName(), PARAM_START_DATE.getDefaultValue());
        final Date endDate = configuration.getMandatoryShortUtcDateValue(PARAM_END_DATE.getName(), PARAM_END_DATE.getDefaultValue());
        final AveragingAggregator aggregator = new AveragingAggregator(regionMaskList, fileStore, climatology, lut1, lut2, sstDepth);
        final List<AveragingTimeStep> timeSteps;
        try {
            timeSteps = aggregator.aggregate(startDate, endDate, temporalResolution);
        } catch (IOException e) {
            throw new ToolException("Averaging failed: " + e.getMessage(), e, ToolException.TOOL_IO_ERROR);
        }

        final String outputDirString = configuration.getMandatoryStringValue(PARAM_OUTPUT_DIR.getName(), PARAM_OUTPUT_DIR.getDefaultValue());
        final File outputDir = FileUtil.getExistingDirectory(outputDirString, toolHome);
        final boolean writeText = configuration.getBooleanValue(PARAM_WRITE_TEXT.getName(), false);
        try {
            writeOutputs(outputDir, writeText, productType, filenameRegex,
                    sstDepth, startDate, endDate, temporalResolution, regionMaskList, timeSteps);
        } catch (IOException e) {
            throw new ToolException("Writing of output failed: " + e.getMessage(), e, ToolException.TOOL_IO_ERROR);
        }
    }

    private static LUT1 getLUT1(File lut1File) throws ToolException {
        final LUT1 lut1;
        try {
            lut1 = LUT1.read(lut1File);
            SstLogging.getLogger().info(String.format("LUT-1 read from '%s'", lut1File));
        } catch (IOException e) {
            throw new ToolException(e, ToolException.TOOL_IO_ERROR);
        }
        return lut1;
    }

    private static LUT2 getLUT2(File lut2File) throws ToolException {
        final LUT2 lut2;
        try {
            lut2 = LUT2.read(lut2File);
            SstLogging.getLogger().info(String.format("LUT-2 read from '%s'", lut2File));
        } catch (IOException e) {
            throw new ToolException(e, ToolException.TOOL_IO_ERROR);
        }
        return lut2;
    }

    private void writeOutputs(File outputDir,
                              boolean writeText,
                              ProductType productType,
                              String filenameRegex,
                              SstDepth sstDepth,
                              Date startDate, Date endDate,
                              TemporalResolution temporalResolution,
                              RegionMaskList regionMaskList,
                              List<AveragingTimeStep> timeSteps) throws IOException {

        final PrintWriter textWriter = getTextWriter(outputDir, writeText);

        for (int regionIndex = 0; regionIndex < regionMaskList.size(); regionIndex++) {
            final RegionMask regionMask = regionMaskList.get(regionIndex);
            final String outputFilename = getOutputFilename(TimeUtil.formatInsituFilenameFormat(startDate),
                    TimeUtil.formatInsituFilenameFormat(endDate),
                    regionMask.getName(),
                    productType.getProcessingLevel(),
                    "SST_" + sstDepth + "_average",
                    "PS",
                    "DM"
            );
            final File file = new File(outputDir, outputFilename);
            SstLogging.getLogger().info("Writing output file '" + file + "'...");
            writeOutputFile(file, textWriter, productType, filenameRegex, sstDepth, startDate, endDate,
                    temporalResolution, regionMask, regionIndex, timeSteps);
        }

        if (textWriter != null) {
            textWriter.close();
        }
    }

    private static PrintWriter getTextWriter(File outputDir, boolean writeText) throws IOException {
        final PrintWriter writer;
        if (writeText) {
            String fileName = String.format("%s-output-%s.txt", TOOL_NAME,
                    new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            writer = new PrintWriter(new FileWriter(new File(outputDir, fileName)));
        } else {
            writer = null;
        }
        return writer;
    }

    private static void writeOutputFile(File file, PrintWriter textWriter,
                                        ProductType productType,
                                        String filenameRegex, SstDepth sstDepth,
                                        Date startDate, Date endDate,
                                        TemporalResolution temporalResolution,
                                        RegionMask regionMask, int regionIndex,
                                        List<AveragingTimeStep> timeSteps) throws IOException {
        final NetcdfFileWriteable netcdfFile = NetcdfFileWriteable.createNew(file.getPath());
        try {
            netcdfFile.addGlobalAttribute("title", String.format("%s SST_%s anomalies", productType.toString(),
                    sstDepth.toString()));
            netcdfFile.addGlobalAttribute("institution", "IAES, University of Edinburgh");
            netcdfFile.addGlobalAttribute("contact", "c.merchant@ed.ac.uk");
            netcdfFile.addGlobalAttribute("file_format_version", FILE_FORMAT_VERSION);
            netcdfFile.addGlobalAttribute("tool_name", TOOL_NAME);
            netcdfFile.addGlobalAttribute("tool_version", TOOL_VERSION);
            netcdfFile.addGlobalAttribute("generated_at", TimeUtil.formatIsoUtcFormat(new Date()));
            netcdfFile.addGlobalAttribute("product_type", productType.toString());
            netcdfFile.addGlobalAttribute("sst_depth", sstDepth.toString());
            netcdfFile.addGlobalAttribute("start_date", TimeUtil.formatIsoUtcFormat(startDate));
            netcdfFile.addGlobalAttribute("end_date", TimeUtil.formatIsoUtcFormat(endDate));
            netcdfFile.addGlobalAttribute("temporal_resolution", temporalResolution.toString());
            netcdfFile.addGlobalAttribute("region_name", regionMask.getName());
            netcdfFile.addGlobalAttribute("filename_regex", filenameRegex);

            final int numSteps = timeSteps.size();
            Dimension timeDimension = netcdfFile.addDimension("time", numSteps, true, false, false);
            Dimension[] dims = {timeDimension};

            Variable startTimeVar = netcdfFile.addVariable("start_time", DataType.FLOAT, dims);
            startTimeVar.addAttribute(new Attribute("units", "seconds"));
            startTimeVar.addAttribute(new Attribute("long_name",
                    "reference start time of averaging period in seconds until 1981-01-01T00:00:00"));

            Variable endTimeVar = netcdfFile.addVariable("end_time", DataType.FLOAT, dims);
            endTimeVar.addAttribute(new Attribute("units", "seconds"));
            endTimeVar.addAttribute(new Attribute("long_name",
                    "reference end time of averaging period in seconds until 1981-01-01T00:00:00"));

            final Variable[] variables = productType.getFileType().addResultVariables(netcdfFile, dims, sstDepth);
            final Array[] variableData = new Array[variables.length];
            for (int i = 0; i < variables.length; i++) {
                final Variable variable = variables[i];
                if (variable != null) {
                    variableData[i] = Array.factory(variable.getDataType(), new int[]{numSteps});
                }
            }

            final long millisSince1981 = TimeUtil.createCalendarAtBeginningOfYear(1981).getTimeInMillis();

            final float[] startTime = new float[numSteps];
            final float[] endTime = new float[numSteps];

            for (int t = 0; t < numSteps; t++) {
                final AveragingTimeStep timeStep = timeSteps.get(t);
                startTime[t] = (timeStep.getStartDate().getTime() - millisSince1981) / 1000.0F;
                endTime[t] = (timeStep.getEndDate().getTime() - millisSince1981) / 1000.0F;
                final Number[] results = timeStep.getRegionalAggregationResults(regionIndex);
                for (int i = 0; i < results.length; i++) {
                    if (variableData[i] != null) {
                        variableData[i].setObject(t, results[i]);
                    }
                }
            }
            netcdfFile.create();

            netcdfFile.write(startTimeVar.getFullNameEscaped(), Array.factory(DataType.FLOAT, new int[]{numSteps}, startTime));
            netcdfFile.write(endTimeVar.getFullNameEscaped(), Array.factory(DataType.FLOAT, new int[]{numSteps}, endTime));
            for (int i = 0; i < variables.length; i++) {
                final Variable variable = variables[i];
                if (variable != null) {
                    netcdfFile.write(variable.getFullNameEscaped(), variableData[i]);
                }
            }
            if (textWriter != null) {
                outputText(textWriter, getNames(variables), regionMask.getName(), regionIndex, timeSteps);
            }
        } catch (InvalidRangeException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            e.printStackTrace();
            SstLogging.getLogger().log(Level.SEVERE, "", e);
            throw new IOException(e);
        } finally {
            try {
                netcdfFile.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void outputText(PrintWriter textWriter, String[] outputNames, String regionName, int regionIndex,
                                   List<AveragingTimeStep> timeSteps) {
        textWriter.println();
        textWriter.printf("%s\t%s\t%s\t%s\t%s\n", "region", "start", "end", "step", cat(outputNames, "\t"));
        for (int t = 0; t < timeSteps.size(); t++) {
            AveragingTimeStep timeStep = timeSteps.get(t);
            textWriter.printf("%s\t%s\t%s\t%s\t%s\n",
                    regionName,
                    TimeUtil.formatShortUtcFormat(timeStep.getStartDate()),
                    TimeUtil.formatShortUtcFormat(timeStep.getEndDate()),
                    t + 1,
                    cat(timeStep.getRegionalAggregationResults(regionIndex), "\t"));
        }
    }

    private static String cat(Object[] values, String sep) {
        final StringBuilder sb = new StringBuilder();
        for (final Object value : values) {
            if (value != null) {
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(value);
            }
        }
        return sb.toString();
    }

    private static RegionMaskList parseRegionList(Configuration configuration) throws ToolException {
        try {
            final String regionListValue = configuration.getStringValue(PARAM_REGION_LIST.getName(), PARAM_REGION_LIST.getDefaultValue());
            return RegionMaskList.parse(regionListValue);
        } catch (Exception e) {
            throw new ToolException(e, ToolException.TOOL_USAGE_ERROR);
        }
    }

    private static String[] getNames(Variable[] variables) {
        final List<String> names = new ArrayList<>(variables.length);
        for (final Variable v : variables) {
            if (v != null) {
                names.add(v.getShortName());
            }
        }
        return names.toArray(new String[names.size()]);
    }

    /**
     * Generates a filename of the form
     * <code>
     * <i>startOfPeriod</i><b>-</b><i>endOfPeriod</i><b>-</b><i>regionName</i><b>_average-ESACCI-</b><i>processingLevel</i><b>_GHRSST-</b><i>sstType</i><b>-</b><i>productString</i><b>-</b><i>additionalSegregator</i><b>-v02.0-fv</b><i>fileVersion</i><b>.nc</b>
     * </code>
     *
     * @param startOfPeriod        Start of period = YYYYMMDD
     * @param endOfPeriod          End of period = YYYYMMDD
     * @param regionName           Region Name or Description
     * @param processingLevel      Processing Level = L3C, L3U or L4
     * @param sstType              SST Type
     * @param productString        Product String (see Table 5 in PSD) // TODO - find out from PSD what productString is
     * @param additionalSegregator Additional Segregator = LT or DM  // TODO - find out from PSD what additionalSegregator is
     * @return The filename.
     */
    String getOutputFilename(String startOfPeriod,
                             String endOfPeriod,
                             String regionName,
                             ProcessingLevel processingLevel,
                             String sstType,
                             String productString,
                             String additionalSegregator) {
        String rdac = productType.getFileType().getRdac();
        return String.format("%s-%s-%s_average-" + rdac + "-%s_GHRSST-%s-%s-%s-v%s-fv%s.nc",
                startOfPeriod,
                endOfPeriod,
                regionName,
                processingLevel,
                sstType,
                productString,
                additionalSegregator,
                TOOL_VERSION,
                FILE_FORMAT_VERSION);
    }

    void setProductType(ProductType productType) { // for test only
        this.productType = productType;
    }
}
