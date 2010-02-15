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

package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.DoubleVector;


/**
 * An interface for classes which will maintain and generate random {@code
 * DoubleVector}s.  The main purpose of this of this class is to allow any
 * algorithm that makes use of some sort of random vector, such as Random
 * Indexing, can easily swap out the type of indexing used for experimentation
 * purposes.
 */
public interface DoubleVectorGenerator {

    /**
     * Creates an {@code VectorVector} with the provided length.
     *
     * @param length the length of the index vector
     *
     * @return an index vector
     */
    public DoubleVector generateRandomVector(int length);
}