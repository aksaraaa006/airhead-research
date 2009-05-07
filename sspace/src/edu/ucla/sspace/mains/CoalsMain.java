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

import java.io.IOException;

import java.util.Properties;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.coals.Coals;

/**
 * An executable class for running {@link Coals} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 * <li> document sources (must provide one)
 *   <ul>
 *
 *   <li> {@code --docFile=<filename>} a file where each line is treated as a separate
 *        document.  This is the preferred option for LSA operations for large
 *        numbers of documents due to reduced I/O demands.
 *
 *   </ul>
 *
 * <li> {@code --dimensions=<int>} how many dimensions to use for the LSA vectors.
 *      See {@link Coals} for default value
 *
 * <li> {@code --threads=<int>} how many threads to use when processing the
 *      documents.  The default is one per core.
 * 
 * <li> {@code --overwrite=<boolean>} specifies whether to overwrite the
 *      existing output files.  The default is {@code true}.  If set to {@code
 *      false}, a unique integer is inserted into the file name.
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output
 */
public class CoalsMain extends GenericMain {
    private CoalsMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
      options.addOption('n', "dimensions", 
                        "the number of dimensions in the semantic space",
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
    
    public SemanticSpace getSpace() {
      return new Coals();
    }

    public Properties setupProperties() {
      Properties props = System.getProperties();
      if (argOptions.hasOption("dimensions")) {
        props.setProperty(Coals.REDUCE_MATRIX_DIMENSION_PROPERTY,
                          argOptions.getStringOption("dimensions"));
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
