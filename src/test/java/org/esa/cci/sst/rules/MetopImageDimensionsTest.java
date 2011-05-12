/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.cci.sst.rules;

import org.esa.cci.sst.data.ColumnBuilder;
import org.esa.cci.sst.data.Item;

import static org.junit.Assert.assertEquals;

public class MetopImageDimensionsTest extends AbstractRuleTest {

    @Override
    public void testNumericConversion() throws RuleException {
        // no numeric conversion is carried out for this rule
    }

    @Override
    protected void assertTargetColumn(Item targetColumn) {
        assertEquals(3, targetColumn.getRank());
        assertEquals("n metop.ny metop.nx", targetColumn.getDimensions());
    }

    @Override
    protected ColumnBuilder configureSourceColumnBuilder(ColumnBuilder columnBuilder) {
        return columnBuilder.rank(1).dimensions("n");
    }
}
