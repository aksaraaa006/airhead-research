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

import edu.ucla.sspace.common.DocumentSpace;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.SemanticVector;
import edu.ucla.sspace.vector.SparseSemanticVector;
import edu.ucla.sspace.text.IteratorFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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
public class ExplicitSemanticAnalysis implements DocumentSpace {
  public static final String ESA_SSPACE_NAME =
    "esa-semantic-space";

  /**
   * The logger for this class based on the fully qualified class name
   */
  private static final Logger ESA_LOGGER = 
    Logger.getLogger(ExplicitSemanticAnalysis.class.getName());
    
  /**
   * The term by wiki article occurrence matrix.  This field is set in {@link
   * processDocument(BufferedReader) processDocument}.
   */
  private Matrix termWikiMatrix;

  private final ConcurrentMap<String, Integer> termToIndex;

  private final ArrayList<String> indexToArticle;

  private AtomicInteger articleCount;
  private AtomicInteger termCounter;

  public ExplicitSemanticAnalysis() {
    indexToArticle = new ArrayList<String>();
    termToIndex = new ConcurrentHashMap<String, Integer>();
    termWikiMatrix = new GrowingSparseMatrix();
    articleCount = new AtomicInteger(0);
    termCounter = new AtomicInteger(0);
  }

  /**
   * Parses the provided Wikipedia article.
   *
   * @param article A wikipedia article.
   */
  public void processDocument(BufferedReader article) throws IOException {
    Map<String, Integer> termCounts =
      new LinkedHashMap<String, Integer>(1 << 10, 16f);  

    Iterator<String> articleTokens = IteratorFactory.tokenize(article);

    if (!articleTokens.hasNext())
      return;

    String articleName = articleTokens.next();
    // for each word in the text article, keep a count of how many
    // times it has occurred
    while (articleTokens.hasNext()) {
      String word = articleTokens.next();
        
      // Add the term to the total list of terms to ensure it has a
      // proper index.  If the term was already added, this method is
      // a no-op
      addTerm(word);
      Integer termCount = termCounts.get(word);

      // update the term count
      termCounts.put(word, (termCount == null) 
          ? Integer.valueOf(1)
          : Integer.valueOf(1 + termCount.intValue()));
    }

    article.close();

    // check that we actually loaded in some terms before we increase the
    // articleCount.  This could possibly save some dimensions in the final
    // array for articles that were essentially blank.  If we didn't see
    // any terms, just return 0
    if (termCounts.isEmpty())
        return;

    int articleIndex = 0;
    synchronized (this) {
      articleIndex = articleCount.incrementAndGet();
      indexToArticle.add(articleIndex, articleName);
    }

    // Once the article has been fully parsed, output all of the sparse
    // data points using the writer.  Synchronize on the writer to prevent
    // any interleaving of output by other threads
    synchronized(termWikiMatrix) {
      for (Map.Entry<String, Integer> e : termCounts.entrySet()) {
        String term = e.getKey();
        int count = e.getValue().intValue();
        int termIndex = termToIndex.get(term).intValue();
        termWikiMatrix.set(termIndex, articleIndex, count);
      }
    }
  }

  /**
   * Represent a document as the summation of term SemanticVectors.
   * {@inheritDoc}
   */
  public SemanticVector representDocument(BufferedReader document)
    throws IOException {
    SemanticVector documentVector = new SparseSemanticVector(getVectorSize());
    Iterator<String> articleTokens = IteratorFactory.tokenize(article);
    while (articleTokens.hasNext()) {
      SemanticVector termVector = getSemanticVectorFor(articleTokens.next());
      // Add the term Vector to the document Vector.
    }
    return documentVector;
  }

  /**
   * {@inheritDoc}
   */
  public void processSpace(Properties properties) {
  }

  /**
   * Adds the term to the list of terms and gives it an index, or if the term
   * has already been added, does nothing.
   */
  private void addTerm(String term) {
    Integer index = termToIndex.get(term);
    if (index == null) {
      synchronized(this) {
        // recheck to see if the term was added while blocking
        index = termToIndex.get(term);

        // if some other thread has not already added this term while
        // the current thread was blocking waiting on the lock, then add
        // it.
        if (index == null) {
          index = Integer.valueOf(termCounter.incrementAndGet());
          termToIndex.put(term, index);
        }
      }
    }
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
    return articleCount.get();
  }

  /**
   * {@inheritDoc}
   */
  public double[] getVectorFor(String word) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public SemanticVector getSemanticVectorFor(String word) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return Collections.unmodifiableSet(termToIndex.keySet());
  }    
}
