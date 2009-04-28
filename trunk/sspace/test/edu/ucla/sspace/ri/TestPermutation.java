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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestPermutation {

    @Test public void testPermutation() {
	IndexVector v = new IndexVector(32);
	System.out.println("original: " + v);
	DefaultPermutationFunction func = new DefaultPermutationFunction();
	IndexVector permuted = func.permute(v, 1);
	System.out.println("permuted: " + permuted);
	assertNotEquals(v, permuted);
    }

    @Test public void testNegativePermutation() {
	IndexVector v = new IndexVector(32);
	DefaultPermutationFunction func = new DefaultPermutationFunction();
	IndexVector permuted = func.permute(v, -1);
	assertNotEquals(v, permuted);
    }

    @Test public void testInversePermutation() {
	IndexVector v = new IndexVector(32);
	DefaultPermutationFunction func = new DefaultPermutationFunction();
	IndexVector permuted = func.permute(v, 1);
	IndexVector invPermuted = func.permute(permuted, -1);
	assertNotEquals(v, permuted);
	assertNotEquals(permuted, invPermuted);
	assertEquals(v, invPermuted);
    }

    @Test public void testInverseNegativePermutation() {
	IndexVector v = new IndexVector(32);
	DefaultPermutationFunction func = new DefaultPermutationFunction();
	IndexVector permuted = func.permute(v, -1);
	IndexVector invPermuted = func.permute(permuted, 1);
	assertNotEquals(v, permuted);
	assertNotEquals(permuted, invPermuted);
	assertEquals(v, invPermuted);
    }

    private void assertNotEquals(Object o1, Object o2) {
	assertFalse(o1.equals(o2));
    }
}