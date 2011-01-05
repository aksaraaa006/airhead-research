package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DoubleVector;


/**
 * @author Keith Stevens
 */
public class SimilarityFloatMatrix implements Matrix {

    private final int rows;
    private final int columns;

    private float[][] values;

    public SimilarityFloatMatrix(int rows) {
        this.rows = rows;
        this.columns = rows;
        this.values = new float[rows][0];
        for (int r = 0; r < rows; ++r)
            values[r] = new float[columns - r];
    }

    public double get(int row, int col) {
        if (row < col)
            return values[row][col-row];
        return values[col][row-col];
    }

    public void set(int row, int col, double value) {
        if (row < col)
            values[row][col-row] = (float) value;
        else
            values[col][row-col] = (float) value;
    }

    public double[] getColumn(int column) {
        return null;
    }

    public DoubleVector getColumnVector(int column) {
        return null;
    }

    public double[] getRow(int row) {
        return null;
    }

    public DoubleVector getRowVector(int row) {
        return null;
    }

    public int columns() {
        return columns;
    }
    
    public int rows() {
        return rows;
    }

    public double[][] toDenseArray() {
        return null;
    }

    public void setColumn(int column, double[] values) {
        return;
    }

    public void setColumn(int column, DoubleVector values) {
        return;
    }

    public void setRow(int row, double[] values) {
    }

    public void setRow(int row, DoubleVector values) {
    }
}
