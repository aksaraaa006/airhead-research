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


/**
 * An interface for reporting the results of a {@link Wordsi} run.  Reports can
 * take several forms, such as the number of times each word was assigned to
 * each cluster or  something similar to the SenseEval/SemEval word sense
 * induction output, which requires a cluster labeling for each word context
 * generated.
 *
 * @author Keith Stevens
 */
public interface AssignmentReporter {

  /**
   * Updates the assignment report with the knowledge that {@code primaryKey}
   * and {@code secondaryKey} were associated with {@code clusterId} once.
   */
  void updateAssignment(String primaryKey, String secondaryKey, int clusterId);

  /**
   * Finalizes the assignment report.  Any output streams must be closed and any
   * unreported data must be reported.
   */
  void finalizeReport();
}
