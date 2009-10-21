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

import edu.ucla.sspace.vector.Vector;

import java.io.File;


/**
 * An interface for classes which will maintain and generate random indexes.
 * The main purpose of this of this class is to allow any algorithm which makes
 * use of some sort of random index, such as Random Indexing, Beagle, or other
 * varients, can easily swap out the type of indexing used for
 * experimentation purposes.
 */
public interface IndexGenerator {

    /**
     * Read in an set of existing index vectors from a binary file, along with
     * any other data needed to represent the index vector accurately.
     *
     * @param vectorFile The file representing the stored {@code IndexBuiler}.
     */
    public void loadIndexVectors(File vectorFile);

    /**
     * Store the set of existing index vectors to a binary file, along with any
     * other data needed to represent the index vector accurately.
     *
     * @param vectorFile The file which will store the {@code IndexBuiler}.
     */
    public void saveIndexVectors(File vectorFile);

    /**
     * Return a new, emtpy {@code Vector} appropriate for use with a specific
     * {@code IndexBuilder}.  For {@code IndexBuilder}s which use a dense
     * representation internally, a {@code DenseVector} would be appropriate,
     * whereas a {@code SparseVector} would be fitting for {@code IndexBuilder}s
     * which utilize sparse representations internally.
     *
     * @return a {@Vector} having the same type as the vectors used internally.
     */
    public Vector getEmtpyVector();

    /**
     * Return an index {@code Vector} for the given term, if no {@code Vector}
     * exists for {@code term}, then generate a new one.  Any modification done
     * by this method must be thread safe.
     *
     * @param term The term specifying the index {@code Vector} to return.
     *
     * @return A {@code Vector} corresponding to {@code term}.
     */
    public Vector getIndexVector(String term);
}
