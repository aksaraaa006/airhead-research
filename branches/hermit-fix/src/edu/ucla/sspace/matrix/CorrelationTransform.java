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
 * Transforms a matrix using row correlation weighting.  The input matrix is
 * assumed to be formatted as rows representing terms and columns representing
 * co-occuring terms.  Each matrix cell indicates the number of times the row's
 * word occurs the other term.  See the following paper for details and
 * analysis:
 *
 * <p style="font-family:Garamond, Georgia, serif"> Rohde, D. L. T., Gonnerman,
 * L. M., Plaut, D. C. (2005).  An Improved Model of Semantic Similarity Based
 * on Lexical Co-Occurrence. <i>Cognitive Science</i> <b>(submitted)</b>.
 * Available <a
 * href="http://www.cnbc.cmu.edu/~plaut/papers/pdf/RohdeGonnermanPlautSUB-CogSci.COALS.pdf">here</a></p>

 * @author Keith Stevens
 */
public class CorrelationTransform implements AltTransform {

    /**
     * The summation of the values each row
     */
    private double[] rowSums;

    /**
     * The summation of the values each column 
     */
    private double[] colSums;

    /**
     * The total sum of all values in the matrix.
     */
    private double totalSum;

    /**
     * Creates an instance of {@code CorrelationTransform} from a {@link
     * Matrix}.
     */
    public CorrelationTransform(Matrix matrix) {
        // Initialize the statistics.
        totalSum = 0;
        rowSums = new double[matrix.rows()];
        colSums = new double[matrix.columns()];

        if (matrix instanceof SparseMatrix) {
            // Special case for sparse matrices so that only non zero values are
            // traversed.
            SparseMatrix smatrix = (SparseMatrix) matrix;

            // Compute the total, row and column sums.
            for (int row = 0; row < matrix.rows(); ++row) {
                SparseDoubleVector rowVec = smatrix.getRowVector(row);
                int[] nonZeros = rowVec.getNonZeroIndices();
                for (int index : nonZeros) {
                    double value = rowVec.get(index);
                    colSums[index] += value;
                    rowSums[row] += value;
                    totalSum += value;
                }
            }
        } else {
            // Compute the total, row and column sums over all values in the
            // matrix.
            for (int row = 0; row < matrix.rows(); ++row) {
                for (int column = 0; column < matrix.columns(); ++column) {
                    double value = matrix.get(row, column);
                    rowSums[row] += value;
                    colSums[column] += value;
                    totalSum += value;
                }
            }
        }
    }

    /**
     * Creates an instance of {@code CorrelationTransform} from a {@code
     * File} for format {@link Format}.
     */
    public CorrelationTransform(File inputMatrixFile,
                                Format format) {
        // Initialize the statistics.
        int numColumns = 0;
        int numRows = 0;
        totalSum = 0;
        Map<Integer, Double> rowSumMap = new IntegerMap<Double>();
        Map<Integer, Double> colSumMap = new IntegerMap<Double>();

        // Get an iterator for the matrix file.
        Iterator<MatrixEntry> iter = MatrixIO.iterate(inputMatrixFile, format);
        while (iter.hasNext()) {
            MatrixEntry entry = iter.next();

            // Get the total number of columns and rows.
            if (entry.column() >= numColumns)
                numColumns = entry.column() + 1;
            if (entry.row() >= numRows)
                numRows = entry.row() + 1;

            // Skip entries with non zero values.
            if (entry.value() == 0d)
                continue;

            totalSum += entry.value();

            // Gather the row sums.
            Double occurance = rowSumMap.get(entry.row());
            rowSumMap.put(entry.row(), (occurance == null)
                    ? entry.value()
                    : occurance + entry.value());

            // Gather the column sums.
            occurance = colSumMap.get(entry.column());
            colSumMap.put(entry.column(), (occurance == null)
                    ? entry.value()
                    : occurance + entry.value());

        }

        // Convert the maps to arrays.
        rowSums = extractValues(rowSumMap, numRows);
        colSums = extractValues(colSumMap, numColumns);
    }

    /**
     * Extracts the values from the given map into an array form.  This is
     * neccesary since {@code toArray} on a {@link IntegerMap} does not work
     * with primitives and {@code Map} does not provide this functionality.
     * Each key in the map corresponds to an index in the array being created
     * and the value is the value in stored at the specified index.
     */
    private <T extends Number> double[] extractValues(Map<Integer, T> map,
                                                      int size)  {
        double[] values = new double[size];
        for (Map.Entry<Integer, T> entry : map.entrySet()) {
            if (entry.getKey() > values.length)
                throw new IllegalArgumentException(
                        "Array size is too small for values in the given map");
            values[entry.getKey()] = (double) entry.getKey();
        }
        return values;
    }

    /**
     * Computes the correlation, scaled using the square root, between item
     * {@code row} and feature {@code column} where {@code value} specifies the
     * number of occurances.   If {@code value} is zero, the correlation is
     * zero.
     *
     * @param row The index specifying the item being observed
     * @param column The index specifying the feature being observed
     * @param value The number of occurance of the item and feature
     *
     * @return the square root of the correlation between the item a feature
     */
    public double transform(int row, int column, double value) {
        if (value == 0d)
            return 0;

        double newValue = (totalSum * value - rowSums[row] * colSums[column]) /
            Math.sqrt(rowSums[row] * (totalSum - rowSums[row]) *
                      colSums[column] * (totalSum - colSums[column]));
        return Math.sqrt(newValue);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "Correlation":
    }
}
