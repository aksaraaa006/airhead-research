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

import edu.ucla.sspace.holograph.Holograph;
import edu.ucla.sspace.holograph.BeagleIndexBuilder;

import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link Holograph} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 * <li> document sources (must provide one)
 *   <ul>
 *
 *   <li> {@code --fileList=<filename>} a file containing a list of file names, each of
 *        which is treated as a separate document.

 *   <li> {@code --docFile=<filename>} a file where each line is treated as a separate
 *        document.  This is the preferred option for LSA operations for large
 *        numbers of documents due to reduced I/O demands.
 *
 *   </ul>
 *
 * <li> {@code --dimensions=<int>} how many dimensions to use for the Holograph vectors.
 *      512 is the default value.
 *
 * <li> {@code --threads=<int>} how many threads to use when processing the
 *      documents.  The default is one per core.
 * 
 * <li> {@code --overwrite=<boolean>} specifies whether to overwrite the
 *      existing output files.  The default is {@code true}.  If set to {@code
 *      false}, a unique integer is inserted into the file name.
 *
 * <li> {@code --verbose | -v} specifies whether to print runtime
 *      information to standard out
 *
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * holograph-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code holograph-semantic-space<number>.sspace} will be
 * created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see Holograph
 *
 * @author Keith Stevens 
 */
public class HolographMain extends GenericMain {

    public static final int DEFAULT_DIMENSION = 512;
    private int dimension;

    public static final String HOLOGRAPH_SEMANTIC_SPACE_FILE_NAME =
      "holograph-semantic-space";

    private HolographMain() {
      super(HOLOGRAPH_SEMANTIC_SPACE_FILE_NAME);
    }

    public static void main(String[] args) {
	HolographMain hMain = new HolographMain();
	try {
	    hMain.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
      options.addOption('n', "dimension",
                        "the length of each holographic vector",
                        true, "INT");
    }

    public void handleExtraOptions() {
      dimension = (argOptions.hasOption("dimension"))
        ? argOptions.getIntOption("dimension")
        : DEFAULT_DIMENSION;
    }

    public SemanticSpace getSpace() {
      return new Holograph(new BeagleIndexBuilder(dimension), dimension);
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java HolographMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}
