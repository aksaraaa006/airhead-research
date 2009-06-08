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
import edu.ucla.sspace.common.IndexBuilder;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.hermit.Hermit;

import edu.ucla.sspace.holograph.BeagleIndexBuilder;
import edu.ucla.sspace.holograph.RandomIndexBuilder;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link Hermit} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 *
 * <li> {@code --dimensions=<int>} how many dimensions to use for the LSA vectors.
 *      See {@link Hermit} for default value
 * 
 * <li> {@code --preprocess=<class name>} specifies an instance of {@link
 *      edu.ucla.sspace.lsa.MatrixTransformer} to use in preprocessing the
 *      word-document matrix compiled by LSA prior to computing the SVD.  See
 *      {@link Hermit} for default value
 *
 * <li> {@code --holographsize} length of the holograph vectors used in
 *      conjuction with the lsa term-document matrix.  by default 2048.
 *
 * <li> {@code --builder} class name of the
 *      {@link edu.ucla.sspace.common.IndexBuilder} used to compose the
 *      holograph vectors.  Currently only accepts "BeagleIndexBuilder" and
 *      "RandomIndexBuilder".
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * hermit-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code hermit-semantic-space<number>.sspace} will be
 * created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see Hermit
 * @see edu.ucla.sspace.lsa.MatrixTransformer MatrixTransformer
 *
 * @author Keith Stevens 
 */
public class HermitMain extends GenericMain {
    private static final int DEFAULT_DIMENSION = 2048;
    private int dimension;
    private IndexBuilder builder;

    private HermitMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
	options.addOption('n', "dimensions", 
			     "the number of dimensions in the semantic space",
			     true, "INT"); 
	options.addOption('p', "preprocess", "a MatrixTransform class to "
			     + "use for preprocessing", true, "CLASSNAME");
    options.addOption('h', "holographsize", "The size of the holograph vectors",
                      true, "INT");
    options.addOption('b', "builder", "Index builder to use for hermit",
                      true, "CLASSNAME", "Process Properties");
    }

    public static void main(String[] args) {
	HermitMain hermit = new HermitMain();
	try {
	    hermit.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    public void handleExtraOptions() {
      dimension = (argOptions.hasOption("holographsize"))
        ? argOptions.getIntOption("holographsize")
        : DEFAULT_DIMENSION;
      String builderType = (argOptions.hasOption("builder"))
        ? argOptions.getStringOption("builder")
        : "BeagleIndexBuilder";
      if (builderType.equals("BeagleIndexBuilder"))
        builder = new BeagleIndexBuilder(dimension);
      else if (builderType.equals("RandomIndexBuilder"))
         builder = new RandomIndexBuilder(dimension);
    }

    public SemanticSpace getSpace() {
      try {
        return new Hermit(builder, dimension);
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }

    public Properties setupProperties() {
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();

	if (argOptions.hasOption("dimensions")) {
	  props.setProperty(Hermit.LSA_DIMENSIONS_PROPERTY,
          argOptions.getStringOption("dimensions"));
	}

	if (argOptions.hasOption("preprocess")) {
	  props.setProperty(Hermit.MATRIX_TRANSFORM_PROPERTY,
          argOptions.getStringOption("preprocess"));
	}
    if (argOptions.hasOption("threads")) {
      props.setProperty(Hermit.NUM_THREADS_PROPERTY,
          argOptions.getStringOption("threads"));
    }
    return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java HermitMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}
