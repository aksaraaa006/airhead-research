/*
 * Copyright 2010 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.MaskedDoubleVectorView;

import java.util.Map;


/**
 * This {@link Matrix} decorator allows every row and column index to be
 * remapped to new indices.  The size of this matrix will depend on the number
 * of mapped rows and columns.  Write-through access is allowed for each cell in
 * the underlying matrix through the {@code set} method.  Array based accessors
 * and getters are disabled when using this decorator.
 *
 * @author Keith Stevens
 */
public class CellMaskedMatrix implements Matrix {
    
    /**
     * The mapping for rows from new indices to old indices.
     */
    private final Map<Integer, Integer> rowMaskMap;

    /**
     * The mapping for columns from new indices to old indices.
     */
    private final Map<Integer, Integer> colMaskMap;

    /**
     * The original underlying matrix.
     */
    private final Matrix matrix;

    /**
     * Creates a new {@link CellMaskedMatrix} from a given {@link Matrix} and
     * maps, one for the row indices and one for the column indices.  Only valid
     * mappings should be included.
     *
     * @param matrix The underlying matrix to decorate
     * @param rowMaskMap A mapping from new indices to old indices in the
     *        original map for rows.
     * @param colMaskMap A mapping from new indices to old indices in the
     *        original map for columns.
     */
    public CellMaskedMatrix(Matrix matrix,
                            Map<Integer, Integer> rowMaskMap,
                            Map<Integer, Integer> colMaskMap) {
        this.matrix = matrix;
        this.rowMaskMap = rowMaskMap;
        this.colMaskMap = colMaskMap;
    }

    /**
     * Returns the new index value for a given index from a given mapping.
     * Returns -1 if no mapping is found for the requested row.
     */
    private int getIndexFromMap(Map<Integer, Integer> indexMap, int row) {
        Integer newRow = indexMap.get(row);
        return (newRow == null) ? -1 : newRow.intValue();
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        row = getIndexFromMap(rowMaskMap, row);
        col = getIndexFromMap(colMaskMap, col);

        if (row == -1 || col == -1)
            return 0;
        return matrix.get(row, col);
    }

    /**
     * Unsupported.
     */
    public double[] getColumn(int column) {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        DoubleVector v = matrix.getColumnVector(column);
        return new MaskedDoubleVectorView(v, rowMaskMap);
    }


    /**
     * Unsupported.
     */
    public double[] getRow(int row) {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        DoubleVector v = matrix.getRowVector(row);
        return new MaskedDoubleVectorView(v, colMaskMap);
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return colMaskMap.size();
    }
    
    /**
     * Unsupported.
     */
    public double[][] toDenseArray() {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rowMaskMap.size();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        row = getIndexFromMap(rowMaskMap, row);
        col = getIndexFromMap(colMaskMap, col);

        if (row == -1 || col == -1)
            return;
        matrix.set(row, col, val);
    }

    /**
     * Unsupported.
     */
    public void setColumn(int column, double[] values) {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }

    /**
     * Unsupported.
     */
    public void setColumn(int column, DoubleVector values) {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }

    /**
     * Unsupported.
     */
    public void setRow(int row, double[] columns) {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        throw new UnsupportedOperationException("Cannot retrieve the column;");
    }
}
