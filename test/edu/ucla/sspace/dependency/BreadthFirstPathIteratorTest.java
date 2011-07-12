package edu.ucla.sspace.dependency;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class BreadthFirstPathIteratorTest extends PathIteratorTestBase {

    @Test public void testIterator() {
        String[][] treeData = {
            {"cat", "n", "1"},
            {"is", "det", "obj"},
            {"dog", "n", "2"},
            {"and", "conj", "blah"},
            {"chicken", "n", "3"}
        };
        int[][] treeLinks = {
            {},
            {0, 2, 4},
            {3},
            {4},
            {}
        };
        DependencyTreeNode[] tree = makeTree(treeData, treeLinks);
        Iterator<DependencyPath> pathIter = new BreadthFirstPathIterator(
                tree[1]);

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "cat");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "dog");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "chicken");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "and");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 3, "obj", "is", "chicken");

        assertFalse(pathIter.hasNext());
    }
}
