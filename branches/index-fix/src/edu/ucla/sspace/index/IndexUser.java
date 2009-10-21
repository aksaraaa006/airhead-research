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


/**
 * An interface for classes which will utilize random indexes.  Implementations
 * are not intended to be theadsafe in any, and should be used with this
 * expectation.  Any particular usage of this class should be local to a thread,
 * and simply call {@code generateMeaning} everytime a new meaning {@code
 * Vector} needs to be generated.
 *
 * </p> Implementations may make use of this non-thread safety to store local
 * state, such as a small window of what terms have been recently inspected.  If
 * an implementation stores internal states, this should be explictly
 * documented.
 */
public interface IndexUser {

    /**
     * Generate a meaning {@code Vector} for some focus term, and a co-occuring
     * term which occurs some terms away.  This can include a simple summation
     * of the occurring term's {@code Vector} to the focus term's {@code
     * Vector}, or permuting the shape of the co-occurring {@code Vector} based
     * on the word's distance, or even storing n-grams to include more
     * information.
     *
     * @param focusVector The {@code Vector} representing the focus term.
     * @param termVector The {@code Vector} representing the co-occuring term.
     * @param distance The distance between {@code focusVector} and 
     *                 {@code termVector}.
     *
     * @return A vector which representings a meaning for {@code focusvector}
     * co-occuring with {@code termVector} {@code distance} terms away.
     */
    public Vector generateMeaning(Vector focusVector,
                                  Vector termVector,
                                  int distance);
}
