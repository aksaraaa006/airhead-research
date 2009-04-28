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

/**
 * An interface for classes which will maintain and utilize random indexes.
 * The main purpose of this of this class is to allow any algorithm which makes
 * use of some sort of random index, such as Random Indexing, Beagle, or other
 * varients, can easily swap out the type of random indexing used for
 * experimentation purposes.  Implementations should be thread safe.
 */
public interface IndexBuilder {
  /** 
   * Add a new index vector for this term if one does not already exist.
   */
  public void addTermIfMissing(String term);

  /**
   * Given a current meaning vector, update it using index vectors from a given
   * window of words.
   *
   * @param meaning An existing meaning vector.  After calling this method, the
   * values of meaning will be updated according to some scheme.
   * @param context An array of words, each of which will be combined with
   * meaning.
   */
  public void updateMeaningWithTerm(double[] meaning, String[] context);
}
