/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.esa;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.AtomicGrowingMatrix;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.text.WordIterator;

import java.io.BufferedReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of Explicit Semanic Analysis proposed by Evgeniy
 * Gabrilovich and Shaul Markovitch.  For full details see:
 *
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Evgeniy Gabrilovich and
 *     Shaul Markovitch. (2007). "Computing Semantic Relatedness using
 *     Wikipedia-based Explicit Semantic Analysis," Proceedings of The 20th
 *     International Joint Conference on Artificial Intelligence (IJCAI),
 *     Hyderabad, India, January 2007. </li>
 *
 * </ul>
 *
 * @author David Jurgens
 */
public class ExplicitSemanticAnalysis implements SemanticSpace {
    public static final String ESA_SSPACE_NAME =
    "esa-semantic-space";

    /**
     * The logger for this class based on the fully qualified class name
     */
    private static final Logger ESA_LOGGER = 
	Logger.getLogger(ExplicitSemanticAnalysis.class.getName());
    
    private Map<Integer, String> indexToArticleName;

    private Map<String, Integer> termToIndex;

    private Map<String, Integer> termDocCount;

    /**
     * The article co-occurrence matrix.  This field is set in {@link
     * processDocument(BufferedReader) processDocument} after the number of
     * valid articles is known.
     */
    private Matrix termDocMatrix;

    private AtomicInteger articleCounter;
    private AtomicInteger termCounter;

    public ExplicitSemanticAnalysis() {
      articleCounter = new AtomicInteger(0);
      indexToArticleName = new ConcurrentHashMap<Integer, String>(7000000);
      termToIndex = new ConcurrentHashMap<String, Integer>();
      termDocCount = new ConcurrentHashMap<String, Integer>();
      termDocMatrix = new AtomicGrowingMatrix();
    }

    /**
     */
    public void processDocument(BufferedReader wikipediaSnapshot) {
      Iterator<String> words = new WordIterator(wikipediaSnapshot);
      String word = null;

      // Create the title from the document.
      StringBuilder titleBuilder = new StringBuilder();
      while (words.hasNext() && !(word = words.next()).equals("|"))
        titleBuilder.append(word).append(" ");

      if (!words.hasNext())
        // If no title exists, or there is nothing existing after the title.
        return;

      // Count the number of times each word occurs in the document.
      Map<String, Integer> termCounts =
        new LinkedHashMap<String, Integer>(1 << 10, 16f);
      while (words.hasNext()) {
        word = words.next();
        addTerm(word);
        Integer termCount = termCounts.get(word);
        termCounts.put(word, (termCount == null) ?
                       Integer.valueOf(1) :
                       Integer.valueOf(1 + termCount.intValue()));
      }

      // Empty documents should be ignored.
      if (termCounts.isEmpty())
        return;

      // Record the counts in the term doc matrix.
      int docIndex = articleCounter.incrementAndGet();
      indexToArticleName.put(docIndex, titleBuilder.toString());
      for (Map.Entry<String, Integer> termCount : termCounts.entrySet()) {
        int termIndex = termToIndex.get(termCount.getKey()).intValue();
        termDocMatrix.set(termIndex, docIndex,
                          termCount.getValue().intValue());
        incrementTermCount(termCount.getKey());
      }
    }

    private void incrementTermCount(String term) {
      Integer index = termDocCount.get(term);
      if (index == null) {
        synchronized(this) {
          index = termToIndex.get(term);
          termDocCount.put(term, (index == null) ?
                           0 : 1 + index.intValue());
        }
      }
    }

    private void addTerm(String term) {
      Integer index = termToIndex.get(term);
      if (index == null) {
        synchronized(this) {
          index = termToIndex.get(term);
          if (index == null)
            termToIndex.put(term, termCounter.incrementAndGet());
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
      return ESA_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorSize() {
      return articleCounter.get() - 1;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String word) {
      Integer index = termToIndex.get(word);
      return (index == null) ? null : termDocMatrix.getRow(index.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
      return Collections.unmodifiableSet(termToIndex.keySet());
    }
}
