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

package edu.ucla.sspace.graph;

import edu.ucla.sspace.common.Statistics;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;


/**
 * An implementation of the Randomized EnumerateSubgraphs (RAND-ESU) method from
 * Wernicke (2006), which provides a unbiased random sampling of the
 * size-<i>k</i> subgraphs of an input graph.  For full details see: <ul>
 *
 * <li style="font-family:Garamond, Georgia, serif"> Sebastian
 *   Wernicke. Efficient detection of network motifs. <i>in</i> IEEE/ACM
 *   Transactions on Computational Biology and Bioinformatics.
 * </li>
 * </ul>
 *
 * <p> This class is not thread-safe and does not support the {@link #remove()}
 * method.
 *
 * @author David Jurgens
 */
public class SamplingSubgraphIterator<T extends Edge> 
        implements Iterator<Graph<T>> {

    private final Graph<T> g;

    private final int subgraphSize;

    private Iterator<Integer> vertexIter;

    private Queue<Graph<T>> nextSubgraphs;

    private final double[] traversalProbabilitiesAtDepth;

    public SamplingSubgraphIterator(Graph<T> g, int subgraphSize,
                                    double[] traversalProbabilitiesAtDepth) {
        this.g = g;
        this.subgraphSize = subgraphSize;
        if (g == null)
            throw new NullPointerException();
        if (subgraphSize < 1)
            throw new IllegalArgumentException("size must be positive");
        if (subgraphSize > g.order())
            throw new IllegalArgumentException("size must not be greater " 
                + "than the number of vertices in the graph");
        if (traversalProbabilitiesAtDepth.length != subgraphSize) 
            throw new IllegalArgumentException("must specify the probability " +
                "of traversal at each depth; i.e., one propability for each " +
                "subgraph size");
        this.traversalProbabilitiesAtDepth = 
            Arrays.copyOf(traversalProbabilitiesAtDepth,
                          traversalProbabilitiesAtDepth.length);
        // Check that the probabilities are all in the range (0, 1]
        for (int i = 0; i < this.traversalProbabilitiesAtDepth.length; ++i) {
            double prob = this.traversalProbabilitiesAtDepth[i];
            if (prob <= 0 || prob > 1)
                throw new IllegalArgumentException("Invalid probability at " +
                    "depth " +  i + ": " + prob + "; probabilities must all " +
                    "be in the range (0, 1]");
        }
        vertexIter = g.vertices().iterator();
        nextSubgraphs = new ArrayDeque<Graph<T>>();
        advance();
    }

    private void advance() {
        if (nextSubgraphs.isEmpty() && vertexIter.hasNext()) {
            Integer nextVertex = vertexIter.next();
            // Determine the set of vertices that are greater than this vertex
            Set<Integer> extension = new HashSet<Integer>();
            for (Integer v : g.getAdjacentVertices(nextVertex))
                if (v > nextVertex)
                    extension.add(v);
            Set<Integer> subgraph = new HashSet<Integer>();
            subgraph.add(nextVertex);
            extendSubgraph(subgraph, extension, nextVertex);
        }
    }

    private void extendSubgraph(Set<Integer> subgraph, Set<Integer> extension, 
                                Integer v) {
        // If we found a set of vertices that match the required subgraph size,
        // create a snapshot of it from the original graph and 
        if (subgraph.size() == subgraphSize) {
            Graph<T> sub = g.subgraph(subgraph);
            nextSubgraphs.add(sub);
            return;
        }

        // Randomly decide whether this portion of the subgraph search space
        // will be explored.
        int depth = subgraph.size();
        double explorationProbability = traversalProbabilitiesAtDepth[depth];
        int numChildren = extension.size();
        BitSet childrenToExplore = null;        

        // Special case for when we plan to explore all the children.  This
        // frequently happens in shallow depths where pruning would remove too
        // many children to be effective
        if (explorationProbability == 1d) {
            childrenToExplore = new BitSet(numChildren);
            // Mark all the children as needing to be explored.
            childrenToExplore.set(0, numChildren);
        }
        // Special case for when the extension has no children to set, at which
        // the tree-based traversal can just stop early.
        else if (numChildren == 0) {
            childrenToExplore = new BitSet(numChildren);
        }
        else {
            // Decide how many children to explore
            int numChildrenToExplore = 
                   (Math.random() <= (numChildren * explorationProbability) - 
                    Math.floor((numChildren * explorationProbability)))
                ? (int)(Math.ceil(numChildren * explorationProbability))
                : (int)(Math.floor(numChildren * explorationProbability));

            // Generate a bit mask over the children saying which of them will be
            // expanded.  We do this to avoid creating another Set<Integer>, which
            // wastes more space, or to just iterate until the sample size has been
            // reached, which isn't necessarily random.
            childrenToExplore = Statistics.
                randomDistribution(numChildrenToExplore, numChildren);
        }

        int child = 0;
        Iterator<Integer> iter = extension.iterator();
        while (extension.size() > 0) {
            // Choose and remove an aribitrary vertex from the extension            
            Integer w = iter.next();
            iter.remove();
            
            // If this child was not in the list of children that will be
            // randomly explored, then skip further processing here.
            if (!childrenToExplore.get(child++))
                continue;

            // The next extension is formed from all edges to vertices whose
            // indices are greater than the currently selected vertex, w, and
            // that point to a vertex in the exclusive neighborhood of w.  The
            // exclusive neighborhood is defined relative to a set of vertices
            // N: all vertices that are adjacent to w but are not in N or the
            // neighbors of N.  In this case, N is the current subgraph's
            // vertices
            Set<Integer> nextExtension = new HashSet<Integer>(extension);
            next_vertex:
            for (Integer n : g.getAdjacentVertices(w))
                // Perform the fast vertex value test and check for whether the
                // vertex is currently in the subgraph
                if (n > v && !subgraph.contains(n)) {
                    // Then perform the most expensive exclusive-neighborhood
                    // test that looks at the neighbors of the vertices in the
                    // current subgraph
                    for (int inCur : subgraph) {
                        // If we find n within the neighbors of a vertex in the
                        // current subgraph, then skip the remaining checks and
                        // examine another vertex adjacent to w.
                        if (g.getAdjacentVertices(inCur).contains(n))
                            continue next_vertex;
                    }
                    // Otherwise, n is in the exclusive neighborhood of w, so
                    // add it to the future extension.
                    nextExtension.add(n);
                }
            Set<Integer> nextSubgraph = new HashSet<Integer>(subgraph);
            nextSubgraph.add(w);
            
            extendSubgraph(nextSubgraph, nextExtension, v);
        }
    }

    public boolean hasNext() {
        return !nextSubgraphs.isEmpty();
    }

    public Graph<T> next() {
        if (nextSubgraphs.isEmpty()) 
            throw new NoSuchElementException();
        Graph<T> next = nextSubgraphs.poll();
        
        // If we've exhausted the current set of subgraphs, queue up more of
        // them, generated from the remaining vertices
        if (nextSubgraphs.isEmpty())
            advance();
        return next;
    }

    public void remove() {
        throw new UnsupportedOperationException(
            "Cannot remove subgraphs during iteration");
    }
}