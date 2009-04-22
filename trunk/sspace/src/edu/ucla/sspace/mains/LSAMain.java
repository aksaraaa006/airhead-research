package edu.ucla.sspace.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Pattern;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.lsa.LatentSemanticAnalysis;

/**
 *
 */
public class LSAMain {

    private static final String TERM_MATRIX_SUFFIX =
	"-term-document-matrix.dat";
    
    private static final String TERM_INDEX_SUFFIX =
	".indexToTerm.dat";
    
    public static void main(String[] args) {
		
	if (args.length == 0) {
	    usage();
	    return;
	}


	try {
	    run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private static void run(String[] args) throws Exception {

	LatentSemanticAnalysis lsa = new LatentSemanticAnalysis();
	
	// process command line args
	ArgOptions options = new ArgOptions(args);


	if (args.length != 2 && args.length != 3) {
	    System.out.println("usage: java TermDocumentMatrixCreator " + 
			       "[--fileList=<file>|--docFile=<file>] " +
			       "<output term-doc matrix file> " +
			       "[valid terms list]");
	    return;
	}
// 	try {
// 	    // figure out what kind of document file we're getting
// 	    String[] typeAndFile = args[0].split("=");
// 	    if (typeAndFile.length != 2) {
// 		System.out.println("invalid document file arg: " + args[0]);
// 		return;
// 	    }
		
// 	    DocumentIterator docIter = null;
// 	    if (typeAndFile[0].equals("--fileList")) {
// 		// we have a file that contains the list of all document files
// 		// we are to process
// 		docIter = new FileListDocumentIterator(typeAndFile[1]);
// 	    }
// 	    else if (typeAndFile[0].equals("--docFile")) {
// 		// all the documents are listed in one file, with one
// 		// document per line
// 		docIter = new SingleFileDocumentIterator(typeAndFile[1]);
// 	    }
// 	    else {
// 		System.out.println("invalid document file arg: " + args[0]);
// 		return;
// 	    }
	    
// 	    ((args.length == 2)
// 	     ? new TermDocumentMatrixCreator()
// 	     : new TermDocumentMatrixCreator(args[2]))
// 		.parseDocumentsMultiThreaded(docIter, args[1]);
// 	} catch (Throwable t) {
// 	    t.printStackTrace();
// 	}

    }



//     /**
//      * Parses all the documents in the provided list and writes the resulting
//      * term-document matrix to the provided file
//      */
//     private void parseDocumentsMultiThreaded(final DocumentIterator docIter, 
// 					     String termDocumentMatrixFilePrefix)
// 	throws IOException {

// 	final String termDocumentMatrixFileName = 
// 	    termDocumentMatrixFilePrefix + 
// 	    TERM_MATRIX_SUFFIX;
	
// 	final PrintWriter termDocumentMatrixFileWriter = 
// 	    new PrintWriter(new File(termDocumentMatrixFileName));

// 	int NUM_THREADS = 5;
// 	Collection<Thread> threads = new LinkedList<Thread>();

// 	final AtomicInteger count = new AtomicInteger(0);
	

// 	for (int i = 0; i < NUM_THREADS; ++i) {
// 	    Thread t = new Thread() {
// 		    public void run() {
// 			// repeatedly try to process documents while some still
// 			// remain
// 			while (docIter.hasNext()) {
// 			    long startTime = System.currentTimeMillis();
// 			    Document doc = docIter.next();
// 			    int docNumber = count.incrementAndGet();
// 			    int terms = 0;
// 			    try {
// 				parseDocument(doc.reader());
// 			    } catch (Throwable t) {
// 				t.printStackTrace();
// 			    }
// 			    long endTime = System.currentTimeMillis();
// 			    System.out.printf("parsed document #" + docNumber + 
// 					      " (" + terms +
// 					      " terms) in %.3f seconds)%n",
// 					      ((endTime - startTime) / 1000d));
// 			}
// 		    }
// 		};
// 	    threads.add(t);
// 	}
	
// 	// start all the threads processing
// 	for (Thread t : threads)
// 	    t.start();

// 	System.out.println("Awaiting finishing");

// 	// wait until all the documents have been parsed
// 	try {
// 	    for (Thread t : threads)
// 		t.join();
// 	} catch (InterruptedException ie) {
// 	    ie.printStackTrace();
// 	}

// 	termDocumentMatrixFileWriter.close();

// 	System.out.printf("Saw %d terms over %d documents%n",
// 			  termToIndex.size(), count.get());

// 	System.out.println("writing index-term map file termIndex.txt");

// 	// Last, write out the index-to-term map that will allow us to
// 	// reconstruct which row a term is in the term-document matrix
// 	String indexToTermFileName = 
// 	    termDocumentMatrixFilePrefix + TERM_INDEX_SUFFIX;
	
// 	PrintWriter pw = new PrintWriter(indexToTermFileName);
// 	int termIndex = 0;
// 	for (Map.Entry<String,Integer> e : termToIndex.entrySet())
// 	    pw.printf("%07d\t%d\t%s%n", (termIndex = e.getValue().intValue()),
// 		      termCountsForAllDocs.get(termIndex), e.getKey());
// 	pw.close();

//     }



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
    
    private static void usage() {
	System.out.println("java LSAMain <options>\n");
    }
}
