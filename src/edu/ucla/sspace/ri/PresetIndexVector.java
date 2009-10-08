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

package edu.ucla.sspace.ri;

import java.util.Arrays;

/**
 * An index vector implementation where the values are predetermined at
 * construction.  This class is designed for use when the index vector values
 * need to be manually assigned.
 */
public class PresetIndexVector implements IndexVector {
   	
    protected final int length;
    
    protected final int[] positive;

    protected final int[] negative;

    /**
     * Constructs the index vector with the provided positive and negative
     * indices.
     */
    public PresetIndexVector(int length, int[] positiveIndices,
			     int[] negativeIndices) {
	this.length = length;
	this.positive = positiveIndices;
	this.negative = negativeIndices;

	// sort so we can use a binary search in getValue()
	Arrays.sort(positive);
	Arrays.sort(negative);	    
    }

    public boolean equals(Object o) {
	if (o instanceof IndexVector) {
	    IndexVector v = (IndexVector)o;
	    return Arrays.equals(positive, v.positiveDimensions()) &&
		   Arrays.equals(negative, v.negativeDimensions());
	}
	return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getValue(int index) {
	if (Arrays.binarySearch(positive,index) >= 0)
	    return 1;
	else return (Arrays.binarySearch(negative,index) >= 0) ? -1 : 0;
    }

    public int hashCode() {
	return Arrays.hashCode(positive) ^ Arrays.hashCode(negative);
    }
	
    /**
     * {@inheritDoc}
     */
    public int length() {
	return length;
    }

    /**
     * {@inheritDoc}
     */
    public int[] negativeDimensions() {
	return negative;
    }

    /**
     * {@inheritDoc}
     */
    public int[] positiveDimensions() {
	return positive;
    }

    public String toString() {
	return "IndexVector {pos: " + Arrays.toString(positive) + ", neg: " +
	    Arrays.toString(negative) + "}";
    }
}