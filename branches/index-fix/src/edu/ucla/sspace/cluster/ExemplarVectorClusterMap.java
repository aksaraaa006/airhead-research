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

package edu.ucla.sspace.cluster;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import edu.ucla.sspace.matrix.Matrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A cluster map which uses exemplar clusters for {@code Vector}s.  For each
 * cluster generated, only the centroid is stored.  When a given {@code Vector}
 * is assigned to a cluster, that value is stored into a set of recent
 * exemplars.  Once a certain number of values exist within this cluster, the
 * oldest value is summed into a centroid {@code Vector}, which is always
 * retained.  New clusters for a given key are created when no other cluster has
 * a similarity to the given {@code Vector} that is higher than a specified
 * threshold.  There can also be a limitation to the number of clusters
 * generated, in which case, once the maximum number of clusters has been
 * reached, the given {@code Vector} is assigned to the best matching cluster.
 */
public class ExemplarVectorClusterMap implements BottomUpVectorClusterMap {

    public enum ClusterLinkage {
        SINGLE_LINKAGE,
        COMPLETE_LINKAGE,
        MEAN_LINKAGE,
        MEDIAN_LINKAGE,
    }

    public static final String LINKAGE_PROPERTY = 
        BottomUpVectorClusterMap.PROPERTY_PREFIX + ".linkage";

    private static final String DEFAULT_LINKAGE = "SINGLE_LINKAGE";

    /**
     * A mapping from Strings to cluster centroids.
     */
    private Map<String, List<ExemplarCluster>> vectorClusters;

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /**
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    private final ClusterLinkage linkage;

    public ExemplarVectorClusterMap() {
        this(System.getProperties());
    }

    /**
     * Create a new {@code ExemplarVectorClusterMap} with the given threshold
     * size.
     */
    public ExemplarVectorClusterMap(Properties props) {
        vectorClusters = new HashMap<String, List<ExemplarCluster>>();

        clusterThreshold = Double.parseDouble(props.getProperty(
                    BottomUpVectorClusterMap.THRESHOLD_PROPERTY, ".75"));
        maxNumClusters = Integer.parseInt(props.getProperty(
                    BottomUpVectorClusterMap.MAX_CLUSTERS_PROPERTY, "2"));

        String linkageStr = props.getProperty(LINKAGE_PROPERTY, DEFAULT_LINKAGE);
        linkage = ClusterLinkage.valueOf(linkageStr.toUpperCase());
    }

    /**
     * {@inheritDoc}
     */
    public int addVector(String key, Vector value) {
        // Get the set of term vectors for this word that have been found so
        // far.
        List<ExemplarCluster> termClusters = vectorClusters.get(key);
        if (termClusters == null) {
            synchronized (this) {
                termClusters = vectorClusters.get(key);
                if (termClusters == null) {
                    termClusters = new ArrayList<ExemplarCluster>();
                    vectorClusters.put(key, termClusters);
                }
            }
        }

        // Update the set of centriods.

        // First make a shallow copy of the cluster list to work on.  Note that
        // by making this shallow copy, if new clusters are added while assigning
        // this instance, the new cluster will be skipped.
        List<ExemplarCluster> copiedList = null;
        synchronized (termClusters) {
            copiedList = new ArrayList<ExemplarCluster>(termClusters.size());
            for (ExemplarCluster c : termClusters)
                copiedList.add(c);
        }

        // Find the centriod with the best similarity.
        Cluster bestMatch = null;
        int bestIndex = copiedList.size();
        double bestScore = -1;
        double similarity = -1;
        int i = 0;
        for (ExemplarCluster cluster : copiedList) {
            similarity = cluster.compareWithVector(value);
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = cluster;
                bestIndex = i;
            }
            ++i;
        }

        // Add the current term vector if the similarity is high enough, or set
        // it as a new centroid.
        synchronized (termClusters) {
            if (similarity >= clusterThreshold ||
                termClusters.size() >= maxNumClusters)
                bestMatch.addVector(value);
            else
                termClusters.add(new ExemplarCluster(value));
            return bestIndex;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear() {
        vectorClusters.clear();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<Vector> getCluster(String key, int clusterIndex) {
        List<ExemplarCluster> termClusters = vectorClusters.get(key);
        if (termClusters == null || termClusters.size() <= clusterIndex)
            return null;
        return termClusters.get(clusterIndex).getMembers();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<List<Vector>> getClusters(String key) {
        List<ExemplarCluster> termClusters = vectorClusters.get(key);
        if (termClusters == null)
            return null;
        List<List<Vector>> clusters =
            new ArrayList<List<Vector>>(termClusters.size());
        for (ExemplarCluster cluster : termClusters) {
            clusters.add(cluster.getMembers());
        }
        return clusters;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Set<String> keySet() {
        return vectorClusters.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getNumClusters(String key) {
        List<ExemplarCluster> termClusters = vectorClusters.get(key);
        return (termClusters != null) ? termClusters.size() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void removeClusters(String key) {
        vectorClusters.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int size() {
        return vectorClusters.size();
    }

    /**
     * Returns a string describing this {@code ClusterMap}.
     */
    public String toString() {
        return "ExemplarClusterMap";
    }

    public int getMaxNumClusters() {
        return maxNumClusters;
    }

    public synchronized int getOverflowCount(String key) {
        return 0;
    }

    /**
     * A simple class which represents a single cluster in the Exemplar model.
     * In this, the {@code MAX_EXEMPLARS} most recent {@code Vector}s are stored
     * in this cluster, with all other vectors being summed into a centroied
     * {@code Vector}.  When computing the similarity, the summed similarity
     * between a given {@code Vector} and all exemplars, and the centroid is
     * returned.
     */
    private class ExemplarCluster extends Cluster {
        public int MAX_EXEMPLARS = 20;

        public Queue<Vector> exemplars;

        public ExemplarCluster(Vector vector) {
            super(vector);
            exemplars = new LinkedList<Vector>();
        }

        /**
         * Compute the similarity of {@code vector} with each exemplar and the
         * stored {@code centroid}, if it exists.
         *
         * @param vector The vector to compare this cluster against.
         *
         * @return The total similarity of {@code vector} to all {@code Vector}s
         *         in this cluster.
         */
        public synchronized double compareWithVector(Vector vector) {
            List<Vector> vList = new ArrayList<Vector>();
            vList.add(vector);
            return computeSimilarity(getMembers(), vList);
        }

        private double computeSimilarity(List<Vector> cluster,
                                         List<Vector> toCompare) {
            double finalSimilarity = 0;
            switch (linkage) {
                case SINGLE_LINKAGE: {
                    double highestSimilarity = -1;
                    for (Vector c : cluster) {
                        for (Vector t : toCompare) {
                            double sim = Similarity.cosineSimilarity(c, t);
                            if (sim > highestSimilarity)
                                highestSimilarity = sim;
                        }
                    }
                    finalSimilarity = highestSimilarity;
                    break;
                }
                case COMPLETE_LINKAGE: {
                    double lowestSimilarity = 2;
                    for (Vector c : cluster) {
                        for (Vector t : toCompare) {
                            double sim = Similarity.cosineSimilarity(c, t);
                            if (sim < lowestSimilarity)
                                lowestSimilarity = sim;
                        }
                    }
                    finalSimilarity = lowestSimilarity;
                    break;
                }
                case MEAN_LINKAGE: {
                    double similarity = 2;
                    for (Vector c : cluster)
                        for (Vector t : toCompare)
                            similarity += Similarity.cosineSimilarity(c, t);
                    finalSimilarity =
                        similarity / (cluster.size() * toCompare.size());
                    break;
                }
                case MEDIAN_LINKAGE: {
                    double[] similarity =
                        new double[cluster.size() * toCompare.size()];
                    int index = 0;
                    for (Vector c : cluster)
                        for (Vector t : toCompare)
                            similarity[index++] =
                                Similarity.cosineSimilarity(c,t);
                    Arrays.sort(similarity);
                    finalSimilarity = similarity[similarity.length / 2];
                    break;
                }
            }
            return finalSimilarity;
        }

        public synchronized void addCluster(Cluster cluster) {
            ExemplarCluster eCluster = (ExemplarCluster) cluster;

            // Push half of the exemplars in each cluster into the centroid.
            for (int i = 0; i < MAX_EXEMPLARS / 2; ++i) {
                super.addVector(this.exemplars.remove());
                super.addVector(eCluster.exemplars.remove());
            }

            // Store the rest from the other cluster to this set of exemplars.
            for (Vector otherExemplar : eCluster.exemplars)
                exemplars.offer(otherExemplar);

            // Merge the two centroids.
            super.addCluster(cluster);
        }

        /**
         * Add a {@code Vector} to this {@code ExemplarCluster}.  If the maximum
         * number of stored {@code Vector}s has been reached, add the oldest
         * {@code Vector} into the centroid.
         *
         * @param vector The new {@code Vector} to add to the cluster.
         */
        public synchronized void addVector(Vector vector) {
            exemplars.add(vector);
            if (exemplars.size() > MAX_EXEMPLARS) {
                super.addVector(exemplars.remove());
            }
        }

        public synchronized List<Vector> getMembers() {
            // Return a shallow copy.
            List<Vector> members = super.getMembers();
            for (Vector v : exemplars)
                members.add(Vectors.immutableVector(v));
            return members;
        }

        public synchronized int getTotalMemberCount() {
            return super.getTotalMemberCount() + exemplars.size();
        }

        public synchronized double clusterSimilarity(Cluster otherCluster) {
            return computeSimilarity(getMembers(), otherCluster.getMembers());
        }
    }

    public synchronized Map<Integer, Integer> mergeOrDropClusters(
            String term, double minPercentage) {
        List<ExemplarCluster> clusters = vectorClusters.get(term);
        double[] clusterSizes = new double[clusters.size()];
        int i = 0;
        int sum = 0;
        for (Cluster cluster : clusters) {
          clusterSizes[i] = cluster.getTotalMemberCount();
          sum += clusterSizes[i];
        }
        int dropCount = 0;
        for (i = 0; i < clusterSizes.length; ++i) {
            if (clusterSizes[i]/sum < minPercentage) {
                clusters.remove(i - dropCount);
                dropCount++;
            }
        }
        return null;
    }

    public synchronized Matrix pairWiseSimilarity(String term) {
        return null;
    }
}
