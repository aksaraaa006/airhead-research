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
 */
public interface MultiGraph<T> extends Graph {

    /**
     * Adds an edge of the <i>default</i> type between the two vertices,
     * returning {@code true} if the edge was not previously present.
     * Implementations are free to decide what the default edge type is, or to
     * throw an exception upon adding an edge if no default type has been set.
     */
    boolean addEdge(int vertex1, int vertex2);

    /**
     * Adds an edge of the provided type between the two vertices, returning
     * {@code true} if the edge was not previously present.
     */
    boolean addEdge(int vertex1, int vertex2, T edgeType);

    /**
     * Removes all the edges in the graph with the specified edge type.
     */
    void clearEdges(T edgeType);

    /**
     * Returns {@code true} if there exists an edge between {@code vertex1} and
     * {@code vertex2} of <i>any</i> type.
     */
    boolean containsEdge(int vertex1, int vertex2);

    /**
     * Returns {@code true} if there exists an edge between {@code vertex1} and
     * {@code vertex2} of the specified type.
     */
    boolean containsEdge(int vertex1, int vertex2, T edgeType);

    /**
     * Returns the set of typed edges in the graph
     */
    Set<TypedEdge<T>> edges();

    /**
     * Returns the set of typed edges connected to the vertex.
     */
    Set<TypedEdge<T>> getAdjacencyList(int vertex);

    /**
     * Removes <i>all</i> edges between {@code vertex1} and {@code vertex2},
     * returning {@code true} if any edges were removed
     */
    boolean removeEdge(int vertex1, int vertex2);

    /**
     * Removes the edge between {@code vertex1} and {@code vertex2} with the
     * provided type, returning {@code true} if the edge existed and was
     * removed.
     */
    boolean removeEdge(int vertex1, int vertex2, T edgeType);
        
}
