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
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A simple clustering mapping for {@code Vector}s.  For each cluster generated,
 * only the centroid is stored.  When a given {@code Vector} is assigned to a
 * cluster, that value is summed into the centroid.  New clusters for a given
 * key are created when no other cluster has a similarity to the given {@code
 * Vector} that is higher than a specified threshold.  There can also be a
 * limitation to the number of clusters generated, in which case, once the
 * maximum number of clusters has been reached, the given {@code Vector} is
 * assigned to the best matching cluster.
 */
public class SimpleVectorClusterMap implements BottomUpVectorClusterMap {

    /**
     * A property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.SimpleVectorClusterMap";

    /**
     * The property for setting the weight of an exponential weighted moving
     * average.  This weight will be given to the historical data, and this
     * weight subtracted from 1 will be given to the new data points.
     */
    public static final String WEIGHTING_PROPERTY =
        PROPERTY_PREFIX + ".weights";

    /**
     * A mapping from Strings to cluster centroids.
     */
    private Map<String, List<Cluster>> vectorClusters;

    private ConcurrentMap<String, AtomicInteger> overflowCounts;

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /**
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    /**
     * A weight for an exponential weighted moving average.  If this value is
     * not set in the constructor, no moving average will be used.
     */
    private final double clusterWeight;

    /**
     * Create a new {@code SimpleVectorClusterMap} with the given threshold
     * size.
     */
    public SimpleVectorClusterMap() {
        this(System.getProperties());
    }

    public SimpleVectorClusterMap(Properties props) {
        vectorClusters = new HashMap<String, List<Cluster>>();
        overflowCounts = new ConcurrentHashMap<String, AtomicInteger>();

        clusterThreshold = Double.parseDouble(props.getProperty(
                    BottomUpVectorClusterMap.THRESHOLD_PROPERTY, ".75"));
        maxNumClusters = Integer.parseInt(props.getProperty(
                    BottomUpVectorClusterMap.MAX_CLUSTERS_PROPERTY, "2"));
        clusterWeight =
            Double.parseDouble(props.getProperty(WEIGHTING_PROPERTY, "0"));
    }

    /**
     * {@inheritDoc}
     */
    public int addVector(String key, Vector value) {
        // Get the set of term vectors for this word that have been found so
        // far.
        List<Cluster> termClusters = vectorClusters.get(key);
        if (termClusters == null) {
            synchronized (this) {
                termClusters = vectorClusters.get(key);
                if (termClusters == null) {
                    termClusters = new ArrayList<Cluster>();
                    vectorClusters.put(key, termClusters);
                }
            }
        }

        // Update the set of centriods.
        synchronized (termClusters) {
            Cluster bestMatch = null;
            int bestIndex = termClusters.size();
            double bestScore = -1;
            double similarity = -1;
            
            // Find the centriod with the best similarity.
            int i = 0;
            for (Cluster cluster : termClusters) {
                similarity = cluster.compareWithVector(value);
                if (similarity > bestScore) {
                    bestScore = similarity;
                    bestMatch = cluster;
                    bestIndex = i;
                }
                ++i;
            }

            // Add the current term vector if the similarity is high enough,
            // or set it as a new centroid.
            if (similarity >= clusterThreshold ||
                termClusters.size() >= maxNumClusters) {
                bestMatch.addVector(value);
                if (similarity < clusterThreshold) {
                    AtomicInteger count = overflowCounts.get(key);
                    if (count != null)
                        count.incrementAndGet();
                }
            } else
                termClusters.add(getNewCluster(value));
            return bestIndex;
        }
    }

    private Cluster getNewCluster(Vector vector) {
        return (clusterWeight == 0d)
            ? new Cluster(vector)
            : new Cluster(vector, clusterWeight, 1-clusterWeight);
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
        List<Cluster> termClusters = vectorClusters.get(key);
        if (termClusters == null || termClusters.size() <= clusterIndex)
            return null;
        return termClusters.get(clusterIndex).getMembers();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<List<Vector>> getClusters(String key) {
        List<Cluster> termClusters = vectorClusters.get(key);
        if (termClusters == null)
            return null;
        List<List<Vector>> clusters =
            new ArrayList<List<Vector>>(termClusters.size());
        for (Cluster cluster : termClusters) {
            clusters.add(cluster.getMembers());
        }
        return clusters;
    }

    public synchronized int getOverflowCount(String key) {
        AtomicInteger count = overflowCounts.get(key);
        return (count != null) ? count.get() : 0;
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
        List<Cluster> termClusters = vectorClusters.get(key);
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
        return "SimpleClusterMap-SenseCount" + maxNumClusters +
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
    public synchronized void mergeOrDropClusters(double minPercentage) {
        for (Map.Entry<String, List<Cluster>> entry :
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
                    entry.getValue().remove(i - dropCount);
                    dropCount++;
                }
            }
        }
    }
}
