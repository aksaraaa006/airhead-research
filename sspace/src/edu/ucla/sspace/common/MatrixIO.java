package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.logging.Logger;

/**
 * A shared utility for printing matrices to files in a uniform manner and
 * converting between different formats
 */
public class MatrixIO {

    private static final Logger MATRIX_IO_LOGGER = 
	Logger.getLogger(MatrixIO.class.getName());

    public enum Format {
	/**
	 *
	 */
	DENSE_TEXT,

	/**
	 *
	 */
	MATLAB_SPARSE,

	/**
	 *
	 */
	SVDLIBC_SPARSE_TEXT,

	/**
	 *
	 */
	SVDLIBC_DENSE_TEXT,

	/**
	 *
	 */
	SVDLIBC_SPARSE_BINARY,

	/**
	 *
	 */
	SVDLIBC_DENSE_BINARY,
    }

    public static File convertFormat(File matrix, Format current, 
				     Format output) throws IOException {
	if (current.equals(output)) {
	    return matrix;
	}
	
	switch (current) {
	case DENSE_TEXT: {
	    break;
	}
	case MATLAB_SPARSE: {
	    if (output.equals(Format.SVDLIBC_SPARSE_TEXT)) {
		
	    }
	    break;
	}
	case SVDLIBC_SPARSE_TEXT: {
	    break;
	}
	case SVDLIBC_DENSE_TEXT: {
	    break;
	}
	}
	throw new UnsupportedOperationException(
	    "converting from " + current + " to " + output + 
	    " is not currently supported");
    }       

    public static double[][] readMatrixArray(File input, Format format) 
	    throws IOException {

	BufferedReader br = new BufferedReader(new FileReader(input));
	
	switch(format) {
	case DENSE_TEXT:
	    // read in the all the lines
	    List<double[]> matrix = new LinkedList<double[]>();
	    int cols = -1;
	    for (String line = null; (line = br.readLine()) != null; ) {
		String[] values = line.split("\\s+");
		int length = values.length;
		if (length == 0) {
		    throw new IOException("bad matrix line: " + line);
		}
		double[] d = new double[length];
		for (int i = 0; i < length; ++i) {
		    d[i] = Double.parseDouble(values[i]);
		}
		if (cols == -1) {
		    cols = length;
		}
		// check that all the vectors have the same  length;
		else if (cols != length) {
		    throw new IOException("Inconsistent number of columns: " +
					  line);
		}
		matrix.add(d);
	    }

	    if (matrix.isEmpty()) {
		throw new IOException("Empty matrix file");
	    }

	    double[][] array = new double[matrix.size()][0];
	    Iterator<double[]> it = matrix.iterator();
	    for (int i = 0; i < matrix.size(); ++i) {
		array[i] = it.next();
	    }
	    
	    MATRIX_IO_LOGGER.info("loaded a " + array.length + " x " + 
				  array[0].length + " dense text matrix from " +
				  input);
				  

	    return array;
	    
	case MATLAB_SPARSE:

	case SVDLIBC_SPARSE_TEXT:

	case SVDLIBC_DENSE_TEXT:

	case SVDLIBC_SPARSE_BINARY:

	case SVDLIBC_DENSE_BINARY:
	
	}
	
	throw new Error("implement me");
    }

    public static void writeMatrix(double[][] matrix, File output) 
	    throws IOException {
	if (matrix.length == 0 || matrix[0].length == 0) {
	    throw new IllegalArgumentException("invalid matrix dimensions");
	}
	PrintWriter pw = new PrintWriter(output);
	int rows = matrix.length;
	int cols = matrix[0].length;
	for (int row = 0; row < rows; ++row) {
	    for (int col = 0; col < cols; ++col) {
		pw.print(matrix[row][col] + ((col + 1 == cols) ? "\n" : " "));
	    }
	}
	pw.close();
	return;
    }

}