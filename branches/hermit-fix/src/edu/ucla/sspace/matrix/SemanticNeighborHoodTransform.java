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

import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.IntegerMap;
import edu.ucla.sspace.util.SortedMultiMap;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;


/**
 * An {@link Transform} that computes the weight of each entry in a matrix based
 * on the semantic neighborhood of each row.  This is done by first computing
 * the N most similar rows to each row.  Then, for each row, the weight of each
 * non zero entry in the matrix is the summation of similarity values to the
 * most similar rows that have a non zero entry at the same column.  This is an
 * implementation of the feature weight definition defined in the following
 * paper:
 *
 * <li style="font-family:Garamond, Georgia, serif">M. Zhitomirsky-Geffet and I.
 *     Dagan. (2001). "Bootstrapping Distributional Feature Vector Quality".
 *    <i>Association for Computational Linguistics</i>.
 * </li>
 *
 * @author Keith Stevens
 */
public class SemanticNeighborHoodTransform implements Transform {

    /**
     * The property prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.matrix.SemanticNeighborHoodTransform";

    /**
     * The property for setting the number of similar rows to consider part of a
     * neighboorhood.
     */
    public static final String NUM_SIMILAR_PROPERTY =
        PROPERTY_PREFIX + ".numSimilar";

    /**
     * The default number of similar rows to include in the neighboorhood.
     */
    public static final String DEFAULT_NUM_SIMILAR = "10";

    /**
     * The number of rows to include when building the list of most similar rows
     * for each row.
     */
    private final int numSimilar;

    /**
     * Creates a new {@link SemanticNeighborHoodTransform} with default values.
     */
    public SemanticNeighborHoodTransform() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@link SemanticNeighborHoodTransform} with the provided
     * properties.
     */
    public SemanticNeighborHoodTransform(Properties props) {
        numSimilar = Integer.parseInt(props.getProperty(
                    NUM_SIMILAR_PROPERTY, DEFAULT_NUM_SIMILAR));
    }

    /**
     * {@inheritDoc}.
     */
    public File transform(File inputMatrixFile, 
                          MatrixIO.Format format) throws IOException {
        File outputFile = File.createTempFile("Transformed-matrix-file", "dat");
        transform(inputMatrixFile, format, outputFile);
        return outputFile;
    }

    /**
     * {@inheritDoc}.
     */
    public void transform(File inputMatrixFile,
                          MatrixIO.Format inputFormat, 
                          File outputMatrixFile) throws IOException {
        Matrix m = null;
        MatrixIO.readMatrix(inputMatrixFile, inputFormat);
        m = transform(m);
        MatrixIO.writeMatrix(m, outputMatrixFile, inputFormat);
    }

    /**
     * {@inheritDoc}.
     */
    public Matrix transform(Matrix input) {
        // Setup the list of most similar rows that will be sorted based on
        // their row similarity.
        ArrayList<SortedMultiMap<Double, Integer>> mostSimilarPerRow = new
            ArrayList<SortedMultiMap<Double, Integer>>(input.rows());
        for (int i = 0; i < input.rows(); ++i)
            mostSimilarPerRow.add(
                    new BoundedSortedMultiMap<Double, Integer>(numSimilar));

        // Compute the most similar rows for each row using the cosine
        // similarity.
        RowComparator comparator = new RowComparator();
        for (int i = 0; i < input.rows(); ++i)
            comparator.getMostSimilar(i, input, mostSimilarPerRow,
                                      SimType.COSINE);

        Matrix transformed = null;

        // Special case for sparse matrices.
        if (input instanceof SparseMatrix) {
            transformed = Matrices.create(input.rows(), input.columns(), 
                                          Matrix.Type.SPARSE_IN_MEMORY);
            SparseMatrix smat = (SparseMatrix) input;
            for (int row = 0; row < input.rows(); ++row) {
                // Transform the most similar list into a mapping from row index
                // to similarity score.
                SortedMultiMap<Double,Integer> mostSimilar =
                    mostSimilarPerRow.get(row);
                Map<Integer, Double> neighbors = new IntegerMap<Double>();
                for (Map.Entry<Double, Integer> entry : mostSimilar.entrySet())
                    neighbors.put(entry.getValue(), entry.getKey());

                // Compute the new transformed values for each feature in the
                // row according to the feature overlap in most similar rows.
                int[] nonZeros = smat.getRowVector(row).getNonZeroIndices();
                for (int index : nonZeros) {
                    // Compute the new weight of the value at row,index by
                    // summing the similarity values of all the neighbors which
                    // have a non zero value for feature index.
                    double newWeight = 0;
                    for (Map.Entry<Integer, Double> other :
                            neighbors.entrySet()) {
                        double otherValue = input.get(other.getKey(), index);
                        if (otherValue == 0d)
                            continue;
                        newWeight += other.getValue();
                    }
                    transformed.set(row, index, newWeight);
                }
            }
        } else {
            transformed = Matrices.create(input.rows(), input.columns(), 
                                          Matrix.Type.SPARSE_IN_MEMORY);
            for (int row = 0; row < input.rows(); ++row) {
                // Transform the most similar list into a mapping from row index
                // to similarity score.
                SortedMultiMap<Double,Integer> mostSimilar =
                    mostSimilarPerRow.get(row);
                Map<Integer, Double> neighbors = new IntegerMap<Double>();
                for (Map.Entry<Double, Integer> entry : mostSimilar.entrySet())
                    neighbors.put(entry.getValue(), entry.getKey());

                // Compute the new transformed values for each feature in the
                // row according to the feature overlap in most similar rows.
                for (int col = 0; col < input.columns(); ++col) {
                    // Skip columns that have a zero value.
                    if (input.get(row, col) == 0d)
                        continue;

                    // Compute the new weight of the value at row,index by
                    // summing the similarity values of all the neighbors which
                    // have a non zero value for feature index.
                    double newWeight = 0;
                    for (Map.Entry<Integer, Double> other :
                            neighbors.entrySet()) {
                        double otherValue = input.get(other.getKey(), col);
                        if (otherValue == 0d)
                            continue;
                        newWeight += other.getValue();
                    }
                    transformed.set(row, col, newWeight);
                }
            }
        }

        return transformed;
    }
}
