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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * A map of A String to a set of vector clusters.  For each String, the set of
 * stored {@code Vector}s will be stored in an {@code OnlineClustering}
 * implementation that may or may not retain all {@code Vector} instances.  The
 * clustering method used will be determeined by a {@code
 * OnlineClusteringGenerator}.  
 *
 * @author Keith Stevens
 */
public class ClusterMap<T extends Vector> {

    /**
     * A mapping from Strings to a clustering implementation.
     */
    private Map<String, OnlineClustering<T>> vectorClusters;

    /**
     * A generator for clustering implementations.
     */
    private final OnlineClusteringGenerator<T> generator;

    /**
     * Generates a new {@code OnlineClusterMap} using the given {@code
     * OnlineClusteringGenerator} and a {@code HashMap}.
     */
    public ClusterMap(OnlineClusteringGenerator<T> generator) {
        this(generator, new HashMap<String, OnlineClustering<T>>());
    }

    /**
     * Generates a new {@code OnlineClusterMap} using the given {@code
     * OnlineClusteringGenerator} and the given {@code Map} type.
     */
    public ClusterMap(OnlineClusteringGenerator<T> generator,
                      Map<String, OnlineClustering<T>> map) {
        this.generator = generator;
        vectorClusters = map;
    }

    /**
     * Assigns {@code value} to a cluster for {@code key}.  The returned value
     * will be the cluster index {@code value} was assigned to.
     *
     * @param key The set of clusters to assing {@code value} to.
     * @param value The vector to assign to a cluster.
     *
     * @return The cluster id {@code value} was assigned to.
     */
    public int addVector(String key, T value) {
        // Get the set of term vectors for this word that have been found so
        // far.
        OnlineClustering<T> clustering = vectorClusters.get(key);
        if (clustering == null) {
            synchronized (vectorClusters) {
                clustering = vectorClusters.get(key);
                if (clustering == null) {
                    clustering = generator.getNewClusteringInstance(); 
                    vectorClusters.put(key, clustering);
                }
            }
        }
        return clustering.addVector(value);
    }

    /**
     * Clears the set of cluster mappings.
     */
    public synchronized void clear() {
        vectorClusters.clear();
    }

    public synchronized OnlineClustering<T> getClustering(String key) {
        return vectorClusters.get(key);
    }

    /**
     * Returns the set of clustered {@code Vector}s that {@code key} maps to.
     */
    public synchronized List<T> getCentroids(String key) {
        OnlineClustering<T> clustering = vectorClusters.get(key);
        return (clustering == null) ? null : clustering.getCentroids();
    }

    /**
     * Returns the set of keys stored in this map.
     */
    public synchronized Set<String> keySet() {
        return vectorClusters.keySet();
    }

    /**
     * Returns the number of clusters {@code key} maps to.
     */
    public synchronized int getNumClusters(String key) {
        OnlineClustering<T> clustering = vectorClusters.get(key);
        return (clustering == null) ? 0 : clustering.size();
    }

    /**
     * Removes all clusters associated with {@code key}.
     */
    public synchronized void removeClusters(String key) {
        vectorClusters.remove(key);
    }

    /**
     * Returns the number of mappings stored in this map.
     */
    public synchronized int size() {
        return vectorClusters.size();
    }

    public String toString() {
        return generator.toString();
    }
}
