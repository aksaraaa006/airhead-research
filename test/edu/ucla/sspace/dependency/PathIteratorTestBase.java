package edu.ucla.sspace.dependency;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PathIteratorTestBase {

    public static DependencyTreeNode[] makeTree(String[][] treeData,
                                                int[][] treeLinks) {
        SimpleDependencyTreeNode[] tree =
            new SimpleDependencyTreeNode[treeData.length];
        for (int i = 0; i < tree.length; ++i)
            tree[i] = new SimpleDependencyTreeNode(
                    treeData[i][0], treeData[i][1]);

        for (int i = 0; i < tree.length; ++i)
            for (int n : treeLinks[i])
                tree[i].addNeighbor(new SimpleDependencyRelation(
                            tree[i], treeData[i][2], tree[n]));
        return tree;
    }

    public static void testPath(DependencyPath path,
                                int expectedLength,
                                String expectedRelation,
                                String expectedFirst,
                                String expectedLast) {
        assertEquals(expectedLength, path.length());
        assertEquals(expectedRelation, path.getRelation(0));
        assertEquals(expectedFirst, path.first().word());
        assertEquals(expectedLast, path.last().word());
    }
}

