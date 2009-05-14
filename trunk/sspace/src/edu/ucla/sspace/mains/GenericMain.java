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
import edu.ucla.sspace.common.CombinedIterator;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceUtils;
import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;

import edu.ucla.sspace.common.document.Document;
import edu.ucla.sspace.common.document.FileListDocumentIterator;
import edu.ucla.sspace.common.document.OneLinePerDocumentIterator;

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


/**
 * A base class for running {@link SemanticSpace} algorithms.  All derived main
 * classes must implement the abstract functions.  Derived classes have the
 * option of adding more command line options, which can then be handled
 * independently by the derived class to build the SemanticSpace correctly, or
 * produce the Properties object required for processing the space.
 *
 * All mains which inherit from this class will automatically have the ability
 * to process the documents in parallel, and from a variety of file sources.
 * The provided command line arguments are as follows:
 *
 * <ul>
 * <li> <u>Document sources (must provide one)</u>
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile=FILE[,FILE...]}  a file containing a list
 *        of file names, each of which is treated as a separate document.
 *
 *   <li> {@code -f}, {@code --fileList=FILE[,FILE...]} a file where each line
 *        is treated as a separate document.  This is the preferred option when
 *        working with large corpora due to reduced I/O demands for multiple
 *        files.
 *
 *   </ul>
 *
 * <li> <u>Program Options</u>
 *
 *   <ul> 
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link SemanticSpaceUtils} for format details.
 *
 *   <li> {@code -t}, {@code --threads=INT} how many threads to use when processing the
 *        documents.  The default is one per core.
 * 
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output files.  The default is {@code true}.  If set to
 *        {@code false}, a unique integer is inserted into the file name.
 *
 *   <li> {@code -v}, {@code --verbose}  specifies whether to print runtime
 *        information to standard out
 *
 *   </ul>
 * </ul>
 *
 * @author David Jurgens
 */
public abstract class GenericMain {
    /**
     * Extension used for all saved semantic space files.
     */
    public static final String EXT = ".sspace";

    /**
     * Whether to emit messages to {@code stdout} when the {@code verbose}
     * methods are used.
     */
    protected boolean verbose;

    /**
     * The processed argument options available to the main classes.
     */
    protected final ArgOptions argOptions;

    public GenericMain() {
	argOptions = setupOptions();
	verbose = false;
    }

    /**
     * Returns the {@link SemanticSpace} that will be used for processing
     */
    abstract public SemanticSpace getSpace();

    /**
     * Prints out information on how to run the program to {@code stdout}.
     */
    abstract public void usage();

    /**
     * Adds options to the provided {@code ArgOptions} instance, which will be
     * used to parse the command line.  This method allows subclasses the
     * ability to add extra command line options.
     *
     * @param options the ArgOptions object which more main specific options can
     *        be added to.
     *
     * @see #handleExtraOptions()
     */
    protected void addExtraOptions(ArgOptions options) { }

    /**
     * Once the command line has been parsed, allows the subclasses to perform
     * additional steps based on class-specific options.  This method will be
     * called before {@link #getSpace() getSpace}.
     *
     * @see #addExtraOptions(ArgOptions)
     */
    protected void handleExtraOptions() { }

    /**
     * Returns the {@code Properties} object that will be used when calling
     * {@link SemanticSpace#processSpace(Properties)}.  Subclasses should
     * override this method if they need to specify additional properties for
     * the space.  This method will be called once before {@link #getSpace()}.
     *
     * @return the {@code Properties} used for processing the semantic space.
     */
    protected Properties setupProperties() {
	Properties props = System.getProperties();
	return props;
    }

    /**
     * Adds the default options for running semantic space algorithms from the
     * command line.  Subclasses should override this method and return a
     * different instance if the default options need to be different.
     */
    protected ArgOptions setupOptions() {
	ArgOptions options = new ArgOptions();
	options.addOption('f', "fileList", "a list of document files", 
			  true, "FILE[,FILE...]", "Required (at least one of)");
	options.addOption('d', "docFile", 
			  "a file where each line is a document", true,
			  "FILE[,FILE...]", "Required (at least one of)");

	options.addOption('o', "outputFormat", "the .sspace format to use",
			  true, "{text|binary}", "Program Options");
	options.addOption('t', "threads", "the number of threads to use",
			  true, "INT", "Program Options");
	options.addOption('w', "overwrite", "specifies whether to " +
			  "overwrite the existing output", true, "BOOL",
			  "Program Options");
	options.addOption('v', "verbose", "prints verbose output",
			  false, null, "Program Options");
	addExtraOptions(options);
	return options;
    }

    /**
     * Processes the arguments and begins processing the documents using the
     * {@link SemanticSpace} returned by {@link #getSpace() getSpace}.
     *
     * @param args arguments used to configure this program and the {@code
     *        SemanticSpace}
     */
    public void run(String[] args) throws Exception {
	if (args.length == 0) {
	    usage();
	    System.exit(1);
	}
	argOptions.parseOptions(args);
	
	if (argOptions.numPositionalArgs() == 0) {
	    throw new IllegalArgumentException("must specify output directory");
	}

	File outputDir = new File(argOptions.getPositionalArg(0));
	if (!outputDir.isDirectory()){
	    throw new IllegalArgumentException(
		"output directory is not a directory: " + outputDir);
	}

	verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");

	// all the documents are listed in one file, with one document per line
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

	// Second, determine where the document input sources will be coming
	// from.
	Collection<Iterator<Document>> docIters = 
	    new LinkedList<Iterator<Document>>();

	if (fileList != null) {
	    String[] fileNames = fileList.split(",");
	    // we have a file that contains the list of all document files we
	    // are to process
	    for (String s : fileNames) {
		docIters.add(new FileListDocumentIterator(s));
	    }
	}
	if (docFile != null) {
	    String[] fileNames = docFile.split(",");
	    // all the documents are listed in one file, with one document per
	    // line
	    for (String s : fileNames) {
		docIters.add(new OneLinePerDocumentIterator(s));
	    }
	}

	// combine all of the document iterators into one iterator.
	docIter = new CombinedIterator<Document>(docIters);
	
	int numThreads = Runtime.getRuntime().availableProcessors();
	if (argOptions.hasOption("threads")) {
	    numThreads = argOptions.getIntOption("threads");
	}

	boolean overwrite = true;
	if (argOptions.hasOption("overwrite")) {
	    overwrite = argOptions.getBooleanOption("overwrite");
	}
	
	handleExtraOptions();

	Properties props = setupProperties();
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.

	SemanticSpace space = getSpace(); 
	
	parseDocumentsMultiThreaded(space, docIter, numThreads);

	long startTime = System.currentTimeMillis();
	space.processSpace(props);
	long endTime = System.currentTimeMillis();
	verbose("processed space in %.3f seconds%n",
		((endTime - startTime) / 1000d));
	
	File output = (overwrite)
	    ? new File(outputDir, space.getSpaceName() + EXT)
	    : File.createTempFile(space.getSpaceName(), EXT, outputDir);

	SSpaceFormat format = (argOptions.hasOption("outputFormat"))
	    ? SSpaceFormat.valueOf(
	        argOptions.getStringOption("outputFormat").toUpperCase())
	    : SSpaceFormat.TEXT;

	startTime = System.currentTimeMillis();
	SemanticSpaceUtils.printSemanticSpace(space, output, format);
	endTime = System.currentTimeMillis();
	verbose("printed space in %.3f seconds%n",
		((endTime - startTime) / 1000d));
    }

    /**
     * Calls {@link SemanticSpace#processDocument(BufferedReader)
     * processDocument} once for every document in {@code docIter} using a
     * single thread to interact with the {@code SemanticSpace} instance.
     *
     * @param sspace the space to build
     * @param docIter an iterator over all the documents to process
     */
    protected void parseDocumentsSingleThreaded(SemanticSpace sspace,
						Iterator<Document> docIter)
	throws IOException {

	long processStart = System.currentTimeMillis();
	int count = 0;

	while (docIter.hasNext()) {
	    long startTime = System.currentTimeMillis();
	    Document doc = docIter.next();
	    int docNumber = ++count;
	    int terms = 0;
	    sspace.processDocument(doc.reader());
	    long endTime = System.currentTimeMillis();
	    verbose("processed document #%d in %.3f seconds%n",
		    docNumber, ((endTime - startTime) / 1000d));
	}

	verbose("processed %d document in %.3f total seconds)%n",
		count,
		((System.currentTimeMillis() - processStart) / 1000d));	    
    }

    /**
     * Calls {@link SemanticSpace#processDocument(BufferedReader)
     * processDocument} once for every document in {@code docIter} using a the
     * specified number thread to call {@code processSpace} on the {@code
     * SemanticSpace} instance.
     *
     * @param sspace the space to build
     * @param docIter an iterator over all the documents to process
     * @param numThreads the number of threads to use
     */
    protected void parseDocumentsMultiThreaded(final SemanticSpace sspace,
					       final Iterator<Document> docIter,
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
				sspace.processDocument(doc.reader());
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
    protected static Set<String> loadValidTermSet(String validTermsFileName) 
	throws IOException {

	Set<String> validTerms = new HashSet<String>();
	BufferedReader br = new BufferedReader(
	    new FileReader(validTermsFileName));
	
	for (String line = null; (line = br.readLine()) != null; ) {
	    validTerms.add(line);
	}
	 
	br.close();

	return validTerms;
    }

    protected void verbose(String msg) {
	if (verbose) {
	    System.out.println(msg);
	}
    }

    protected void verbose(String format, Object... args) {
	if (verbose) {
	    System.out.printf(format, args);
	}
    }
}
