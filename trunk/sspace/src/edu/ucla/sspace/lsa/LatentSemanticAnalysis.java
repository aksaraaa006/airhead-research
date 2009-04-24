package edu.ucla.sspace.lsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOError;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import edu.ucla.sspace.common.BoundedSortedMap;
import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.SVD;

/**
 * An implementation of Latent Semantic Analysis (LSA).  This implementation is
 * based on two papers.
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., Foltz,
 *     P. W., & Laham, D. (1998).  Introduction to Latent Semantic
 *     Analysis. <i>Discourse Processes</i>, <b>25</b>, 259-284.  Available <a
 *     href="http://lsa.colorado.edu/papers/dp1.LSAintro.pdf">here</a> </li>
 * 
 * <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., and
 *    Dumais, S. T. (1997). A solution to Plato's problem: The Latent Semantic
 *    Analysis theory of the acquisition, induction, and representation of
 *    knowledge.  <i>Psychological Review</i>, <b>104</b>, 211-240.  Available
 *    <a href="http://lsa.colorado.edu/papers/plato/plato.annote.html">here</a>
 *    </li>
 *
 * </ul> See the Wikipedia page on <a
 * href="http://en.wikipedia.org/wiki/Latent_semantic_analysis"> Latent Semantic
 * Analysis </a> for an execuative summary.
 *
 * <p>
 * 
 * LSA first processes documents into a word-document matrix where each unique
 * word is a assigned a row in the matrix, and each column represents a
 * document.  The values of ths matrix correspond to the number of times the
 * row's word occurs in the column's document.  After the matrix has been built,
 * the <a
 * href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Singular
 * Value Decomposition</a> (SVD) is used to reduce the dimensionality of the
 * original word-document matrix, denoted as <span style="font-family:Garamond,
 * Georgia, serif">A</span>. The SVD is a way of factoring any matrix A into
 * three matrices <span style="font-family:Garamond, Georgia, serif">U &Sigma;
 * V<sup>T</sup></span> such that <span style="font-family:Garamond, Georgia,
 * serif"> &Sigma; </span> is a diagonal matrix containing the singular values
 * of <span style="font-family:Garamond, Georgia, serif">A</span>. The singular
 * values of <span style="font-family:Garamond, Georgia, serif"> &Sigma; </span>
 * are ordered according to which causes the most variance in the values of
 * <span style="font-family:Garamond, Georgia, serif">A</span>. The original
 * matrix may be approximated by recomputing the matrix with only <span
 * style="font-family:Garamond, Georgia, serif">k</span> of these singular
 * values and setting the rest to 0. The approximated matrix <span
 * style="font-family:Garamond, Georgia, serif"> &Acirc; = U<sub>k</sub>
 * &Sigma;<sub>k</sub> V<sub>k</sub><sup>T</sup></span> is the least squares
 * best-ﬁt rank-<span style="font-family:Garamond, Georgia, serif">k</span>
 * approximation of <span style="font-family:Garamond, Georgia, serif">A</span>.
 * LSA reduces the dimensions by keeping only the ﬁrst <span
 * style="font-family:Garamond, Georgia, serif">k</span> dimensions from the row
 * vectors of <span style="font-family:Garamond, Georgia, serif">U</span>.
 * These vectors form the <i>semantic space</i> of the words.
 *
 * <p>
 *
 * This class offer configurable preprocessing and dimensionality reduction.
 * through two parameters.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MATRIX_TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link LogEntropyTransformer}
 *
 * <dd style="padding-top: .5em">This variable sets the preprocessing algorithm
 *      to use on the term-document matrix prior to computing the SVD.  The
 *      property value should be teh fully qualified named of a class that
 *      implements {@link MatrixTransformer}.  The class should be public, not
 *      abstract, and should provide a public no-arg constructor.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value LSA_DIMENSIONS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code 300}
 *
 * <dd style="padding-top: .5em">The number of dimensions to use for the
 *       semantic space.  This value is used as input to the SVD.<p>
 *
 * </dl> <p>
 *
 * <p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  Once {@link
 * #processSpace(Properties) processSpace} has been called, no further calls to
 * {@code processDocument} should be made.  This implementation does not support
 * access to the semantic vectors until after {@code processSpace} has been
 * called.
 *
 * @see MatrixTransformer
 * @see SVD
 * 
 * @author David Jurgens
 */
public class LatentSemanticAnalysis implements SemanticSpace {

    public static final String MATRIX_TRANSFORM_PROPERTY =
	"edu.ucla.sspace.lsa.LatentSemanticAnalysis.transform";

    public static final String LSA_DIMENSIONS_PROPERTY =
	"edu.ucla.sspace.lsa.LatentSemanticAnalysis.dimensions";

    /**
     * A mapping from a word to the row index in the that word-document matrix
     * that contains occurrence counts for that word.
     */
    private final ConcurrentMap<String,Integer> termToIndex;

    /**
     * The counter for recording the current, largest word index in the
     * word-document matrix.
     */
    private final AtomicInteger termIndexCounter;

    /**
     * The counter for recording the current, largest document index in the
     * word-document matrix.
     */
    private final AtomicInteger docIndexCounter;
    
    /**
     * A file in {@link edu.ucla.sspace.common.MatrixIO.Format#MATLAB_SPARSE
     * MATLAB_SPARSE} format.
     */
    private final File rawTermDocMatrix;

    /**
     * The writer to the {@code rawTermDocMatrix}.
     */
    private final PrintWriter rawTermDocMatrixWriter;

    /**
     * The word space of the LSA model.  This matrix is only available after the
     * {@link #processSpace(Properties) processSpace} method has been called.
     */
    private Matrix wordSpace;

    /**
     * Constructs the {@code LatentSemanticAnalysis}.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public LatentSemanticAnalysis() throws IOException {

	termToIndex = new ConcurrentHashMap<String,Integer>();
	termIndexCounter = new AtomicInteger(0);
	docIndexCounter = new AtomicInteger(0);

	rawTermDocMatrix = 
	    File.createTempFile("lsa-term-document-matrix", "dat");
	rawTermDocMatrixWriter = new PrintWriter(rawTermDocMatrix);

	wordSpace = null;
    }   

    /**
     * Parses the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {

	Map<String,Integer> termCounts = 
	    new LinkedHashMap<String,Integer>(1 << 10, 16f);	

	int lineNum = 0;
	for (String line = null; (line = document.readLine()) != null; ) {
	    
	    // replace all non-word characters with whitespace.  We also include
	    // some uncommon characters (such as those with the eacute).  line =
	    // line.replaceAll("[^A-Za-z0-9'\u00E0-\u00FF]", " ").
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
	    return;

	int documentIndex = docIndexCounter.incrementAndGet();

	// Once the document has been fully parsed, output all of the sparse
	// data points using the writer.  Synchronize on the writer to prevent
	// any interleaving of output by other threads
	synchronized(rawTermDocMatrixWriter) {
	    for (Map.Entry<String,Integer> e : termCounts.entrySet()) {
		String term = e.getKey();
		int count = e.getValue().intValue();
		StringBuffer sb = new StringBuffer(32);
		sb.append(termToIndex.get(term).intValue()).append("\t").
		    append(documentIndex).append("\t").append(count);
		rawTermDocMatrixWriter.println(sb.toString());
	    }
	    
	    rawTermDocMatrixWriter.flush();
	}

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
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return Collections.unmodifiableSet(termToIndex.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String word) {
	// determine the index for the word
	Integer index = termToIndex.get(word);
	return (index == null)
	    ? null
	    : wordSpace.getRow(index.intValue());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     *
     * This method is thread-safe and may be called in parallel with separate
     * documents to speed up overall processing time.
     *
     * @param properties {@inheritDoc} See this class's {@link
     *        LatentSemanticAnalysis javadoc} for the full list of supported
     *        properties.
     */
    public void processSpace(Properties properties) {
	try {
	    // first ensure that we are no longer writing to the matrix
	    synchronized(rawTermDocMatrix) {
		rawTermDocMatrixWriter.close();
	    }

	    MatrixTransformer transform = new LogEntropyTransformer();

	    String transformClass = 
		properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
	    if (transformClass != null) {
		try {
		    Class clazz = Class.forName(transformClass);
		    transform = (MatrixTransformer)(clazz.newInstance());
		} 
		// perform a general catch here due to the number of possible
		// things that could go wrong.  Rethrow all exceptions as an
		// error.
		catch (Exception e) {
		    throw new Error(e);
		} 
	    }

	    // Convert the raw term counts using the specified transform
	    File processedTermDocumentMatrix = 
		transform.transform(rawTermDocMatrix);
	    
	    int dimensions = 300; // default
	    String userSpecfiedDims = 
		properties.getProperty(LSA_DIMENSIONS_PROPERTY);
	    if (userSpecfiedDims != null) {
		try {
		    dimensions = Integer.parseInt(userSpecfiedDims);
		} catch (NumberFormatException nfe) {
		    throw new IllegalArgumentException(
			LSA_DIMENSIONS_PROPERTY + " is not an integer: " +
			userSpecfiedDims);
		}
	    }

	    // Compute SVD on the pre-processed matrix.
	    Matrix[] usv = SVD.svd(processedTermDocumentMatrix, dimensions);
	    
	    // Load the left factor matrix, which is the word semantic space
	    wordSpace = usv[0];

	} catch (IOException ioe) {
	    //rethrow as Error
	    throw new IOError(ioe);
	}
    }
}
