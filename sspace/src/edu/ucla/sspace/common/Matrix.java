package edu.ucla.sspace.common;

/**
 *
 *
 * @see MatrixIO
 * @see Matrix.Type
 */
public interface Matrix {

    /**
     *
     */
    public enum Type {
	/**
	 *
	 */
	SPARSE_IN_MEMORY,

	/**
	 *
	 */
	DENSE_IN_MEMORY,

	/**
	 *
	 */
	SPARSE_ON_DISK,

	/**
	 *
	 */
	DENSE_ON_DISK	    
    }

    /**
     *
     */
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