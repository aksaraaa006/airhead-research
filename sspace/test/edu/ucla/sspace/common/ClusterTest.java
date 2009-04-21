package edu.ucla.sspace.common;

import org.junit.*;

import static org.junit.Assert.*;

import java.util.ArrayList;

public class ClusterTest {
  @Test public void kMeansTest() {
    ArrayList<double[]> testVectors = new ArrayList<double[]>();
    double[][] testValues = {
    {1, 2, 3, 4, 5, 0, 0, 0, 0, 0},
    {1, 2, 2, 5, 6, 0, 0, 0, 0, 0},
    {2, 3, 3, 2, 4, 0, 0, 0, 0, 0},
    {0, 0, 0, 0, 1, 2, 3, 4, 5, 6},
    {0, 0, 0, 0, 0, 2, 2, 4, 7, 6},
    {0, 0, 0, 0, 0, 3, 3, 4, 6, 5}};
    for (int i = 0; i < 6; ++i)
      testVectors.add(testValues[i]);
    int[] results = Cluster.kMeansCluster(testVectors, 2, 10);
    assertEquals(6, results.length);
    assertTrue((results[0] == 0 && results[1] == 0 && results[2] == 0) ||
               (results[0] == 1 && results[1] == 1 && results[2] == 1));
    assertTrue((results[3] == 0 && results[4] == 0 && results[5] == 0) ||
               (results[3] == 1 && results[4] == 1 && results[5] == 1));
    assertTrue(results[0] != results[3]);
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(ClusterTest.class);
  }
}
