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


public interface EdgeSet<T extends Edge> extends Set<T> {  

    /**
     * Adds the edge to the set only if the edge is connected to the root
     * vertex.
     *
     * @return {@code true} if the edge was added, {@code false} if the edge was
     *         already present, or if it could not be added to this edge set due
     *         to the root vertex not being connected to the edge
     */
    boolean add(T edge);

    /**
     * Returns the set of vertices connected to the root edges.  Implementations
     * are left free to decide whether modifications to this set are allowed.
     */
    Set<Integer> connected();

    /**
     * Adds an edge between the root vertex and the provided vertex.
     *
     * @returns {@code true} if the edge between the root and provided vertex
     *          was not previously present and was successfully added, {@code
     *          false} otherwise
     */
    boolean connect(int vertex);   

    /**
     * Returns true if the root vertex is connected to the provided vertex.
     */
    boolean connects(int vertex);

    /**
     * Removes the edge between the root vertex and the provided vertex.
     *
     * @returns {@code true} if the edge between the root and provided vertex
     *          was present and subsequenetly removed, {@code false} if the edge
     *          was not present to begin with.
     */
    boolean disconnect(int vertex);

    /**
     * Returns the {@link Edge} that connects the root vertex with this vertex
     * or {@code null} if no such edges exist.  If this edge set supports
     * parallel edges between two vertices, an implementation is left free to
     * decide which of the possible edges is returned.
     */
    T getEdge(int vertex);

    /**
     * Returns the vertex to which all edges in this set are connected.
     */
    int getRoot();
}