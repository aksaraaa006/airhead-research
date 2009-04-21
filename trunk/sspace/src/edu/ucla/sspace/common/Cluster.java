package edu.ucla.sspace.common;

import java.util.ArrayList;
import java.util.HashSet;
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
  public static int[] kMeansCluster(ArrayList<double[]> dataPoints,
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
      kCenters[k_index] = dataPoints.get(index.intValue());
      k_index++;
    }

    boolean converged = false;
    ArrayList<ArrayList<Integer>> clusters = null;
    while (!converged) {
      double[][] distances = new double[dataPoints.size()][k];
      for (int i = 0; i < dataPoints.size(); ++i) {
        for (int j = 0; j < k; ++j) {
          distances[i][j] = Similarity.euclideanDistance(kCenters[j],
                                                         dataPoints.get(i));
        }
      }
      clusters = new ArrayList<ArrayList<Integer>>();
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
    if (clusters == null)
      return null;

    int[] resultClustering = new int[dataPoints.size()];
    for (int i = 0; i < k; ++i) {
      for (int j = 0; j < clusters.get(i).size(); ++j) {
        resultClustering[clusters.get(i).get(j).intValue()] = i;
      }
    }
    return resultClustering;
  }
}
