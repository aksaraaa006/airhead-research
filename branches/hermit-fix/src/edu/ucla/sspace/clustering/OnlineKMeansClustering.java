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
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.IntegerMap;
import edu.ucla.sspace.util.MultiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.CopyOnWriteArrayList;


/**
 * A simple online implementation of K-Means clustering for {@code Vector}s,
 * with the option to perform agglomerative clustering once all elements have
 * been clustered.
 *
 * @author Keith Stevens
 */
public class OnlineKMeansClustering<T extends Vector>
        implements OnlineClustering<T> {

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
     * The set of clusters.
     */
    private final List<Cluster> elements;

    /**
     * Creates a new instance of online KMeans clustering.
     */
    public OnlineKMeansClustering(double mergeThreshold,
                                  double dropThreshold,
                                  int maxNumClusters) {
        elements = new CopyOnWriteArrayList<Cluster>();
        this.clusterThreshold = mergeThreshold;
        this.dropThreshold = dropThreshold;
        this.maxNumClusters = maxNumClusters;
    }

    /**
     * {@inheritDoc}
     */
    public int addVector(T value) {
        // Update the set of centriods.

        // First make a shallow copy of the cluster list to work on.  Note that
        // by making this shallow copy, if new clusters are added while
        // assigning this instance, the new cluster will be skipped.
        List<Cluster> copiedElements = new ArrayList<Cluster>(elements);

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
        if (similarity >= clusterThreshold ||
                elements.size() >= maxNumClusters) {
            bestMatch.addVector(value);
            return bestIndex;
        } else {
            // lock to ensure that the number of clusters doesn't change while
            // we add this one
            synchronized(elements) {
                // Perform an additional check to see whether the number of
                // elements changed while we waiting on the lock
                if (elements.size() < maxNumClusters) {
                    elements.add(new Cluster(value));
                    return elements.size() - 1;
                }
                // Otherwise, while we were waiting, another thread increased
                // the number of elements to more than the max number of
                // clusters, so this element should be merged instead.
                else {
                    bestMatch.addVector(value);
                    return bestIndex;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public T getCentroid(int clusterIndex) {
        if (elements.size() <= clusterIndex)
            return null;
        return elements.get(clusterIndex).getCentroid();
    }

    /**
     * {@inheritDoc}
     */
    public List<T> getCentroids() {
        List<T> clusters = new ArrayList<T>(elements.size());
        for (Cluster cluster : elements) 
            clusters.add(cluster.getCentroid());
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
               "-threshold" + clusterThreshold;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxNumClusters() {
        return maxNumClusters;
    }

    /**
     * {@inheritDoc}
     */
    public int getCentroidSize(int clusterIndex) {
        return elements.get(clusterIndex).getTotalMemberCount();
    }

    /**
     * A simple class representing a single cluster of {@code Vector}s.
     */
    private class Cluster {

        /**
         * The centroid of the {@code Cluster}.
         */
        protected T centroid;

        /**
         * The number of items stored in this {@code Cluster}
         */
        protected int itemCount;

        /**
         * Creates a new {@code Cluster} with {@code firstVector} as the
         * centroid, and no weighting.
         */
        public Cluster(T firstVector) {
            centroid = firstVector; 
            itemCount = 1;
        }

        /**
         * Adds a {@code Vector} to the {@code Cluster}.
         *
         * @param vector The vector to add.
         */
        public synchronized void addVector(T vector) {
            VectorMath.add(centroid, vector);
            ++itemCount;
        }

        /**
         * Computes the similarity of this {@code Cluster} to a provided {@code
         * Vector}.
         */
        public synchronized double compareWithVector(T vector) {
            return Similarity.cosineSimilarity(centroid, vector);
        }

        /**
         * Returns a list of members in this {@code Cluster} as a list of {@code
         * Vector}s.
         */
        public synchronized T getCentroid() {
            return centroid;
        }

        /**
         * Returns the total number of items represented by this {@code
         * Cluster}.
         */
        public int getTotalMemberCount() {
            return itemCount;
        }
    }
}
