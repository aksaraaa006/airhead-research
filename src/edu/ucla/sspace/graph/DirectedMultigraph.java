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
     * Creates an empty graph with node edges
     */
    public DirectedMultigraph() { 
        typeToEdges = new HashMap<T,DirectedGraph<DirectedTypedEdge<T>>>();
        // vertices = new IntSet();
        vertices = new HashSet<Integer>();
        edges = new EdgeView();
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
    public Set<DirectedTypedEdge<T>> getAdjacencyList(int vertex) {
        // By contract, if vertex is not in this graph, then we return null.
        // However, we go ahead and construct the sets first anyway, as they are
        // cheap to construct and the common case is to ask for vertices that
        // are in the graph
        List<Set<DirectedTypedEdge<T>>> sets = 
            new ArrayList<Set<DirectedTypedEdge<T>>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
            Set<DirectedTypedEdge<T>> adj = g.getAdjacencyList(vertex);
            if (adj != null)
                sets.add(adj);
        }
        return (sets.isEmpty()) ? null : 
            new CombinedSet<DirectedTypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2) {
        // By contract, if the two vertices do not have any edges in this graph,
        // then we return null.  However, we go ahead and construct the sets
        // first anyway, as they are cheap to construct and the common case is
        // to ask for vertices that are in the graph
        List<Set<DirectedTypedEdge<T>>> sets = 
            new ArrayList<Set<DirectedTypedEdge<T>>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
            Set<DirectedTypedEdge<T>> e = g.getEdges(vertex1, vertex2);
            if (e != null) {
                sets.add(e);
            }
        }
        return (sets.isEmpty()) ? null : 
            new CombinedSet<DirectedTypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getNeighbors(int vertex) {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        for (DirectedGraph<DirectedTypedEdge<T>> g : typeToEdges.values()) {
            Set<Integer> n = g.getNeighbors(vertex);
            if (n != null)
                sets.add(n);
        }
        System.out.println("neighbors: " + sets);
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
         return new Subgraph(typeToEdges.keySet(), subset);
    }

    /**
     * {@inheritDoc}
     */
     public DirectedMultigraph<T> subgraph(Set<Integer> subset, Set<T> edgeTypes) {
         return new Subgraph(edgeTypes, subset);
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
            this.vertexSubset = vertexSubset;
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
            if (e instanceof TypedEdge) {
                TypedEdge<?> te = (TypedEdge<?>)e;
                if (!validTypes.contains(te.edgeType()))
                    return false;
                if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.from()))
                    return false;
                return DirectedMultigraph.this.contains(e);
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
        public Set<DirectedTypedEdge<T>> getAdjacencyList(int vertex) {
            if (!vertexSubset.contains(vertex))
                return null;
            Set<DirectedTypedEdge<T>> adj = 
                DirectedMultigraph.this.getAdjacencyList(vertex);
            return (adj == null) ? null
                : new SubgraphAdjacencyListView(vertex, adj);
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2) {
            List<Set<DirectedTypedEdge<T>>> sets = 
                new ArrayList<Set<DirectedTypedEdge<T>>>();
            for (T type : validTypes) {
                DirectedGraph<DirectedTypedEdge<T>> g = typeToEdges.get(type);
                Set<DirectedTypedEdge<T>> edges = g.getEdges(vertex1, vertex2);
                if (edges != null)
                    sets.add(edges);
            }
            return (sets.isEmpty()) 
                ? null : new CombinedSet<DirectedTypedEdge<T>>(sets);
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> getNeighbors(int vertex) {
            if (!vertexSubset.contains(vertex))
                return null;
            Set<Integer> neighbors = 
                DirectedMultigraph.this.getNeighbors(vertex);
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
            if (edges == null)
                return null;

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
            if (edges == null)
                return null;
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
        public boolean remove(DirectedTypedEdge<T> e) {
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
            for (DirectedTypedEdge<T> e : edges())
                size++;
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
            return new Subgraph(validTypes, verts);
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
            return new Subgraph(edgeTypes, verts);
        }
     
        /**
         * {@inheritDoc}
         */
        public Set<Integer> vertices() {
            return Collections.unmodifiableSet(vertexSubset);
        }


        /**
         * A view for the {@code Edge} adjacent edges of a vertex within a
         * subgraph.  This class monitors for changes to edge set to update the
         * state of this graph
         */
        private class SubgraphAdjacencyListView 
                extends AbstractSet<DirectedTypedEdge<T>> {

            /**
             * The adjacency list of edges in the backing graph.
             */
            private final Set<DirectedTypedEdge<T>> adjacencyList;
            
            /**
             * The root vertex in the subgraph.
             */ 
            private final int root;

            public SubgraphAdjacencyListView(int root,
                    Set<DirectedTypedEdge<T>> adjacencyList) {
                this.root = root;
                this.adjacencyList = adjacencyList;                
            }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(DirectedTypedEdge<T> edge) {
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
            
            public Iterator<DirectedTypedEdge<T>> iterator() {
                return new SubgraphAdjacencyListIterator();
            }
            
            /**
             * Removes the edge, if present in this subgraph
             */
            public boolean remove(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> edge = (DirectedTypedEdge<T>)o;

                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.remove(edge);
            }

            public int size() {
                int sz = 0;
                Iterator<DirectedTypedEdge<T>> it = iterator();
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
                    implements Iterator<DirectedTypedEdge<T>> {
                
                private final Iterator<DirectedTypedEdge<T>> edges;
                
                private DirectedTypedEdge<T> next;
                private DirectedTypedEdge<T> cur;

                public SubgraphAdjacencyListIterator() {
                    edges = adjacencyList.iterator();
                    advance();
                }
                
                private void advance() {
                    next = null;
                    while (edges.hasNext()) {
                        DirectedTypedEdge<T> e = edges.next();

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
                
                public DirectedTypedEdge<T> next() {
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

            /**
             * An {@code Iterator} that combines all the iterators returned by
             * {@link #getAdjacencyList(int)} for the vertices in this subview
             * and filters the results to remove symmetric edges found in two
             * lists.
             */
            private class SubgraphEdgeIterator implements Iterator<DirectedTypedEdge<T>> {

                private final Iterator<SubgraphAdjacencyListView> adjacencyLists;

                private Iterator<DirectedTypedEdge<T>> curIter;

                private int curRoot;

                private DirectedTypedEdge<T> next;
                
                private DirectedTypedEdge<T> cur;

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
                        Set<DirectedTypedEdge<T>> adj =
                            DirectedMultigraph.this.getAdjacencyList(v);
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
                        DirectedTypedEdge<T> e = curIter.next();

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

                public DirectedTypedEdge<T> next() {
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
                        if (Subgraph.this.hasEdge(root, v))
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
