/*
 * Copyright 2009 Keith Stevens 
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
