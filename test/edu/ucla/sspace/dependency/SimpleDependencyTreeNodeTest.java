package edu.ucla.sspace.dependency;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SimpleDependencyTreeNodeTest {

    @Test public void testGettersNoLemma() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");

        assertEquals("cat", node1.word());
        assertEquals("n", node1.pos());
        assertEquals("cat", node1.lemma());
    }

    @Test public void testGetters() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");

        assertEquals("cat", node1.word());
        assertEquals("n", node1.pos());
        assertEquals("c", node1.lemma());
    }

    @Test public void testAddNeighbor() {
        SimpleDependencyTreeNode node1 = new SimpleDependencyTreeNode(
                "cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("dog","n","c");
        DependencyRelation rel =
            new SimpleDependencyRelation(node1, "c", node2);
        node1.addNeighbor(rel);

        List<DependencyRelation> relations = node1.neighbors();
        assertEquals(1, relations.size());
        assertEquals(rel, relations.get(0));
    }

    @Test public void testNotEqualsWord() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("dog","n","c");
        assertFalse(node1.equals(node2));
    }

    @Test public void testNotEqualsPos() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","v","c");
        assertFalse(node1.equals(node2));
    }

    @Test public void testNotEqualsLemma() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","n","t");
        assertFalse(node1.equals(node2));
    }

    @Test public void testEquals() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","n","c");
        assertEquals(node1, node2);
    }

    @Test public void testEqualsWithRelation() {
        SimpleDependencyTreeNode node1 = new SimpleDependencyTreeNode(
                "cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyRelation rel =
            new SimpleDependencyRelation(node1, "c", node2);
        node1.addNeighbor(rel);

        assertEquals(node1, node2);
    }
}
