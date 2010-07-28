package edu.ucla.sspace.vector;


/**
 * A decorator for {@link DoubleVector}s that scales every value in a given
 * {@link DoubleVector} by some non zero scale.
 *
 * @author Keith Stevens
 */
public class ScaledDoubleVector implements DoubleVector {

    /**
     * The original vector.
     */
    private DoubleVector vector;

    /**
     * The scale applied to each value in {@code vector}
     */
    private double scale;

    /**
     * Creates a new {@link ScaledDoubleVector} that decorates a given {@link
     * DoubleVector} by scaling each value in {@code vector} by {@code scale}.
     */
    public ScaledDoubleVector(DoubleVector vector, double scale) {
        this.vector = vector;
        this.scale = scale;
        if (scale == 0d)
            throw new IllegalArgumentException("Cannot scale a vector by 0");
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        return vector.add(index, delta/scale) * scale;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.get(index) * scale;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector.set(index, value / scale);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        vector.set(index, value.doubleValue() / scale);
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        return vector.magnitude() * scale;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length();
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] values = vector.toArray();
        for (int i = 0; i < values.length; ++i)
            values[i] *= scale;
        return values;
    }
}
