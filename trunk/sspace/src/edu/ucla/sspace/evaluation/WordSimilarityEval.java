package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.Vector;

public class WordSimilarityEval {
  private WordSimilarityEval() {
  }

  public static double evalSemanticSpace(SemanticSpace space, String filename) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String pair = null;
      Vector<String[]> inputRatings = new Vector<String[]>();
      while ((pair = br.readLine()) != null) {
        String[] text = pair.split("\\s");
        inputRatings.add(text);
      }
      double[] goldScores = new double[inputRatings.size()];
      double[] evalScores = new double[inputRatings.size()];
      for (int i = 0; i < inputRatings.size(); ++i) {
        String[] text = inputRatings.get(i);
        goldScores[i] = Double.valueOf(text[2]).doubleValue();
        evalScores[i] = -1; // FIXME: space.computeSimilarity(text[0], text[1]);
      }
      double goldSum = 0;
      double evalSum = 0;
      double evalSumSquared = 0;
      double pairSum = 0;
      int size = goldScores.length;
      for (int i = 0; i < size; ++i) {
        double goldValue = goldScores[i];
        double evalValue = evalScores[i];
        goldSum += goldValue;
        evalSum += evalValue;
        evalSumSquared += (evalValue * evalValue);
        pairSum += (goldValue * evalValue);
      }
      double slope = (goldSum * evalSum - size * pairSum) / 
                     (Math.pow(evalSum, 2) - (size * evalSumSquared));
      for (int i = 0; i < size; ++i)
        evalScores[i] = Math.pow(evalScores[i], slope);
      return Similarity.correlation(goldScores, evalScores) * 100;
    } catch (IOException e) {
    }
    return 0;
  }
}
