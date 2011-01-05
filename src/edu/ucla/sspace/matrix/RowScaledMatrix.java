package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;


/**
 * @author Keith Stevens
 */
public class RowScaledMatrix implements Matrix {

    /**
     * The backing instance of the matrix.
     */
    private final Matrix m;

    private final DoubleVector scales;

    /**
     * Creates a {@code RowScaledMatrix} that provides scaled read only access
     * to the provided {@code Matrix} instance.
     */
    public RowScaledMatrix(Matrix matrix, DoubleVector v) {
        this.m = matrix;
        this.scales = v;
    }
    
    /**
     * {@inheritDoc}
     */
    public int columns() {
        return m.columns();
    }
           
    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return m.get(row, col) * scales.get(row);
    }
           
    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        throw new UnsupportedOperationException("Cannot access column");
    }
           
    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        throw new UnsupportedOperationException("Cannot access column");
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        double[] values = m.getRow(row);
        for (int i = 0; i < values.length; ++i)
            values[i] *= scales.get(i);
        return values;
    }
           
    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return new ScaledDoubleVector(m.getRowVector(row), scales.get(row));
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return m.rows();
    }
           
    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] values) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        throw new UnsupportedOperationException("Cannot set values");
    }
     
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        return m.toDenseArray();
    }
}
