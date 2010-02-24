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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;


/**
 * A special cased {@link Matrix} that is symmetric.  Only half of the values in
 * the matrix are stored to save some room.  
 *
 * @author Keith Stevens
 */
public class SymmetricMatrix implements Matrix {

    /**
     * The top half unique values representing this matrix.  This top half
     * includes the diagonal values.
     */
    private double[][] values;

    /**
     * The number of rows and columns in the matrix.
     */
    private final int size;

    /**
     * Creates a new {@link SymmetricMatrix} with {@code size} rows and columns
     */
    public SymmetricMatrix(int size) {
        this.size = size;
        values = new double[size][0];
        // For each row, drop any cell that is below the diagonal.
        for (int row = 0; row < size; ++row)
            values[row] = new double[size - row];
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        // If the row is larger than the column, swap the indices, this works
        // since the matrix is symmetric.
        if (row > col) {
            int temp = row;
            row = col;
            col = temp;
        }
        return values[row][col - row];
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        double[] colValues = new double[size];
        for (int row = 0; row < size; ++row)
            colValues[row] = get(row,column);
        return colValues;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        return Vectors.asVector(getColumn(column));
    }

    public double[] getRow(int row) {
        double[] rowValues = new double[size];
        for (int i = 0; i < size; ++i)
            rowValues[i] = get(row, i);
        return rowValues;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return Vectors.asVector(getRow(row));
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] results = new double[size][size];
        for (int i = 0; i < size; ++i) 
            for (int j = 0; j < size; ++j) 
                results[i][j] = get(i, j);
        return results;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        if (row > col) {
            int temp = row;
            row = col;
            col = temp;
        }

        values[row][col-row] = val;
    }

    /**
     * Not supported.
     */
    public void setColumn(int column, double[] values) {
        throw new UnsupportedOperationException(
                "Setting values with column arrays is not permited");
    }

    /**
     * Not supported.
     */
    public void setColumn(int column, DoubleVector values) {
        throw new UnsupportedOperationException(
                "Setting values with column arrays is not permited");
    }

    /**
     * Not supported.
     */
    public void setRow(int row, double[] columns) {
        throw new UnsupportedOperationException(
                "Setting values with row arrays is not permited");
    }

    /**
     * Not supported.
     */
    public void setRow(int row, DoubleVector columns) {
        throw new UnsupportedOperationException(
                "Setting values with row arrays is not permited");
    }
}
