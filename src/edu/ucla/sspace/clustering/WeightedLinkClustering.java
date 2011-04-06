/*
 * Copyright 2011 David Jurgens 
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

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A special case of {@link LinkClustering} for processing weighted graphs.
 * This class differs on in the edge similarity function used.
 *
 * <p> This class is <i>not</i> thread safe.
 */
public class WeightedLinkClustering extends LinkClustering {

    private static final long serialVersionUID = 1L;

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(WeightedLinkClustering.class.getName());

    /**
     * A mapping from all the rows to their associated weight vectors.
     */
    private SparseDoubleVector[] rowWeightVectors;
    
    public WeightedLinkClustering() { 
        rowWeightVectors = null;   
    }

    /**
     * {@inheritDoc}
     */
    @Override public Assignment[] cluster(Matrix matrix, int numClusters, 
                                          Properties props) {
        rowWeightVectors = new SparseDoubleVector[matrix.rows()];
        return super.cluster(matrix, numClusters, props);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Assignment[] cluster(Matrix matrix, Properties props) { 
        rowWeightVectors = new SparseDoubleVector[matrix.rows()];
        return super.cluster(matrix, props);
    }

    /**
     * Returns the normalized weight vector for the specified row, to be used in
     * edge comparisons.  The weight vector is normalized by the number of edges
     * from the row with positive weights and includes a weight for the row to
     * itself, which reflects the similarity of the keystone nod.
     */ 
    private SparseDoubleVector getRowWeightVector(SparseMatrix sm, int row) {
        SparseDoubleVector weightVec = rowWeightVectors[row];
        if (weightVec == null) {
             synchronized(this) {
                // Check that another thread didn't already just compute the
                // vector while the current thread was blocking
                weightVec = rowWeightVectors[row];
                if (weightVec == null) {
                    int rows = rowWeightVectors.length;
                    weightVec = new CompactSparseVector(rows);

                    int[] neighbors = sm.getRowVector(row).getNonZeroIndices();
                    
                    // Count how many neighbors have positive edge weights
                    int positiveWeights = 0;
                    for (int n : neighbors)
                        if (sm.get(row, n) > 0)
                            positiveWeights++;
                    double normalizer = 1d / positiveWeights;

                    // For each of the neighbors, normalize the positive edge
                    // weights by the number of neighbors (with pos. weights)
                    for (int n : neighbors) {
                        double weight = sm.get(row, n);
                        if (weight > 0)
                            weightVec.set(n, normalizer * weight);
                    }                    

                    // Last, although the graph is assumed to not have
                    // self-loops, the weight for an node to itself is the
                    // normalizing constant (1/num positive weights).  This is
                    // analogous to the similarity contribution from the
                    // keystone node in the unweighted version
                    weightVec.set(row, normalizer);
                }
            }
        }
        return weightVec;
    }

    /**
     * Computes the similarity of the two edges as the Jaccard index of the
     * neighbors of two impost nodes, independent of the keystone node.
     *
     * @param sm a matrix containing the connections between edges.  A non-zero
     *        value in location (i,j) indicates a node <i>i</i> is connected to
     *        node <i>j</i> by an edge.
     * @param e1 an edge to be compared with {@code e2}
     * @param e2 an edge to be compared with {@code e1}
     *
     * @return the similarity of the edges.a
     */
    @Override protected double getEdgeSimilarity(SparseMatrix sm, Edge e1, Edge e2) {
        // Determing the keystone (shared) node by the edges and the other two
        // impost (unshared) nodes.
        int impost1 = -1;
        int impost2 = -1;
        if (e1.from == e2.from) {
            impost1 = e1.to;
            impost2 = e2.to;
        }
        else if (e1.from == e2.to) {
            impost1 = e1.to;
            impost2 = e2.from;
        }
        else if (e2.to == e1.from) {
            impost1 = e1.to;
            impost2 = e2.from;
        }
        else if (e1.to == e2.to) {
            impost1 = e1.from;
            impost2 = e2.from;
        }
        else
            return 0d;

        // Use the extended Tanimoto coefficient to compute the similarity of
        // two edges on the basis of the impost nodes' weight vectors.
        return Similarity.tanimotoCoefficient(
            getRowWeightVector(sm, impost1), getRowWeightVector(sm, impost2));
    }

}
