package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

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
	SemanticSpaceIO.save(test, textFile, SSpaceFormat.TEXT);
	SemanticSpace onDisk = new OnDiskSemanticSpace(textFile);
	
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
	SemanticSpaceIO.save(test, sparseTextFile, SSpaceFormat.SPARSE_TEXT);
	SemanticSpace onDisk = new OnDiskSemanticSpace(sparseTextFile);
	
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
	SemanticSpaceIO.save(test, binaryFile, SSpaceFormat.BINARY);
	SemanticSpace onDisk = new OnDiskSemanticSpace(binaryFile);
	
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
	SemanticSpaceIO.save(test, sparseBinaryFile);
        SemanticSpace onDisk = new OnDiskSemanticSpace(sparseBinaryFile);
	
        assertEquals(test.getWords().size(), onDisk.getWords().size());
        assertTrue(test.getWords().containsAll(onDisk.getWords()));
        for (String word : test.getWords()) {
            assertEquals(Arrays.toString(test.getVectorFor(word)),
                         Arrays.toString(onDisk.getVectorFor(word)));
        }
    }
}
