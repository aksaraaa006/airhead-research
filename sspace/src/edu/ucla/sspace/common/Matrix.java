package edu.ucla.sspace.common;

/**
 *
 */
public interface Matrix {

    double get(int row, int col);

    /**
     *
     */
    double[] getRow(int row);

    /**
     *
     */
    int columns();
    
    /**
     *
     */
    double[][] toDenseArray();

    /**
     *
     */
    int rows();    

    /**
     *
     */
    void set(int row, int col, double val);

}