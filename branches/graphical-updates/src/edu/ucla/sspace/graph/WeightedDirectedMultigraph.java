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
 * An interface specification for interacting with weighted, directed <a
 * href="http://en.wikipedia.org/wiki/Multigraph">Multigraph</a> objects.  This
 * interface unifies the {@link DirectedMultigraph} and {@link
 * WeigthedMultigraph} interfaces by associating each edge with a type,
 * orientation, and a numeric value reflecting the strength of connection
 * between the two vertices.
 *
 * @author David Jurgens
 */
public interface WeightedDirectedMultigraph<T> {
    // extends DirectedMultigraph, WeightedMultigraph {

    /**
     * Returns the set of directed edges where {@code vertex} is the head of the
     * edge
     */
    Set<? extends WeightedDirectedTypedEdge> inEdges(int vertex);

    /**
     * Returns the sum of the weights for all directed edges where {@code
     * vertex} is the tail of the edge, i.e. the edge originates at {@code
     * vertex}
     */
    double outDegreeWeight(int vertex);

    /**
     * Returns the set of directed edges where {@code vertex} is the tail of the
     * edge, i.e. the edge originates at {@code vertex}
     */
    Set<? extends WeightedDirectedTypedEdge> outEdges(int vertex);    

    /**
     * Returns the sum of the weights for all directed edges where {@code
     * vertex} is the head of the edge, i.e. the edge points to {@code vertex}
     */
    double inDegreeWeight(int vertex);

    /**
     * Returns the set of edges contained in this graph.
     */
    Set<? extends WeightedDirectedTypedEdge> edges();

    /**
     * Returns the set of edges connected to the provided vertex.
     */
    Set<? extends WeightedDirectedTypedEdge> getAdjacencyList(int vertex);
    
    /**
     * {@inheritDoc}
     */
    WeightedDirectedMultigraph subgraph(Set<Integer> vertices);

    /**
     * {@inheritDoc}
     */
    WeightedDirectedMultigraph subgraph(Set<Integer> vertices, 
                                        Set<T> edgeTypes);
    
}