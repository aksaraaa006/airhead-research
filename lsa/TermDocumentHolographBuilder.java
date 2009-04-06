import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;

import Jama.Matrix;

class TermDocumentHolographBuilder {
  public static final int CONTEXT_SIZE = 6;
  public static final int LINES_TO_SKIP = 40;
  public static final int MAX_LINES = 500;

  private final RandomIndexBuilder indexBuilder;
  private final LinkedList<String> words;
  private final HashMap<Index, Matrix> termDocHolographs;
  private final HashMap<String, Matrix> termHolographs;
  private final int indexVectorSize;
  private int docCount;

  public TermDocumentHolographBuilder() {
    indexBuilder = new RandomIndexBuilder();
    termDocHolographs = new HashMap<Index, Matrix>();
    termHolographs = new HashMap<String, Matrix>();
    words = new LinkedList<String>();
    docCount = 0;
    indexVectorSize = 2048;
  }

  public void parseDocument(String filename) throws IOException {
    docCount++;
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line = null;
    int lineNum = 0;
    while ((line = br.readLine()) != null) {
      if (lineNum++ < LINES_TO_SKIP)
        continue;

      if (lineNum > MAX_LINES)
        break;

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
      updateHolograph(filename);
      }
    }
    br.close();
  }

  private void updateHolograph(String filename) {
    if (words.size() < CONTEXT_SIZE) {
      return;
    }
    String[] context = words.toArray(new String[0]);
    String mainWord = context[1];
    context[1] = "";
    Index index = new Index(mainWord, filename);
    Matrix meaning = termDocHolographs.get(index);
    if (meaning == null) {
      meaning = new Matrix(indexVectorSize, 1, 0);
      termDocHolographs.put(index, meaning);
      termHolographs.put(mainWord, new Matrix(meaning.getArray()));
    }
    indexBuilder.updateMeaningWithTerm(meaning, context);
    indexBuilder.updateMeaningWithTerm(termHolographs.get(mainWord), context);
    words.removeFirst();
  }

  private double cosineSimilarity(Matrix a, Matrix b) {
    double dotProduct = 0.0;
    double aMagnitude = 0.0;
    double bMagnitude = 0.0;
    for (int i = 0; i < indexVectorSize; i++) {
      double aValue = a.get(i,0);
      double bValue = b.get(i,0);
      aMagnitude += aValue * aValue;
      bMagnitude += bValue * bValue;
      dotProduct += aValue * bValue;
    }
    aMagnitude = Math.sqrt(aMagnitude);
    bMagnitude = Math.sqrt(bMagnitude);
    return dotProduct / (aMagnitude * bMagnitude);
  }

  public double computeSimilarity(String left, String right) {
    if (!termHolographs.containsKey(left) || !termHolographs.containsKey(right))
      return 0.0;
    return cosineSimilarity(termHolographs.get(left), termHolographs.get(right));
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
