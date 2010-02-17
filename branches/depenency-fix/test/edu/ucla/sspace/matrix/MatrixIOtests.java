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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class MatrixIOtests {
    public static double[][] testMatrix = {
        {2.3, 0, 4.2},
        {0, 1.3, 2.2},
        {3.8, 0, 0.5}};

    public static File getMatlabFile() throws Exception {
	File f = File.createTempFile("unit-test",".dat");
	PrintWriter pw = new PrintWriter(f);
	pw.println("1 1 2.3");
	pw.println("1 3 4.2");
	pw.println("2 2 1.3");
	pw.println("2 3 2.2");
	pw.println("3 1 3.8");
	pw.println("3 3 0.5"); 
	pw.close();
	return f;
    }

    public static File getSparseSVDLIBCFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        pw.println("3 3 6");
        pw.println("2");
        pw.println("0 2.3");
        pw.println("2 3.8");

        pw.println("1");
        pw.println("1 1.3");

        pw.println("3");
        pw.println("0 4.2");
        pw.println("1 2.2");
        pw.println("2 0.5");
        pw.close();
        return f;
    }

    public static File getSparseBinarySVDLIBCFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        DataOutputStream pw = new DataOutputStream(new FileOutputStream(f));
        pw.writeInt(3);
        pw.writeInt(3);
        pw.writeInt(6);

        pw.writeInt(2);
        pw.writeInt(0);
        pw.writeFloat(2.3f);
        pw.writeInt(2);
        pw.writeFloat(3.8f);

        pw.writeInt(1);
        pw.writeInt(1);
        pw.writeFloat(1.3f);

        pw.writeInt(3);
        pw.writeInt(0);
        pw.writeFloat(4.2f);
        pw.writeInt(1);
        pw.writeFloat(2.2f);
        pw.writeInt(2);
        pw.writeFloat(0.5f);

        pw.close();
        return f;
    }

    @Test public void matlabToSVDLIBCsparse() throws Exception {
	
	File matlab = getMatlabFile();
	File svdlibc = MatrixIO.convertFormat(matlab,
            MatrixIO.Format.MATLAB_SPARSE, MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
	
	WordIterator it = new WordIterator(
            new BufferedReader(new FileReader(svdlibc)));

	// size
	assertEquals("3", it.next());
	assertEquals("3", it.next());
	assertEquals("6", it.next());

	// col 1
	assertEquals("2", it.next());
	assertEquals("0", it.next());
	assertEquals("2.3", it.next());
	assertEquals("2", it.next());
	assertEquals("3.8", it.next());

	// col 2
	assertEquals("1", it.next());
	assertEquals("1", it.next());
	assertEquals("1.3", it.next());

	// col 3
	assertEquals("3", it.next());
	assertEquals("0", it.next());
	assertEquals("4.2", it.next());
	assertEquals("1", it.next());
	assertEquals("2.2", it.next());
	assertEquals("2", it.next());
	assertEquals("0.5", it.next());
    }

    @Test public void readMatrixArrayFromMatlabSparse() throws Exception {
        File matlab = getMatlabFile();
        double[][] resultMatrix =
            MatrixIO.readMatrixArray(matlab, MatrixIO.Format.MATLAB_SPARSE);
        assertEquals(testMatrix.length, resultMatrix.length); 
        assertEquals(testMatrix[0].length, resultMatrix[0].length); 
        int rows = testMatrix.length;
        int cols = testMatrix[0].length;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                assertEquals(testMatrix[i][j], resultMatrix[i][j], 0.0001);
            }
        }
    }

    @Test public void readMatrixArrayFromSVDLIBCSparse() throws Exception {
        File svdlibcSparseText = getSparseSVDLIBCFile();
        double[][] resultMatrix =
            MatrixIO.readMatrixArray(svdlibcSparseText,
                                     MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
        assertEquals(testMatrix.length, resultMatrix.length); 
        assertEquals(testMatrix[0].length, resultMatrix[0].length); 
        int rows = testMatrix.length;
        int cols = testMatrix[0].length;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                assertEquals(testMatrix[i][j], resultMatrix[i][j], 0.0001);
            }
        }
    }

    @Test public void readMatrixArrayFromSVDLIBCSparseBinary() throws Exception {
        File svdlibcSparseText = getSparseBinarySVDLIBCFile();
        double[][] resultMatrix =
            MatrixIO.readMatrixArray(svdlibcSparseText,
                                     MatrixIO.Format.SVDLIBC_SPARSE_BINARY);
        assertEquals(testMatrix.length, resultMatrix.length); 
        assertEquals(testMatrix[0].length, resultMatrix[0].length); 
        int rows = testMatrix.length;
        int cols = testMatrix[0].length;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                assertEquals(testMatrix[i][j], resultMatrix[i][j], 0.0001);
            }
        }
    }
}
