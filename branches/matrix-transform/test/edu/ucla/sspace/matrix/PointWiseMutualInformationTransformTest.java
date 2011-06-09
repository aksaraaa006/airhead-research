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

public class PointWiseMutualInformationTransformTest {

    @Test public void testMatrixTransformSize() {
        int[][] testInput = {{0, 0, 0, 0, 1, 1, 2, 4, 5, 0},
                             {0, 1, 1, 2, 0, 1, 5, 2, 8,10},
                             {1, 5, 0, 0, 1, 0, 6, 3, 7, 9},
                             {0, 1, 0, 1, 0, 1, 2, 0, 3, 0},
                             {1, 5, 7, 0, 0, 1, 6,10, 2,45},
                             {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};

        double[][] testOutput= {
            { 0.0000, 0.0000, 0.0000, 0.0000, 1.4722, 0.9614, 0.1730, 0.9614, 0.9222, 0.0000},
            { 0.0000, -0.8303, -0.4626, 1.0415, 0.0000, 0.1252, 0.2530, -0.5680, 0.5559, -0.1372},
            { 0.5715, 0.7146, 0.0000, 0.0000, 0.5715, 0.0000, 0.3708, -0.2271, 0.3579, -0.3071},
            { 0.0000, 0.4914, 0.0000, 1.6701, 0.0000, 1.4469, 0.6585, 0.0000, 0.8969, 0.0000},
            { -0.3066, -0.1635, 0.5407, 0.0000, 0.0000, -0.8174, -0.5073, 0.0988, -1.7730, 0.4243},
            { 1.7346, 0.2683, 0.6360, 1.4469, 1.7346, 1.2238, -0.2578, -0.1625, -0.4249, -1.3412}};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 10);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 10; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new PointWiseMutualInformationTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 10; ++col)
                assertEquals(inputMatrix.get(row, col),
                             outputMatrix.get(row, col), .00001);
        }
    }
}

