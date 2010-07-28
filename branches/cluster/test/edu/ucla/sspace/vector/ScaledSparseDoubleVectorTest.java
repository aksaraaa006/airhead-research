/*
 * Copyright 2010 Keith Stevens 
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
 * Tests for the {@link ScaledDoubleVector} class.
 */
public class ScaledSparseDoubleVectorTest {

    @Test public void testScaledCreate() {
        double[] values = new double[] {1, 2, 0, 4, 0, 6, 0, 8, 0, 0};
        double scale = 10;
        SparseDoubleVector v = new SparseHashDoubleVector(values);
        SparseDoubleVector scaled = new ScaledSparseDoubleVector(v, scale);
        for (int i = 0; i < 10; ++i)
            assertEquals(values[i]*scale, scaled.get(i), .000001);
    }

    @Test public void testScaledAdd() {
        double[] values = new double[] {1, 2, 0, 4, 0, 6, 0, 8, 0, 0};
        double scale = 10;
        SparseDoubleVector v = new SparseHashDoubleVector(values);
        SparseDoubleVector scaled = new ScaledSparseDoubleVector(v, scale);
        scaled.add(0, 5);
        assertEquals(values[0]*scale + 5, scaled.get(0), .000001);
        assertEquals(values[0] + 5/scale, v.get(0), .000001);
        for (int i = 1; i < 10; ++i)
            assertEquals(values[i]*scale, scaled.get(i), .000001);
    }

    @Test public void testScaledSet() {
        double[] values = new double[] {1, 2, 0, 4, 0, 6, 0, 8, 0, 0};
        double scale = 10;
        SparseDoubleVector v = new SparseHashDoubleVector(values);
        SparseDoubleVector scaled = new ScaledSparseDoubleVector(v, scale);
        scaled.set(0, 5);
        assertEquals(5, scaled.get(0), .000001);
        assertEquals(5/scale, v.get(0), .000001);
        for (int i = 1; i < 10; ++i)
            assertEquals(values[i]*scale, scaled.get(i), .000001);
    }
}
