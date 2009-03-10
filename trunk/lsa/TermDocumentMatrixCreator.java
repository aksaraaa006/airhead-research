/*
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.SpellDictionary;

import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
//import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * A class for creating the term-document matrix.
 */
public class TermDocumentMatrixCreator {

    private final ConcurrentMap<String,Integer> termToIndex;

    // private final SpellChecker spellChecker;

    private final AtomicInteger termIndexCounter;

    private final AtomicInteger docIndexCounter;
    
    private final AtomicIntegerArray termCountsForAllDocs;

    private static final String TERM_MATRIX_SUFFIX =
	"-term-document-matrix.dat";
    
    private static final String TERM_INDEX_SUFFIX =
	".indexToTerm.dat";

    private final String[] indexToTerm;

    /**
     * If a term-list has been provided to the constructor, this set will
     * contain all the terms that this class will use.  All other terms,
     * regarless of whether they are spelled correctly, will be excluded from
     * the term-document matrix.
     */
    private final Set<String> validTerms;

    public TermDocumentMatrixCreator() {
	this(null);
    }

    public TermDocumentMatrixCreator(String validTermsFileName) {
	termToIndex = new ConcurrentHashMap<String,Integer>();
	//spellChecker = loadSpellChecker();
	termIndexCounter = new AtomicInteger(0);
	docIndexCounter = new AtomicInteger(0);
	termCountsForAllDocs = new AtomicIntegerArray(1 << 25);
	indexToTerm = new String[100000000]; // 10 million
	validTerms = (validTermsFileName == null)
	    ? null : loadValidTermSet(validTermsFileName);
	
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

    /**
     * Attempts to load the spell checker from the {@code dictionary/english}
     * path, and returns it.  If no spell checker can be found, returns {@code
     * null}
     */ 
    /*
    private static SpellChecker loadSpellChecker() {
	try {
	    String DICTIONARY_PATH = "dictionary/english/";
	    SpellDictionaryHashMap dictionary = 
		new SpellDictionaryHashMap();

	    // load the standard american english dictionaries from file
	    dictionary.addDictionary(new File(DICTIONARY_PATH + "eng_com.dic"));
	    dictionary.addDictionary(new File(DICTIONARY_PATH + "center.dic"));
	    dictionary.addDictionary(new File(DICTIONARY_PATH + "ize.dic"));
	    dictionary.addDictionary(new File(DICTIONARY_PATH + "labeled.dic"));
	    dictionary.addDictionary(new File(DICTIONARY_PATH + "yze.dic"));
	    dictionary.addDictionary(new File(DICTIONARY_PATH + "color.dic"));
	    
	    return new SpellChecker(dictionary);
	}
	catch (Exception e) {
	    e.printStackTrace();
	    System.out.println("No spell checker available");
	    return null;
	}
	}*/

    /**
     * Parses the document and prints all the term occurrences to the provided
     * {@code termDocumentMatrixWriter}
     *
     * @return the number of unique words seen in this document
     */
    private int parseDocument(BufferedReader document,
			      PrintWriter termDocumentMatrixWriter)
	throws IOException {

	Map<String,Integer> termCounts = 
	    new LinkedHashMap<String,Integer>(1 << 10, 16f);	

	int lineNum = 0;
	for (String line = null; (line = document.readLine()) != null; ) {

	    // replace all non-word characters with whitespace.  We also include
	    // some uncommon characters (such as those with the eacute).
	    //line = line.replaceAll("[^A-Za-z0-9'\u00E0-\u00FF]", " ").
	    line = line.replaceAll("\\W"," ").toLowerCase();

	    // split the line based on whitespace
	    String[] text = line.split("\\s+");

	    // for each word in the text document, keep a count of how many
	    // times it has occurred
	    for (String word : text) {
		if (word.length() == 0)
		    continue;
		
		// clean up each word before entering it into the matrix
		String cleaned = word;
		// skip any mispelled or unknown words
		if (!isValid(cleaned))
		    continue;
		
		//System.out.println(cleaned);
		
		// Add the term to the total list of terms to ensure it has a
		// proper index.  If the term was already added, this method is
		// a no-op
		addTerm(cleaned);
		Integer termCount = termCounts.get(cleaned);

		// update the term count
		termCounts.put(cleaned, (termCount == null) 
			       ? Integer.valueOf(1)
			       : Integer.valueOf(1 + termCount.intValue()));
	    }
	}

	document.close();

	// check that we actually loaded in some terms before we increase the
	// documentIndex.  This could possibly save some dimensions in the final
	// array for documents that were essentially blank.  If we didn't see
	// any terms, just return 0
	if (termCounts.isEmpty())
	    return 0;

	int documentIndex = docIndexCounter.incrementAndGet();

	// Once the document has been fully parsed, output all of the sparse
	// data points using the writer.  Synchronize on the writer to prevent
	// any interleaving of output by other threads
	synchronized(termDocumentMatrixWriter) {
	    for (Map.Entry<String,Integer> e : termCounts.entrySet()) {
		String term = e.getKey();
		int count = e.getValue().intValue();
		StringBuffer sb = new StringBuffer(32);
		sb.append(termToIndex.get(term).intValue()).append("\t").
		    append(documentIndex).append("\t").append(count);
		termDocumentMatrixWriter.println(sb.toString());
	    }
	    
	    termDocumentMatrixWriter.flush();
	}

	// then update the final counts for each term.  Note that we do this in
	// a separate loop since the printing must be synchronized but these
	// term counts can be written concurrently.
	for (Map.Entry<String,Integer> e : termCounts.entrySet()) {
	    termCountsForAllDocs.addAndGet(termToIndex.
					   get(e.getKey()).intValue(),
					   e.getValue().intValue());
	}
	
	return termCounts.size();
    }
    
    /**
     * Adds the term to the list of terms and gives it an index, or if the term
     * has already been added, does nothing.
     */
    private void addTerm(String term) {
	// ensure that we are using the canonical version of this term so that
	// we can properly lock on it.
	term = term.intern();
	Integer index = termToIndex.get(term);
	if (index == null) {
	    // lock on the term itself so that only two threads trying to add
	    // the same term will block on each other
	    synchronized(term) {
		// recheck to see if the term was added while blocking
		index = termToIndex.get(term);
		// if some other thread has not already added this term while
		// the current thread was blocking waiting on the lock, then add
		// it.
		if (index == null) {
		    index = Integer.valueOf(termIndexCounter.incrementAndGet());
		    termToIndex.put(term, index);
		    indexToTerm[index.intValue()] = term;
		}
	    }
	}
    }

    /**
     * Returns whether the provided word is valid according to the spell
     * checker, or returns {@code true} if no spell checker has been loaded.
     */
    private boolean isValid(String word) {
	return  (validTerms == null) || validTerms.contains(word);
    }

    /**
     * Parses all the documents in the provided list and writes the resulting
     * term-document matrix to the provided file
     */
    private void parseDocumentsMultiThreaded(final DocumentIterator docIter, 
					     String termDocumentMatrixFilePrefix)
	throws IOException {

	final String termDocumentMatrixFileName = 
	    termDocumentMatrixFilePrefix + 
	    TERM_MATRIX_SUFFIX;
	
	final PrintWriter termDocumentMatrixFileWriter = 
	    new PrintWriter(new File(termDocumentMatrixFileName));

	int NUM_THREADS = 4;
	Collection<Thread> threads = new LinkedList<Thread>();

	final AtomicInteger count = new AtomicInteger(0);
	

	for (int i = 0; i < NUM_THREADS; ++i) {
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
				terms = 
				    parseDocument(doc.reader(), 
						  termDocumentMatrixFileWriter);
			    } catch (Throwable t) {
				t.printStackTrace();
			    }
			    long endTime = System.currentTimeMillis();
			    System.out.printf("parsed document #" + docNumber + 
					      " (" + terms +
					      " terms) in %.3f seconds)%n",
					      ((endTime - startTime) / 1000d));
			}
		    }
		};
	    threads.add(t);
	}
	
	// start all the threads processing
	for (Thread t : threads)
	    t.start();

	System.out.println("Awaiting finishing");

	// wait until all the documents have been parsed
	try {
	    for (Thread t : threads)
		t.join();
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}

	termDocumentMatrixFileWriter.close();

	System.out.printf("Saw %d terms over %d documents%n",
			  termToIndex.size(), count.get());

	System.out.println("writing index-term map file termIndex.txt");

	// Last, write out the index-to-term map that will allow us to
	// reconstruct which row a term is in the term-document matrix
	String indexToTermFileName = 
	    termDocumentMatrixFilePrefix + TERM_INDEX_SUFFIX;
	
	PrintWriter pw = new PrintWriter(indexToTermFileName);
	int termIndex = 0;
	for (Map.Entry<String,Integer> e : termToIndex.entrySet())
	    pw.printf("%07d\t%d\t%s%n", (termIndex = e.getValue().intValue()),
		      termCountsForAllDocs.get(termIndex), e.getKey());
	pw.close();

    }


    // NOTE: currently unused
    private void pruneIndices(String termDocumentMatrixFilePrefix) 
	throws IOException {

	int[] termCounts = new int[termToIndex.size()];
	for (int i = 0; i < termCounts.length; ++i) {
	    termCounts[i] = termCountsForAllDocs.get(i);
	}
	System.out.println("sorting by term counts");
	Arrays.sort(termCounts);
	// use the top terms whose values is greater than or equal to the term
	// count for the term at the 80% mark.
	int keepThreshold = termCounts[(int)(termCounts.length * .91d)];
	
	// now read in the term matrix and look at the count for each term.  If
	// the term count is below the threshold, do not print the term to the
	// pruned file
	BufferedReader br = new BufferedReader(new FileReader(
	    termDocumentMatrixFilePrefix + TERM_MATRIX_SUFFIX));
	PrintWriter prunedWriter = new PrintWriter(
	    termDocumentMatrixFilePrefix + "-pruned-" +
	    TERM_MATRIX_SUFFIX);

	System.out.println("pruning rare terms");

	BitSet prunedTerms = new BitSet(termCounts.length);

	int pruneCount = 0;
	String line = null;
	while ((line = br.readLine()) != null) {
	    String[] termDocCount = line.split("\t");
	    if (termDocCount.length != 3)
		throw new Error("unknown line: " + line);
	    int termIndex = Integer.valueOf(termDocCount[0]);
	    int termCount = termCountsForAllDocs.get(termIndex);
	    
	    if (termCount >= keepThreshold)
		prunedWriter.println(line);
	    else {
		pruneCount++;
		prunedTerms.set(termIndex);
	    }
	}

	System.out.println("Terms Pruned: " + pruneCount);

	PrintWriter prunedTermsWriter = new PrintWriter(
	    termDocumentMatrixFilePrefix + "-pruned-terms.dat");	

	for (int i = prunedTerms.nextSetBit(0); i >= 0; 
	     i = prunedTerms.nextSetBit(i+1)) {
	    prunedTermsWriter.println(i + "\t" + indexToTerm[i]);
	}

	prunedWriter.close();
	br.close();
    }


    public static void main(String[] args) {
	if (args.length != 2 && args.length != 3) {
	    System.out.println("usage: java TermDocumentMatrixCreator " + 
			       "[--fileList=<file>|--docFile=<file>] " +
			       "<output term-doc matrix file> " +
			       "[valid terms list]");
	    return;
	}
	try {
	    // figure out what kind of document file we're getting
	    String[] typeAndFile = args[0].split("=");
	    if (typeAndFile.length != 2) {
		System.out.println("invalid document file arg: " + args[0]);
		return;
	    }
		
	    DocumentIterator docIter = null;
	    if (typeAndFile[0].equals("--fileList")) {
		// we have a file that contains the list of all document files
		// we are to process
		docIter = new FileListDocumentIterator(typeAndFile[1]);
	    }
	    else if (typeAndFile[0].equals("--docFile")) {
		// all the documents are listed in one file, with one
		// document per line
		docIter = new SingleFileDocumentIterator(typeAndFile[1]);
	    }
	    else {
		System.out.println("invalid document file arg: " + args[0]);
		return;
	    }
	    
	    ((args.length == 2)
	     ? new TermDocumentMatrixCreator()
	     : new TermDocumentMatrixCreator(args[2]))
		.parseDocumentsMultiThreaded(docIter, args[1]);
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    public interface Document {

	/**
	 * Returns the {@code BufferedReader} for this document's text
	 */
	BufferedReader reader();

    }

    public interface DocumentIterator {
	
	/**
	 * Returns {@code true} if there are still documents left.
	 */
	boolean hasNext();

	Document next();
	
    }

    public static class FileListDocumentIterator implements DocumentIterator {

	private final Queue<String> filesToProcess;

	public FileListDocumentIterator(String fileListName) 
	    throws IOException {
	    
	    filesToProcess = new ConcurrentLinkedQueue<String>();
	    
	    // read in all the files we have to process
	    BufferedReader br = 
		new BufferedReader(new FileReader(fileListName));
	    for (String line = null; (line = br.readLine()) != null; )
		filesToProcess.offer(line.trim());	    
	}

	public boolean hasNext() {
	    return !filesToProcess.isEmpty();
	}

	public Document next() {
	    String fileName = filesToProcess.poll();
	    return (fileName == null) ? null : new FileDocument(fileName);
	}	
    }

    public static class SingleFileDocumentIterator implements DocumentIterator {

	private final BufferedReader documentsReader;

	private String nextLine;

	public SingleFileDocumentIterator(String documentsFile) 
	    throws IOException {
	    
	    documentsReader = new BufferedReader(new FileReader(documentsFile));
	    nextLine = documentsReader.readLine();
	}

	public synchronized boolean hasNext() {
	    return nextLine != null;
	}

	public synchronized Document next() {
	    Document next = new StringDocument(nextLine);
	    try {
		nextLine = documentsReader.readLine();
	    } catch (Throwable t) {
		t.printStackTrace();
		nextLine = null;
	    }
	    return next;
	}	
    }
    

    private static class FileDocument implements Document {
	
	private final BufferedReader reader;

	public FileDocument(String fileName) {
	    BufferedReader r = null;
	    try {
		r = new BufferedReader(new FileReader(fileName));
	    } catch (Throwable t) {
		t.printStackTrace();
	    }
	    reader = r;
	}

	public BufferedReader reader() {
	    return reader;
	}

    }

    private static class StringDocument implements Document {
	
	private final BufferedReader reader;

	private StringDocument(String docText) {
	    reader = new BufferedReader(new StringReader(docText));
	}

	public BufferedReader reader() {
	    return reader;
	}
	
    }
    
}