package edu.ucla.sspace.vector;


public class SparseScaledDoubleVector implements SparseDoubleVector {

    SparseDoubleVector vector;

    double scale;

    public SparseScaledDoubleVector(SparseDoubleVector vector, double scale) {
        this.vector = vector;
        this.scale = scale;
        if (scale == 0d)
            throw new IllegalArgumentException("Cannot scale a vector by 0");
    }

    public double add(int index, double delta) {
        double newValue = vector.add(index, delta/scale);
        return newValue * scale;
    }

    public double get(int index) {
        return vector.get(index) * scale;
    }

    public Double getValue(int index) {
        return get(index);
    }

    public int length() {
        return vector.length();
    }

    public double magnitude() {
        return vector.magnitude() * scale;
    }

    public void set(int index, double value) {
        vector.set(index, value/scale);
    }

    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    public double[] toArray() {
        double[] values = vector.toArray();
        for (int i = 0; i < values.length; ++i)
            values[i] *= scale;
        return values;
    }

    public int[] getNonZeroIndices() {
      return vector.getNonZeroIndices();
    }

}
