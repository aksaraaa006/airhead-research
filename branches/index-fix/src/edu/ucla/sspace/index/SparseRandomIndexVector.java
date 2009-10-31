package edu.ucla.sspace.index;

import edu.ucla.sspace.util.SparseIntArray;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;


public class SparseRandomIndexVector implements SparseVector {

    private final SparseIntArray intArray;

    public SparseRandomIndexVector(int length) {
        intArray = new SparseIntArray(length);
    }

    public SparseRandomIndexVector(Vector v) {
        intArray = new SparseIntArray(v.length());
        for (int i = 0; i < v.length(); ++i)
            intArray.set(i, (int) v.get(i));
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        int newValue = intArray.getPrimitive(index) + (int) delta;
        intArray.set(index, newValue);
        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return intArray.getPrimitive(index);
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return intArray.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return intArray.length();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        intArray.set(index, (int) value);
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        for (int i = 0; i < values.length; ++i)
            intArray.set(i, (int) values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray(int size) {
        double[] array = new double[size];
        int maxSize = (size > length()) ? length() : size;
        for (int i = 0; i < maxSize; ++i)
            array[i] = intArray.getPrimitive(i);
        return array;
    }
}
