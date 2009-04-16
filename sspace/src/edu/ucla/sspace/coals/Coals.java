package edu.ucla.sspace.coals;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.Normalize;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import Jama.Matrix;

public class Coals implements SemanticSpace {
  private HashMap<Index, Double> correlation;
  private HashMap<String, Integer> wordToIndex;
  Matrix finalCorrelation;
  private ArrayList<String> wordWindow;
  private boolean isFilling;

  public Coals() {
    correlation = new HashMap<Index, Double>();
    wordToIndex = new HashMap<String, Integer>();
    wordWindow = new ArrayList<String>();
    isFilling = true;
    finalCorrelation = null;
  }

  public void parseDocument(String document) {
    String[] text = document.split("\\s");
    for (String word : text) {
      //String cleaned = StringUtils.cleanup(word);
      wordWindow.add(word.toLowerCase());
      update();
    }
    finishUpdates();
  }

  private void addIfMissing(Index i, double value) {
    if (!correlation.containsKey(i))
      correlation.put(i, value);
    else
      correlation.put(i, correlation.get(i).doubleValue() + value);
  }

  private void finishUpdates() {
    int size = wordWindow.size();
    for (int i = 0; i < 4; ++i) {
      String mainWord = wordWindow.get(4);
      for (int j = 0; j < 4; j++)
        addIfMissing(new Index(mainWord, wordWindow.get(j)), j+1);
      for (int j = 5; j < wordWindow.size(); j++)
        addIfMissing(new Index(mainWord, wordWindow.get(j)), 9-j);
      wordWindow.remove(0);
    }
  }

  private void update() {
    int size = wordWindow.size();
    if (size < 9 && isFilling)
      return;
    else if (size >= 9 && isFilling) {
      for (int i = 0; i < 4; i++) {
        String mainWord = wordWindow.get(i);
        for (int j = 0; j < i; j++)
          addIfMissing(new Index(mainWord, wordWindow.get(j)), j-i+5);
        for (int j = i+1; j < i+5; j++)
          addIfMissing(new Index(mainWord, wordWindow.get(j)), i+5-j);
      }
      isFilling = false;
    }

    String mainWord = wordWindow.get(4);
    for (int i = 0; i < 4; i++)
      addIfMissing(new Index(mainWord, wordWindow.get(i)), i+1);
    for (int i = 5; i < size; i++)
      addIfMissing(new Index(mainWord, wordWindow.get(i)), 9-i);
    wordWindow.remove(0);
  }

  public void reduce() {
  }

  public double computeSimilarity(String word1, String word2) {
    return 0.0;
  }

  public void processSpace() {
    finalCorrelation = buildMatrix();
    //Normalize.byCorrelation(finalCorrelation);
    /*
    for (int i = 0; i < wordCount; ++i) {
      for (int j = 0; j < wordCount; ++j) {
        if (correl.get(i,j) < 0)
          correl.set(i,j, 0);
        else
          correl.set(i, j, Math.sqrt(correl.get(i, j));
      }
    }
    */
  }

  private Matrix buildMatrix() {
    HashSet<String> wordSet = new HashSet<String>();
    for (Index index : correlation.keySet()) {
      wordSet.add(index.word);
      wordSet.add(index.document);
    }
    String[] keys = wordSet.toArray(new String[0]);
    Arrays.sort(keys);
    for (int i = 0; i < keys.length; ++i)
      wordToIndex.put(keys[i], i);
    int wordCount = keys.length;

    Matrix correl = new Matrix(wordCount, wordCount, 0);
    for (Map.Entry<Index, Double> entry : correlation.entrySet()) {
      Index key = entry.getKey();
      double value = entry.getValue().doubleValue();
      int index1 = wordToIndex.get(key.word).intValue();
      int index2 = wordToIndex.get(key.document).intValue();
      correl.set(index1, index2, value);
    }
    return correl;
  }

  public Matrix compareToMatrix(Matrix o) {
    if (finalCorrelation == null)
      finalCorrelation = buildMatrix();
    return finalCorrelation.minus(o);
  }
   
  public void printCorrelations() {
    finalCorrelation.print(5, 2);
    for (Map.Entry<String, Integer> entry : wordToIndex.entrySet())
      System.out.println(entry.getKey() + " has index: " + entry.getValue());
  }
}
