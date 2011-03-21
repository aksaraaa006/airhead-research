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
 * An interface specification for interacting with weighted <a
 * href="http://en.wikipedia.org/wiki/Graph_(mathematics)">Graph</a> objects.
 * This interface refines the {@link Graph} interface to associate each edge
 * with a numeric value reflecting the strength of connection between the two
 * vertices.  
 *
 * <p> This interface permits having 0 or negative edge weights.
 *
 * @author David Jurgens
 */
public interface WeightedGraph extends Graph {

    /**
     * Adds an edge between the two vertices with the <i>default</i> weight,
     * returning {@code true} if the edge was not previously present.
     * Implemenations are free to decide the behavior for cases where <ul> <li>
     * no default weight has been specified <li> one or both of the vertices are
     * not currently in the graph, and <li> whether self-edges are allowed
     * (i.e. {@code vertex1 == vertex2}). </ul>
     */
    boolean addEdge(int vertex1, int vertex2);

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
    boolean addEdge(int vertex1, int vertex2, double weight);

    /**
     * Returns the set of edges contained in this graph.
     */
    @Override Set<? extends WeightedEdge> edges();

    /**
     * Returns the set of edges connected to the provided vertex.
     */
    @Override Set<? extends WeightedEdge> getAdjacencyList(int vertex);
    
    /**
     * {@inheritDoc}
     */
    @Override WeightedEdge getEdge(int vertex1, int vertex2);

    /**
     * Returns the weigth for the edge connecting the two vertices.
     *
     * @throws IllegalArgumentException if an edge does not exist between the
     *         two vertices.
     */
    double getEdgeWeight(int vertex1, int vertex2);
    
//     /**
//      * {@inheritDoc}
//      */
//     WeightedGraph subgraph(Set<Integer> vertices);
    
}
