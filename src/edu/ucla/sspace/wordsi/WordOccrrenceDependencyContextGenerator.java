package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;

import edu.ucla.sspace.dv.DependencyPathBasisMapping;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Iterator;

/**
 * TODO: JAVADOC
 * @author Keith Stevens
 */
public class WordOccrrenceDependencyContextGenerator
        implements DependencyContextGenerator {

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

    private final int pathLength;

    /**
     * The filter that accepts only dependency paths that match predefined
     * criteria.
     */
    private final DependencyPathAcceptor acceptor;

    public WordOccrrenceDependencyContextGenerator(
            DependencyPathBasisMapping basisMapping,
            DependencyPathWeight weighter,
            DependencyPathAcceptor acceptor,
            int pathLength) {
        this.basisMapping = basisMapping;
        this.weighter = weighter;
        this.acceptor = acceptor;
        this.pathLength = pathLength;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                              int focusIndex) {
        DependencyTreeNode focusNode = tree[focusIndex];

        SparseDoubleVector focusMeaning = new CompactSparseVector();
        // Get all the valid paths starting from this word. 
        Iterator<DependencyPath> paths = new FilteredDependencyIterator(
                focusNode, acceptor, pathLength);
            
        // For each of the paths rooted at the focus word, update the
        // co-occurrences of the focus word in the dimension that the
        // BasisFunction states with the weight generated by the
        // DependencyPathWeight function.
        while (paths.hasNext()) {
            DependencyPath path = paths.next();

            // Get the dimension from the basis mapping, ignore any features
            // that are not mapped.
            int dimension = basisMapping.getDimension(path);
                if (dimension < 0)
                    continue;

            double weight = weighter.scorePath(path);
            focusMeaning.add(dimension, weight);                                        
        }
        return focusMeaning;
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
    public void setReadOnly(boolean readOnly) {
        basisMapping.setReadOnly(readOnly);
    }
}