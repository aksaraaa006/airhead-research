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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.CombinedSet;
import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.OpenIntSet;


/**
 * An {@link EdgeSet} implementation that stores {@link TypedEdge} instances for
 * a vertex.  This class provides additional methods beyond the {@code EdgeSet}
 * interface for interacting with edges on the basis of their type.
 */
public class SparseDirectedTypedEdgeSet<T> extends AbstractSet<DirectedTypedEdge<T>> 
        implements EdgeSet<DirectedTypedEdge<T>> {
        
    /**
     * The vertex to which all edges in the set are connected
     */
    private final int rootVertex;
    
    /**
     * A mapping from a type to the set of incoming edges
     */
    private final Map<T,OpenIntSet> typeToInEdges;

    /**
     * A mapping from a type to the set of outgoing edges
     */
    private final Map<T,OpenIntSet> typeToOutEdges;
        
    /**
     * Creates a new {@code SparseDirectedTypedEdgeSet} for the specfied vertex.
     */
    public SparseDirectedTypedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        typeToInEdges = new HashMap<T,OpenIntSet>();
        typeToOutEdges = new HashMap<T,OpenIntSet>();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(DirectedTypedEdge<T> e) {
        if (e.from() == rootVertex) {
            OpenIntSet edges = getOutEdgesForType(e.edgeType());
            return edges.add(e.to());
        }
        else if (e.to() == rootVertex) {
            OpenIntSet edges = getInEdgesForType(e.edgeType());
            return edges.add(e.from());
        }
        return false;
    }

    /**
     * {@inheritDoc}  The set of vertices returned by this set is immutable.
     */
    public Set<Integer> connected() {
        int setSize = typeToInEdges.size() + typeToOutEdges.size();
        if (setSize == 0)
            return Collections.<Integer>emptySet();
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>(setSize);
        for (Set<Integer> s : typeToInEdges.values())
            sets.add(s);
        for (Set<Integer> s : typeToOutEdges.values())
            sets.add(s);
        return new CombinedSet<Integer>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        for (OpenIntSet edges : typeToInEdges.values()) {
            if (edges.contains(vertex))
                return true;
        }
        for (OpenIntSet edges : typeToOutEdges.values()) {
            if (edges.contains(vertex))
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (!(o instanceof DirectedTypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;
        if (e.from() == rootVertex) {
            OpenIntSet edges = getOutEdgesForType(e.edgeType());
            return edges.contains(e.to());
        }
        else if (e.to() == rootVertex) {
            OpenIntSet edges = getInEdgesForType(e.edgeType());
            return edges.contains(e.from());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex) {
        return new VertexEdgeSet(vertex);
    }    

    /**
     * Returns the set of incoming edges that have the specified type.
     */
    private OpenIntSet getInEdgesForType(T type) {
        OpenIntSet edges = typeToInEdges.get(type);
        if (edges == null) {
            edges = new OpenIntSet();
            typeToInEdges.put(type, edges);
        }
        return edges;
    }

    /**
     * Returns the set of outgoing edges that have the specified type.
     */
    private OpenIntSet getOutEdgesForType(T type) {
        OpenIntSet edges = typeToOutEdges.get(type);
        if (edges == null) {
            edges = new OpenIntSet();
            typeToOutEdges.put(type, edges);
        }
        return edges;
    }

    /**
     * {@inheritDoc}
     */
    public int getRoot() {
        return rootVertex;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return typeToInEdges.isEmpty() && typeToOutEdges.isEmpty();
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<DirectedTypedEdge<T>> iterator() {
        return new DirectedTypedEdgeIterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (!(o instanceof DirectedTypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;
           
        if (e.from() == rootVertex) {
            OpenIntSet edges = getOutEdgesForType(e.edgeType());
            boolean b = edges.remove(e.to());
            // If this was the last edge of that type, remove the type
            if (b && edges.isEmpty())
                typeToOutEdges.remove(e.edgeType());
            return b;
        }
        else if (e.to() == rootVertex) {
            OpenIntSet edges = getInEdgesForType(e.edgeType());
            boolean b = edges.remove(e.from());
            // If this was the last edge of that type, remove the type
            if (b && edges.isEmpty())
                typeToInEdges.remove(e.edgeType());
            return b;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        int sz = 0;
        for (OpenIntSet edges : typeToInEdges.values())
            sz += edges.size();
        for (OpenIntSet edges : typeToOutEdges.values())
            sz += edges.size();
        return sz;
    }

    /**
     * Returns the set of types contained within this set
     */
    public Set<T> types() {
        Set<T> t = new HashSet<T>(typeToInEdges.keySet());
        t.addAll(typeToOutEdges.keySet());
        return t;
    }

    /**
     * A wrapper around the set of edges that connect another vertex to the root
     * vertex
     */
    private class VertexEdgeSet extends AbstractSet<DirectedTypedEdge<T>> {
        
        /**
         * The vertex in the edges that is not this root vertex
         */
        private final int otherVertex;

        public VertexEdgeSet(int otherVertex) {
            this.otherVertex = otherVertex;
        }

        @Override public boolean add(DirectedTypedEdge<T> e) {
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseDirectedTypedEdgeSet.this.add(e);
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof DirectedTypedEdge))
                return false;
            DirectedTypedEdge<?> e = (DirectedTypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseDirectedTypedEdgeSet.this.contains(e);
        }

        @Override public boolean isEmpty() {
            for (OpenIntSet in : typeToInEdges.values()) {
                if (in.contains(otherVertex))
                    return false;
            }
            for (OpenIntSet out : typeToOutEdges.values()) {
                if (out.contains(otherVertex))
                    return false;
            }    
            return true;
        }

        @Override public Iterator<DirectedTypedEdge<T>> iterator() {
            return new EdgeIterator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof DirectedTypedEdge))
                return false;
            DirectedTypedEdge<?> e = (DirectedTypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseDirectedTypedEdgeSet.this.remove(e);
        }

        @Override public int size() {
            int size = 0;
            for (OpenIntSet in : typeToInEdges.values()) {
                if (in.contains(otherVertex))
                    size++;
            }
            for (OpenIntSet out : typeToOutEdges.values()) {
                if (out.contains(otherVertex))
                    size++;
            }    
            return size;
        }

        /**
         * An iterator over all the edges that connect the root vertex to a
         * single vertex
         */
        class EdgeIterator implements Iterator<DirectedTypedEdge<T>> {
            
            /**
             * The types that may correspond to in edges
             */
            Iterator<Map.Entry<T,OpenIntSet>> inEdges;

            /**
             * The types that may correspond to in edges
             */
            Iterator<Map.Entry<T,OpenIntSet>> outEdges;

            /**
             * The next edge to return
             */ 
            DirectedTypedEdge<T> next;

            /**
             * The edge that was most recently returned or {@code null} if the
             * edge was removed or has yet to be returned.
             */
            DirectedTypedEdge<T> cur;

            public EdgeIterator() {
                inEdges = typeToInEdges.entrySet().iterator();
                outEdges = typeToOutEdges.entrySet().iterator();
                advance();
            }

            private void advance() {
                next = null;
                while (inEdges.hasNext() && next == null) {
                    Map.Entry<T,OpenIntSet> e = inEdges.next();
                    if (e.getValue().contains(otherVertex))
                        next = new SimpleDirectedTypedEdge<T>(
                           e.getKey(), otherVertex, rootVertex);
                }
                while (next == null && outEdges.hasNext()) {
                    Map.Entry<T,OpenIntSet> e = outEdges.next();
                    if (e.getValue().contains(otherVertex))
                        next = new SimpleDirectedTypedEdge<T>(
                            e.getKey(), rootVertex, otherVertex);
                }
            }

            public boolean hasNext() {
                return next != null;
            }

            public DirectedTypedEdge<T> next() {
                cur = next;
                advance();
                return cur;
            }

            public void remove() {
                if (cur == null)
                    throw new IllegalStateException();
                SparseDirectedTypedEdgeSet.this.remove(cur);
                cur = null;
            }
        }
    }

    /**
     * A wrapper around the set of {@link DirectedTypedEdge} instances that
     * either point to the root (the in-edges) or originate from the root
     * (out-edges).  This class is a utility to expose both edges sets while
     * allowing modifications to the returned sets to be reflected in this
     * {@code SparseTypedDirectedEdgeSet}.
     *
     * @see #inEdges()
     * @see #outEdges()
     */
    private class EdgeSetWrapper extends AbstractSet<DirectedTypedEdge<T>> {
        
        /**
         * The set of vertices linked to the root vertex according the {@code
         * areInEdge} property
         */
        private final Map<T,Set<Integer>> vertices;

        /**
         * {@code true} if the edges being wraped are in-edges (point to the
         * root), or {@code false} if the edges are out-edges (originate from
         * the root).
         */
        private final boolean areInEdges;

        public EdgeSetWrapper(Map<T,Set<Integer>> vertices, boolean areInEdges) {
            this.vertices = vertices;
            this.areInEdges = areInEdges;
        }

        @Override public boolean add(DirectedTypedEdge<T> e) {
            if (areInEdges) {
                return e.to() == rootVertex
                    && SparseDirectedTypedEdgeSet.this.add(e);
            }
            else {
                return e.from() == rootVertex
                    && SparseDirectedTypedEdgeSet.this.add(e);
            }
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof DirectedTypedEdge))
                return false;
            DirectedTypedEdge<?> e = (DirectedTypedEdge)o;
            if (areInEdges) {
                return e.to() == rootVertex
                    && SparseDirectedTypedEdgeSet.this.contains(e);
            }
            else {
                return e.from() == rootVertex
                    && SparseDirectedTypedEdgeSet.this.contains(e);
            }
        }

        @Override public boolean isEmpty() {
            int size = 0;
            for (Set<Integer> s : ((areInEdges) 
                      ? typeToInEdges.values() : typeToOutEdges.values())) {
                if (!s.isEmpty())
                    return false;
            }
            return true;
        }

        @Override public Iterator<DirectedTypedEdge<T>> iterator() {
            Map<T,OpenIntSet> m = (areInEdges) ? typeToInEdges: typeToOutEdges;
            Collection<Iterator<DirectedTypedEdge<T>>> iters = 
                new ArrayList<Iterator<DirectedTypedEdge<T>>>(m.size());
            for (Map.Entry<T,OpenIntSet> e : m.entrySet())
                iters.add(new EdgeSetWrapperIterator(e.getKey(), 
                                                     e.getValue().iterator()));
            return new CombinedIterator<DirectedTypedEdge<T>>(iters);
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof DirectedTypedEdge))
                return false;
            DirectedTypedEdge<?> e = (DirectedTypedEdge)o;
            if (areInEdges) {
                return e.to() == rootVertex
                    && SparseDirectedTypedEdgeSet.this.remove(e);
            }
            else {
                return e.from() == rootVertex
                    && SparseDirectedTypedEdgeSet.this.remove(e);
            }
        }

        @Override public int size() {
            int size = 0;
            for (Set<Integer> s : ((areInEdges) 
                      ? typeToInEdges.values() : typeToOutEdges.values())) {
                size += s.size();
            }
            return size;
        }

        public class EdgeSetWrapperIterator 
                implements Iterator<DirectedTypedEdge<T>> {
            
            private final Iterator<Integer> iter;
            
            /**
             * The type of edges returned by this iterator
             */
            private final T curType;

            public EdgeSetWrapperIterator(T curType, Iterator<Integer> iter) {
                this.iter = iter;
                this.curType = curType;
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public DirectedTypedEdge<T> next() {
                return (areInEdges) 
                    ? new SimpleDirectedTypedEdge<T>(curType, iter.next(),
                                                     rootVertex)
                    : new SimpleDirectedTypedEdge<T>(curType, rootVertex, 
                                                     iter.next());
            }

            public void remove() {
                iter.remove();
            }
        }

    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class DirectedTypedEdgeIterator implements Iterator<DirectedTypedEdge<T>> {

        /**
         * An iterator over the types being returned
         */
        private Iterator<T> typeIter;
        
        /**
         * An iterator over the incoming edges for the current type
         */
        private Iterator<Integer> curInEdges;

        /**
         * An iterator over the outgoing edges for the current type
         */
        private Iterator<Integer> curOutEdges;

        /**
         * The current edge type being returned
         */
        private T curType;

        /**
         * The iterator on which remove() should be called
         */
        private Iterator<Integer> toRemoveFrom;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;
        
        public DirectedTypedEdgeIterator() {
            typeIter = types().iterator();
            advance();
        }

        private void advance() {
            next = null;           

            // Loop until we find an edge to return or we run out of information
            // for constructing edges
            do {
                // If we have an in-edge
                if (curInEdges != null && curInEdges.hasNext()) {
                    toRemoveFrom = curInEdges;
                    next = new SimpleDirectedTypedEdge<T>(
                        curType, curInEdges.next(), rootVertex);
                }
                // If we have an out-edge
                else if (curOutEdges != null && curOutEdges.hasNext()) {
                    toRemoveFrom = curOutEdges;
                    next = new SimpleDirectedTypedEdge<T>(
                        curType, rootVertex, curOutEdges.next());
                }
                // If we have neither an in-edge or an out-edge for the current
                // type, but still have more types for which there are edges
                else if (typeIter.hasNext()) {
                    curType = typeIter.next();
                    OpenIntSet in = typeToInEdges.get(curType);
                    OpenIntSet out = typeToOutEdges.get(curType);
                    curInEdges = (in == null) ? null : in.iterator();
                    curOutEdges = (out == null) ? null : out.iterator();
                }
                // In the base case, we have no edge iterators and the type
                // iterator is out of elements, so we can stop looking
                else
                    break;
            } while (next == null);
        }

        public boolean hasNext() {
            return next != null;
        }

        public DirectedTypedEdge<T> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            DirectedTypedEdge<T> n = next;
            advance();
            return n;
        }

        public void remove() {
            if (toRemoveFrom == null)
                throw new IllegalStateException("No element to remove");
            toRemoveFrom.remove();
        }
    }

//     /**
//      *
//      */
//     private class CombinedSet extends AbstractSet<Integer> {

//         @Override public boolean contains(Object o) {
//             if (!(o instanceof Integer))
//                 return false;
//             Integer i = (Integer)o;
//             for (OpenIntSet s : typeToInEdges.values())
//                 if (s.contains(i))
//                     return true;
//             for (OpenIntSet s : typeToOutEdges.values())
//                 if (s.contains(i))
//                     return true;
//             return false;
//         }

//         @Override public Iterator<Integer> iterator() {
//             OpenIntSet combined = new OpenIntSet();
//             for (OpenIntSet s : typeToInEdges.values())
//                 combined.addAll(s);
//             for (OpenIntSet s : typeToOutEdges.values())
//                 combined.addAll(s);
//             return Collections.unmodifiableSet(combined).iterator();
//         }

//         @Override public int size() {
//             OpenIntSet combined = new OpenIntSet();
//             for (OpenIntSet s : typeToInEdges.values())
//                 combined.addAll(s);
//             for (OpenIntSet s : typeToOutEdges.values())
//                 combined.addAll(s);
//             return combined.size();
//         }
//     }
}