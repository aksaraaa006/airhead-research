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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.ucla.sspace.util.OpenIntSet;


/**
 * An {@link EdgeSet} implementation that imposes no restrictions on the type of
 * edges that may be contained within.  This implemenation is fundamentally
 * {@link Edge}-based and any vertex-based operations are expected to take O(n)
 * time, where <i>n</i> is the number of edges in this set.
 *
 * @author David Jurgens
 *
 * @param T the type of edge to be stored in the set
 */
public class GenericEdgeSet<T extends Edge> extends AbstractSet<T> 
        implements EdgeSet<T> {
        
    private final int rootVertex;
    
    private final Set<T> edges;
    
    public GenericEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        edges = new HashSet<T>();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(T e) {
        return (e.from() == rootVertex || e.to() == rootVertex)
            && edges.add(e);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> connected() {
        Set<Integer> v = new HashSet<Integer>();
        for (T e : edges) {
            v.add(e.from());
            v.add(e.to());
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        for (T e : edges) {
            if (vertex == e.from() || vertex == e.to())
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        return edges.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getEdges(int vertex) {
        Set<T> toReturn = new HashSet<T>();
        for (T e : edges) {
            if (vertex == e.from() || vertex == e.to())
                toReturn.add(e);
        }
        return toReturn;
    }    

    /**
     * {@inheritDoc}
     */
    public int getRoot() {
        return rootVertex;
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<T> iterator() {
        return edges.iterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        return edges.remove(o);
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return edges.size();
    }
}