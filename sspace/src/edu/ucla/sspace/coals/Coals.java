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

import edu.ucla.sspace.common.matrix.ArrayMatrix;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.MatrixIO;
import edu.ucla.sspace.common.MatrixIO.Format;
import edu.ucla.sspace.common.Normalize;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StringUtils;
import edu.ucla.sspace.common.SVD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.PriorityQueue;
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

  public Coals() {
    init(14000);
  }

  public Coals(int numWords) {
    init(numWords);
  }

  public void init(int numWords) {
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
  public double[] getVectorFor(String term) {
    if (wordToIndex.containsKey(term))
      return finalCorrelation.getRow(wordToIndex.get(term).intValue());
    return null;
  }

  public String getSpaceName() {
    return COALS_SSPACE_NAME;
  }

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    ArrayList<String> wordWindow = new ArrayList<String>();
    HashMap<String, Integer> wordFreq = new HashMap<String, Integer>();
    HashMap<Index, Integer> documentCorrels = new HashMap<Index, Integer>();
    boolean isFilling = true;
    for (String line = null; (line = document.readLine()) != null;) {
      String[] text = line.split("\\s");
      for (String word : text) {
        word = word.intern();
        wordWindow.add(word);
        int updatedFreq = 1;
        if (wordFreq.containsKey(word))
          updatedFreq = wordFreq.get(word).intValue() + 1;
        wordFreq.put(word, updatedFreq);
        isFilling = update(documentCorrels, wordWindow, isFilling);
      }
      finishUpdates(documentCorrels, wordWindow, isFilling);
    }
    synchronized (rawOccuranceWriter) {
      for (Map.Entry<Index, Integer> entry : documentCorrels.entrySet()) {
        StringBuffer sb = new StringBuffer(32);
        sb.append(entry.getKey().word).append("|")
          .append(entry.getKey().document).append("|").append(entry.getValue());
        rawOccuranceWriter.println(sb.toString());
      }
    }
    for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
      synchronized (entry.getKey()) {
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

  private void finishUpdates(HashMap<Index, Integer> map,
                             ArrayList<String> wordWindow,
                             boolean isFilling) {
    if (isFilling) {
      int size = wordWindow.size();
      for (int i = 0; i < size; ++i) {
        String mainWord = wordWindow.get(i).intern();
        for (int j = 0; j < i; ++j)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j).intern()), j-i+5);
        for (int j = i+1; j < i+5 && j < size; j++)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j).intern()), i+5-j);
      }
      return;
    }

    int size = wordWindow.size();
    for (int i = 0; i < 4; ++i) {
      String mainWord = wordWindow.get(4).intern();
      for (int j = 0; j < 4; j++)
        addIfMissing(map, new Index(mainWord, wordWindow.get(j).intern()), j+1);
      for (int j = 5; j < wordWindow.size(); j++)
        addIfMissing(map, new Index(mainWord, wordWindow.get(j).intern()), 9-j);
      wordWindow.remove(0);
    }
  }

  private boolean update(HashMap<Index, Integer> map,
                         ArrayList<String> wordWindow,
                         boolean isFilling) {
    int size = wordWindow.size();
    if (size < 9 && isFilling)
      return true;
    else if (size >= 9 && isFilling) {
      for (int i = 0; i < 4; i++) {
        String mainWord = wordWindow.get(i).intern();
        for (int j = 0; j < i; j++)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j).intern()), j-i+5);
        for (int j = i+1; j < i+5; j++)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j).intern()), i+5-j);
      }
    }

    String mainWord = wordWindow.get(4).intern();
    for (int i = 0; i < 4; i++)
      addIfMissing(map, new Index(mainWord, wordWindow.get(i).intern()), i+1);
    for (int i = 5; i < size; i++)
      addIfMissing(map, new Index(mainWord, wordWindow.get(i).intern()), 9-i);
    wordWindow.remove(0);
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void processSpace(Properties properties) {
    if (finalCorrelation == null)
      finalCorrelation = buildMatrix();
    int wordCount = finalCorrelation.rows();
    Normalize.byCorrelation(finalCorrelation);
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
    String reduceMatrix = properties.getProperty(REDUCE_MATRIX_PROPERTY);
    if (reduceMatrix != null) {
      try {
        File coalsMatrixFile =
          File.createTempFile("coals-term-doc-matrix", "txt");
        MatrixIO.writeMatrix(finalCorrelation,
                             coalsMatrixFile,
                             Format.SVDLIBC_DENSE_BINARY);
        String dims = properties.getProperty(REDUCE_MATRIX_DIMENSION_PROPERTY);
        int dimensions = (300 > wordCount) ? wordCount : 300;
        if (dims != null)
          dimensions = Integer.parseInt(dims);
        Matrix[] usv = SVD.svd(coalsMatrixFile, SVD.Algorithm.ANY, Format.SVDLIBC_DENSE_BINARY, dimensions);
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
    PriorityQueue<Map.Entry<String, Integer>> wordFreqFilter =
      new PriorityQueue<Map.Entry<String, Integer>>(maxWords, new EntryComp());
    wordFreqFilter.addAll(totalWordFreq.entrySet());

    int wordCount =
      (wordFreqFilter.size() > maxWords) ? maxWords : wordFreqFilter.size();
    String[] keys = new String[wordCount];
    for (int i = 0; i < wordCount && wordFreqFilter.size() > 0; ++i)
      keys[i] = wordFreqFilter.poll().getKey();

    Arrays.sort(keys);
    for (int i = 0; i < wordCount; ++i)
      wordToIndex.put(keys[i], i);

    totalWordFreq.clear();
    wordFreqFilter.clear();
    synchronized (rawOccuranceWriter) {
      rawOccuranceWriter.close();
    }
    
    try {
      BufferedReader br = new BufferedReader(new FileReader(rawOccurances));
      String line = null;
      Matrix correl = new ArrayMatrix(wordCount, wordCount);
      while ((line = br.readLine()) != null) {
        String[] splitLine = line.split("\\|");
        Integer r = wordToIndex.get(splitLine[0]);
        Integer c = wordToIndex.get(splitLine[1]);
        if (r == null || c == null)
          continue;
        double value = Double.parseDouble(splitLine[2]);
        correl.set(r.intValue(), c.intValue(),
                   value + correl.get(r.intValue(), c.intValue()));
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
      return diff;
    }
  }
}
