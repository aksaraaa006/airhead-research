/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.matrix.Matrix.Type;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A shared utility for printing matrices to files in a uniform manner and
 * converting between different formats.
 *
 * @see MatrixIO.Format
 * @see Matrix
 * @see Matrix.Type
 *
 * @author David Jurgens
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
     * Converts the format of the input {@code matrix}, returning a temporary
     * file containing the matrix's data in the desired format.
     *
     * @param matrix a file containing a matrix to convert
     * @param current the format of the {@code matrix} file
     * @param desired the format of the returned matrix file
     *
     * @returns a matrix file with the same data in the desired format
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         wring the output matrix
     */
    public static File convertFormat(File matrix, Format current, 
				     Format desired) throws IOException {
	if (current.equals(desired)) {
	    return matrix;
	}
	
	switch (current) {
	case MATLAB_SPARSE: {
	    if (desired.equals(Format.SVDLIBC_SPARSE_TEXT)) {
		File output = File.createTempFile(
                    "matlab-to-SVDLIBC-sparse-text",".dat");
		output.deleteOnExit();
		matlabToSVDLIBCsparse(matrix, output);
		return output;
	    }
	    break;
	}
        case SVDLIBC_SPARSE_BINARY: {
            if (desired.equals(Format.MATLAB_SPARSE)) {
		File output = File.createTempFile(
                    "SVDLIBC-sparse-binary-to-Matlab",".dat");
		output.deleteOnExit();
		svdlibcSparseBinaryToMatlab(matrix, output);
		return output;
            }
        }
	}
	throw new UnsupportedOperationException(
	    "converting from " + current + " to " + desired + 
	    " is not currently supported");
    }       

    /**
     * Reads in a matrix in the {@link Format#MATLAB_SPARSE} format and writes
     * it to the output file in {@link Format#SVDLIBC_SPARSE_TEXT} format.
     */
    private static void matlabToSVDLIBCsparse(File input, File output) 
        throws IOException {
        MATRIX_IO_LOGGER.info("Converting from Matlab double values to " +
                      "SVDLIBC float values; possible loss of " +
                      "precision");

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

            // NOTE: subtract by 1 here because Matlab arrays start at 1, while
            // SVDLIBC arrays start at 0.
            Integer colCount = colToNonZero.get(col-1);
            colToNonZero.put(col-1, (colCount == null) ? 1 : colCount + 1);
        }
        br.close();

        // print out the header information 
        PrintWriter pw = new PrintWriter(output);
        pw.println(rows + "\t" + cols + "\t" + nonZero);

        // Process the entire array in chunks in case the matlab array is too
        // big to fit into memory.

        // REMINDER: this should probably be chosen based on the number of rows
        // and their expected density
        int chunkSize = 1000; 

        // This keeps track of the last columns printed.  We need this outside
        // the loop to ensure that blank columns at the end of a chunk are still
        // printed by the next non-zero chunk
        int lastCol = -1;
        
        // lower bound inclusive, upper bound exclusive
        for (int lowerBound = 0, upperBound = chunkSize ; lowerBound < rows; 
                 lowerBound = upperBound, upperBound += chunkSize) {
            // Once the dimensions and number of non-zero values are known,
            // reprocess the matrix, storing the rows and values for each column
            // that are inside the bounds 
            br = new BufferedReader(new FileReader(input));

            
            // for each column, keep track of which in the next index into the
            // rows array that should be used to store the row index.  Also keep
            // track of the value associated for that row
            int[] colIndices = new int[cols];

            // columns are kept in sorted order
            SortedMap<Integer,int[]> colToRowIndex = 
                new TreeMap<Integer,int[]>();
            SortedMap<Integer,float[]> colToRowValues = 
                new TreeMap<Integer,float[]>();

            for (String line = null; (line = br.readLine()) != null; ) {
                String[] rowColVal = line.split("\\s+");
                int row = Integer.parseInt(rowColVal[0]) - 1;
                int col = Integer.parseInt(rowColVal[1]) - 1;
                // NOTE: SVDLIBC uses floats instead of doubles, which can cause
                // a loss of precision
                float val = Double.valueOf(rowColVal[2]).floatValue();
                
                // check that the current column is within the current chunk
                if (col < lowerBound || col >= upperBound)
                    continue;

                // get the arrays used to store the non-zero row indices for
                // this column and the parallel array that stores the
                // row-index's value
                int[] rowIndices = colToRowIndex.get(col);
                float[] rowValues = colToRowValues.get(col);
                if (rowIndices == null) {
                    rowIndices = new int[colToNonZero.get(col)];
                    rowValues = new float[colToNonZero.get(col)];
                    colToRowIndex.put(col,rowIndices);
                    colToRowValues.put(col,rowValues);
                }
                
                // determine what is the current index in the non-zero row array
                // that can be used to store this row.
                int curColIndex = colIndices[col];
                rowIndices[curColIndex] = row;
                rowValues[curColIndex] = val;
                colIndices[col] += 1;
            }    
            br.close();

            // loop through the stored column and row values, printing out for
            // each column, the number of non zero rows, followed by each row
            // index and the value.  This is the SVDLIBC sparse text format.
            for (Map.Entry<Integer,int[]> e : colToRowIndex.entrySet()) {
                int col = e.getKey().intValue();
                int[] nonZeroRows = e.getValue();
                float[] values = colToRowValues.get(col);
            
                if (col != lastCol) {
                    // print any missing columns in case not all the columns
                    // have data
                    for (int i = lastCol + 1; i < col; ++i)
                        pw.println(0);

                    // print the new header
                    int colCount = colToNonZero.get(col);
                    lastCol = col;
                    pw.println(colCount);            
                }
            
                for (int i = 0; i < nonZeroRows.length; ++i)
                    pw.println(nonZeroRows[i] + " " + values[i]);
            }
        }

        pw.flush();
        pw.close();
    }

    /**
     * Reads in a matrix in the {@link Format#SVDLIBC_SPARSE_BINARY} format and writes
     * it to the output file in the {@link Format#MATLAB_SPARSE} format.
     */
    private static void svdlibcSparseBinaryToMatlab(File input, File output) 
	    throws IOException {
        
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(input)));

        PrintWriter pw = new PrintWriter(output);

        int rows = dis.readInt();
        int cols = dis.readInt();
        int entries = dis.readInt();

        // SVDLIBC sparse binary is organized as column data.  
        int entriesSeen = 0;
        int col = 0;
        for (; entriesSeen < entries; ++col) {
            int nonZero = dis.readInt();

            for (int i = 0; i < nonZero; ++i, ++entriesSeen) {
                int row = dis.readInt();
                float val = dis.readFloat();
                pw.println((1 + row) + " " + (1 + col) + " " + val);
            }
        }
        
        dis.close();
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
            case DENSE_TEXT: {
                // read in the all the lines
                List<double[]> matrix = new LinkedList<double[]>();
                int cols = -1;
                for (String line = null; (line = br.readLine()) != null; ) {
                    String[] values = line.split("\\s+");
                    int length = values.length;
                    if (length == 0)
                        throw new IOException("bad matrix line: " + line);

                    double[] d = new double[length];
                    for (int i = 0; i < length; ++i)
                        d[i] = Double.parseDouble(values[i]);
                    if (cols == -1)
                        cols = length;
                    else if (cols != length)
                        // check that all the vectors have the same  length;
                        throw new IOException(
                                "Inconsistent number of columns: line");
                    matrix.add(d);
                }

                if (matrix.isEmpty())
                    throw new IOException("Empty matrix file");

                double[][] array = new double[matrix.size()][0];
                Iterator<double[]> it = matrix.iterator();
                for (int i = 0; i < matrix.size(); ++i)
                    array[i] = it.next();
                
                MATRIX_IO_LOGGER.fine("loaded a " + array.length + " x " + 
                        array[0].length + " dense text matrix from " +
                        input);

                return array;
            }
                
            case MATLAB_SPARSE: {
                Matrix matrix = new GrowingSparseMatrix();
                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] rowColVal = line.split("\\s");
                    int row = Integer.parseInt(rowColVal[0]) - 1;
                    int col = Integer.parseInt(rowColVal[1]) - 1;
                    double value = Double.parseDouble(rowColVal[2]);
                    matrix.set(row, col, value);
                }

                double[][] array = new double[matrix.rows()][0];
                for (int i = 0; i < matrix.rows(); ++i)
                    array[i] = matrix.getRow(i);
                return array;
            }

            case SVDLIBC_SPARSE_TEXT: {
                String line = br.readLine();
                if (line == null)
                    throw new IOException("Empty input Matrix");

                String[] numRowsColsNonZeros = line.split("\\s");
                int numRows = Integer.parseInt(numRowsColsNonZeros[0]);
                int numCols = Integer.parseInt(numRowsColsNonZeros[1]);

                double[][] array = new double[numRows][numCols];
                for (int j = 0; j < numCols && (line = br.readLine()) != null;
                     ++j) {
                    int numNonZeros = Integer.parseInt(line);
                    for (int i = 0; i < numNonZeros &&
                            (line = br.readLine()) != null; ++i) {
                        String[] rowValue = line.split("\\s");
                        int row = Integer.parseInt(rowValue[0]);
                        double value = Double.parseDouble(rowValue[1]);
                        array[row][j] = value;
                    }
                }
                return array;
            }


            case SVDLIBC_DENSE_BINARY: {
                DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(input)));
                int numRows = in.readInt();
                int numCols = in.readInt();
                double[][] array = new double[numRows][numCols];

                for (int row = 0; row < numRows; ++row) {
                    for (int col = 0; col < numCols; ++col) {
                        array[row][col] = in.readFloat();
                    }
                }                 
                
                return array;
            }

            case SVDLIBC_DENSE_TEXT:
                // TODO IMPLEMENT ME.
                break;

            case SVDLIBC_SPARSE_BINARY: {
                DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(input)));
                int numRows = in.readInt();
                int numCols = in.readInt();
                int allNonZeros = in.readInt();

                double[][] array = new double[numRows][numCols];
                for (int j = 0; j < numCols; ++j) {
                    int numNonZeros = in.readInt();
                    for (int i = 0; i < numNonZeros; ++i) {
                        int row = in.readInt(); 
                        double value = in.readDouble();
                        array[row][j] = value;
                    }
                }
                return array;
            }
        }

        throw new Error("implement me");
    }

    /**
     * Converts the contents of a matrix file as a {@link Matrix} object, using
     * the provided type description as a hint for what kind to create.  The
     * type of {@code Matrix} object created will be based on an estimate of
     * whether the data will fit into the available memory.  Note that the
     * returned {@link Matrix} instance is not backed by the data on file;
     * changes to the {@code Matrix} will <i>not</i> be reflected in the
     * original file's data.
     *
     * @param matrix a file contain matrix data
     * @param format the format of the file
     * @param matrixType the expected type and behavior of the matrix in
     *        relation to memory.  This value will be used as a hint for what
     *        kind of {@code Matrix} instance to create
     *
     * @return the {@code Matrix} instance that contains the data in the
     *         provided file
     */
    public static Matrix readMatrix(File matrix, Format format, 
                    Type matrixType) 
        throws IOException {

        return readMatrix(matrix, format, matrixType, false);
    }

    /**
     * Converts the contents of a matrix file as a {@link Matrix} object, using
     * the provided type description as a hint for what kind to create.  The
     * type of {@code Matrix} object created will be based on an estimate of
     * whether the data will fit into the available memory.  Note that the
     * returned {@link Matrix} instance is not backed by the data on file;
     * changes to the {@code Matrix} will <i>not</i> be reflected in the
     * original file's data.
     *
     * @param matrix a file contain matrix data
     * @param format the format of the file
     * @param matrixType the expected type and behavior of the matrix in
     *        relation to memory.  This value will be used as a hint for what
     *        kind of {@code Matrix} instance to create
     * @param transposeOnRead {@code true} if the matrix should be transposed as
     *        its data is read in.  For certain formats, this is more efficient
     *        than reading the data in and then transposing it directly.
     *
     * @return the {@code Matrix} instance that contains the data in the
     *         provided file, optionally transposed from its original format
     *
     * @throws IOException if any error occurs while reading in the matrix data
     */
    public static Matrix readMatrix(File matrix, Format format, 
				    Type matrixType, boolean transposeOnRead) 
	    throws IOException {

	switch(format) {
	case DENSE_TEXT: 
	    return readDenseTextMatrix(matrix, matrixType, transposeOnRead);
	    
	case MATLAB_SPARSE:
	    break;
	case SVDLIBC_SPARSE_TEXT:
	    break;

	case SVDLIBC_DENSE_TEXT: 
	    return readDenseSVDLIBCtext(matrix, matrixType, transposeOnRead);
	   
	case SVDLIBC_SPARSE_BINARY:
	    break;

	case SVDLIBC_DENSE_BINARY:
            return readDenseSVDLIBCbinary(matrix, matrixType, transposeOnRead);
	}
	
	throw new Error("Reading matrices of " + format + " format is not "+
                        "currently supported. Email " + 
                        "s-space-research-dev@googlegroups.com to request its" +
                        "inclusion and it will be quickly added");
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
    private static  Matrix readDenseTextMatrix(File matrix, Type matrixType,
                                               boolean transposeOnRead) 
        throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(matrix));

	// unknown number of rows, so do a quick scan to determine it
	int rows = 0;
	int cols = -1;
	for (String line = null; (line = br.readLine()) != null; rows++) {
            // Create a scanner to parse out the values from the current line of
            // the file.
            Scanner s = new Scanner(line);
            int vals = 0;
            // Count how many columns the row has
            for (; s.hasNextDouble(); vals++, s.nextDouble())
                ;

            // Base case if the number of columns has not been set
            if (cols == -1) 
                cols = vals;
            // Otherwise, ensure that the number of values in the current row
            // matches the number of values seen in the first
            else {
                if (cols != vals) {
                    throw new Error("line " + (rows + 1) + " contains an " + 
                                    "inconsistent number of columns");
                }
            }
	}
        br.close();

        if (MATRIX_IO_LOGGER.isLoggable(Level.FINE)) {
            MATRIX_IO_LOGGER.fine("reading in text matrix with " + rows  +
                                  " rows and " + cols + " cols");
        }

        // Once the matrix has had its dimensions determined, re-open the file
        // to load the data into a Matrix instance.  Use a Scanner to parse the
        // text for us.
        Scanner scanner = new Scanner(matrix);
	Matrix m = (transposeOnRead)
            ? Matrices.create(cols, rows, matrixType)
            : Matrices.create(rows, cols, matrixType);
        
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                double d = scanner.nextDouble();
                if (transposeOnRead)
                    m.set(col, row, d);
                else
                    m.set(row, col, d);
            }
        }
        scanner.close();
	
	return m;
    }

    /**
     * Creates a {@code Matrix} from the data encoded as {@link
     * Format#SVDLIBC_DENSE_TEXT} in provided file.
     *
     * @param matrix
     * @param matrixType
     *
     * @return a matrix whose data was specified by the provided file
     */
    private static Matrix readDenseSVDLIBCtext(File matrix, Type matrixType,
                                               boolean transposeOnRead) 
	    throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(matrix));

	// Note that according to the formatting, spaces and new lines are
	// equivalent.  Therefore, someone could just print all of the matrix
	// values on a single line.
	
	int rows = -1;
	int cols = -1;
	int valuesSeen = 0;
	// REMINDER: possibly use on disk if the matrix is too big
	Matrix m = null; 

	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] vals = line.split("\\s+");
	    for (int i = 0; i < vals.length; ++i) {
		// rows is specified first
		if (rows == -1) {
		    rows = Integer.parseInt(vals[i]);
		}
		// cols will be second
		else if (cols == -1) {
		    cols = Integer.parseInt(vals[i]);

		    // once both rows and cols have been assigned, create the
		    // matrix
		    m = (transposeOnRead) 
                        ? Matrices.create(cols, rows, matrixType)
                        : Matrices.create(rows, cols, matrixType);
		    MATRIX_IO_LOGGER.log(Level.FINE, 
			"created matrix of size {0} x {1}", 
			new Object[] {Integer.valueOf(rows), 
				      Integer.valueOf(cols)});
		}
		else {
		    int row = valuesSeen / cols;
		    int col = valuesSeen % cols;

		    double val = Double.parseDouble(vals[i]);

                    if (transposeOnRead)
                        m.set(col, row, val);
                    else
                        m.set(row, col, val);
		
		    // increment the number of values seen to properly set the
		    // next index of the matrix
		    ++valuesSeen;
		}
	    }
	}
	
	return m;
    }    

    /**
     * Creates a {@code Matrix} from the data encoded as {@link
     * Format#SVDLIBC_DENSE_BINARY} in provided file.
     *
     * @param matrix
     * @param matrixType
     *
     * @return a matrix whose data was specified by the provided file
     */
    private static Matrix readDenseSVDLIBCbinary(File matrix, Type matrixType,
                                                 boolean transposeOnRead) 
	    throws IOException {
	DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(matrix)));

        int rows = dis.readInt();
        int cols = dis.readInt();
        Matrix m = (transposeOnRead)
            ? Matrices.create(cols, rows, matrixType)
            : Matrices.create(rows, cols, matrixType);
        
        if (transposeOnRead) {
            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < cols; ++col) {
                    m.set(col, row, dis.readFloat());
                }
            }
        }
        else {
            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < cols; ++col) {
                    m.set(row, col, dis.readFloat());
                }
            }
        }

        return m;
    }    

    /**
     * Writes the matrix to the specified output file in the provided format
     *
     * @param matrix the matrix to be written
     * @param output the file in which the matrix should be written
     * @param format the data format in which the matrix's data should be
     *        written
     *
     * @throws IllegalArgumentException if the input matrix is 0-dimensional
     * @throws IOException if an error occurs while writing to the output file
     */
    public static void writeMatrix(Matrix matrix, File output, Format format)
        throws IOException {
	if (matrix.rows() == 0 || matrix.columns() == 0)
	    throw new IllegalArgumentException(
                "cannot write 0-dimensional matrix");
	switch (format) {
	   
	case DENSE_TEXT: {
	    PrintWriter pw = new PrintWriter(output);
	    for (int i = 0; i < matrix.rows(); ++i) {
		StringBuffer sb = new StringBuffer(matrix.columns() *  5);
		for (int j = 0; j < matrix.columns(); ++j) {
		    sb.append(matrix.get(i,j)).append(" ");
		}
		pw.println(sb.toString());
	    }
	    pw.close();
	    break;
	}

        case SVDLIBC_DENSE_TEXT: {
	    PrintWriter pw = new PrintWriter(output);
	    pw.println(matrix.rows() + " " + matrix.columns());
	    for (int i = 0; i < matrix.rows(); ++i) {
		StringBuffer sb = new StringBuffer(32);
		for (int j = 0; j < matrix.columns(); ++j) {
		    sb.append(matrix.get(i,j)).append(" ");
		}
		pw.println(sb.toString());
	    }
	    pw.close();
	    break;
        }

        case SVDLIBC_DENSE_BINARY: {
	    DataOutputStream outStream = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(output)));
	    outStream.writeInt(matrix.rows());
	    outStream.writeInt(matrix.columns());
	    for (int i = 0; i < matrix.rows(); ++i) {
		for (int j = 0; j < matrix.columns(); ++j) {
		    outStream.writeFloat(new Double(matrix.get(i,j)).floatValue());
		}
	    }
	    outStream.close();
	    break;
        }

	case SVDLIBC_SPARSE_TEXT: {
	    PrintWriter pw = new PrintWriter(output);
	    // count the number of non-zero values for each column as well as
	    // the total
	    int nonZero = 0;
	    int[] nonZeroPerCol = new int[matrix.columns()];
	    for (int i = 0; i < matrix.rows(); ++i) {
		for (int j = 0; j < matrix.columns(); ++j) {
		    if (matrix.get(i, j) != 0) {
			nonZero++;
			nonZeroPerCol[j]++;
		    }
		}
	    }

	    // loop through the matrix a second time, printing out the number of
	    // non-zero values for each column, followed by those values and
	    // their associated row
	    pw.println(matrix.rows() + " " + matrix.columns() + " " + nonZero);
	    for (int col = 0; col < matrix.columns(); ++col) {
		pw.println(nonZeroPerCol[col]);
		if (nonZeroPerCol[col] > 0) {
		    for (int row = 0; row < matrix.rows(); ++row) {
			double val = matrix.get(row, col);
			if (val != 0) {
			    // NOTE: need to convert to float since this is what
			    // SVDLIBC uses
			    pw.println(row + " " + 
				       Double.valueOf(val).floatValue());
			}
		    }
		}
	    }
	    pw.close();
	    break;
	}

        case MATLAB_SPARSE: {
	    PrintWriter pw = new PrintWriter(output);
	    for (int j = 0; j < matrix.columns(); ++j) {
		for (int i = 0; i < matrix.rows(); ++i) {
		    if (matrix.get(i,j) == 0)
			continue;
		    StringBuffer sb = new StringBuffer(32);
		    sb.append(i).append(" ").append(j)
			.append(" ").append(matrix.get(i,j));
		    System.out.println(sb.toString());
		    pw.println(sb.toString());
		}
	    }
	    pw.close();
	    break;	    	    
	}
	default:
	    throw new UnsupportedOperationException(
		"writing to " + format + " is currently unsupported");
	}
    }

    public static void writeMatrixArray(double[][] matrix, File output) 
        throws IOException {
        if (matrix.length == 0 || matrix[0].length == 0)
            throw new IllegalArgumentException("invalid matrix dimensions");

        PrintWriter pw = new PrintWriter(output);
        int rows = matrix.length;
        int cols = matrix[0].length;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col)
                pw.print(matrix[row][col] + ((col + 1 == cols) ? "\n" : " "));
        }

        pw.close();
        return;
    }
}
