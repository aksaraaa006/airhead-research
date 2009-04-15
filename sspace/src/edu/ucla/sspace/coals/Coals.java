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
  }

  public void parseDocument(String document) {
    String[] text = document.split("\\s");
    for (String word : text) {
      //String cleaned = StringUtils.cleanup(word);
      wordWindow.add(word.toLowerCase());
      update();
    }
  }

  private void addIfMissing(Index i, double value) {
    if (!correlation.containsKey(i))
      correlation.put(i, value);
    else
      correlation.put(i, correlation.get(i).doubleValue() + value);
  }

  private void update() {
    int size = wordWindow.size();
    if (size < 9 && isFilling && size < 5) {
      String mainWord = wordWindow.get(size - 1);
      for (int i = 0; i < size - 2; i++)
        addIfMissing(new Index(mainWord, wordWindow.get(i)), i+1);
      return;
    } else if (size < 9 && !isFilling) {
      if (size <= 5) {
        String mainWord = wordWindow.get(0);
        for (int i = 1; i < size; i++)
          addIfMissing(new Index(mainWord, wordWindow.get(i)), 5-i);
      } else {
        String mainWord = wordWindow.get(size - 5);
        for (int i = 0; i < size-5; i++)
          addIfMissing(new Index(mainWord, wordWindow.get(i)), i - (size - 5) + 1);
        for (int i = 5; i < size; i++)
          addIfMissing(new Index(mainWord, wordWindow.get(i)), 9-i);
      }
      return;
    } else
      isFilling = false;

    String mainWord = wordWindow.get(4);
    for (int i = 0; i < 4; i++)
      addIfMissing(new Index(mainWord, wordWindow.get(i)), i+1);
    for (int i = 5; i < size; i++)
      addIfMissing(new Index(mainWord, wordWindow.get(i)), 9-i);
    if (size >= 9) 
      wordWindow.remove(0);
  }

  public void reduce() {
  }

  public double computeSimilarity(String word1, String word2) {
    return 0.0;
  }

  public void processSpace() {
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
    //Normalize.byCorrelation(correl);
    /*
    for (int i = 0; i < wordCount; ++i) {
      for (int j = 0; j < wordCount; ++j) {
        if (correl.get(i,j) < 0)
          correl.set(i,j, 0);
        else
          correl.set(i, j, Math.pow(correl.get(i, j), 2));
      }
    }
    */
    finalCorrelation = correl;
  }

  public void printCorrelations() {
    finalCorrelation.print(5, 2);
    for (Map.Entry<String, Integer> entry : wordToIndex.entrySet())
      System.out.println(entry.getKey() + " has index: " + entry.getValue());
  }
}
