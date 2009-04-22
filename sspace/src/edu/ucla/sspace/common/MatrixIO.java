package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.logging.Logger;

import edu.ucla.sspace.common.matrix.ArrayMatrix;

/**
 * A shared utility for printing matrices to files in a uniform manner and
 * converting between different formats
 */
public class MatrixIO {

    private static final Logger MATRIX_IO_LOGGER = 
	Logger.getLogger(MatrixIO.class.getName());

    /**
     * An enum that specifies the formatting used for the matrix that is
     * serialized in a file.  Format specifications are as follows: <br>
     *
     * <table valign="top" border="1">
     *
     * <tr><td><center>format</center></td>
     *   <td><center>description</center></td></tr>
     *
     *   <tr><td valign="top">{@link Format#DENSE_TEXT DENSE_TEXT}</td>
     *       <td>insert descripton here</td>
     *   </tr>
     *
     *   <tr><td valign="top">{@link Format#SVDLIBC_DENSE_TEXT SVDLIBC_DENSE_TEXT}</td>
     *       <td>
     * <pre><b>numRows numCols</b>
     *<i>for each row</i>:
     *  <i>for each column</i>:
     *     <tt>value</tt></pre>
     *  
     *  Newlines and spaces are equivalent. 
     *   </td>
     *  </tr>
     */
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

    /**
     *
     */
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

    public static Matrix readMatrix(File matrix, Format format) 
	    throws IOException {

	// REMINDER: this should be augmented to determine whether the matrix
	// can fit in memory (e.g. using File.size() anda Runtime.freeMemory()),
	// or whether the matrix should stay on disk but be wrapped by a Matrix
	// object

	BufferedReader br = new BufferedReader(new FileReader(matrix));
	
	switch(format) {
	case DENSE_TEXT:

	    break;
	case MATLAB_SPARSE:

	    break;
	case SVDLIBC_SPARSE_TEXT:

	    break;

	case SVDLIBC_DENSE_TEXT:

	    // Note that according to the formatting, spaces and new lines are
	    // equivalent.  Therefore, someone could just print all of the
	    // matrix values on a single line.

// 	    String[] dimensions = br.readLine().split;
// 	    int rows = Integer.parseInt(dimensions[0]);
// 	    int cols = Integer.parseInt(dimensions[1]);

// 	    // REMINDER: possibly use on disk if the matrix is too big
// 	    Matrix inMemory = new ArrayMatrix(rows, cols);

// 	    // keep track of how many values we have seen rather that the
// 	    // current row in order to support the white space formating
// 	    int valuesSeen = 0;
// 	    for (String line = null; (line = br.readLine()) != null; ) {
// 		if (row >= rows) {
// 		    throw new IOError("more rows than specified");
// 		}
// 		String[] colVals = line.split("\\s+");
		
// 		// Determine how many rows are actually represented by this line
// 		int rowsInLine = colVals.length / cols;
		
// 		for (int i = 0; i < colVals.length; ++i) {
// 		    int col = i % cols;
// 		    int row = valuesSeen / cols;
		    
// 		    int val = Integer.parseInt(colVals[i]);
// 		    inMemory.setValue(row, col, val);
		    
// 		    ++valuesSeen;
// 		}	       
// 	    }
// 	    return inMemory;

	case SVDLIBC_SPARSE_BINARY:

	    break;
	case SVDLIBC_DENSE_BINARY:
	
	    break;
	}
	
	throw new Error("implement me");

    }

    /**
     *
     */
    public static void transposeMatrix(File matrix, Format format, File output)
	    throws IOException {

	// REMINDER: this should be augmented to determine whether the tranpose
	// can be computed in memory (e.g. using File.size() and
	// Runtime.freeMemory()), or whether the operation needs to be completed
	// on disk.

	BufferedReader br = new BufferedReader(new FileReader(matrix));
	
	switch(format) {
	case DENSE_TEXT:

	    break;
	case MATLAB_SPARSE:

	    break;
	case SVDLIBC_SPARSE_TEXT:

	    break;

	case SVDLIBC_DENSE_TEXT:
	    Matrix m = readMatrix(matrix, format);
	    
	    break;
	case SVDLIBC_SPARSE_BINARY:

	    break;
	case SVDLIBC_DENSE_BINARY:
	
	    break;
	}
	
	throw new Error("implement me");

    }


    public static void writeMatrixArray(double[][] matrix, File output) 
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