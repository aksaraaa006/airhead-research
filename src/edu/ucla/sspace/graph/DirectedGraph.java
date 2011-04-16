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
 * href="http://en.wikipedia.org/wiki/Directed_graph">directed graph</a>
 * objects.
 *
 * @author David Jurgens
 */
public interface DirectedGraph extends Graph {

    /**
     * Returns the number of directed edges where {@code vertex} is the head of
     * the edge, i.e. the edge points to {@code vertex}.
     */
    int inDegree(int vertex);

    /**
     * Returns the set of directed edges where {@code vertex} is the head of the
     * edge
     */
    Set<? extends DirectedEdge> inEdges(int vertex);

    /**
     * Returns the number of directed edges where {@code vertex} is the tail of
     * the edge, i.e. the edge originates at {@code vertex}
     */
    int outDegree(int vertex);

    /**
     * Returns the set of directed edges where {@code vertex} is the tail of the
     * edge, i.e. the edge originates at {@code vertex}
     */
    Set<? extends DirectedEdge> outEdges(int vertex);    

    /**
     * Returns the set of vertices that point to this vertex.  That is, the set
     * of vertices for which there is an incoming edge to the vertex.
     */
    Set<Integer> predecessors(int vertex);

    /**
     * Returns the set of vertices that can be reached by following the outgoing
     * edges from this vertex.
     */
    Set<Integer> successors(int vertex);

//     /**
//      * {@inheritDoc}
//      */
//     DirectedGraph subgraph(Set<Integer> vertices);
    
}
