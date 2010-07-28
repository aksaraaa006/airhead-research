package edu.ucla.sspace.vector;


/**
 * A decorator for {@link SparseDoubleVector}s that scales every value in a
 * given {@link DoubleVector} by some non zero scale.
 *
 * @author Keith Stevens
 */
public class ScaledSparseDoubleVector extends ScaledDoubleVector 
                                      implements SparseDoubleVector {

  /**
   * The original vector.
   */
  private SparseDoubleVector vector;

  /**
   * Creates a new {@link ScaledSparseDoubleVector} that decorates a given
   * {@link SparseDoubleVector} by scaling each value in {@code vector} by
   * {@code scale}.
   */
  public ScaledSparseDoubleVector(SparseDoubleVector vector, double scale) {
      super(vector, scale);
      this.vector = vector;
  }

  /**
   * {@inheritDoc}
   */
  public int[] getNonZeroIndices() {
      return vector.getNonZeroIndices();
  }
}
