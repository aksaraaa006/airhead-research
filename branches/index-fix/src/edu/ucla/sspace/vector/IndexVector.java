/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.vector;

import java.io.Serializable;

import java.util.Arrays;


/**
 * An class for ternary (+1, 0, -1) valued index vectors.  This will return +1
 * for indicies returned by {@code positiveDimensions}, -1 for indices returned
 * by {@code negativeDimensions}, and 0 for all others.
 */
public class IndexVector implements Vector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The length of the created {@code IndexVector}.
     */
    private int indexVectorLength;

    /**
     * The indices which are all set to +1.
     */
    private int[] positiveDimensions;

    /**
     * The indices which are all set to -1.
     */
    private int[] negativeDimensions;

    /**
     * Create an {@code IndexVector} with the specified length, and
     * postive/negative dimensions.
     *
     * @param length The lenght of the {@code IndexVector}.
     * @param positiveIndices All indices which are pre-set to +1.
     * @param negativeIndices All indices which are pre-set to -1.
     */
    public IndexVector(int length,
                       int[] positiveIndices,
                       int[] negativeIndices) {
        indexVectorLength = length;
        positiveDimensions = positiveIndices;
        negativeDimensions = negativeIndices;
    }

    /**
     * {@inheritDoc}.
     */
    public double add(int index, double delta) {
        throw new UnsupportedOperationException(
                "Add is not supported on a IndexVector");
    }

    /**
     * {@inheritDoc}.
     */
    public double get(int index) {
        if (Arrays.binarySearch(positiveDimensions, index) >= 0)
            return 1;
        if (Arrays.binarySearch(negativeDimensions, index) >= 0)
            return -1;
        return 0;
    }

    /**
     * {@inheritDoc}.
     */
    public int length() {
        return indexVectorLength;
    }

    /**
     * {@inheritDoc}.
     */
    public void set(int index, double value) {
        throw new UnsupportedOperationException(
                "Set is not supported on a IndexVector");
    }

    /**
     * {@inheritDoc}.
     */
    public void set(double[] values) {
        throw new UnsupportedOperationException(
                "Set is not supported on a IndexVector");
    }

    /**
     * Returns the indices at which this vector is valued {@code -1}.
     *
     * @return An array of indices which have negative values.
     */
    public int[] negativeDimensions() {
        return negativeDimensions;
    }
    
    /**
     * Returns the indices at which this vector is valued {@code +1}.
     *
     * @return An array of indices which have positive values.
     */
    public int[] positiveDimensions() {
        return positiveDimensions;
    }
    
    /**
     * {@inheritDoc}.
     */
    public double[] toArray(int size) {
        double[] array = new double[size];
        for (int p : positiveDimensions)
            array[p] = 1;
        for (int n : negativeDimensions)
            array[n] = -1;
        return array;
    }
}
