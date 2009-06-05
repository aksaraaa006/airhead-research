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

package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;

import java.io.*;

import java.util.*;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link FileBasedSemanticSpace} class.
 */
public class FileBasedSemanticSpaceTest {

    private final DummySemanticSpace test;

    public FileBasedSemanticSpaceTest() {
	test = new DummySemanticSpace();
	test.setVectorFor("cow", new double[] {1, 0, 0, 0});
	test.setVectorFor("dog", new double[] {0, 1, 0, 0});
	test.setVectorFor("ear", new double[] {0, 0, 1, 0});
	test.setVectorFor("fig", new double[] {0, 0, 0, 1});
	test.setVectorFor("git", new double[] {1, 1, 0, 0});
	test.setVectorFor("hat", new double[] {1, 0, 1, 0});
	test.setVectorFor("its", new double[] {1, 0, 0, 1});
    }

    /**
     * Test that {@link edu.ucla.sspace.tools.FileBasedSemanticSpace} can read in
     * a properly formatted text file, and be capable of returning the expected
     * wordset, along with the vectors for each word.
     */
    @Test public void testFileSpace() throws IOException {
	String[] words = {"the", "cat", "loves", "cheezburgers"};
	double[][] values = {{5, 4, 3, 2, 1},
			     {1, 2, 3, 4, 5},
			     {1, 1, 1, 1, 1},
			     {1.0, 2.0, 3.0, 4.0, 6.0}};
	// First build the test file.
	File testSpaceData = File.createTempFile("test-sspace-matrix", "dat");
	PrintWriter pw = new PrintWriter(testSpaceData);
	pw.println("4 5");
	for (int i = 0; i < 4; ++i) {
	    StringBuffer sb = new StringBuffer(words[i]);
	    sb.append("|");
	    for (int j = 0; j < 5; ++j) {
		sb.append(values[i][j]).append(" ");
	    }
	    pw.println(sb.toString());
	}
	pw.close();
	FileBasedSemanticSpace testSpace =
	    new FileBasedSemanticSpace(testSpaceData.getAbsolutePath());
	// Check the word set provided by the SemanticSpace, and the vectors
	// returned for each word.
	Set<String> spaceWords = testSpace.getWords();
	assertEquals(words.length, spaceWords.size());
	for (int i = 0; i < words.length; ++i) {
	    assertTrue(spaceWords.contains(words[i]));
	    double[] spaceVector = testSpace.getVectorFor(words[i]);
	    assertTrue(spaceVector != null);
	    for (int j = 0; j < 5; ++j) {
		assertEquals(values[i][j], spaceVector[j], .0001);
	    }
	}
    }



    @Test public void testText() throws Exception { 
	File textFile = File.createTempFile("test-text",".sspace");
	textFile.deleteOnExit();
	SemanticSpaceUtils.printSemanticSpace(test, textFile, SSpaceFormat.TEXT);
	SemanticSpace onDisk = 
	    new FileBasedSemanticSpace(textFile, SSpaceFormat.TEXT);
	
	assertEquals(test.getWords().size(), onDisk.getWords().size());
	assertTrue(test.getWords().containsAll(onDisk.getWords()));
	for (String word : test.getWords()) {
	    assertEquals(Arrays.toString(test.getVectorFor(word)),
			 Arrays.toString(onDisk.getVectorFor(word)));
	}	
    }

    @Test public void testSparseText() throws Exception { 
	File sparseTextFile = File.createTempFile("test-sparse-text",".sspace");
	sparseTextFile.deleteOnExit();
	SemanticSpaceUtils.
	    printSemanticSpace(test, sparseTextFile, SSpaceFormat.SPARSE_TEXT);
	SemanticSpace onDisk = 
	    new FileBasedSemanticSpace(sparseTextFile, SSpaceFormat.SPARSE_TEXT);
	
	assertEquals(test.getWords().size(), onDisk.getWords().size());
	assertTrue(test.getWords().containsAll(onDisk.getWords()));
	for (String word : test.getWords()) {
	    assertEquals(Arrays.toString(test.getVectorFor(word)),
			 Arrays.toString(onDisk.getVectorFor(word)));
	}	
    }

    @Test public void testBinary() throws Exception { 
	File binaryFile = File.createTempFile("test-binary",".sspace");
 	binaryFile.deleteOnExit();
	SemanticSpaceUtils.
	    printSemanticSpace(test, binaryFile, SSpaceFormat.BINARY);
	SemanticSpace onDisk = 
	    new FileBasedSemanticSpace(binaryFile, SSpaceFormat.BINARY);
	
	assertEquals(test.getWords().size(), onDisk.getWords().size());
	assertTrue(test.getWords().containsAll(onDisk.getWords()));
	for (String word : test.getWords()) {
	    assertEquals(Arrays.toString(test.getVectorFor(word)),
			 Arrays.toString(onDisk.getVectorFor(word)));
	}	
    }

    @Test public void testSparseBinary() throws Exception { 
	File sparseBinaryFile = File.createTempFile("test-sparse-binary",".sspace");
	sparseBinaryFile.deleteOnExit();
	SemanticSpaceUtils.printSemanticSpace(test, sparseBinaryFile, 
					      SSpaceFormat.SPARSE_BINARY);
	SemanticSpace onDisk = 
	    new FileBasedSemanticSpace(sparseBinaryFile, SSpaceFormat.SPARSE_BINARY);
	
	assertEquals(test.getWords().size(), onDisk.getWords().size());
	assertTrue(test.getWords().containsAll(onDisk.getWords()));
	for (String word : test.getWords()) {
	    assertEquals(Arrays.toString(test.getVectorFor(word)),
			 Arrays.toString(onDisk.getVectorFor(word)));
	}	
    }

    public static junit.framework.Test suite() {
	return new junit.framework.JUnit4TestAdapter(FileBasedSemanticSpaceTest.class);
    }
}
