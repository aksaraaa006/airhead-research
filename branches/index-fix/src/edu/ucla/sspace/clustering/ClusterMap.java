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


public class ClusterMap {

    /**
     * A mapping from Strings to cluster centroids.
     */
    private Map<String, OnlineClustering> vectorClusters;

    private final OnlineClusteringGenerator generator;

    public ClusterMap(OnlineClusteringGenerator generator) {
        this.generator = generator;
        vectorClusters = new HashMap<String, OnlineClustering>();
    }

    /**
     * {@inheritDoc}
     */
    public int addVector(String key, Vector value) {
        // Get the set of term vectors for this word that have been found so
        // far.
        OnlineClustering clustering = vectorClusters.get(key);
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

    public int assignVector(String key, Vector value) {
        OnlineClustering clustering = vectorClusters.get(key);
        return (clustering == null) ? -1 : clustering.assignVector(value);
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
    public synchronized List<List<Vector>> getClusters(String key) {
        OnlineClustering clustering = vectorClusters.get(key);
        return (clustering == null) ? null : clustering.getClusters();
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
        OnlineClustering clustering = vectorClusters.get(key);
        return (clustering == null) ? 0 : clustering.size();
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
     * {@inheritDoc}
     */
    public Map<Integer, Integer> finalizeClustering(String term) {
        OnlineClustering clustering = vectorClusters.get(term);
        return clustering.finalizeClustering();
    }
}
