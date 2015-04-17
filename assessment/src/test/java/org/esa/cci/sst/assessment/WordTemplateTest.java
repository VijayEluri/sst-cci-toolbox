/*
 * Copyright (c) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.assessment;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class WordTemplateTest {

    private static WordDocument wordDocument;

    @BeforeClass
    public static void setUp() throws Exception {
        wordDocument = new WordDocument(WordTemplateTest.class.getResource("car-template.docx"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        wordDocument.save(new File("car.docx"));
    }

    @Ignore
    @Test
    public void testReplaceVariableWithParagraph() throws Exception {
        final String text = "This was '${paragraph.summary_text}' and now is a summary.";

        wordDocument.replaceWithParagraph("${paragraph.summary_text}", text);
    }
}
