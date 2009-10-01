package edu.ucla.sspace.vector;

/**
 * A {@code SemanticVector} where all values are held in memory. The underlying
 * implementation is simply an array of doubles.  <p>
 *
 * This class is thread-safe.
*/
public class DenseSemanticVector implements SemanticVector {

    /**
     * The values of this {@code DenseSemanticVector}.
     */
    private final double[] vector;

    /**
     * Create an {@code DenseSemanticVector} with all values starting at 0 with
     * the given length.
     *
     * @param vectorLength The size of the vector to create.
     */
    public DenseSemanticVector(int vectorLength) {
        vector = new double[vectorLength];
    }

    /**
     * Create a {@code DenseSemanticVector} taking the values given by {@code
     * vector}.
     *
     * @param vector The vector values to start with.
     */
    public DenseSemanticVector(double[] vector) {
        this.vector = vector;
    }
	
    /**
     * {@inheritDoc}
     */
    public synchronized void addVector(SemanticVector vector) {
        // Skip vectors of different lengths.
        if (vector.size() != size())
            return;
        for (int i = 0; i < size(); ++i) {
            double value = vector.get(i);
            add(i, value);
        }
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

    /**
     * {@inheritDoc}
     */
    public double size() {
        return vector.length;
    }
}
