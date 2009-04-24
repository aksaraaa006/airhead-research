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
import java.util.Properties;
import java.util.Set;

/**
 * An implementation of the Beagle Semantic Space model.  This implementation is
 * based on
 * <p style="font-family:Garamond, Georgia, serif">Jones, M. N., Mewhort, D.
 * J.L. (2007).  Representing Word Meaning and Order Information in a Composite
 * Holographic Lexicon.  <i>Psychological Review</i> <b>114</b>, 1-37.
 * Available <a href="www.indiana.edu/~clcl/BEAGLE/Jones_Mewhort_PR.pdf">here</a></p>
 *
 * For every word, a unique random index vector is created, where the vector has
 * some large dimension (by default 512), with each entry in the vector being
 * from a random gaussian distribution.  The holographic meaning of a word is
 * updated by first adding the sum of index vectors for all the words in a sliding
 * window centered around the target term.  Additionally a sum of convolutions
 * of several n-grams is added to the holographic meaning.  The main
 * functionality of this class can be found in the {@link RandomIndexBuilder}
 * class.
 *
 * Currently this class is not thread safe and does not accept Properties.
 */
public class Holograph implements SemanticSpace {
  public static final int CONTEXT_SIZE = 6;
  public static final int LINES_TO_SKIP = 40;
  public static final int MAX_LINES = 500;

  private final RandomIndexBuilder indexBuilder;
  private final LinkedList<String> words;
  private final HashMap<String, double[]> termHolographs;
  private final int indexVectorSize;

  public Holograph() {
    indexVectorSize = 512;
    indexBuilder = new RandomIndexBuilder();
    termHolographs = new HashMap<String, double[]>();
    words = new LinkedList<String>();
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return termHolographs.keySet();
  }

  /**
   * {@inheritDoc}
   */
  public double[] getVectorFor(String term) {
    return termHolographs.get(term);
  }

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    for (String line = null; (line = document.readLine()) != null;) {
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
        updateHolograph();
      }
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public void processSpace(Properties properties) {
  }

  private void updateHolograph() {
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
