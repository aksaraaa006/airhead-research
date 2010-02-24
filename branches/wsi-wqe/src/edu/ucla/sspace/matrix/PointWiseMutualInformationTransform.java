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

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.File;

import java.util.Iterator;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class PointWiseMutualInformationTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new PointWiseMutualInformationGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new PointWiseMutualInformationGlobalTransform(
                inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "PMI";
    }

    public class PointWiseMutualInformationGlobalTransform
            implements GlobalTransform {

        /**
         * The total sum of occurances for each row (row).
         */
        private double[] rowCounts;

        /**
         * The total sum of occurances for each col (column).
         */
        private double[] colCounts;

        /**
         * Creates an instance of {@code PointWiseMutualInformationTransform}
         * from a given {@link Matrix}.
         */
        public PointWiseMutualInformationGlobalTransform(Matrix matrix) {
            // Initialize the statistics.
            rowCounts = new double[matrix.rows()];
            colCounts = new double[matrix.columns()];

            if (matrix instanceof SparseMatrix) {
                // Special case for sparse matrices so that only non zero values
                // are traversed.
                SparseMatrix smatrix = (SparseMatrix) matrix;

                // Compute the col and row sums. 
                for (int row = 0; row < matrix.rows(); ++row) {
                    SparseDoubleVector rowVec = smatrix.getRowVector(row);
                    int[] nonZeros = rowVec.getNonZeroIndices();
                    for (int index : nonZeros) {
                        double value = rowVec.get(index);
                        rowCounts[row] += value;
                        colCounts[index] += value;
                    }
                }
            } else {
                // Compute the col and row sums by iterating over all
                // values in the matrix.
                for (int row = 0; row < matrix.rows(); ++row) {
                    for (int col = 0; col < matrix.columns(); ++col) {
                        double value = matrix.get(row, col);
                        rowCounts[row] += value;
                        colCounts[col] += value;
                    }
                }
            }
        }

        /**
         * Creates an instance of {@code PointWiseMutualInformationTransform}
         * from a matrix {@code File} of format {@code format}.
         */
        public PointWiseMutualInformationGlobalTransform(
                File inputMatrixFile,
                MatrixIO.Format format) {
            // Initialize the statistics.
            int numColumns = 0;
            int numRows = 0;
            Map<Integer, Double> rowCountMap = new IntegerMap<Double>();
            Map<Integer, Double> colCountMap = new IntegerMap<Double>();

            // Get an iterator for the matrix file.
            Iterator<MatrixEntry> iter =
                MatrixIO.iterate(inputMatrixFile, format);

            while (iter.hasNext()) {
                MatrixEntry entry = iter.next();

                // Get the total number of columns and rows.
                if (entry.column() >= numColumns)
                    numColumns = entry.column() + 1;
                if (entry.row() >= numRows)
                    numRows = entry.row() + 1;

                // Skip non zero entries.
                if (entry.value() == 0d)
                    continue;

                // Gather the row sums.
                Double occurance = rowCountMap.get(entry.row());
                rowCountMap.put(entry.row(), (occurance == null)
                        ? entry.value()
                        : occurance + entry.value());

                // Gather the column sums.
                occurance = colCountMap.get(entry.column());
                colCountMap.put(entry.column(), (occurance == null)
                        ? entry.value()
                        : occurance + entry.value());
            }

            // Convert the maps to arrays.
            rowCounts = extractValues(rowCountMap, numRows);
            colCounts = extractValues(colCountMap, numColumns);
        }

        /**
         * Extracts the values from the given map into an array form.  This is
         * neccesary since {@code toArray} on a {@link IntegerMap} does not work
         * with primitives and {@code Map} does not provide this functionality.
         * Each key in the map corresponds to an index in the array being
         * created and the value is the value in stored at the specified index.
         */
        private <T extends Number> double[] extractValues(Map<Integer, T> map,
                                                          int size)  {
            double[] values = new double[size];
            for (Map.Entry<Integer, T> entry : map.entrySet()) {
                if (entry.getKey() > values.length)
                    throw new IllegalArgumentException(
                            "Array size is too small for values in the " +
                            "given map");
                values[entry.getKey()] = (double) entry.getKey();
            }
            return values;
        }

        /**
         * Computes the point wise-mutual information between the {@code row}
         * and {@code col} with {@code value} specifying the number of
         * occurances of {@code row} with {@code col}.   This is
         * approximated based on the occurance counts for each {@code row} and
         * {@code col}.
         *
         * @param row The index specifying the row being observed
         * @param col The index specifying the col being observed
         * @param value The number of ocurrances of row and col together
         *
         * @return log(value) / (rowSum[row] * colSum[col])
         */
        public double transform(int row, int col, double value) {
            return Math.log(value) / 
                   (rowCounts[row] * colCounts[col]);
        }
    }
}
