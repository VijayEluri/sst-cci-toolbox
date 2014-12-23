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

package org.eumetsat.beam.dataio.metop;

import org.esa.beam.dataio.avhrr.AvhrrConstants;
import org.esa.beam.dataio.avhrr.HeaderUtil;
import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * Reads the Global Internal Auxiliary Data Record (GIADR)
 * containing Radiance information.
 *
 * @author Marco Zühlke
 */
class GiadrRadiance {

    private short rampCalibrationCoefficient;

    private int yearRecentCalibration;

    private int dayRecentCalibration;

    private int primaryCalibrationAlgorithmId;

    private short primaryCalibrationAlgorithmOption;

    private int secondaryCalibrationAlgorithmId;

    private short secondaryCalibrationAlgorithmOption;

    private double irTempCoeff1[] = new double[4];

    private double irTempCoeff2[] = new double[4];

    private double irTempCoeff3[] = new double[4];

    private double irTempCoeff4[] = new double[4];

    private double irTempCoeff5[] = new double[4];

    private double irTempCoeff6[] = new double[4];

    private float solarFilteredIrradiance[] = new float[3];

    private float equivalentFilteredWidth[] = new float[3];

    private float centralWavenumber[] = new float[3];

    private float constant1[] = new float[3];

    private float constant2Slope[] = new float[3];


    float getSolarIrradiance(int channel) {
        return solarFilteredIrradiance[channel];
    }

    float getEquivalentWidth(int channel) {
        return equivalentFilteredWidth[channel];
    }

    float getCentralWavenumber(int channel) {
        return centralWavenumber[channel - AvhrrConstants.CH_3B];
    }

    float getConstant1(int channel) {
        return constant1[channel - AvhrrConstants.CH_3B];
    }

    float getConstant2(int channel) {
        return constant2Slope[channel - AvhrrConstants.CH_3B];
    }

    void readRecord(ImageInputStream inputStream) throws IOException {
        GenericRecordHeader grh = new GenericRecordHeader();
        boolean correct = grh.readGenericRecordHeader(inputStream);
        if (!correct) {
            throw new IllegalArgumentException("Bad GRH.");
        }

        rampCalibrationCoefficient = inputStream.readShort();
        yearRecentCalibration = inputStream.readUnsignedShort();
        dayRecentCalibration = inputStream.readUnsignedShort();
        primaryCalibrationAlgorithmId = inputStream.readUnsignedShort();
        primaryCalibrationAlgorithmOption = inputStream.readShort();
        secondaryCalibrationAlgorithmId = inputStream.readUnsignedShort();
        secondaryCalibrationAlgorithmOption = inputStream.readShort();
        for (int i = 0; i < 4; i++) {
            irTempCoeff1[i] = inputStream.readShort() * 1E-2;
            irTempCoeff2[i] = inputStream.readShort() * 1E-5;
            irTempCoeff3[i] = inputStream.readShort() * 1E-8;
            irTempCoeff4[i] = inputStream.readShort() * 1E-11;
            irTempCoeff5[i] = inputStream.readShort() * 1E-14;
            irTempCoeff6[i] = inputStream.readShort() * 1E-17;
        }
        for (int i = 0; i < 3; i++) {
            solarFilteredIrradiance[i] = inputStream.readShort() * 1E-1f;
            equivalentFilteredWidth[i] = inputStream.readShort() * 1E-3f;
        }

        for (int i = 0; i < 3; i++) {
            final float cwScaleFactor = (i == 0) ? 1E-2f : 1E-3f;
            centralWavenumber[i] = inputStream.readInt() * cwScaleFactor;
            constant1[i] = inputStream.readInt() * 1E-5f;
            constant2Slope[i] = inputStream.readInt() * 1E-6f;
        }
    }

    MetadataElement getMetaData() {
        final MetadataElement element = new MetadataElement("RADIANCE_CONVERSION");
        element.addAttribute(HeaderUtil.createAttribute("RAMP_CALIBRATION_COEFFICIENT", rampCalibrationCoefficient));
        element.addAttribute(HeaderUtil.createAttribute("YEAR_RECENT_CALIBRATION", yearRecentCalibration, AvhrrConstants.UNIT_YEARS));
        element.addAttribute(HeaderUtil.createAttribute("DAY_RECENT_CALIBRATION", dayRecentCalibration, AvhrrConstants.UNIT_DAYS));
        element.addAttribute(HeaderUtil.createAttribute("PRIMARY_CALIBRATION_ALGORITHM_ID", primaryCalibrationAlgorithmId));
        element.addAttribute(HeaderUtil.createAttribute("PRIMARY_CALIBRATION_ALGORITHM_OPTION", primaryCalibrationAlgorithmOption));
        element.addAttribute(HeaderUtil.createAttribute("SECONDARY_CALIBRATION_ALGORITHM_ID", secondaryCalibrationAlgorithmId));
        element.addAttribute(HeaderUtil.createAttribute("SECONDARY_CALIBRATION_ALGORITHM_OPTION", secondaryCalibrationAlgorithmOption));
        for (int i = 0; i < 4; i++) {
            element.addAttribute(HeaderUtil.createAttribute("IR_TEMPERATURE" + (i + 1) + "_COEFFICIENT1", (float) irTempCoeff1[i], "K"));
            element.addAttribute(HeaderUtil.createAttribute("IR_TEMPERATURE" + (i + 1) + "_COEFFICIENT2", (float) irTempCoeff2[i], "K/cnt"));
            element.addAttribute(HeaderUtil.createAttribute("IR_TEMPERATURE" + (i + 1) + "_COEFFICIENT3", (float) irTempCoeff3[i], "K/cnt^2"));
            element.addAttribute(HeaderUtil.createAttribute("IR_TEMPERATURE" + (i + 1) + "_COEFFICIENT4", (float) irTempCoeff4[i], "K/cnt^3"));
            element.addAttribute(HeaderUtil.createAttribute("IR_TEMPERATURE" + (i + 1) + "_COEFFICIENT5", (float) irTempCoeff5[i], "K/cnt^4"));
            element.addAttribute(HeaderUtil.createAttribute("IR_TEMPERATURE" + (i + 1) + "_COEFFICIENT6", (float) irTempCoeff6[i], "K/cnt^5"));
        }

        for (int i = 0; i < 3; i++) {
            element.addAttribute(HeaderUtil.createAttribute(getNamePrefix(i) + "SOLAR_FILTERED_IRRADIANCE", solarFilteredIrradiance[i], "W/m^2"));
            element.addAttribute(HeaderUtil.createAttribute(getNamePrefix(i) + "EQUIVALENT_FILTER_WIDTH", equivalentFilteredWidth[i], "m^-6"));
        }
        for (int i = 0; i < 3; i++) {
            element.addAttribute(HeaderUtil.createAttribute(getNamePrefix(i + 3) + "CENTRAL_WAVE_NUMBER", centralWavenumber[i], "cm^-1"));
            element.addAttribute(HeaderUtil.createAttribute(getNamePrefix(i + 3) + "CONSTANT1", constant1[i], "K"));
            element.addAttribute(HeaderUtil.createAttribute(getNamePrefix(i + 3) + "CONSTANT2_SLOPE", constant2Slope[i], "K/K"));
        }
        return element;
    }

    private String getNamePrefix(int i) {
        return "CH" + AvhrrConstants.CH_STRINGS[i].toUpperCase() + "_";
    }
}
