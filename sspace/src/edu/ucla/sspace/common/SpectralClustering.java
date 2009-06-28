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

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.DiagonalMatrix;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Normalize;

import edu.ucla.sspace.util.Duple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jnt.FFT.Factorize;

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
public class SpectralClustering implements Clustering {
  private List<ClusterNode> nodeClusters;
  private int indexVectorSize;
  private int nodeCount = 0;
  private int[] assignments;

  /** 
   * A special constructor to facilitate testing.
   *
   * @param vectorSize define the vector size of each data point.
   */
  protected SpectralClustering(int vectorSize) {
    indexVectorSize = vectorSize;
  }

  /**
   * An empty default constructor.
   */
  public SpectralClustering() {
  }

  private List<List<DataPoint>> optimalClustering = null;
  // TODO: Test the correctness of the clustering.

  /**
   * Cluster the given data points according to the Spectral Clustering
   * algorithm.  The resulting cluster assignments for each data point will be
   * saved in assignments.
   *
   * @param data data points to cluster.
   * @param vectorSize length of each vector in data.
   */
  @SuppressWarnings("unchecked")
  public void cluster(List<double[]> data, int vectorSize) {
    assignments = new int[data.size()];
    indexVectorSize = vectorSize;
    nodeCount = 0;
    nodeClusters = new ArrayList<ClusterNode>();
    List<DataPoint> dataPoints = new ArrayList<DataPoint>();
    int i = 0;
    for (double[] dataPoint : data) {
      normalize(dataPoint);
      dataPoints.add(new DataPoint(i, dataPoint));
      i++;
    }
    int rootIndex = recSpectralCluster(dataPoints);
    ClusterNode rootNode = nodeClusters.get(rootIndex);
    optimalClustering = null;
    double optimalPotential = Double.MAX_VALUE;
    for (Duple<List<List<DataPoint>>, Double> clustering : rootNode.clusters.values()) {
      if (clustering.y < optimalPotential) {
        optimalPotential = clustering.y;
        optimalClustering = clustering.x;
      }
    }

    List<DataPoint> sortedDataPoints = new ArrayList<DataPoint>();
    i = 0;
    for (List<DataPoint> cluster : optimalClustering) {
      for (DataPoint dataPoint : cluster) {
        dataPoint.clusterNumber = i;
        sortedDataPoints.add(dataPoint);
      }
      i++;
    }
    Collections.sort(sortedDataPoints);
    i = 0;
    for (DataPoint dataPoint : sortedDataPoints) {
      assignments[i] = dataPoint.clusterNumber;
      i++;
    }
  }

  private void normalize(double[] vector) {
    double sum = 0;
    for (double d : vector) {
      sum += d;
    }
    for (int i = 0; i < vector.length; ++i) {
      vector[i] = vector[i] / sum;
    }
  }

  public int[] getAssignments() {
    return assignments;
  }

  public void clusterK(List<double[]> data, int vectorSize, int k) {
    // TODO: implement this.
    assignments = new int[data.size()];
  }

  public double dotProduct(double[] arr1, double[] arr2) {
    double product = 0;
    for (int i = 0; i < arr1.length; ++i) {
      product += arr1[i] * arr2[i];
    }
    return product;
  }

  public double[] computeP(List<DataPoint> dataPoints) {
    double[] u = new double[indexVectorSize];
    for (DataPoint dataPoint : dataPoints) {
      for (int i = 0; i < indexVectorSize; ++i) {
        u[i] += dataPoint.data[i];
      }
    }
    int i = 0;
    double[] p = new double[dataPoints.size()];
    for (DataPoint dataPoint : dataPoints) {
      p[i] = dotProduct(dataPoint.data, u);
      i++;
    }
    return p;
  }

  public int recSpectralCluster(List<DataPoint> dataPoints) {
    int size = dataPoints.size();
    int nodeValue = nodeCount;
    ClusterNode currCluster = new ClusterNode();
    nodeClusters.add(currCluster);
    nodeCount++;
    if (size == 0)
      return -1;

    if (size == 1) {
      List<List<DataPoint>> cluster = new ArrayList<List<DataPoint>>();
      List<DataPoint> singleCluster = new ArrayList<DataPoint>();
      singleCluster.add(dataPoints.get(0));
      cluster.add(singleCluster);
      currCluster.addCluster(cluster);
      return nodeValue;
    }

    // Create vector u, a summation of each data point.
    double[] p = computeP(dataPoints);
    List<DataPoint> sortedDataPoints = computeSortedVector(dataPoints, p);
    List<List<DataPoint>> splitVectors = computeCut(sortedDataPoints, p);

    int leftNodeValue = recSpectralCluster(splitVectors.get(0));
    int rightNodeValue = recSpectralCluster(splitVectors.get(1));
    ClusterNode leftNode = nodeClusters.get(leftNodeValue);
    ClusterNode rightNode = nodeClusters.get(rightNodeValue);

    // Do the merging.
    List<List<DataPoint>> singleCluster = new ArrayList<List<DataPoint>>();
    singleCluster.add(sortedDataPoints);
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

    return nodeValue;
  }

  @SuppressWarnings("unchecked")
  public List<DataPoint> computeSortedVector(List<DataPoint> dataPoints,
                                              double[] p) {
    int size = dataPoints.size();
    int log = Factorize.log2(dataPoints.size());

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
    // For now just let it be a random normalized vector.
    List<Double> permutedV = new LinkedList<Double>();
    for (int i = 0; i < size; ++i) {
      permutedV.add(pi[i] * DInv.get(i, i));
    }

    Collections.shuffle(permutedV);
    Matrix v = new ArrayMatrix(size, 1);
    int i = 0;
    for (Double value : permutedV) {
      v.set(i, 0, value);
      i++;
    }

    for (int k = 0; k < log; ++k) {
      Normalize.byMagnitude(v);

      // For matrix multiplications:
      // v = DInv * v;
      v = Matrices.multiply(DInv, v);

      // v = ATrans * v;
      Matrix newV = new ArrayMatrix(indexVectorSize, 1);
      i = 0;
      for (DataPoint dataPoint : dataPoints) {
        for (int j = 0; j < indexVectorSize; ++j) {
          double value = newV.get(j, 0) + v.get(i, 0) * dataPoint.data[j];
          newV.set(j, 0, value);
        }
        i++;
      }

      // v = A * v;
      i = 0;
      for (DataPoint dataPoint : dataPoints) {
        double sum = 0;
        for (int j = 0; j < indexVectorSize; ++j)
          sum += dataPoint.data[j] * newV.get(j, 0);
        v.set(i, 0, sum);
        i++;
      }

      // v = D * RInv * v;
      v = Matrices.multiply(DRInv, v);
    }

    // v = DInv * v;
    v = Matrices.multiply(DInv, v);

    // Sort v such that v[i] < v[i+1], and sort the rows of A in the same
    // order.
    List<VectorPair> sortedV = new ArrayList<VectorPair>();
    i = 0;
    for (DataPoint dataPoint : dataPoints) {
      sortedV.add(new VectorPair(v.get(i, 0), dataPoint));
      i++;
    }
    
    Collections.sort(sortedV);
    List<DataPoint> sortedDataPoints = new ArrayList<DataPoint>();
    for (VectorPair pair : sortedV) {
      sortedDataPoints.add(pair.dataVector);
    }
    return sortedDataPoints;
  }

  public List<List<DataPoint>> computeCut(List<DataPoint> dataPoints,
                                          double[] p) {

    // Special case, when there are just two data points, the cut is trivial.
    if (dataPoints.size() == 2) {
      List<DataPoint> S = new ArrayList<DataPoint>();
      S.add(dataPoints.get(0));
      List<DataPoint> T = new ArrayList<DataPoint>();
      T.add(dataPoints.get(1));
      List<List<DataPoint>> splitVectors = new ArrayList<List<DataPoint>>();
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
    for (DataPoint dataPoint : dataPoints) {
      if (i == 0) {
        x = Arrays.copyOf(dataPoint.data, indexVectorSize);
        pS = p[i];
      } else if (i == 1) {
        y = Arrays.copyOf(dataPoint.data, indexVectorSize);
        pT = p[i];
      } else {
        pT += p[i];
        for (int j = 0; j < indexVectorSize; ++j) {
          y[j] += dataPoint.data[j];
        }
      }
      i++;
    }

    double u = dotProduct(x, y);

    i = 0;
    for (DataPoint dataPoint : dataPoints) {
      // Update u, pS, and pT for cuts beside the first.
      if (i == (dataPoints.size() - 1))
        continue;
      if (i > 0) {
        pS += p[i];
        pT -= p[i];
        u = u -
            dotProduct(x, dataPoint.data) +
            dotProduct(y, dataPoint.data) +
            dotProduct(dataPoint.data, dataPoint.data);
        for (int j = 0; j < indexVectorSize; ++j) {
          x[j] += dataPoint.data[j];
          y[j] -= dataPoint.data[j];
        }
      }
      double conductance = u / Math.min(pS, pT);
      if (conductance < bestValue) {
        bestValue = conductance;
        bestIndex = i;
      }
      i++;
    }

    i = 0;
    List<DataPoint> S = new ArrayList<DataPoint>();
    List<DataPoint> T = new ArrayList<DataPoint>();
    for (DataPoint dataPoint : dataPoints) {
      if (i <= bestIndex)
        S.add(dataPoint);
      else
        T.add(dataPoint);
      i++;
    }
    List<List<DataPoint>> splitList = new ArrayList<List<DataPoint>>();
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
    public Map<Integer, Duple<List<List<DataPoint>>, Double>> clusters;

    /**
     * Create the cluster node.
     */
    public ClusterNode() {
      clusters = new HashMap<Integer, Duple<List<List<DataPoint>>, Double>>();
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
    public void addCluster(List<List<DataPoint>> clustering) {
      int numClusters = clustering.size();
      List<double[]> centroids = new ArrayList<double[]>();
      for (List<DataPoint> cluster : clustering) {
        double[] centroid = new double[indexVectorSize];
        for (DataPoint point : cluster) {
          for (int i = 0; i < indexVectorSize; ++i) {
            centroid[i] += point.data[i];
          }
        }
        centroids.add(centroid);
      }

      Iterator<double[]> centroidIter = centroids.iterator();
      Iterator<List<DataPoint>> clusterIter = clustering.iterator();
      double distSum = 0;
      while (centroidIter.hasNext() && clusterIter.hasNext()) {
        double[] centroid = centroidIter.next();
        List<DataPoint> cluster = clusterIter.next();
        for (DataPoint point : cluster) {
          distSum += Math.pow(
              Similarity.euclideanDistance(centroid, point.data), 2);
        }
      }
      Duple<List<List<DataPoint>>, Double> p =
        new Duple<List<List<DataPoint>>, Double>(clustering, distSum);
      clusters.put(numClusters, p);
    }

    /**
     * Combine two sets of clusters, computing a new potential of the total
     * clustering.
     * @param cluster1 first set of clusters.
     * @param cluster2 second set of clusters.
     */
    public void addMerge(Duple<List<List<DataPoint>>, Double> cluster1,
                         Duple<List<List<DataPoint>>, Double> cluster2) {
      List<List<DataPoint>> newClusters = new ArrayList<List<DataPoint>>();
      for (List<DataPoint> c : cluster1.x)
        newClusters.add(c);
      for (List<DataPoint> c : cluster2.x)
        newClusters.add(c);
      double newDistSum = cluster1.y.doubleValue() + cluster2.y.doubleValue();

      Duple<List<List<DataPoint>>, Double> newDuple =
        new Duple<List<List<DataPoint>>, Double>(newClusters, newDistSum);
      clusters.put(newClusters.size(), newDuple);
    }
  }

  /**
   * A simple pair object which allows sorting of data points by their
   * corresponding eigenvector value.
   */
  private class VectorPair implements Comparable {
    public double eigenVectorValue;
    public DataPoint dataVector;
    public VectorPair(double e, DataPoint vector) {
      eigenVectorValue = e;
      dataVector = vector;
    }

    public int compareTo(Object o) {
      VectorPair other = (VectorPair) o;
      return (int) (this.eigenVectorValue - other.eigenVectorValue);
    }
  }

  private class DataPoint implements Comparable {
    public int originalPosition;
    public int clusterNumber;
    public double[] data;

    public DataPoint(int originalPos, double[] d) {
      data = d;
      originalPosition = originalPos;
      clusterNumber = 0;
    }

    public int compareTo(Object o) {
      DataPoint other = (DataPoint) o;
      return this.originalPosition - other.originalPosition;
    }
  }
}
