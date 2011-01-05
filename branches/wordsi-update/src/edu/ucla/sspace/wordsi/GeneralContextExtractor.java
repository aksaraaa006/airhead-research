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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;


/**
 * A general purpose {@link ContextExtractor}.  This extractor assumes that
 * documents are simply raw text and contexts should be defined by word
 * co-occurrences.  This class depends on a {@link ContextGenerator} for
 * generating context vectors.
 *
 * @author Keith Stevens
 */
public class GeneralContextExtractor implements ContextExtractor {

  /**
   * The generator responsible for creating context vectors from a sliding
   * window of text.
   */
  private final ContextGenerator generator;

  /**
   * The size of the sliding window of text, in a single direction.
   */
  private final int windowSize;

  /**
   * Creates a new {@link GeneralContextExtracto}.
   *
   * @param generator The {@link ContextGenerator} responsible for creating
   *        context vectors
   * @param windowSize The number of words before and after the focus word which
   *        compose a context
   */
  public GeneralContextExtractor(ContextGenerator generator,
                                 int windowSize) {
    this.generator = generator;
    this.windowSize = windowSize;
  }

  /**
   * {@inheritDoc}
   */
  public int getVectorLength() {
    return generator.getVectorLength();
  }

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document, Wordsi wordsi) {
    Queue<String> prevWords = new ArrayDeque<String>();
    Queue<String> nextWords = new ArrayDeque<String>();

    Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

    // Fill up the words after the context so that when the real processing
    // starts, the context is fully prepared.
    for (int i = 0 ; i < windowSize && it.hasNext(); ++i)
      nextWords.offer(it.next());

    // Iterate through each of the words in the context, generating context
    // vectors for each acceptable word.
    String focusWord = null;
    while (!nextWords.isEmpty()) {
      focusWord = nextWords.remove();

      // Advance the sliding window to the right.
      if (it.hasNext())
        nextWords.offer(it.next());

      // Represent the word if wordsi is willing to process it.
      if (wordsi.acceptWord(focusWord)) {
        SparseDoubleVector contextVector = generator.generateContext(
            prevWords, nextWords);
        wordsi.handleContextVector(focusWord, focusWord, contextVector);
      }

      // Advance the sliding window to the right.
      prevWords.offer(focusWord);
      if (prevWords.size() > windowSize)
        prevWords.remove();
    }
  }
}
