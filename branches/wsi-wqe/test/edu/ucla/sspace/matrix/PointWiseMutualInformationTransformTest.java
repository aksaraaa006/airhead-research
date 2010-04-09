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
            {0.00000, 0.00000, 0.00000, 0.00000, 1.47224, 0.96141, 0.17295, 0.96141, 0.92219, 0.00000},
            {0.00000, -0.83035, -0.46262, 1.04145, 0.00000, 0.12516, 0.25300, -0.56798, 0.55595, -0.13720},
            {0.57145, 0.71455, 0.00000, 0.00000, 0.57145, 0.00000, 0.37078, -0.22706, 0.35788, -0.30710},
            {0.00000, 0.49141, 0.00000, 1.67006, 0.00000, 1.44692, 0.65846, 0.00000, 0.89687, 0.00000},
            {-0.30662, -0.16352, 0.54068, 0.00000, 0.00000, -0.81744, -0.50729, 0.09885, -1.77296, 0.42427},
            {1.73460, 0.26826, 0.63599, 1.44692, 1.73460, 1.22378, -0.25783, -0.16252, -0.42488, -1.34117}};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 10);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 10; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new PointWiseMutualInformationTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 10; ++col)
                assertEquals(testOutput[row][col], outputMatrix.get(row,col),
                             error);
        }
    }
}

