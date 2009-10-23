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

package edu.ucla.sspace.coals;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.Normalize;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SVD;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import edu.ucla.sspace.text.StringUtils;
import edu.ucla.sspace.text.IteratorFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of the COALS Semantic Space model.  This implementation is
 * based on:
 *
 * <p style="font-family:Garamond, Georgia, serif"> Rohde, D. L. T.,
 * Gonnerman, L. M., Plaut, D. C. (2005).  An Improved Model of Semantic
 * Similarity Based on Lexical Co-Occurrence. <i>Cognitive Science</i>
 * <b>(submitted)</b>.  Available <a
 * href="http://www.cnbc.cmu.edu/~plaut/papers/pdf/RohdeGonnermanPlautSUB-CogSci.COALS.pdf">here</a></p>
 *
 * COALS first computes a term by term co-occurrance using a ramped 4-word
 * window.  Once all documents have been processed, the raw counts are converted
 * into correlations.  Negative values are replaced with 0, and all other values
 * are replaced with their square root.  
 * From there, the semantic similarity of two words is best evaluated as the 
 * correlation of their vectors.
 *
 * As of right now this class is not thread safe, and still relies on the Jama
 * Matrix class.  It also does not accept any Properties.
 */
public class Coals implements SemanticSpace {
  public static final String REDUCE_MATRIX_PROPERTY =
    "edu.ucla.sspace.coals.Coals.reduce";
  public static final String REDUCE_MATRIX_DIMENSION_PROPERTY =
    "edu.ucla.sspace.coals.Coals.dimension";
  public static final String COALS_SSPACE_NAME = 
    "coals-semantic-space";
  private static final int MAX_SAVED_WORDS = 150000;

  /**
   * A temporary file containing temprorary word co-occurance counts from each
   * document.
   */
  private File rawOccurances;

  /**
   * The writer to the {@code rawTermDocMatrix}.
   */
  private PrintWriter rawOccuranceWriter;

  private HashMap<String, Integer> wordToIndex;
  private Map<String, Integer> totalWordFreq;
  Matrix finalCorrelation;
  private int maxWords;
  private boolean reduceMatrix;
  private int reducedDims;

  public Coals() {
    init(14000);
  }

  public Coals(int numWords) {
    init(numWords);
  }

  private void init(int numWords) {
    maxWords = numWords;
    wordToIndex = new HashMap<String, Integer>();
    totalWordFreq = new ConcurrentHashMap<String, Integer>();
    try {
      rawOccurances = File.createTempFile("coals-occurance-values", "dat");
      rawOccuranceWriter = new PrintWriter(rawOccurances);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(1);
    }
    finalCorrelation = null;
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return wordToIndex.keySet();
  }

  /**
   * {@inheritDoc}
   */
  public Vector getVector(String term) {
    if (wordToIndex.containsKey(term)) {
        int index = wordToIndex.get(term).intValue();
      return Vectors.immutableVector(finalCorrelation.getRowVector(index));
    }
    return null;
  }

  public String getSpaceName() {
    String ret = COALS_SSPACE_NAME;
    if (reduceMatrix)
      ret += "-svd-" + reducedDims;
    return ret;
  }

  public int getVectorLength() {
    return reducedDims;
  }

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    HashMap<String, Integer> wordFreq = new HashMap<String, Integer>();
    HashMap<Index, Integer> documentCorrels = new HashMap<Index, Integer>();
    Queue<String> prevWords = new ArrayDeque<String>();
    Queue<String> nextWords = new ArrayDeque<String>();
    Iterator<String> it = IteratorFactory.tokenizeOrdered(document);
    for (int i = 0; i < 4 && it.hasNext(); ++i)
      nextWords.offer(it.next());

    if (nextWords.size() < 4)
      return;

    while (!nextWords.isEmpty()) {
      String focusWord = nextWords.remove();

      if (it.hasNext())
        nextWords.offer(it.next());
      int updatedFreq = 1;
      if (wordFreq.containsKey(focusWord))
        updatedFreq += wordFreq.get(focusWord).intValue();
      wordFreq.put(focusWord, updatedFreq);
      Iterator<String> wordIter = prevWords.iterator();
      int offset = 4 - prevWords.size() + 1;
      for (int i = 0; wordIter.hasNext(); ++i) {
        String word = wordIter.next();
        addIfMissing(documentCorrels, new Index(focusWord, word), offset + i);
      }
      wordIter = nextWords.iterator();
      offset = 4;
      for (int i = 0; wordIter.hasNext(); ++i) { 
        String word = wordIter.next();
        addIfMissing(documentCorrels, new Index(focusWord, word), offset - i);
      }
      prevWords.offer(focusWord);
      if (prevWords.size() > 4)
        prevWords.remove();
    }
    synchronized (rawOccuranceWriter) {
      for (Map.Entry<Index, Integer> entry : documentCorrels.entrySet()) {
        StringBuffer sb = new StringBuffer(32);
        sb.append(entry.getKey().word).append("|")
          .append(entry.getKey().document).append("|").append(entry.getValue());
        rawOccuranceWriter.println(sb.toString());
      }
    }
    synchronized (this) {
      for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
        int newValue = entry.getValue().intValue();
        Integer v = totalWordFreq.get(entry.getKey());
        if (v != null)
          newValue += v.intValue();
        totalWordFreq.put(entry.getKey(), newValue);
      }
    }
  }

  private void addIfMissing(HashMap<Index, Integer> map, Index i, int value) {
    if (!map.containsKey(i))
      map.put(i, value);
    else
      map.put(i, map.get(i).intValue() + value);
  }

  /**
   * {@inheritDoc}
   */
  public void processSpace(Properties properties) {
    if (finalCorrelation == null)
      finalCorrelation = buildMatrix();
    int wordCount = finalCorrelation.rows();
    Normalize.byCorrelation(finalCorrelation, false);
    for (int i = 0; i < wordCount; ++i) {
      for (int j = 0; j < wordCount; ++j) {
        double newValue;
        if (finalCorrelation.get(i,j) < 0)
          newValue = 0;
        else 
          newValue = Math.sqrt(finalCorrelation.get(i,j));
        finalCorrelation.set(i,j, newValue);
      }
    }
    reduceMatrix = properties.getProperty(REDUCE_MATRIX_PROPERTY) != null;
    if (reduceMatrix) {
      try {
        File coalsMatrixFile =
          File.createTempFile("coals-term-doc-matrix", "txt");
        MatrixIO.writeMatrix(finalCorrelation,
                             coalsMatrixFile,
                             Format.SVDLIBC_DENSE_BINARY);
        String dims = properties.getProperty(REDUCE_MATRIX_DIMENSION_PROPERTY);
        reducedDims = (800> wordCount) ? wordCount : 800;
        if (dims != null)
          reducedDims = Integer.parseInt(dims);
        Matrix[] usv = SVD.svd(coalsMatrixFile, SVD.Algorithm.ANY,
                               Format.SVDLIBC_DENSE_BINARY, reducedDims);
        finalCorrelation = usv[0];
      } catch (IOException ioe) {
        throw new IOError(ioe);
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException(
            REDUCE_MATRIX_DIMENSION_PROPERTY + " is not an integer");
      }
    }
  }

  private Matrix buildMatrix() {
    ArrayList<Map.Entry<String, Integer>> wordCountList =
      new ArrayList<Map.Entry<String, Integer>>(totalWordFreq.entrySet());
    Collections.sort(wordCountList, new EntryComp());
    for (int i = 0; i < wordCountList.size(); ++i)
      wordToIndex.put(wordCountList.get(i).getKey(), i);

    totalWordFreq.clear();
    synchronized (rawOccuranceWriter) {
      rawOccuranceWriter.close();
    }
    
    int wordCount =
      (wordCountList.size() > maxWords) ? maxWords : wordCountList.size();
    try {
      BufferedReader br = new BufferedReader(new FileReader(rawOccurances));
      String line = null;
      Matrix correl = new SparseMatrix(wordCountList.size(), wordCount);
      while ((line = br.readLine()) != null) {
        String[] splitLine = line.split("\\|");
        if (splitLine[0].equals(IteratorFactory.EMPTY_TOKEN) ||
            splitLine[1].equals(IteratorFactory.EMPTY_TOKEN))
          continue;
        int r = wordToIndex.get(splitLine[0]).intValue();
        int c = wordToIndex.get(splitLine[1]).intValue();
        double value = Double.parseDouble(splitLine[2]);
        if (r >= MAX_SAVED_WORDS || c >= wordCount || value == 0.0) 
          continue;
        correl.set(r, c, value + correl.get(r, c));
      }
      return correl;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  public Matrix compareToMatrix(Matrix o) {
    if (finalCorrelation == null)
      finalCorrelation = buildMatrix();
    Matrix returnMatrix =
      new ArrayMatrix(finalCorrelation.rows(), finalCorrelation.columns());
    for (int i = 0; i < finalCorrelation.rows(); ++i) {
      for (int j = 0; j < finalCorrelation.columns(); ++j) {
        returnMatrix.set(i, j, o.get(i, j) - finalCorrelation.get(i, j));
      }
    }
    return returnMatrix;
  }
   
  public void dump(File output) throws IOException {
    MatrixIO.writeMatrix(finalCorrelation, output, MatrixIO.Format.MATLAB_SPARSE);
  }

  private class EntryComp implements Comparator<Map.Entry<String,Integer>> {
    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
      int diff = o2.getValue().intValue() - o1.getValue().intValue();
      return (diff != 0) ? diff : o2.getKey().compareTo(o1.getKey());
    }
  }
}
