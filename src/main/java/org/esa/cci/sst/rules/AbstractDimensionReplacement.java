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

/**
 * Abstract base class for dimension replacement rules.
 *
 * @author Ralf Quast
 */
abstract class AbstractDimensionReplacement extends AbstractAttributeModification {

    protected AbstractDimensionReplacement() {
    }

    @Override
    public final Item apply(Item sourceColumn) throws RuleException {
        final ColumnBuilder cb = new ColumnBuilder(sourceColumn);
        final DimensionStringBuilder sb = new DimensionStringBuilder(sourceColumn.getDimensions());

        replaceDimensions(sb);
        cb.rank(sb.getDimensionCount());
        cb.dimensions(sb.toString());

        return cb.build();
    }

    protected abstract void replaceDimensions(DimensionStringBuilder builder) throws RuleException;
}
