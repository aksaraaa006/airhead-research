package edu.ucla.sspace.evaluation;

import org.junit.*;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
;
public class WordSimilarityEvalTest {
  @Test public void handleEmptyFileTest() {
    try {
      File tempFile = File.createTempFile(".wsetTest", ".tst", null);
      BufferedWriter writer =
        new BufferedWriter(new FileWriter(tempFile));
      writer.flush();
      TestSemanticSpace space = new TestSemanticSpace();
      assertEquals(0, WordSimilarityEval.evalSemanticSpace(
            space, tempFile.getAbsolutePath(), space.getSimilarityMethod()), .0001);
    } catch (IOException e) {
    }
  }

  @Test public void basicInputTest() {
    /*
     * TODO: Figure out how to best test this class
    try {
      File tempFile = File.createTempFile(".wsetTest", ".tst", null);
      BufferedWriter writer =
        new BufferedWriter(new FileWriter(tempFile));
      writer.write("w1 w2 1.0\n");
      writer.write("w1 w2 1.0");
      writer.flush();
      TestSemanticSpace space = new TestSemanticSpace();
      space.addVector(new double[]{9,6});
      space.addVector(new double[]{1,0});
      space.addVector(new double[]{2,1});
      space.addVector(new double[]{1,0});
      assertEquals(100, WordSimilarityEval.evalSemanticSpace(
            space, tempFile.getAbsolutePath(), space.getSimilarityMethod()), .0001);
    } catch (IOException e) {
    }
    */
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(WordSimilarityEvalTest.class);
  }
}
