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

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
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

public class KPartiteLinkClustering extends LinkClustering {

    private static final long serialVersionUID = 1L;

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(KPartiteLinkClustering.class.getName());

    /**
     * The number of disjoint sets of nodes in the network.
     */
    private int numSeparations;

    /**
     * A mapping from a row to the set of nodes of which it is a part.  The
     * number of sets is equal to the partite-ness of the graph.
     */
    private List<Integer> rowToSet;

    /**
     * <i>Ignores the specified number of clusters</i> and returns the
     * clustering solution according to the partition density.
     *
     * @param numClusters this parameter is ignored.
     *
     * @throws IllegalArgumentException if {@code matrix} is not square, or is
     *         not an instance of {@link SparseMatrix}
     */
    public Assignment[] cluster(Matrix matrix, int numClusters, 
                                Properties props) {
        LOGGER.warning("PartitionLink clustering has been called without " +
                       "specifying the k-partiteness in the graph.  Assuming " +
                       "all nodes are part of one partition, which is " +
                       "probably not what you want.");
        return cluster(matrix, props, 1, singlePartition(matrix.rows()));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code matrix} is not square, or is
     *         not an instance of {@link SparseMatrix}
     */
    public Assignment[] cluster(Matrix matrix, Properties props) { 
        LOGGER.warning("PartitionLink clustering has been called without " +
                       "specifying the k-partiteness in the graph.  Assuming " +
                       "all nodes are part of one partition, which is " +
                       "probably not what you want.");
        return cluster(matrix, props, 1, singlePartition(matrix.rows()));
    }

    /**
     * 
     * <p> Note that this implementation does not check that the k-partite-ness
     * of the graph is consistent.  Any violations (i.e. edges betwen the
     * k-disjoint sets) will not be caught and will subsequently affect the
     * results.
     *
     * @param numPartitions the number of disjoint sets in the graph represented
     *        as the matrix
     * @param rowToPartition a mapping from a row index to its partition
     *
     * @throws IllegalArgumentException if {@code matrix} is not square, or is
     *         not an instance of {@link SparseMatrix}
     */
    public Assignment[] cluster(Matrix matrix, Properties props,
                                int numPartitions, 
                                List<Integer> rowToPartition) {
        // Update the class-local state that the similarity and density
        // functions will use in their calculations
        this.numSeparations = numPartitions;
        this.rowToSet = rowToPartition;
        return super.cluster(matrix, props);
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

        /*
         * IMPLEMENTATION NOTE: see the note in LinkClustering on the
         * similarity.  This current code is optimized for the K-partite case
         * which is non-inclusive of the impost nodes.
         */

        // Determine the overlap between the neighbors of the impost nodes
        int[] impost1edges = sm.getRowVector(impost1).getNonZeroIndices();
        int[] impost2edges = sm.getRowVector(impost2).getNonZeroIndices();

        // Identify which of the two edge sets is smaller.  We will iterate over
        // the smaller set, using the smaller bitset for indexing.  Benchmarking
        // showed this was ~10% faster in some cases.
        int[] smaller = null;
        int[] larger = null;
        if (impost1edges.length > impost2edges.length) {
            larger = impost1edges;
            smaller = impost2edges;
        }
        else {
            larger = impost2edges;
            smaller = impost1edges;
        }
        
        // IMPLEMENTATION NOTE: normally, we would need to sort the larger
        // array.  However, the contract for SparseVector indicates the returned
        // indices must be returned in sorted order.  Therefore, we can elide
        // the sort while still calling Arrays.binarySearch.  The sort call is
        // left in place, but commented out for the reader's benefit.
        //
        // Arrays.sort(larger);
            
        int inCommon = 0; 
        for (int j : smaller) {
            if (Arrays.binarySearch(larger, j) >= 0)
                inCommon++;
        }
        
        double unionSize = (smaller.length - inCommon) + larger.length;

        return inCommon / unionSize;
    }

    /**
     * Calculates the density of the provided partitioning.  Subclasses should
     * override this method only if they need to redefine the density to take
     * into account the network structure (e.g. for a k-partite network).
     *
     * @param clusterToElements a mapping from a cluster's ordinal to the set of
     *        ids of the edges contained within it.
     * @param edgeList the list of edges that are being clustered.  Note that
     *        the size of this list is the number of edges in the graph.
     * @param numVertices the number of vertices being clustered
     * @return the density of this partition.
     */
    @Override protected double calculatePartitionDensity(
            MultiMap<Integer,Integer> clusterToElements,
            List<Edge> edgeList,
            int numVertices) {

        int numEdges = edgeList.size();

        // Compute the cluster density for each division of this partition
        double clusterDensitySum = 0d;
        for (Integer cluster : clusterToElements.keySet()) {
            Set<Integer> linkPartition = clusterToElements.get(cluster);
            // Special case for partition with two nodes
            if (linkPartition.size() == 1)
                continue; // cluster density = 0
            int edgesInPartition = linkPartition.size();
                        
            Set<Integer> nodesInPartition = 
                new HashSet<Integer>(edgesInPartition >> 1);
            
            int[] nodesInEachSet = new int[numSeparations];
            for (Integer linkIndex : linkPartition) {
                Edge link = edgeList.get(linkIndex);
                int from = link.from;
                int to = link.to;
                nodesInPartition.add(from);
                nodesInPartition.add(to);
                nodesInEachSet[rowToSet.get(from)]++;
                nodesInEachSet[rowToSet.get(to)]++;
            }
            int numNodesInAllSets = nodesInPartition.size();

            // The modified cluster density uses two terms, the first of which
            // calculates the sum of the product of a set's size and sizes of
            // all other sets.  
            int denomFirstTerm = 0;
            for (int s = 0; s < numSeparations; ++s) {
                int inCurSet = nodesInEachSet[s];
                int sum = 0;
                for (int s2 = 0; s2 < numSeparations; ++s2) {
                    if (s == s2)
                        continue;
                    sum += inCurSet * nodesInEachSet[s2];
                }
                denomFirstTerm += sum;
            }

            // This reflects the density of this particular cluster weighted by
            // the number of edges in it
            double clusterDensity = edgesInPartition * 
                ((edgesInPartition + 1d - numNodesInAllSets) 
                 / (denomFirstTerm - (2 * (numNodesInAllSets - 1))));
            
            clusterDensitySum += clusterDensity;
        }

        // Compute the density for the total partitioning solution across all
        // clusters
        double partitionDensity = (2d / numEdges) * clusterDensitySum;
        return partitionDensity;
    }

    /**
     * Returns a {@code List} for which all elements return the same partition
     * value.
     *
     * @param numElements the number of nodes in the graph, which are all
     *        treated as being a part of the same set
     */
    private List<Integer> singlePartition(final int numElements) {
        // Instead of creating an empty array and wrapping it as a list, just
        // extend AbstractList to make a read-only list customized to the data
        return new AbstractList<Integer>() {
            public Integer get(int i) {
                if (i < 0 || i >= numElements)
                    throw new IndexOutOfBoundsException();
                return 0;
            }
            public int size() {
                return numElements;
            }
        };
    }
}
