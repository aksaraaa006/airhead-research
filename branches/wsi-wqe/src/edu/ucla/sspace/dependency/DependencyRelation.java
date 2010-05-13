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
 * An interface for representing a dependency relationship in the form of (term,
 * relation) pair that composes a sequence of relations in a {@link
 * DependencyPath}.  In addition, this node is marked as a Head node, or a
 * governing node, if the token in this relationship governs the next token in
 * the {@link DependencyPath} sequence in the original dependency parse tree.
 */
public interface DependencyRelation {

    /**
     * Returns the token represented by this {@link DependencyRelation}.
     */
    public String token();

    /**
     * Returns the relation the the current has with the next token in a {@link
     * DependencyPath}.
     */
    public String relation();

    /**
     * Returns true if this token is the head word for the relationship
     * represented by this {@link DependencyRelation}.
     */
    public boolean isHeadNode();
}
