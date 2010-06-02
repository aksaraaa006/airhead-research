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

package edu.ucla.sspace.ri;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.Logger;

/**
 * A document-based approach to statistical semantics that uses a randomized
 * projection of a full term-document matrix (the <a
 * href="http://en.wikipedia.org/wiki/Vector_space_model">Vector Space
 * Model</a>) to perform dimensionality reduction.  This implementation is based
 * on the papers: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> 

 *     P. Kanerva, J. Kristoferson and A. Holst, "Random Indexing of Text
 *     Samples for Latent Semantic Analysis." In Gleitman, L.R. and Josh,
 *     A.K. (Eds.): <i>Proceedings of the 22nd Annual Conference of the
 *     Cognitive Science Society</i>, p. 1036. Mahwah, New Jersey: Erlbaum,
 *     2000.  Available <a
 *     href="http://www.rni.org/kanerva/cogsci2k-poster.txt">here</a>.</li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">
 *    J. Karlgren, and M. Sahlgren.  "From Words to Understanding." In Uesaka,
 *    Y., Kanerva, P. & Asoh, H. (Eds.): <i>Foundations of Real-World
 *    Intelligence</i>, pp. 294-308, Stanford: CSLI Publications. (2001).
 *    Available <a
 *    href="http://www.sics.se/~mange/papers/KarlgrenSahlgren2001.pdf">here</a>.</li>
 *
 * </ul>
 *
 *
 * This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * RandomIndexingByDocument#RandomIndexingByDocument(Properties)} constructor.
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
 * <dt> <i>Property:</i> <code><b>{@value #USE_SPARSE_SEMANTICS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code true} 
 *
 * <dd style="padding-top: .5em">This property specifies whether to use a sparse
 *       encoding for each word's semantics.  Using a sparse encoding can result
 *       in a large saving in memory, while requiring more time to process each
 *       document.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code null} 
 *
 * <dd style="padding-top: .5em">This property specifies an optional {@link
 *        edu.ucla.sspace.matrix.Transform} class that will be used to transform
 *        the vector space when {@link #processSpace(Properties)} is called.<p>
 *
 * </dl> <p>
 *
 * This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.<p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.  <p>
 *
 * The {@link #processSpace(Properties) processSpace} method does nothing for
 * this class and calls to it will not affect the results of {@code
 * getVectorFor}.
 *
 * @see IndexVectorGenerator
 * 
 * @author David Jurgens
 */
public class RandomIndexingByDocument implements SemanticSpace, Filterable {

    public static final String RI_SSPACE_NAME =
        "random-indexing-by-document";

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.ri.RandomIndexingByDocument";

    /**
     * The property to specify the number of dimensions to be used by the index
     * and semantic vectors.
     */
    public static final String VECTOR_LENGTH_PROPERTY = 
        PROPERTY_PREFIX + ".vectorLength";

    /**
     * The property to specify a {@link Transform} class that will be applied
     * the vector space during {@link processSpace}.
     */
    public static final String TRANSFORM_PROPERTY = 
        PROPERTY_PREFIX + ".transform";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space but requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
        PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 4000;
    
    /**
     * The class logger for reporting the algorithm status.
     */
    private static final Logger LOGGER =
        Logger.getLogger(RandomIndexingByDocument.class.getName());

    /**
     * A mapping from each word to the vector the represents its semantics
     */
    private final Map<String,IntegerVector> wordToMeaning;

    /**
     * A mapping from each word to the transformed vector that is its semantics.
     * This mapping is only filled if the {@value #TRANSFORM_PROPERTY} is set
     * and {@link #processSpace(Properties)} has been called.
     */
    private final Map<String,DoubleVector> wordToTransformedMeaning;

    /**
     * The number of dimensions for the semantic and index vectors.
     */
    private final int vectorLength;

    /**
     * A flag for whether this instance should use {@code SparseIntegerVector}
     * instances for representic a word's semantics, which saves space but
     * requires more computation.
     */
    private final boolean useSparseSemantics;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private final Set<String> semanticFilter;

    /**
     * The generator used to create each document's index vectors.
     */
    private final RandomIndexVectorGenerator indexVectorGenerator;

    /**
     * Creates a new {@code RandomIndexingByDocument} instance using the current
     * {@code System} properties for configuration.
     */
    public RandomIndexingByDocument() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code RandomIndexingByDocument} instance using the
     * provided properites for configuration.
     */
   public RandomIndexingByDocument(Properties properties) {
        String vectorLengthProp = 
            properties.getProperty(VECTOR_LENGTH_PROPERTY);
        vectorLength = (vectorLengthProp != null)
            ? Integer.parseInt(vectorLengthProp)
            : DEFAULT_VECTOR_LENGTH;

        indexVectorGenerator = 
            new RandomIndexVectorGenerator(vectorLength, properties);

        String useSparseProp = 
        properties.getProperty(USE_SPARSE_SEMANTICS_PROPERTY);
        useSparseSemantics = (useSparseProp != null)
            ? Boolean.parseBoolean(useSparseProp)
            : true;

        wordToMeaning = new ConcurrentHashMap<String,IntegerVector>();
        wordToTransformedMeaning = new HashMap<String,DoubleVector>();
        semanticFilter = new HashSet<String>();
    }

    /**
     * Removes all associations between word and semantics while still retaining
     * the word to index vector mapping.  This method can be used to re-use the
     * same instance of a {@code RandomIndexingByDocument} on multiple corpora
     * while keeping the same semantic space.
     */
    public void clearSemantics() {
        wordToMeaning.clear();
    }

    /**
     * Returns the current semantic vector for the provided word, or if the word
     * is not currently in the semantic space, a vector is added for it and
     * returned.
     *
     * @param word a word
     *
     * @return the {@code SemanticVector} for the provide word.
     */
    private IntegerVector getSemanticVector(String word) {
        IntegerVector v = wordToMeaning.get(word);
        if (v == null) {
            // lock on the word in case multiple threads attempt to add it at
            // once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                v = wordToMeaning.get(word);
                if (v == null) {
                    v = (useSparseSemantics) 
                        ? new CompactSparseIntegerVector(vectorLength)
                        : new DenseIntVector(vectorLength);
                    wordToMeaning.put(word, v);
                }
            }
        }
        return v;
    }

   /**
     * {@inheritDoc}
     */ 
    public Vector getVector(String word) {
        // Check whether the vector has been transformed
        Vector v = wordToTransformedMeaning.get(word);
        if (v != null)
            return v;
        v = wordToMeaning.get(word);
        if (v == null) {
            return null;
        }
        return Vectors.immutable(v);
    }

    /**
     * {@inheritDoc}
     */ 
    public String getSpaceName() {
        return RI_SSPACE_NAME + "-" + vectorLength + "v";
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorLength;
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
        return Collections.unmodifiableSet(wordToMeaning.keySet());
    }
    
    /**
     * Updates the semantic vectors based on the words in the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {

        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document);
        TernaryVector docVector = 
            indexVectorGenerator.generate();

        while (documentTokens.hasNext()) {
            String token = documentTokens.next();

            // If we are filtering the semantic vectors, check whether this word
            // should have its semantics calculated.  In addition, if there is a
            // filter and it would have excluded the word, do not keep its
            // semantics around
            if (!(semanticFilter.isEmpty() || semanticFilter.contains(token)
                  && !token.equals(IteratorFactory.EMPTY_TOKEN)))
                continue;

            IntegerVector tokenSemantics = getSemanticVector(token);
            add(tokenSemantics, docVector);
        }    

        document.close();
    }
    
    /**
     * Does nothing.
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
            
        String transformName = properties.getProperty(TRANSFORM_PROPERTY);
        if (transformName == null)
            return;

        Transform transform = null;
        try {
            Class clazz = Class.forName(transformName);
            transform = (Transform)(clazz.newInstance());
        } 
        // Perform a general catch here due to the number of possible
        // things that could go wrong.  Rethrow all exceptions as an
        // error.
        catch (Exception e) {
            throw new Error(e);
        } 
        LOGGER.info("performing " + transform + " transform");
        
        // Note: that this copy operation assumes the iteration order is stable
        // when accessing the semantic vectors.  If another thread were to add a
        // new word during this, the list would become invalid.
        List<SparseDoubleVector> semanticsAsList =
            new ArrayList<SparseDoubleVector>(wordToMeaning.size());
        for (IntegerVector v : wordToMeaning.values())
            semanticsAsList.add((SparseDoubleVector)(Vectors.asDouble(v)));
        Matrix semanticsAsMatrix = Matrices.asSparseMatrix(semanticsAsList);
        Matrix transformedSemantics = transform.transform(semanticsAsMatrix);
        // Due to memory pressure, we initially free the references to the
        // original values from the matrix and list, and then iterate over the
        // semantic vector map, removing those one at a time.
        semanticsAsMatrix = null;
        semanticsAsList = null;
        Iterator<Map.Entry<String,IntegerVector>> it = 
            wordToMeaning.entrySet().iterator();
        int row = 0;
        while (it.hasNext()) {
            Map.Entry<String,IntegerVector> e = it.next();
            wordToTransformedMeaning.put(
                e.getKey(), transformedSemantics.getRowVector(row));
            it.remove();
            row++;
        }
    }

    /**
     * {@inheritDoc} Note that all words will still have an index vector
     * assigned to them, which is necessary to properly compute the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    public void setSemanticFilter(Set<String> semanticsToRetain) {
        semanticFilter.clear();
        semanticFilter.addAll(semanticsToRetain);
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
