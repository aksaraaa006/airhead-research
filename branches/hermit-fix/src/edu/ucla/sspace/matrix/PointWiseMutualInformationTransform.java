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
public class PointWiseMutualInformationTransform implements AltTransform {

    /**
     * The total sum of occurances for each item (row).
     */
    private double[] itemCounts;

    /**
     * The total sum of occurances for each feature (column).
     */
    private double[] featureCounts;

    /**
     * Creates an instance of {@code PointWiseMutualInformationTransform} from a
     * given {@link Matrix}.
     */
    public PointWiseMutualInformationTransform(Matrix matrix) {
        // Initialize the statistics.
        itemCounts = new double[matrix.rows()];
        featureCounts = new double[matrix.columns()];

        if (matrix instanceof SparseMatrix) {
            // Special case for sparse matrices so that only non zero values are
            // traversed.
            SparseMatrix smatrix = (SparseMatrix) matrix;

            // Compute the feature and item sums. 
            for (int item = 0; item < matrix.rows(); ++item) {
                SparseDoubleVector itemVec = smatrix.getRowVector(item);
                int[] nonZeros = itemVec.getNonZeroIndices();
                for (int index : nonZeros) {
                    double value = itemVec.get(index);
                    itemCounts[item] += value;
                    featureCounts[index] += value;
                }
            }
        } else {
            // Compute the feature and item sums by iterating over all values in
            // the matrix.
            for (int item = 0; item < matrix.rows(); ++item) {
                for (int feature = 0; feature < matrix.columns(); ++feature) {
                    double value = matrix.get(item, feature);
                    itemCounts[item] += value;
                    featureCounts[feature] += value;
                }
            }
        }
    }

    /**
     * Creates an instance of {@code PointWiseMutualInformationTransform} from a
     * matrix {@code File} of format {@code format}.
     */
    public PointWiseMutualInformationTransform(File inputMatrixFile,
                                               Format format) {
        // Initialize the statistics.
        int numColumns = 0;
        int numRows = 0;
        Map<Integer, Double> itemCountMap = new IntegerMap<Double>();
        Map<Integer, Double> featureCountMap = new IntegerMap<Double>();

        // Get an iterator for the matrix file.
        Iterator<MatrixEntry> iter = MatrixIO.iterate(inputMatrixFile, format);

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
            Double occurance = itemCountMap.get(entry.row());
            itemCountMap.put(entry.row(), (occurance == null)
                    ? entry.value()
                    : occurance + entry.value());

            // Gather the column sums.
            occurance = featureCountMap.get(entry.column());
            featureCountMap.put(entry.column(), (occurance == null)
                    ? entry.value()
                    : occurance + entry.value());
        }

        // Convert the maps to arrays.
        itemCounts = extractValues(itemCountMap, numRows);
        featureCounts = extractValues(featureCountMap, numColumns);
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
     * Computes the point wise-mutual information between the {@code item} and
     * {@code feature} with {@code value} specifying the number of occurances of
     * {@code item} with {@code feature}.   This is approximated based on the
     * occurance counts for each {@code item} and {@code feature}.
     *
     * @param item The index specifying the item being observed
     * @param feature The index specifying the feature being observed
     * @param value The number of ocurrances of item and feature together
     *
     * @return log(value) / (itemSum[item] * featureSum[feature])
     */
    public double transform(int item, int feature, double value) {
        return Math.log(value) / (itemCounts[item] * featureCounts[feature]);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "PMI";
    }
}
