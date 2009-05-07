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

package edu.ucla.sspace.holograph;

import edu.ucla.sspace.common.IndexBuilder;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.WordIterator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of the Beagle Semantic Space model.  This implementation is
 * based on
 * <p style="font-family:Garamond, Georgia, serif">Jones, M. N., Mewhort, D.
 * J.L. (2007).  Representing Word Meaning and Order Information in a Composite
 * Holographic Lexicon.  <i>Psychological Review</i> <b>114</b>, 1-37.
 * Available <a href="www.indiana.edu/~clcl/BEAGLE/Jones_Mewhort_PR.pdf">here</a></p>
 *
 * For every word, a unique random index vector is created, where the vector has
 * some large dimension (by default 512), with each entry in the vector being
 * from a random gaussian distribution.  The holographic meaning of a word is
 * updated by first adding the sum of index vectors for all the words in a
 * sliding window centered around the target term.  Additionally a sum of
 * convolutions of several n-grams is added to the holographic meaning.  The
 * main functionality of this class can be found in the {@link IndexBuilder}
 * class.
 */
public class Holograph implements SemanticSpace {
  public static final int CONTEXT_SIZE = 6;
  public static final String BEAGLE_SSPACE_NAME = 
    "holograph-semantic-space";

  private final IndexBuilder indexBuilder;
  private final Map<String, double[]> termHolographs;
  private final int indexVectorSize;
  private int prevSize;
  private int nextSize;

  public Holograph(IndexBuilder builder, int vectorSize) {
    indexVectorSize = vectorSize;
    indexBuilder = builder;
    prevSize = builder.expectedSizeOfPrevWords();
    nextSize = builder.expectedSizeOfNextWords();
    termHolographs = new ConcurrentHashMap<String, double[]>();
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return termHolographs.keySet();
  }

  /**
   * {@inheritDoc}
   */
  public double[] getVectorFor(String term) {
    return termHolographs.get(term);
  }

  /**
   * {@inheritDoc}
   */
  public String getSpaceName() {
    return BEAGLE_SSPACE_NAME + "-" + indexVectorSize;
  }

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    Queue<String> prevWords = new ArrayDeque<String>();
    Queue<String> nextWords = new ArrayDeque<String>();

    WordIterator it = new WordIterator(document);

    for (int i = 0 ; i < nextSize && it.hasNext(); ++i)
      nextWords.offer(it.next().intern());
    prevWords.offer("");

    String focusWord = null;
    while (!nextWords.isEmpty()) {
      focusWord = nextWords.remove();
      if (it.hasNext())
        nextWords.offer(it.next().intern());
      synchronized (focusWord) {
        double[] meaning = termHolographs.get(focusWord);
        if (meaning == null) {
          meaning = new double[indexVectorSize];
          termHolographs.put(focusWord, meaning);
        }
        indexBuilder.updateMeaningWithTerm(meaning, prevWords, nextWords);
      }
      prevWords.offer(focusWord);
      if (prevWords.size() > prevSize)
        prevWords.remove();
    }
  }
  
  /**
   * No processing is performed on the holographs.
   */
  public void processSpace(Properties properties) {
  }

}
