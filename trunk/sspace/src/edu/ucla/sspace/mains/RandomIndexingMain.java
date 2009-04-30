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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpaceUtils;

import edu.ucla.sspace.common.document.Document;
import edu.ucla.sspace.common.document.FileListDocumentIterator;
import edu.ucla.sspace.common.document.OneLinePerDocumentIterator;

import edu.ucla.sspace.ri.RandomIndexing;

/**
 * An executable class for running {@link RandomIndexing} from the command line.
 * This class provides several options:
 *
 * <ul>
 *
 * <li><u>Required (at least one of)</u>:
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile} a file where each line is a document
 *
 *   <li> {@code -f}, {@code --fileList} a list of document files
 *   </ul>
 *
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *   
 *   <li>  {@code -l}, {@code --vectorLength}  length of semantic vectors

 *   <li> {@code -n}, {@code --permutationFunction} permutation function to use

 *   <li> {@code -p}, {@code --usePermutations} whether to permute index vectors
 *        based on word order

 *   <li> {@code -s}, {@code --windowSize} how many words to consider in each
 *        direction
 *   </ul> 
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *   <li> {@code -t}, {@code --threads} the number of threads to use
 *
 *   <li> {@code -v}, {@code --verbose} prints verbose output
 *
 *   <li> {@code -w}, {@code --overwrite} specifies whether to overwrite the
 *        existing output
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


    public static final String RANDOM_INDEXING_SPACE_FILE_NAME =
	"random-indexing.sspace";

    private final ArgOptions argOptions;
    
    private RandomIndexingMain() {
	argOptions = new ArgOptions();
	addOptions();
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    private void addOptions() {

	argOptions.addOption('p', "usePermutations", "whether to permute " +
			     "index vectors based on word order", true,
			     "boolean", "Algorithm Options");
	argOptions.addOption('l', "vectorLength", "length of semantic vectors",
			     true, "int", "Algorithm Options");
	argOptions.addOption('s', "windowSize", "how many words to consider " +
			     "in each direction", true,
			     "int", "Algorithm Options");
	argOptions.addOption('n', "permutationFunction", "permutation function "
			     + "to use", true,
			     "PermutationFunction class", "Algorithm Options");


	argOptions.addOption('f', "fileList", "a list of document files", 
			     true, "file name", "Required (at least one of)");
	argOptions.addOption('d', "docFile", 
			     "a file where each line is a document", true,
			     "file name", "Required (at least one of)");

	argOptions.addOption('t', "threads", "the number of threads to use",
			     true, "int", "Program Options");
	argOptions.addOption('w', "overwrite", "specifies whether to " +
			     "overwrite the existing output", true, "boolean",
			     "Program Options");
	argOptions.addOption('v', "verbose", "prints verbose output", false, 
			     null, "Program Options");
    }



    public static void main(String[] args) {
	try {
	    RandomIndexingMain main = new RandomIndexingMain();
	    if (args.length == 0) {
		main.usage();
		return;
	    }

	    main.run(args);

	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    public void run(String[] args) throws Exception {

	// process command line args
	argOptions.parseOptions(args);

	if (argOptions.numPositionalArgs() == 0) {
	    throw new IllegalArgumentException("must specify output directory");
	}

	File outputDir = new File(argOptions.getPositionalArg(0));
	if (!outputDir.isDirectory()){
	    throw new IllegalArgumentException(
		"output directory is not a directory: " + outputDir);
	}

	// Second, determine where the document input sources will be coming
	// from.
	Iterator<Document> docIter = null;
	String fileList = (argOptions.hasOption("fileList"))
	    ? argOptions.getStringOption("fileList")
	    : null;

	String docFile = (argOptions.hasOption("docFile"))
	    ? argOptions.getStringOption("docFile")
	    : null;
	if (fileList == null && docFile == null) {
	    throw new Error("must specify document sources");
	}
	else if (fileList != null && docFile != null) {
	    throw new Error("cannot specify both docFile and fileList");
	}
	else if (fileList != null) {
	    // we have a file that contains the list of all document files we
	    // are to process
	    docIter =  new FileListDocumentIterator(fileList);
	}
	else {
	    // all the documents are listed in one file, with one document per
	    // line
	    docIter = new OneLinePerDocumentIterator(docFile);
	}

	Properties props = System.getProperties();

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


	// Load the program-specific options next.
	int numThreads = Runtime.getRuntime().availableProcessors();
	if (argOptions.hasOption("threads")) {
	    numThreads = argOptions.getIntOption("threads");
	}

	boolean overwrite = true;
	if (argOptions.hasOption("overwrite")) {
	    overwrite = argOptions.getBooleanOption("overwrite");
	}

	verbose = argOptions.hasOption("v") || argOptions.hasOption("verbose");
	
	// Once all the optional properties are known and set, create the
	// RandomIndexing algorithm using them
	RandomIndexing ri = new RandomIndexing(props);
	
	// process all of the documents
	parseDocumentsMultiThreaded(ri, docIter, props, numThreads);

	File output = (overwrite)
	    ? new File(outputDir, RANDOM_INDEXING_SPACE_FILE_NAME)
	    : File.createTempFile("random-indexing", "sspace",
				  outputDir);

	SemanticSpaceUtils.printSemanticSpace(ri, output);

    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    private void usage() {
 	System.out.println(
 	    "usage: java RandomIndexingMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }

}