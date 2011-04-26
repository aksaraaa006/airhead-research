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

import java.util.Set;


/**
 * An interface specification for interacting with <a
 * href="http://en.wikipedia.org/wiki/Graph_(mathematics)">Graph</a> objects.
 * This interface assumes the most general contract for interacting with a graph
 * as a set of edges and vertices.  This interface permits the following
 * behaviors of a graph: <ul>
 *
 * <li> Multiple edges between two vertices (a {@link Multigraph}).
 *
 * <li> Weights associated with each edge (a {@link WeightedGraph});
 *
 * <li> Directionality between edges, i.e. an edge x-&gt;y is not equal to
 *      x&lt;-y (a {@link DirectedGraph})
 *
 * <li> Self-loops
 *
 * </ul>
 *
 * Implementations may enforce any, some, or all of these behaviors to fully
 * define their behavior.
 *
 * @author David Jurgens
 */
public interface Graph2<E extends Edge> {

    /**
     * Adds a vertex with the provided index to the graph, returning {@code
     * true} if the vertex was not previously present (optional operation).
     *
     * @param i a non-negative index for a vertex.  If the graph has size bounds
     *        (i.e. a limited number of vertices), the implementation may throw
     *        an exception if this index exceeds those bounds.
     *
     * @return {@true} if the vertex was not previously present
     *
     * @throws IllegalArgumentException if {@code i} is negative or beyond the
     *         number of representable vertices in the current instance
     */
    boolean addVertex(int i);

    /**
     * Adds an edge between the two vertices, returning {@code true} if the edge
     * was not previously present (optional operation).
     *
     * <p> If adding this edge would violate some structural constraints on the
     * graph, implementations may return {@code false} or throw a {@link
     * GraphConstructionException}.  If {@code false} is returned, the called
     * may check whether the edge was added using {@link #containsEdge(int,int)
     * containsEdge}
     *
     * <p> Implemenations are free to decide the behavior for cases where one or
     * both of the vertices are not currently in the graph, and whether
     * self-edges are allowed (i.e. {@code vertex1 == vertex2}).
     *
     * @return {@code true} if the edge was added, {@code false} if the edge was
     *         not added, or if the edge was aready present
     *
     * @throws GraphConstructionException if adding this edge cannot be added to
     *         the graph
     * 
     * @see #containsEdge(int, int)
     */
    boolean addEdge(E edge);

    /**
     * Removes all the edges and vertices from this graph (optional operation).
     */
    void clear();

    /**
     * Removes all the edges in this graph, retaining all the vertices (optional
     * operation).
     */
    void clearEdges();

    /**
     * Returns {@code true} if this graph a vertex with the specified index
     */
    boolean containsVertex(int vertex);

    /**
     * Returns {@code true} if this graph contains an edge between {@code from}
     * and {@code to}.  Imeplementations are free to define whether the ordering
     * of the vertices matters.
     */
    boolean containsEdge(int from, int to);

    /**
     * Returns {@code true} if this graph contains an edge of the specific type
     * between {@code vertex1} and {@code vertex2}.
     */
    boolean containsEdge(E e);

    /**
     * Returns the set of edges contained in this graph.  It is expected that
     * changes to this set be reflected in the backing graph.
     */
    Set<E> edges();

    /**
     * Returns the set of edges connected to the provided vertex.
     */
    Set<E> getAdjacencyList(int vertex);

    /**
     * Returns the set of vertices that are connected to the specified vertex.
     */
    Set<Integer> getAdjacentVertices(int vertex);

    /**
     * Returns the {@code Edge} instance connecting the two vertices or {@code
     * null} if the vertices are not connected.
     */
    E getEdge(int vertex1, int vertex2);

    /**
     * Computes whether this graph is acyclic with its current set of edges, and
     * returns {@code true} if this graph contains cycles, {@code false} if
     * acyclic.
     */
    boolean hasCycles();

    /**
     * Returns the number of edges in this graph.
     */
    int size();

    /**
     * Returns the number of vertices in this graph.
     */
    int order();

    /**
     * Removes the edge from {@code vertex1} to {@code vertex2}, returning
     * {@code true} if the edge existed and was removed (optional operation).
     */
    boolean removeEdge(E e);

    /**
     * Removes the vertex and all of its connected edges from the graph
     * (optional operation).
     */
    boolean removeVertex(int vertex);    
    
    /**
     * Returns a view of this graph containing only the specified vertices.
     * Only edges connecting two vertices in the provided set will be viewable
     * in the subgraph.  Any changes to the subgraph will be reflected in this
     * graph and vice versa.
     *
     * @param vertices the vertices to include in the subgraph
     *
     * @throws IllegalArgumentException if {@code vertices} contains vertices
     *         not present in this graph
     */
    Graph2<E> subgraph(Set<Integer> vertices);

    /**
     * Returns the set of vertices in this graph.
     */
    Set<Integer> vertices();
    
}
