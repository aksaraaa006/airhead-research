package edu.ucla.sspace.coals;

import edu.ucla.sspace.common.matrix.SparseMatrix;
import edu.ucla.sspace.common.matrix.ArrayMatrix;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.MatrixIO;
import edu.ucla.sspace.common.Normalize;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StringUtils;
import edu.ucla.sspace.common.SVD;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.PriorityQueue;
import java.util.Set;

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

  private HashMap<Index, Integer> correlation;
  private HashMap<String, Integer> wordToIndex;
  private HashMap<String, Integer> totalWordFreq;
  Matrix finalCorrelation;
  private boolean isFilling;
  private int maxWords;

  public Coals() {
    init(14000);
  }

  public Coals(int numWords) {
    init(numWords);
  }

  public void init(int numWords) {
    maxWords = numWords;
    correlation = new HashMap<Index, Integer>();
    wordToIndex = new HashMap<String, Integer>();
    totalWordFreq = new HashMap<String, Integer>();
    isFilling = true;
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

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    ArrayList<String> wordWindow = new ArrayList<String>();
    HashMap<String, Integer> wordFreq = new HashMap<String, Integer>();
    HashMap<Index, Integer> documentCorrels = new HashMap<Index, Integer>();
    for (String line = null; (line = document.readLine()) != null;) {
      String[] text = line.split("\\s");
      for (String word : text) {
        wordWindow.add(word);
        int updatedFreq = 1;
        if (wordFreq.containsKey(word))
          updatedFreq = wordFreq.get(word).intValue() + 1;
        wordFreq.put(word, updatedFreq);
        update(documentCorrels, wordWindow);
      }
      finishUpdates(documentCorrels, wordWindow);
    }
    for (Map.Entry<Index, Integer> entry : documentCorrels.entrySet()) {
      synchronized (entry.getKey()) {
        int newValue = 0;
        if (correlation.containsKey(entry.getKey())) {
          newValue = correlation.get(entry.getKey()).intValue() +
                     entry.getValue().intValue();
        } else {
          newValue = entry.getValue().intValue();
        }
        correlation.put(entry.getKey(), newValue);
      }
    }
    for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
      synchronized (entry.getKey()) {
        int newValue = 0;
        if (totalWordFreq.containsKey(entry.getKey())) {
          newValue = totalWordFreq.get(entry.getKey()).intValue() +
                     entry.getValue().intValue();
        } else {
          newValue = entry.getValue().intValue();
        }
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
                             ArrayList<String> wordWindow) {
    if (isFilling) {
      int size = wordWindow.size();
      for (int i = 0; i < size; ++i) {
        String mainWord = wordWindow.get(i);
        for (int j = 0; j < i; ++j)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j)), j-i+5);
        for (int j = i+1; j < i+5 && j < size; j++)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j)), i+5-j);
      }
      return;
    }

    int size = wordWindow.size();
    for (int i = 0; i < 4; ++i) {
      String mainWord = wordWindow.get(4);
      for (int j = 0; j < 4; j++)
        addIfMissing(map, new Index(mainWord, wordWindow.get(j)), j+1);
      for (int j = 5; j < wordWindow.size(); j++)
        addIfMissing(map, new Index(mainWord, wordWindow.get(j)), 9-j);
      wordWindow.remove(0);
    }
  }

  private void update(HashMap<Index, Integer> map,
                      ArrayList<String> wordWindow) {
    int size = wordWindow.size();
    if (size < 9 && isFilling)
      return;
    else if (size >= 9 && isFilling) {
      for (int i = 0; i < 4; i++) {
        String mainWord = wordWindow.get(i);
        for (int j = 0; j < i; j++)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j)), j-i+5);
        for (int j = i+1; j < i+5; j++)
          addIfMissing(map, new Index(mainWord, wordWindow.get(j)), i+5-j);
      }
      isFilling = false;
    }

    String mainWord = wordWindow.get(4);
    for (int i = 0; i < 4; i++)
      addIfMissing(map, new Index(mainWord, wordWindow.get(i)), i+1);
    for (int i = 5; i < size; i++)
      addIfMissing(map, new Index(mainWord, wordWindow.get(i)), 9-i);
    wordWindow.remove(0);
  }

  /**
   * {@inheritDoc}
   */
  public void processSpace(Properties properties) {
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
        MatrixIO.writeMatrix(finalCorrelation, coalsMatrixFile);
        String dims = properties.getProperty(REDUCE_MATRIX_DIMENSION_PROPERTY);
        int dimensions = 300;
        if (dims != null)
          dimensions = Integer.parseInt(dims);
        Matrix[] usv = SVD.svd(coalsMatrixFile, 300);
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

    Matrix correl = new SparseMatrix(wordCount, wordCount);
    for (Map.Entry<Index, Integer> entry : correlation.entrySet()) {
      Index key = entry.getKey();
      double value = entry.getValue().intValue();
      if (!wordToIndex.containsKey(key.word) ||
          !wordToIndex.containsKey(key.document))
        continue;
      int index1 = wordToIndex.get(key.word).intValue();
      int index2 = wordToIndex.get(key.document).intValue();
      correl.set(index1, index2, value);
    }
    totalWordFreq.clear();
    correlation.clear();
    return correl;
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
    MatrixIO.writeMatrix(finalCorrelation, output);
  }

  private class EntryComp implements Comparator<Map.Entry<String,Integer>> {
    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
      int diff = o2.getValue().intValue() - o1.getValue().intValue();
      return diff;
    }
  }
}