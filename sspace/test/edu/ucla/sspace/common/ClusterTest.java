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

package edu.ucla.sspace.common;

import org.junit.*;

import static org.junit.Assert.*;

import java.util.ArrayList;

public class ClusterTest {
  @Test public void kMeansTest() {
    KMeansClustering cluster = new KMeansClustering();

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
    cluster.clusterK(testVectors, 10, 2);
    int[] results = cluster.getAssignments();
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
