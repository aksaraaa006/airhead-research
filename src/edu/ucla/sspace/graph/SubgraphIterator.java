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

import edu.ucla.sspace.util.OpenIntSet;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;


/**
 * An implementation of the EnumerateSubgraphs (ESU) method from Wernicke
 * (2006).  For full details see: <ul>
 *
 * <li style="font-family:Garamond, Georgia, serif"> Sebastian
 *   Wernicke. Efficient detection of network motifs. <i>in</i> IEEE/ACM
 *   Transactions on Computational Biology and Bioinformatics.
 * </li>
 * </ul>
 *
 * In summary, this iterator returns all possible size-<i>k</i> subgraphs of the
 * input graph through an efficient traversal.
 *
 * <p> This implementation does not store the entire set of possible subgraphs
 * in memory at one time, but may hold some arbitrary number of them in memory
 * and compute the rest as needed.
 *
 * <p> This class is not thread-safe and does not support the {@link #remove()}
 * method.
 *
 * @author David Jurgens
 */
public class SubgraphIterator<E extends Edge,G extends Graph<E>> 
    implements Iterator<G>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The graph whose subgraphs are being iterated
     */
    private final G g;

    /**
     * The size of the subgraph to return
     */
    private final int subgraphSize;

    /**
     * An interator over the vertices of {@code g}.
     */
    private Iterator<Integer> vertexIter;

    /**
     * An internal queue of the next sequence of subgraphs to return.  This
     * queue is periodically filled through expanding the next series of
     * subgraphs for a vertex.
     */
    private Queue<G> nextSubgraphs;

    /**
     * Constructs an iterator over all the subgraphs of {@code g} with the
     * specified subgraph size.
     *
     * @param g a graph
     * @param subgraphSize the size of the subgraphs to return
     *
     * @throws IllegalArgumentException if subgraphSize is less than 1 or is
     *         greater than the number of vertices of {@code g}
     * @throws NullPointerException if {@code g} is {@code null}
     */
    public SubgraphIterator(G g, int subgraphSize) {
        this.g = g;
        this.subgraphSize = subgraphSize;
        if (g == null)
            throw new NullPointerException();
        if (subgraphSize < 1)
            throw new IllegalArgumentException("size must be positive");
        if (subgraphSize > g.order())
            throw new IllegalArgumentException("size must not be greater " 
                + "than the number of vertices in the graph");
        vertexIter = g.vertices().iterator();
        nextSubgraphs = new ArrayDeque<G>();
        advance();
    }

    /**
     * If the {@code nextSubgraphs} queue is empty, expands the graph frontier
     * of the next available vertex, if one exists, and the subgraphs reachable
     * from it to the queue.
     */
    private void advance() {
        while (nextSubgraphs.isEmpty() && vertexIter.hasNext()) {
            Integer nextVertex = vertexIter.next();
            // Determine the set of vertices that are greater than this vertex
            // Set<Integer> extension = new HashSet<Integer>();
            Set<Integer> neighbors = g.getNeighbors(nextVertex);
            OpenIntSet extension = new OpenIntSet(); //neighbors.size());
            for (Integer v : neighbors)
                if (v > nextVertex)
                    extension.add(v);
            OpenIntSet subgraph = new OpenIntSet();
            subgraph.add(nextVertex);
            extendSubgraph(subgraph, extension, nextVertex);
        }
    }

    /**
     * For the set of vertices in {@code subgraph}, and the next set of
     * reachable vertices in {@code extension}, creates the non-duplicated
     * subgraphs and adds them to {@code nextSubgraphs}.
     *
     * @param subgraph the current set of vertices making up the subgraph
     * @param extension the set of vertices that may be added to {@code
     *        subgraph} to expand the current subgraph
     * @param v the vertex from which the next expansion will take place
     */
    private void extendSubgraph(OpenIntSet subgraph, OpenIntSet extension, 
                                Integer v) {
        // If we found a set of vertices that match the required subgraph size,
        // create a snapshot of it from the original graph and 
        if (subgraph.size() == subgraphSize) {
            // The return type of subgraph() isn't parameterized on the type of
            // the graph itself.  However, we know that all the current
            // interfaces confirm to the convention that the type is refined
            // (narrowed, really), so we perform the cast here to give the user
            // back the more specific type.
            @SuppressWarnings("unchecked")
            G sub = (G)g.copy(subgraph);
            // System.out.printf("Made subgraph of vertices %s: %s%n", subgraph, sub);
            nextSubgraphs.add(sub);
            return;
        }
        Iterator<Integer> iter = extension.iterator();
        while (extension.size() > 0) {
            // Choose and remove an aribitrary vertex from the extension            
            Integer w = iter.next();
            iter.remove();

            // The next extension is formed from all edges to vertices whose
            // indices are greater than the currently selected vertex, w, and
            // that point to a vertex in the exclusive neighborhood of w.  The
            // exclusive neighborhood is defined relative to a set of vertices
            // N: all vertices that are adjacent to w but are not in N or the
            // neighbors of N.  In this case, N is the current subgraph's
            // vertices
            OpenIntSet nextExtension = new OpenIntSet(extension);

            next_vertex:
            for (Integer n : g.getNeighbors(w))
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
                        // if (g.getNeighbors(inCur).contains(n))
                        if (g.contains(inCur, n))
                            continue next_vertex;
                    }
                    // Otherwise, n is in the exclusive neighborhood of w, so
                    // add it to the future extension.
                    nextExtension.add(n);
                }
            OpenIntSet nextSubgraph = new OpenIntSet(subgraph);
            nextSubgraph.add(w);
            
            extendSubgraph(nextSubgraph, nextExtension, v);
        }
    }

    /**
     * Returns {@code true} if there are more subgraphs to return
     */
    public boolean hasNext() {
        return !nextSubgraphs.isEmpty();
    }

    /**
     * Returns the next subgraph from the backing graph.
     */ 
    public G next() {
        if (nextSubgraphs.isEmpty()) 
            throw new NoSuchElementException();
        G next = nextSubgraphs.poll();
        
        // If we've exhausted the current set of subgraphs, queue up more of
        // them, generated from the remaining vertices
        if (nextSubgraphs.isEmpty())
            advance();
        return next;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
        throw new UnsupportedOperationException(
            "Cannot remove subgraphs during iteration");
    }
}