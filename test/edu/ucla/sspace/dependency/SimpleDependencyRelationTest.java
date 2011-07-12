package edu.ucla.sspace.dependency;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SimpleDependencyRelationTest {

    @Test public void testGetters() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyRelation rel =
            new SimpleDependencyRelation(node1, "c", node2);

        assertEquals(node1, rel.headNode());
        assertEquals(node2, rel.dependentNode());
        assertEquals("c", rel.relation());
    }

    @Test public void testEquals() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node1, "c", node2);
        assertEquals(rel1, rel2);
    }

    @Test public void testNotEqualsRelation() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node1, "b", node2);
        assertFalse(rel1.equals(rel2));
    }

    @Test public void testNotEqualsNode2() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyTreeNode node3 = new SimpleDependencyTreeNode("dog", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node1, "c", node3);

        assertFalse(rel1.equals(rel2));
    }

    @Test public void testNotEqualsNode1() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyTreeNode node3 = new SimpleDependencyTreeNode("dog", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node3, "c", node2);

        assertFalse(rel1.equals(rel2));
    }
}

