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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 *
 */
public class IndexVector {

    private static final int BITS_TO_SET = 9; // +/- 3
    private static final int BIT_VARIANCE = 3;

    private static final Random RANDOM = RandomIndexing.RANDOM;
    
    private final int[] positive;
    private final int[] negative;

    private final int length;
    
    public IndexVector(int length) {
	HashSet<Integer> pos = new HashSet<Integer>();
	HashSet<Integer> neg = new HashSet<Integer>();
	this.length = length;
	
	// randomly set bits in the index vector
	int bitsToSet = BITS_TO_SET +
	    (int)(RANDOM.nextDouble() * BIT_VARIANCE *
		  ((RANDOM.nextDouble() > .5) ? 1 : -1));
	for (int i = 0; i < bitsToSet; ++i) {

	    boolean picked = false;
	    // loop to ensure we actually pick the full number of bits
	    while (!picked) {
		// pick some random index
		int index = (int)(RANDOM.nextDouble() * length);
		    
		// check that we haven't already added this index
		if (pos.contains(index) || neg.contains(index))
		    continue;
		    
		// decide positive or negative
		((RANDOM.nextDouble() > .5) ? pos : neg).add(index);
		picked = true;
	    }
	}
	    
	positive = new int[pos.size()];
	negative = new int[neg.size()];

	Iterator<Integer> it = pos.iterator();
	for (int i = 0; i < positive.length; ++i) 
	    positive[i] = it.next();

	it = neg.iterator();
	for (int i = 0; i < negative.length; ++i) 
	    negative[i] = it.next();		

	// sort so we can use a binary search in getValue()
	Arrays.sort(positive);
	Arrays.sort(negative);
    }
	

    /**
     * Constructs the index vector with the provided positive and negative
     * indices.
     */
    IndexVector(int length, int[] positiveIndices, int[] negativeIndices) {
	// package protected
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

    public int getValue(int index) {
	if (Arrays.binarySearch(positive,index) >= 0)
	    return 1;
	else return (Arrays.binarySearch(negative,index) >= 0) ? -1 : 0;
    }

    public int hashCode() {
	return Arrays.hashCode(positive) ^ Arrays.hashCode(negative);
    }
	
    public int length() {
	return length;
    }

    public int[] negativeDimensions() {
	return negative;
    }

    public int[] positiveDimensions() {
	return positive;
    }

    public String toString() {
	return "IndexVector {pos: " + Arrays.toString(positive) + ", neg: " +
	    Arrays.toString(negative) + "}";
    }
}