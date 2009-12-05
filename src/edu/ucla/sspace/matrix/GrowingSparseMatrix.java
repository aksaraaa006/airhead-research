/*
 * Copyright 2009 Keith Stevens 
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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseHashVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.util.HashMap;
import java.util.Map;


/**
 * A growing sparse {@code Matrix} based on the Yale Sparse Matrix Format.  Each
 * row is allocated a pair of arrays which keeps the non-zero column values in
 * column order.  Lookups are O(log n) where n is the number of non-zero values
 * for the largest row.  Calls to set and setColumn can expand the matrix by
 * both rows and columns. <p>
 *
 * Calls to {@link #getRowVector(int) getRowVector} and {@link
 * #getColumnVector(int) getColumnVector} return a snapshot of the matrix data
 * at the time of the call.  Subsequent updates to the matrix will not be
 * reflected in these vectors.  The returned vectors are immutable and any calls
 * to mutating operations will throw an {@code UnsupportedOperationException}.
 *
 * This class is not thread-safe.
 *
 * @author Keith Stevens 
 */
public class GrowingSparseMatrix implements Matrix {

    /**
     * The current number of rows in this {@code GrowingSparseMatrix}.
     */
    private int rows;

    /**
     * The current number of columns in this {@code GrowingSparseMatrix}.
     */
    private int cols;

    /**
     * Each row is defined as a {@link CompactSparseVector} which does most of the
     * work.
     */
    private final Map<Integer,CompactSparseVector> rowToColumns;

    /**
     * Create a new empty {@code GrowingSparseMatrix}.
     */
    public GrowingSparseMatrix() {
        this(0,0);
    }

    /**
     * Create a new empty {@code GrowingSparseMatrix} with the specified
     * dimensions.
     *
     * @param rows the number of rows in the matrix
     * @param columns the number of columns in the matrix
     */
    public GrowingSparseMatrix(int rows, int columns) {
        this.rows = rows;
        this.cols = columns;
        rowToColumns = new HashMap<Integer,CompactSparseVector>();
    }

    /**
     * Validate that the row and column indices are non-zero.
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0)
            throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        checkIndices(row, col);
        CompactSparseVector sv = rowToColumns.get(row);
        return (sv == null) ? 0 : sv.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        double[] values = new double[rows];
        for (int row = 0; row < rows; ++row)
            values[row] = get(row, column);
        return values;
    }

    /**
     * Returns a {@link SparseVector} of the contents of the column.
     *
     * @param column {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector getColumnVector(int column) {
        Vector values = new SparseHashVector(rows);
        for (int row = 0; row < rows; ++row) {
            double val = get(row, column);
            values.set(row, val);
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return rowToColumns.get(row).toArray(cols);
    }

    /**
     * Returns a {@link SparseVector} of the contents of the row.
     *
     * @param row {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Vector getRowVector(int row) {
        Vector v = rowToColumns.get(row);
        return (v == null)
            ? new SparseHashVector(cols)
            : Vectors.view(v, 0, cols);
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }

    /**
     * {@inheritDoc}
     *
     * The size of the matrix will be expanded if either row or col is larger
     * than the largest previously seen row or column value.    When the matrix
     * is expanded by either dimension, the values for the new row/column will
     * all be assumed to be zero.
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);

        // Check whether the dimensions need to be updated
        if (row + 1 > rows)
            rows = row + 1;
        if (col + 1 > cols)
            cols = col + 1;

        CompactSparseVector rowVec = rowToColumns.get(row);
        if (rowVec == null) {
            rowVec = new CompactSparseVector();
            rowToColumns.put(row, rowVec);
        }
        rowVec.set(col, val);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        for (int row = 0; row < rows; ++row)
            set(row, column, values[row]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, Vector values) {
        for (int row = 0; row < rows; ++row)
            set(row, column, values.get(row));
    }

    /** 
     * {@inheritDoc}
     *
     * The size of the matrix will be expanded if either row or
     * col is larger than the largest previously seen row or column value.
     * When the matrix is expanded by either dimension, the values for the new
     * row/column will all be assumed to be zero.
     */
    public void setRow(int row, double[] columns) {
        checkIndices(row, columns.length - 1);

        if (cols <= columns.length)
            cols = columns.length;
        
        CompactSparseVector rowVec = rowToColumns.get(row);
        if (rowVec == null) {
            rowVec = new CompactSparseVector();
            rowToColumns.put(row, rowVec);
        }

        for (int col = 0; col < cols; ++col) {
            double val = columns[col];
            rowVec.set(col, val);
        }
    }

    /** 
     * {@inheritDoc}
     *
     * The size of the matrix will be expanded if either row or
     * col is larger than the largest previously seen row or column value.
     * When the matrix is expanded by either dimension, the values for the new
     * row/column will all be assumed to be zero.
     */
    public void setRow(int row, Vector columns) {
        checkIndices(row, columns.length() -1);

        if (cols <= columns.length())
            cols = columns.length();

        CompactSparseVector rowVec = rowToColumns.get(row);
        if (rowVec == null) {
            rowVec = new CompactSparseVector();
            rowToColumns.put(row, rowVec);
        }
     
        Vectors.copy(rowVec, columns);
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; ++r) {
            CompactSparseVector v = rowToColumns.get(r);
            if (v != null) 
                m[r] = v.toArray(cols);
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }
}
