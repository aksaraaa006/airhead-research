/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.wordsi;

import java.io.OutputStream;
import java.io.PrintStream;


/**
 * A {@link AssignmentReporter} that creates a SenseEval or SemEval answer key.
 * This should be used in conjunction with a {@link SenseEvalContextExtractor}.
 * When reporting, Primary keys are not used.  Secondary keys are expected to be
 * the instance identifier.  For each call to {@code updateAssignment}, the
 * reporter will generate a line of the form
 *   word.pos word.pos.idNumber word.pos.clusterNumber
 * which designates word.pos.idNumber is the secondary key.  The line signifies
 * that the given word instance was assigned to a cluster identified by
 * clusterNumber.
 *
 * @author Keith Stevens
 */
public class SenseEvalReporter implements AssignmentReporter {

  /**
   * The writer used to output the SenseEval answer key.
   */
  private PrintStream writer;

  /**
   * Creates a new {@link SenseEvalReporter}.
   *
   * @param stream The stream to which the answer key should be written.
   */
  public SenseEvalReporter(OutputStream stream) {
    writer = new PrintStream(stream);
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void updateAssignment(String primaryKey, 
                                            String secondaryKey,
                                            int clusterId) {
    // Get the word and part of speech information.
    int splitIndex = secondaryKey.lastIndexOf(".");
    String wordPos = secondaryKey.substring(0, splitIndex);

    // Print out the answer key.
    writer.printf("%s %s %s.%d\n", wordPos, secondaryKey, wordPos, clusterId);
  }

  /**
   * {@inheritDoc}
   */
  public void finalizeReport() {
    writer.close();
  }
}

