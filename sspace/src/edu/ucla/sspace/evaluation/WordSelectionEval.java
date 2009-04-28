/*
 * Copyright 2009 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Vector;

public class WordSelectionEval {
  public static double evalSemanticSpace(SemanticSpace space,
                                         String filename,
                                         Method simMethod) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String pair = null;
      int questionCount = 0;
      int correctAnswers = 0;
      while ((pair = br.readLine()) != null) {
        if (pair.startsWith("#"))
          continue;
        String[] text = pair.split(" ");
        if (text.length != 9)
          continue;
        questionCount++;
        double maxSimilarity = 0;
        int bestIndex = 1;
        double[] questionVector = space.getVectorFor(text[0]);
        for (int i = 2; i < text.length; i+=2) {
          double[] answerVector = space.getVectorFor(text[i]);
          Double wordSim = (Double) simMethod.invoke(
              null, new Object[] {questionVector, answerVector});
          if (maxSimilarity < wordSim.doubleValue()) {
            maxSimilarity = wordSim.doubleValue();
            bestIndex = i;
          }
        }
        if (bestIndex == 2)
          correctAnswers++;
      }
      if (questionCount == 0)
        return 0;

      return correctAnswers / (double) questionCount * 100;
    } catch (IOException e) {
    } catch (IllegalAccessException iae) {
    } catch (InvocationTargetException iae) {
    }
    return 0;
  }
}
