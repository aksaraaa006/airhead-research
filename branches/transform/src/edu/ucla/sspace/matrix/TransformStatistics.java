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

import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.IOError;
import java.io.IOException;
import java.io.File;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A static utility class used for gather statistics that are frequently used in
 * matrix {@link Transform} implementations.  Given a {@link Matrix} or a {@link
 * Matrix} file, this class will gather row summations, column summations, and
 * the total summation of the matrix.  Optionally, when gathering either the row
 * or column summations, the number of non zero values in the row or column can
 * be counted instead of a full summation, which is needed for the {@link
 * TfIdfTransform}.  
 *
 * @author Keith Stevens
 */
public class TransformStatistics {

    enum StatisticType {
        ROW_OCCURRANCE,
        COLUMN_OCCURRANCE,
        ROW_SUMS,
        COLUMN_SUMS,
        MATRIX_SUM,
        MATRIX_COUNTS,
    }

    /**
     * Extracts the row, column, and matrix summations based on entries in
     * the given {@link Matrix}.  If {@code countRowOccurrances} is true, the
     * number of non zeros in each row will be counted for the row summation.
     * If {@code countColumnOccurrances} is true, the same will be done for the
     * columns.  In either case, the matrix summation will remain the same.
     *
     * @param matrix a {@link Matrix} to sum over
     * @param countRowOccurrances true if the row summation should only count
     *        the number of non zero values in a row
     * @param countColumnOccurrances true if the column summation should only
     *        count the number of non zero values in a column 
     * @return a {@link MatrixStatistics} instance containing the summations
     */
    public static MatrixStatistics extractStatistics(
            Matrix matrix,
            Set<StatisticType> statType) {
        // Initialize the statistics.
        MatrixStatistics stats = createStatistics(
                matrix.rows(),matrix.columns(),statType);

        if (matrix instanceof SparseMatrix) {
            // Special case for sparse matrices so that only non zero values
            // are traversed.
            SparseMatrix smatrix = (SparseMatrix) matrix;

            // Compute the col and row sums. 
            for (int row = 0; row < matrix.rows(); ++row) {
                SparseDoubleVector rowVec = smatrix.getRowVector(row);
                int[] nonZeros = rowVec.getNonZeroIndices();
                for (int col : nonZeros) {
                    double value = rowVec.get(col);
                    handleValue(row, col, value, statType, stats);
                }
            }
        } else {
            // Compute the col and row sums by iterating over all
            // values in the matrix.
            for (int row = 0; row < matrix.rows(); ++row) {
                for (int col = 0; col < matrix.columns(); ++col) {
                    double value = matrix.get(row, col);
                    handleValue(row, col, value, statType, stats);
                }
            }
        }
        return stats;
    }

    private static MatrixStatistics createStatistics(int rows, int columns,
                                                     Set<StatisticType> types) {
        double[] rowSums = null;
        double[] columnSums = null;
        int[] rowCounts = null;
        int[] columnCounts = null;
        double matrixSum = 0;
        int matrixCounts = 0;

        for (StatisticType type : types) {
            switch (type) {
                case ROW_OCCURRANCE:
                    rowCounts = new int[rows];
                    break;
                case COLUMN_OCCURRANCE:
                    columnCounts = new int[columns];
                    break;
                case ROW_SUMS:
                    rowSums = new double[rows];
                    break;
                 case COLUMN_SUMS:
                    columnSums = new double[columns];
                    break;
            }
        }
        return new MatrixStatistics(rowSums, columnSums,
                                    rowCounts, columnCounts,
                                    matrixSum, matrixCounts);
    }

    private static void handleValue(int row, int col, 
                                    double value, Set<StatisticType> types,
                                    MatrixStatistics stats) {
        if (value == 0d)
            return;

        if (types.contains(StatisticType.ROW_OCCURRANCE))
            stats.rowCounts[row]++;
        if (types.contains(StatisticType.ROW_SUMS))
            stats.rowSums[row] += value;
        if (types.contains(StatisticType.COLUMN_OCCURRANCE))
            stats.columnCounts[col]++;
        if (types.contains(StatisticType.COLUMN_SUMS))
            stats.columnSums[col] += value;
        if (types.contains(StatisticType.MATRIX_COUNTS))
            stats.matrixCounts++;
        if (types.contains(StatisticType.MATRIX_SUM))
            stats.matrixSum += value;
    }

    /**
     * Extracts the row, column, and matrix summations based on entries in
     * the given {@link Matrix}.  If {@code countRowOccurrances} is true, the
     * number of non zeros in each row will be counted for the row summation.
     * If {@code countColumnOccurrances} is true, the same will be done for the
     * columns.  In either case, the matrix summation will remain the same.
     *
     * @param inputMatrixFfile a {@link Matrix} file  to sum over
     * @param format the matrix {@link Format} of {@code inputMatrixFile}
     * @param countRowOccurrances true if the row summation should only count
     *        the number of non zero values in a row
     * @param countColumnOccurrances true if the column summation should only
     *        count the number of non zero values in a column 
     * @return a {@link MatrixStatistics} instance containing the summations
     */
    public static MatrixStatistics extractStatistics(
            File inputMatrixFile,
            Format format,
            Set<StatisticType> types) {
        // Initialize the statistics.
        int numColumns = 0;
        int numRows = 0;
        double matrixSum = 0;
        int matrixCount = 0;
        Map<Integer, Double> rowSumMap = new IntegerMap<Double>();
        Map<Integer, Double> columnSumMap = new IntegerMap<Double>();
        Map<Integer, Integer> rowCountMap = new IntegerMap<Integer>();
        Map<Integer, Integer> columnCountMap = new IntegerMap<Integer>();

        // Get an iterator for the matrix file.
        Iterator<MatrixEntry> iter;
        try {
            iter = MatrixIO.getMatrixFileIterator(inputMatrixFile, format);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

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

            if (types.contains(StatisticType.ROW_OCCURRANCE)) {
                Integer occurance = rowCountMap.get(entry.row());
                rowCountMap.put(entry.row(), (occurance == null)
                        ? 1 
                        : 1 + occurance);
            }
            if (types.contains(StatisticType.COLUMN_OCCURRANCE)) {
                Integer occurance = columnCountMap.get(entry.column());
                rowCountMap.put(entry.column(), (occurance == null)
                        ? 1
                        : 1 + occurance);
            }
            if (types.contains(StatisticType.COLUMN_SUMS)) {
                Double occurance = columnSumMap.get(entry.column());
                columnSumMap.put(entry.column(), (occurance == null)
                        ? entry.value()
                        : entry.value() + occurance);
            }
            if (types.contains(StatisticType.ROW_SUMS)) {
                Double occurance = rowSumMap.get(entry.row());
                rowSumMap.put(entry.row(), (occurance == null)
                        ? entry.value()
                        : entry.value() + occurance);
            }
            if (types.contains(StatisticType.MATRIX_COUNTS))
                matrixCount++;
            if (types.contains(StatisticType.MATRIX_SUM))
                matrixSum += entry.value();
        }

        // Convert the maps to arrays.
        double[] rowSums = extractValues(rowSumMap, numRows);
        double[] columnSums = extractValues(columnSumMap, numColumns);
        int[] rowCounts = extractValues(rowCountMap, numRows);
        int[] columnCounts = extractValues(columnCountMap, numColumns);
        return new MatrixStatistics(rowSums, columnSums,
                                    rowCounts, columnCounts,
                                    matrixSum, matrixCount);
    }

    /**
     * Extracts the values from the given map into an array form.  This is
     * neccesary since {@code toArray} on a {@link IntegerMap} does not work
     * with primitives and {@code Map} does not provide this functionality.
     * Each key in the map corresponds to an index in the array being
     * created and the value is the value in stored at the specified index.
     */
    private static double[] extractValues(
            Map<Integer, Double> map, int size)  {
        double[] values = new double[size];
        for (Map.Entry<Integer, Double> entry : map.entrySet()) {
            if (entry.getKey() > values.length)
                throw new IllegalArgumentException(
                        "Array size is too small for values in the " +
                        "given map");
            values[entry.getKey()] = entry.getValue();
        }
        return values;
    }
 
    /**
     * Extracts the values from the given map into an array form.  This is
     * neccesary since {@code toArray} on a {@link IntegerMap} does not work
     * with primitives and {@code Map} does not provide this functionality.
     * Each key in the map corresponds to an index in the array being
     * created and the value is the value in stored at the specified index.
     */
    private static int[] extractValues(
            Map<Integer, Integer> map, int size)  {
        int[] values = new int[size];
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (entry.getKey() > values.length)
                throw new IllegalArgumentException(
                        "Array size is too small for values in the " +
                        "given map");
            values[entry.getKey()] = entry.getValue();
        }
        return values;
    }

    /**
     * A struct recording the row, column, and matrix summations as doubles.
     */
    public static class MatrixStatistics {
        public double[] rowSums;
        public double[] columnSums;
        public int[] rowCounts;
        public int[] columnCounts;
        public double matrixSum;
        public int matrixCounts;

        /**
         * Creates a new {@link MatrixStatistics} instance using the given
         * double values.
         */
        public MatrixStatistics(double[] rowSums,
                                double[] columnSums,
                                int[] rowCounts,
                                int[] columnCounts,
                                double matrixSum, 
                                int matrixCounts) {
            this.rowSums = rowSums;
            this.columnSums = columnSums;
            this.rowCounts = rowCounts;
            this.columnCounts = columnCounts;
            this.matrixSum = matrixSum;
            this.matrixCounts = matrixCounts;
        }
    }
}
