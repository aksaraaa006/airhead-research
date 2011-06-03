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

import java.lang.ref.WeakReference;

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
import edu.ucla.sspace.util.IntSet;
import edu.ucla.sspace.util.OpenIntSet;

/**
 *
 *
 * @param T a class type whose values are used to distinguish between edge types
 * 
 * @author David Jurgens
 */
public class UndirectedMultigraph<T> 
        implements Multigraph<T,TypedEdge<T>>, java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * A mapping from an edge type to the graph that contains all edges with
     * that type.
     */
    private final Map<T,Graph<TypedEdge<T>>> typeToEdges;

    // private final IntSet vertices;
    private final Set<Integer> vertices;

    /**
     * An interal view of the total set of edges across each of the
     * type-specific graphs
     */
    private EdgeView edges;

    /**
     * A collection of all the subgraphs that have been returned from this
     * graph.  This collection is necessary to inform subgraphs of any vertex
     * changes (removals) to this graph without requiring them to constantly
     * check for updates.  We use a {@link WeakReference} in order to keep track
     * of the canonical {@code Subgraph} instance while ensuring that it is
     * garbage collected when it is no longer referred to (which would never
     * happen if this list contained strong references).
     */
    private Collection<WeakReference<Subgraph>> subgraphs;
    
    /**
     * Creates an empty graph with node edges
     */
    public UndirectedMultigraph() { 
        typeToEdges = new HashMap<T,Graph<TypedEdge<T>>>();
        // vertices = new IntSet();
        vertices = new HashSet<Integer>();
        edges = new EdgeView();
        subgraphs = new ArrayList<WeakReference<Subgraph>>();
    }

    /**
     * Creates a directed multigraph with a copy of all the vertices and edges
     * in {@code g}.
     */
    public UndirectedMultigraph(Graph<? extends TypedEdge<T>> g) {
        this();
        for (Integer v : g.vertices())
            add(v);
        for (TypedEdge<T> e : g.edges())
            add(e);
    }    

    /**
     * {@inheritDoc}
     */
    public boolean add(int vertex) {
        return vertices.add(vertex);
    }

    public boolean add(TypedEdge<T> e) {
        Graph<TypedEdge<T>> g = typeToEdges.get(e.edgeType());
        if (g == null) {
            g = new UndirectedTypedGraph<T>();
            typeToEdges.put(e.edgeType(), g);
            // Update the edge view so that it is aware of the edges that are
            // now present in this graph
            edges.update();
        }
        // If we've added a new edge, update the local state for the vertices
        // and edge types
        if (g.add(e)) {
            vertices.add(e.from());
            vertices.add(e.to());
            return true;
        }
        return false;
    }

    /**
     * InheritDoc
     */
    public void clear() {
        typeToEdges.clear();
        vertices.clear();
        edges.update();
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges() { 
        typeToEdges.clear();
        edges.update();
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges(T edgeType) { 
        typeToEdges.remove(edgeType);
        edges.update();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex) {
        return vertices.contains(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Edge e) {
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            if (g.contains(e))
                return true;
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2) {
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            if (g.contains(vertex1, vertex2))
                return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2, T edgeType) {
        Graph<TypedEdge<T>> g = typeToEdges.get(edgeType);
        if (g != null)
            return g.contains(vertex1, vertex2);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int degree(int vertex) {
        int degree = 0;
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            degree += g.degree(vertex);
        return degree;
    }

    /**
     * Returns the set of typed edges in the graph
     */
    public Set<TypedEdge<T>> edges() {
        return edges;
    }

    /**
     * Returns the set of edge types currently present in this graph.
     */
    public Set<T> edgeTypes() {
        return Collections.unmodifiableSet(typeToEdges.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getAdjacencyList(int vertex) {
        // By contract, if vertex is not in this graph, then we return null.
        // However, we go ahead and construct the sets first anyway, as they are
        // cheap to construct and the common case is to ask for vertices that
        // are in the graph
        List<Set<TypedEdge<T>>> sets = 
            new ArrayList<Set<TypedEdge<T>>>();
        for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
            Set<TypedEdge<T>> adj = g.getAdjacencyList(vertex);
            if (adj != null)
                sets.add(adj);
        }
        return (sets.isEmpty()) ? null : 
            new CombinedSet<TypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(int vertex1, int vertex2) {
        // By contract, if the two vertices do not have any edges in this graph,
        // then we return null.  However, we go ahead and construct the sets
        // first anyway, as they are cheap to construct and the common case is
        // to ask for vertices that are in the graph
        List<Set<TypedEdge<T>>> sets = 
            new ArrayList<Set<TypedEdge<T>>>();
        for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
            Set<TypedEdge<T>> e = g.getEdges(vertex1, vertex2);
            if (e != null) {
                sets.add(e);
            }
        }
        return (sets.isEmpty()) ? null : 
            new CombinedSet<TypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getNeighbors(int vertex) {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
            Set<Integer> n = g.getNeighbors(vertex);
            if (n != null)
                sets.add(n);
        }
        return (sets.isEmpty()) ? null : new CombinedSet<Integer>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCycles() {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public int order() {
        return vertices.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int vertex) {
        // If we can remove the vertex from the global set, then at least one of
        // the type-specific graphs has this vertex.
        if (vertices.remove(vertex)) {
            Iterator<Map.Entry<T,Graph<TypedEdge<T>>>> it = 
                typeToEdges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<T,Graph<TypedEdge<T>>> e = it.next();
                Graph<TypedEdge<T>> g = e.getValue();
                // Check whether removing this vertex has caused us to remove
                // the last edge for this type in the graph.  If so, the graph
                // no longer has this type and we need to update the state.
                if (g.remove(vertex) && g.size() == 0) {
                    // Get rid of the type mapping
                    it.remove(); 
                    // Update the state of the edge view
                    edges.update();
                }
            }
            // Update any of the subgraphs that had this vertex to notify them
            // that it was removed
            Iterator<WeakReference<Subgraph>> iter = subgraphs.iterator();
            while (iter.hasNext()) {
                WeakReference<Subgraph> ref = iter.next();
                Subgraph s = ref.get();
                // Check whether this subgraph was already gc'd (the subgraph
                // was no longer in use) and if so, remove the ref from the list
                // to avoid iterating over it again
                if (s == null) {
                    iter.remove();
                    continue;
                }
                // If we removed the vertex from the subgraph, then check
                // whether we also removed any of the types in that subgraph
                if (s.vertexSubset.remove(vertex)) {
                    Iterator<T> types = s.validTypes.iterator();
                    while (types.hasNext()) {
                        if (!typeToEdges.containsKey(types.next()))
                            types.remove();
                    }
                }
            }
            return true;
        }
        return false;        
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(TypedEdge<T> edge) {
        Iterator<Map.Entry<T,Graph<TypedEdge<T>>>> it = 
            typeToEdges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T,Graph<TypedEdge<T>>> e = it.next();
            Graph<TypedEdge<T>> g = e.getValue();
            if (g.remove(edge)) {
                // Check whether we've just removed the last edge for this type
                // in the graph.  If so, the graph no longer has this type and
                // we need to update the state.
                if (g.size() == 0) {
                    // Get rid of the type mapping
                    it.remove(); 
                    // Update the state of the edge view
                    edges.update();

                    // Remove this edge type from all the subgraphs as well
                    Iterator<WeakReference<Subgraph>> sIt = subgraphs.iterator();
                    while (sIt.hasNext()) {
                        WeakReference<Subgraph> ref = sIt.next();
                        Subgraph s = ref.get();
                        // Check whether this subgraph was already gc'd (the
                        // subgraph was no longer in use) and if so, remove the
                        // ref from the list to avoid iterating over it again
                        if (s == null) {
                            sIt.remove();
                            continue;
                        }
                        s.validTypes.remove(e.getKey());
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        int size = 0;
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            size += g.size();
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> subset) {
         Subgraph sub = new Subgraph(typeToEdges.keySet(), subset);
         subgraphs.add(new WeakReference<Subgraph>(sub));
         return sub;
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> subset, 
                                               Set<T> edgeTypes) {
         Subgraph sub = new Subgraph(edgeTypes, subset);
         subgraphs.add(new WeakReference<Subgraph>(sub));
         return sub;
     }     

    /**
     * Returns a description of the graph as the sequence of its edges.
     */
    public String toString() {
        // REMINDER: make this more efficient with a StringBuilder
        return "{ vertices: " + vertices() + ", edges: " + edges() + "}";
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> vertices() {
        return Collections.unmodifiableSet(vertices);
    }


    /**
     * A graph that imposes no restriction on the types of edges that may connect
     * its vertices.
     */
    class UndirectedTypedGraph<T> extends AbstractGraph<TypedEdge<T>,
                                                SparseTypedEdgeSet<T>> {
        
        private static final long serialVersionUID = 1L;
        
        public UndirectedTypedGraph() { }
                
        @Override protected SparseTypedEdgeSet<T> 
                createEdgeSet(int vertex) {
            return new SparseTypedEdgeSet<T>(vertex);
        }        
    }

    /**
     * The {@code EdgeView} class encapsulates all the of the edge sets from the
     * different edge types.  Because {@link CombinedSet} does not allow for
     * addition, we add an {@code add} method that handles adding new edges to
     * the graph via this set.
     */
    class EdgeView extends AbstractSet<TypedEdge<T>> {

        private CombinedSet<TypedEdge<T>> backing;

        public EdgeView() {
            update();
        }

        public boolean add(TypedEdge<T> e) {
            // Calling the backing graph's add will route the edge to the
            // type-appropriate set
            return UndirectedMultigraph.this.add(e);
        }

        public boolean contains(Object o) {
            return backing.contains(o);
        }

        public Iterator<TypedEdge<T>> iterator() {
            return backing.iterator();
        }


        public boolean remove(Object o) {
            return backing.remove(o);
        }

        public int size() {
            return backing.size();
        }

        /**
         * Completely updates the set of edges maintained by this view by
         * iterating over the type-graphs and storing their corresponding edge
         * sets.
         */
        private void update() {
            List<Set<TypedEdge<T>>> sets = 
                new ArrayList<Set<TypedEdge<T>>>();
            for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
                sets.add(g.edges());
            }
            backing = new CombinedSet<TypedEdge<T>>(sets);
        }
    }

    /**
     * An implementation for handling the subgraph behavior.
     */
     class Subgraph extends UndirectedMultigraph<T> {

        private static final long serialVersionUID = 1L;

        /**
         * The set of types in this subgraph
         */
        private final Set<T> validTypes;

        /**
         * The set of vertices in this subgraph
         */
        private final Set<Integer> vertexSubset;

        public Subgraph(Set<T> validTypes, Set<Integer> vertexSubset) {
            this.validTypes = validTypes;
            this.vertexSubset = new OpenIntSet(vertexSubset);
        }

        /**
         * {@inheritDoc}
         */
        public boolean add(int vertex) {
            if (vertexSubset.contains(vertex))
                return false;
            throw new UnsupportedOperationException(
                "Cannot add new vertex to subgraph");
        }

        /**
         * {@inheritDoc}
         */
        public boolean add(TypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                throw new UnsupportedOperationException(
                    "Cannot add new vertex to subgraph");
            return UndirectedMultigraph.this.add(e);
        }

        /**
         * {@inheritDoc}
         */        
        public void clear() {
            for (Integer v : vertexSubset)
                UndirectedMultigraph.this.remove(v);
            vertexSubset.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void clearEdges() { 
            for (Integer v : vertexSubset) {
                // Only consider removing edges of the valid types
                for (T type : validTypes) {
                    // Get all the edges for the current vertex for that type
                    Graph<TypedEdge<T>> g = 
                        typeToEdges.get(type);
                    // Check whether each of the adjacent edges points to
                    // another vertex in this subgraph.  If so, remove it
                    for (TypedEdge<T> e : g.getAdjacencyList(v)) {
                        int from = e.from();
                        int to = e.to();
                        // Check the other vertex to be in this subgraph
                        if ((from == v && vertexSubset.contains(to))
                                || (to == v && vertexSubset.contains(from)))
                            g.remove(e);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clearEdges(T edgeType) { 
            // Get all the edges for the current vertex for the type
            Graph<TypedEdge<T>> g = typeToEdges.get(edgeType);
            for (Integer v : vertexSubset) {
                // Check whether each of the adjacent edges points to another vertex
                // in this subgraph.  If so, remove it
                for (TypedEdge<T> e : g.getAdjacencyList(v)) {
                    int from = e.from();
                    int to = e.to();
                    // Check the other vertex to be in this subgraph
                    if ((from == v && vertexSubset.contains(to))
                            || (to == v && vertexSubset.contains(from)))
                        g.remove(e);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex) {
            return vertexSubset.contains(vertex);
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(Edge e) {
            if (e instanceof TypedEdge) {
                TypedEdge<?> te = (TypedEdge<?>)e;
                if (!validTypes.contains(te.edgeType()))
                    return false;
                if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.from()))
                    return false;
                return UndirectedMultigraph.this.contains(e);
            }
            return false;
        }
    
        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex1, int vertex2) {
            if (!vertexSubset.contains(vertex1)
                    || !vertexSubset.contains(vertex2))
                return false;

            Set<TypedEdge<T>> edges = 
                UndirectedMultigraph.this.getEdges(vertex1, vertex2);
            if (edges != null) {
                // If there were edges between the two vertices, ensure that at
                // least one of them has the type represented in this subgraph
                for (TypedEdge<T> e : edges)
                    if (validTypes.contains(e.edgeType()))
                        return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex1, int vertex2, T edgeType) {
            if (!vertexSubset.contains(vertex1)
                    || !vertexSubset.contains(vertex2))
                return false;

            Graph<TypedEdge<T>> g = typeToEdges.get(edgeType);
            return g.contains(vertex1, vertex2);
        }

        /**
         * {@inheritDoc}
         */
        public int degree(int vertex) {
            int degree = 0;
            for (T type : validTypes) {
                Graph<TypedEdge<T>> g = typeToEdges.get(type);
                degree += g.degree(vertex);
            }
            return degree;
        }

        /**
         * Returns the set of typed edges in the graph
         */
        public Set<TypedEdge<T>> edges() {
            return new SubgraphEdgeView();
        }

        /**
         * Returns the set of edge types currently present in this graph.
         */
        public Set<T> edgeTypes() {
            return Collections.unmodifiableSet(validTypes);
        }

        /**
         * {@inheritDoc}
         */
        public Set<TypedEdge<T>> getAdjacencyList(int vertex) {
            if (!vertexSubset.contains(vertex))
                return null;
            Set<TypedEdge<T>> adj = 
                UndirectedMultigraph.this.getAdjacencyList(vertex);
            return (adj == null) ? null
                : new SubgraphAdjacencyListView(vertex, adj);
        }

        /**
         * {@inheritDoc}
         */
        public Set<TypedEdge<T>> getEdges(int vertex1, int vertex2) {
            List<Set<TypedEdge<T>>> sets = 
                new ArrayList<Set<TypedEdge<T>>>();
            for (T type : validTypes) {
                Graph<TypedEdge<T>> g = typeToEdges.get(type);
                Set<TypedEdge<T>> edges = g.getEdges(vertex1, vertex2);
                if (edges != null)
                    sets.add(edges);
            }
            return (sets.isEmpty()) 
                ? null : new CombinedSet<TypedEdge<T>>(sets);
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> getNeighbors(int vertex) {
            if (!vertexSubset.contains(vertex))
                return null;
            Set<Integer> neighbors = 
                UndirectedMultigraph.this.getNeighbors(vertex);
            return new SubgraphNeighborsView(vertex, neighbors);
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasCycles() {
            throw new Error();
        }

        /**
         * Returns {@code true} if the two vertices are contained within this
         * subgraph and have an edge that is valid within this subgraph's type
         * constraints.
         */
        private boolean hasEdge(int v1, int v2) {
            if (vertexSubset.contains(v1) && vertexSubset.contains(v2)) {
                Set<TypedEdge<T>> edges = getEdges(v1, v2);
                return edges != null;
            }
            return false;            
        }

        /**
         * {@inheritDoc}
         */
        public int order() {
            return vertexSubset.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(int vertex) {
            throw new UnsupportedOperationException(
                "Cannot remove vertices from a subgraph");
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(TypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                return false;
            return UndirectedMultigraph.this.remove(e);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            int size = 0;
            for (TypedEdge<T> e : edges()) {
                size++;
            }
            return size;
        }

        /**
         * {@inheritDoc}
         */
        public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> verts) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            return new Subgraph(validTypes, verts);
        }

        /**
         * {@inheritDoc}
         */
        public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> verts, 
                                                   Set<T> edgeTypes) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            if (!validTypes.containsAll(edgeTypes))
                throw new IllegalArgumentException("provided types is not a " +
                    "subset of the edge types of this graph");
            return new Subgraph(edgeTypes, verts);
        }
     
        /**
         * {@inheritDoc}
         */
        public Set<Integer> vertices() {
            // Check that the vertices are up to date with the backing graph
            return Collections.unmodifiableSet(vertexSubset);
        }

        /**
         * A view for the {@code Edge} adjacent edges of a vertex within a
         * subgraph.  This class monitors for changes to edge set to update the
         * state of this graph
         */
        private class SubgraphAdjacencyListView 
                extends AbstractSet<TypedEdge<T>> {

            /**
             * The adjacency list of edges in the backing graph.
             */
            private final Set<TypedEdge<T>> adjacencyList;
            
            /**
             * The root vertex in the subgraph.
             */ 
            private final int root;

            public SubgraphAdjacencyListView(int root,
                    Set<TypedEdge<T>> adjacencyList) {
                this.root = root;
                this.adjacencyList = adjacencyList;                
            }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(TypedEdge<T> edge) {
                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.add(edge);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Edge))
                    return false;
                Edge e = (Edge)o;
                return vertexSubset.contains(e.to()) 
                    && vertexSubset.contains(e.from())
                    && adjacencyList.contains(e);
            }
            
            public Iterator<TypedEdge<T>> iterator() {
                return new SubgraphAdjacencyListIterator();
            }
            
            /**
             * Removes the edge, if present in this subgraph
             */
            public boolean remove(Object o) {
                if (!(o instanceof TypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                TypedEdge<T> edge = (TypedEdge<T>)o;

                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.remove(edge);
            }

            public int size() {
                int sz = 0;
                Iterator<TypedEdge<T>> it = iterator();
                while (it.hasNext()) {
                    it.next();
                    sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for the adjacency list for a
             * vertex in a subgraph, which tracks edges removal to update the
             * number of edges in the graph.
             */
            private class SubgraphAdjacencyListIterator 
                    implements Iterator<TypedEdge<T>> {
                
                private final Iterator<TypedEdge<T>> edges;
                
                private TypedEdge<T> next;
                private TypedEdge<T> cur;

                public SubgraphAdjacencyListIterator() {
                    edges = adjacencyList.iterator();
                    advance();
                }
                
                private void advance() {
                    next = null;
                    while (edges.hasNext()) {
                        TypedEdge<T> e = edges.next();

                        // Skip edges between vertices not in this subgraph
                        if (!vertexSubset.contains(e.from()) 
                                || !vertexSubset.contains(e.to())
                                || !validTypes.contains(e.edgeType()))
                            continue;

                        next = e;
                        break;
                    }
                }
                
                public boolean hasNext() {
                    return next != null;
                }
                
                public TypedEdge<T> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    cur = next;
                    advance();
                    return cur;
                }
                
                /**
                 * {@inheritDoc}
                 */                
                public void remove() {
                    if (null == null)
                        throw new IllegalStateException();
                    Subgraph.this.remove(cur);
                    cur = null;
                }            
            }
        }

        /**
         * A view-based class over the edges in a {@link Subgraph}.  This class
         * masks the state of the edges that are in the backing graph but not in
         * the subgraph.
         */
        private class SubgraphEdgeView extends AbstractSet<TypedEdge<T>> {           

            public SubgraphEdgeView() { }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(TypedEdge<T> e) {
                return Subgraph.this.add(e);
            }

            public boolean contains(Object o) {
                if (!(o instanceof TypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                TypedEdge<T> edge = (TypedEdge<T>)o;
                return Subgraph.this.contains(edge);
            }

            public Iterator<TypedEdge<T>> iterator() {
                return new SubgraphEdgeIterator();
            }

            /**
             * {@inheritDoc}
             */
            public boolean remove(Object o) {
                if (!(o instanceof TypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                TypedEdge<T> edge = (TypedEdge<T>)o;
                return Subgraph.this.remove(edge);
            }

            public int size() {
                return Subgraph.this.size();
            }

            /**
             * An {@code Iterator} that combines all the iterators returned by
             * {@link #getAdjacencyList(int)} for the vertices in this subview
             * and filters the results to remove symmetric edges found in two
             * lists.
             */
            private class SubgraphEdgeIterator implements Iterator<TypedEdge<T>> {

                private final Iterator<SubgraphAdjacencyListView> adjacencyLists;

                private Iterator<TypedEdge<T>> curIter;

                private int curRoot;

                private TypedEdge<T> next;
                
                private TypedEdge<T> cur;

                /**
                 * Creates an iterator that combines the adjacency lists
                 * iterators for all the vertices in this subview.
                 */
                public SubgraphEdgeIterator() {
                    // Create a list for all the adjacency lists of the vertices
                    // in this subview
                    List<SubgraphAdjacencyListView> lists = 
                        new ArrayList<SubgraphAdjacencyListView>(
                            vertices.size());
                    
                    // Loop over all vertices in the subview and wrap their
                    // adjacency lists
                    for (Integer v : vertices) {
                        Set<TypedEdge<T>> adj =
                            UndirectedMultigraph.this.getAdjacencyList(v);
                        if (adj == null)
                            continue;
                        // Wrap it to only return the edges present in this
                        // subview
                        lists.add(new SubgraphAdjacencyListView(v, adj));
                    }
                    adjacencyLists = lists.iterator();
                    advance();
                }

                private void advance() {
                    next = null;
                    // If there are no more elements in the current adjacency
                    // list and there are no futher adjacency lists to use, the
                    // iterator is finished
                    if ((curIter == null || !curIter.hasNext()) 
                            && !adjacencyLists.hasNext())
                        return;

                    do {
                        // Find an edge iterator with at least one edge
                        while ((curIter == null || !curIter.hasNext()) 
                                   && adjacencyLists.hasNext()) {
                            
                            // Get the next adjacency list
                            SubgraphAdjacencyListView adjList = 
                                adjacencyLists.next();
                            // Record what the root vertex is for it
                            curRoot = adjList.root;
                            // Set the current iterator to return this list's
                            // edges
                            curIter = adjList.iterator();
                        }

                        // If we didn't find one, short circuit
                        if (curIter == null || !curIter.hasNext())
                            return;                   

                        // Get the next edge to examine
                        TypedEdge<T> e = curIter.next();

                        // The backing graph stores symmetric edges in order to
                        // maintain the adjacency lists.  To account for this,
                        // we toss out edges that will have their symmetric
                        // version counted, using the edge's to and from to make
                        // the distinction.
                        if ((curRoot == e.from() && curRoot < e.to())
                                || (curRoot == e.to() && curRoot < e.from()))
                            next = e;
                    } while (next == null); 
                }

                public boolean hasNext() {
                    return next != null;
                }

                public TypedEdge<T> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    cur = next;
                    advance();
                    return cur;                    
                }

                /**
                 * Throws an {@link UnsupportedOperationException} if called.
                 */
                public void remove() {
                    if (cur == null)
                        throw new IllegalStateException();
                    Subgraph.this.remove(cur);
                    cur = null;
                }
            }
        }

        /**
         * A view of a subgraph's vertex's neighbors that are also in the
         * subview.  This view monitors for additions and removals to the set in
         * order to update the state of this {@code Subgraph}.
         */
        private class SubgraphNeighborsView extends AbstractSet<Integer> {

            /**
             * The set of adjacent vertices to a vertex.  This set is itself a view
             * to the data and is updated by the {@link EdgeList} for a vertex.
             */
            private Set<Integer> adjacent;

            private int root;
            
            /**
             * Constructs a view around the set of adjacent vertices
             */
            public SubgraphNeighborsView(int root, Set<Integer> adjacent) {
                this.root = root;
                this.adjacent = adjacent;
            }
            
            /**
             * Adds an edge to this vertex and adds the vertex to the graph if it
             * was not present before.
             */
            public boolean add(Integer vertex) {
                throw new UnsupportedOperationException(
                    "Cannot add vertices to subgraph");
            }
            
            public boolean contains(Object o) {
                if (!(o instanceof Integer))
                    return false;
                Integer i = (Integer)o;
                return Subgraph.this.hasEdge(root, i);
            }
            
            public Iterator<Integer> iterator() {
                return new SubgraphNeighborsIterator();
            }
            
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
            
            public int size() {
                int sz = 0;
                for (Integer v : adjacent) {
                    if (hasEdge(root, v))
                        sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for a subgraphs's neighboring
             * vertices set, which keeps track of which neighboring vertices are
             * actually in this subview.
             */
            private class SubgraphNeighborsIterator implements Iterator<Integer> {

                private final Iterator<Integer> iter;

                private Integer next;

                public SubgraphNeighborsIterator() {
                    iter = adjacent.iterator();
                    advance();
                }
                
                /**
                 * Finds the next adjacent vertex that is also in this subview.
                 */
                private void advance() {
                    next = null;
                    while (iter.hasNext() && next == null) {
                        Integer v = iter.next();
                        if (Subgraph.this.hasEdge(v, root))
                            next = v;
                    }                    
                }

                public boolean hasNext() {
                    return next != null;
                }

                public Integer next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    Integer cur = next;
                    advance();
                    return cur;
                }

                /**
                 * Throws an {@link UnsupportedOperationException} if called.
                 */
                public void remove() {
                    throw new UnsupportedOperationException();
                }            
            }
        }
    }
}
