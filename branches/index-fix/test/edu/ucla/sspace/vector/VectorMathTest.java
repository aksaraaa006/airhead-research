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

package edu.ucla.sspace.vector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link VectorMath} class
 */
public class VectorMathTest {

    @Test public void testIntegerVectorAdd() {
        IntegerVector a = new CompactSparseIntegerVector(100);
        a.set(1, 20);
        a.set(5, -1);
        IntegerVector b = new CompactSparseIntegerVector(100);
        b.set(2, 20);
        b.set(5, 1);
        IntegerVector c = VectorMath.add(a, b);
        assertEquals(20, c.get(1));
        assertEquals(20, c.get(2));
        assertEquals(0, c.get(5));
        assertSame(a, c);
    }

    @Test public void testDoubleVectorAdd() {
        DoubleVector a = new CompactSparseVector(100);
        a.set(1, 20);
        a.set(5, -1);
        DoubleVector b = new CompactSparseVector(100);
        b.set(2, 20);
        b.set(5, 1);
        DoubleVector c = VectorMath.add(a, b);
        assertEquals(20, c.get(1), 0);
        assertEquals(20, c.get(2), 0);
        assertEquals(0, c.get(5), 0);
        assertSame(a, c);
    }

    @Test public void testIntegerVectorAddUnmodified() {
        IntegerVector a = new CompactSparseIntegerVector(100);
        a.set(1, 20);
        a.set(5, -1);
        IntegerVector b = new CompactSparseIntegerVector(100);
        b.set(2, 20);
        b.set(5, 1);
        IntegerVector c = VectorMath.addUnmodified(a, b);
        assertEquals(20, c.get(1));
        assertEquals(20, c.get(2));
        assertEquals(0, c.get(5));
        assertNotSame(a, c);
    }

    @Test public void testDoubleVectorAddUnmodified() {
        DoubleVector a = new CompactSparseVector(100);
        a.set(1, 20);
        a.set(5, -1);
        DoubleVector b = new CompactSparseVector(100);
        b.set(2, 20);
        b.set(5, 1);
        DoubleVector c = VectorMath.addUnmodified(a, b);
        assertEquals(20, c.get(1), 0);
        assertEquals(20, c.get(2), 0);
        assertEquals(0, c.get(5), 0);
        assertNotSame(a, c);
    }
}
