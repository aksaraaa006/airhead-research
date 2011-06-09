/*
 * Copyright 2011 Keith Stevens
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

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public abstract class FileTransformerTestBase {

    /**
     * Returns the matrix format being tested.
     */
    abstract MatrixIO.Format format();

    /**
     * Returns the {@link FileTransformer} specific to the file format being
     * tested.
     */
    abstract FileTransformer fileTransformer();

    @Test public void testFileTransform() throws IOException {
        double[][] data = new double[][] {
            {1, 2, 0, 0, 4, 6},
            {6, 1, 0, 1, 0, 2},
            {1, 2, 3, 4, 5, 6},
            {0, 0, 0, 0, 1, 0},
        };
        Matrix input = new ArrayMatrix(data);
        Matrix expected = new ArrayMatrix(input.rows(), input.columns());
        for (int i = 0; i < expected.rows(); ++i)
            for (int j = 0; j < expected.columns(); ++j)
                expected.set(i, j, 1+input.get(i, j));

        MatrixIO.Format format = format();

        GlobalTransform transform = new OnePlusTransform();

        File out = File.createTempFile("testOut", "dat");
        out.deleteOnExit();

        File in = File.createTempFile("testIn", "dat");
        in.deleteOnExit();
        MatrixIO.writeMatrix(input, in, format);

        FileTransformer transformer = fileTransformer();
        transformer.transform(in, out, transform);

        Matrix created = MatrixIO.readMatrix(out, format);

        assertEquals(expected.rows(), created.rows());
        assertEquals(expected.columns(), created.columns());
        for (int i = 0; i < expected.rows(); ++i)
            for (int j = 0; j < expected.columns(); ++j)
                if (input.get(i,j) != 0d)
                    assertEquals(expected.get(i,j), created.get(i,j), .000001);
    }

    private class OnePlusTransform implements GlobalTransform {
        public void initializeStats(Matrix m) {
        }

        public void initializeStats(File f, MatrixIO.Format format) {
        }

        public int rows() {
            return 0;
        }

        public int columns() {
            return 0;
        }

        public double transform(int row, int col, double val) {
            return val + 1;
        }
    }
}
