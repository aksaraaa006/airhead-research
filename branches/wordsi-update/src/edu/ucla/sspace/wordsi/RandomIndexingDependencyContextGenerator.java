package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPermutationFunction;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Keith Stevens
 */
public class RandomIndexingDependencyContextGenerator
    implements DependencyContextGenerator {

  private final DependencyPermutationFunction<TernaryVector> permFunc;

  private final Map<String, TernaryVector> indexMap;

  private final int indexVectorLength;

  private final int pathLength;

  /**
   * The filter that accepts only dependency paths that match predefined
   * criteria.
   */
  private final DependencyPathAcceptor acceptor;

  private boolean readOnly;

  public RandomIndexingDependencyContextGenerator(
          DependencyPermutationFunction<TernaryVector> permFunc,
          DependencyPathAcceptor acceptor,
          Map<String, TernaryVector> indexMap,
          int indexVectorLength,
          int pathLength) {
      this.permFunc = permFunc;
      this.acceptor = acceptor;
      this.indexMap = indexMap;
      this.indexVectorLength = indexVectorLength;
      this.pathLength = pathLength;
  }

  /**
   * {@inheritDoc}
   */
  public SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                            int focusIndex) {
    DependencyTreeNode focusNode = tree[focusIndex];

    SparseDoubleVector meaning = new CompactSparseVector(indexVectorLength);

    Iterator<DependencyPath> paths = new FilteredDependencyIterator(
            focusNode, acceptor, pathLength);

    while (paths.hasNext()) {
        DependencyPath path = paths.next();
        if (readOnly && !indexMap.containsKey(path.last().word()))
            continue;

        TernaryVector termVector = indexMap.get(path.last().word());
        if (permFunc != null)
            termVector = permFunc.permute(termVector, path);
        add(meaning, termVector);
    }
    return meaning;
  }

  /**
   * {@inheritDoc}
   */
  public int getVectorLength() {
      return indexVectorLength;
  }

  /**
   * {@inheritDoc}
   */
  public void setReadOnly() {
      readOnly = true;
  }

  /**
   * {@inheritDoc}
   */
  public void unsetReadOnly() {
      readOnly = false;
  }

  /**
   * Adds a {@link TernaryVector} to a {@link IntegerVector}
   */
  private void add(SparseDoubleVector dest, TernaryVector src) {
    for (int p : src.positiveDimensions())
      dest.add(p, 1);
    for (int n : src.negativeDimensions())
      dest.add(n, -1);
  }
}
