/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

/**
 * A sparse {@code Matrix} based on the Yale Sparse Matrix Format, as
 * implemented in {@link SparseVector}.  Each row is allocated a pair of arrays
 * which keeps the non-zero column values in column order.  Lookups are O(log n)
 * where n is the number of non-zero values for the largest row.   The size of
 * this matrix is fixed, and attempts to access rows or columns beyond the size
 * will throw an exception.
 *
 *
 * @author David Jurgens
 */
public class SparseMatrix implements Matrix {

    private final int rows;

    private final int cols;

    /**
     * Each row is defined as a {@link SparseVector} which does most of the
     * work.
     */
    private final SparseVector[] sparseMatrix;

    /**
     */
    public SparseMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        sparseMatrix = new SparseVector[rows];
        for (int i = 0; i < rows; ++i)
            sparseMatrix[i] = new SparseVector(cols);
    }

    /**
     *
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0 || row >= rows || col >= cols) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return sparseMatrix[row].get(col);
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(int row) {
        return sparseMatrix[row];
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return sparseMatrix[row].toArray(cols);
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        sparseMatrix[row].set(col, val);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        if (columns.length != cols) {
            throw new IllegalArgumentException(
            "invalid number of columns: " + columns.length);
        }
        for (int col = 0; col < cols; ++col) {
            sparseMatrix[row].set(col, columns[col]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; ++r) {
            m[r] = sparseMatrix[r].toArray(cols);
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
