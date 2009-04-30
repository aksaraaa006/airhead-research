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
import edu.ucla.sspace.common.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
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
 * updated by first adding the sum of index vectors for all the words in a sliding
 * window centered around the target term.  Additionally a sum of convolutions
 * of several n-grams is added to the holographic meaning.  The main
 * functionality of this class can be found in the {@link RandomIndexBuilder}
 * class.
 *
 * Currently this class is not thread safe and does not accept Properties.
 */
public class Holograph implements SemanticSpace {
  public static final int CONTEXT_SIZE = 6;
  public static final int LINES_TO_SKIP = 40;
  public static final int MAX_LINES = 500;

  private final IndexBuilder indexBuilder;
  private final Map<String, double[]> termHolographs;
  private final int indexVectorSize;

  public Holograph(IndexBuilder builder) {
    indexVectorSize = 512;
    indexBuilder = builder;
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
  public void processDocument(BufferedReader document) throws IOException {
    LinkedList<String> words = new LinkedList<String>();
    for (String line = null; (line = document.readLine()) != null;) {
      // split the line based on whitespace
      String[] text = line.split("\\s");
      for (String word : text) {
        // clean up each word before entering it into the matrix
        String cleaned = StringUtils.cleanup(word).intern();
        // skip any mispelled or unknown words
        if (!StringUtils.isValid(cleaned))
          continue;
        words.add(cleaned);
        indexBuilder.addTermIfMissing(cleaned);
        updateHolograph(words);
      }
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public void processSpace(Properties properties) {
  }

  private void updateHolograph(LinkedList<String> words) {
    if (words.size() < CONTEXT_SIZE) {
      return;
    }
    String[] context = words.toArray(new String[0]);
    String mainWord = context[1];
    context[1] = "";
    synchronized (mainWord) {
      double[] meaning = termHolographs.get(mainWord);
      if (meaning == null) {
        meaning = new double[indexVectorSize];
        termHolographs.put(mainWord, meaning);
      }
      indexBuilder.updateMeaningWithTerm(meaning, context);
      words.removeFirst();
    }
  }
}
