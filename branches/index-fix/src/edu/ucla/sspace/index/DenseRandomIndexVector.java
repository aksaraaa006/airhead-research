package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.Vector;


public class DenseRandomIndexVector implements Vector {

    private int[] vector;

    public DenseRandomIndexVector(int length) {
        vector = new int[length];
    }

    public DenseRandomIndexVector(Vector v) {
        vector = new int[v.length()];
        for (int i = 0; i < v.length(); ++i)
            vector[i] = (int) v.get(i);
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        vector[index] += (int) delta;
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector[index] = (int) value;
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        for (int i = 0; i < values.length; ++i)
            vector[i] = (int) values[i];
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray(int size) {
        double[] array = new double[size];
        int maxSize = (size > length()) ? length() : size;
        for (int i = 0; i < maxSize; ++i)
            array[i] = vector[i];
        return array;
    }
}
