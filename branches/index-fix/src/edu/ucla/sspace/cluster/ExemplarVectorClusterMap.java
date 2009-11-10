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

import java.util.ArrayList;
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

    /**
     * A mapping from Strings to cluster centroids.
     */
    private Map<String, List<ExemplarCluster>> vectorClusters;

    private ConcurrentMap<String, AtomicInteger> overflowCounts;

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /**
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    public ExemplarVectorClusterMap() {
        this(System.getProperties());
    }

    /**
     * Create a new {@code ExemplarVectorClusterMap} with the given threshold
     * size.
     */
    public ExemplarVectorClusterMap(Properties props) {
        vectorClusters = new HashMap<String, List<ExemplarCluster>>();
        overflowCounts = new ConcurrentHashMap<String, AtomicInteger>();

        clusterThreshold = Double.parseDouble(props.getProperty(
                    BottomUpVectorClusterMap.THRESHOLD_PROPERTY, ".75"));
        maxNumClusters = Integer.parseInt(props.getProperty(
                    BottomUpVectorClusterMap.MAX_CLUSTERS_PROPERTY, "2"));
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

        // Update the set of clusters.
        synchronized (termClusters) {
            double[] scores = new double[termClusters.size()];
            double totalScore = 0;
            // Compute the similarity of value with each of the clusters.
            int i = 0;
            for (ExemplarCluster cluster : termClusters) {
                scores[i] = cluster.compareWithVector(value);
                totalScore += scores[i];
                ++i;
            }

            // Find the cluster which had the highest normalized similarity.
            for (i = 0; i < scores.length; ++i)
                scores[i] /= totalScore;
            double bestScore = -1;
            int bestIndex = termClusters.size();
            for (i = 0; i < scores.length; ++i) {
                if (scores[i] > bestScore) {
                    bestScore = scores[i];
                    bestIndex = i;
                }
            }

            // Store the value in the cluster with the highest similarity as
            // long as that score is higher than the threshold, or we have
            // reached the maximum number of clusters.
            if (bestScore > clusterThreshold ||
                termClusters.size() > maxNumClusters) {
                termClusters.get(bestIndex).addVector(value);
                if (bestScore < clusterThreshold) {
                    AtomicInteger count = overflowCounts.get(key);
                    if (count != null)
                        count.incrementAndGet();
                }
            } else  {
                // If there are not the maximum number of clusters, and the
                // similarity to all known clusters was too weak, add the value
                // into a new cluster.
                termClusters.add(new ExemplarCluster(value));
            }
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
        AtomicInteger count = overflowCounts.get(key);
        return (count != null) ? count.get() : 0;
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
            double similarity = 0;
            for (Vector exemplar : exemplars)
                similarity += Similarity.cosineSimilarity(vector, exemplar);
            similarity += super.compareWithVector(vector);
            similarity /= (exemplars.size() + 1);
            return similarity;
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
            return new ArrayList<Vector>(exemplars);
        }

        public synchronized int getTotalMemberCount() {
            return super.getTotalMemberCount() + exemplars.size();
        }
    }
    public synchronized void mergeOrDropClusters(double minPercentage) {
        for (Map.Entry<String, List<ExemplarCluster>> entry :
                vectorClusters.entrySet()) {
            double[] clusterSizes = new double[entry.getValue().size()];
            int i = 0;
            int sum = 0;
            for (Cluster cluster : entry.getValue()) {
              clusterSizes[i] = cluster.getTotalMemberCount();
              sum += clusterSizes[i];
            }
            int dropCount = 0;
            for (i = 0; i < clusterSizes.length; ++i) {
                if (clusterSizes[i]/sum < minPercentage) {
                    entry.getValue().remove(i + dropCount);
                    dropCount++;
                }
            }
        }
    }
}
