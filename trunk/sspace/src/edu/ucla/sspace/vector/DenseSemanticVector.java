package edu.ucla.sspace.vector;

/**
 * A {@code SemanticVector} where all values are held in memory. <p>
 *
 * This class is thread-safe.
*/
public class DenseSemanticVector implements SemanticVector {

  private final double[] vector;

  public DenseSemanticVector(int vectorLength) {
    vector = new double[vectorLength];
  }
	
  /**
   * {@inheritDoc}
   */
  public synchronized void add(int index, double delta) {
    vector[index] += delta;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void set(int index, double value) {
    vector[index] = value;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized double get(int index) {
    return vector[index];
  }

  /**
   * {@inheritDoc}
   */
  public synchronized double[] getVector() {
    return vector;
  }
}
