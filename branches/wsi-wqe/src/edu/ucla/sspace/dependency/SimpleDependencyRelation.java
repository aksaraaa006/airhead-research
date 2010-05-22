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
 * A simple {@link DependencyRelation} that is created from a token string,
 * a relation string, and a boolean specifiying if the token is a head node.
 */
public class SimpleDependencyRelation implements DependencyRelation {

    /**
     * The token represented by this relation.
     */
    private String token;

    /**
     * The relation string.
     */
    private String relation;

    /**
     * Specifies whether or not {@code token} is a head node in the real
     * dependency parse tree.
     */
    private boolean isHeadNode;

    /**
     * Creates a {@link SimpleDependencyRelation}.
     */
    public SimpleDependencyRelation(String token,
                                    String relation,
                                    boolean isHeadNode) {
        this.token = token;
        this.relation = relation;
        isHeadNode = isHeadNode;
    }

    /**
     * {@inheritDoc}
     */
    public String token() {
        return token;
    }

    /**
     * {@inheritDoc}
     */
    public String pos() {
        // Current return nothing.
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public String relation() {
        return relation;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHeadNode() {
        return isHeadNode;
    }
}
