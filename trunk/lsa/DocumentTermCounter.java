
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.SpellDictionary;

import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
import java.util.NavigableSet;
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

public class DocumentTermCounter {

    private final ConcurrentMap<String,Integer> termToIndex;

    private final SpellChecker spellChecker;

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

    public DocumentTermCounter() {
	termToIndex = new ConcurrentHashMap<String,Integer>();
	spellChecker = loadSpellChecker();
	termIndexCounter = new AtomicInteger(0);
	docIndexCounter = new AtomicInteger(0);
	termCountsForAllDocs = new AtomicIntegerArray(1 << 25);
	indexToTerm = new String[2500000];
	validTerms = null;
    }

    public DocumentTermCounter(String validTermsFileName) {
	termToIndex = new ConcurrentHashMap<String,Integer>();
	spellChecker = loadSpellChecker();
	termIndexCounter = new AtomicInteger(0);
	docIndexCounter = new AtomicInteger(0);
	termCountsForAllDocs = new AtomicIntegerArray(1 << 25);
	indexToTerm = new String[2500000];
	validTerms = loadValidTermSet(validTermsFileName);
	
    }
    
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
    }

    /**
     *
     * @return the number of unique words seen in this document
     */
    private int parseDocument(String document,
			      PrintWriter documentTermCountFileWriter)
	throws IOException {

	int documentIndex = docIndexCounter.incrementAndGet();

	Map<String,Integer> termCounts = 
	    new LinkedHashMap<String,Integer>(1 << 10, 16f);	

	BufferedReader br = new BufferedReader(new FileReader(document));

	int lineNum = 0;
	int terms = 0;
	for (String line = br.readLine(); line != null; line = br.readLine()) {

	    // split the line based on whitespace
	    line = line.replaceAll("[^A-Za-z0-9'\u00E0-\u00FF]", " ").
		toLowerCase();
	    String[] text = line.split("\\s+");
	    for (String word : text) {
		if (word.length() == 0)
		    continue;
		
		// clean up each word before entering it into the matrix
		String cleaned = word;
		// skip any mispelled or unknown words
		if (!isValid(cleaned))
		    continue;
		
		//System.out.println(cleaned);
		addTerm(cleaned);
		terms++;
	    }
	}	

	br.close();

	// once the document has been fully parsed, output all of the sparse
	// data points using the writer
	synchronized(documentTermCountFileWriter) {
	    documentTermCountFileWriter.println(terms + "\t" + document);
	    documentTermCountFileWriter.flush();
	}

	return termCounts.size();
    }

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
	if (validTerms != null) 
	    return validTerms.contains(word);
	return (spellChecker == null) 
	    ? true
	    : spellChecker.isCorrect(word);
    }

    private static String cleanup(String word) {
	// remove all non-letter characters
	word = word.replaceAll("\\W", "");
	// make the string lower case
	return word.toLowerCase().intern();
    }

    private void parseDocumentsMultiThreaded(String documentsListing, 
					     String documentTermCountFile)
	throws IOException {

	BufferedReader br = 
	    new BufferedReader(new FileReader(documentsListing));
			
	final PrintWriter  documentTermCountFileWriter = 
	    new PrintWriter(new File(documentTermCountFile));

	int NUM_THREADS = 8;

	ThreadPoolExecutor executor = 
	    new ScheduledThreadPoolExecutor(NUM_THREADS);

	String document = null;
	AtomicInteger count = new AtomicInteger(0);	
	
	while ((document = br.readLine()) != null) {
	    final String toParse = document;
	    final int docNumber = count.incrementAndGet();
	    executor.submit(new Runnable() {
		    public void run() {
			long startTime = System.currentTimeMillis();
			int terms = 0;
			try {
			    terms = parseDocument(toParse, 
						  documentTermCountFileWriter);
			} catch (Throwable t) {
			    t.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.printf("parsed document #" + docNumber + 
					  " " + toParse + " (" + terms +
					  " terms) in %.3f seconds)%n",
					  ((endTime - startTime) / 1000d));
		    }
		});
	}
	
	System.out.println("Enqueued tasks to parse all documents");
		
	// notify the executor that no futher tasks will be submitted
	// and that it should shut down after finishing the current
	// queue
	executor.shutdown();

	System.out.println("Awaiting finishing");

	// wait until all the documents have been parsed
	try {
	    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	} catch (InterruptedException ie) {
	    ie.printStackTrace();
	}

	documentTermCountFileWriter.close();

	System.out.printf("Saw %d terms over %d documents%n",
			  termToIndex.size(), count.get());
    }

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
	    System.out.println("usage: java DocumentTermCounter " + 
			       "<doc file> <output term-doc matrix file>" +
			       " [valid terms list]");
	}
	try {
	    ((args.length == 2)
	     ? new DocumentTermCounter()
	     : new DocumentTermCounter(args[2]))
		.parseDocumentsMultiThreaded(args[0], args[1]);
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }
}