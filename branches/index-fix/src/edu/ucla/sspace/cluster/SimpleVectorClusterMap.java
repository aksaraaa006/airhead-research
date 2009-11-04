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
import java.util.Set;


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
     * A mapping from Strings to cluster centroids.
     */
    private Map<String, List<Cluster>> vectorClusters;

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /**
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    /**
     * Create a new {@code SimpleVectorClusterMap} with the given threshold
     * size.
     */
    public SimpleVectorClusterMap(double threshold, int maxClusters) {
        clusterThreshold = threshold;
        maxNumClusters = maxClusters;
        vectorClusters = new HashMap<String, List<Cluster>>();
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
            if (similarity > clusterThreshold ||
                termClusters.size() >= maxNumClusters)
                bestMatch.addVector(value);
            else
                termClusters.add(new Cluster(value));
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
        return "SimpleClusterMap-SenseCount" + maxNumClusters;
    }

    public int getMaxNumClusters() {
        return maxNumClusters;
    }
}
