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
            {0.00000, 0.00000, 0.00000, 0.00000, 3553.88936, 3504.10871, 3091.79828, 3143.47379, 3000.56169, 0.00000},
            {0.00000, 3584.61660, 3680.87953, 3804.74367, 0.00000, 3778.08670, 3366.91717, 3415.55529, 3273.98009, 2363.18312},
            {3862.79673, 3621.92864, 0.00000, 0.00000, 3862.79673, 0.00000, 3403.05162, 3450.22324, 3307.44837, 2400.04200},
            {0.00000, 3242.10491, 0.00000, 3462.89686, 0.00000, 3438.04208, 3025.78816, 0.00000, 2931.64907, 0.00000},
            {4756.89274, 4513.48258, 4614.49726, 0.00000, 0.00000, 4709.26072, 4299.69349, 4344.47505, 4221.18830, 3317.35842},
            {3513.08847, 3267.50268, 3364.91050, 3488.06004, 3513.08847, 3463.23065, 3050.54025, 3098.51786, 2954.97078, 2051.07081}};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 10);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 10; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new LogLikelihoodTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 10; ++col)
                assertEquals(testOutput[row][col],
                             outputMatrix.get(row, col), error);
        }
    }
}
