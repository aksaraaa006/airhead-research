/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.ri.RandomIndexing;

import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link RandomIndexing} from the command line.
 * This class provides several options:
 *
 * <ul>
 *
 * <li><u>Required (at least one of)</u>:
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile=FILE[,FILE...]} a file where each line is
 *        a document
 *
 *   <li> {@code -f}, {@code --fileList=FILE[,FILE...]} a list of document files
 *   </ul>
 *
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *
 *   <li> {@code -i}, {@code --vectorGenerator=CLASSNAME} the {@link
 *        edu.ucla.sspace.ri.IndexVectorGenerator IndexVectorGenerator} class to
 *        use for generating the index functions.
 *   
 *   <li> {@code -l}, {@code --vectorLength=INT} length of semantic vectors
 *
 *   <li> {@code -n}, {@code --permutationFunction=CLASSNAME} the {@link
 *        edu.ucla.sspace.ri.PermutationFunction PermutationFunction} class to
 *        use for permuting index vectors, if permutation is enabled.
 *
 *   <li> {@code -p}, {@code --usePermutations=BOOL} whether to permute index
 *        vectors based on word order
 *
 *   <li> {@code -s}, {@code --windowSize=INT} how many words to consider in each
 *        direction
 *   </ul> 
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link SemanticSpaceUtils} for format details.
 *
 *   <li> {@code -t}, {@code --threads=INT} the number of threads to use
 *
 *   <li> {@code -v}, {@code --verbose} prints verbose output
 *
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output
 *   </ul>
 *
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code random-indexing.sspace}.
 * If {@code overwrite} was set to {@code true}, this file will be replaced for
 * each new semantic space.  Otherwise, a new output file of the format {@code
 * random-indexing<number>.sspace} will be created, where {@code
 * <number>} is a unique identifier for that program's invocation.  The output
 * file will be placed in the directory specified on the command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see RandomIndexing
 * 
 * @author David Jurgens
 */
public class RandomIndexingMain extends GenericMain {

    private Properties props;

    private RandomIndexingMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
	options.addOption('i', "vectorGenerator", "IndexVectorGenerator "
			  + "class to use", true,
			  "CLASSNAME", "Algorithm Options");
	options.addOption('l', "vectorLength", "length of semantic vectors",
			  true, "INT", "Algorithm Options");
	options.addOption('n', "permutationFunction", "permutation function "
			  + "to use", true,
			  "CLASSNAME", "Algorithm Options");
	options.addOption('p', "usePermutations", "whether to permute " +
			  "index vectors based on word order", true,
			  "BOOL", "Algorithm Options");
	options.addOption('s', "windowSize", "how many words to consider " +
			  "in each direction", true,
			  "INT", "Algorithm Options");
    }

    public static void main(String[] args) {
	try {
	    RandomIndexingMain main = new RandomIndexingMain();
	    main.run(args);
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    public Properties setupProperties() {
	props = System.getProperties();
	// Use the command line options to set the desired properites in the
	// constructor.  Use the system properties in case these properties were
	// set using -Dprop=<value>
	if (argOptions.hasOption("usePermutations")) {
	    props.setProperty(RandomIndexing.USE_PERMUTATIONS_PROPERTY,
			      argOptions.getStringOption("usePermutations"));
	}

	if (argOptions.hasOption("permutationFunction")) {
	    props.setProperty(RandomIndexing.PERMUTATION_FUNCTION_PROPERTY,
			     argOptions.getStringOption("permutationFunction"));
	}

	if (argOptions.hasOption("windowSize")) {
	    props.setProperty(RandomIndexing.WINDOW_SIZE_PROPERTY,
			     argOptions.getStringOption("windowSize"));
	}

	if (argOptions.hasOption("vectorLength")) {
	    props.setProperty(RandomIndexing.VECTOR_LENGTH_PROPERTY,
			     argOptions.getStringOption("vectorLength"));
	}
    return props;
    }

    public SemanticSpace getSpace() {
      // Once all the optional properties are known and set, create the
      // RandomIndexing algorithm using them
      return new RandomIndexing(props);
    }
	
    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java RandomIndexingMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}
