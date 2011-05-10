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

import java.util.Map;
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
public interface WeightedMultigraph<T>  {
// extends Multigraph, WeightedGraph {

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
    WeightedTypedEdge<T> getEdge(int vertex1, int vertex2);

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
