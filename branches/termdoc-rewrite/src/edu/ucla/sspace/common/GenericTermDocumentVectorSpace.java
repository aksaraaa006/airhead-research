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

package edu.ucla.sspace.common;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.SVD;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * LSA first processes documents into a word-document matrix where each unique
 * word is a assigned a row in the matrix, and each column represents a
 * document.  The values of ths matrix correspond to the number of times the
 * row's word occurs in the column's document.  After the matrix has been built,
 * the <a
 * href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Singular
 * Value Decomposition</a> (SVD) may be used to reduce the dimensionality of the
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
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  Once {@link
 * #processSpace(Transform, SVD.Algorithm, int, boolean) processSpace} has been
 * called, no further calls to {@code processDocument} should be made.  This
 * implementation does not support access to the semantic vectors until after
 * {@code processSpace} has been
 * called.
 *
 * @see Transform
 * @see SVD
 * 
 * @author David Jurgens
 */
public abstract class GenericTermDocumentVectorSpace 
        extends LoggedSemanticSpace implements SemanticSpace {

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
     * The counter for recording the current number of documents observed.
     */
    private final AtomicInteger documentCounter;

    /**
     * The builder used to construct the term-document matrix as new documents
     * are processed.
     */
    private final MatrixBuilder termDocumentMatrixBuilder;

    /**
     * If true, the first token in each document is considered to be a document
     * header.
     */
    private final boolean readHeaderToken;

    /**
     * The word space of the term document based word space model.  If the word
     * space is reduced, it is the left factor matrix of the SVD of the
     * word-document matrix.  This matrix is only available after the {@link
     * #processSpace(Transform, SVD.Algorithm, int, boolean) processSpace}
     * method has been called.
     */
    private Matrix wordSpace;

    /**
     * The document space of the term document based word space If the word
     * space is reduced.  After reduction it is the right factor matrix of the
     * SVD of the word-document matrix.  This matrix is only available after the
     * {@link #processSpace(Transform, SVD.Algorithm, int, boolean)
     * processSpace} method has been called.
     */
    private Matrix documentSpace;
    
    /**
     * Constructs the {@code GenericTermDocumentVectorSpace}.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public GenericTermDocumentVectorSpace() throws IOException {
        this(false, new ConcurrentHashMap<String, Integer>());
    }

    /**
     * Constructs the {@code GenericTermDocumentVectorSpace} using the specified
     * properties for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public GenericTermDocumentVectorSpace(
            boolean readHeaderToken,
            ConcurrentMap<String, Integer> termToIndex) throws IOException {
        this.readHeaderToken = readHeaderToken;
        this.termToIndex = termToIndex;
        termIndexCounter = new AtomicInteger(0);
        documentCounter = new AtomicInteger(0);

        termDocumentMatrixBuilder = Matrices.getMatrixBuilderForSVD();

        wordSpace = null;
        documentSpace = null;
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

        // Increaes the count of documents observed so far.
        documentCounter.addAndGet(1);

        // If the document is empty, skip it
        if (!documentTokens.hasNext())
            return;

        // If the first token is to be interpreted as the title read it.
        // TODO: Use this document header as a descriptor for each dimension.
        if (readHeaderToken)
            documentTokens.next();

        // For each word in the text document, keep a count of how many times it
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
        // documentIndex. This is done after increasing the document count since
        // some configurations may need the document order preserved, for
        // example, if each document corresponds to some cluster assignment.
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
     * TODO: Replace this with a basis mapping.
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
     * Returns the semantics of the document as represented by a numeric vector.
     * Note that document semantics may be represented in an entirely different
     * space, so the corresponding semantic dimensions in the word space will be
     * completely unrelated.  However, document vectors may be compared to find
     * those document with similar content.
     *
     * </p>
     *
     * Similar to {@code getVector}, this method is only to be used after {@code
     * processSpace} has been called.  By default, the document space is not
     * retained unless {@code retainDocumentSpace} is set to true.
     *
     * </p>
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
     * @return the semantics of the document in the document space.
     * @throws IllegalArgumentException If the document space was not retained
     *         or the document number is out of range.
     */
    public DoubleVector getDocumentVector(int documentNumber) {
        if (documentSpace == null)
            throw new IllegalArgumentException(
                    "The document space has not been retained or generated.");

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
    public int getVectorLength() {
        return wordSpace.columns();
    }

    /**
     * Processes the {@link GenericTermDocumentVectorSpace} with the provided
     * {@link SemanticSpace} modifying objects.
     *
     * @param transform A matrix transform used to rescale the original raw
     *        document counts.  If {@code null} no transform is done.
     * @param alg The {@link SVD#Algorithm} for reducing the scaled term
     *        document space.  If {@code null} no reduction is done.
     * @param dimensions The reduced number of dimensions requsted. {@code 0},
     *        no reduction is done. 
     * @param retainDocumentSpace If true, the reduced document space is saved.
     *        Vectors in this space can later be accessible by {@link
     *        #getDocumentVector}.
     */
    protected void processSpace(Transform transform,
                                SVD.Algorithm alg,
                                int dimensions,
                                boolean retainDocumentSpace) {
        try {
            // first ensure that we are no longer writing to the matrix
            termDocumentMatrixBuilder.finish();

            // Get the finished matrix file from the builder
            File termDocumentMatrix = termDocumentMatrixBuilder.getFile();

            // If a transform was specified, perform the matrix transform.
            if (transform != null) {
                info("performing %s transform", transform);

                verbose("stored term-document matrix in format %s at %s",
                        termDocumentMatrixBuilder.getMatrixFormat(),
                        termDocumentMatrix.getAbsolutePath());

                // Convert the raw term counts using the specified transform
                termDocumentMatrix = transform.transform(
                        termDocumentMatrix, 
                        termDocumentMatrixBuilder.getMatrixFormat());

                verbose("transformed matrix to %s",
                        termDocumentMatrix.getAbsolutePath());
            }
            
            // If no SVD Algorithm was selected, transform the term document
            // matrix file to a matrix.
            if (alg == null || dimensions == 0) {
                wordSpace = MatrixIO.readMatrix(
                        termDocumentMatrix, 
                        termDocumentMatrixBuilder.getMatrixFormat());
                return;
            }

            info("reducing to %d dimensions", dimensions);

            // Compute SVD on the pre-processed matrix.
            Matrix[] usv = SVD.svd(
                    termDocumentMatrix, alg,
                    termDocumentMatrixBuilder.getMatrixFormat(),
                    dimensions);
            
            // Load the left factor matrix, which is the word semantic space
            wordSpace = usv[0];
            // Weight the values in the word space by the singular values.
            Matrix singularValues = usv[1];
            for (int r = 0; r < wordSpace.rows(); ++r) {
                for (int c = 0; c < wordSpace.columns(); ++c) {
                    wordSpace.set(r, c, wordSpace.get(r, c) * 
                                        singularValues.get(c, c));
                }
            }

            // Save the reduced document space if requested.
            if (retainDocumentSpace) {
                verbose("loading in document space");
                // We transpose the document space to provide easier access to
                // the document vectors, which in the un-transposed version are
                // the columns.
                //
                documentSpace = Matrices.transpose(usv[2]);
                // Weight the values in the document space by the singular
                // values.
                //
                // REMINDER: when the RowScaledMatrix class is merged in with
                // the trunk, this code should be replaced.
                for (int r = 0; r < documentSpace.rows(); ++r) {
                    for (int c = 0; c < documentSpace.columns(); ++c) {
                        documentSpace.set(r, c, documentSpace.get(r, c) * 
                                                singularValues.get(c, c));
                    }
                }
            }
        } catch (IOException ioe) {
            //rethrow as Error
            throw new IOError(ioe);
        }
    }
}
