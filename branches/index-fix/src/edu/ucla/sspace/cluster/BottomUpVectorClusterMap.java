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

import edu.ucla.sspace.vector.Vector;

import java.util.List;
import java.util.Set;


/**
 * An interface for clustering sets of {@code Vector}s, and mapping to these
 * clusters via Strings.  Implementations will be used to perform bottom up
 * clustering algorithms on an increasing number of {@code Vector}s.
 * Implementations have the option of storing a fixed number of {@code Vector}s
 * given, or all {@code Vector}s. The expected result of these clustering
 * algorithms will be either a set of {@code Vector}s which are in each cluster,
 * or simply the centroid which best describes each cluster for a key.
 *
 * </p>All Implementations must be threadsafe in all operations.
 */
public interface BottomUpVectorClusterMap {

    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.BottomUpVectorClusterMap";

    public static final String THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".threshold";

    public static final String MAX_CLUSTERS_PROPERTY =
        PROPERTY_PREFIX + ".maxclusters";

    /**
     * Add a {@code Vector} corresponding the the given key and place it in an
     * appropriate cluster.  Implementations may merge this value into a
     * centroid, hold onto this value for a period of time and then throw it
     * out, or store the value indefiniately.
     *
     * @param key The string corresponding to this {@code Vector}
     * @param value The {@code Vector} to cluster.
     */
    int addVector(String key, Vector value);

    /**
     * Clear all clusters and mappings.
     */
    void clear();

    /**
     * Return the number of mappings stored by this cluster mapping.
     *
     * @return The number of key to sets of clusters mapped.
     */
    int size();

    /**
     * Return a set of clusters corresponding to the given {@code key}.  The
     * returned clusters do not have to contain all {@code Vector}s assigned to
     * that cluster.
     *
     * @param key the key whose clusters are to be returned.
     *
     * @return The clusters corresponding to {@code key}.
     */
    List<List<Vector>> getClusters(String key);

    /**
     * Return a specific cluster for a {@code key}.  If the given {@code
     * clusterIndex} specifies a cluster which does not exist, implementations
     * may throw an error or simply return null.
     *
     * @param key The key whose cluster is to be returned.
     * @param clusterIndex The specific cluster to return.
     *
     * @return The {@code clusterIndex}th cluster corresponding to {@code key}.
     */
    List<Vector> getCluster(String key, int clusterIndex);

    /**
     * Return the number of clusters corresponding to the given {@code key}.
     *
     * @param key The key whose number of clusters is to be returned.
     *
     * @return The number of clusters corresponding to {@code key}.
     */
    int getNumClusters(String key);

    /**
     * Return the set of keys which this cluster map contains.
     *
     * @return The keys that have mappings.
     */
    Set<String> keySet();

    /**
     * Remove all clusters corresponding to a particular {@code key}.
     *
     * @param key The key whose clusters are to be removed.
     */
    void removeClusters(String key);

    int getMaxNumClusters();

    int getOverflowCount(String key);

    void mergeOrDropClusters(double minPercentage);
}
