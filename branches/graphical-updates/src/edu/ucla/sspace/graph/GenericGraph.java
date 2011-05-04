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
 * A graph that imposes no restriction on the types of edges that may connect
 * its vertices.
 */
public class GenericGraph<T extends Edge> 
        extends AbstractGraph<T,GenericEdgeSet<T>> {

    private static final long serialVersionUID = 1L;
    
    public GenericGraph() { }
    
    /**
     * Creates a new {@code GenericGraph} with a copy of all the edges and
     * vertices contained within {@code g}
     */
    public GenericGraph(Graph<? extends T> g) {
        for (int v : g.vertices())
            addVertex(v);
        for (T edge : g.edges())
            addEdge(edge);
    }

    @Override protected GenericEdgeSet<T> createEdgeSet(int vertex) {
        return new GenericEdgeSet<T>(vertex);
    }
    
}
