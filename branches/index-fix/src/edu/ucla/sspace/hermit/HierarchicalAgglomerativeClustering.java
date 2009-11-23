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

package edu.ucla.sspace.hermit;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility class for performing <a
 * href="http://en.wikipedia.org/wiki/Cluster_analysis#Agglomerative_hierarchical_clustering">Hierarchical
 * Agglomerative Clustering</a> on matrix data in a file.
 *
 * @author David Jurgens
 */
public class HierarchicalAgglomerativeClustering {

    /**
     * The method to use when comparing the similarity of two clusters.
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
         * Clusters will be compared using the similarity of the computed
         * mean data point for each cluster
         */
        MEAN_LINKAGE,
        
        /**
         * Clusters will be compared using the similarity of the computed
         * median data point for each cluster
         */
        MEDIAN_LINKAGE 
    }

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(HierarchicalAgglomerativeClustering.class.getName());

    /**
     * Identifies each row in the matrix file as a data point, and then clusters
     * all rows using the specified cluster similarity measure for comparison
     * and threshold to stop clustering.  Clusters will be repeatedly merged
     * until the highest cluster similarity is below the threshold.
     *
     * @param matrixFile a file containing matrix data where each row is a data
     *        point
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
    public static Duple<int[], Vector[]> cluster(
            File contextFile, 
            double clusterSimilarityThreshold,
            ClusterLinkage linkage) {
        LOGGER.info("Reading in data points from file");
        List<Duple<Integer, Vector>> contextInstances = null;

        try {
            ObjectInputStream inStream = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(contextFile)));
            contextInstances = new ArrayList<Duple<Integer, Vector>>();
            Object obj = null;
            try {
                while ((obj = inStream.readObject()) != null)
                    contextInstances.add((Duple<Integer, Vector>) obj);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException("The class does not exist");
            } catch (EOFException eof) {
                // Do nothing.
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        Vector[] contexts = new Vector[contextInstances.size()];
        for (Duple<Integer, Vector> d : contextInstances)
            contexts[d.x] = d.y;

        if (contextInstances.size() == 1)
            return new Duple<int[], Vector[]>(new int[]{0}, contexts);

        int rows = contextInstances.size();
        
        LOGGER.info("Generating similarity matrix of size: " + rows);
        Matrix similarityMatrix = 
            Matrices.create(rows, rows, Matrix.Type.DENSE_IN_MEMORY);
        for (int i = 0; i < rows; ++i) {
            for (int j = i + 1; j < rows; ++j) {
                double similarity = Similarity.cosineSimilarity(
                        contexts[i], contexts[j]);
                similarityMatrix.set(i, j, similarity);
                similarityMatrix.set(j, i, similarity);
            }
        }
        
        contextInstances = null;

        // Keep track of which rows in the matrix have not been clusted
        Set<Integer> notClustered = new LinkedHashSet<Integer>();
        for (int i = 0; i < rows; ++i)
            notClustered.add(i);

        // Create the initial set of clusters where each row is originally in
        // its own cluster
        Map<Integer,Set<Integer>> clusterAssignment =
            new HashMap<Integer,Set<Integer>>(rows);        
        for (int i = 0; i < rows; ++i) {
            HashSet<Integer> cluster= new HashSet<Integer>();
            cluster.add(i);
            clusterAssignment.put(i, cluster);
        }


        LOGGER.info("Calculating initial inter-cluster similarity using " 
                    + linkage);
        // Generate the initial set of cluster pairings based on the highest
        // similarity.  This mapping will be update as the number of clusters
        // are reduced, where merging a cluster will causes all the pairings
        // pointing to it constinuents recalculated.
        Map<Integer,Pairing> clusterSimilarities =
            new HashMap<Integer,Pairing>();
        for (Integer clusterId : clusterAssignment.keySet()) {
            double mostSimilar = -1;
            Integer paired = null;
            for (Integer otherId : clusterAssignment.keySet()) {
                if (otherId.equals(clusterId))
                    continue;
                double similarity = getSimilarity(
                    similarityMatrix, clusterAssignment.get(clusterId),
                    clusterAssignment.get(otherId), linkage);
                if (similarity > mostSimilar) {
                    mostSimilar = similarity;
                    paired = otherId;
                }
            }
            clusterSimilarities.put(
                clusterId, new Pairing(mostSimilar, paired));
        }

        LOGGER.info("Assigning clusters");

        int nextClusterId = rows;
        double closestClusterSimilarity = -1;
        do {
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
            if (highestSimilarity < clusterSimilarityThreshold)
                break;
            
            // Assign the merged cluster a new ID, which lets us track any
            // pairings to the original clusters that may need to be
            // recalculated
            int newClusterId = nextClusterId++;

            Set<Integer> cluster1 = clusterAssignment.get(cluster1index);
            Set<Integer> cluster2 = clusterAssignment.get(cluster2index);

            LOGGER.log(Level.FINE, "Merged cluster {0} with {1}",
                       new Object[] { cluster1, cluster2 });

            // Update the cluster assignments
            cluster1.addAll(cluster2);
            clusterAssignment.put(newClusterId, cluster1);
            clusterAssignment.remove(cluster1index);
            clusterAssignment.remove(cluster2index);
            clusterSimilarities.remove(cluster1index);
            clusterSimilarities.remove(cluster2index);


            double mostSimilarToMerged = -1;
            Integer msId = null;

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
                double sim = getSimilarity(similarityMatrix, cluster1,
                                           clusterAssignment.get(clusterId),
                                           linkage);
                if (sim > mostSimilarToMerged) {
                    mostSimilarToMerged = sim;
                    msId = clusterId;
                }

                // Second, if the pair was previously paired with one of the
                // merged clusters, recompute what its most similar is
                Pairing p = e.getValue();
                if (p.pairedIndex == cluster1index 
                        || p.pairedIndex == cluster2index) {
                    
                    // Start with with the most similar being set to the newly
                    // merged cluster, as this value has already been computed
                    double mostSimilar = sim;
                    Integer paired = newClusterId;
                    for (Integer otherId : clusterAssignment.keySet()) {
                        if (otherId.equals(clusterId))
                            continue;
                        double similarity = getSimilarity(
                            similarityMatrix, clusterAssignment.get(clusterId),
                            clusterAssignment.get(otherId), linkage);
                        if (similarity > mostSimilar) {
                            mostSimilar = similarity;
                            paired = otherId;
                        }
                    }
                    // Reassign with the new most-similar
                    clusterSimilarities.put(
                        clusterId, new Pairing(mostSimilar, paired));
                }                        
            }
            
            // Update the new most similar to the newly-merged cluster
            clusterSimilarities.put(newClusterId, 
                                    new Pairing(mostSimilarToMerged, msId));

        } while (clusterAssignment.size() > 1);

        int[] clusters = new int[rows];
        for (int i = 0; i < rows; ++i)
            clusters[i] = -1;
        int clusterIndex = 0;
        for (Set<Integer> cluster : clusterAssignment.values()) {
            // Decide whether this cluster has already been assigned
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
        return new Duple<int[], Vector[]>(clusters, contexts);
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
        case MEDIAN_LINKAGE:
            double[] similarities = new double[cluster.size() * toAdd.size()];
            int index = 0;
            for (int i : cluster) {
                for (int j : toAdd) {
                    similarities[index++] = similarityMatrix.get(i, j);
                }
            }
            Arrays.sort(similarities);
            similarity = similarities[similarities.length / 2];
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