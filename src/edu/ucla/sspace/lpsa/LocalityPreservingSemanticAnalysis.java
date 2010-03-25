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

package edu.ucla.sspace.lpsa;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.LocalityPreservingProjection;
import edu.ucla.sspace.matrix.LocalityPreservingProjection.EdgeType;
import edu.ucla.sspace.matrix.LocalityPreservingProjection.EdgeWeighting;
import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseIntHashArray;

import edu.ucla.sspace.vector.DoubleVector;
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
 * An implementation of Locality Preserving Semantic Analysis (LPSA).  This
 * implementation is based on the following paper.  <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> <i>forthcoming</i></li>
 *
 * </ul>
 *
 * <p>
 *
 * This class offers configurable preprocessing and dimensionality reduction.
 * through three parameters.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MATRIX_TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> none.
 *
 * <dd style="padding-top: .5em">This variable sets the preprocessing algorithm
 *      to use on the term-document matrix prior to computing the SVD.  The
 *      property value should be the fully qualified named of a class that
 *      implements {@link Transform}.  The class should be public, not abstract,
 *      and should provide a public no-arg constructor.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value LPSA_DIMENSIONS_PROPERTY}
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
 * @see Transform
 * @see LocalityPreservingProjection
 * @see LSA
 * 
 * @author David Jurgens
 */
public class LocalityPreservingSemanticAnalysis implements SemanticSpace {

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.lpsa.LocalityPreservingSemanticAnalysis";

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
    public static final String LPSA_DIMENSIONS_PROPERTY =
        PROPERTY_PREFIX + ".dimensions";

    public static final String LPSA_AFFINITY_EDGE_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeType";

    public static final String LPSA_AFFINITY_EDGE_PARAM_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeTypeParam";

    public static final String LPSA_AFFINITY_EDGE_WEIGHTING_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeWeighting";

    public static final String LPSA_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeWeightingParam";


    /**
     * The name prefix used with {@link #getName()}
     */
    private static final String LPSA_SSPACE_NAME =
        "lpsa-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(LocalityPreservingSemanticAnalysis.class.getName());

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
     * The word space of the LPSA model, which is the left factor matrix of the
     * SVD of the word-document matrix.  This matrix is only available after the
     * {@link #processSpace(Properties) processSpace} method has been called.
     */
    private Matrix wordSpace;
    
    /**
     * Constructs the {@code LocalityPreservingSemanticAnalysis} using the system properties
     * for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public LocalityPreservingSemanticAnalysis() throws IOException {
        this(System.getProperties());
    }

    /**
     * Constructs the {@code LocalityPreservingSemanticAnalysis} using the specified
     * properties for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public LocalityPreservingSemanticAnalysis(Properties properties) 
            throws IOException {
        termToIndex = new ConcurrentHashMap<String,Integer>();
        termIndexCounter = new AtomicInteger(0);

        // Use the transposed SVDLIBC sparse builder since the LPP uses that
        // format internally for its initial constructions.  In this format the
        // data rows will become the columns of the matrix.
        termDocumentMatrixBuilder = new SvdlibcSparseBinaryMatrixBuilder(true);

        wordSpace = null;
    }   

    /**
     * Parses the document.
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
            if (word.equals(IteratorFactory.EMPTY_TOKEN))
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
        if (termCounts.isEmpty())
            return;

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
                    index = Integer.valueOf(termIndexCounter.getAndIncrement());
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
            : wordSpace.getRowVector(index.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return LPSA_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return wordSpace.columns();
    }

    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link
     *        LocalityPreservingSemanticAnalysis javadoc} for the full list of
     *        supported properties.
     */
    public void processSpace(Properties properties) {
        try {
            // first ensure that we are no longer writing to the matrix
            termDocumentMatrixBuilder.finish();
            // Get the finished matrix file from the builder
            File termDocumentMatrix = termDocumentMatrixBuilder.getFile();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("stored term-document matrix in format " + 
                        termDocumentMatrixBuilder.getMatrixFormat()
                        + " at " + termDocumentMatrix.getAbsolutePath());
            }

            // By default, do no tranformation on the data prior to LPP.
            File transformedMatrix = termDocumentMatrix;

            // If the user specified a transform, then apply it and update the
            // matrix file
            String transformClass = 
                properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
            if (transformClass != null) {
                Transform transform  = null;
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

                LOGGER.info("performing " + transform + " transform");
                

                // Convert the raw term counts using the specified transform
                transformedMatrix = transform.transform(termDocumentMatrix, 
                    termDocumentMatrixBuilder.getMatrixFormat());

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("transformed matrix to " + 
                                transformedMatrix.getAbsolutePath());
                }
            }

            // Set all of the default properties
            int dimensions = 300; 
            EdgeType edgeType = EdgeType.NEAREST_NEIGHBORS;
            double edgeTypeParam = 20;
            EdgeWeighting weighting = EdgeWeighting.COSINE_SIMILARITY;
            double edgeWeightParam = 0; // unused with default weighting

            // Then load any of the user-specified properties
            String dimensionsProp = 
                properties.getProperty(LPSA_DIMENSIONS_PROPERTY);
            if (dimensionsProp != null) {
                try {
                    dimensions = Integer.parseInt(dimensionsProp);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                        LPSA_DIMENSIONS_PROPERTY + " is not an integer: " +
                        dimensionsProp);
                }
            }

            String edgeTypeProp = 
                properties.getProperty(LPSA_AFFINITY_EDGE_PROPERTY);
            if (edgeTypeProp != null) 
                edgeType = EdgeType.valueOf(edgeTypeProp.toUpperCase());
            String edgeTypeParamProp = 
                properties.getProperty(LPSA_AFFINITY_EDGE_PARAM_PROPERTY);
            if (edgeTypeParamProp != null) {
                try {
                    edgeTypeParam = Double.parseDouble(edgeTypeParamProp);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                        LPSA_AFFINITY_EDGE_PARAM_PROPERTY + 
                        " is not an double: " + edgeTypeParamProp);
                }
            }

            String edgeWeightingProp = 
                properties.getProperty(LPSA_AFFINITY_EDGE_WEIGHTING_PROPERTY);
            if (edgeWeightingProp != null) 
                weighting = EdgeWeighting.valueOf(
                    edgeWeightingProp.toUpperCase());
            String edgeWeightingParamProp = properties.getProperty(
                LPSA_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY);
            if (edgeWeightingParamProp != null) {
                try {
                    edgeWeightParam = Double.parseDouble(edgeWeightingParamProp);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                        LPSA_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY + 
                        " is not an double: " + edgeWeightingParamProp);
                }
            }

            LOGGER.info("reducing to " + dimensions + " dimensions");
            File tiMap = new File("term-index.map");
            PrintWriter pw = new PrintWriter(tiMap);
//             edu.ucla.sspace.util.SerializableUtil.save(termToIndex, tiMap);
            for (Map.Entry<String,Integer> e : termToIndex.entrySet())
                pw.println(e.getKey() + "\t" + e.getValue());
            pw.close();
            LOGGER.info("wrote term-index map to " + tiMap);

            
            Matrix termDocMatrix = MatrixIO.readMatrix(
                transformedMatrix, 
                termDocumentMatrixBuilder.getMatrixFormat(), 
                Matrix.Type.SPARSE_IN_MEMORY, true);
            wordSpace = LocalityPreservingProjection.project(
                 termDocMatrix,
                 dimensions,
                 Similarity.SimType.COSINE, 
                 edgeType, edgeTypeParam, weighting, edgeWeightParam);
            
            /*
            File projectedFile = LocalityPreservingProjection.project(
                transformedMatrix, 
                termDocumentMatrixBuilder.getMatrixFormat(),
                true, // the matrix is written so the data points are columns
                dimensions,
                Similarity.SimType.COSINE, 
                edgeType, edgeTypeParam, weighting, edgeWeightParam);
                
            // Load the word space as sparse in memory
            wordSpace = MatrixIO.readMatrix(projectedFile, Format.DENSE_TEXT);
            */

        } catch (IOException ioe) {
            //rethrow as Error
            throw new IOError(ioe);
        }
    }
}
