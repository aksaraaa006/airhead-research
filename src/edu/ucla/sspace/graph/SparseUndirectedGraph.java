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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.OpenIntSet;


/**
 * A undirected {@link Graph} implementation backed by a adjacency matrix.  This
 * class performs best for graphs with a small number of edges.
 *
 * @author David Jurgens
 */
public class SparseUndirectedGraph extends AbstractGraph<Edge> {

    private static final long serialVersionUID = 1L;

    public SparseUndirectedGraph() { }
    
    /**
     * Creates a sparse edge set that treats all edges as symmetric.
     */
    @Override protected EdgeSet<Edge> createEdgeSet(int vertex) {
        return new SparseSymmetricEdgeSet(vertex);
    }

    ////
    //
    // Implementation note: @Override the three ordering sensitive methods.  If
    // we didn't then the AbstractGraph could possibly select the EdgeSet for
    // the vertex with the higher index, which would not perform any of thte
    // operations by construction.
    //
    //// 

    /**
     * {@inheritDoc}
     */
    @Override public boolean addEdge(int vertex1, int vertex2) {
        // Make sure v1 < v2 so that the right edge set will be selected 
        if (vertex1 > vertex2) {
            int tmp = vertex2;
            vertex2 = vertex1;
            vertex1 = tmp;
        }
        return super.addEdge(vertex1, vertex2);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Edge getEdge(int vertex1, int vertex2) {
        // Make sure v1 < v2 so that the right edge set will be selected 
        if (vertex1 > vertex2) {
            int tmp = vertex2;
            vertex2 = vertex1;
            vertex1 = tmp;
        }
        return super.getEdge(vertex1, vertex2);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override public boolean removeEdge(int vertex1, int vertex2) {
        // Make sure v1 < v2 so that the right edge set will be selected 
        if (vertex1 > vertex2) {
            int tmp = vertex2;
            vertex2 = vertex1;
            vertex1 = tmp;
        }
        return super.removeEdge(vertex1, vertex2);
    }
}
