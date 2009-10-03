package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.io.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link OnDiskSemanticSpace} 
 */
public class OnDiskSemanticSpaceTests {

    DummySemanticSpace test;
    
    public OnDiskSemanticSpaceTests() {
	test = new DummySemanticSpace();
	test.setVectorFor("cow", new double[] {1, 0, 0, 0});
	test.setVectorFor("dog", new double[] {0, 1, 0, 0});
	test.setVectorFor("ear", new double[] {0, 0, 1, 0});
	test.setVectorFor("fig", new double[] {0, 0, 0, 1});
	test.setVectorFor("git", new double[] {1, 1, 0, 0});
	test.setVectorFor("hat", new double[] {1, 0, 1, 0});
	test.setVectorFor("its", new double[] {1, 0, 0, 1});
    }

    @Test public void testText() throws Exception { 
	File textFile = File.createTempFile("test-text",".sspace");
	textFile.deleteOnExit();
	SemanticSpaceUtils.printSemanticSpace(test, textFile, SSpaceFormat.TEXT);
	SemanticSpace onDisk = 
	    new OnDiskSemanticSpace(textFile, SSpaceFormat.TEXT);
	
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
	    new OnDiskSemanticSpace(sparseTextFile, SSpaceFormat.SPARSE_TEXT);
	
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
	    new OnDiskSemanticSpace(binaryFile, SSpaceFormat.BINARY);
	
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
	    new OnDiskSemanticSpace(sparseBinaryFile, SSpaceFormat.SPARSE_BINARY);
	
	assertEquals(test.getWords().size(), onDisk.getWords().size());
	assertTrue(test.getWords().containsAll(onDisk.getWords()));
	for (String word : test.getWords()) {
	    assertEquals(Arrays.toString(test.getVectorFor(word)),
			 Arrays.toString(onDisk.getVectorFor(word)));
	}	
    }
}