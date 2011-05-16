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
import org.esa.cci.sst.tools.Constants;
import ucar.ma2.DataType;

/**
 * Matchup reference flag
 *
 * @author Ralf Quast
 */
final class MatchupReferenceFlag extends AbstractAttributeModification {

    private static final byte[] FLAG_MASKS = new byte[]{1, 2, 4, 8, 16};
    private static final String FLAG_MEANINGS = "training testing algorithm_selection reference undefined";

    @Override
    protected void configureTargetColumn(ColumnBuilder targetColumnBuilder, Item sourceColumn) {
        targetColumnBuilder.type(DataType.BYTE).
                unsigned(true).
                rank(1).
                dimensions(Constants.DIMENSION_NAME_MATCHUP).
                flagMasks(FLAG_MASKS).
                flagMeanings(FLAG_MEANINGS);
    }

}
