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

import edu.ucla.sspace.vector.Vector;

import java.util.List;
import java.util.Map;

/**
 * An interface for any Online clustering implementation.  These
 * implementations should support adding of new vectors, finding the most
 * similar cluster for a vector, returning clustered vectors, and optionally a
 * finalization step that may drop or merge clusters.
 *
 * @author Keith Stevens
 */
public interface OnlineClustering {

    /**
     * Adds {@code value} to one of the clusters.  Returns the unique index of
     * the designated cluster.
     */
    int addVector(Vector value);

    /**
     * Returns the unique index of the cluster {@code value} is most similar to.
     */
    int assignVector(Vector value);

    /**
     * Finalizes any clustering.  Returns a mapping that describes any cluster
     * indexes that were dropped or merged.  Keys will be cluster indexes that
     * were relocated, and mapped values will be -1 if the cluster was dropped.
     * Positive values will be the new cluster index values stored at the key
     * index are stored at.
     */
    Map<Integer, Integer> finalizeClustering();

    /**
     * Returns the list of {@code Vector}s in the given cluster.
     */
    List<Vector> getCluster(int clusterIndex);

    /**
     * Returns the list of all clustered {@code Vector}s.
     */
    List<List<Vector>> getClusters();

    /**
     * Returns the numeber of clusters.
     */
    int size();
}
