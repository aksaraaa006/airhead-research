/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.vsm;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseIntHashArray;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of the <a
 * href="http://en.wikipedia.org/wiki/Vector_space_model">Vector Space Model</a>
 * (VSM).  This model was first based on the paper <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> G. Salton, A. Wong, and
 *     C. S. Yang (1975), "A Vector Space Model for Automatic Indexing,"
 *     Communications of the ACM, vol. 18, nr. 11, pages 613–620.  Available <a
 *     href="http://doi.acm.org/10.1145/361219.361220">here</a> </li>
 *
 * </ul>
 *
 * <p>
 * 
 * The VSM first processes documents into a word-document matrix where each
 * unique word is a assigned a row in the matrix, and each column represents a
 * document.  The values of ths matrix correspond to the number of times the
 * row's word occurs in the column's document.  Optionally, after the matrix has
 * been completely, its values may be transformed.  This is frequently done
 * using the {@link edu.ucla.sspace.matrix.TfIdfTransform Tf-Idf Transform}.
 *
 * <p>
 *
 * This class offers one configurable parameter.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MATRIX_TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> none
 *
 * <dd style="padding-top: .5em">This variable sets the preprocessing algorithm
 *      to use on the term-document matrix.  The property value should be the
 *      fully qualified named of a class that implements {@link Transform}.  The
 *      class should be public, not abstract, and should provide a public no-arg
 *      constructor.<p>
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
 * @see Transform
 * 
 * @author David Jurgens
 */
public class VectorSpaceModel implements SemanticSpace, Filterable {

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.vsm.VectorSpaceModel";

    /**
     * The property to define the {@link Transform} class to be used
     * when processing the space after all the documents have been seen.
     */
    public static final String MATRIX_TRANSFORM_PROPERTY =
        PROPERTY_PREFIX + ".transform";

    /**
     * The name prefix used with {@link #getName()}
     */
    private static final String VSM_SSPACE_NAME =
        "vector-space-model";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(VectorSpaceModel.class.getName());

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
     * The builder used to construct the term-document matrix as new documents
     * are processed.
     */
    private final MatrixBuilder termDocumentMatrixBuilder;

    /**
     * The vector space of the VSM model. This matrix is only available after the
     * {@link #processSpace(Properties) processSpace} method has been called.
     */
    private Matrix vectorSpace;
    
    private Set<String> acceptedWords;

    /**
     * Constructs the {@code VectorSpaceModel} using the system properties
     * for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public VectorSpaceModel() throws IOException {
        this(System.getProperties());
    }

    /**
     * Constructs the {@code VectorSpaceModel} using the specified
     * properties for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public VectorSpaceModel(Properties properties) throws IOException {
        termToIndex = new ConcurrentHashMap<String,Integer>();
        termIndexCounter = new AtomicInteger(0);
        termDocumentMatrixBuilder = Matrices.getMatrixBuilderForSVD();
        vectorSpace = null;
        acceptedWords = null;
    }   

    /**
     * {@inheritDoc}
     *
     * <p>
     *
     * This method is thread-safe and may be called in parallel with separate
     * documents to speed up overall processing time.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        // Create a mapping for each term that is seen in the document to the
        // number of times it has been seen.  This mapping would more elegantly
        // be a SparseArray<Integer> however, the length of the sparse array
        // isn't known ahead of time, which prevents it being used by the
        // MatrixBuilder.  Note that the SparseArray implementation would also
        // incur an additional performance hit since each word would have to be
        // converted to its index form for each occurrence, which results in a
        // double Map look-up.
        Map<String,Integer> termCounts = new HashMap<String,Integer>(1000);
        Iterator<String> documentTokens = IteratorFactory.tokenize(document);

        // for each word in the text document, keep a count of how many times it
        // has occurred
        while (documentTokens.hasNext()) {
            String word = documentTokens.next();
            
            // Skip added empty tokens for words that have been filtered out
            if (!acceptWord(word))
                continue;
            
            // Add the term to the total list of terms to ensure it has a proper
            // index.  If the term was already added, this method is a no-op
            addTerm(word);
            Integer termCount = termCounts.get(word);

            // update the term count
            termCounts.put(word, (termCount == null) 
                           ? 1
                           : 1 + termCount.intValue());
        }

        document.close();

        // Check that we actually loaded in some terms before we increase the
        // documentIndex.  This could possibly save some dimensions in the final
        // array for documents that were essentially blank.  If we didn't see
        // any terms, just perform no updates. 
        //if (termCounts.isEmpty())
        //    return;

        // Get the total number of terms encountered so far, including any new
        // unique terms found in the most recent document
        int totalNumberOfUniqueWords = termIndexCounter.get();

        // Convert the Map count to a SparseArray
        SparseArray<Integer> documentColumn = 
            new SparseIntHashArray(totalNumberOfUniqueWords);
        for (Map.Entry<String,Integer> e : termCounts.entrySet())
            documentColumn.set(termToIndex.get(e.getKey()), e.getValue());

        // Update the term-document matrix with the results of processing the
        // document.
        termDocumentMatrixBuilder.addColumn(documentColumn);
    }
    
    public void setSemanticFilter(Set<String> words) {
        acceptedWords = words;
    }

    private boolean acceptWord(String word) {
        return !word.equals(IteratorFactory.EMPTY_TOKEN) && 
               (acceptedWords == null || acceptedWords.contains(word));
    }

    /**
     * Adds the term to the list of terms and gives it an index, or if the term
     * has already been added, does nothing.
     */
    private void addTerm(String term) {
        Integer index = termToIndex.get(term);
        if (index == null) {
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termToIndex.get(term);
                // if some other thread has not already added this term while
                // the current thread was blocking waiting on the lock, then add
                // it.
                if (index == null) {
                    index = termIndexCounter.getAndIncrement();
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
    public Vector getVector(String word) {
        // determine the index for the word
        Integer index = termToIndex.get(word);
        
        return (index == null)
            ? null
            : vectorSpace.getRowVector(index);
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
     * original document ordering as it exists within the corpus files.
     * However, in a single-threaded environment, the original ordering will be
     * preserved.
     *
     * @param documentNumber the number of the document according to when it was
     *        processed
     *
     * @return the semantics of the document in the document space
     */
    public DoubleVector getDocumentVector(int documentNumber) {
        if (documentNumber < 0 || documentNumber >= vectorSpace.columns()) {
            throw new IllegalArgumentException(
                    "Document number is not within the bounds of the number of "
                    + "documents: " + documentNumber);
        }
        return vectorSpace.getColumnVector(documentNumber);
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return VSM_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorSpace.columns();
    }

    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link VectorSpaceModel
     *        javadoc} for the full list of supported properties.
     */
    public void processSpace(Properties properties) {
        try {
            // first ensure that we are no longer writing to the matrix
            termDocumentMatrixBuilder.finish();

            Transform transform = null;

            // Load any optionally specifie transform class
            String transformClass = 
                properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
            if (transformClass != null) {
                try {
                    Class clazz = Class.forName(transformClass);
                    transform = (Transform)(clazz.newInstance());
                } 
                // perform a general catch here due to the number of possible
                // things that could go wrong.  Rethrow all exceptions as an
                // error.
                catch (Exception e) {
                    throw new Error(e);
                } 
            }

            File termDocumentMatrix = termDocumentMatrixBuilder.getFile();
            MatrixIO.Format format = 
                termDocumentMatrixBuilder.getMatrixFormat();

            // If the user specified a transform to perform on the t-d matrix,
            // apply it and set the vector space to the result.
            if (transform != null) {
                LOGGER.info("performing " + transform + " transform");
                
                // Get the finished matrix file from the builder

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("stored term-document matrix in format " + 
                                    termDocumentMatrixBuilder.getMatrixFormat()
                                    + " at " + 
                                    termDocumentMatrix.getAbsolutePath());
                }
                
                // Convert the raw term counts using the specified transform
                File transformedMatrix = transform.transform(
                    termDocumentMatrix, format);
                
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("transformed matrix to " + 
                                    transformedMatrix.getAbsolutePath());
                }
                vectorSpace = MatrixIO.readMatrix(transformedMatrix, format);
            }
            // Otherwise, if the user did not specify any transform, set the
            // vector space directly as the the raw t-d matrix.
            else {
                vectorSpace = MatrixIO.readMatrix(termDocumentMatrix, format);
            }            

            PrintWriter writer = new PrintWriter("vsm-docspace-matlab.mat");
            if (vectorSpace instanceof SparseMatrix) {
                SparseMatrix sm = (SparseMatrix) vectorSpace;
                for (int row = 0; row < sm.rows(); ++row) {
                    SparseDoubleVector sdv = sm.getRowVector(row);
                    for (int col : sdv.getNonZeroIndices())
                        writer.printf("%d %d %f\n", col+1, row+1, sm.get(row, col));
                }
            }
            writer.close();
        } catch (IOException ioe) {
            //rethrow as Error
            throw new IOError(ioe);
        }

    }
}
