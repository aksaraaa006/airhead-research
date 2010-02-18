/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.clustering;


/**
 * A simple implementation of a {@link Assignment} that only supports
 * hard assignments.
 */
public class HardAssignment implements Assignment {

    /**
     * The array holding the single assignment.
     */
    private int[] assignments;

    /**
     * Creates a new {@link HardAssignment} where there is no
     * assignment provided.
     */
    public HardAssignment() {
        assignments = new int[0];
    }

    /**
     * Creates a new {@link HardAssignment} where there is only one
     * assignment provided.
     */
    public HardAssignment(int assignment) {
        assignments = new int[1];
        assignments[0] = assignment;
    }

    /**
     * {@inheritDoc}
     */
    public int[] assignments() {
        return assignments;
    }
}
