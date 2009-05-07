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

import java.io.BufferedReader;
import java.io.IOException;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

public class TestSemanticSpace implements SemanticSpace {
  private ArrayList<double[]> testVectors;
  private int timesCalled;

  public TestSemanticSpace() {
    testVectors = new ArrayList<double[]>();
    timesCalled = 0;
  }

  public void addVector(double[] a) {
    testVectors.add(a);
  }

  public void processDocument(BufferedReader reader) throws IOException {
  }

  public Set<String> getWords() {
    return null;
  }

  public double[] getVectorFor(String word) {
    double[] ret = testVectors.get(timesCalled);
    timesCalled = (timesCalled + 1) % testVectors.size();
    return ret;
  }

  public void processSpace(Properties prop) {
  }

  Method getSimilarityMethod() {
    try {
      Class<?> clazz = Class.forName("edu.ucla.sspace.common.Similarity");
      return clazz.getMethod("cosineSimilarity", double[].class, double[].class);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }
    return null;
  }

  public String getSpaceName() {
    return "";
  }
}
