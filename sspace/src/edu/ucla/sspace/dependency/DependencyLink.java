/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.dependency;

/**
 * A simple struct that represents a single link in a dependency parse tree.
 * This struct contains the relation that connects two nodes and the index of
 * the neighboring node.
 */
public class DependencyLink {

    private String relation;
    private int neighbor;
    private boolean isHeadNode;

    /**
     * Creates a new {@link DependencyLink}
     */
    public DependencyLink(int neighbor, String relation, boolean isHeadNode) {
        this.relation = relation;
        this.neighbor = neighbor;
        this.isHeadNode = isHeadNode;
    }

    /**
     * Returns the relation the the current node has with it's neighbor.
     */
    public String relation() {
        return relation;
    }

    /**
     * Returns the neighbor to the current node.
     */
    public int neighbor() {
        return neighbor;
    }

    public boolean isHeadNode() {
        return isHeadNode;
    }
}
