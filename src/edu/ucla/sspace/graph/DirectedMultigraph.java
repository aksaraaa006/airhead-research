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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import edu.ucla.sspace.util.CombinedIterator;
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
public class DirectedMultigraph<T> 
    implements Multigraph<T,DirectedTypedEdge<T>>, 
               DirectedGraph<DirectedTypedEdge<T>>, 
               java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * A mapping from an edge type to the graph that contains all edges with
     * that type.
     */
    private final Map<T,DirectedGraph<DirectedTypedEdge<T>>> typeToEdges;

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
    public DirectedMultigraph() { 
        typeToEdges = new HashMap<T,DirectedGraph<DirectedTypedEdge<T>>>();
        // vertices = new IntSet();
        vertices = new HashSet<Integer>();
        edges = new EdgeView();
        subgraphs = new ArrayList<WeakReference<Subgraph>>();
    }

    /**
     * Creates a directed multigraph with a copy of all the vertices and edges
     * in {@code g}.
     */
    public DirectedMultigraph(Graph<? extends DirectedTypedEdge<T>> g) {
        this();
        for (Integer v : g.vertices())
            add(v);
        for (DirectedTypedEdge<T> e : g.edges())
            add(e);
    }    

//     /**
//      * Creates a directed multigraph with a copy of all the vertices and edges
//      * in {@code g}.
//      */
//     public DirectedMultigraph(Multigraph<? extends T,? extends DirectedTypedEdge<T>> g) {
//         this();
//         for (Integer v : g.vertices())
//             add(v);
//         for (DirectedTypedEdge<T> e : g.edges())
//             add(e);
//     }    

    /**
     * {@inheritDoc}
     */
    public boolean add(int vertex) {
        return vertices.add(vertex);
    }

    public boolean add(DirectedTypedEdge<T> e) {
        DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(e.edgeType());
        if (g == null) {
            Graph<DirectedTypedEdge<T>> f = new TypeGraph<T>();
            g = Graphs.asDirectedGraph(f);
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
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            if (g.contains(e))
                return true;
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2) {
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            if (g.contains(vertex1, vertex2))
                return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2, T edgeType) {
        DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(edgeType);
        if (g != null)
            return g.contains(vertex1, vertex2);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int degree(int vertex) {
        int degree = 0;
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            degree += g.degree(vertex);
        return degree;
    }

    /**
     * Returns the set of typed edges in the graph
     */
    public Set<DirectedTypedEdge<T>> edges() {
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
    @Override public boolean equals(Object o) {
        if (o instanceof Graph) {
            Graph<?> g = (Graph<?>)o;
            return g.order() == order()
                && g.size() == size()
                && g.vertices().equals(vertices())
                && g.edges().equals(edges());                    
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getAdjacencyList(int vertex) {
        List<Set<DirectedTypedEdge<T>>> sets = 
            new ArrayList<Set<DirectedTypedEdge<T>>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
            Set<DirectedTypedEdge<T>> adj = g.getAdjacencyList(vertex);
            if (!adj.isEmpty())
                sets.add(adj);
        }
        return (sets.isEmpty()) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : new CombinedSet<DirectedTypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2) {
        List<Set<DirectedTypedEdge<T>>> sets = 
            new ArrayList<Set<DirectedTypedEdge<T>>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
            Set<DirectedTypedEdge<T>> e = g.getEdges(vertex1, vertex2);
            if (!e.isEmpty()) {
                sets.add(e);
            }
        }
        return (sets.isEmpty()) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : new CombinedSet<DirectedTypedEdge<T>>(sets);
    }


    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2, T t) {
        DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(t);
        return (g == null) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : g.getEdges(vertex1, vertex2);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getNeighbors(int vertex) {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
            Set<Integer> n = g.getNeighbors(vertex);
            if (!n.isEmpty())
                sets.add(n);
        }
        return (sets.isEmpty()) 
            ? Collections.<Integer>emptySet() 
            : new CombinedSet<Integer>(sets);
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
    public int hashCode() {
        return vertices().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public int inDegree(int vertex) {
        int inDegree = 0;
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            inDegree += g.inDegree(vertex);
        return inDegree;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> inEdges(int vertex) {
        List<Set<DirectedTypedEdge<T>>> sets = 
            new ArrayList<Set<DirectedTypedEdge<T>>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            sets.add(g.inEdges(vertex));
        return new CombinedSet<DirectedTypedEdge<T>>(sets);
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
    public int outDegree(int vertex) {
        int outDegree = 0;
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            outDegree += g.outDegree(vertex);
        return outDegree;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> outEdges(int vertex) {
        List<Set<DirectedTypedEdge<T>>> sets = 
            new ArrayList<Set<DirectedTypedEdge<T>>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            sets.add(g.outEdges(vertex));
        return new CombinedSet<DirectedTypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> predecessors(int vertex) {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            sets.add(g.predecessors(vertex));
        return new CombinedSet<Integer>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int vertex) {
        // If we can remove the vertex from the global set, then at least one of
        // the type-specific graphs has this vertex.
        if (vertices.remove(vertex)) {
            Iterator<Map.Entry<T,DirectedGraph<DirectedTypedEdge<T>>>> it = 
                typeToEdges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<T,DirectedGraph<DirectedTypedEdge<T>>> e = it.next();
                DirectedGraph<DirectedTypedEdge<T>> g = e.getValue();
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
    public boolean remove(DirectedTypedEdge<T> edge) {
        Iterator<Map.Entry<T,DirectedGraph<DirectedTypedEdge<T>>>> it = 
            typeToEdges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T,DirectedGraph<DirectedTypedEdge<T>>> e = it.next();
            DirectedGraph<DirectedTypedEdge<T>> g = e.getValue();
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
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            size += g.size();
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> successors(int vertex) {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
            sets.add(g.successors(vertex));
        return new CombinedSet<Integer>(sets);
    }

    /**
     * {@inheritDoc}
     */
     public DirectedMultigraph<T> subgraph(Set<Integer> subset) {
         Subgraph sub = new Subgraph(typeToEdges.keySet(), subset);
         subgraphs.add(new WeakReference<Subgraph>(sub));
         return sub;
    }

    /**
     * {@inheritDoc}
     */
     public DirectedMultigraph<T> subgraph(Set<Integer> subset, Set<T> edgeTypes) {
         if (edgeTypes.isEmpty()) 
           throw new IllegalArgumentException("Must specify at least one type");
         if (!typeToEdges.keySet().containsAll(edgeTypes)) {
             throw new IllegalArgumentException(
                 "Cannot create subgraph with more types than exist");
         }
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
    class TypeGraph<T> extends AbstractGraph<DirectedTypedEdge<T>,
                                                SparseDirectedTypedEdgeSet<T>> {
        
        private static final long serialVersionUID = 1L;
        
        public TypeGraph() { }
                
        @Override protected SparseDirectedTypedEdgeSet<T> 
                createEdgeSet(int vertex) {
            return new SparseDirectedTypedEdgeSet<T>(vertex);
        }        
    }

    /**
     * The {@code EdgeView} class encapsulates all the of the edge sets from the
     * different edge types.  Because {@link CombinedSet} does not allow for
     * addition, we add an {@code add} method that handles adding new edges to
     * the graph via this set.
     */
    class EdgeView extends AbstractSet<DirectedTypedEdge<T>> {

        private CombinedSet<DirectedTypedEdge<T>> backing;

        public EdgeView() {
            update();
        }

        public boolean add(DirectedTypedEdge<T> e) {
            // Calling the backing graph's add will route the edge to the
            // type-appropriate set
            return DirectedMultigraph.this.add(e);
        }

        public boolean contains(Object o) {
            return backing.contains(o);
        }

        public Iterator<DirectedTypedEdge<T>> iterator() {
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
            List<Set<DirectedTypedEdge<T>>> sets = 
                new ArrayList<Set<DirectedTypedEdge<T>>>();
            for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
                sets.add(g.edges());
            }
            backing = new CombinedSet<DirectedTypedEdge<T>>(sets);
        }
    }

    /**
     * An implementation for handling the subgraph behavior.
     */
     class Subgraph extends DirectedMultigraph<T> {

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
        public boolean add(DirectedTypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                throw new UnsupportedOperationException(
                    "Cannot add new vertex to subgraph");

            DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(e.edgeType());
            if (g == null) {
                Graph<DirectedTypedEdge<T>> f = new TypeGraph<T>();
                g = Graphs.asDirectedGraph(f);
                typeToEdges.put(e.edgeType(), g);
            }
            return g.add(e);
        }

        /**
         * {@inheritDoc}
         */        
        public void clear() {
            for (Integer v : vertexSubset)
                DirectedMultigraph.this.remove(v);
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
                    DirectedGraph<DirectedTypedEdge<T>> g = 
                        typeToEdges.get(type);
                    // Check whether each of the adjacent edges points to
                    // another vertex in this subgraph.  If so, remove it
                    for (DirectedTypedEdge<T> e : g.getAdjacencyList(v)) {
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
            DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(edgeType);
            for (Integer v : vertexSubset) {
                // Check whether each of the adjacent edges points to another vertex
                // in this subgraph.  If so, remove it
                for (DirectedTypedEdge<T> e : g.getAdjacencyList(v)) {
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
            if (e instanceof DirectedTypedEdge) {
                DirectedTypedEdge<?> te = (DirectedTypedEdge<?>)e;
                return vertexSubset.contains(e.from())
                    && vertexSubset.contains(e.to())
                    && validTypes.contains(te.edgeType())
                    && DirectedMultigraph.this.contains(e);
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

            Set<DirectedTypedEdge<T>> edges = 
                DirectedMultigraph.this.getEdges(vertex1, vertex2);
            if (edges != null) {
                // If there were edges between the two vertices, ensure that at
                // least one of them has the type represented in this subgraph
                for (DirectedTypedEdge<T> e : edges)
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

            DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(edgeType);
            return g.contains(vertex1, vertex2);
        }

        /**
         * {@inheritDoc}
         */
        public int degree(int vertex) {
            int degree = 0;
            for (T type : validTypes) {
                DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(type);
                degree += g.degree(vertex);
            }
            return degree;
        }

        /**
         * Returns the set of typed edges in the graph
         */
        public Set<DirectedTypedEdge<T>> edges() {
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
        @Override public boolean equals(Object o) {
            if (o instanceof Graph) {
                Graph<?> g = (Graph<?>)o;
                return g.order() == order()
                    && g.size() == size()
                    && g.vertices().equals(vertices())
                    && g.edges().equals(edges());                    
            }
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> getAdjacencyList(int vertex) {
            if (!vertexSubset.contains(vertex))
                return Collections.<DirectedTypedEdge<T>>emptySet();
            Set<DirectedTypedEdge<T>> adj = 
                DirectedMultigraph.this.getAdjacencyList(vertex);
            return (adj.isEmpty()) 
                ? Collections.<DirectedTypedEdge<T>>emptySet()
                : new SubgraphAdjacencyListView(vertex);
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2) {
            if (!vertexSubset.contains(vertex1) 
                    || !vertexSubset.contains(vertex2))
                return Collections.<DirectedTypedEdge<T>>emptySet();

            List<Set<DirectedTypedEdge<T>>> sets = 
                new ArrayList<Set<DirectedTypedEdge<T>>>();
            for (T type : validTypes) {
                Set<DirectedTypedEdge<T>> edges = 
                    DirectedMultigraph.this.getEdges(vertex1, vertex2, type);
                if (!edges.isEmpty())
                    sets.add(edges);
            }
            return (sets.isEmpty()) 
                ? Collections.<DirectedTypedEdge<T>>emptySet() 
                : new CombinedSet<DirectedTypedEdge<T>>(sets);
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> getNeighbors(int vertex) {
            if (!vertexSubset.contains(vertex))
                return Collections.<Integer>emptySet();
//             Set<Integer> neighbors = 
//                 DirectedMultigraph.this.getNeighbors(vertex);
//             // If the set of neighbors is larger than the number of vertices in
//             // this subgraph, then just use the set of vertices
//             return (neighbors.size() > vertexSubset.size())
            return new SubgraphNeighborsView(vertex, vertexSubset);
                // : new SubgraphNeighborsView(vertex, neighbors);
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
        public int hashCode() {
            return vertices().hashCode();
        }

        /**
         * Returns {@code true} if the two vertices are contained within this
         * subgraph and have an edge that is valid within this subgraph's type
         * constraints.
         */
        private boolean hasEdge(int v1, int v2) {
            if (vertexSubset.contains(v1) && vertexSubset.contains(v2)) {
                Set<DirectedTypedEdge<T>> edges = getEdges(v1, v2);
                return edges != null;
            }
            return false;            
        }

        /**
         * {@inheritDoc}
         */
        public int inDegree(int vertex) {
            int inDegree = 0;
            for (T type : validTypes) {
                DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(type);
                inDegree += g.inDegree(vertex);
            }
            return inDegree;
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> inEdges(int vertex) {
            // REMINDER: this is probably best wrapped with yet another
            // decorator class to avoid the O(n) penality of iteration over all
            // the edges
            Set<DirectedTypedEdge<T>> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return Collections.<DirectedTypedEdge<T>>emptySet();

            Set<DirectedTypedEdge<T>> in = new HashSet<DirectedTypedEdge<T>>();
            for (DirectedTypedEdge<T> e : edges) {
                if (e.to() == vertex)
                    in.add(e);
            }
            return in;
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
        public int outDegree(int vertex) {
            int outDegree = 0;
            for (T type : validTypes) {
                DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(type);
                outDegree += g.outDegree(vertex);
            }
            return outDegree;
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> outEdges(int vertex) {
            // REMINDER: this is probably best wrapped with yet another
            // decorator class to avoid the O(n) penality of iteration over all
            // the edges
            Set<DirectedTypedEdge<T>> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return Collections.<DirectedTypedEdge<T>>emptySet();;
            Set<DirectedTypedEdge<T>> out = new HashSet<DirectedTypedEdge<T>>();
            for (DirectedTypedEdge<T> e : edges) {
                if (e.from() == vertex)
                    out.add(e);
            }
            return out;
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> predecessors(int vertex) {
            Set<Integer> preds = new HashSet<Integer>();
            for (DirectedTypedEdge<T> e : inEdges(vertex))
                preds.add(e.from());
            return preds;
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
        @Override public boolean remove(DirectedTypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                return false;
            return DirectedMultigraph.this.remove(e);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            int size = 0;
            for (DirectedTypedEdge<T> e : edges()) {
                size++;
            }
            return size;
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> successors(int vertex) {
            Set<Integer> succs = new HashSet<Integer>();
            for (DirectedTypedEdge<T> e : outEdges(vertex))
                succs.add(e.to());
            return succs;
        }

        /**
         * {@inheritDoc}
         */
        public DirectedMultigraph<T> subgraph(Set<Integer> verts) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            return DirectedMultigraph.this.subgraph(verts, validTypes);
        }

        /**
         * {@inheritDoc}
         */
        public DirectedMultigraph<T> subgraph(Set<Integer> verts, Set<T> edgeTypes) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            if (!validTypes.containsAll(edgeTypes))
                throw new IllegalArgumentException("provided types is not a " +
                    "subset of the edge types of this graph");
            return DirectedMultigraph.this.subgraph(verts, edgeTypes);
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
                extends AbstractSet<DirectedTypedEdge<T>> {

            private final int root;

            public SubgraphAdjacencyListView(int root) {
                this.root = root;
            }

            public boolean add(DirectedTypedEdge<T> edge) {
                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.add(edge);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Edge))
                    return false;
                Edge e = (Edge)o;
                return (e.to() == root ||
                         e.from() == root)
                        && Subgraph.this.contains(e);
            }
            
            public Iterator<DirectedTypedEdge<T>> iterator() {
                return new SubgraphAdjacencyListIterator();
            }
            
            public boolean remove(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                DirectedTypedEdge<?> e = (DirectedTypedEdge<?>)o;
                if (!validTypes.contains(e.edgeType())) 
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> e2 = (DirectedTypedEdge<T>)o;
                return (e2.to() == root ||
                        e2.from() == root)
                    && Subgraph.this.remove(e2);
            }

            public int size() {                
                int sz = 0;
                for (int v : vertexSubset) {
                    if (v == root)
                        continue;
                    for (T type : validTypes) {
                        DirectedGraph<DirectedTypedEdge<T>> g = 
                            typeToEdges.get(type);
                        Set<DirectedTypedEdge<T>> edges = g.getEdges(v, root);
                        sz += edges.size();
                    }
                }
                return sz;
            }

            /**
             * A decorator around the iterator for the adjacency list for a
             * vertex in a subgraph, which tracks edges removal to update the
             * number of edges in the graph.
             */
            private class SubgraphAdjacencyListIterator 
                    implements Iterator<DirectedTypedEdge<T>> {
            
//                 Integer curNeighbor;

//                 Iterator<Integer> remainingNeighbors;

//                 T curType;

//                 Iterator<T> remaingTypes;

                Iterator<DirectedTypedEdge<T>> edges;

                public SubgraphAdjacencyListIterator() {
                    // remainingNeighbors = vertexSubset.iterator();
//                     // Set up the first neighbor
//                     advance();
                    List<Iterator<DirectedTypedEdge<T>>> iters =
                        new LinkedList<Iterator<DirectedTypedEdge<T>>>();

                    for (int v : vertexSubset) {
                        if (v == root)
                            continue;
                        for (T type : validTypes) {
                            DirectedGraph<DirectedTypedEdge<T>> g = 
                                typeToEdges.get(type);
                            Set<DirectedTypedEdge<T>> edges = g.getEdges(v, root);
                            if (!edges.isEmpty())
                                iters.add(edges.iterator());
                        }
                    }
                    edges = new CombinedIterator<DirectedTypedEdge<T>>(iters);
                }

                public boolean hasNext() {
                    return edges.hasNext();
                }

                public DirectedTypedEdge<T> next() {
                    return edges.next();
                }

                public void remove() {
                    edges.remove();
                }

//                 void nextNeighbor() {
//                     curNeighbor == null;
//                     if (remainingNeighbors.hasNext()) {
//                         do {
//                             curNeighbor = remainingNeighbors.next();
//                             remainingTypes = validTypes.iterator();
//                         } while (curNeighbor == root 
//                                  && remainingNeighbors.hasNext());
//                     }
//                     if (curNeighbor != null && curNeighbor == root)
//                         curNeighbor == null;
//                 }
                
//                 void advance() {
//                     // Don't advance the state if there are still connections or
//                     // the first edge
//                     if (curEdges != null && curEdges.hasNext())
//                         return;

//                     // Base case: there are no more neighbors
//                     if (curNeighbor == null && !remainingNeighbors.hasNext())
//                         return;
                    
//                     // If there are no more types for the current neighbor, then
//                     // try to find another one
//                     if (remainingTypes == null || !remainingTypes.hasNext()) 
//                         nextNeighbor();
                    
//                     // If we have a valid neighbor, move to the next type
//                     if (curNeigbhor != null) {
//                         T type = remainingTypes.next();
//                         curEdges = 
//                             typeToEdges.get(type).getEdges(root, curNeighbor);
//                     }
//                 }

            }
        }

//         /**
//          * A view for the {@code Edge} adjacent edges of a vertex within a
//          * subgraph.  This class monitors for changes to edge set to update the
//          * state of this graph
//          */
//         private class SubgraphAdjacencyListView 
//                 extends AbstractSet<DirectedTypedEdge<T>> {

//             /**
//              * The adjacency list of edges in the backing graph.
//              */
//             private final Set<DirectedTypedEdge<T>> adjacencyList;
            
//             /**
//              * The root vertex in the subgraph.
//              */ 
//             private final int root;

//             public SubgraphAdjacencyListView(int root,
//                     Set<DirectedTypedEdge<T>> adjacencyList) {
//                 this.root = root;
//                 this.adjacencyList = adjacencyList;                
//             }

//             /**
//              * Throws an {@link UnsupportedOperationException} if called.
//              */
//             public boolean add(DirectedTypedEdge<T> edge) {
//                 return (edge.from() == root 
//                         || edge.to() == root) 
//                     && Subgraph.this.add(edge);
//             }

//             public boolean contains(Object o) {
//                 if (!(o instanceof Edge))
//                     return false;
//                 Edge e = (Edge)o;
//                 return vertexSubset.contains(e.to()) 
//                     && vertexSubset.contains(e.from())
//                     && adjacencyList.contains(e);
//             }
            
//             public Iterator<DirectedTypedEdge<T>> iterator() {
//                 return new SubgraphAdjacencyListIterator();
//             }
            
//             /**
//              * Removes the edge, if present in this subgraph
//              */
//             public boolean remove(Object o) {
//                 if (!(o instanceof DirectedTypedEdge))
//                     return false;
//                 @SuppressWarnings("unchecked")
//                 DirectedTypedEdge<T> edge = (DirectedTypedEdge<T>)o;

//                 return (edge.from() == root 
//                         || edge.to() == root) 
//                     && Subgraph.this.remove(edge);
//             }

//             public int size() {
//                 int sz = 0;
//                 Iterator<DirectedTypedEdge<T>> it = iterator();
//                 while (it.hasNext()) {
//                     it.next();
//                     sz++;
//                 }
//                 return sz;
//             }

//             /**
//              * A decorator around the iterator for the adjacency list for a
//              * vertex in a subgraph, which tracks edges removal to update the
//              * number of edges in the graph.
//              */
//             private class SubgraphAdjacencyListIterator 
//                     implements Iterator<DirectedTypedEdge<T>> {
                
//                 private final Iterator<DirectedTypedEdge<T>> edges;
                
//                 private DirectedTypedEdge<T> next;
//                 private DirectedTypedEdge<T> cur;

//                 public SubgraphAdjacencyListIterator() {
//                     edges = adjacencyList.iterator();
//                     advance();
//                 }
                
//                 private void advance() {
//                     next = null;
//                     while (edges.hasNext()) {
//                         DirectedTypedEdge<T> e = edges.next();

//                         // Skip edges between vertices not in this subgraph
//                         if (!vertexSubset.contains(e.from()) 
//                                 || !vertexSubset.contains(e.to())
//                                 || !validTypes.contains(e.edgeType()))
//                             continue;

//                         next = e;
//                         break;
//                     }
//                 }
                
//                 public boolean hasNext() {
//                     return next != null;
//                 }
                
//                 public DirectedTypedEdge<T> next() {
//                     if (!hasNext())
//                         throw new NoSuchElementException();
//                     cur = next;
//                     advance();
//                     return cur;
//                 }
                
//                 /**
//                  * {@inheritDoc}
//                  */                
//                 public void remove() {
//                     if (null == null)
//                         throw new IllegalStateException();
//                     Subgraph.this.remove(cur);
//                     cur = null;
//                 }            
//             }
//         }

        /**
         * A view-based class over the edges in a {@link Subgraph}.  This class
         * masks the state of the edges that are in the backing graph but not in
         * the subgraph.
         */
        private class SubgraphEdgeView extends AbstractSet<DirectedTypedEdge<T>> {           

            public SubgraphEdgeView() { }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(DirectedTypedEdge<T> e) {
                return Subgraph.this.add(e);
            }

            public boolean contains(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> edge = (DirectedTypedEdge<T>)o;
                return Subgraph.this.contains(edge);
            }

            public Iterator<DirectedTypedEdge<T>> iterator() {
                return new SubgraphEdgeIterator();
            }

            /**
             * {@inheritDoc}
             */
            public boolean remove(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> edge = (DirectedTypedEdge<T>)o;
                return Subgraph.this.remove(edge);
            }

            public int size() {
                return Subgraph.this.size();
            }

            private class SubgraphEdgeIterator 
                    implements Iterator<DirectedTypedEdge<T>> {                

                private final Queue<DirectedTypedEdge<T>> edgesToReturn;

                private final Queue<Integer> remainingVertices;

                private Iterator<Integer> possibleNeighbors;

                private Integer curVertex;

                public SubgraphEdgeIterator() {
                    remainingVertices = 
                        new ArrayDeque<Integer>(Subgraph.this.vertices());
                    edgesToReturn = new ArrayDeque<DirectedTypedEdge<T>>();
                    curVertex = null;
                    advance();
                }
                
                private void advance() {
                    // If there are still edges from the current pair, skip
                    // processing any further connections
                    if (!edgesToReturn.isEmpty())
                        return;
                    
                    do {
                        // Find the next vertex to analyze for connections
                        while ((possibleNeighbors == null
                                    || !possibleNeighbors.hasNext())
                               && !remainingVertices.isEmpty()) {
                            // Pull off the next vertex and then use the remaining
                            // vertices to check for connections
                            curVertex = remainingVertices.poll();
                            possibleNeighbors = remainingVertices.iterator();
                        }
                        
                        // Iterate through the possible neighbors of this
                        // vertex, stopping when we find another vertex that has
                        // at least one edge
                        while (edgesToReturn.isEmpty()
                                   && possibleNeighbors.hasNext()) {
                            Integer v = possibleNeighbors.next();
                            edgesToReturn.addAll(getEdges(curVertex, v));
                        }
                    } while (edgesToReturn.isEmpty()
                             && !remainingVertices.isEmpty());
                }

                public boolean hasNext() {
                    return edgesToReturn.size() > 0;
                }

                public DirectedTypedEdge<T> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    DirectedTypedEdge<T> n = edgesToReturn.poll();
                    advance();
                    return n;
                }

                public void remove() {
                    throw new IllegalStateException();
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
            private Set<Integer> allPossibleNeighbors;

            private int root;
            
            /**
             * Constructs a view around the set of adjacent vertices
             */
            public SubgraphNeighborsView(int root, Set<Integer> adjacent) {
                this.root = root;
                this.allPossibleNeighbors = adjacent;
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
                return allPossibleNeighbors.contains(i) && checkVertex(i);
            }
            
            private boolean checkVertex(Integer i) {
                for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values())
                    if (g.contains(i, root))
                        return true;
                return false;
            }

            public Iterator<Integer> iterator() {
                return new SubgraphNeighborsIterator();
            }
            
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
            
            public int size() {
                int sz = 0;
                for (Integer v : allPossibleNeighbors) {
                    if (checkVertex(v))
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
                    iter = allPossibleNeighbors.iterator();
                    advance();
                }
                
                /**
                 * Finds the next adjacent vertex that is also in this subview.
                 */
                private void advance() {
                    next = null;
                    while (iter.hasNext() && next == null) {
                        Integer v = iter.next();
                        if (checkVertex(v))
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
