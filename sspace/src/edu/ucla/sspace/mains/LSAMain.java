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
 *      MatrixTransform} to use in preprocessing the word-document matrix
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
 * An invocation will produce one file as output
 *
 * <ol>
 *
 *   <li> {@code 
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core.
 *
 */
public class LSAMain {

    public static final String LSA_SEMANTIC_SPACE_FILE_NAME =
	"lsa-semantic-space.sspace";

    /**
     * internal flag for printing verbose information to stdout.
     */
    private boolean verbose;
    
    private LSAMain() {

    }

    public static void main(String[] args) {
		
	if (args.length == 0) {
	    usage();
	    return;
	}

	try {
	    new LSAMain().run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private void run(String[] args) throws Exception {
	
	// process command line args
	ArgOptions options = new ArgOptions(args);

	if (options.numArgs() == 0) {
	    throw new IllegalArgumentException("must specify output directory");
	}

	File outputDir = new File(options.getArg(0));
	if (!outputDir.isDirectory()){
	    throw new IllegalArgumentException(
		"output directory is not a directory: " + outputDir);
	}

	LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
	
	verbose = options.hasOption("v") || options.hasOption("verbose");

	Iterator<Document> docIter = null;
	String fileList = options.getStringOption("fileList");
	String docFile = options.getStringOption("docFile");
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
	if (options.hasOption("threads")) {
	    numThreads = options.getIntOption("threads");
	}

	boolean overwrite = true;
	if (options.hasOption("overwrite")) {
	    overwrite = options.getBooleanOption("overwrite");
	}
	
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();

	if (options.hasOption("dimensions")) {
	    props.setProperty(LatentSemanticAnalysis.LSA_DIMENSIONS_PROPERTY,
			      options.getStringOption("dimensions"));
	}

	if (options.hasOption("preprocess")) {
	    props.setProperty(LatentSemanticAnalysis.MATRIX_TRANSFORM_PROPERTY,
			      options.getStringOption("preprocess"));
	}

	parseDocumentsMultiThreaded(lsa, docIter, props, numThreads);

	lsa.processSpace();
	
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
			    verbose("parsed document #%d in %.3f seconds)%n",
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
    private static void usage() {
	System.out.println(
	    "usage: java LSAMain [options] <document source> <output-dir>\n" + 
	    "\tdocument sources (select one):\n" +
	    "\t [--fileList=<file containing document file names>\n" +
	    "\t  | --docFile=<file with one document per line>]\n" +
	    "\toptions:\n" +
	    "\t [--dimensions=<int>]\n" +
	    "\t [--threads=<int>]\n" +
	    "\t [--preprocess=<MatrixTransform implementation name>]\n" +
	    "\t [--overwrite=[true|false] (overwrite existing files)\n");
    }
}
