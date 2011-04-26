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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.OpenIntSet;


/**
 *
 */
public class SparseDirectedEdgeSet extends AbstractSet<DirectedEdge> 
        implements EdgeSet<DirectedEdge> {
        
    private final int rootVertex;
    
    private final OpenIntSet outEdges;

    private final OpenIntSet inEdges;
    
    public SparseDirectedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        outEdges = new OpenIntSet();
        inEdges = new OpenIntSet();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(DirectedEdge e) {
        if (e.from() == rootVertex) {
            return outEdges.add(e.to());
        }
        else if (e.to() == rootVertex) {
            return inEdges.add(e.from());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> connected() {
        throw new Error("fixme");
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return inEdges.contains(vertex) || outEdges.contains(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (o instanceof DirectedEdge) {
            DirectedEdge e = (DirectedEdge)o;
            if (e.to() == rootVertex) 
                return inEdges.contains(e.from());
            else if (e.from() == rootVertex)
                return outEdges.contains(e.to());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedEdge> getEdges(int vertex) {
        Set<DirectedEdge> edges = new HashSet<DirectedEdge>();
        if (inEdges.contains(vertex))
            edges.add(new SimpleDirectedEdge(vertex, rootVertex));
        else if (outEdges.contains(vertex))
            edges.add(new SimpleDirectedEdge(rootVertex, vertex));
        return edges;
    }    

    /**
     * {@inheritDoc}
     */
    public int getRoot() {
        return rootVertex;
    }

    /**
     * Returns the set of {@link DirectedEdge} instances that point to the root
     * vertex.  Changes to this set will be reflected in this {@link EdgeSet}
     * and vice versa.
     */
    public Set<DirectedEdge> inEdges() {
        return new EdgeSetWrapper(inEdges, true);        
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<DirectedEdge> iterator() {
        return new DirectedEdgeIterator();
    }
    
    /**
     * Returns the set of {@link DirectedEdge} instances that originate from the
     * root vertex.  Changes to this set will be reflected in this {@link
     * EdgeSet} and vice versa.
     */
    public Set<DirectedEdge> outEdges() {
        return new EdgeSetWrapper(outEdges, false);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (o instanceof DirectedEdge) {
            DirectedEdge e = (DirectedEdge)o;
            if (e.to() == rootVertex) 
                return inEdges.remove(e.from());
            else if (e.from() == rootVertex)
                return outEdges.remove(e.to());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return inEdges.size() + outEdges.size();
    }

    private class EdgeSetWrapper extends AbstractSet<DirectedEdge> {
        
        private final Set<Integer> vertices;
        private final boolean areInEdges;

        public EdgeSetWrapper(Set<Integer> vertices, boolean areInEdges) {
            this.vertices = vertices;
            this.areInEdges = areInEdges;
        }

        @Override public boolean add(DirectedEdge e) {
            if (areInEdges) {
                return vertices.add(e.from());
            }
            else {
                assert e.from() == rootVertex : "incorrect edge set view";
                return vertices.add(e.to());
            }
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof DirectedEdge))
                return false;
            DirectedEdge e  = (DirectedEdge)o;
            if (areInEdges) {
                return vertices.contains(e.from());
            }
            else {
                assert e.from() == rootVertex : "incorrect edge set view";
                return vertices.contains(e.to());
            }                             
        }

        @Override public Iterator<DirectedEdge> iterator() {
            return new EdgeSetWrapperIterator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof DirectedEdge))
                return false;
            DirectedEdge e  = (DirectedEdge)o;
            if (areInEdges) {
                return vertices.remove(e.from());
            }
            else {
                assert e.from() == rootVertex : "incorrect edge set view";
                return vertices.remove(e.to());
            }                             
        }

        @Override public int size() {
            return vertices.size();
        }

        public class EdgeSetWrapperIterator implements Iterator<DirectedEdge> {
            
            private final Iterator<Integer> iter;

            public EdgeSetWrapperIterator() {
                iter = vertices.iterator();
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public DirectedEdge next() {
                return (areInEdges) 
                    ? new SimpleDirectedEdge(iter.next(), rootVertex)
                    : new SimpleDirectedEdge(rootVertex, iter.next());
            }

            public void remove() {
                iter.remove();
            }
        }

    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class DirectedEdgeIterator implements Iterator<DirectedEdge> {

        private Iterator<Integer> inVertices;

        private Iterator<Integer> outVertices;
        
        private Iterator<Integer> lastRemovedFrom;

        public DirectedEdgeIterator() {
            inVertices = inEdges.iterator();
            outVertices = outEdges.iterator();
            lastRemovedFrom = null;
        }

        public boolean hasNext() {
            return inVertices.hasNext() || inVertices.hasNext();
        }

        public DirectedEdge next() {
            if (!hasNext())
                throw new NoSuchElementException();
            int from = -1, to = -1;
            if (inVertices.hasNext()) {
                from = inVertices.next();
                to = rootVertex;
                lastRemovedFrom = inVertices;
            }
            else {
                assert outVertices.hasNext() : "bad iterator logic";
                from = rootVertex;
                to = outVertices.next();
                lastRemovedFrom = outVertices;
            }
            return new SimpleDirectedEdge(from, to);
        }

        public void remove() {
            lastRemovedFrom.remove();
        }
    }
}