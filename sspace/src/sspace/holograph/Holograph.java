package sspace.holograph;

import sspace.common.Index;
import sspace.common.Similarity;
import sspace.common.SemanticSpace;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;

public class Holograph implements SemanticSpace {
  public static final int CONTEXT_SIZE = 6;
  public static final int LINES_TO_SKIP = 40;
  public static final int MAX_LINES = 500;

  private final RandomIndexBuilder indexBuilder;
  private final LinkedList<String> words;
  private final HashMap<Index, double[]> termDocHolographs;
  private final HashMap<String, double[]> termHolographs;
  private final int indexVectorSize;
  private int docCount;

  public Holograph() {
    indexVectorSize = 2048;
    indexBuilder = new RandomIndexBuilder();
    termDocHolographs = new HashMap<Index, double[]>();
    termHolographs = new HashMap<String, double[]>();
    words = new LinkedList<String>();
    docCount = 0;
  }

  public void parseDocument(String line) throws IOException {
    docCount++;
    // split the line based on whitespace
    String[] text = line.split("\\s");
    for (String word : text) {
    // clean up each word before entering it into the matrix
      String cleaned = cleanup(word);
      // skip any mispelled or unknown words
      if (!isValid(cleaned))
        continue;
      words.add(cleaned);
      indexBuilder.addTermIfMissing(cleaned);
      updateHolograph("" + docCount);
    }
  }
  
  public void computeDistances(String filename, int similarCount) {
  }

  public void processSpace() {
  }

  public void reduce() {
  }

  private void updateHolograph(String filename) {
    if (words.size() < CONTEXT_SIZE) {
      return;
    }
    String[] context = words.toArray(new String[0]);
    String mainWord = context[1];
    context[1] = "";
    //Index index = new Index(mainWord, filename);
    double[] meaning = termHolographs.get(mainWord);
    //termDocHolographs.get(index);
    if (meaning == null) {
      meaning = new double[indexVectorSize];
      //termDocHolographs.put(index, meaning);
      //termHolographs.put(mainWord, new double[](meaning.getArray()));
      termHolographs.put(mainWord, meaning);
    }
    indexBuilder.updateMeaningWithTerm(meaning, context);
    //indexBuilder.updateMeaningWithTerm(termHolographs.get(mainWord), context);
    words.removeFirst();
  }

  public double computeDistance(String left, String right) {
    if (!termHolographs.containsKey(left) || !termHolographs.containsKey(right))
      return 0.0;
    return Similarity.cosineSimilarity(termHolographs.get(left),
                                       termHolographs.get(right));
  }

  private boolean isValid(String word) {
    return true;
  }

  private static String cleanup(String word) {
    // remove all non-letter characters
    word = word.replaceAll("\\W", "");
    // make the string lower case
    return word.toLowerCase();
  }
}
