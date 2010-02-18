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

import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.Assignment;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.Misc;
import edu.ucla.sspace.util.SortedMultiMap;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * An {@link Transform} that computes the weight of each entry in a matrix based
 * on the clustering of each row in the matrix.  This is done by first computing
 * the k clustering for all rows in the matrix.  Then, for each row, the weight
 * of each non zero entry in the matrix is the summation of similarity values to
 * of rows in the same cluster that have a non zero entry at the same column.
 * This is an alteration of the of the feature weight definition defined in the
 * following paper:
 *
 * <li style="font-family:Garamond, Georgia, serif">M. Zhitomirsky-Geffet and I.
 *     Dagan. (2001). "Bootstrapping Distributional Feature Vector Quality".
 *    <i>Association for Computational Linguistics</i>.
 * </li>
 *
 * @author Keith Stevens
 */
public class SemanticClusterTransform implements Transform {

    /**
     * The prefix for all properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.matrix.SemanticClusterTransform";

    /**
     * The property for setting the {@link Clustering} method to use.
     */
    public static final String CLUSTERING_PROPERTY =
        PROPERTY_PREFIX + ".clustering";

    /**
     * The property for setting the similarity type to use.
     */
    public static final String SIM_TYPE_PROPERTY =
        PROPERTY_PREFIX + ".simType";

    /**
     * The default {@link Clustering} method to use.
     */
    public static final String DEFAULT_CLUSTERING =
        "edu.ucla.sspace.clustering.ClutoClustering";

    /**
     * The default similarity type to use.
     */
    public static final String DEFAULT_SIM_TYPE =
        "COSINE";

    /**
     * The {@link Clustering} method to use for each matrix.
     */
    private final Clustering clustering;

    /**
     * The similarity type to use when computing the pairwise similarities
     * between cluster items.
     */
    private final SimType simType;

    /**
     * Creates a new {@link semanticClusterTransform} using default options.
     */
    public SemanticClusterTransform() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@link semanticClusterTransform} using the given set of
     * {@code Properties}.
     */
    public SemanticClusterTransform(Properties props) {
        clustering = (Clustering) Misc.getObjectInstance(
                props.getProperty(CLUSTERING_PROPERTY, DEFAULT_CLUSTERING));
        simType = SimType.valueOf(
                props.getProperty(SIM_TYPE_PROPERTY, DEFAULT_SIM_TYPE));
    }

    /**
     * {@inheritDoc}
     */
    public File transform(File inputMatrixFile, 
                          MatrixIO.Format format) throws IOException {
        File outputFile = File.createTempFile("Transformed-matrix-file", "dat");
        transform(inputMatrixFile, format, outputFile);
        return outputFile;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public Matrix transform(Matrix input) {
        int cols = input.columns();
        int rows = input.rows();

        // Cluster the elements in the matrix.
        Assignment[] assignments =
            clustering.cluster(input, System.getProperties());

        // Split up the items into their clusters.
        List<Cluster> clusters = new ArrayList<Cluster>();
        for (int item = 0; item < assignments.length; ++item) {
            int[] itemAssignments = assignments[item].assignments();
            if (itemAssignments.length == 0)
                continue;

            int assignment = itemAssignments[0];
            for (int size = assignment; size <= clusters.size(); ++size)
                clusters.add(new Cluster(input));
            clusters.get(assignment).addItem(item);
        }

        // Compute the pair-wise similarities for each cluster.
        for (Cluster cluster : clusters)
            cluster.computeSimilarities();

        Matrix transformed = null;

        // Special case for sparse matrices.
        if (input instanceof SparseMatrix) {
            transformed = Matrices.create(input.rows(), input.columns(), 
                                          Matrix.Type.SPARSE_IN_MEMORY);

            SparseMatrix smat = (SparseMatrix) input;

            // Iterate over each cluster to compute the feature over lap between
            // each item in the cluster.
            for (Cluster cluster : clusters) {
                // Iterate over each item in a specific cluster to find it's
                // overlap with features in other vectors in the same cluster.
                for (Integer row : cluster.cluster) {

                    // Get the non zero values.
                    SparseDoubleVector rowVector = smat.getRowVector(row);
                    int[] nonZeros = rowVector.getNonZeroIndices();

                    // Compute the new weight for each feature to be the
                    // summation of similarity scores of rows that have the same
                    // feature.
                    for (int col : nonZeros) {
                        double newValue = 0;

                        // Iterate over every other item in the cluster.
                        for (Integer other : cluster.cluster) {
                            // Ignore the row if it's the same a the one being
                            // updated.
                            if (row == other)
                                continue;

                            // Ignore rows that do not share the same feature.
                            if (input.get(row, col) == 0d)
                                continue;

                            // Increase the new value.
                            newValue += cluster.similarities.get(row, other);
                        }

                        // Set the new value.
                        transformed.set(row, col, newValue);
                    }
                }
            }
        } else {
            transformed = Matrices.create(input.rows(), input.columns(), 
                                          Matrix.Type.DENSE_IN_MEMORY);
 
            // Iterate over each cluster to compute the feature over lap between
            // each item in the cluster.
            for (Cluster cluster : clusters) {
                // Iterate over each item in a specific cluster to find it's
                // overlap with features in other vectors in the same cluster.
                for (Integer row : cluster.cluster) {

                    // Compute the new weight for each feature to be the
                    // summation of similarity scores of rows that have the same
                    // feature.
                    for (int col = 0; col < cols; ++col) {
                        // Ignore features that have a zero value for this row.
                        if (input.get(row, col) == 0d)
                            continue;

                        double newValue = 0;

                        // Iterate over every other item in the cluster.
                        for (Integer other : cluster.cluster) {
                            // Ignore the row if it's the same a the one being
                            // updated.
                            if (row == other)
                                continue;

                            // Ignore rows that do not share the same feature.
                            if (input.get(row, col) == 0d)
                                continue;

                            // Increase the new value.
                            newValue += cluster.similarities.get(row, other);
                        }

                        // Set the new value.
                        transformed.set(row, col, newValue);
                    }
                }
            }
        }

        return transformed;
    }

    /**
     * A private internal class that represents a single cluster and the
     * pair-wise similarities between each value in the matrix.
     */
    private class Cluster {

        /**
         * The list of row indices in this cluster.
         */
        public List<Integer> cluster;

        /**
         * The set of pair-wise similarities for items in this cluster.
         */
        public Matrix similarities;

        /**
         * The original matrix to use for generating the pair-wise similarities.
         */
        public Matrix original;

        /**
         * Creates a new {@link Cluster} with an empty list of clusters.
         */
        public Cluster(Matrix original) {
            this.cluster = new ArrayList<Integer>();
            this.original = original;
        }

        /**
         * Adds an item to this cluster.
         */
        public void addItem(int item) {
            cluster.add(item);
        }

        /**
         * Computes the pair-wise similarities between each item in this
         * cluster.  Note that this approach assumes that the similarity method
         * used is symmetric.
         */
        public void computeSimilarities() {
            // Only compute the similarity for each pair once.
            similarities = new SymmetricMatrix(cluster.size());
            for (int i = 0; i < cluster.size(); ++i) {
                DoubleVector rowVector = original.getRowVector(i);
                for (int j = i+1; j < cluster.size(); ++j) {
                    DoubleVector otherVector = original.getRowVector(j);
                    similarities.set(i, j, Similarity.getSimilarity(
                                simType, rowVector, otherVector));
                }
            }
        }
    }
}
