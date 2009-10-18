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

package edu.ucla.sspace.common;


import edu.ucla.sspace.vector.Vector;

import java.io.File;
import java.util.Queue;

/**
 * An interface for classes which will maintain and utilize random indexes.
 * The main purpose of this of this class is to allow any algorithm which makes
 * use of some sort of random index, such as Random Indexing, Beagle, or other
 * varients, can easily swap out the type of indexing used for
 * experimentation purposes.
 *
 * The main work should be done in {@code updateMeaningWithTerm} where the words
 * prior to the focus word are passed in as a {@code Queue}, and the words after
 * the focus word are also passed in as a {@code Queue}.  Implementations are
 * expected to traverse these {@code Queue}s in any order that makes sense, and
 * should update {@code meaning} with whatever information is computed.
 */
// TODO: Merge this with David's index building classes.  Make sure we can
// incorporate or mix/match permutation abilities.
public interface IndexBuilder {

    /**
     * Given a current meaning vector, update it using index vectors from a given
     * window of words.
     *
     * @param meaning An existing meaning vector. After calling this method, the
     *                values of meaning will be updated according to some scheme.
     * @param prevWords words appearing before the focus word whose meaning is
     *                being updated
     * @param nextWords words appearing after the focus word whose meaning is
     *                being updated
     */
    public void updateMeaningWithTerm(Vector meaning,
                                      Queue<String> prevWords,
                                      Queue<String> nextWords);

    /**
     * Read in an set of existing index vectors from a binary file, along with any
     * other data needed to represent the index vector accurately.
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
     * Return a new {@code Vector} appropriate for use with a specific {@code
     * IndexBuilder}.  For {@code IndexBuilder}s which use a dense representation
     * internally, a {@code DenseVector} would be appropriate, whereas a {@code
     * SparseVector} would be fitting for {@code IndexBuilder}s which utilize
     * sparse representations internally.
     *
     * @return a {@Vector} having the same type as the vectors used internally.
     */
    public Vector getSemanticVector();

    /**
     * Return the number of words expected before the focus word when examining
     * a context window.
     *
     * @return The number of terms prior to the focus word.
     */
    public int expectedSizeOfPrevWords();

    /**
     * Return the number of words expected after the focus word when examining
     * a context window.
     *
     * @return The number of terms after the focus word.
     */
    public int expectedSizeOfNextWords();
}
