package org.esa.beam.dataio.amsre;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AmsreProductReaderTest {

    @Test
    public void testExtractProductName() {
        final String productName = AmsreProductReader.extractProductName(CORE_META);
        assertEquals("AMSR_E_L2A_BrightnessTemperatures_V12_200502170446_A.hdf", productName);
    }

    @Test
    public void testAssembleUtcString() {
        final String dateString = "2005-02-17";
        final String timeString = "04:46:34.83Z";

        assertEquals("2005-02-17T04:46:34", AmsreProductReader.assembleUTCString(dateString, timeString));
    }

    @Test
    public void testRemoveDots() {
        assertEquals("BT_HV_23_8", AmsreProductReader.removeDots("BT.HV_23.8"));
        assertEquals("blabla_DOT", AmsreProductReader.removeDots("blabla_DOT"));
    }

    private static final String CORE_META = "GROUP = INVENTORYMETADATA\n" +
            "   GROUPTYPE = MASTERGROUP\n" +
            "   GROUP = ECSDATAGRANULE\n" +
            "      OBJECT = LOCALGRANULEID\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"AMSR_E_L2A_BrightnessTemperatures_V12_200502170446_A.hdf\"\n" +
            "      END_OBJECT = LOCALGRANULEID\n" +
            "      OBJECT = PRODUCTIONDATETIME\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"2013-06-16T22:30:15Z\"\n" +
            "      END_OBJECT = PRODUCTIONDATETIME\n" +
            "      OBJECT = REPROCESSINGACTUAL\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"reprocessed twice\"\n" +
            "      END_OBJECT = REPROCESSINGACTUAL\n" +
            "   END_GROUP = ECSDATAGRANULE\n" +
            "   GROUP = MEASUREDPARAMETER\n" +
            "      OBJECT = MEASUREDPARAMETERCONTAINER\n" +
            "         CLASS = \"1\"\n" +
            "         OBJECT = PARAMETERNAME\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = \"Brightness Temperatures\"\n" +
            "         END_OBJECT = PARAMETERNAME\n" +
            "         GROUP = QAFLAGS\n" +
            "            CLASS = \"1\"\n" +
            "            OBJECT = AUTOMATICQUALITYFLAG\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Passed\"\n" +
            "            END_OBJECT = AUTOMATICQUALITYFLAG\n" +
            "            OBJECT = AUTOMATICQUALITYFLAGEXPLANATION\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Program Execution Completed\"\n" +
            "            END_OBJECT = AUTOMATICQUALITYFLAGEXPLANATION\n" +
            "            OBJECT = OPERATIONALQUALITYFLAG\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Not Investigated\"\n" +
            "            END_OBJECT = OPERATIONALQUALITYFLAG\n" +
            "            OBJECT = OPERATIONALQUALITYFLAGEXPLANATION\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Currently not used\"\n" +
            "            END_OBJECT = OPERATIONALQUALITYFLAGEXPLANATION\n" +
            "            OBJECT = SCIENCEQUALITYFLAG\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Passed\"\n" +
            "            END_OBJECT = SCIENCEQUALITYFLAG\n" +
            "            OBJECT = SCIENCEQUALITYFLAGEXPLANATION\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Greater than 50% valid Brightness Temperatures\"\n" +
            "            END_OBJECT = SCIENCEQUALITYFLAGEXPLANATION\n" +
            "         END_GROUP = QAFLAGS\n" +
            "         GROUP = QASTATS\n" +
            "            CLASS = \"1\"\n" +
            "            OBJECT = QAPERCENTMISSINGDATA\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = 22\n" +
            "            END_OBJECT = QAPERCENTMISSINGDATA\n" +
            "            OBJECT = QAPERCENTOUTOFBOUNDSDATA\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = 0\n" +
            "            END_OBJECT = QAPERCENTOUTOFBOUNDSDATA\n" +
            "         END_GROUP = QASTATS\n" +
            "      END_OBJECT = MEASUREDPARAMETERCONTAINER\n" +
            "   END_GROUP = MEASUREDPARAMETER\n" +
            "   GROUP = ORBITCALCULATEDSPATIALDOMAIN\n" +
            "      OBJECT = ORBITCALCULATEDSPATIALDOMAINCONTAINER\n" +
            "         CLASS = \"1\"\n" +
            "         OBJECT = ORBITALMODELNAME\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = \"WRS-2\"\n" +
            "         END_OBJECT = ORBITALMODELNAME\n" +
            "         OBJECT = STARTORBITNUMBER\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = 14853\n" +
            "         END_OBJECT = STARTORBITNUMBER\n" +
            "         OBJECT = STOPORBITNUMBER\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = 14854\n" +
            "         END_OBJECT = STOPORBITNUMBER\n" +
            "         OBJECT = EQUATORCROSSINGLONGITUDE\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = 124.65\n" +
            "         END_OBJECT = EQUATORCROSSINGLONGITUDE\n" +
            "         OBJECT = EQUATORCROSSINGTIME\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = \"05:13:45.20Z\"\n" +
            "         END_OBJECT = EQUATORCROSSINGTIME\n" +
            "         OBJECT = EQUATORCROSSINGDATE\n" +
            "            NUM_VAL = 1\n" +
            "            CLASS = \"1\"\n" +
            "            VALUE = \"2005-02-17\"\n" +
            "         END_OBJECT = EQUATORCROSSINGDATE\n" +
            "      END_OBJECT = ORBITCALCULATEDSPATIALDOMAINCONTAINER\n" +
            "   END_GROUP = ORBITCALCULATEDSPATIALDOMAIN\n" +
            "   GROUP = COLLECTIONDESCRIPTIONCLASS\n" +
            "      OBJECT = SHORTNAME\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"AE_L2A\"\n" +
            "      END_OBJECT = SHORTNAME\n" +
            "      OBJECT = VERSIONID\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = 3\n" +
            "      END_OBJECT = VERSIONID\n" +
            "   END_GROUP = COLLECTIONDESCRIPTIONCLASS\n" +
            "   GROUP = INPUTGRANULE\n" +
            "      OBJECT = INPUTPOINTER\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"P1AME050217220MA_P01A0000000.00\"\n" +
            "      END_OBJECT = INPUTPOINTER\n" +
            "   END_GROUP = INPUTGRANULE\n" +
            "   GROUP = RANGEDATETIME\n" +
            "      OBJECT = RANGEBEGINNINGDATE\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"2005-02-17\"\n" +
            "      END_OBJECT = RANGEBEGINNINGDATE\n" +
            "      OBJECT = RANGEBEGINNINGTIME\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"04:46:34.83Z\"\n" +
            "      END_OBJECT = RANGEBEGINNINGTIME\n" +
            "      OBJECT = RANGEENDINGDATE\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"2005-02-17\"\n" +
            "      END_OBJECT = RANGEENDINGDATE\n" +
            "      OBJECT = RANGEENDINGTIME\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"05:36:36.10Z\"\n" +
            "      END_OBJECT = RANGEENDINGTIME\n" +
            "   END_GROUP = RANGEDATETIME\n" +
            "   GROUP = PGEVERSIONCLASS\n" +
            "      OBJECT = PGEVERSION\n" +
            "         NUM_VAL = 1\n" +
            "         VALUE = \"12\"\n" +
            "      END_OBJECT = PGEVERSION\n" +
            "   END_GROUP = PGEVERSIONCLASS\n" +
            "   GROUP = ADDITIONALATTRIBUTES\n" +
            "      OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "         CLASS = \"1\"\n" +
            "         OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "            CLASS = \"1\"\n" +
            "            NUM_VAL = 1\n" +
            "            VALUE = \"AscendingDescendingFlg\"\n" +
            "         END_OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "         GROUP = INFORMATIONCONTENT\n" +
            "            CLASS = \"1\"\n" +
            "            OBJECT = PARAMETERVALUE\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"1\"\n" +
            "               VALUE = \"Ascending\"\n" +
            "            END_OBJECT = PARAMETERVALUE\n" +
            "         END_GROUP = INFORMATIONCONTENT\n" +
            "      END_OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "      OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "         CLASS = \"2\"\n" +
            "         OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "            CLASS = \"2\"\n" +
            "            NUM_VAL = 1\n" +
            "            VALUE = \"NominalPassIndex\"\n" +
            "         END_OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "         GROUP = INFORMATIONCONTENT\n" +
            "            CLASS = \"2\"\n" +
            "            OBJECT = PARAMETERVALUE\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"2\"\n" +
            "               VALUE = \"125\"\n" +
            "            END_OBJECT = PARAMETERVALUE\n" +
            "         END_GROUP = INFORMATIONCONTENT\n" +
            "      END_OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "      OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "         CLASS = \"3\"\n" +
            "         OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "            CLASS = \"3\"\n" +
            "            NUM_VAL = 1\n" +
            "            VALUE = \"StartingPolygonNumber\"\n" +
            "         END_OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "         GROUP = INFORMATIONCONTENT\n" +
            "            CLASS = \"3\"\n" +
            "            OBJECT = PARAMETERVALUE\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"3\"\n" +
            "               VALUE = \"1\"\n" +
            "            END_OBJECT = PARAMETERVALUE\n" +
            "         END_GROUP = INFORMATIONCONTENT\n" +
            "      END_OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "      OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "         CLASS = \"4\"\n" +
            "         OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "            CLASS = \"4\"\n" +
            "            NUM_VAL = 1\n" +
            "            VALUE = \"EndingPolygonNumber\"\n" +
            "         END_OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "         GROUP = INFORMATIONCONTENT\n" +
            "            CLASS = \"4\"\n" +
            "            OBJECT = PARAMETERVALUE\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"4\"\n" +
            "               VALUE = \"18\"\n" +
            "            END_OBJECT = PARAMETERVALUE\n" +
            "         END_GROUP = INFORMATIONCONTENT\n" +
            "      END_OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "      OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "         CLASS = \"5\"\n" +
            "         OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "            CLASS = \"5\"\n" +
            "            NUM_VAL = 1\n" +
            "            VALUE = \"identifier_product_doi\"\n" +
            "         END_OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "         GROUP = INFORMATIONCONTENT\n" +
            "            CLASS = \"5\"\n" +
            "            OBJECT = PARAMETERVALUE\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"5\"\n" +
            "               VALUE = \"10.5067/AMSR-E/AE_L2A.003\"\n" +
            "            END_OBJECT = PARAMETERVALUE\n" +
            "         END_GROUP = INFORMATIONCONTENT\n" +
            "      END_OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "      OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "         CLASS = \"6\"\n" +
            "         OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "            CLASS = \"6\"\n" +
            "            NUM_VAL = 1\n" +
            "            VALUE = \"identifier_product_doi_authority\"\n" +
            "         END_OBJECT = ADDITIONALATTRIBUTENAME\n" +
            "         GROUP = INFORMATIONCONTENT\n" +
            "            CLASS = \"6\"\n" +
            "            OBJECT = PARAMETERVALUE\n" +
            "               NUM_VAL = 1\n" +
            "               CLASS = \"6\"\n" +
            "               VALUE = \"http://dx.doi.org\"\n" +
            "            END_OBJECT = PARAMETERVALUE\n" +
            "         END_GROUP = INFORMATIONCONTENT\n" +
            "      END_OBJECT = ADDITIONALATTRIBUTESCONTAINER\n" +
            "   END_GROUP = ADDITIONALATTRIBUTES\n" +
            "END_GROUP = INVENTORYMETADATA\n" +
            "END\n";


}
