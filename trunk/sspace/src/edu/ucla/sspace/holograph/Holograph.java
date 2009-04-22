package edu.ucla.sspace.holograph;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Holograph {
  public static final int CONTEXT_SIZE = 6;
  public static final int LINES_TO_SKIP = 40;
  public static final int MAX_LINES = 500;

  private final RandomIndexBuilder indexBuilder;
  private final LinkedList<String> words;
  private final HashMap<String, double[]> termHolographs;
  private final int indexVectorSize;
  private int docCount;

  public Holograph() {
    indexVectorSize = 2048;
    indexBuilder = new RandomIndexBuilder();
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
      String cleaned = StringUtils.cleanup(word);
      // skip any mispelled or unknown words
      if (!StringUtils.isValid(cleaned))
        continue;
      words.add(cleaned);
      indexBuilder.addTermIfMissing(cleaned);
      updateHolograph("" + docCount);
    }
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
    double[] meaning = termHolographs.get(mainWord);
    if (meaning == null) {
      meaning = new double[indexVectorSize];
      termHolographs.put(mainWord, meaning);
    }
    indexBuilder.updateMeaningWithTerm(meaning, context);
    words.removeFirst();
  }

  public double computeSimilarity(String left, String right) {
    if (!termHolographs.containsKey(left) || !termHolographs.containsKey(right))
      return 0.0;
    return Similarity.cosineSimilarity(termHolographs.get(left),
                                       termHolographs.get(right));
  }

  public void lutherTest() {
    double[] lutherMeaning = termHolographs.get("luther");
    double[] right = indexBuilder.decode(lutherMeaning, true);
    double[] left = indexBuilder.decode(lutherMeaning, false);
    double maxLeft = 0;
    double maxright = 0;
    String leftWord = "";
    String rightWord = "";
    for (Map.Entry<String, double[]> entry : termHolographs.entrySet()) {
      double leftSim = Similarity.cosineSimilarity(entry.getValue(), left);
      if (leftSim > maxLeft) {
        maxLeft = leftSim;
        leftWord = entry.getKey();
      }
      double rightSim = Similarity.cosineSimilarity(entry.getValue(), right);
      if (rightSim > maxLeft) {
        maxLeft = rightSim;
        rightWord = entry.getKey();
      }
    }
    System.out.println("for luther we get for left: " + leftWord +
                       ", and right: " + rightWord);
  }
}
