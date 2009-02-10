
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

public class OnTheFlyTermDocumentParser {

    private static final int LINES_TO_SKIP = 200;

    private static final int MAX_LINES = 750;

    private static final int NUM_THREADS = 1;

    private final ConcurrentMap<String,Integer> wordToIndex;

    private final ConcurrentMap<String,Integer> wordToCount;

    private final ConcurrentMap<Integer,String> indexToWord;

    private final ConcurrentMap<String,Integer> documentToIndex;

    private final SpellChecker spellChecker;

    private final ThreadPoolExecutor executor;

    private final AtomicInteger termCount;

    private final AtomicInteger docCount;
    
    public OnTheFlyTermDocumentParser() {
	wordToIndex = new ConcurrentHashMap<String,Integer>();
	wordToCount = new ConcurrentHashMap<String,Integer>(1 << 16, 2f);
	indexToWord = new ConcurrentHashMap<Integer,String>();
	documentToIndex = new ConcurrentHashMap<String,Integer>();
	spellChecker = loadSpellChecker();
	executor = new ScheduledThreadPoolExecutor(NUM_THREADS);
	termCount = new AtomicInteger(0);
	docCount = new AtomicInteger(0);
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
    private void parseDocument(String document, 
			       Collection<Point> points) throws IOException {

	int documentIndex = docCount.incrementAndGet();
	documentToIndex.put(document, Integer.valueOf(documentIndex));

	Map<String,Integer> termCounts = 
	    new LinkedHashMap<String,Integer>(1 << 10, 4f);	

	BufferedReader br = new BufferedReader(new FileReader(document));
	String line = null;
	int lineNum = 0;
	while ((line = br.readLine()) != null) {
	    // don't count blank lines 
	    if (line.length() == 0)
		continue;

	    if (lineNum++ < LINES_TO_SKIP)
		continue;

	    if (lineNum > MAX_LINES)
		break;

	    // split the line based on whitespace
	    String[] text = line.split("\\s");
	    for (String word : text) {
		// clean up each word before entering it into the matrix
		String cleaned = cleanup(word);
		// skip any mispelled or unknown words
		if (!isValid(cleaned))
		    continue;

		addTerm(cleaned);
		Integer termCount = termCounts.get(cleaned);
		termCounts.put(cleaned, (termCount == null) 
			       ? Integer.valueOf(1)
			       : Integer.valueOf(1 + termCount.intValue()));
	    }
	}
	
	// once the document has been fully parsed, output all of the sparse
	// data points using the writer
	for (Map.Entry<String,Integer> e : termCounts.entrySet()) {
	    String term = e.getKey();
	    int count = e.getValue().intValue();
	    points.add(new Point(wordToIndex.get(term).intValue(),
				 documentIndex,	count));
				 
	    Integer oldCount = wordToCount.get(term);
	    wordToCount.put(term, (oldCount== null) ? Integer.valueOf(count)
			    : Integer.valueOf(count + oldCount.intValue()));
	}
	br.close();
    }

    private void addTerm(String term) {
	// ensure that we are using the canonical version of this term
	// so that we can properly lock on it.
	term = term.intern();
	Integer index = wordToIndex.get(term);
	if (index == null) {
	    // lock on the term itself so that only two threads trying
	    // to add the same term will block on each other
	    synchronized(term) {
		// recheck to see if the term was added while blocking
		index = wordToIndex.get(term);
		// if some other thread has not already added this
		// term while the current thread was blocking waiting
		// on the lock, then add it.
		if (index == null) {
		    index = Integer.valueOf(termCount.incrementAndGet());
		    wordToIndex.put(term, index);
		    indexToWord.put(index, term);
		}
	    }
	}
    }

    /**
     * Returns whether the provided word is valid according to the spell
     * checker, or returns {@code true} if no spell checker has been loaded.
     */
    private boolean isValid(String word) {
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


    public void parseDocuments(String documentsListing, 
			       String outputFile) throws IOException {
	// switch behavior depending on whether the parsing is
	// multithreaded
	if (NUM_THREADS > 1) 
	    parseDocumentsMulti(documentsListing, outputFile);
	else
	    parseDocumentsSingle(documentsListing, outputFile);
    }

    private void parseDocumentsMulti(String documentsListing, 
				      String outputFile) throws IOException {

	BufferedReader br = 
	    new BufferedReader(new FileReader(documentsListing));
	
	String document = null;
	final AtomicInteger parsed = new AtomicInteger(0);

	// Use a ConcurrentQueue for fast insertion
	final Queue<Point> allPoints = new ConcurrentLinkedQueue<Point>();

	int totalTermsSeen = 0;
	while ((document = br.readLine()) != null) {
	    final String doc = document;
	    executor.submit(new Runnable() {
		    public void run() {
			System.out.print(Thread.currentThread() + 
					 " parsing document " + 
					 parsed.incrementAndGet() + ": "
					 + doc + "...");
			long startTime = System.currentTimeMillis();
			try {
			    parseDocument(doc, allPoints);
			} catch (Throwable t) {
			    t.printStackTrace();
			}
			long endTime = System.currentTimeMillis();
			System.out.printf("complete (%.3f seconds)%n",
					  (endTime - startTime) / 1000d);
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

    }

    private void parseDocumentsSingle(String documentsListing, 
				      String outputFile) throws IOException {
	BufferedReader br = 
	    new BufferedReader(new FileReader(documentsListing));
		
	String document = null;
	int count = 0;

	// List<Point> allPoints = new ArrayList<Point>(44968551);

	// Use a LinkedList for fast insertion and removal
	List<Point> allPoints = new LinkedList<Point>();

	int totalTermsSeen = 0;
	while ((document = br.readLine()) != null) {
	    System.out.print("parsing document " + (count++) + ": "
			     + document + "...");
	    long startTime = System.currentTimeMillis();
	    try {
		parseDocument(document, allPoints);
	    } catch (Throwable t) {
		t.printStackTrace();
	    }
	    long endTime = System.currentTimeMillis();
	    System.out.printf("complete (%.3f seconds)%n",
			      (endTime - startTime) / 1000d);
	}

	// output a histogram of the term occurence in the matrix
	int[] histogram = new int[32];
	int sumOccurrence = 0;
	for (Map.Entry<String,Integer> e : wordToCount.entrySet()) {	
	    int i = 0;
	    int occurrences = e.getValue();
	    while (occurrences > (1 << i++));
	    histogram[i-1]++;
	    sumOccurrence += occurrences;
	}

	double mean = sumOccurrence / (double)wordToCount.size();
	
	double maxBinValue = 0;
	int firstBin = histogram.length-1;
	int lastBin = 0;
	for (int i = 0; i < histogram.length; ++i) {
	    int bin = histogram[i];
	    if (bin > 0 && i < firstBin) {
		firstBin = i;
	    }
	    if (bin > maxBinValue)
		maxBinValue = bin;
	    if (bin > 0 && i > lastBin) {
		lastBin = i;
	    }
	}
	int barsToShow = 40;
		
	StringBuilder sb = new StringBuilder("Distribution of Term Occurences\n");
	for (int i = firstBin; i <= lastBin; ++i) {
	    int bin = histogram[i];
	    int bars = (int)((bin / maxBinValue) * barsToShow);
	    if (bin > 0)
		bars++;
	    sb.append(String.format("%5d:", (1 << i)));
	    for (int b = 0; b < bars; ++b)
		sb.append("*");
	    sb.append("\n");
	}
	
	System.out.print(sb.toString());

	System.out.printf("saw %d terms in %d documents for %d data elements, "
			  + "mean term occurrence %.3f%n%n",
			  wordToIndex.size(), documentToIndex.size(), 
			  allPoints.size(), mean);

	System.out.println("removing uncommon terms");

	// truncate the least common terms in the matrix
	Iterator<Point> it = allPoints.iterator();
	double lowThreshold = mean / 2;
	double highThreshold = mean * 16;
	int removed = 0;
	Set<String> removedTerms = new HashSet<String>();
	NavigableSet<Integer> removedTermIndices = new TreeSet<Integer>();
	while (it.hasNext()) {
	    Point p = it.next();
	    // find the count for the term this point is associated with
	    String term = indexToWord.get(p.x);
	    int termOccurrences = wordToCount.get(term).intValue();
	    
	    if (termOccurrences < lowThreshold ||
		termOccurrences > highThreshold) {
		it.remove();
		removed++;
		removedTerms.add(term);
		removedTermIndices.add(wordToIndex.get(term));
	    }
	}

	System.out.printf("Pruned %d data points for %d terms with low " +
			  "occurrence counts%n", removed, removedTerms.size());

	System.out.printf("Compacting term dimension by %d terms...",
			  removedTermIndices.size());
	long startTime = System.currentTimeMillis();
	
	// compact the arrays based on the terms that are being pruned
	Collection<Point> toAdd = new LinkedList<Point>();
	it = allPoints.iterator();
	while (it.hasNext()) {
	    Point p = it.next();
	    int termIndex = p.x;
	    Set<Integer> termsRemovedPrior = 
		removedTermIndices.headSet(Integer.valueOf(termIndex));
	    // if any terms with lower indicies that the current term
	    // index were removed in the pruning stage, decrement this
	    // points index by that amount to compact the array
	    if (termsRemovedPrior.size() > 0) {
		// since the point is immutable, remove it from the
		// collection and add in a different point with the
		// new compacted index
		it.remove();
		int compactedIndex = p.x - termsRemovedPrior.size();
		toAdd.add(new Point(compactedIndex, p.y, p.val));
	    }
	}

	// add back all the new compacted-index points
	allPoints.addAll(toAdd);

	// remove the pruned terms from the maps
	for (String removedTerm : removedTerms)
	    wordToIndex.remove(removedTerm);
	for (Integer removedTermIndex : removedTermIndices)
	    indexToWord.remove(removedTermIndex);

	// lastly, update the termToIndex matrix
	for (Map.Entry<String,Integer> e : wordToIndex.entrySet()) {
	    
	    Integer termIndex = e.getValue();
	    Set<Integer> termsRemovedPrior = 
		removedTermIndices.headSet(termIndex);

	    if (termsRemovedPrior.size() > 0) {
		Integer compactedIndex = 
		    Integer.valueOf(termIndex.intValue() - 
				    termsRemovedPrior.size());
		e.setValue(compactedIndex);
		indexToWord.put(compactedIndex, e.getKey());
	    }
	    
	}

	long endTime = System.currentTimeMillis();
	System.out.printf("done (%.3f seconds)%n",
			  (endTime - startTime) / 1000d);


	// then compact the documents that no longer have any terms
	System.out.print("Compacting document dimension ");
	startTime = System.currentTimeMillis();
	
	// first determine which documents are being used
	Set<Integer> usedDocIndices = new LinkedHashSet<Integer>();
	for (Point p : allPoints)
	    usedDocIndices.add(Integer.valueOf(p.y));
	
	NavigableSet<Integer> unusedDocIndices = 
	    new TreeSet<Integer>(documentToIndex.values());
	unusedDocIndices.removeAll(usedDocIndices);

	System.out.print("by " + unusedDocIndices.size() 
			 + " documents...");

	// clear the old points, for we will be adding new ones with
	// compacted document indicies
	toAdd.clear();
	it = allPoints.iterator();
	if (unusedDocIndices.size() > 0) {
	    while (it.hasNext()) {
		Point p = it.next();
		int docIndex = p.y;
		Set<Integer> docsUnusedPrior = 
		    unusedDocIndices.headSet(Integer.valueOf(docIndex));
		// if any documents with indices lower than the one in
		// the current point are not used, then this point
		// will be compacted
		if (docsUnusedPrior.size() > 0) {
		    // remove the immutable point from the list and
		    // add a new one
		    it.remove();
		    toAdd.add(new Point(p.x, p.y - docsUnusedPrior.size(), 
					p.val));
		}
	    }
	    // add back all the new compacted-index points
	    allPoints.addAll(toAdd);	
	}

	// lastly, update the documentToIndex matrix
	for (Map.Entry<String,Integer> e : documentToIndex.entrySet()) {
	    
	    Integer docIndex = e.getValue();
	    Set<Integer> docsUnusedPrior = 
		unusedDocIndices.headSet(docIndex);

	    if (docsUnusedPrior.size() > 0) {
		Integer compactedIndex = 
		    Integer.valueOf(docIndex.intValue() - 
				    docsUnusedPrior.size());
		e.setValue(compactedIndex);
	    }	    
	}


	endTime = System.currentTimeMillis();
	System.out.printf("done (%.3f seconds)%n",
			  (endTime - startTime) / 1000d);
	


	if (false) {
	    System.out.print("Sorting...");
	    startTime = System.currentTimeMillis();
	    // recast the LinkedList has an ArrayList to provide for
	    // random access
	    allPoints = new ArrayList<Point>(allPoints);
	    Collections.sort(allPoints);
	    endTime = System.currentTimeMillis();
	    System.out.printf("done (%.3f seconds)%n",
			      (endTime - startTime) / 1000d);
	}


	System.out.println("writing matrix file termDocumentMatrix.txt");

	PrintWriter pw = new PrintWriter("termDocumentMatrix.dat");
	for (Point p : allPoints) {
	    pw.printf("%d\t%d\t%d%n", p.x, p.y, p.val);
	} 
	pw.close();

	System.out.println("writing index-term map file termIndex.txt");

	pw = new PrintWriter("termIndex.txt");
	for (Map.Entry<String,Integer> e : wordToIndex.entrySet())
	    pw.printf("%05d\t%s%n", e.getValue().intValue(), e.getKey());
	pw.close();
    }

    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage: java OnTheFlyTermDocumentParser " + 
			       "<doc file> <output file>");
	}
	try {
	    new OnTheFlyTermDocumentParser().parseDocuments(args[0], args[1]);
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private static class Point implements Comparable<Point> {
	
	public final int x;
	public final int y;
	public final int val;

	public Point(int x, int y, int val) {
	    this.x = x;
	    this.y = y;
	    this.val = val;
	}

	public boolean equals(Object o) {
	    if (o instanceof Point) {
		Point p = (Point)o;
		return p.x == x && p.y == y && p.val == val;
	    }
	    return false;
	}

	public int compareTo(Point p) {
	    return (x == p.x) ? y - p.y : x - p.x;
	}

	public int hashCode() {
	    return x + y + val;
	}
    }

}