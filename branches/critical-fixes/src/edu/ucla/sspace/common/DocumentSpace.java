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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * A common interface for interacting with document space models of meaning.
 * Implementations should expect the sequence of processing steps declared in
 * SemanticSpace, with the addition of a new method primarily for developing a 
 * representation of a document.
 *
 * <ol>

 * <li> {@link #representDocument(BufferedReader) representDocument} is called
 *      when a document needs a semantic representation.
 *
 * </ol>
 *
 */
public interface DocumentSpace extends SemanticSpace {

  /**
   * Processes the contents of the provided file as a document.
   *
   * @param document a reader that allows access to the text of the document
   *
   * @throws IOException if any error occurs while reading the document
   */
  Vector representDocument(BufferedReader document) throws IOException;
}
