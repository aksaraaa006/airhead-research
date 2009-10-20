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
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.List;


/**
 * A growing sparse {@code Matrix} based on the Yale Sparse Matrix Format.  Each
 * row is allocated a pair of arrays which keeps the non-zero column values in
 * column order.  Lookups are O(log n) where n is the number of non-zero values
 * for the largest row.  Calls to set and setColumn can expand the matrix by
 * both rows and columns.
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
    private final List<CompactSparseVector> sparseMatrix;

    /**
     * Create a new empty {@code GrowingSparseMatrix}.
     */
    public GrowingSparseMatrix() {
        this.rows = 0;
        this.cols = 0;
        sparseMatrix = new ArrayList<CompactSparseVector>();
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
        return sparseMatrix.get(row).get(col);
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
     * {@inheritDoc}
     */
    public Vector getColumnVector(int column) {
        Vector values = new CompactSparseVector(rows);
        for (int row = 0; row < rows; ++row)
            values.set(row, get(row, column));
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return sparseMatrix.get(row).toArray(cols);
    }

    /**
     * {@inheritDoc}
     */
    public Vector getRowVector(int row) {
        return Vectors.immutableVector(sparseMatrix.get(row));
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

        // Resize the number of rows if the given row is larger than the max.
        if (row >= sparseMatrix.size()) {
            while (sparseMatrix.size() <= row)
                sparseMatrix.add(new CompactSparseVector());
        }

        // Resize the number of columns if the given column is larger than the
        // max.
        if (col >= cols)
            cols = col + 1;

        sparseMatrix.get(row).set(col, val);
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

        // Resize the number of rows if the given row is larger than the max.
        if (row >= sparseMatrix.size())
            while (sparseMatrix.size() <= row)
                sparseMatrix.add(new CompactSparseVector());

        for (int col = 0; col < cols; ++col) {
            double val = columns[col];
            if (val != 0)
                sparseMatrix.get(row).set(col, val);
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

        // Resize the number of rows if the given row is larger than the max.
        if (row >= sparseMatrix.size())
            while (sparseMatrix.size() <= row)
                sparseMatrix.add(new CompactSparseVector());

        for (int col = 0; col < cols; ++col) {
            double val = columns.get(col);
            if (val != 0)
                sparseMatrix.get(row).set(col, val);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];

        for (int r = 0; r < rows; ++r) 
            m[r] = sparseMatrix.get(r).toArray(cols);
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return sparseMatrix.size();
    }
}
