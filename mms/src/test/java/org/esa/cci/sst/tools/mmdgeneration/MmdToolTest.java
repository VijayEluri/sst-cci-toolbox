package org.esa.cci.sst.tools.mmdgeneration;


import org.esa.cci.sst.ColumnRegistry;
import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.tools.Configuration;
import org.esa.cci.sst.tools.ToolException;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

public class MmdToolTest {

    @Test
    public void testCreateNetCDFWriter() throws IOException {
        final Configuration configuration = new Configuration();
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_DIR, "here/we");
        configuration.put(Configuration.KEY_MMS_MMD_TARGET_FILENAME, "are_now.nc");

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetCDFWriter(configuration);
        assertNotNull(netCDFWriter);
        assertEquals(NetcdfFileWriter.Version.netcdf4, netCDFWriter.getVersion());
        final NetcdfFile netcdfFile = netCDFWriter.getNetcdfFile();
        assertEquals(toPath("here", "we", "are_now.nc"), netcdfFile.getLocation());
    }

    @Test
    public void testCreateNetCDFWriter_withConfigurationDefaultValues() throws IOException {
        final Configuration configuration = new Configuration();

        final NetcdfFileWriter netCDFWriter = MmdTool.createNetCDFWriter(configuration);
        assertNotNull(netCDFWriter);
        assertEquals(NetcdfFileWriter.Version.netcdf4, netCDFWriter.getVersion());
        final NetcdfFile netcdfFile = netCDFWriter.getNetcdfFile();
        assertEquals(toPath(".", "mmd.nc"), netcdfFile.getLocation());
    }

    @Test
    public void testInitializeDimensionNames_emptyNameList() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        final ArrayList<String> nameList = new ArrayList<>();

        final TreeSet<String> dimensionNames = MmdTool.initializeDimensionNames(nameList, columnRegistry);
        assertNotNull(dimensionNames);
        assertEquals(0, dimensionNames.size());
    }

    @Test
    public void testInitializeDimensionNames() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("a b c").rank(3).build());
        columnRegistry.register(new ColumnBuilder().name("Klaus").dimensions("left right").rank(2).build());

        final ArrayList<String> nameList = new ArrayList<>();
        nameList.add("Klaus");
        nameList.add("Heike");

        final TreeSet<String> dimensionNames = MmdTool.initializeDimensionNames(nameList, columnRegistry);
        assertNotNull(dimensionNames);
        assertEquals(5, dimensionNames.size());
        assertThat(dimensionNames, hasItem("left"));
        assertThat(dimensionNames, hasItem("b"));
    }

    @Test
    public void testInitializeDimensionNames_emptyDimensionTriggersException() {
        final ColumnRegistry columnRegistry = new ColumnRegistry();
        columnRegistry.register(new ColumnBuilder().name("Heike").dimensions("").rank(0).build());

        final ArrayList<String> nameList = new ArrayList<>();
        nameList.add("Heike");

        try {
            MmdTool.initializeDimensionNames(nameList, columnRegistry);
            fail("ToolException expected");
        } catch (ToolException expected) {
        }
    }

    private String toPath(String... pathComponents) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String component : pathComponents) {
            stringBuilder.append(component);
            stringBuilder.append(File.separator);
        }

        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }
}