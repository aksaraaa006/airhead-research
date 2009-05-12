package edu.ucla.sspace.common.matrix;

import edu.ucla.sspace.common.Matrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SparseMatrixTest {
  @Test public void testReplaceWithZero() {
    double[][] testData = {{0, 0, 0, 1},
                           {1, 0, 2, 0},
                           {0, 0, 0, 0},
                           {1, 2, 4, 5},
                           {0, 0, 1, 0}};
    SparseMatrix testMatrix = new SparseMatrix(5, 4);
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        testMatrix.set(i, j, testData[i][j]);
      }
    }
    testData[1][1] = 4;
    testMatrix.set(1, 1, 4);
    testData[1][0] = 0;
    testMatrix.set(1, 0, 0);
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        assertEquals(testData[i][j], testMatrix.get(i, j), .0001);
      }
    }
  }

  @Test public void testMatrix() {
    double[][] testData = {{0, 0, 0, 1},
                           {1, 0, 2, 0},
                           {0, 0, 0, 0},
                           {1, 2, 4, 5},
                           {0, 0, 1, 0}};
    SparseMatrix testMatrix = new SparseMatrix(5, 4);
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        testMatrix.set(i, j, testData[i][j]);
      }
    }
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        assertEquals(testData[i][j], testMatrix.get(i, j), .0001);
      }
    }
  }
}
