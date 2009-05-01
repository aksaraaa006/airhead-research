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
 * A base class for running {@link SemanticSpace} algorithms.
 *
 * @author David Jurgens
 */
public abstract class GenericMain {

    /**
     * Whether to emit messages to {@code stdout} when the {@code verbose}
     * methods are used.
     */
    protected boolean verbose;

    /**
     * The processed argument options available to the main classes.
     */
    protected final ArgOptions argOptions;

    /**
     * The default file name used when replacing an existing semantic space
     * output, ends in ".sspace".
     */
    private final String defaultFileName;

    /**
     * The temporary file name used when dumping a semantic space.
     */
    private final String tempFileName;

    public GenericMain(String defaultName) {
      argOptions = setupOptions();
      verbose = false;
      defaultFileName = defaultName + ".sspace";
      tempFileName = defaultName;
    }

    /**
     * Abstract method to return a {@link SemanticSpace} for use when running.
     */
    abstract public SemanticSpace getSpace();

    /**
     * Abstract method which should print out the usage text for the particular
     * name.
     */
    abstract public void usage();

    /**
     * Allows sub-mains the ability to add extra command line options.
     * @param options the ArgOptions object which more main specific options can
     * be added to.
     */
    public void addExtraOptions(ArgOptions options) {
    }

    /**
     * Parallel to {@link addExtraOptions}, allows the sub-main to process extra
     * options.  Will be called before {@link getSpace}.
     */
    public void handleExtraOptions() {
    }

    /**
     * Allows sub-mains to process arguments and build a Properties object for
     * use when calling {@link processSpace}.  Will be called before
     * {@link getSpace}.
     */
    public Properties setupProperties() {
      Properties props = System.getProperties();
      return props;
    }

    /**
     * Setup default options which all mains will use.
     */
    public ArgOptions setupOptions() {
      ArgOptions options = new ArgOptions();
	options.addOption('f', "fileList", "a list of document files", 
			     true, "FILE[,FILE...]", "Required (at least one of)");
	options.addOption('d', "docFile", 
			     "a file where each line is a document", true,
			     "FILE[,FILE]", "Required (at least one of)");

	options.addOption('t', "threads", "the number of threads to use",
			     true, "INT", "Program Options");
	options.addOption('w', "overwrite", "specifies whether to " +
			     "overwrite the existing output", false, null,
                 "Program Options");

	options.addOption('v', "verbose", "prints verbose output",
                      false, null, "Program Options");
    addExtraOptions(options);
    return options;
    }

    protected void run(String[] args) throws Exception {
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

	parseDocumentsMultiThreaded(space, docIter, props, numThreads);

    long startTime = System.currentTimeMillis();
	space.processSpace(props);
	long endTime = System.currentTimeMillis();
    verbose("processed space in %.3f seconds%n",
            ((endTime - startTime) / 1000d));
	
	File output = (overwrite)
	    ? new File(outputDir, defaultFileName)
	    : File.createTempFile(tempFileName, "sspace", outputDir);

    startTime = System.currentTimeMillis();
	SemanticSpaceUtils.printSemanticSpace(space, output);
	endTime = System.currentTimeMillis();
    verbose("printed space in %.3f seconds%n",
            ((endTime - startTime) / 1000d));
    }

    protected void parseDocumentsSingleThreaded(SemanticSpace sspace,
						Iterator<Document> docIter,
						Properties properties) 
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

    protected void parseDocumentsMultiThreaded(final SemanticSpace sspace,
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
