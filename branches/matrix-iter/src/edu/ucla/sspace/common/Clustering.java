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

import java.util.List;

/**
 * A standard interface for implementations of clustering algorithms.  Given a
 * set of data points to cluster, and the length of each data point, an
 * approximation of the optimal number of clusters will be found.  Each data
 * point must then be assigned some number specifying which cluster it is part
 * of.
 */
public interface Clustering {
  /**
   * Cluster the data points for any valid number of clusters.
   */
  public void cluster(List<double[]> data, int indexVectorSize);

  /**
   * Cluster the data points for a certain number of clusters.
   * Implementations which decide not to implement this should throw an
   * exception.
   */
  public void clusterK(List<double[]> data, int indexVectorSize, int k);

  /**
   * Return the assignments for each data point.  The assignments must be in the
   * same order as the original data points were passed in.
   */
  public int[] getAssignments();
}
