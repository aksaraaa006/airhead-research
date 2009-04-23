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
}
