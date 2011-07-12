package edu.ucla.sspace.basis;

import java.util.Set;

import org.junit.*;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class StringBasisMappingTest {

    @Test public void testGetDimension() {
        StringBasisMapping basis = new StringBasisMapping();
        assertEquals(0, basis.getDimension("cat"));
        assertEquals(1, basis.getDimension("dog"));
        assertEquals(2, basis.getDimension("c"));
        assertEquals(0, basis.getDimension("cat"));
    }

    @Test public void testNumDimensions() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("c");
        basis.getDimension("at");
        assertEquals(3, basis.numDimensions());
    }

    @Test public void testKeySet() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("c");
        basis.getDimension("at");
        Set<String> keySet = basis.keySet();
        assertTrue(keySet.contains("cat"));
        assertTrue(keySet.contains("c"));
        assertTrue(keySet.contains("at"));
    }

    @Test public void testReadOnly() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("c");
        basis.getDimension("at");

        basis.setReadOnly(true);
        assertEquals(0, basis.getDimension("cat"));
        assertEquals(1, basis.getDimension("c"));
        assertEquals(2, basis.getDimension("at"));
        assertEquals(-1, basis.getDimension("blah"));
        assertTrue(basis.isReadOnly());

        basis.setReadOnly(false);
        assertEquals(0, basis.getDimension("cat"));
        assertEquals(1, basis.getDimension("c"));
        assertEquals(2, basis.getDimension("at"));
        assertEquals(3, basis.getDimension("blah"));
        assertFalse(basis.isReadOnly());
    }

    @Test public void testGetDimensionDescription() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("ca");
        basis.getDimension("dog");

        assertEquals("cat", basis.getDimensionDescription(0));
        assertEquals("ca", basis.getDimensionDescription(1));
        assertEquals("dog", basis.getDimensionDescription(2));
    }
}
