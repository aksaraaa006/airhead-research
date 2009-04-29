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

package edu.ucla.sspace.tools;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

import java.util.Set;

import org.junit.*;

import static org.junit.Assert.*;

public class FileBasedSemanticSpaceTest {

  /**
   * Test that {@link edu.ucla.sspace.tools.FileBasedSemanticSpace} can read in
   * a properly formatted text file, and be capable of returning the expected
   * wordset, along with the vectors for each word.
   */
  @Test public void testFileSpace() throws IOException {
    String[] words = {"the", "cat", "loves", "cheezburgers"};
    double[][] values = {{5, 4, 3, 2, 1},
                         {1, 2, 3, 4, 5},
                         {1, 1, 1, 1, 1},
                         {1.0, 2.0, 3.0, 4.0, 6.0}};
    // First build the test file.
    File testSpaceData = File.createTempFile("test-sspace-matrix", "dat");
    PrintWriter pw = new PrintWriter(testSpaceData);
    pw.println("4 5");
    for (int i = 0; i < 4; ++i) {
      StringBuffer sb = new StringBuffer(words[i]);
      sb.append("|");
      for (int j = 0; j < 5; ++j) {
        sb.append(values[i][j]).append(" ");
      }
      pw.println(sb.toString());
    }
    pw.close();
    FileBasedSemanticSpace testSpace =
      new FileBasedSemanticSpace(testSpaceData.getAbsolutePath());
    // Check the word set provided by the SemanticSpace, and the vectors
    // returned for each word.
    Set<String> spaceWords = testSpace.getWords();
    assertEquals(words.length, spaceWords.size());
    for (int i = 0; i < words.length; ++i) {
      assertTrue(spaceWords.contains(words[i]));
      double[] spaceVector = testSpace.getVectorFor(words[i]);
      assertTrue(spaceVector != null);
      for (int j = 0; j < 5; ++j) {
        assertEquals(values[i][j], spaceVector[j], .0001);
      }
    }
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(FileBasedSemanticSpaceTest.class);
  }
}
