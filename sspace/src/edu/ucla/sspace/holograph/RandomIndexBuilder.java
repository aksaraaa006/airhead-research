package edu.ucla.sspace.holograph;

import edu.ucla.sspace.common.IndexBuilder;

import edu.ucla.sspace.ri.IndexVector;
import edu.ucla.sspace.ri.IndexVectorGenerator;
import edu.ucla.sspace.ri.RandomIndexVectorGenerator;

import java.util.Map;
import java.util.Queue;
import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;

public class RandomIndexBuilder implements IndexBuilder {
  /**
   * The default number of dimensions to be used by the index and semantic
   * vectors.
   */
  public static final int DEFAULT_VECTOR_LENGTH = 2048;
  public static final int DEFAULT_WINDOW_SIZE = 5;

  private Map<String,IndexVector> wordToIndexVector;

  /**
   * A private source of randomization used for creating the index vectors.
   */
  // We use our own source rather than Math.random() to ensure reproduceable
  // behavior when a specific seed is set.
  //
  // NOTE: intentionally package-private to allow other RI-related classes to
  // based their randomness on a this class's seed.
  static final Random RANDOM = new Random();

  /**
   * The number of dimensions for the semantic and index vectors.
   */
  private int indexVectorSize;

  private int windowSize;

  private IndexVectorGenerator indexVectorGenerator;

  public RandomIndexBuilder() {
    init(DEFAULT_VECTOR_LENGTH, DEFAULT_WINDOW_SIZE);
  }

  public RandomIndexBuilder(int vectorSize) {
    init(vectorSize, DEFAULT_WINDOW_SIZE);
  }

  public RandomIndexBuilder(int vectorSize, int windowSize) {
    init(vectorSize, windowSize);
  }

  public void init(int vectorSize, int windowSize) {
	wordToIndexVector = new ConcurrentHashMap<String,IndexVector>();
    indexVectorSize = vectorSize;
    this.windowSize = windowSize;
    indexVectorGenerator = new RandomIndexVectorGenerator();
  }

  private IndexVector getIndexVector(String word) {
    IndexVector v = wordToIndexVector.get(word);
    if (v == null) {
	  // lock on th word in case multiple threads attempt to add it at
	  // once
	  synchronized(word) {
        // recheck in case another thread added it while we were waiting
        // for the lock
        v = wordToIndexVector.get(word);
        if (v == null) {
          v = indexVectorGenerator.create(indexVectorSize);
          wordToIndexVector.put(word, v);
        }
      }
    }
    return v;
  }

  public int expectedSizeOfPrevWords() {
    return windowSize;
  }

  public int expectedSizeOfNextWords() {
    return windowSize;
  }

  private void addToMeaning(double[] meaning, IndexVector vector) {
    for (int p : vector.positiveDimensions())
      meaning[p] += 1;
    for (int p : vector.negativeDimensions())
      meaning[p] -= 1;
  }

  public void updateMeaningWithTerm(double[] meaning,
                                    Queue<String> prevWords,
                                    Queue<String> nextWords) {
    for (String word : prevWords)
      addToMeaning(meaning, getIndexVector(word));
    for (String word : nextWords)
      addToMeaning(meaning, getIndexVector(word));
  }
}