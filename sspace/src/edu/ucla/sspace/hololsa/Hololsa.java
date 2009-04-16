package edu.ucla.sspace.hololsa;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.StringUtils;

import edu.ucla.sspace.holograph.RandomIndexBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Hololsa implements SemanticSpace {
  public static final int CONTEXT_SIZE = 6;
  public static final int LINES_TO_SKIP = 40;
  public static final int MAX_LINES = 500;

  private final RandomIndexBuilder indexBuilder;
  private final LinkedList<String> words;
  private HashMap<String, double[]> termDocHolographs;
  private final HashMap<String, BufferedWriter> termFileWriters;
  private final int indexVectorSize;
  private int docCount;

  public Hololsa() {
    indexVectorSize = 2048;
    indexBuilder = new RandomIndexBuilder();
    termFileWriters = new HashMap<String, BufferedWriter>();
    words = new LinkedList<String>();
    docCount = 0;
  }

  public void parseDocument(String line) throws IOException {
    docCount++;
    // split the line based on whitespace
    termDocHolographs = new HashMap<String, double[]>();
    String[] text = line.split("\\s");
    for (String word : text) {
      // clean up each word before entering it into the matrix
      String cleaned = StringUtils.cleanup(word);
      // skip any mispelled or unknown words
      if (!StringUtils.isValid(cleaned))
        continue;
      if (!termFileWriters.containsKey(cleaned))
        termFileWriters.put(cleaned,
                            new BufferedWriter(new FileWriter(cleaned+".txt")));
      words.add(cleaned);
      indexBuilder.addTermIfMissing(cleaned);
      updateHolograph();
    }
    dumpMeaningToFile(""+docCount);
  }
  
  private void dumpMeaningToFile(String document) {
    try {
      for (Map.Entry<String, double[]> entry : termDocHolographs.entrySet()) {
        BufferedWriter writer = termFileWriters.get(entry.getKey());
        writer.write(document + " : ");
        double[] value = entry.getValue();
        for (int i = 0; i < indexVectorSize; ++i)
          writer.write(value[i] + " ");
        writer.write("\n");
      }
    } catch (IOException e) {
    }
  }

  public void computeDistances(String filename, int similarCount) {
  }

  public void processSpace() {
  }

  public void reduce() {
  }

  private void updateHolograph() {
    if (words.size() < CONTEXT_SIZE) {
      return;
    }
    String[] context = words.toArray(new String[0]);
    String mainWord = context[1];
    context[1] = "";
    double[] meaning = termDocHolographs.get(mainWord);
    if (meaning == null) {
      meaning = new double[indexVectorSize];
      termDocHolographs.put(mainWord, meaning);
    }
    indexBuilder.updateMeaningWithTerm(meaning, context);
    words.removeFirst();
  }

  public double computeSimilarity(String left, String right) {
    if (!termDocHolographs.containsKey(left) ||
        !termDocHolographs.containsKey(right))
      return 0.0;
    return Similarity.cosineSimilarity(termDocHolographs.get(left),
                                       termDocHolographs.get(right));
  }
}
