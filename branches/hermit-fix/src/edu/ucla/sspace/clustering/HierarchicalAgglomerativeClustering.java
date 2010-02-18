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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility class for performing <a
 * href="http://en.wikipedia.org/wiki/Cluster_analysis#Agglomerative_hierarchical_clustering">Hierarchical
 * Agglomerative Clustering</a> on matrix data in a file.
 *
 * </p>
 *
 * This class provides static accessors to several variations of agglomerative
 * clustering and conforms to the {@link Clustering} interface, which
 * allows this method to be used in place of other clustering algorithms.
 *
 * </p>
 *
 * The following properties are provided:
 *
 * <dl style="margin-left: 1em">
 * <dt> <i>Property:</i> <code><b>{@value #CLUSTER_SIMILARITY_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_CLUSTER_SIMILARITY_PROPERTY }
 *
 * <dd style="padding-top: .5em"> This property specifies the cluster similarity
 * threshold at which two clusters are merged together.  Merging will continue
 * until either all clusters have similarities below this threshold or the
 * number of desired clusters has been reached. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #CLUSTER_LINKAGE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_CLUSTER_LINKAGE_PROPERTY}
 *
 * <dd style="padding-top: .5em"> This property specifies the {@link
 * ClusterLinkage} to use when computing cluster similarity. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #SIMILARITY_FUNCTION_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_SIMILARITY_FUNCTION_PROPERTY}
 *
 * <dd style="padding-top: .5em"> This property specifies the {@link SimType} to
 * use when comparing the similarity between data points. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #NUM_CLUSTER_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_NUM_CLUSTER_PROPERTY}
 *
 * <dd style="padding-top: .5em"> The desired number of clusters. </p>
 *
 * </dl>
 *
 * @author David Jurgens
 */
public class HierarchicalAgglomerativeClustering implements Clustering {

    /**
     * The method to use when comparing the similarity of two clusters.  See <a
     * href="http://home.dei.polimi.it/matteucc/Clustering/tutorial_html/hierarchical.html">
     * here </a> for an example of how the different linkages operate.
     */
    public enum ClusterLinkage { 
        /**
         * Clusters will be compared based on the most similar link between
         * them.
         */
        SINGLE_LINKAGE, 

        /**
         * Clusters will be compared based on the total similarity of all
         * pair-wise comparisons of the data points in each.
         */
        COMPLETE_LINKAGE, 

        /**
         * Clusters will be compared using the similarity of the computed mean
         * data point (or <i>centroid</i>) for each cluster.  This comparison
         * method is also known as UPGMA.
         */
        MEAN_LINKAGE,
        
        /**
         * Clusters will be compared using the similarity of the computed
         * median data point for each cluster
         */
        MEDIAN_LINKAGE 
    }

    /**
     * A prefix for specifying properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering";

    /**
     * The property for specifying the cluster similarity threshold.
     */
    public static final String CLUSTER_SIMILARITY_PROPERTY =
        PROPERTY_PREFIX + ".clusterThreshold";

    /**
     * The property for specifying the cluster linkage to use.
     */
    public static final String CLUSTER_LINKAGE_PROPERTY =
        PROPERTY_PREFIX + ".clusterLinkage";

    /**
     * The property for specifying the similarity function to use.
     */
    public static final String SIMILARITY_FUNCTION_PROPERTY =
        PROPERTY_PREFIX + ".simFunc";

    /**
     * The default similarity threshold to use.
     */
    private static final String DEFAULT_CLUSTER_SIMILARITY_PROPERTY = "-1";

    /**
     * The default linkage method to use.
     */
    private static final String DEFAULT_CLUSTER_LINKAGE_PROPERTY =
        "COMPLETE_LINKAGE";

    /**
     * The default similarity function to use.
     */
    private static final String DEFAULT_SIMILARITY_FUNCTION_PROPERTY =
        "cosineSimilarity";

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(HierarchicalAgglomerativeClustering.class.getName());

    /**
     * {@inheritDoc}
     */
    public Assignment[] cluster(Matrix matrix, Properties props) {
        if (props.getProperty(CLUSTER_SIMILARITY_PROPERTY) == null)
            throw new IllegalArgumentException("The number of clusters or a " +
                    "threshold must be specified.");

        return cluster(matrix, -1, props);
    }

    /**
     * {@inheritDoc}
     */
    public Assignment[] cluster(Matrix m,
                                int numClusters,
                                Properties props) {
        double clusterSimilarityThreshold =
            Double.parseDouble(props.getProperty(
                        CLUSTER_SIMILARITY_PROPERTY,
                        DEFAULT_CLUSTER_SIMILARITY_PROPERTY));

        ClusterLinkage linkage = ClusterLinkage.valueOf(props.getProperty(
                    CLUSTER_LINKAGE_PROPERTY,
                    DEFAULT_CLUSTER_LINKAGE_PROPERTY));

        SimType similarityFunction = SimType.valueOf(props.getProperty(
                    SIMILARITY_FUNCTION_PROPERTY,
                    DEFAULT_SIMILARITY_FUNCTION_PROPERTY));
        int[] rawAssignments = cluster(m, clusterSimilarityThreshold, linkage,
                                       similarityFunction, numClusters);

        Assignment[] assignments = new Assignment[rawAssignments.length];
        for (int i = 0; i < rawAssignments.length; ++i)
            assignments[i] = new HardAssignment(rawAssignments[i]);
        return assignments;
    }

    /**
     * Clusters all rows in the matrix using the specified cluster similarity
     * measure for comparison and stopping when the number of clusters is equal
     * to the specified number.
     *
     * @param m a matrix whose rows are to be clustered
     * @param numClusters the number of clusters into which the matrix should
     *        divided
     * @param linkage the method to use for computing the similarity of two
     *        clusters
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.
     */
    public static int[] partitionRows(Matrix m, int numClusters,
                                      ClusterLinkage linkage,
                                      SimType similarityFunction) {
        return cluster(m, -1, linkage, similarityFunction, numClusters);
    }

    /**
     * Clusters all rows in the matrix using the specified cluster similarity
     * measure for comparison and threshold for when to stop clustering.
     * Clusters will be repeatedly merged until the highest cluster similarity
     * is below the threshold.
     *
     * @param m a matrix whose rows are to be clustered
     * @param clusterSimilarityThreshold the threshold to use when deciding
     *        whether two clusters should be merged.  If the similarity of the
     *        clusters is below this threshold, they will not be merged and the
     *        clustering process will be stopped.
     * @param linkage the method to use for computing the similarity of two
     *        clusters
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.
     */
    @SuppressWarnings("unchecked")
    public static int[] clusterRows(Matrix m, double clusterSimilarityThreshold,
                                    ClusterLinkage linkage,
                                    SimType similarityFunction) {
        return cluster(m, clusterSimilarityThreshold, linkage, 
                       similarityFunction, -1);
    }

    /**
     * 
     *
     * @param m a matrix whose rows are to be clustered
     * @param clusterSimilarityThreshold the optional parameter for specifying
     *        the minimum inter-cluster similarity to use when deciding whether
     *        two clusters should be merged.  If {@code maxNumberOfClusters} is
     *        positive, this value is discarded in order to cluster to a fixed
     *        number.  Otherwise all clusters will be merged until the minimum
     *        distance is less than this threshold.
     * @param linkage the method to use for computing the similarity of two
     *        clusters
     * @param maxNumberOfClusters an optional parameter to specify the maximum
     *        number of clusters to have.  If this value is non-positive,
     *        clusters will be merged until the inter-cluster similarity is
     *        below the threshold, otherwise; if the value is positive, clusters
     *        are merged until the desired number of clusters has been reached.
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.
     */
    private static int[] cluster(Matrix m, double clusterSimilarityThreshold,
                                 ClusterLinkage linkage, 
                                 SimType similarityFunction,
                                 int maxNumberOfClusters) {
        int rows = m.rows();
        LOGGER.info("Generating similarity matrix for " + rows+ " data points");
        Matrix similarityMatrix = 
            computeSimilarityMatrix(m, similarityFunction);

        // Keep track of which rows in the matrix have not been clusted
        Set<Integer> notClustered = new LinkedHashSet<Integer>();
        for (int i = 0; i < rows; ++i)
            notClustered.add(i);

        // Create the initial set of clusters where each row is originally in
        // its own cluster
        Map<Integer,Set<Integer>> clusterAssignment = 
            generateInitialAssignment(rows);

        LOGGER.info("Calculating initial inter-cluster similarity using " 
                    + linkage);
        // Generate the initial set of cluster pairings based on the highest
        // similarity.  This mapping will be update as the number of clusters
        // are reduced, where merging a cluster will causes all the pairings
        // pointing to it constinuents recalculated.
        Map<Integer,Pairing> clusterSimilarities =
            new HashMap<Integer,Pairing>();
        for (Integer clusterId : clusterAssignment.keySet()) {
            clusterSimilarities.put(clusterId,
                 findMostSimilar(clusterAssignment, clusterId, 
                                 linkage, similarityMatrix));
        }

        LOGGER.info("Assigning clusters");

        // Keep track of which ID is available for the new, merged cluster
        int nextClusterId = rows;

        // While we still have more clusters than the maximum number loop.  Note
        // that if the maximum was set to negative, this condition will always
        // be true and the inner loop check for inter-cluster similarity will
        // break out of this loop
        while (clusterAssignment.size() > maxNumberOfClusters) {
            // Find a row that has yet to be clustered by searching for the pair
            // that is most similar
            int cluster1index = 0;
            int cluster2index = 0;
            double highestSimilarity = -1;

            // Find the row with the highest similarity to another            
            for (Map.Entry<Integer,Pairing> e : 
                     clusterSimilarities.entrySet()) {

                Pairing p = e.getValue();
                Integer i = e.getKey();
                Integer j = p.pairedIndex;
                if (p.similarity > highestSimilarity) {
                    cluster1index = i;
                    cluster2index = j;
                    highestSimilarity = p.similarity;
                }
            }            
            
            // If the similarity of the two most similar clusters falls below
            // the threshold, then the final set of clusters has been
            // determined.
            if (maxNumberOfClusters < 1 &&
                highestSimilarity < clusterSimilarityThreshold)
                break;
            
            // Assign the merged cluster a new ID, which lets us track any
            // pairings to the original clusters that may need to be
            // recalculated
            int newClusterId = nextClusterId++;

            Set<Integer> cluster1 = clusterAssignment.get(cluster1index);
            Set<Integer> cluster2 = clusterAssignment.get(cluster2index);

            LOGGER.log(Level.FINE, "Merged cluster {0} with {1}",
                       new Object[] { cluster1, cluster2 });

            // Update the cluster assignments, adding in the new cluster and
            // remove all references to the two merged clusters.
            cluster1.addAll(cluster2);
            clusterAssignment.put(newClusterId, cluster1);
            clusterAssignment.remove(cluster1index);
            clusterAssignment.remove(cluster2index);
            clusterSimilarities.remove(cluster1index);
            clusterSimilarities.remove(cluster2index);

            // Local state variables to use while recalculating the similarities
            double mostSimilarToMerged = -1;
            Integer mostSimilarToMergedId = null;

            // Check whether we have just merged the last two clusters, in which
            // case the similarity recalculation is unnecessary
            if (clusterSimilarities.isEmpty())
                break;

            // Recalculate the inter-cluster similarity of a cluster that was
            // paired with either of these two (i.e. was most similar to one of
            // them before the merge).  At the same time, calculate the
            // most-similar to the newly merged cluster
            for (Map.Entry<Integer,Pairing> e : 
                     clusterSimilarities.entrySet()) {

                Integer clusterId = e.getKey();

                // First, calculate the similarity between this cluster and the
                // newly merged cluster
                double simToNewCluster = 
                    getSimilarity(similarityMatrix, cluster1,
                                  clusterAssignment.get(clusterId), linkage);
                if (simToNewCluster > mostSimilarToMerged) {
                    mostSimilarToMerged = simToNewCluster;
                    mostSimilarToMergedId = clusterId;
                }

                // Second, if the pair was previously paired with one of the
                // merged clusters, recompute what its most similar is
                Pairing p = e.getValue();
                if (p.pairedIndex == cluster1index 
                        || p.pairedIndex == cluster2index) {
                    // Reassign with the new most-similar
                    e.setValue(findMostSimilar(clusterAssignment, clusterId, 
                                               linkage, similarityMatrix));
                }
            }
            
            // Update the new most similar to the newly-merged cluster
            clusterSimilarities.put(newClusterId, 
                                    new Pairing(mostSimilarToMerged, 
                                                mostSimilarToMergedId));
        }

        return toAssignArray(clusterAssignment, rows);
    }

    /**
     * For the current cluster, finds the most similar cluster using the
     * provided linkage method and returns the pairing for it.
     */
    private static Pairing findMostSimilar(
            Map<Integer,Set<Integer>> curAssignment, int curCluster, 
            ClusterLinkage linkage,  Matrix similarityMatrix) {
        // Start with with the most similar being set to the newly merged
        // cluster, as this value has already been computed
        double mostSimilar = -1;
        Integer paired = -1;
        for (Integer otherId : curAssignment.keySet()) {
            if (otherId.equals(curCluster))
                continue;
            double similarity = getSimilarity(
                similarityMatrix, curAssignment.get(curCluster),
                    curAssignment.get(otherId), linkage);
            if (similarity > mostSimilar) {
                mostSimilar = similarity;
                paired = otherId;
            }
        }
        return new Pairing(mostSimilar, paired);
    }

    /**
     * Returns the final mapping of data points as an array where each row is
     * assigned to a single cluster value from 0 to <i>n</u>, the number of
     * clusters.
     *
     * @param assignment a mapping from cluster number to the data points (rows)
     *        that are contained in it
     * @param p the number of initial data points
     *
     * @return the cluster assignment
     */
    private static int[] toAssignArray(Map<Integer,Set<Integer>> assignment, 
                                       int numDataPoints) {
        int[] clusters = new int[numDataPoints];
        for (int i = 0; i < numDataPoints; ++i)
            clusters[i] = -1;
        int clusterIndex = 0;
        for (Set<Integer> cluster : assignment.values()) {
            // Decide whether this cluster has already been assigned by picking
            // out the first element in the cluster and seeing if it has the
            // dummy cluster value (-1)
            int r = cluster.iterator().next();
            if (clusters[r] != -1)
                continue;
            // Otherwise the row this cluster needs to be assigned a cluster
            // index
            for (int row : cluster) 
                clusters[row] = clusterIndex;
            // Increment the cluster index for the next cluster
            clusterIndex++;
        }
        LOGGER.info("total number of clusters: " + clusterIndex);
        return clusters;
    }

    /**
     *
     * @param numDataPoints the number of initial data points
     */
    private static Map<Integer,Set<Integer>> generateInitialAssignment(
        int numDataPoints) {
        Map<Integer,Set<Integer>> clusterAssignment = 
            new HashMap<Integer,Set<Integer>>(numDataPoints);
        for (int i = 0; i < numDataPoints; ++i) {
            HashSet<Integer> cluster=  new HashSet<Integer>();
            cluster.add(i);
            clusterAssignment.put(i, cluster);
        }
        return clusterAssignment;
    }

    /**
     * Computes and returns the similarity matrix for {@code m} using the
     * specified similarity function
     */
    private static Matrix computeSimilarityMatrix(Matrix m, 
                                                  SimType similarityFunction) {
        Matrix similarityMatrix = 
            Matrices.create(m.rows(), m.rows(), Matrix.Type.DENSE_ON_DISK);
        for (int i = 0; i < m.rows(); ++i) {
            for (int j = i + 1; j < m.rows(); ++j) {
                double similarity = 
                    Similarity.getSimilarity(similarityFunction,
                                             m.getRowVector(i),
                                             m.getRowVector(j));
                similarityMatrix.set(i, j, similarity);
                similarityMatrix.set(j, i, similarity);
            }
        }
        return similarityMatrix;
    }

    /**
     * Returns the similarity of two clusters according the specified linkage
     * function.
     * 
     * @param similarityMatrix a matrix containing pair-wise similarity of each
     *        data point in the entire set
     * @param cluster the first cluster to be considered
     * @param toAdd the second cluster to be considered
     * @param linkage the method by which the similarity of the two clusters
     *        should be computed
     *
     * @return the similarity of the two clusters
     */
    private static double getSimilarity(Matrix similarityMatrix,
                                        Set<Integer> cluster, 
                                        Set<Integer> toAdd,
                                        ClusterLinkage linkage) {
        double similarity = -1;
        switch (linkage) {
        case SINGLE_LINKAGE: {
            double highestSimilarity = -1;
            for (int i : cluster) {
                for (int j : toAdd) {
                    double s = similarityMatrix.get(i, j);
                    if (s > highestSimilarity)
                        highestSimilarity = s;
                }
            }
            similarity = highestSimilarity;
            break;
        }

        case COMPLETE_LINKAGE: {
            double lowestSimilarity = 1;
            for (int i : cluster) {
                for (int j : toAdd) {
                    double s = similarityMatrix.get(i, j);
                    if (s < lowestSimilarity)
                        lowestSimilarity = s;
                }
            }
            similarity = lowestSimilarity;
            break;
        }

        case MEAN_LINKAGE: {
            double similaritySum = 0;
            for (int i : cluster) {
                for (int j : toAdd) {
                    similaritySum += similarityMatrix.get(i, j);
                }
            }
            similarity = similaritySum / (cluster.size() * toAdd.size());
            break;
        }

        case MEDIAN_LINKAGE: {
            double[] similarities = new double[cluster.size() * toAdd.size()];
            int index = 0;
            for (int i : cluster) {
                for (int j : toAdd) {
                    similarities[index++] = similarityMatrix.get(i, j);
                }
            }
            Arrays.sort(similarities);
            similarity = similarities[similarities.length / 2];
            break;
        }
        
        default:
            assert false : "unknown linkage method";
        }
        return similarity;
    }

    /**
     * A utility structure for holding the assignment of a cluster to another
     * cluster by means of a high similarity.
     */
    private static class Pairing implements Comparable<Pairing> {
        
        /**
         * The similarity of the other cluster to the cluster indicated by
         * {@code pairedIndex}
         */
        public final double similarity;

        /**
         * The index of the cluster that is paired
         */
        public final int pairedIndex;

        public Pairing(double similarity, int pairedIndex) {
            this.similarity = similarity;
            this.pairedIndex = pairedIndex;
        }

        public int compareTo(Pairing p) {
            return (int)((p.similarity - similarity) * Integer.MAX_VALUE);
        }

        public boolean equals(Object o) {
            return (o instanceof Pairing)
                && ((Pairing)o).pairedIndex == pairedIndex;
        }
        
        public int hashCode() {
            return pairedIndex;
        }        
    }
}
