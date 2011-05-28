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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A decorator over the {@link Graph} returned by {@link #subview(Set)} that
 * extends the functionality to support the {@link DirectedGraph} interface.
 */ 
class DirectedGraphAdaptor<T extends DirectedEdge> extends GraphAdaptor<T> 
    implements DirectedGraph<T>, java.io.Serializable  {

    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new adaptor over the graph.
     */
    public DirectedGraphAdaptor(Graph<T> g) {
        super(g);
    }

    /**
     * {@inheritDoc}
     */
    public int inDegree(int vertex) {
        int degree = 0;
        Set<T> edges = getAdjacencyList(vertex);
        if (edges == null)
            return 0;
        for (T e : edges) {
            if (e.to() == vertex)
                degree++;
        }
        return degree;
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> inEdges(int vertex) {
        // REMINDER: this is probably best wrapped with yet another
        // decorator class to avoid the O(n) penality of iteration over all
        // the edges
        Set<T> edges = getAdjacencyList(vertex);
        if (edges == null)
            return null;

        Set<T> in = new HashSet<T>();
        for (T e : edges) {
            if (e.to() == vertex)
                in.add(e);
        }
        return in;
    }

    /**
     * {@inheritDoc}
     */
    public int outDegree(int vertex) {
        int degree = 0;
        Set<T> edges = getAdjacencyList(vertex);
        if (edges == null)
            return 0;
        for (T e : edges) {
            if (e.from() == vertex)
                degree++;
        }
        return degree;
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> outEdges(int vertex) {
        // REMINDER: this is probably best wrapped with yet another
        // decorator class to avoid the O(n) penality of iteration over all
        // the edges
        Set<T> edges = getAdjacencyList(vertex);
        if (edges == null)
            return null;
        Set<T> out = new HashSet<T>();
        for (T e : edges) {
            if (e.from() == vertex)
                out.add(e);
        }
        return out;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> predecessors(int vertex) {
        Set<Integer> preds = new HashSet<Integer>();
        for (T e : inEdges(vertex))
            preds.add(e.from());
        return preds;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> successors(int vertex) {
        Set<Integer> succs = new HashSet<Integer>();
        for (T e : outEdges(vertex))
            succs.add(e.to());
        return succs;
    }

    /**
     * {@inheritDoc}
     */
    public DirectedGraph<T> subgraph(Set<Integer> vertices) {
        Graph<T> g = super.subgraph(vertices);
        return new DirectedGraphAdaptor<T>(g);
    }

//     /**
//      * {@inheritDoc}
//      */
//     public DirectedGraph<T> subview(Set<Integer> vertices) {
//         Graph<T> g = super.subview(vertices);
//         return new DirectedGraphAdaptor<T>(g);
//     }
}