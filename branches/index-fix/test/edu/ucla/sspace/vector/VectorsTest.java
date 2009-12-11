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


import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link Vectors} class
 */
public class VectorsTest {

    @Test public void testInstanceOf() {
        CompactSparseIntegerVector siv = new CompactSparseIntegerVector(100);
        CompactSparseIntegerVector sivCopy = Vectors.instanceOf(siv);
        assertEquals(siv.length(), sivCopy.length());

        CompactSparseVector csv = new CompactSparseVector(100);
        CompactSparseVector csvCopy = Vectors.instanceOf(csv);
        assertEquals(csv.length(), csvCopy.length());
    }

    @Test public void testIntegerCopyOf() {
        IntegerVector a = new CompactSparseIntegerVector(5);
        a.set(1, -123);
        IntegerVector b = Vectors.copyOf(a);
        assertTrue(b instanceof SparseIntegerVector);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.get(1));
        assertNotSame(a, b);

        a = new DenseIntVector(new int[] {1, 2, 4, 5});
        b = Vectors.copyOf(a);
        assertTrue(b instanceof DenseIntVector);
        assertEquals(a.length(), b.length());
        for (int i = 0; i < b.length(); ++i)
            assertEquals(a.get(i), b.get(i));
        assertNotSame(a, b);

    }

    @Test public void testDoubleCopyOf() {
        DoubleVector a = new CompactSparseVector(5);
        a.set(1, -123);
        DoubleVector b = Vectors.copyOf(a);
        assertTrue(b instanceof CompactSparseVector);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.get(1), 0);
        assertNotSame(a, b);

        a = new DenseVector(new double[] {1, 2, 4, 5});
        b = Vectors.copyOf(a);
        assertTrue(b instanceof DenseVector);
        assertEquals(a.length(), b.length());
        for (int i = 0; i < b.length(); ++i)
            assertEquals(a.get(i), b.get(i), 0);
        assertNotSame(a, b);
    }

    @Test public void testGenericCopyOf() {
        Vector a = new CompactSparseVector(5);
        a.set(1, -123);
        Vector b = Vectors.copyOf(a);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.getValue(1).doubleValue(), 0);
        assertNotSame(a, b);
    }
}
