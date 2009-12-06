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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.IntegerMap;
import edu.ucla.sspace.util.MultiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;


/**
 * A simple online implementation of K-Means clustering for {@code Vector}s,
 * with the option to perform agglomerative clustering once all elements have
 * been clustered.
 *
 * @author Keith Stevens
 */
public class OnlineKMeansClustering implements OnlineClustering {

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /** 
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    /**
     * The threshold for droping a cluster.
     */
    private final double dropThreshold;

    /**
     * A weight for an exponential weighted moving average.  If this value is
     * not set in the constructor, no moving average will be used.
     */
    private final double clusterWeight;

    /**
     * The set of clusters.
     */
    private final List<Cluster> elements;

    /**
     * Creates a new instance of online KMeans clustering.
     */
    public OnlineKMeansClustering(double mergeThreshold,
                                  double dropThreshold,
                                  int maxNumClusters,
                                  double clusterWeight) {
        elements = new ArrayList<Cluster>();
        this.clusterThreshold = mergeThreshold;
        this.dropThreshold = dropThreshold;
        this.maxNumClusters = maxNumClusters;
        this.clusterWeight = clusterWeight;
    }

    /**
     * {@inheritDoc}
     */
    public int addVector(Vector value) {
        // Update the set of centriods.

        // First make a shallow copy of the cluster list to work on.  Note that
        // by making this shallow copy, if new clusters are added while
        // assigning this instance, the new cluster will be skipped.
        List<Cluster> copiedElements = null;
        synchronized (elements) {
            copiedElements = new ArrayList<Cluster>(elements.size());
            for (Cluster c : elements)
                copiedElements.add(c);
        }

        // Find the centriod with the best similarity.
        Cluster bestMatch = null;
        int bestIndex = copiedElements.size();
        double bestScore = -1;
        double similarity = -1;
        int i = 0;
        for (Cluster cluster : copiedElements) {
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
        synchronized (elements) {
            if (similarity >= clusterThreshold ||
                elements.size() >= maxNumClusters) {
                bestMatch.addVector(value);
                return bestIndex;
            } else {
                elements.add(getNewCluster(value));
                return elements.size() - 1;
            }
        }
    }

    public int assignVector(Vector value) {
        // First make a shallow copy of the cluster list to work on.  Note that
        // by making this shallow copy, if new clusters are added while
        // assigning this instance, the new cluster will be skipped.
        List<Cluster> copiedElements = null;
        synchronized (elements) {
            copiedElements = new ArrayList<Cluster>(elements.size());
            for (Cluster c : elements)
                copiedElements.add(c);
        }

        // Find the centriod with the best similarity.
        Cluster bestMatch = null;
        int bestIndex = copiedElements.size();
        double bestScore = -1;
        double similarity = -1;
        int i = 0;
        for (Cluster cluster : copiedElements) {
            similarity = cluster.compareWithVector(value);
            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = cluster;
                bestIndex = i;
            }
            ++i;
        }
        return bestIndex;
    }

    private Cluster getNewCluster(Vector vector) {
        return (clusterWeight == 0d)
            ? new Cluster(vector)
            : new Cluster(vector, clusterWeight, 1-clusterWeight);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<Vector> getCluster(int clusterIndex) {
        if (elements.size() <= clusterIndex)
            return null;
        return elements.get(clusterIndex).getMembers();
    }

    public synchronized List<List<Vector>> getClusters() {
        List<List<Vector>> clusters =
            new ArrayList<List<Vector>>(elements.size());
        for (Cluster cluster : elements) {
            clusters.add(cluster.getMembers());
        }
        return clusters;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int size() {
        return elements.size();
    }

    /**
     * Returns a string describing this {@code ClusterMap}.
     */
    public String toString() {
        return "OnlineKMeansClustering-maxNumClusters" + maxNumClusters +
               "-clusterWeight" + clusterWeight +
               "-threshold" + clusterThreshold;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getMaxNumClusters() {
        return maxNumClusters;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Map<Integer, Integer> finalizeClustering() {
        Map<Integer, Integer> mapping = mergeClusters();
        //List<Integer> droppedList = dropClusters(term, minPercentage);
        //for (Integer dropped : droppedList)
        //    mapping.put(dropped, -1);
        return mapping;
    }

    private Map<Integer, Integer> mergeClusters() {
        MultiMap<Integer, Integer> mergeMap =
            new HashMultiMap<Integer, Integer>();
        Set<Integer> skipList = new TreeSet<Integer>();

        boolean merged = true;
        while (merged) {
            merged = false;

            // For each cluster found, try to merge it with other clusters
            // which are similar enough.
            for (int i = 0; i < elements.size(); ++i) {
                // Skip any clusters which have already been merged.
                if (skipList.contains(i))
                    continue;

                for (int j = i+1; j < elements.size(); ++j) {
                    // Skip any clusters which have already been merged.
                    if (skipList.contains(j))
                        continue;

                    // Compute the similarity between these two clusters.
                    double similarity =
                        elements.get(i).clusterSimilarity(elements.get(j));

                    // If the similarity is high enough, add cluster j to
                    // cluster i.
                    if (similarity >= clusterThreshold) {
                        elements.get(i).addCluster(elements.get(j));

                        // Mark cluster j as merged so that it gets skipped.
                        skipList.add(j);
                        mergeMap.put(i, j);

                        // Check if any values are already merged into j.
                        // If so, merge them to i and remove the mapping to
                        // j.
                        Set<Integer> mergedValues = mergeMap.get(j);
                        if (mergedValues != null) {
                            for (Integer index : mergedValues)
                                mergeMap.put(i, index);
                            mergeMap.remove(j);
                        }

                        merged = true;
                    }
                }
            }
        }

        // Drop any merged clusters.
        int dropped = 0;
        for (Integer dropIndex : skipList) {
            elements.remove(dropIndex.intValue() - dropped);
            dropped++; 
        }

        // Compute which clusters were merged and provide a mapping from merged
        // cluster to result cluster index.
        Map<Integer, Integer> resultMergeMap = new IntegerMap<Integer>();
        if (mergeMap.size() != 0) {
            for (Map.Entry<Integer, Integer> merges : mergeMap.entrySet())
                resultMergeMap.put(merges.getValue(), merges.getKey());
        }
        return resultMergeMap;
    }

    private List<Integer> dropClusters() {
        List<Integer> dropped = new LinkedList<Integer>();

        double[] clusterSizes = new double[elements.size()];
        int i = 0;
        int sum = 0;
        for (Cluster cluster : elements) {
          clusterSizes[i] = cluster.getTotalMemberCount();
          sum += clusterSizes[i];
        }

        int dropCount = 0;
        for (i = 0; i < clusterSizes.length; ++i) {
            if (clusterSizes[i]/sum < dropThreshold) {
                dropped.add(i);
                elements.remove(i - dropCount);
                dropCount++;
            }
        }
        return dropped;
    }
}
