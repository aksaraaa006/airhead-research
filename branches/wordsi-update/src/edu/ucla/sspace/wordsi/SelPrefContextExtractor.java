/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.SelectionalPreferenceSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;

import edu.ucla.sspace.dv.DependencyPathBasisMapping;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;


/**
 * This {@link ContextExtractor} reads in documents that have been dependency
 * parsed.  Contexts are defined by a {@link FilteredDependencyIterator}, which
 * is used to  traverse all possible dependency paths rooted at each word of
 * interest in a document.  Each reachable and valid {@link DependencyPath}
 * forms a feature and is weighted by a {@link DependencyPathWeight}.
 *
 * @author Keith Stevens
 */
public class SelPrefContextExtractor implements ContextExtractor {

    /**
     * The {@link DependencyExtractor} used to extract parse trees from the
     * already parsed documents
     */
    protected final DependencyExtractor extractor;

    /**
     * A basis mapping from dependency paths to the the dimensions that
     * represent the content of those paths.
     */
    private final DependencyPathBasisMapping basisMapping;

    /**
     * A function that weights {@link DependencyPath} instances according to
     * some criteria.
     */
    private final DependencyPathWeight weighter;

    /**
     * The filter that accepts only dependency paths that match predefined
     * criteria.
     */
    private final DependencyPathAcceptor acceptor;

    /**
     * The {@link SelectionalPreferenceSpace} which provides context vectors
     * based on the focus word and it's neighboring terms.
     */
    private final SelectionalPreferenceSpace sspace;

    /**
     * Creates a new {@link SelPrefContextExtractor}.
     *
     * @param extractor The {@link DependencyExtractor} that parses the document
     *                and returns a valid dependency tree
     * @param basisMapping A mapping from dependency paths to feature indices
     * @param weighter A weighting function for dependency paths
     * @param acceptor An accepting function that validates dependency paths which
     *                may serve as features
     */
    public SelPrefContextExtractor(DependencyExtractor extractor,
                                   DependencyPathBasisMapping basisMapping,
                                   DependencyPathWeight weighter,
                                   DependencyPathAcceptor acceptor,
                                   SelectionalPreferenceSpace sspace) {
        this.extractor = extractor;
        this.basisMapping = basisMapping;
        this.weighter = weighter;
        this.acceptor = acceptor;
        this.sspace = sspace;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return basisMapping.numDimensions();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, Wordsi wordsi) {
        try {
            // Handle the context header, if one exists.  Context headers are
            // assumed to be the first line in a document.
            String contextHeader = handleContextHeader(document);

            // Iterate over all of the parseable dependency parsed sentences in
            // the document.
            for (DependencyTreeNode[] nodes = null; 
                     (nodes = extractor.readNextTree(document)) != null; ) {

                // Skip empty documents.
                if (nodes.length == 0)
                    continue;                        

                // Examine the paths for each word in the sentence.
                for (int wordIndex = 0; wordIndex < nodes.length; ++wordIndex) {
                    DependencyTreeNode focusNode = nodes[wordIndex];

                    // Get the focus word, i.e., the primary key, and the
                    // secondary key.  These steps are made as protected methods
                    // so that the SenseEvalDependencyContextExtractor
                    // PseudoWordDependencyContextExtractor can manage only the
                    // keys, instead of the document traversal.
                    String focusWord = getPrimaryKey(focusNode);
                    String secondarykey = getSecondaryKey(focusNode, contextHeader);

                    // Ignore any focus words that are unaccepted by Wordsi.
                    if (!acceptWord(focusWord, contextHeader, wordsi))
                        continue;

                    // Create a new context vector.
                    SparseDoubleVector contextVector = sspace.contextualize(
                        new FilteredDependencyIterator(nodes[wordIndex], acceptor, 1));

                    wordsi.handleContextVector(focusWord, secondarykey, contextVector);
                }
                document.close();
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns true if {@link Wordsi} should generate a context vector for
     * {@code focusWord}.    
     */
    protected boolean acceptWord(String focusWord, 
                                 String contextHeader,
                                 Wordsi wordsi) {
        return wordsi.acceptWord(focusWord);
    }

    /**
     * Returns the token for the primary key, i.e. the focus word.  This is just
     * the text of the {@code focusNode}.
     */
    protected String getPrimaryKey(DependencyTreeNode focusNode) {
        return focusNode.word();
    }

    /**
     * Returns the token for the secondary key.  If a {@code contextHeader} is
     * provided, this is the {@code contextHeader}, otherwise it is the word for
     * the {@code focusNode}.
     */
    protected String getSecondaryKey(DependencyTreeNode focusNode,
                                     String contextHeader) {
        return (contextHeader == null) ? focusNode.word() : contextHeader;
    }

    /**
     * Returns the string for the context header.  In this case, it is {@code
     * null}.
     */
    protected String handleContextHeader(BufferedReader document)
            throws IOException {
        return null;
    }
}
