package edu.ucla.sspace.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A collection of unit tests for {@link TrieMap}.
 */ 
public class TrieMapTests {

    
    @Test public void testConstructor() {
	TrieMap<String> m = new TrieMap<String>();
    }

    @Test public void testPut() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("a", "1");
	String s = m.get("a");
	assertEquals("1", s);
    }

    @Test public void testPutConflict() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("a", "1");
	m.put("a", "2");
	String s = m.get("a");
	assertEquals("2", s);
    }

    @Test public void testPutSubstringOfKey() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertEquals("1", m.get("catapult"));
	assertEquals("2", m.get("cat"));
    }

    @Test public void testPutKeyIsLonger() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "1");
	m.put("catapult", "2");

	assertEquals("1", m.get("cat"));
	assertEquals("2", m.get("catapult"));
    }
    
    @Test public void testPutKeyIsLongerByOne() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "1");
	m.put("cats", "2");

	assertEquals("1", m.get("cat"));
	assertEquals("2", m.get("cats"));
    }

    @Test public void testPutKeysLongerByOne() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "1");
	m.put("catx", "2");
	m.put("catxy", "3");
	m.put("catxyz", "4");

	assertEquals("1", m.get("cat"));
	assertEquals("2", m.get("catx"));
	assertEquals("3", m.get("catxy"));
	assertEquals("4", m.get("catxyz"));
    }

    @Test public void testPutKeysDiverge() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("category", "1");
	m.put("catamaran", "2");

	assertEquals("1", m.get("category"));
	assertEquals("2", m.get("catamaran"));
    }

    @Test public void testPutKeysConflictAndDiverge() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	assertEquals("0", m.get("cat"));
	assertEquals("1", m.get("category"));
	assertEquals("2", m.get("catamaran"));
    }

    @Test public void testContainsKey() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertTrue(m.containsKey("catapult"));
	assertTrue(m.containsKey("cat"));
    }

    @Test public void testContainsKeyFalse() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertFalse(m.containsKey("dog"));
	assertFalse(m.containsKey("c"));
	assertFalse(m.containsKey("ca"));
	assertFalse(m.containsKey("cats"));
	assertFalse(m.containsKey("catapul"));
    }

    @Test public void testKeySet() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	Set<CharSequence> test = m.keySet();
	
	assertTrue(test.contains("cat"));
	assertTrue(test.contains("category"));
	assertTrue(test.contains("catamaran"));
    }

    @Test public void testKeyIterator() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	Set<String> control = new HashSet<String>();
	control.add("cat");
	control.add("category");
	control.add("catamaran");

	Set<CharSequence> test = m.keySet();

	assertTrue(test.containsAll(control));
	assertTrue(control.containsAll(test));
    }

    @Test public void testValueIterator() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	Set<String> control = new HashSet<String>();
	control.add("0");
	control.add("1");
	control.add("2");

	Collection<String> test = m.values();

	System.out.println("test: " + test);
	
	assertEquals(control.size(), test.size());
	for (String s : test) {	    
	    assertTrue(control.contains(s));
	}
    }

    @Test public void testIteratorHasNext() {
	
	TrieMap<String> m = new TrieMap<String>();
	Iterator<CharSequence> it = m.keySet().iterator();
	assertFalse(it.hasNext());

	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	it = m.keySet().iterator();

	Set<CharSequence> control = new HashSet<CharSequence>();

	while (it.hasNext()) {
	    control.add(it.next());
	}

	Set<CharSequence> test = m.keySet();

	assertTrue(test.containsAll(control));
	assertTrue(control.containsAll(test));
    }

    @Test(expected=IllegalStateException.class)
    public void testIteratorNextError() {
	
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");

	Iterator<CharSequence> it = m.keySet().iterator();
	it.next();
	it.next(); // error
    }

    @Test(expected=IllegalStateException.class)
    public void testEmptyTrieIteratorNextError() {
	
	TrieMap<String> m = new TrieMap<String>();

	Iterator<CharSequence> it = m.keySet().iterator();
	it.next(); // error
    }

//     @Test public void testIteratorRemove() {
	
// 	TrieMap<String> m = new TrieMap<String>();
// 	m.put("cat", "0");
// 	assertTrue(m.containsKey("cat"));

// 	Iterator<CharSequence> it = m.keySet().iterator();
// 	it.next();
// 	it.remove();
// 	assertFalse(m.containsKey("cat"));
//     }

    @Test public void testSize() {
	TrieMap<String> m = new TrieMap<String>();
	assertEquals(0, m.size());
	m.put("cat", "0");
	assertEquals(1, m.size());
	m.put("category", "1");
	assertEquals(2, m.size());
	m.put("catamaran", "2");
	assertEquals(3, m.size());
    }

    @Test public void testIsEmtpy() {
	TrieMap<String> m = new TrieMap<String>();
	assertTrue(m.isEmpty());

	m.put("cat", "0");
	assertFalse(m.isEmpty());
	
	m.clear();
	assertTrue(m.isEmpty());
    }

    @Test public void testClear() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	m.clear();
	assertEquals(0, m.size());
	assertFalse(m.containsKey("cat"));
	assertFalse(m.containsKey("category"));
	assertFalse(m.containsKey("catamaran"));
    }

    public static void main(String args[]) {
	org.junit.runner.JUnitCore.main(TrieMapTests.class.getName());
    }

}