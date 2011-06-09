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

package edu.ucla.sspace.matrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;

public class RowMagnitudeTransformTest {

    @Test public void testMatrixTransformSize() {
        int[][] testInput = {{0, 0, 0, 0, 1, 1, 2, 4, 5, 0},
                             {0, 1, 1, 2, 0, 1, 5, 2, 8,10},
                             {1, 5, 0, 0, 1, 0, 6, 3, 7, 9},
                             {0, 1, 0, 1, 0, 1, 2, 0, 3, 0},
                             {1, 5, 7, 0, 0, 1, 6,10, 2,45},
                             {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};

        double[][] testOutput= {
            { 0.0000, 0.0000, 0.0000, 0.0000, 0.1459, 0.1459, 0.2917, 0.5835, 0.7293, 0.0000},
            { 0.0000, 0.0707, 0.0707, 0.1414, 0.0000, 0.0707, 0.3536, 0.1414, 0.5657, 0.7071},
            { 0.0704, 0.3518, 0.0000, 0.0000, 0.0704, 0.0000, 0.4222, 0.2111, 0.4925, 0.6332},
            { 0.0000, 0.2500, 0.0000, 0.2500, 0.0000, 0.2500, 0.5000, 0.0000, 0.7500, 0.0000},
            { 0.0211, 0.1056, 0.1479, 0.0000, 0.0000, 0.0211, 0.1267, 0.2112, 0.0422, 0.9506},
            { 0.3162, 0.3162, 0.3162, 0.3162, 0.3162, 0.3162, 0.3162, 0.3162, 0.3162, 0.3162}};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 10);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 10; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new RowMagnitudeTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 10; ++col)
                assertEquals(inputMatrix.get(row, col),
                             outputMatrix.get(row, col), .00001);
        }
    }
}

