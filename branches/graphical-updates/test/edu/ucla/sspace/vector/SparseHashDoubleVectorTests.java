/*
 * Copyright 2010 David Jurgens
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

import edu.ucla.sspace.util.*;
import java.util.*;


/**
 * Tests for the {@link SparseHashDoubleVector} class.
 */
public class SparseHashDoubleVectorTests {

    @Test public void testMagnitude() {
        SparseHashDoubleVector v = new SparseHashDoubleVector(100);
        assertEquals(0, v.magnitude(), .0001);

        v.set(1, 1);
        assertEquals(1, v.magnitude(), .0001);

        v.set(1, 3);
        v.set(2, 4);
        assertEquals(5, v.magnitude(), .0001);

        SparseHashDoubleVector v2 = new SparseHashDoubleVector(v);
        assertEquals(5, v2.magnitude(), .0001);
    }

    @Test public void testSetAndAdd() {
        SparseHashDoubleVector v = new SparseHashDoubleVector(100);
        assertEquals(0, v.get(1), .0001);
        v.set(1, 1);
        assertEquals(1, v.get(1), .0001);

        v.set(1, 2);
        assertEquals(2, v.get(1), .0001);

        v.set(2, 3);
        assertEquals(3, v.get(2), .0001);

        v.add(1, 2);
        assertEquals(4, v.get(1), .0001);
        
        v.add(3, 1);
        assertEquals(1, v.get(3), .0001);

        v.add(4, -1);
        assertEquals(-1, v.get(4), .0001);

        v.add(3, -1);
        assertEquals(0, v.get(3), .0001);
    }

    @Test public void testIterator() {
        SparseHashDoubleVector v = new SparseHashDoubleVector(new double[] { 1, 2, 3, 4, 5 });
        Iterator<DoubleEntry> iter = v.iterator();
        int i = 0;
        Set<Double> control = new HashSet<Double>(Arrays.asList(new Double[] { 1d, 2d, 3d, 4d, 5d }));
        Set<Double> test = new HashSet<Double>();
        while (iter.hasNext()) {
            DoubleEntry e = iter.next();
            assertEquals(e.index() + 1d, e.value(), 0.001);
            test.add(e.value());
            i++;
        }
        assertEquals(5, i);
        assertEquals(control, test);
    }

    @Test public void testNonZeroIndices() {
        SparseHashDoubleVector v = new SparseHashDoubleVector(100);
        v.set(1, 3d);
        v.set(3, 3d);
        v.set(5, 3d);
        int[] nz = v.getNonZeroIndices();
        System.out.println("nz: " + Arrays.toString(nz));
        Arrays.sort(nz);
        assertEquals(3, nz.length);
        assertTrue(Arrays.binarySearch(nz, 1) >= 0);
        assertTrue(Arrays.binarySearch(nz, 3) >= 0);
        assertTrue(Arrays.binarySearch(nz, 5) >= 0);
    }
}