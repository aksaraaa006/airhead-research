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
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Duple;
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
 * This base class centralizes much of the common text processing needed for
 * term-document based {@link SemanticSpace}s.  It processes a document by
 * tokenizing all of the provided text and counting the term occurrences within
 * the document.  Each column in these spaces represent a document, and the
 * column values initially represent the number of occurrences for each word.
 * After all documents are processed, the word space can be modified with one of
 * the many {@link Matrix} {@link Transform} classes.  A single transform, if
 * provided will be used to reweight each term document occurrence count.  This
 * reweighting is typically done to increase the score for important and
 * distinguishing terms while less salient terms, such as stop words, are given
 * a lower score.
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
     * #processSpace(Transform) processSpace}
     * method has been called.
     */
    private Matrix wordSpace;

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

    protected void setWordSpace(Matrix wordSpace) {
        this.wordSpace = wordSpace;
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
    protected Duple<File, Format> processSpace(Transform transform)
            throws IOException {
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

        return new Duple<File, Format>(
                termDocumentMatrix, 
                termDocumentMatrixBuilder.getMatrixFormat());
    }
}
