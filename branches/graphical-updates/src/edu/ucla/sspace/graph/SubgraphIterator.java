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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;


/**
 * An implementation of the EnumerateSubgraphs method from Wernicke (2006).  For
 * full details see:
 * <ul>
 *
 * <li style="font-family:Garamond, Georgia, serif"> Sebastian
 *   Wernicke. Efficient detection of network motifs. <i>in</i> IEEE/ACM
 *   Transactions on Computational Biology and Bioinformatics.
 * </li>
 *
 * </ul> This implementation does not store the entire set of possible subgraphs
 * in memory at one time, but may hold some arbitrary number of them in memory
 * and compute the rest as needed.
 */
public class SubgraphIterator<T extends Edge> implements Iterator<Graph<T>> {

    private final Graph<T> g;

    private final int subgraphSize;

    private Iterator<Integer> vertexIter;

    private Queue<Graph<T>> nextSubgraphs;

    public SubgraphIterator(Graph<T> g, int subgraphSize) {
        this.g = g;
        this.subgraphSize = subgraphSize;
        vertexIter = g.vertices().iterator();
        nextSubgraphs = new ArrayDeque<Graph<T>>();
    }

    private void advance() {
        if (nextSubgraphs.isEmpty() && vertexIter.hasNext()) {
            Integer nextVertex = vertexIter.next();
            // Determine the set of vertices that are greater than this vertex
            Set<Integer> extension = new HashSet<Integer>();
            for (Integer v : g.vertices())
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
            nextSubgraphs.add(g.subgraph(subgraph));
            return;
        }
        Iterator<Integer> iter = extension.iterator();
        while (extension.size() > 0) {
            // Choose and remove an aribitrary vertex from the extension            
            Integer w = iter.next();
            iter.remove();

            Set<Integer> nextExtension = new HashSet<Integer>(extension);
            for (Integer n : g.getAdjacentVertices(w))
                if (n > v)
                    nextExtension.add(n);
            Set<Integer> nextSubgraph = new HashSet<Integer>(subgraph);
            nextSubgraph.add(w);
            
            extendSubgraph(nextSubgraph, nextExtension, v);
        }
    }

    public boolean hasNext() {
        return nextSubgraphs.isEmpty() && !vertexIter.hasNext();
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