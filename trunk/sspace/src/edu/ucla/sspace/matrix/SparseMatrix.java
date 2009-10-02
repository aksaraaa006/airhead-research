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

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A sparse {@code Matrix} based on the Yale Sparse Matrix Format.
 * Each row is allocated a linked list structure which keeps the non-zero column
 * values in column order.  Lookups are O(log n) where n is the number of
 * non-zero values for the largest row.  Inserting is currently dependent on the
 * insertion time of an ArrayList.
 *
 * @author David Jurgens
 */
public class SparseMatrix implements Matrix {
        
    private final int rows;

    private final int cols;
    
    /**
     * Each row is defined as a {@link RowEntry} which does most of the work.
     */
    private final RowEntry[] sparseMatrix;

    /**
     */
    public SparseMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        sparseMatrix = new RowEntry[rows];
        for (int i = 0; i < rows; ++i)
            sparseMatrix[i] = new RowEntry();
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
        return sparseMatrix[row].getValue(col);
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(int row) {
        return sparseMatrix[row].getVector(cols);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return sparseMatrix[row].getRow(cols);
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
        sparseMatrix[row].setValue(col, val);
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
            sparseMatrix[row].setValue(col, columns[col]);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; ++r) {
            m[r] = sparseMatrix[r].getRow(cols);
        }
        return m;
    }

    /**
     *
     */
    public int rows() {
        return rows;
    }

    public static final class RowEntry {
        /**
         * An arraylist of non zero values for this row, stored in the correct
         * column order.
         */
        private ArrayList<Double> values;
        /**
         * An arraylist of which column indexes are stored for this row in
         * sorted order.
         */
        private ArrayList<Integer> columnIndexes;

        /**
         * Create the two lists, with zero values in them initially.
         */
        public RowEntry() {
            values = new ArrayList<Double>();
            columnIndexes = new ArrayList<Integer>();
        }

        /**
         * retrieve the value at specified column
         *
         * @param column The column value to get
         * @return the value for the specified column, or 0 if no column is
         *         found.
         */
        public double getValue(int column) {
            int valueIndex = Collections.binarySearch(columnIndexes, column);
            return (valueIndex >= 0) ? values.get(valueIndex) : 0.0;
        }

        /**
         * Update the RowEntry such that the index at column now stores value.
         * If value is 0, this will remove the column from the row entry for
         * efficency.
         *
         * @param column The column index this value should be stored as
         * @param value The value to store
         */
        public void setValue(int column, double value) {
            int valueIndex = Collections.binarySearch(columnIndexes, column);
            if (valueIndex >= 0 && value > 0.0) {
                values.set(valueIndex, value);
            } else if (value > 0.0) {
                values.add((valueIndex + 1) * -1, value);
                columnIndexes.add((valueIndex+1) * -1, column);
            } else if (valueIndex >= 0) {
                values.remove(valueIndex);
                columnIndexes.remove(valueIndex);
            }
        }

        /**
         * A dense double array which this RowEntry represents.
         */
        public Vector getVector(int columnSize) {
            Vector vector = new SparseDoubleVector(columnSize);
            for (int i = 0; i < columnIndexes.size(); ++i) {
                vector.set(columnIndexes.get(i).intValue(),
                           values.get(i).doubleValue());
            }
            return vector;
        }

        /**
         * A dense double array which this RowEntry represents.
         */
        public double[] getRow(int columnSize) {
            double[] dense = new double[columnSize];
            for (int i = 0; i < columnIndexes.size(); ++i) {
                dense[columnIndexes.get(i).intValue()] =
                    values.get(i).doubleValue();
            }
            return dense;
        }
    }
}
