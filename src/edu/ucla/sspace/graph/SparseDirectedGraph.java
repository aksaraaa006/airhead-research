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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.OpenIntSet;


/**
 * A undirected {@link Graph} implementation backed by a adjacency matrix.  This
 * class performs best for graphs with a small number of edges.
 *
 * @author David Jurgens
 */
public class SparseDirectedGraph extends AbstractGraph<DirectedEdge,SparseDirectedEdgeSet>
        implements DirectedGraph {

    private static final long serialVersionUID = 1L;

    public SparseDirectedGraph() { }
    
    /**
     * Creates a sparse edge set that treats all edges as symmetric.
     */
    @Override protected SparseDirectedEdgeSet createEdgeSet(int vertex) {
        return new SparseDirectedEdgeSet(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public int inDegree(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) ? 0 : edges.inEdges().size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedEdge> inEdges(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) ? null : new EdgeSetDecorator(edges.inEdges());
    }

    /**
     * {@inheritDoc}
     */
    public int outDegree(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) ? 0 : edges.outEdges().size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedEdge> outEdges(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) ? null : new EdgeSetDecorator(edges.outEdges());
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> predecessors(int vertex) {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> successors(int vertex) {
        throw new Error();
    }

    private class EdgeSetDecorator extends AbstractSet<DirectedEdge> {

        private final Set<DirectedEdge> edges;
        
        public EdgeSetDecorator(Set<DirectedEdge> edges) {
            this.edges = edges;
        }

        @Override public boolean add(DirectedEdge e) {
            // Rather than add the edge to the set directly, add it to the
            // graph, which will propagate the edge to the appropriate EdgeSet
            // instances.
            return addEdge(e);
        }

        @Override public boolean contains(Object o) {
            return edges.contains(o);
        }

        @Override public Iterator<DirectedEdge> iterator() {
            return new EdgeSetIteratorDecorator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof DirectedEdge))
                return false;
            // Rather than removing the edge to the set directly, removing it
            // from the graph, which will remove the edge froma the appropriate
            // EdgeSet instances.
            return removeEdge((DirectedEdge)o);
        }

        @Override public int size() {
            return edges.size();
        }

        private class EdgeSetIteratorDecorator 
                implements Iterator<DirectedEdge> {

            private final Iterator<DirectedEdge> iter;

            private boolean alreadyRemoved;

            public EdgeSetIteratorDecorator() {
                iter = edges.iterator();
                alreadyRemoved = true;
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public DirectedEdge next() {
                alreadyRemoved = false;
                return iter.next();
            }

            public void remove() {
                // REMINDER: I think this method would be extremely problematic
                // to actually implement.  The call to iter.remove() would leave
                // the symmetric edge in place in the AbstractGraph, while
                // calling removeEdge() would likely result in a concurrent
                // modification to the EdgeSet being iterated over, which may
                // have unpredictable results. Therefore, we just throw an
                // exception to indicate it's not supported until we can
                // identify a better implementation solution.  -david
                throw new UnsupportedOperationException();
            }
        }
    }

}
