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

import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.common.matrix.SparseMatrix;
import edu.ucla.sspace.common.matrix.OnDiskMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * A {@link edu.ucla.sspace.common.SemanticSpace} which performs no processing
 * of documents, but simply reads
 * in a text file produced by another semantic space and converts this into a
 * {@link edu.ucla.sspace.common.matrix.SparseMatrix}.  The input format of the
 * file should be that which is produced by
 * @link{edu.ucla.sspace.common.SemanticSpaceUtils}.
 */
public class FileBasedSemanticSpace implements SemanticSpace {
  /**
   * The {@link edu.ucla.sspace.common.matrix.SparseMatrix} which contains the
   * data read from a finished {@link edu.ucla.sspace.common.SemanticSpace}.
   */
  private final Matrix wordSpace;

  /**
   * A mapping of terms to row indexes.  Also serves as a quick means of
   * retrieving the words known by this
   * {@link edu.ucla.sspace.common.SemanticSpace}.
   */
  private final HashMap<String, Integer> termToIndex ;

  /**
   * Creates the {@link FileBasedSemanticSpace} from the file with the provided
   * name, which must be in the format produced by {@link
   * edu.ucla.sspace.common.SemanticSpaceUtils}.
   *
   * @param filename filename of the data intended be provided by this
   *   {@link edu.ucla.sspace.common.SemanticSpace}.
   */
  public FileBasedSemanticSpace(String filename) {
      this(new File(filename));
  }

  /**
   * Creates the {@link FileBasedSemanticSpace} from the provided file, which
   * must be in the format produced by {@link
   * edu.ucla.sspace.common.SemanticSpaceUtils}.
   *
   * @param file a file containing the data intended be provided by this {@link
   *   edu.ucla.sspace.common.SemanticSpace}.
   */
  public FileBasedSemanticSpace(File file) {
    termToIndex = new HashMap<String, Integer>();
    Matrix builtMatrix = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = br.readLine();
      if (line == null)
        throw new IOError(new Throwable("An empty file has been passed in"));
      String[] dimensions = line.split("\\s");
      int rows = Integer.parseInt(dimensions[0]);
      int columns = Integer.parseInt(dimensions[1]);
      int index = 0;
      builtMatrix = new OnDiskMatrix(rows, columns);
      while ((line = br.readLine()) != null) {
        String[] termVectorPair = line.split("\\|");
        String[] values = termVectorPair[1].split("\\s");
        termToIndex.put(termVectorPair[0], index);
        if (values.length != columns)
          throw new IOError(
              new Throwable("improperly formated semantic space file"));
        for (int c = 0; c < columns; ++c) {
          double d = Double.parseDouble(values[c]);
          if (d == 0.0)
            continue;
          builtMatrix.set(index, c, d);
        }
        index++;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(1);
    }  
    wordSpace = builtMatrix;
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return Collections.unmodifiableSet(termToIndex.keySet());
  }
  
  /**
   * {@inheritDoc}
   */
  public double[] getVectorFor(String term) {
    Integer index = termToIndex.get(term);
    return (index == null) ? null : wordSpace.getRow(index.intValue());
  }

  /**
   * A noop.
   */
  public void processDocument(BufferedReader document) {
  }

  /**
   * A noop.
   */
  public void processSpace(Properties props) {
  }
}
