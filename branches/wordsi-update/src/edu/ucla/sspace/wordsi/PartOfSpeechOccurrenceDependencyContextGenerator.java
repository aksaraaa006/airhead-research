
package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.hal.WeightingFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

/**
 * TODO: JAVADOC
 * @author Keith Stevens
 */
public class PartOfSpeechOccurrenceDependencyContextGenerator
        implements DependencyContextGenerator {

    /**
     * The {@link BasisMapping} used to represent the feature space.
     */
    private final BasisMapping<String, String> basis;

    /**
     * A function that weights {@link DependencyPath} instances according to
     * some criteria.
     */
    /**
     * The type of weight to apply to a the co-occurrence word based on its
     * relative location
     */
    private final boolean usePos;

    private final boolean useOrder;

    private final int windowSize;

    public PartOfSpeechOccurrenceDependencyContextGenerator(
            BasisMapping<String, String> basis,
            boolean usePos,
            boolean useOrder,
            int windowSize) {
        this.basis = basis;
        this.usePos = usePos;
        this.useOrder = useOrder;
        this.windowSize = windowSize;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                              int focusIndex) {
        Queue<String> prevWords = new ArrayDeque<String>();
        for (int i = Math.max(0, focusIndex-windowSize); i < focusIndex; ++i)
            prevWords.add(getFeature(tree[i], i-focusIndex));
                
        Queue<String> nextWords = new ArrayDeque<String>();
        for (int i = focusIndex+1;
                 i < Math.min(focusIndex+windowSize, tree.length); ++i)
            nextWords.add(getFeature(tree[i], i-focusIndex));

        SparseDoubleVector focusMeaning = new CompactSparseVector();
        addContextTerms(focusMeaning, prevWords, -1 * prevWords.size());
        addContextTerms(focusMeaning, nextWords, 1);
        return focusMeaning;
    }

    private String getFeature(DependencyTreeNode node, int index) {
        if (useOrder)
                return node.word() + "-" + index;
        if (usePos)
                return node.word() + "-" + node.pos();
        return node.word();
    }

    /**
     * Adds a feature for each word in the context that has a valid dimension.
     * Feature are scored based on the context word's distance from the focus
     * word.
     */
    protected void addContextTerms(SparseDoubleVector meaning,
                                   Queue<String> words,
                                   int distance) {
        // Iterate through each of the context words.
        for (String term : words) {
            if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                // Ignore any features that have no valid dimension.
                int dimension = basis.getDimension(term);
                if (dimension == -1)
                    continue;

                // Add the feature to the context vector and increase the
                // distance from the focus word.
                meaning.set(dimension, 1);
                ++distance;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return basis.numDimensions();
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        basis.setReadOnly(readOnly);
    }
}
