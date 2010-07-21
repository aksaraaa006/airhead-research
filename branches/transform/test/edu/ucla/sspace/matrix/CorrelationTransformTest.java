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

package edu.ucla.sspace.matrix;

import org.junit.*;

import static org.junit.Assert.*;


public class CorrelationTransformTest {

    @Test public void testCorrelationWithSaveNegs() {
        // 13 x 13 matrix.
        double[] testCorrelations = {0, 5, 9, 6, 1,10, 4, 8,18, 9,10, 0, 0,
                                     5, 4, 2, 1, 0, 0, 7,10, 3, 2, 1, 0, 5,
                                     9, 2, 0, 8, 0, 5, 1, 9,11, 2, 4, 3, 3,
                                     6, 1, 8, 0, 0, 4, 0, 6, 8, 0, 2, 2, 2,
                                     1, 0, 0, 0, 0, 0, 4, 3, 0, 2, 0, 0, 0,
                                     10,0, 5, 4, 0, 0, 0, 0,10, 3, 8, 0, 0,
                                     4, 7, 1, 0, 4, 0, 0,10, 2, 3, 0, 0, 3,
                                     8,10, 9, 6, 3, 0,10, 2, 8, 5, 0, 4, 6,
                                     18,3,11, 8, 0,10, 2, 8, 0, 8, 10,1, 1,
                                     9, 2, 2, 0, 2, 3, 3, 5, 8, 0, 5, 0, 0,
                                     10, 1, 4, 2, 0, 8, 0, 0,10, 5,0, 0, 0,
                                     0, 0, 3, 2, 0, 0, 0, 4, 1, 0, 0, 0, 0,
                                     0, 5, 3, 2, 0, 0, 3, 6, 1, 0, 0, 0, 0};
        Matrix testIn = new ArrayMatrix(13, 13, testCorrelations);

        double[][] testResults = {
            {0, 0, .12, .093, 0, .291, 0, 0, .31, .262, .291, 0, 0},
            {0, .175, 0, 0, 0, 0, .364, .320, 0, 0, 0, 0, .365},
            {.12, 0, 0, .306, 0, .146, 0, .177, .220, 0, 0, .297, .175},
            {.093, 0, .306, 0, 0, .182, 0, .149, .221, 0, 0, .263, .151},
            {0, 0, 0, 0, 0, 0, .438, .265, 0, .263, 0, 0, 0},
            {.291, 0, .146, .182, 0, 0, 0, 0, .291, .076, .372, 0, 0},
            {0, .364, 0, 0, .438, 0, 0, .358, 0, .136, 0, 0, .268},
            {0, .320, .177, .149, .265, 0, .358, 0, 0, .034, 0, .333, .317},
            {.310, 0, .220, .221, .0, .291, 0, 0, 0, .221, .291, 0, 0},
            {.262, 0, 0, 0, .263, .076, .136, .034, .221, 0, .246, 0, 0},
            {.291, 0, 0, 0, 0, .372, 0, 0, .291, .246, 0, 0, 0},
            {0, 0, .297, .263, 0, 0, 0, .333, 0, 0, 0, 0, 0},
            {0, .365, .175, .151, .0, 0, .268, .317, .0, 0, 0, 0, 0}};

        Transform correlation = new CorrelationTransform();
        testIn = correlation.transform(testIn);
        for (int i = 0; i < 13 ; ++i) {
            for (int j = 0; j < 13; ++j)
                assertEquals(testResults[i][j], testIn.get(i, j), .001);
        }
    }
}
