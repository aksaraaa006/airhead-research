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

public class LogLikelihoodTransformTest {

    @Test public void testMatrixTransformSize() {
        int[][] testInput = {{0, 0, 0, 0, 1, 1, 2, 4, 5, 0},
                             {0, 1, 1, 2, 0, 1, 5, 2, 8,10},
                             {1, 5, 0, 0, 1, 0, 6, 3, 7, 9},
                             {0, 1, 0, 1, 0, 1, 2, 0, 3, 0},
                             {1, 5, 7, 0, 0, 1, 6,10, 2,45},
                             {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};

        double[][] testOutput= {
            { 0.0000, 0.0000, 0.0000, 0.0000, 3553.8894, 3504.1087, 3091.7983, 3143.4738, 3000.5617, 0.0000},
            { 0.0000, 3584.6166, 3680.8795, 3804.7437, 0.0000, 3778.0867, 3366.9172, 3415.5553, 3273.9801, 2363.1831},
            { 3862.7967, 3621.9286, 0.0000, 0.0000, 3862.7967, 0.0000, 3403.0516, 3450.2232, 3307.4484, 2400.0420},
            { 0.0000, 3242.1049, 0.0000, 3462.8969, 0.0000, 3438.0421, 3025.7882, 0.0000, 2931.6491, 0.0000},
            { 4756.8927, 4513.4826, 4614.4973, 0.0000, 0.0000, 4709.2607, 4299.6935, 4344.4751, 4221.1883, 3317.3584},
            { 3513.0885, 3267.5027, 3364.9105, 3488.0600, 3513.0885, 3463.2306, 3050.5403, 3098.5179, 2954.9708, 2051.0708}};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 10);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 10; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new LogLikelihoodTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 10; ++col)
                assertEquals(inputMatrix.get(row, col),
                             outputMatrix.get(row, col), .00001);
        }
    }
}

