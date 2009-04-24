package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

import edu.ucla.sspace.common.Matrix.Type;

import edu.ucla.sspace.common.matrix.ArrayMatrix;
import edu.ucla.sspace.common.matrix.OnDiskMatrix;
import edu.ucla.sspace.common.matrix.SparseMatrix;

/**
 * A shared utility for printing matrices to files in a uniform manner and
 * converting between different formats.
 *
 * @see MatrixIO.Format
 * @see Matrix
 * @see Matrix.Type
 */
public class MatrixIO {

    private static final Logger MATRIX_IO_LOGGER = 
	Logger.getLogger(MatrixIO.class.getName());

    /**
     * An enum that specifies the formatting used for the matrix that is
     * serialized in a file.  Format specifications are as follows: <br>
     *
     * <center>
     * <table valign="top" border="1" width="800">
     *
     * <tr><td><center>format</center></td>
     *   <td><center>description</center></td></tr>
     *
     *
     * <tr><td valign="top">{@link Format#DENSE_TEXT DENSE_TEXT}</td>
     *
     *       <td>Each row is on its own line, with each column being delimited
     *       by one or more white space characters.  All rows should have the
     *       same number of columns.</td>
     * </tr>
     *
     *
     * <tr><td valign="top">{@link Format#MATLAB_SPARSE MATLAB_SPARSE}</td>
     *
     *   <td> The sparse format supported by Matlab.  See <a
     *   href="http://www.mathworks.com/access/helpdesk/help/techdoc/index.html?/access/helpdesk/help/techdoc/ref/spconvert.html">here</a>
     *   for full details.</td>
     *
     * </tr>	
     *
     *
     * <tr><td valign="top">{@link Format#SVDLIBC_SPARSE_TEXT SVDLIBC_SPARSE_TEXT}</td>     
     *
     *    <td>The sparse human readable format supported by SVDLIBC.  See <a
     *    href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_ST.html">here</a> for
     *    full details.</td>
     *
     * </tr>
     *
     *
     * <tr><td valign="top">{@link Format#SVDLIBC_DENSE_TEXT SVDLIBC_DENSE_TEXT}</td>
     *
     *   <td> The dense human readable format supported by SVDLIBC.  See <a
     *   href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_DT.html">here</a> for
     *   full details.</td>
     *
     * </tr>
     *
     *
     * <tr><td valign="top">{@link Format#SVDLIBC_SPARSE_BINARY SVDLIBC_SPARSE_BINARY}</td>
     *
     *   <td>The sparse binary format supported by SVDLIBC.  See <a
     *   href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_SB.html">here</a> for
     *   full details.</td>
     *  
     * </tr>
     *
     *
     * <tr><td valign="top">{@link Format#SVDLIBC_DENSE_BINARY SVDLIBC_DENSE_BINARY}</td>
     *
     *   <td>The dense binary format supported by SVDLIBC.  See <a
     *   href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_DB.html">here</a> for
     *   full details.</td>
     *
     * </tr>
     * </table>
     * </center>
     */
    public enum Format {

	/**
	 * A human readable format where each row has its own line and all
	 * column values are provided.
	 */
	DENSE_TEXT,

	/**
	 * The sparse format supported by Matlab.  See <a
	 * href="http://www.mathworks.com/access/helpdesk/help/techdoc/index.html?/access/helpdesk/help/techdoc/ref/spconvert.html">here</a>
	 * for full details.
	 */
	MATLAB_SPARSE,

	/**
	 * The sparse human readable format supported by SVDLIBC.  See <a
	 * href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_ST.html">here</a> for
	 * full details.
	 */
	SVDLIBC_SPARSE_TEXT,

	/**
	 * The dense human readable format supported by SVDLIBC.  See <a
	 * href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_DT.html">here</a> for
	 * full details.
	 */
	SVDLIBC_DENSE_TEXT,

	/**
	 * The sparse binary format supported by SVDLIBC.  See <a
	 * href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_SB.html">here</a> for
	 * full details.
	 */
	SVDLIBC_SPARSE_BINARY,

	/**
	 * The dense binary format supported by SVDLIBC.  See <a
	 * href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_DB.html">here</a> for
	 * full details.
	 */
	SVDLIBC_DENSE_BINARY,
    }

    /**
     * Creates a new {@code Matrix} based on the provided type, with the
     * provided dimensions
     *
     * @param matrixType the type of matrix to create
     * @param rows the number of rows in the matrix
     * @param cols the number of columns in the matrix
     */
    private static Matrix createMatrix(Type matrixType, int rows, int cols) {
	switch (matrixType) {
	case SPARSE_IN_MEMORY:
	    return new SparseMatrix(rows, cols);
	case DENSE_IN_MEMORY:
	    return new ArrayMatrix(rows, cols);
	case SPARSE_ON_DISK:
	    //return new SparseOnDiskMatrix(rows, cols);
	    // REMDINER: implement me
	    return new OnDiskMatrix(rows, cols);
	case DENSE_ON_DISK:
	    return new OnDiskMatrix(rows, cols);
	}
	throw new IllegalArgumentException(
	    "Unknown matrix type: " + matrixType);
    }

    /**
     *
     */
    public static File convertFormat(File matrix, Format current, 
				     Format desired) throws IOException {
	if (current.equals(desired)) {
	    return matrix;
	}
	
	switch (current) {
	case DENSE_TEXT: {
	    break;
	}
	case MATLAB_SPARSE: {
	    if (desired.equals(Format.SVDLIBC_SPARSE_TEXT)) {
		File output = 
		    File.createTempFile("matlab-to-SVDLIBC-sparse","dat");
		matlabToSVDLIBCsparse(matrix, output);
		return output;
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
	    "converting from " + current + " to " + desired + 
	    " is not currently supported");
    }       

    private static void matlabToSVDLIBCsparse(File input, File output) 
	    throws IOException {

	BufferedReader br = new BufferedReader(new FileReader(input));

	Map<Integer,Integer> colToNonZero = new HashMap<Integer,Integer>();

	// read through once to get matrix dimensions
	int rows = 0, cols = 0, nonZero = 0;		
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] rowColVal = line.split("\\s+");
	    int row = Integer.parseInt(rowColVal[0]);
	    int col = Integer.parseInt(rowColVal[1]);
	    if (row > rows)
		rows = row;
	    if (col > cols)
		cols = col;
	    ++nonZero;
	    Integer colCount = colToNonZero.get(col);
		colToNonZero.put(col, (colCount == null) ? 1 : colCount + 1);
	}
	br.close();

	// Matlab indices are indexed starting at 1, while SVDLIBC start at 0,
	// so decrement the total number of rows and columns
	--rows;
	--cols;
	
	br = new BufferedReader(new FileReader(input));
	PrintWriter pw = new PrintWriter(output);

	// loop through a second time and convert each of the rows into its
	// SVDLIBC sparse format
	pw.println(rows + "\t" + cols + "\t" + nonZero);
	int lastCol = 0;
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] rowColVal = line.split("\\s+");
	    int col = Double.parseDouble(rowColVal[1]);
	    if (col != lastCol) {
		// print any missing colums in case not all the columns have
		// data
		for (int i = lastCol + 1; i < col; ++i) {
		    pw.println(0);
		}
		// print the new header
		int colCount = colToNonZero.get(col);
		lastCol = col;
		pw.println(colCount);		    
	    }
	    pw.println(rowColVal[0] + "\t" + rowColVal[2]);
	}
	br.close();
	pw.flush();
	pw.close();
    }

    /**
     *
     * @return a two-dimensional array of the matrix contained in provided file
     */
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

    /**
     *
     *
     *
     * @param matrix
     * @param format
     * @param matrixType
     *
     * @return the {@code Matrix} instance that contains the data in the
     *         provided file
     */
    public static Matrix readMatrix(File matrix, Format format, 
				    Type matrixType) 
	    throws IOException {

	// REMINDER: this should be augmented to determine whether the matrix
	// can fit in memory (e.g. using File.size() anda Runtime.freeMemory()),
	// or whether the matrix should stay on disk but be wrapped by a Matrix
	// object


	
	switch(format) {

	case DENSE_TEXT: 
	    return readDenseTextMatrix(matrix, matrixType);
	    
	case MATLAB_SPARSE:

	    break;
	case SVDLIBC_SPARSE_TEXT:

	    break;

	case SVDLIBC_DENSE_TEXT: 
	    return readDenseSVDLIBCtext(matrix, matrixType);

	    
	case SVDLIBC_SPARSE_BINARY:

	    break;

	case SVDLIBC_DENSE_BINARY:
	
	    break;
	}
	
	throw new Error("implement me");

    }

    /**
     * Creates a {@code Matrix} from the data encoded as {@link
     * Format#DENSE_TEXT} in provided file.
     *
     * @param matrix
     * @param matrixType
     *
     * @return as
     */
    private static  Matrix readDenseTextMatrix(File matrix, Type matrixType) 
	    throws IOException {

	BufferedReader br = new BufferedReader(new FileReader(matrix));

	// unknown number of rows, so do a quick scan to determine it
	int rows = 0;
	int cols = -1;
	for (String line = null; (line = br.readLine()) != null; rows++) {
		if (cols == -1) {
		    cols = line.split("\\s+").length;
		}
	}

	// REMINDER: possibly use on disk if the matrix is too big
	Matrix m = createMatrix(matrixType, rows, cols);
	
	int row = 0;
	for (String line = null; (line = br.readLine()) != null; row++) {
	    
	    String[] valStrs = line.split("\\s+");
	    if (valStrs.length != cols) {
		throw new Error("line " + (row + 1) + " contains an " + 
				"inconsistent number of columns");
		}
	    
	    for (int col = 0; col < cols; ++col) {
		m.set(row, col, Double.parseDouble(valStrs[col]));
	    }
	}
	
	return m;
    }

    
    /**
     * Creates a {@code Matrix} from the data encoded as {@link
     * Format#SVDLIBC_DENSE_TEXT} in provided file.
     *
     * @param matrix
     * @param matrixType
     *
     * @return as
     */
    private static Matrix readDenseSVDLIBCtext(File matrix, Type matrixType) 
	    throws IOException {

	BufferedReader br = new BufferedReader(new FileReader(matrix));

	// Note that according to the formatting, spaces and new lines are
	// equivalent.  Therefore, someone could just print all of the matrix
	// values on a single line.
	
	// determine how big the matrix is
	String line = br.readLine();
	String[] dimensions = line.split("\\s+");
	
	int rows = Integer.parseInt(dimensions[0]);
	int cols = Integer.parseInt(dimensions[1]);
	
	// REMINDER: possibly use on disk if the matrix is too big
	Matrix m = createMatrix(matrixType, rows, cols);
	
	// keep track of how many values we have seen rather that the
	// current row in order to support the white space formating
	int valuesSeen = 0;
	int index = 2; //
	String[] colVals = dimensions;
	do {
	    for (; index < colVals.length; ++index) {
		int col = valuesSeen % cols;
		int row = valuesSeen / cols;
		
		int val = Double.parseDouble(colVals[index]);
		m.set(row, col, val);
		
		++valuesSeen;
	    }
		
	    String next = br.readLine();
	    colVals = (next == null) ? null : next.split("\\s+");
	    index = 0;
	} while (colVals != null);
	
	return m;
    }
    
    /**
     *
     */
    public static Matrix transpose(Matrix matrix) {

	// REMINDER: this should be augmented to determine whether the tranpose
	// can be computed in memory (e.g. using File.size() and
	// Runtime.freeMemory()), or whether the operation needs to be completed
	// on disk.
	
	int rows = matrix.rows();
	int cols = matrix.columns();

	// MAJOR HACK: need to use reflection or some other hint
	Matrix transpose = null;
	if (matrix instanceof SparseMatrix) 
	    transpose = new SparseMatrix(cols, rows);
	else if (matrix instanceof ArrayMatrix) 
	    transpose = new ArrayMatrix(cols, rows);
	else {
	    transpose = new OnDiskMatrix(cols, rows);
	}

	

	for (int row = 0; row < rows; ++row) {
	    for (int col = 0; col < cols; ++col) {
		transpose.set(col, row, matrix.get(row, col));
	    }
	}

	return transpose;
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