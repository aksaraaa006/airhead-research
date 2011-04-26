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

public class SimpleTypedEdge<T> implements TypedEdge<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final int from;

    private final int to;

    private final T type;

    public SimpleTypedEdge(T edgeType, int from, int to) {
        this.type = edgeType;
        this.from = from;
        this.to = to;
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <E extends Edge> E clone(int from, int to) {
        return (E)(new SimpleTypedEdge(type, from, to));
    }

    /**
     * {@inheritDoc}
     */
    public T edgeType() {
        return type;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge)o;
            // REMINDER: fix me
            return e.from() == from && e.to() == to;
        }
        return false;
    }

    public int hashCode() {
        return from ^ to;
    }

    /**
     * {@inheritDoc}
     */
    public Edge flip() {
        return new SimpleEdge(to, from);
    }

    /**
     * {@inheritDoc}
     */
    public int from() {
        return from;
    }

    /**
     * {@inheritDoc}
     */
    public int to() { 
        return to;
    }

    public String toString() {
        return "(" + from + "<-->" + to + "):" + type;
    }
}