package edu.ucla.sspace.coals;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.Normalize;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;

import Jama.Matrix;

public class Coals {
  private HashMap<Index, Double> correlation;
  private HashMap<String, Integer> wordToIndex;
  private HashMap<String, Integer> wordFreq;
  Matrix finalCorrelation;
  private ArrayList<String> wordWindow;
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
    correlation = new HashMap<Index, Double>();
    wordToIndex = new HashMap<String, Integer>();
    wordFreq = new HashMap<String, Integer>();
    wordWindow = new ArrayList<String>();
    isFilling = true;
    finalCorrelation = null;
  }

  public void parseDocument(String document) {
    String[] text = document.split("\\s");
    for (String word : text) {
      //String cleaned = StringUtils.cleanup(word);
      String cleaned = word.toLowerCase();
      wordWindow.add(cleaned);
      int updatedFreq = 1;
      if (wordFreq.containsKey(cleaned))
        updatedFreq = wordFreq.get(cleaned).intValue() + 1;
      wordFreq.put(cleaned, updatedFreq);
      update();
      System.out.println(word + " : " + wordWindow.size());
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
    if (isFilling) {
      int size = wordWindow.size();
      for (int i = 0; i < size; ++i) {
        String mainWord = wordWindow.get(i);
        for (int j = 0; j < i; ++j)
          addIfMissing(new Index(mainWord, wordWindow.get(j)), j-i+5);
        for (int j = i+1; j < i+5 && j < size; j++)
          addIfMissing(new Index(mainWord, wordWindow.get(j)), i+5-j);
      }
      return;
    }

    int size = wordWindow.size();
    System.out.println(size);
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
    int wordCount = finalCorrelation.getRowDimension();
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
  }

  private Matrix buildMatrix() {
    PriorityQueue<Map.Entry<String, Integer>> wordFreqFilter =
      new PriorityQueue<Map.Entry<String, Integer>>(maxWords, new EntryComp());
    wordFreqFilter.addAll(wordFreq.entrySet());

    {
      Iterator<Map.Entry<String, Integer>> iter = wordFreqFilter.iterator();
      while (iter.hasNext()) {
        Map.Entry<String, Integer> e = iter.next();
        System.out.println(e.getKey() + " : " + e.getValue());
      }
    }

    int wordCount =
      (wordFreqFilter.size() > maxWords) ? maxWords : wordFreqFilter.size();
    String[] keys = new String[wordCount];
    for (int i = 0; i < wordCount && wordFreqFilter.size() > 0; ++i)
      keys[i] = wordFreqFilter.poll().getKey();

    Arrays.sort(keys);
    for (int i = 0; i < wordCount; ++i)
      wordToIndex.put(keys[i], i);

    Matrix correl = new Matrix(wordCount, wordCount, 0);
    for (Map.Entry<Index, Double> entry : correlation.entrySet()) {
      Index key = entry.getKey();
      double value = entry.getValue().doubleValue();
      if (!wordToIndex.containsKey(key.word) ||
          !wordToIndex.containsKey(key.document))
        continue;
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
    finalCorrelation.print(6, 4);
    for (Map.Entry<String, Integer> entry : wordToIndex.entrySet())
      System.out.println(entry.getKey() + " has index: " + entry.getValue());
  }

  private class EntryComp implements Comparator<Map.Entry<String,Integer>> {
    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
      int diff = o2.getValue().intValue() - o1.getValue().intValue();
      System.out.println(o1.getKey() + " compare to: " + o2.getKey() + " has: " + diff);
      return diff;
    }
  }
}
