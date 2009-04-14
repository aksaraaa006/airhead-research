package edu.ucla.sspace.coals;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.Normalize;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import Jama.Matrix;

public class Coals implements SemanticSpace {
  private HashMap<Index, Double> correlation;
  private HashMap<String, Integer> wordToIndex;
  Matrix finalCorrelation;
  private ArrayList<String> wordWindow;
  private int wordCount;

  public Coals() {
    correlation = new HashMap<Index, Double>();
    wordToIndex = new HashMap<String, Integer>();
    wordWindow = new ArrayList<String>();
    wordCount = 0;
  }

  public void parseDocument(String document) {
    String[] text = document.split("\\s");
    for (String word : text) {
      String cleaned = StringUtils.cleanup(word);
      if (!wordToIndex.containsKey(cleaned)) {
        wordToIndex.put(cleaned, wordCount);
        wordCount++;
      }
      wordWindow.add(cleaned);
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
    if (wordWindow.size() < 9)
      return;
    String mainWord = wordWindow.get(4);
    for (int i = 0; i < 4; i++)
      addIfMissing(new Index(mainWord, wordWindow.get(i)), i+1);
    for (int i = 5; i < 9; i++)
      addIfMissing(new Index(mainWord, wordWindow.get(i)), 9-i);
  }

  public void reduce() {
  }

  public double computeSimilarity(String word1, String word2) {
    return 0.0;
  }

  public void processSpace() {
    Matrix correl = new Matrix(wordCount, wordCount, 0);
    for (Map.Entry<Index, Double> entry : correlation.entrySet()) {
      Index key = entry.getKey();
      double value = entry.getValue().doubleValue();
      correl.set(wordToIndex.get(key.word).intValue(),
                 wordToIndex.get(key.document).intValue(),
                 value);
    }
    Normalize.byCorrelation(correl);
    for (int i = 0; i < wordCount; ++i) {
      for (int j = 0; j < wordCount; ++j) {
        if (correl.get(i,j) < 0)
          correl.set(i,j, 0);
        else
          correl.set(i, j, Math.pow(correl.get(i, j), 2));
      }
    }
    finalCorrelation = correl;
  }

  public void printCorrelations() {
    finalCorrelation.print(8, 2);
    for (Map.Entry<String, Integer> entry : wordToIndex.entrySet())
      System.out.println(entry.getKey() + " has index: " + entry.getValue());
  }
}
