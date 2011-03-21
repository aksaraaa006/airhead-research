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
 * href="http://en.wikipedia.org/wiki/Multigraph">MultiGraph</a> objects.  A
 * multigraph contains a set of vertices and a list of edges between vertices,
 * where multiple, parallel edges may exist between two vertices.
 *
 * @param T a class type whose values are used to distinguish between edge types
 * 
 * @author David Jurgens
 *
 * @see TypedEdge
 */
public interface WeightedMultigraph<T> extends Multigraph, WeightedGraph {

    /**
     * Adds an edge of the <i>default</i> weight and type between the two
     * vertices, returning {@code true} if the edge was not previously present.
     * Implementations are free to decide what the default edge type is, or to
     * throw an exception upon adding an edge if no default type has been set.
     */
    boolean addEdge(int vertex1, int vertex2);

    /**
     * Adds an edge between the two vertices with the specified weight and
     * <i>default</i> type, returning {@code true} if the edge was not previously
     * present or the weight between the edges was changed.  Implemenations are
     * free to decide the behavior for cases where one or both of the vertices
     * are not currently in the graph, and whether self-edges are allowed
     * (i.e. {@code vertex1 == vertex2}).
     *
     * @return {@code true} if the vertex was not previously present or the
     *         weight between the edges was changed
     */
    boolean addEdge(int vertex1, int vertex2, double weight);

    /**
     * Adds an edge of the provided type between the two vertices with the
     * <i>default</i> edge weight, returning {@code true} if the edge was not
     * previously present.  Implementations are free to decide upon the
     * appropriate behavior of the default edge weight.
     */
    boolean addEdge(int vertex1, int vertex2, T edgeType);

    /**
     * Adds an edge between the two vertices with the specified weight,
     * returning {@code true} if the edge was not previously present or the
     * weight between the edges was changed.  Implemenations are free to decide
     * the behavior for cases where one or both of the vertices are not
     * currently in the graph, and whether self-edges are allowed (i.e. {@code
     * vertex1 == vertex2}).
     *
     * @return {@code true} if the vertex was not previously present or the
     *         weight between the edges was changed
     */
    boolean addEdge(int vertex1, int vertex2, T edgeType, double weight);

    /**
     * Returns the set of typed edges in the graph
     */
    Set<WeightedTypedEdge<T>> edges();

    /**
     * Returns the set of typed edges connected to the vertex.
     */
    Set<WeightedTypedEdge<T>> getAdjacencyList(int vertex);

    /**
     * Returns an arbitrary edge connecting the two vertices if the edges are
     * connected by one or more edges.
     */
    @Override WeightedTypedEdge<T> getEdge(int vertex1, int vertex2);

//     /**
//      * {@inheritDoc}
//      */
//     WeightedMultigraph subgraph(Set<Integer> vertices);

//     /**
//      * Returns a subgraph of this graph containing only the specified vertices
//      * and edges of the specified types.  
//      */
//     WeightedMultigraph subgraph(Set<Integer> vertices, Set<T> edgeTypes);
       
}
