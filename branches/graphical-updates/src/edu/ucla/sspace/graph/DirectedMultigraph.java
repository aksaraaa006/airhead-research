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
 * An interface specification for interacting with directed <a
 * href="http://en.wikipedia.org/wiki/Multigraph"> multigraph</a> objects.  This
 * interface refines the {@link Multigraph} interface by providing information
 * on the edge orientations as well as the in and out degree of its vertices.
 *
 * @author David Jurgens
 */
public interface DirectedMultigraph<T>  {
// extends Multigraph, DirectedGraph {

    /**
     * Returns the number of directed edges of the specified type where {@code
     * vertex} is the head of the edge, i.e. the edge points to {@code vertex}.
     */
    int inDegree(int vertex, T edgeType);

    /**
     * Returns the set of all directed edges of all edge types where {@code
     * vertex} is the head of the edge
     */
    Set<? extends DirectedTypedEdge<T>> inEdges(int vertex);

    /**
     * Returns the set of all directed edges of the specified type where {@code
     * vertex} is the head of the edge
     */
    Set<? extends DirectedTypedEdge<T>> inEdges(int vertex, T edgeType);

    /**
     * Returns the number of directed edges of the specified type where {@code
     * vertex} is the tail of the edge, i.e. the edge originates at {@code
     * vertex}
     */
    int outDegree(int vertex, T edgeType);

    /**
     * Returns the set of all directed edges of all edge types where {@code
     * vertex} is the tail of the edge, i.e. the edge originates at {@code
     * vertex}
     */
    Set<? extends DirectedTypedEdge<T>> outEdges(int vertex);    

    /**
     * Returns the set of all directed edges of the specified type where {@code
     * vertex} is the tail of the edge, i.e. the edge originates at {@code
     * vertex}
     */
    Set<? extends DirectedTypedEdge<T>> outEdges(int vertex, T edgeType);    

//     /**
//      * {@inheritDoc}
//      */
//     DirectedMultigraph subgraph(Set<Integer> vertices);

//     /**
//      * {@inheritDoc}
//      */
//     DirectedMultigraph subgraph(Set<Integer> vertices, Set<T> edgeTypes);
    
}
