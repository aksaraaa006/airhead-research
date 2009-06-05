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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** A collection of basic clustering techniques used in Semantic Space models.
 *  Each clustering method can take in some number of data points, and then
 *  return an array detailing how those data points were seperated into various
 *  categories.
 */
public class Cluster {
  /** Simple implementation of the k-means clustering algorithm.
   * @param dataPoints An Array List of vectors which need to be clustered.
   * @param k          The number of desired clusters.
   * @param indexVectorSize The size of each vector in dataPoints.
   * @return An array of the same size as dataPoints, where each index indicates
   * which cluster number the i'th dataPoint belongs to.
   */
  public static double[][] kMeansCluster(List<double[]> dataPoints,
                                         int k,
                                         int indexVectorSize) {
    double[][] kCenters = new double[k][indexVectorSize];
    HashSet<Integer> randomCenters = new HashSet<Integer>(k);
    Random numGenerator = new Random();
    while (randomCenters.size() != k) {
      randomCenters.add(numGenerator.nextInt(dataPoints.size()));
    }
    int k_index = 0;
    for (Integer index : randomCenters) {
      kCenters[k_index] = dataPoints.get(index.intValue()); k_index++;
    }

    boolean converged = false;
    List<List<Integer>> clusters = null;
    while (!converged) {
      double[][] distances = new double[dataPoints.size()][k];
      for (int i = 0; i < dataPoints.size(); ++i) {
        for (int j = 0; j < k; ++j) {
          distances[i][j] = Similarity.euclideanDistance(kCenters[j],
                                                         dataPoints.get(i));
        }
      }
      clusters = new ArrayList<List<Integer>>();
      for (int i = 0; i < k; ++i)
        clusters.add(new ArrayList<Integer>());

      for (int i = 0; i < dataPoints.size(); ++i) {
        double minDist = distances[i][0];
        int minIndex = 0;
        for (int j = 1; j < k; ++j) {
          if (distances[i][j] < minDist) {
            minDist = distances[i][j];
            minIndex = j;
          }
        }
        clusters.get(minIndex).add(i);
      }

      converged = true;
      for (int i = 0; i < k; ++i) {
        double[] clusterSum = new double[indexVectorSize];
        int size = clusters.get(i).size();
        for (int j = 0; j < size; ++j) {
          double[] currDataPoint =
            dataPoints.get(clusters.get(i).get(j).intValue());
          for (int m = 0; m < indexVectorSize; ++m) {
            clusterSum[m] += currDataPoint[m];
          }
        }
        for (int m = 0; m < indexVectorSize; ++m) {
          double newValue = clusterSum[m] / size;
          double diff = Math.abs(kCenters[i][m] - newValue);
          kCenters[i][m] = newValue;
          if (diff > .0001)
            converged = false;
        }
      }
    }
    return kCenters;
  }

  public static int[] kMeansClusterAssignments(List<double[]> dataPoints,
                                               double[][] centers) {
    int[] resultClustering = new int[dataPoints.size()];
    for (int i = 0; i < dataPoints.size(); ++i) {
      double[] dataPoint = dataPoints.get(i);
      double minClusterDist =
        Similarity.euclideanDistance(centers[0], dataPoint);
      resultClustering[i] = 0;
      for (int j = 1; j < centers.length; ++j) {
        double clusterDist = 
          Similarity.euclideanDistance(centers[j], dataPoint);
        if (clusterDist < minClusterDist)
          resultClustering[i] = j;
      }
    }
    return resultClustering;
  }

  public static double kMeansPotential(List<double[]> dataPoints,
                                       double[][] centers) {
    double sum = 0;
    for (double[] dataPoint : dataPoints) {
      double minClusterDist =
        Similarity.euclideanDistance(centers[0], dataPoint);
      for (int i = 1; i < centers.length; ++i) {
        double clusterDist = 
          Similarity.euclideanDistance(centers[i], dataPoint);
        if (clusterDist < minClusterDist)
          minClusterDist = clusterDist;
      }
      sum += minClusterDist;
    }
    return sum;
  }
}
