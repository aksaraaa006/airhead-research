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
 * An index vector impementation that randomly sets values to +1 and -1 based on
 * provided parameters.
 */
public class RandomIndexVector extends PresetIndexVector {

    /**
     * A reference to the {@code RandomIndexing} source of randomness.  This
     * allows any random values to be correlated with whatever seed {@code
     * RandomIndexing} uses.
     */
    private static final Random RANDOM = RandomIndexing.RANDOM;
        
    public RandomIndexVector(int length, int bitsToSet, int bitVariance) {
	this(length, generateIndices(length, bitsToSet, bitVariance));
    }

    private RandomIndexVector(int length, int[][] posAndNegIndices) {
	super(length, posAndNegIndices[0], posAndNegIndices[1]);
    }

    private static int[][] generateIndices(int length, int bitsToSet,
					   int bitVariance) {
	HashSet<Integer> pos = new HashSet<Integer>();
	HashSet<Integer> neg = new HashSet<Integer>();
	
	// Randomly decide how many bits to set in the index vector based on the
	// variance.
	bitsToSet = bitsToSet +
	    (int)(RANDOM.nextDouble() * bitVariance *
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
	    
	int[] positive = new int[pos.size()];
	int[] negative = new int[neg.size()];

	Iterator<Integer> it = pos.iterator();
	for (int i = 0; i < positive.length; ++i) 
	    positive[i] = it.next();

	it = neg.iterator();
	for (int i = 0; i < negative.length; ++i) 
	    negative[i] = it.next();		

	// sort so we can use a binary search in getValue()
	Arrays.sort(positive);
	Arrays.sort(negative);

	return new int[][] {positive, negative};
    }
}