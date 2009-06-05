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

package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link WordFilter} class.
 */
public class WordFilterTests {
   

    public WordFilterTests() { }

    @Test public void testAllWordsInFilter() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat is big"));
	Set<String> filterSet = new HashSet<String>();
	filterSet.add("my");
	filterSet.add("cat");
	filterSet.add("is");
	filterSet.add("big");
	WordFilter it = new WordFilter(br, filterSet);
	assertEquals("my", it.next());
	assertEquals("cat", it.next());
	assertEquals("is", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testAllWordsInFilterInverted() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat is big"));
	Set<String> filterSet = new HashSet<String>();
	filterSet.add("my");
	filterSet.add("cat");
	filterSet.add("is");
	filterSet.add("big");
	WordFilter it = new WordFilter(br, filterSet, true);
	assertFalse(it.hasNext());
    }

    @Test public void testSomeWordsInFilter() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat is big"));
	Set<String> filterSet = new HashSet<String>();
	filterSet.add("my");
	filterSet.add("is");
	WordFilter it = new WordFilter(br, filterSet, false);
	assertEquals("my", it.next());
	assertEquals("is", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testSomeWordsInFilterInverted() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat is big"));
	Set<String> filterSet = new HashSet<String>();
	filterSet.add("my");
	filterSet.add("is");
	WordFilter it = new WordFilter(br, filterSet, true);
	assertEquals("cat", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

}