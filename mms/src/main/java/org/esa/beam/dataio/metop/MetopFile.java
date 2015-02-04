/*
 * Copyright (c) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.metop;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.AvhrrFile;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.FlagReader;
import org.esa.beam.dataio.avhrr.HeaderUtil;
import org.esa.beam.dataio.avhrr.calibration.Radiance2ReflectanceFactorCalibrator;
import org.esa.beam.dataio.avhrr.calibration.Radiance2TemperatureCalibrator;
import org.esa.beam.dataio.avhrr.calibration.RadianceCalibrator;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData.UTC;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a product file containing a METOP-AVHRR/3 product.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 */
class MetopFile extends AvhrrFile {

    private static final int EXPECTED_PRODUCT_WIDTH = 2048;

    private static final int HIGH_PRECISION_SAMPLE_RATE = 20;

    private static final int LOW_PRECISION_SAMPLE_RATE = 40;

    private static final int LOW_PRECISION_TIE_POINT_WIDTH = 51;

    private static final int HIGH_PRECISION_TIE_POINT_WIDTH = 103;

    /**
     * Specifies the difference in bytes between products
     * with a low or high precision sample rate.
     */
    private static final int TIE_POINT_DIFFERENCE = 832;

    private static final int TIE_POINT_OFFSET = 20556;


    private static final int FLAG_OFFSET = 22204;

    private static final int FRAME_INDICATOR_OFFSET = 26580;

    private final ImageInputStream inputStream;

    private AsciiRecord mainProductHeaderRecord;

    private AsciiRecord secondaryProductHeaderRecord;

    private GiadrRadiance giadrRadiance;

    private int firstMdrOffset;

    private int mdrSize;

    private int numNavPoints;

    private UTC startTime;
    private UTC endTime;
    private MetadataElement geadrMetadata;

    MetopFile(ImageInputStream imageInputStream) {
        this.inputStream = imageInputStream;
    }

    @Override
    public void readHeader() throws IOException {
        final GenericRecordHeader mphrHeader = new GenericRecordHeader();
        boolean correct = mphrHeader.readGenericRecordHeader(inputStream);

        if (!correct
            || mphrHeader.recordClass != GenericRecordHeader.RecordClass.MPHR
            || mphrHeader.instrumentGroup != GenericRecordHeader.InstrumentGroup.GENERIC
            || mphrHeader.recordSubclass != 0) {
            throw new IOException("Unsupported product: bad MPHR. RecordClass="
                                  + mphrHeader.recordClass + " InstrumentGroup="
                                  + mphrHeader.instrumentGroup + " RecordSubclass="
                                  + mphrHeader.recordSubclass);
        }
        mainProductHeaderRecord = new MainProductHeaderRecord();
        mainProductHeaderRecord.readRecord(inputStream);

        if (mainProductHeaderRecord.getIntValue("TOTAL_SPHR") != 1) {
            throw new IOException("Unsupported Product: more than one SPHR.");
        }
        GenericRecordHeader sphrHeader = new GenericRecordHeader();
        correct = sphrHeader.readGenericRecordHeader(inputStream);

        if (!correct
            || sphrHeader.recordClass != GenericRecordHeader.RecordClass.SPHR
            || sphrHeader.instrumentGroup != GenericRecordHeader.InstrumentGroup.AVHRR_3
            || sphrHeader.recordSubclass != 0) {
            throw new IOException("Unsupported product: bad SPHR. RecordClass="
                                  + sphrHeader.recordClass + " InstrumentGroup="
                                  + sphrHeader.instrumentGroup + " RecordSubclass="
                                  + sphrHeader.recordSubclass);
        }
        secondaryProductHeaderRecord = new SecondaryProductHeaderRecord();
        secondaryProductHeaderRecord.readRecord(inputStream);

        if (secondaryProductHeaderRecord.getIntValue("EARTH_VIEWS_PER_SCANLINE") != EXPECTED_PRODUCT_WIDTH) {
            throw new IOException("Unsupported product: bad SPHR. " +
                                  "EARTH_VIEWS_PER_SCANLINE is not " + EXPECTED_PRODUCT_WIDTH + ". Actual value: " +
                                  secondaryProductHeaderRecord.getIntValue("EARTH_VIEWS_PER_SCANLINE"));
        }
        final int navSampleRate = secondaryProductHeaderRecord.getIntValue("NAV_SAMPLE_RATE");
        if (navSampleRate == LOW_PRECISION_SAMPLE_RATE) {
            numNavPoints = LOW_PRECISION_TIE_POINT_WIDTH;
            productWidth = EXPECTED_PRODUCT_WIDTH;
        } else if (navSampleRate == HIGH_PRECISION_SAMPLE_RATE) {
            numNavPoints = HIGH_PRECISION_TIE_POINT_WIDTH;
            productWidth = EXPECTED_PRODUCT_WIDTH;
        } else {
            throw new IOException("Unsupported product: bad SPHR. " +
                                  "NAV_SAMPLE_RATE is: " + navSampleRate);
        }

        List<InternalPointerRecord> iprs = new ArrayList<>();
        InternalPointerRecord internalPointerRecord;
        do {
            internalPointerRecord = new InternalPointerRecord();
            internalPointerRecord.readRecord(inputStream);
            iprs.add(internalPointerRecord);
        } while (internalPointerRecord.targetRecordClass != GenericRecordHeader.RecordClass.MDR);

        for (InternalPointerRecord ipr : iprs) {
            if (ipr.targetRecordClass == GenericRecordHeader.RecordClass.GIADR) {
                if (ipr.targetRecordSubclass == 1) {
                    inputStream.seek(ipr.targetRecordOffset);
                    giadrRadiance = new GiadrRadiance();
                    giadrRadiance.readRecord(inputStream);
                }
            } else if (ipr.targetRecordClass == GenericRecordHeader.RecordClass.GEADR) {
                inputStream.seek(ipr.targetRecordOffset);
                GenericRecordHeader grh = new GenericRecordHeader();
                grh.readGenericRecordHeader(inputStream);
                byte[] geadrText = new byte[100];
                inputStream.read(geadrText);

                if (geadrMetadata == null) {
                    geadrMetadata = new MetadataElement("GEADR");
                }
                geadrMetadata.addAttribute(HeaderUtil.createAttribute(Integer.toString(grh.recordSubclass),
                                                                      new String(geadrText)));
            } else if (ipr.targetRecordClass == GenericRecordHeader.RecordClass.MDR) {
                firstMdrOffset = ipr.targetRecordOffset;
            }
        }
        productHeight = mainProductHeaderRecord.getIntValue("TOTAL_MDR");
        checkMdrs();
        analyzeFrameIndicator();
    }

    @Override
    public String getProductName() {
        return mainProductHeaderRecord.getValue("PRODUCT_NAME");
    }

    @Override
    public UTC getStartDate() {
        return startTime;
    }

    @Override
    public UTC getEndDate() {
        return endTime;
    }

    @Override
    public void addMetaData(MetadataElement metadataRoot) {
        metadataRoot.addElement(mainProductHeaderRecord.getMetaData());
        metadataRoot.addElement(secondaryProductHeaderRecord.getMetaData());
        if (geadrMetadata != null) {
            metadataRoot.addElement(geadrMetadata);
        }
        metadataRoot.addElement(giadrRadiance.getMetaData());
    }

    @Override
    public BandReader createVisibleRadianceBandReader(int channel) {
        return new PlainBandReader(channel, this, inputStream);
    }

    @Override
    public BandReader createIrRadianceBandReader(int channel) {
        return new PlainBandReader(channel, this, inputStream);
    }

    @Override
    public BandReader createReflectanceFactorBandReader(int channel) {
        RadianceCalibrator radianceCalibrator = new Radiance2ReflectanceFactorCalibrator(
                giadrRadiance.getEquivalentWidth(channel), giadrRadiance
                .getSolarIrradiance(channel), 1);
        //TODO this 1 should be the earth-sun-distance-ratio, but this ratio is always 0.
        return new CalibratedBandReader(channel, this, inputStream, radianceCalibrator);
    }

    @Override
    public BandReader createIrTemperatureBandReader(int channel) {
        RadianceCalibrator radianceCalibrator = new Radiance2TemperatureCalibrator(
                giadrRadiance.getConstant1(channel), giadrRadiance.getConstant2(channel),
                giadrRadiance.getCentralWavenumber(channel));

        return new CalibratedBandReader(channel, this, inputStream, radianceCalibrator);
    }

    @Override
    public BandReader createFlagBandReader() {
        return new FlagReader(this, inputStream);
    }

    @Override
    public boolean hasCloudBand() {
        return true;
    }

    @Override
    public BandReader createCloudBandReader() {
        return new CloudBandReader(this, inputStream);
    }

    public int getNumNavPoints() {
        return numNavPoints;
    }

    public int getNavSampleRate() {
        return secondaryProductHeaderRecord.getIntValue("NAV_SAMPLE_RATE");
    }

    @Override
    public String[] getTiePointNames() {
        return new String[]{
                AvhrrConstants.SZA_DS_NAME,
                AvhrrConstants.VZA_DS_NAME, AvhrrConstants.SAA_DS_NAME,
                AvhrrConstants.VAA_DS_NAME, AvhrrConstants.LAT_DS_NAME,
                AvhrrConstants.LON_DS_NAME
        };
    }

    @Override
    public float[][] getTiePointData() throws IOException {
        final int gridHeight = getProductHeight();
        final int numNavPoints = getNumNavPoints();
        final int numTiePoints = numNavPoints * gridHeight;

        float[][] tiePointData = new float[6][numTiePoints];
        final int numRawAngles = numNavPoints * 4;
        final int numRawLatLon = numNavPoints * 2;

        final short[] rawAngles = new short[numRawAngles];
        final int[] rawLatLon = new int[numRawLatLon];

        int targetIndex = 0;
        for (int scanLine = 0; scanLine < getProductHeight(); scanLine++) {
            final int scanLineOffset = getScanLineOffset(scanLine);
            synchronized (inputStream) {
                inputStream.seek(scanLineOffset + TIE_POINT_OFFSET);
                inputStream.readFully(rawAngles, 0, numRawAngles);
                inputStream.readFully(rawLatLon, 0, numRawLatLon);
            }
            for (int scanPoint = 0; scanPoint < numNavPoints; scanPoint++) {
                tiePointData[0][targetIndex] = rawAngles[scanPoint * 4] * 1E-2f;
                tiePointData[1][targetIndex] = rawAngles[scanPoint * 4 + 1] * 1E-2f;
                tiePointData[2][targetIndex] = rawAngles[scanPoint * 4 + 2] * 1E-2f;
                tiePointData[3][targetIndex] = rawAngles[scanPoint * 4 + 3] * 1E-2f;

                tiePointData[4][targetIndex] = rawLatLon[scanPoint * 2] * 1E-4f;
                tiePointData[5][targetIndex] = rawLatLon[scanPoint * 2 + 1] * 1E-4f;

                targetIndex++;
            }
        }

        return tiePointData;
    }

    @Override
    public int getScanLineOffset(int rawY) {
        return firstMdrOffset + (rawY * mdrSize); // 26660 MDR-1B size
    }

    @Override
    public int getFlagOffset(int rawY) {
        int flagOffset = getScanLineOffset(rawY) + FLAG_OFFSET;
        if (numNavPoints == LOW_PRECISION_TIE_POINT_WIDTH) {
            flagOffset = flagOffset - TIE_POINT_DIFFERENCE;
        }
        return flagOffset;
    }

    @Override
    public int getTiePointTrimX() {
        return AvhrrConstants.TP_TRIM_X;
    }

    @Override
    public int getTiePointSubsampling() {
        return AvhrrConstants.TP_SUB_SAMPLING_X;
    }

    int readFrameIndicator(int rawY) throws IOException {
        int flagOffset = getScanLineOffset(rawY) + FRAME_INDICATOR_OFFSET + 1;
        if (numNavPoints == LOW_PRECISION_TIE_POINT_WIDTH) {
            flagOffset = flagOffset - TIE_POINT_DIFFERENCE;
        }
        byte flag;
        synchronized (inputStream) {
            inputStream.seek(flagOffset);
            flag = inputStream.readByte();
        }
        return flag;
    }

    public static boolean canOpenFile(File file) throws IOException {
        try (final ImageInputStream inputStream = new FileImageInputStream(file)) {
            GenericRecordHeader mphrHeader = new GenericRecordHeader();
            boolean correct = mphrHeader.readGenericRecordHeader(inputStream);
            // check for MPHR
            if (!correct
                || mphrHeader.recordClass != GenericRecordHeader.RecordClass.MPHR
                || mphrHeader.instrumentGroup != GenericRecordHeader.InstrumentGroup.GENERIC
                || mphrHeader.recordSubclass != 0) {
                return false;
            }

            inputStream.seek(mphrHeader.recordSize);
            GenericRecordHeader sphrHeader = new GenericRecordHeader();
            correct = sphrHeader.readGenericRecordHeader(inputStream);

            // check for SPHR and AVHRR/3
            if (correct
                && sphrHeader.recordClass == GenericRecordHeader.RecordClass.SPHR
                && sphrHeader.instrumentGroup == GenericRecordHeader.InstrumentGroup.AVHRR_3
                && sphrHeader.recordSubclass == 0) {
                return true;
            }
        }
        return false;
    }

    private void analyzeFrameIndicator() throws IOException {
        final int first = readFrameIndicator(0);
        final int last = readFrameIndicator(getProductHeight() - 1);

        final int firstChannel3ab = first & 1;
        final int lastChannel3ab = last & 1;
        if (firstChannel3ab == 1 && lastChannel3ab == 1) {
            channel3ab = AvhrrConstants.CH_3A;
        } else if (firstChannel3ab == 0 && lastChannel3ab == 0) {
            channel3ab = AvhrrConstants.CH_3B;
        } else {
            channel3ab = -1;
        }
    }

    private void checkMdrs() throws IOException {
        final GenericRecordHeader firstMdr = new GenericRecordHeader();
        synchronized (inputStream) {
            inputStream.seek(firstMdrOffset);
            boolean correct = firstMdr.readGenericRecordHeader(inputStream);
            if (!correct) {
                throw new IllegalArgumentException("Bad GRH in first MDR.");
            }
        }
        startTime = firstMdr.recordStartTime;
        mdrSize = (int) firstMdr.recordSize;

        final long fileSize = inputStream.length();
        final long expectedFileSize = firstMdrOffset + (productHeight * mdrSize);
        if (fileSize != expectedFileSize) {
            productHeight = (int) ((fileSize - firstMdrOffset) / mdrSize);
        }
        GenericRecordHeader lastMdr = new GenericRecordHeader();
        synchronized (inputStream) {
            inputStream.seek(firstMdrOffset + ((productHeight - 1) * mdrSize));
            boolean correct = lastMdr.readGenericRecordHeader(inputStream);
            if (!correct) {
                throw new IllegalArgumentException("Bad GRH in last MDR.");
            }
        }
        endTime = lastMdr.recordEndTime;
    }

    @Override
    public RawCoordinates getRawCoordinates(int sourceOffsetX,
                                            int sourceOffsetY, int sourceWidth, int sourceHeight) {
        final RawCoordinates coordinates = new RawCoordinates();
        coordinates.minX = sourceOffsetX;
        coordinates.maxX = sourceOffsetX + sourceWidth - 1;
        coordinates.minY = sourceOffsetY;
        coordinates.maxY = sourceOffsetY + sourceHeight - 1;
        coordinates.targetStart = 0;
        coordinates.targetIncrement = 1;

        return coordinates;
    }

    @Override
    public void dispose() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    public InternalTargetTemperatureBandReader createInternalTargetTemperatureReader() {
        return new InternalTargetTemperatureBandReader(this, inputStream, giadrRadiance);
    }

}
