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
import java.io.FileReader;
import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpaceUtils;

import edu.ucla.sspace.common.document.Document;
import edu.ucla.sspace.common.document.FileListDocumentIterator;
import edu.ucla.sspace.common.document.OneLinePerDocumentIterator;

import edu.ucla.sspace.lsa.LatentSemanticAnalysis;
import edu.ucla.sspace.lsa.MatrixTransformer;

/**
 * An executable class for running {@link LatentSemanticAnalysis} (LSA) from the
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
 * <li> {@code --dimensions=<int>} how many dimensions to use for the LSA vectors.
 *      See {@link LatentSemanticAnalysis} for default value
 *
 * <li> {@code --threads=<int>} how many threads to use when processing the
 *      documents.  The default is one per core.
 * 
 * <li> {@code --preprocess=<class name>} specifies an instance of {@link
 *      MatrixTransformer} to use in preprocessing the word-document matrix
 *      compiled by LSA prior to computing the SVD.  See {@link
 *      LatentSemanticAnalysis} for default value
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
 * lsa-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code lsa-semantic-space<number>.sspace} will be
 * created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core.
 *
 * @see LatentSemanticAnalysis
 * @see MatrixTransformer
 */
public class LSAMain {

    public static final String LSA_SEMANTIC_SPACE_FILE_NAME =
	"lsa-semantic-space.sspace";

    /**
     * internal flag for printing verbose information to stdout.
     */
    private boolean verbose;

    private final ArgOptions argOptions;
    
    private LSAMain() {
	argOptions = new ArgOptions();
	addOptions();
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    private void addOptions() {
	argOptions.addOption('l', "fileList", "a list of document files", 
			     true, "file name", "Required (at least one of)");
	argOptions.addOption('d', "docFile", 
			     "a file where each line is a document", true,
			     "file name", "Required (at least one of)");

	argOptions.addOption('n', "dimensions", 
			     "the number of dimensions in the semantic space",
			     true, "int"); 
	argOptions.addOption('t', "threads", "the number of threads to use",
			     true, "int");
	argOptions.addOption('p', "preprocess", "a MatrixTransform class to "
			     + "use for preprocessing", true, "class name");
	argOptions.addOption('w', "overwrite", "specifies whether to " +
			     "overwrite the existing output", true, "boolean");

	argOptions.addOption('v', "verbose", "prints verbose output");
    }

    public static void main(String[] args) {
		
	LSAMain lsa = new LSAMain();

	if (args.length == 0) {
	    lsa.usage();
	    return;
	}

	try {
	    lsa.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private void run(String[] args) throws Exception {
	
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

	LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
	
	verbose = argOptions.hasOption("v") || argOptions.hasOption("verbose");

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
	
	int numThreads = Runtime.getRuntime().availableProcessors();
	if (argOptions.hasOption("threads")) {
	    numThreads = argOptions.getIntOption("threads");
	}

	boolean overwrite = true;
	if (argOptions.hasOption("overwrite")) {
	    overwrite = argOptions.getBooleanOption("overwrite");
	}
	
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();

	if (argOptions.hasOption("dimensions")) {
	    props.setProperty(LatentSemanticAnalysis.LSA_DIMENSIONS_PROPERTY,
			      argOptions.getStringOption("dimensions"));
	}

	if (argOptions.hasOption("preprocess")) {
	    props.setProperty(LatentSemanticAnalysis.MATRIX_TRANSFORM_PROPERTY,
			      argOptions.getStringOption("preprocess"));
	}

	parseDocumentsMultiThreaded(lsa, docIter, props, numThreads);

	lsa.processSpace(props);
	
	File output = (overwrite)
	    ? new File(outputDir, LSA_SEMANTIC_SPACE_FILE_NAME)
	    : File.createTempFile("lsa-semantic-space", "sspace", outputDir);

	SemanticSpaceUtils.printSemanticSpace(lsa, output);
    }



     /**
      *
      */
    private void parseDocumentsMultiThreaded(final LatentSemanticAnalysis lsa,
					     final Iterator<Document> docIter, 
					     final Properties properties,
					     int numThreads)	
	    throws IOException, InterruptedException {

	Collection<Thread> threads = new LinkedList<Thread>();

	final AtomicInteger count = new AtomicInteger(0);

	
	for (int i = 0; i < numThreads; ++i) {
	    Thread t = new Thread() {
		    public void run() {
			// repeatedly try to process documents while some still
			// remain
			while (docIter.hasNext()) {
			    long startTime = System.currentTimeMillis();
			    Document doc = docIter.next();
			    int docNumber = count.incrementAndGet();
			    int terms = 0;
			    try {
				lsa.processDocument(doc.reader());
			    } catch (Throwable t) {
				t.printStackTrace();
			    }
			    long endTime = System.currentTimeMillis();
			    verbose("parsed document #%d in %.3f seconds%n",
				    docNumber, ((endTime - startTime) / 1000d));
			}
		    }
		};
	    threads.add(t);
	}

	long threadStart = System.currentTimeMillis();
	
	// start all the threads processing
	for (Thread t : threads)
	    t.start();

	verbose("Beginning processing using %d threads", numThreads);

	// wait until all the documents have been parsed
	for (Thread t : threads)
	    t.join();

	verbose("parsed %d document in %.3f total seconds)%n",
		count.get(),
		((System.currentTimeMillis() - threadStart) / 1000d));
    }



    /**
     * Returns a set of terms based on the contents of the provided file.  Each
     * word is expected to be on its own line.
     */
    private static Set<String> loadValidTermSet(String validTermsFileName) {
	Set<String> validTerms = new HashSet<String>();
	try {
	    BufferedReader br = new BufferedReader(
		new FileReader(validTermsFileName));
	    String line = null;
	    while ((line = br.readLine()) != null) {
		validTerms.add(line);
	    }
	    br.close();
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	return validTerms;
    }

    private void verbose(String msg) {
	if (verbose) {
	    System.out.println(msg);
	}
    }

    private void verbose(String format, Object... args) {
	if (verbose) {
	    System.out.printf(format, args);
	}
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    private void usage() {
 	System.out.println(
 	    "usage: java LSAMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}
