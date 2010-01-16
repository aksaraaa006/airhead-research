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

package edu.ucla.sspace.rlsa;

import edu.ucla.sspace.clustering.ClutoClustering;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.RowMaskedMatrix;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseHashIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;


import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of Reflective Random Indexing, which uses a two passes
 * through the corpus to build semantic vectors that better approximate indirect
 * co-occurrence.  This implementation is based on the paper: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"></li>
 *
 * </ul>
 *
 * <p>
 *
 * This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * ReflectiveLatentSemanticAnalysis#ReflectiveLatentSemanticAnalysis(Properties)} constructor.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #VECTOR_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_VECTOR_LENGTH}
 *
 * <dd style="padding-top: .5em">This property sets the number of dimensions to
 *      be used for the index and semantic vectors. <p>
 *
 * </dl> <p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  The {@link
 * #getVector(String) getVector} method will only return valid reflective
 * vectors after the call to {@link #processSpace(Properties) processSpace}. <p>
 *
 * @author David Jurgens
 */
public class ReflectiveLatentSemanticAnalysis implements SemanticSpace {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.ri.ReflectiveLatentSemanticAnalysis";

    /**
     * The property to define the {@link Transform} class to be used
     * when processing the space after all the documents have been seen.
     */
    public static final String MATRIX_TRANSFORM_PROPERTY =
        PROPERTY_PREFIX + ".transform";

    /**
     * The property to set the number of dimension to which the space should be
     * reduced using the SVD
     */
    public static final String RLSA_DIMENSIONS_PROPERTY =
        PROPERTY_PREFIX + ".dimensions";

    /**
     * The property to set the specific SVD algorithm used by an instance during
     * {@code processSpace}.  The value should be the name of a {@link
     * edu.ucla.sspace.matrix.SVD.Algorithm}.  If this property is unset, any
     * available algorithm will be used according to the ordering defined in
     * {@link SVD}.
     */
    public static final String RLSA_SVD_ALGORITHM_PROPERTY = 
        PROPERTY_PREFIX + ".svd.algorithm";

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 300;

    /**
     * The name returned by {@code getName}.
     */
    private static final String RLSA_SSPACE_NAME =
        "reflective-lsa";

    /**
     * The internal logger used for tracking processing progress.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(ReflectiveLatentSemanticAnalysis.class.getName());

    /**
     * A mapping from a each term to its index in the term-document matrix
     */
    private final ConcurrentMap<String,Integer> termToIndex;

    /**
     * A mapping from a each term to its index in the sense-induced
     * term-document matrix
     */
    private final ConcurrentMap<String,Integer> termToSenseIndex;

    /**
     * A counter for the number of documents seen in the corpus.
     */
    private final AtomicInteger documentCounter;

    /**
     * The builder used to construct the term-document matrix as new documents
     * are processed.
     */
    private final MatrixBuilder termDocumentMatrixBuilder;

    /**
     * The builder used to construct the transposed term-document matrix as new
     * documents are processed.  This matrix is used to keep track of which
     * documents the terms are in for reflective processing in {@link
     * #processSpace(Properties)}.
     */
    private final MatrixBuilder docTermMatrixBuilder;

    /**
     * The number that keeps track of the index values of words.
     */
    private int termIndexCounter;

    /**
     * The word space of the RLSA model, which is the left factor matrix of the
     * SVD of the word-document matrix.  This matrix is only available after the
     * {@link #processSpace(Properties) processSpace} method has been called.
     */
    private Matrix wordSpace;

    /**
     * The document space of the RLSA model, which is the right factor matrix of
     * the SVD of the word-document matrix.  This matrix is only available after
     * the {@link #processSpace(Properties) processSpace} method has been
     * called.
     */
    private Matrix documentSpace;
    

    /**
     * Creates a new {@code ReflectiveLatentSemanticAnalysis} instance using the
     * current {@code System} properties for configuration.
     */
    public ReflectiveLatentSemanticAnalysis() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code ReflectiveLatentSemanticAnalysis} instance using the
     * provided properites for configuration.
     */
   public ReflectiveLatentSemanticAnalysis(Properties properties) {

        // The various maps for keeping word and document state during
        // processing
        termToIndex = new ConcurrentHashMap<String,Integer>();
        termToSenseIndex = new ConcurrentHashMap<String,Integer>();
        documentCounter = new AtomicInteger();

        termDocumentMatrixBuilder = Matrices.getMatrixBuilderForSVD();
        // This build is used entirely for internal (non-SVD) purposes, so
        // manually create it since we want control of the data format.
        docTermMatrixBuilder = new SvdlibcSparseBinaryMatrixBuilder(true);
    }

    /**
     * Returns the index vector for the term, or if creates one if the term to
     * index vector mapping does not yet exist.
     *
     * @param term a word in the semantic space
     *
     * @return the index for the provide term.
     */
    private Integer getTermIndex(String term) {
        Integer i = termToIndex.get(term);
        if (i == null) {
            // lock in case multiple threads attempt to add it at once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                i = termToIndex.get(term);
                if (i == null) {
                    // since this is a new term, also map it to its index for
                    // later look-up when the integer documents are processed
                    i = termIndexCounter++;
                    termToIndex.put(term, i);
                }
            }
        }
        return i;
    }

   /**
     * {@inheritDoc}
     */ 
    public Vector getVector(String word) {
        Integer index = termToSenseIndex.get(word);
        return (index == null)
            ? null
            : Vectors.immutable(wordSpace.getRowVector(index));
    }

    /**
     * Returns the semantics of the document as represented by a numeric vector.
     * Note that document semantics are represented in an entirely different
     * space, so the corresponding semantic dimensions in the word space will be
     * completely unrelated.  However, document vectors may be compared to find
     * those document with similar content.<p>
     *
     * Similar to {@code getVector}, this method is only to be used after
     * {@code processSpace} has been called.<p>
     *
     * Implementation note: If a specific document ordering is needed, caution
     * should be used when using this class in a multi-threaded environment.
     * Beacuse the document number is based on what order it was
     * <i>processed</i>, no guarantee is made that this will correspond with the
     * original document ordering as it exists in the corpus files.  However, in
     * a single-threaded environment, the ordering will be preserved.
     *
     * @param documentNumber the number of the document according to when it was
     *        processed
     *
     * @return the semantics of the document in the document space
     */
    public DoubleVector getDocumentVector(int documentNumber) {
        if (documentNumber < 0 || documentNumber >= documentSpace.rows()) {
            throw new IllegalArgumentException(
                    "Document number is not within the bounds of the number of "
                    + "documents: " + documentNumber);
        }
        return documentSpace.getRowVector(documentNumber);
    }

    /**
     * {@inheritDoc}
     */ 
    public String getSpaceName() {
        return RLSA_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return wordSpace.columns();
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToSenseIndex.keySet());
    }
    
    /**
     * Updates the semantic vectors based on the words in the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        int docIndex = documentCounter.getAndIncrement();

        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document);
        
        // Initially overstate the the size since we don't know the final number
        // of unique tokens until after the document has finished processing
        IntegerVector tokenCounts = 
            new SparseHashIntegerVector(Integer.MAX_VALUE); 

        // for each word in the text document, keep a count of how many times it
        // has occurred
        while (documentTokens.hasNext()) {
            String term = documentTokens.next();
            
            // Skip added empty tokens for words that have been filtered out
            if (term.equals(IteratorFactory.EMPTY_TOKEN))
                continue;
            
            // update the term count
            Integer index = getTermIndex(term);
            tokenCounts.add(index, 1);
        }

        // Alter the length of the vector using a subview to indicate the actual
        // number of unique words (rows)
        Vector docView = Vectors.subview(tokenCounts, 0, termIndexCounter);
        
        // Update the term-document matrix with the results of processing the
        // document.
        termDocumentMatrixBuilder.addColumn(docView);
        // Update the doc-term matrix as well.  Note that this matrix is
        // transposed so no changes need to be made to the column in order to
        // turn it into a row
        docTermMatrixBuilder.addColumn(docView);
    }
    

    /**
     * Computes the reflective semantic vectors for word meanings
     */
    public void processSpace(Properties properties) {
        try {
            // First ensure that we are no longer writing to the matrices
            termDocumentMatrixBuilder.finish();
            docTermMatrixBuilder.finish();

            // Generate the term and document spaces
            Matrix[] usv = transformAndSvd(properties, 
                                           termDocumentMatrixBuilder);

            // Load the left factor matrix, which is the word semantic space
            wordSpace = usv[0];
            // We transpose the document space to provide row access to the
            // document vectors, which in the un-transposed version are the
            // columns.
            documentSpace = Matrices.transpose(usv[2]);

            // This is the counter for the rows in the sense-induced
            // term-document matrix.
            int termSenseRowCounter = 0;

            String[] indexToTerm = new String[termToIndex.size()];
            for (Map.Entry<String,Integer> e : termToIndex.entrySet())
                indexToTerm[e.getValue()] = e.getKey();

            // Generate another matrix builder that will contain the
            // sense-induced sense-document matrix.  Each term will have its
            // documents clustered to discover the similarities.  Then each
            // document cluster will be assigned a column in this matrix.
            MatrixBuilder termSenseDocMatrixBuilder =
                 Matrices.getMatrixBuilderForSVD();
            
            // Once the spaces are created, reprocess the corpus and attempt to
            // distinguish between different meanings of words by clustering the
            // documents in which the words appear
            DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(
                    docTermMatrixBuilder.getFile())));

            // Read off the 12 byte header.
            int numDocs = dis.readInt();
            int numUniqueTerms = dis.readInt();  
            int numMatrixEntries = dis.readInt();

            // For each token, find out how many document it appears in, and
            // then cluster those documents
            int termIndex = 0;
            int entriesSeen = 0;
            for (; entriesSeen < numMatrixEntries; ++termIndex) {

                String term = indexToTerm[termIndex];

                int docsWithTerm = dis.readInt();
                Map<Integer,Integer> selectedDocs = 
                    new HashMap<Integer,Integer>(numDocs);
                for (int i = 0; i < docsWithTerm; ++i, ++entriesSeen) {
                    int docIndex = dis.readInt();
                    // occurrence is specified as a float, rather than an int
                    int occurrences = (int)(dis.readFloat());
                    selectedDocs.put(docIndex, occurrences);
                }

                // Generate a subview of the document space that includes only
                // those documents that had the current term
                Matrix docsToCluster = 
                    new RowMaskedMatrix(documentSpace, selectedDocs.keySet());

                LOGGER.log(Level.FINE, "Clustering {0} documents that contained "
                           + "\"{1}\"", 
                           new Object[] { selectedDocs.size(), term});
                
                // Cluster the documents with the term into groupings to
                // identify which documents use the term in a similar manner
                int[] docGroupings = groupDocs(docsToCluster);

                // Determine the final number of groups
                int numGroups = 0;
                for (int i : docGroupings)
                    if (i + 1 > numGroups)
                        numGroups = i + 1;

                LOGGER.log(Level.FINE, "Discovered {0} different document " +
                           "senses for {1}", new Object[] {numGroups, term});
                
                // Rewrite the original row as a series of rows in the new
                // matrix, with one per word sense
                for (int wordSense = 0; wordSense < numGroups; ++wordSense) {
                    IntegerVector docsWithSense =
                        new SparseHashIntegerVector(numDocs);                    
                    for (int i = 0; i < docGroupings.length; ++i) {
                        if (docGroupings[i] == wordSense)
                            docsWithSense.set(i, selectedDocs.get(i));
                    }
                    termSenseDocMatrixBuilder.addColumn(docsWithSense);
                    
                    String senseTerm = (wordSense > 0) 
                        ? term + "-" + wordSense
                        : term;
                    termToSenseIndex.put(senseTerm, termSenseRowCounter++);
                }
            }
            indexToTerm = null;

            // Once all of the terms have been reprocessed, finish the matrix
            // and compute its svd
            termSenseDocMatrixBuilder.finish();
            usv = transformAndSvd(properties, termSenseDocMatrixBuilder);

            // Reassign the word and document spaces based on the new vectors
            wordSpace = usv[0];
            documentSpace = Matrices.transpose(usv[2]);
            
        } catch (IOException ioe) {
            //rethrow as Error
            throw new IOError(ioe);
        }
    }

    /**
     * Groups the docs into clusters and returns the group assignment for each
     * row in the {@code docs} matrix.
     */
    private int[] groupDocs(Matrix docs) {
        int numRows = docs.rows();
        int numClusters = 20;
        // For matrices with fewer rows than the number of clusters, assign all
        // the documents to the same cluster
        if (numRows < numClusters) {            
            return new int[docs.rows()];
        }
        
        // Initially have Cluto cluster the documents
        int[] initial = ClutoClustering.agglomerativeCluster(docs, numClusters);

        int[] clusterCounts = new int[numClusters];
        for (int i : initial) {
            // Cluto assigned -1 to some vectors if they can't be clustered
            if (i > 0)
                clusterCounts[i]++;
        }
        
        // Prune out any clusters that contain fewer than 10% of the documents
        BitSet pruned = new BitSet(numClusters);
        for (int i = 0; i < clusterCounts.length; ++i) {
            if (clusterCounts[i] / (double)numRows < 0.10)
                pruned.set(i);
        }

        // Remap the unpruned indices to a contiguous term count
        int offset = 0;
        int[] remapping = new int[numClusters];
        for (int i = 0; i < remapping.length; ++i) {
            if (pruned.get(i))
                offset++;
            else
                remapping[i] = i - offset;
        }

        int unprunedClusters = numClusters - pruned.cardinality();

        // Reassign any unclustered or pruned documents to another cluster
        for (int i = 0; i < initial.length; ++i) {
            int assignment = initial[i];
            // If the cluster to which this document was assigned was pruned,
            // then randomly reassign it to a different clusters; otherwise,
            // re-assign it using the compacted cluster indices.
            //
            // NOTE: it might be useful to assign randomly clusters in such a
            // way that maintains the original distribution (of the unpruned
            // clusters).
            int newCluster = (assignment < 0 || pruned.get(assignment))
                ? (int)(Math.random() * unprunedClusters)
                : remapping[assignment];
            
            initial[i] = newCluster;
        }

        return initial;
    }

    /**
     * Transforms the matrix in the provided build and then runs the {@link
     * Matrix Matrices} from the {@link Svd} computation on that matrix.
     */
    private Matrix[] transformAndSvd(Properties properties, 
                                     MatrixBuilder builder) throws IOException {
        Transform transform = new LogEntropyTransform();
        
        String transformClass = 
            properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
        if (transformClass != null) {
            try {
                Class clazz = Class.forName(transformClass);
                transform = (Transform)(clazz.newInstance());
            } 
            // perform a general catch here due to the number of possible things
            // that could go wrong.  Rethrow all exceptions as an error.
            catch (Exception e) {
                throw new Error(e);
            } 
        }

        LOGGER.info("performing " + transform + " transform");
            
        // Get the finished matrix file from the builder
        File termDocumentMatrix = builder.getFile();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("stored matrix in format " + 
                            builder.getMatrixFormat()
                            + " at " + termDocumentMatrix.getAbsolutePath());
        }
        
        // Convert the raw term counts using the specified transform
        File transformedMatrix = transform.transform(termDocumentMatrix, 
            builder.getMatrixFormat());
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("transformed matrix to " + 
                            transformedMatrix.getAbsolutePath());
        }
        
        int dimensions = 300; // default
        String userSpecfiedDims = 
            properties.getProperty(RLSA_DIMENSIONS_PROPERTY);
        if (userSpecfiedDims != null) {
            try {
                dimensions = Integer.parseInt(userSpecfiedDims);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                    RLSA_DIMENSIONS_PROPERTY + " is not an integer: " +
                    userSpecfiedDims);
            }
        }
        
        LOGGER.info("reducing to " + dimensions + " dimensions");
        
        // Determine whether the user specified any specific SVD algorithm
        // or whether the fastest available should be used.
        String svdProp = properties.getProperty(RLSA_SVD_ALGORITHM_PROPERTY);
        SVD.Algorithm alg = (svdProp == null)
            ? SVD.Algorithm.ANY
            : SVD.Algorithm.valueOf(svdProp);
        
        // Compute SVD on the pre-processed matrix.
        Matrix[] usv = SVD.svd(transformedMatrix, alg,
                               builder.getMatrixFormat(),
                               dimensions);
        return usv;
    }

    /**
     * Atomically adds the values of the index vector to the semantic vector.
     * This is a special case addition operation that only iterates over the
     * non-zero values of the index vector.
     */
    private static void add(IntegerVector semantics, TernaryVector index) {
        // Lock on the semantic vector to avoid a race condition with another
        // thread updating its semantics.  Use the vector to avoid a class-level
        // lock, which would limit the concurrency.
        synchronized(semantics) {
            for (int p : index.positiveDimensions())
                semantics.add(p, 1);
            for (int n : index.negativeDimensions())
                semantics.add(n, -1);
        }
    }
}
