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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;


/**
 * An interface for any Ofline clustering implementation.  Immplementations must
 * take in a {@code Matrix} and return the set of cluster assignments for each
 * row in the matrix.  Immplementations should not modify the contents of a
 * {@code Matrix} to cluster.   Implementations should set up any specific
 * variables such as thresholds, number of clusters, weighting parameters, or
 * any others through a constructor.
 *
 * @author Keith Stevens
 */
public interface OfflineClustering {

    /**
     * Clusters the set of rows in the given {@code Matrix} and returns a unique
     * integer specifying the cluster the row to which the row vectors has been
     * assigned.
     */
    int[] cluster(Matrix vectors);
}
