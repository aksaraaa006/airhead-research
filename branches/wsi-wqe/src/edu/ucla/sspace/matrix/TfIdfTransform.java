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

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.File;

import java.util.Iterator;
import java.util.Map;


/**
 * Tranforms a matrix according to the <a
 * href="http://en.wikipedia.org/wiki/Tf%E2%80%93idf">Term frequency-Inverse
 * Document Frequency</a> weighting.  The input matrix is assumed to be
 * formatted as rows representing terms and columns representing documents.
 * Each matrix cell indicates the number of times the row's word occurs within
 * the column's document.  For full details see:
 *
 * <ul><li style="font-family:Garamond, Georgia, serif">Spärck Jones, Karen
 *      (1972). "A statistical interpretation of term specificity and its
 *      application in retrieval". <i>Journal of Documentation</i> <b>28</b>
 *      (1): 11–21.</li></ul>
 *
 * @author David Jurgens
 *
 * @see LogEntropyTransform
 */
public class TfIdfTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new TfIdfGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new TfIdfGlobalTransform(inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "TF-IDF";
    }

    public class TfIdfGlobalTransform implements GlobalTransform {

        /**
         * The total number of occurances of each term (row) in the matrix.
         */
        private double[] termOccuranceCount;

        /**
         * The total number of documents (columns) that each row occurs in.
         */
        private double[] termDocCount;

        /**
         * The total number of documents (columns) present in the matrix.
         */
        private int totalDocCount;

        /**
         * Creates an instance of {@code TfIdfGlobalTransform} from a {@link
         * Matrix}.
         */
        public TfIdfGlobalTransform(Matrix matrix) {
            // Initialize the statistics.
            totalDocCount = matrix.columns();
            termOccuranceCount = new double[matrix.rows()];
            termDocCount = new double[matrix.rows()];

            if (matrix instanceof SparseMatrix) {
                // Special case for sparse matrices so that only non zero values
                // will be traversed.
                SparseMatrix smatrix = (SparseMatrix) matrix;

                // Compute the row sums for each row and the number of columns
                // each term occurs in.
                for (int term = 0; term < matrix.rows(); ++term) {
                    SparseDoubleVector termVec = smatrix.getRowVector(term);
                    int[] nonZeros = termVec.getNonZeroIndices();
                    termDocCount[term] = nonZeros.length;
                    for (int index : nonZeros)
                        termOccuranceCount[term] += termVec.get(index);
                }
            } else {
                // Compute the row sums for each row and the number of columns
                // each term occurs in.
                for (int term = 0; term < matrix.rows(); ++term) {
                    for (int doc = 0; doc < matrix.columns(); ++doc) {
                        double value = matrix.get(term, doc);
                        // Only consider non zero entries.
                        if (value != 0d) {
                            termOccuranceCount[term] += value;
                            termDocCount[doc]++;
                        }
                    }
                }
            }
        }
        
        /**
         * Creates an instance of {@code TfIdfGlobalTransform} from a {@code
         * File} in the format {@link Format}.
         */
        public TfIdfGlobalTransform(File inputMatrixFile,
                                    MatrixIO.Format format) {
            // Initialize the statistics.
            totalDocCount = 0;
            int numRows = 0;
            Map<Integer, Integer> termDocMap = new IntegerMap<Integer>();
            Map<Integer, Double> termOccuranceMap = new IntegerMap<Double>();

            // Get an iterator for the matrix file.
            Iterator<MatrixEntry> iter =
                MatrixIO.iterate(inputMatrixFile, format);

            while (iter.hasNext()) {
                MatrixEntry entry = iter.next();

                // Get the total number of columns and rows.
                if (entry.column() >= totalDocCount)
                    totalDocCount = entry.column() + 1;
                if (entry.row() >= numRows)
                    numRows = entry.row() + 1;

                // Skip non zero entries.
                if (entry.value() == 0d)
                    continue;

                // Count the total sum for this term 
                Double occurance = termOccuranceMap.get(entry.row());
                termOccuranceMap.put(entry.row(), (occurance == null)
                        ? entry.value()
                        : occurance + entry.value());

                // Increase the count for this term occurring in a document by
                // one.
                Integer count = termDocMap.get(entry.column());
                termDocMap.put(entry.column(), (count == null)
                        ? 1
                        : count + 1);
            }

            // Convert the maps to arrays.
            termDocCount = extractValues(termDocMap, numRows);
            termOccuranceCount = extractValues(termOccuranceMap, numRows);
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
         * Computes the Term Frequency-Inverse Document Frequency for a given
         * value where {@code value} is the observed frequency of term {@code
         * row} in document {@code column}.
         *
         * @param row The index speicifying the term being observed
         * @param column The index specifying the document being observed
         * @param value The number of occurances of the term in the document.
         *
         * @return the TF-IDF of the observed value
         */
        public double transform(int row, int column, double value) {
            double tf = value / termOccuranceCount[row];
            double idf = Math.log(totalDocCount / (termDocCount[row] + 1));
            return tf * idf;
        }
    }
}
