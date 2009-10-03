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

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

/**
 * A special-case {@code Matrix} implementation for diagonal matrices. This
 * class provides a memory efficient representation and additional bounds
 * checking to ensure non-diagonal elements cannot be set.
 *
 * @author Keith Stevens 
 */
public class DiagonalMatrix implements Matrix {
        
    private final int diags;
    private double[] values;
    
    /**
     */
    public DiagonalMatrix(int numValues) {
        diags = numValues;
        values = new double[diags];
    }

    /**
     */
    public DiagonalMatrix(int numValues, double[] newValues) {
        diags = numValues;
        values = new double[diags];
        for (int i = 0; i < diags; ++i)
            values[i] = newValues[i];
    }

    /**
     *
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0 || row >= diags || col >= diags) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        if (row == col)
            return values[row];
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(int row) {
        Vector vector = new SparseVector(diags);
        vector.set(row, values[row]);
        return vector;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        double[] returnRow = new double[diags];
        returnRow[row] = values[row];
        return returnRow;
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return diags;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code row != col}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);
        if (row != col) {
            throw new IllegalArgumentException(
                    "cannot set non-diagonal elements in a DiagonalMatrix");
        }
        values[row] = val;
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        values[row] = columns[row];
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[diags][diags];
        for (int r = 0; r < diags; ++r) {
            m[r][r] = values[r];
        }
        return m;
    }

    /**
     *
     */
    public int rows() {
        return diags;
    }
}
