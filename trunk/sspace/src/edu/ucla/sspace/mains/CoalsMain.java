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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.coals.Coals;

import java.io.IOException;

import java.util.Properties;


/**
 * An executable class for running {@link Coals} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 * <li> {@code --dimensions=<int>} how many dimensions to use for the regular 
 *      word vectors. See {@link Coals} for a default value
 *
 * <li> {@code --reduce} If present, the word-word matrix will be reduced using
 *      Singular Valued Decomposition, otherwise no reduction will be performed.
 *
 * <li> {code --svddimension=<int>} size of the reduced svd vectors. See {@link
 *      Coals} for a default value.
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output where the file name will mark
 * the number of words kept in the word-word matrix, and wether or not SVD was
 * used.
 *
 * @see Coals
 */
public class CoalsMain extends GenericMain {

    private int dimensions; 

    private CoalsMain() {
    }

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
          options.addOption('n', "dimensions", 
                            "the number of dimensions in the semantic space",
                            true, "INT"); 
          options.addOption('s', "svddimension", 
                            "the number of dimensions in the reduced space, " +
                            "only used if --reduce is also given",
                            true, "INT"); 
          options.addOption('r', "reduce", 
                            "reduce the semantic space using SVD");
    }

    public static void main(String[] args) {
        CoalsMain coals = new CoalsMain();
        try {
            coals.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public SemanticSpace getSpace() {
        return (dimensions > 0) ? new Coals(dimensions) : new Coals();
    }

    /**
     * {@inheritDoc}
     */
    public void handleExtraOptions() {
        dimensions = argOptions.hasOption("dimensions")
            ? argOptions.getIntOption("dimensions")
            : 0;
    }

    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    /**
     * {@inheritDoc}
     */
    public Properties setupProperties() {
          Properties props = System.getProperties();
          if (argOptions.hasOption("svddimension")) {
                props.setProperty(Coals.REDUCE_MATRIX_DIMENSION_PROPERTY,
                                  argOptions.getStringOption("svddimension"));
          }

          if (argOptions.hasOption("reduce")) {
                props.setProperty(Coals.REDUCE_MATRIX_PROPERTY, "true");
          }
          return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
         System.out.println(
                 "usage: java CoalsMain [options] <output-dir>\n" + 
                 argOptions.prettyPrint());
    }
}
