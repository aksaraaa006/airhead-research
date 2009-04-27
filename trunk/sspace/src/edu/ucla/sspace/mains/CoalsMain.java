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
public class CoalsMain {

    public static final String COALS_SEMANTIC_SPACE_FILE_NAME =
	"coals-semantic-space.sspace";

    /**
     * internal flag for printing verbose information to stdout.
     */
    private boolean verbose;

    private final ArgOptions argOptions;

    private CoalsMain() {
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

	CoalsMain coals = new CoalsMain();

	if (args.length == 0) {
	    coals.usage();
	    return;
	}

	try {
	    coals.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private void run(String[] args) throws Exception {
	
	if (argOptions.numPositionalArgs() == 0) {
	    throw new IllegalArgumentException("must specify output directory");
	}

	File outputDir = new File(argOptions.getPositionalArg(0));
	if (!outputDir.isDirectory()){
	    throw new IllegalArgumentException(
		"output directory is not a directory: " + outputDir);
	}
	
	Coals coals = new Coals();

	verbose = argOptions.hasOption("v") || argOptions.hasOption("verbose");

	Iterator<Document> docIter = null;
	String docFile = argOptions.getStringOption("docFile");

	// all the documents are listed in one file, with one document per line
	docIter = new OneLinePerDocumentIterator(docFile);
	
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
	    props.setProperty(Coals.REDUCE_MATRIX_DIMENSION_PROPERTY,
			      argOptions.getStringOption("dimensions"));
	}

	if (argOptions.hasOption("preprocess")) {
	    props.setProperty(Coals.REDUCE_MATRIX_PROPERTY,
			      argOptions.getStringOption("preprocess"));
	}

	parseDocumentsMultiThreaded(coals, docIter, props, numThreads);

	coals.processSpace(props);
	
	File output = (overwrite)
	    ? new File(outputDir, COALS_SEMANTIC_SPACE_FILE_NAME)
	    : File.createTempFile("coals-semantic-space", "sspace", outputDir);

	SemanticSpaceUtils.printSemanticSpace(coals, output);
    }



     /**
      *
      */
    private void parseDocumentsMultiThreaded(final Coals lsa,
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
    private  void usage() {
 	System.out.println(
 	    "usage: java CoalsMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}
