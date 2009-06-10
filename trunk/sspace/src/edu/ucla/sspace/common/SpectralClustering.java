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

import edu.ucla.sspace.common.matrix.ArrayMatrix;
import edu.ucla.sspace.common.matrix.DiagonalMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Spectral Clustering using divide and merge methodology.
 * The implementation is based on two papers:
 *
 * <ul>
 *   <li style="font-family:Garamond, Georgia, serif">Cheng, D., Kannan, R.,
 *     Vempala, S., Wang, G.  (2006).  A Divide-and-Merge Methodology for
 *     Clustering. <i>ACM Transactions on Database Systsms</i>, <b>31</b>,
 *     1499-1525.  Available <a
 *     href=http://www-math.mit.edu/~vempala/papers/eigencluster.pdf">here</a>
 *   </li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">Kannan, R., Vempala, S.,
 *     Vetta, A.  (2000).  On clustering: Good, bad, and spectral.  
 *     <i>FOCS '00: Proceedings of the 41st Annual Symposium on Foundations of
 *   Computer Science</i> Available <a
 *     href="http://www-math.mit.edu/~vempala/papers/specfocs.ps">here</a>
 *   </li>
 * </ul>
 *
 * Implementation currently still in progress.
 */
public class SpectralClustering {
  private List<double[]> originalDataPoints;
  private List<ClusterNode> nodeClusters;
  private int indexVectorSize;
  private int nodeCount = 0;
  // TODO: Add access to the final clusters, and to a vector showing which
  // clusters reach original data point has been assigned too.
  // TODO: Test the correctness of the clustering.

  /**
   * Create a new SpectralClutering object which will find the best tree
   * respecting clustering of the vectors in data.
   *
   * @param data data points to cluster.
   * @param vectorSize length of each vector in data.
   */
  public SpectralClustering(List<double[]> data, int vectorSize) {
    originalDataPoints = data;
    indexVectorSize = vectorSize;
    nodeCount = 0;
    nodeClusters = new ArrayList<ClusterNode>();
  }

  private double dotProduct(double[] arr1, double[] arr2) {
    double product = 0;
    for (int i = 0; i < arr1.length; ++i) {
      product += arr1[i] * arr2[i];
    }
    return product;
  }

  private double[] computeP(List<double[]> dataPoints) {
    double[] u = new double[indexVectorSize];
    for (double[] dataPoint : dataPoints) {
      for (int i = 0; i < indexVectorSize; ++i) {
        u[i] += dataPoint[i];
      }
    }
    int i = 0;
    double[] p = new double[dataPoints.size()];
    for (double[] dataPoint : dataPoints) {
      p[i] = dotProduct(dataPoint, u);
      i++;
    }
    return p;
  }

  private int recSpectralCluster(List<double[]> dataPoints) {
    int size = dataPoints.size();
    int nodeValue = nodeCount;
    ClusterNode currCluster = new ClusterNode();
    nodeClusters.add(currCluster);
    nodeCount++;
    if (size == 0)
      return -1;

    if (size == 1) {
      List<List<double[]>> cluster = new ArrayList<List<double[]>>();
      List<double[]> singleCluster = new ArrayList<double[]>();
      singleCluster.add(dataPoints.get(0));
      cluster.add(singleCluster);
      currCluster.addCluster(cluster);
      return nodeValue;
    }

    // Create vector u, a summation of each data point.
    double[] p = computeP(dataPoints);
    List<double[]> sortedDataPoints = computeSortedVector(dataPoints, p);
    List<List<double[]>> splitVectors = computeCut(sortedDataPoints, p);
    List<List<double[]>> singleCluster = new ArrayList<List<double[]>>();
    singleCluster.add(sortedDataPoints);

    int leftNodeValue = recSpectralCluster(splitVectors.get(0));
    int rightNodeValue = recSpectralCluster(splitVectors.get(1));
    ClusterNode leftNode = nodeClusters.get(leftNodeValue);
    ClusterNode rightNode = nodeClusters.get(rightNodeValue);

    // Do the merging.
    currCluster.addCluster(singleCluster);
    for (int i = 2; i <= size; ++i) {
      for (int j = 1; j < i; ++j) {
        if (leftNode.clusters.containsKey(j) &&
            rightNode.clusters.containsKey(i-j)) {
          currCluster.addMerge(leftNode.clusters.get(j),
                               rightNode.clusters.get(i-j));
        }
      }
    }

    return nodeCount;
  }

  private List<double[]> computeSortedVector(List<double[]> dataPoints,
                                             double[] p) {
    int size = dataPoints.size();

    // Create Diagonal matrices R and D where R(i, i) = p(i), and
    // D(i, i) = sqrt(p(i) / sum(p)).
    Matrix R = new DiagonalMatrix(size);
    Matrix RInv = new DiagonalMatrix(size);
    double pSum = 0;
    for (int i = 0; i < size; ++i) {
      R.set(i, i, p[i]);
      RInv.set(i, i, 1 / p[i]);
      pSum += p[i];
    }

    double[] pi = new double[size];
    Matrix D = new DiagonalMatrix(size);
    Matrix DInv = new DiagonalMatrix(size);
    for (int i = 0; i < size; ++i) {
      pi[i] = R.get(i, i) / pSum;
      double dI = Math.sqrt(pi[i]);
      D.set(i, i, dI);
      DInv.set(i, i, 1 / dI);
    }
    Matrix DRInv = Matrices.multiply(D, RInv);

    // Set v to be orthogonal to piTrans * DInv;
    Matrix v = new ArrayMatrix(size, 1);

    // For matrix multiplications:
    // v = DInv * v;
    v = Matrices.multiply(DInv, v);

    // v = ATrans * v;
    Matrix newV = new ArrayMatrix(indexVectorSize, 1);
    int i = 0;
    for (double[] dataPoint : dataPoints) {
      for (int j = 0; j < indexVectorSize; ++j) {
        double value = newV.get(j, 1) + v.get(i, 1) * dataPoint[j];
        newV.set(j, 1, value);
      }
      i++;
    }

    // v = A * v;
    i = 0;
    for (double[] dataPoint : dataPoints) {
      double sum = 0;
      for (int j = 0; j < indexVectorSize; ++j)
        sum += dataPoint[j] * newV.get(j, 1);
      v.set(i, 1, sum);
      i++;
    }

    // v = D * RInv * v;
    v = Matrices.multiply(DRInv, v);
    List<VectorPair> sortedV = new ArrayList<VectorPair>();
    i = 0;
    for (double[] dataPoint : dataPoints) {
      sortedV.add(new VectorPair(v.get(i, i), dataPoint));
      i++;
    }
    Collections.sort(sortedV);
    List<double[]> sortedDataPoints = new ArrayList<double[]>();
    for (VectorPair pair : sortedV) {
      sortedDataPoints.add(pair.dataVector);
    }
    return sortedDataPoints;
  }

  private List<List<double[]>> computeCut(List<double[]> dataPoints,
                                          double[] p) {

    // Special case, when there are just two data points, the cut is trivial.
    if (dataPoints.size() == 2) {
      List<double[]> S = new ArrayList<double[]>();
      S.add(dataPoints.get(0));
      List<double[]> T = new ArrayList<double[]>();
      T.add(dataPoints.get(1));
      List<List<double[]>> splitVectors = new ArrayList<List<double[]>>();
      splitVectors.add(S);
      splitVectors.add(T);
      return splitVectors;
    }

    int i = 0;
    double[] x = null;
    double[] y = null;
    double pS = 0;
    double pT = 0;
    double bestValue = Double.MAX_VALUE;
    int bestIndex = 0;
    for (double[] dataPoint : dataPoints) {
      if (i == 0) {
        x = Arrays.copyOf(dataPoint, indexVectorSize);
        pS = p[i];
      } else if (i == 1) {
        y = Arrays.copyOf(dataPoint, indexVectorSize);
        pT = p[i];
      } else {
        pT += p[i];
        for (int j = 0; j < indexVectorSize; ++j) {
          y[j] += dataPoint[j];
        }
      }
    }

    double u = dotProduct(x, y);

    i = 0;
    for (double[] dataPoint : dataPoints) {
      // Update u, pS, and pT for cuts beside the first.
      if (i > 0) {
        pS += p[i];
        pT -= p[i];
        u = u -
            dotProduct(x, dataPoint) +
            dotProduct(y, dataPoint) +
            dotProduct(dataPoint, dataPoint);
        for (int j = 0; j < indexVectorSize; ++j) {
          x[j] += dataPoint[j];
          y[j] -= dataPoint[j];
        }
      }
      double conductance = u / Math.min(pS, pT);
      if (conductance < bestValue) {
        bestValue = conductance;
        bestIndex = i;
      }
    }

    i = 0;
    List<double[]> S = new ArrayList<double[]>();
    List<double[]> T = new ArrayList<double[]>();
    for (double[] dataPoint : dataPoints) {
      if (i < bestIndex)
        S.add(dataPoint);
      else
        T.add(dataPoint);
      i++;
    }
    List<List<double[]>> splitList = new ArrayList<List<double[]>>();
    splitList.add(S);
    splitList.add(T);

    return splitList;
  }

  /**
   * A node representing the data computed at each node in the divided tree of
   * data points.  Each node contains the potential of each possible clustering
   * of the points within this node of the tree.
   */
  private class ClusterNode {
    /**
     * A map from number of clusters to the clusters themselves, and the
     * potential of the clustering.
     */
    public Map<Integer, Duple<List<List<double[]>>, Double>> clusters;

    /**
     * Create the cluster node.
     */
    public ClusterNode() {
      clusters = new HashMap<Integer, Duple<List<List<double[]>>, Double>>();
    }

    /**
     * Add a new clustering to the node.  The potential of this clustering will
     * be computed and added to clusters, based on the number of clusters
     * present in clustering.
     *
     * @param clustering a possible clustering of the leaf nodes contained by
     * this {@code ClusterNode}.  Each inner list contains the data points for a
     * single cluster.
     */
    public void addCluster(List<List<double[]>> clustering) {
      int numClusters = clustering.size();
      List<double[]> centroids = new ArrayList<double[]>();
      for (List<double[]> cluster : clustering) {
        double[] centroid = new double[indexVectorSize];
        for (double[] point : cluster) {
          for (int i = 0; i < indexVectorSize; ++i) {
            centroid[i] += point[i];
          }
        }
        centroids.add(centroid);
      }

      Iterator<double[]> centroidIter = centroids.iterator();
      Iterator<List<double[]>> clusterIter = clustering.iterator();
      double distSum = 0;
      while (centroidIter.hasNext() && clusterIter.hasNext()) {
        double[] centroid = centroidIter.next();
        List<double[]> cluster = clusterIter.next();
        for (double[] point : cluster) {
          distSum += Math.pow(
              Similarity.euclideanDistance(centroid, point), 2);
        }
      }
      Duple<List<List<double[]>>, Double> p =
        new Duple<List<List<double[]>>, Double>(clustering, distSum);
      clusters.put(numClusters, p);
    }

    /**
     * Combine two sets of clusters, computing a new potential of the total
     * clustering.
     * @param cluster1 first set of clusters.
     * @param cluster2 second set of clusters.
     */
    public void addMerge(Duple<List<List<double[]>>, Double> cluster1,
                         Duple<List<List<double[]>>, Double> cluster2) {
      List<List<double[]>> newClusters = new ArrayList<List<double[]>>();
      for (List<double[]> c : cluster1.x)
        newClusters.add(c);
      for (List<double[]> c : cluster2.x)
        newClusters.add(c);
      double newDistSum = cluster1.y.doubleValue() + cluster2.y.doubleValue();

      Duple<List<List<double[]>>, Double> newDuple =
        new Duple<List<List<double[]>>, Double>(newClusters, newDistSum);
      clusters.put(newClusters.size(), newDuple);
    }
  }

  /**
   * A simple pair object which allows sorting of data points by their
   * corresponding eigenvector value.
   */
  private class VectorPair implements Comparable {
    public double eigenVectorValue;
    public double[] dataVector;
    public VectorPair(double e, double[] vector) {
      eigenVectorValue = e;
      dataVector = vector;
    }

    public int compareTo(Object o) {
      VectorPair other = (VectorPair) o;
      return (int) (this.eigenVectorValue - other.eigenVectorValue);
    }
  }
}
