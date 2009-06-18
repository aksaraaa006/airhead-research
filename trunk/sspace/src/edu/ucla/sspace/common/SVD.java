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

package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ucla.sspace.common.MatrixIO.Format;
import edu.ucla.sspace.common.Matrix.Type;

import edu.ucla.sspace.common.matrix.DiagonalMatrix;

/**
 * A utililty class for invoking different implementations of the <a
 * href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Singular
 * Value Decomposition</a> (SVD).  The SVD is a way of factoring any matrix A into
 * three matrices <span style="font-family:Garamond, Georgia, serif">U &Sigma;
 * V<sup>T</sup></span> such that <span style="font-family:Garamond, Georgia,
 * serif"> &Sigma; </span> is a diagonal matrix containing the singular values
 * of <span style="font-family:Garamond, Georgia, serif">A</span>. The singular
 * values of <span style="font-family:Garamond, Georgia, serif"> &Sigma; </span>
 * are ordered according to which causes the most variance in the values of
 * <span style="font-family:Garamond, Georgia, serif">A</span>. 
 *
 * <p>
 *
 * <b>All SVD operations return an array of three {@link Matrix} instances that
 * correspond to <span style="font-family:Garamond, Georgia, serif">U &Sigma;
 * </span> and <span style="font-family:Garamond, Georgia, serif">
 * V<sup>T</sup></span></b>.
 *
 * <p>
 *
 * Four different SVD algorithms are possible:
 * <ol>
 *
 * <li> <a href="http://tedlab.mit.edu/~dr/svdlibc/">SVDLIBC</a> </li>
 *
 * <li> Matlab <a
 * href="http://www.mathworks.com/access/helpdesk/help/techdoc/index.html?/access/helpdesk/help/techdoc/ref/svds.html">svds</a>
 * </li>
 *
 * <li> <a
 * href="http://www.google.com/url?sa=U&start=1&q=http://www.gnu.org/software/octave">GNU
 * Octave</a> &nbsp; <a
 * href="http://octave.sourceforge.net/doc/f/svds.html">svds</a> - Note that
 * this is an optional package and requires that the <a
 * href="http://octave.sourceforge.net/arpack/index.html">ARPACK</a> bindings
 * for Octave is installed. </li>
 *
 * <li><a href="http://math.nist.gov/javanumerics/jama/">JAMA</a> &nbsp;
 * SVD</li>
 *
 * </ol>
 *
 * Support for these algorithms requires that they are invokable from the path
 * used to start the current JVM, or in the case of JAMA, are available in the
 * classpath.  <b>Note that if JAMA is used in conjunction with a {@code .jar}
 * executable, it's location needs to be specified with the {@code jama.path}
 * system property.</b>  This can be set on the command line using <tt>
 * -Djama.path=<i>path/to/jama</i></tt>.
 *
 * <p>
 *
 * Users may select which algorithm to use manually using the {@code Algorithm}
 * enum.  The additional enum value {@code ANY} will select the fastest
 * algorithm available.  If no algorithm is available, a {@code
 * UnsupporteOperationException} is thrown at run-time.
 *
 * <p>
 *
 * This class will automatically convert a file to the appropriate matrix format
 * for the required algorithm.
 *
 * @author David Jurgens
 * @see edu.ucla.sspace.common.MatrixIO
 */
public class SVD {

    private static final Logger SVD_LOGGER = 
	Logger.getLogger(SVD.class.getName());

    /**
     * Which algorithm to use for computing the Singular Value Decomposition
     * (SVD) of a matrix.
     */
    public enum Algorithm {
	SVDLIBC,
	MATLAB,
	OCTAVE,
        JAMA,
	ANY
    }

    /**
     * Uninstantiable
     */
    private SVD() { }

    /**
     * Returns U, S, V<sup>T</sup> matrices for the SVD of the matrix file in
     * {@link Format#MATLAB_SPARSE Matlab sparse} format using the specified SVD
     * algorithm to generate the specified number of singular values.
     *
     * @param matrix a file containing a matrix
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and
     *         V<sup>T</sup> matrices in that order
     *
     * @throws UnsupportedOperationException if no SVD algorithm is available
     */
    public static Matrix[] svd(File matrix, int dimensions) {
	return svd(matrix, Algorithm.ANY, 
		   Format.MATLAB_SPARSE, dimensions);	
    }

    /**
     * Returns U, S, V<sup>T</sup> matrices for the SVD of the matrix file in
     * {@link Format#MATLAB_SPARSE Matlab sparse} format using the specified SVD
     * algorithm to generate the specified number of singular values.
     *
     * @param matrix a file containing a matrix
     * @param alg which algorithm to use for computing the SVD
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and
     *         V<sup>T</sup> matrices in that order
     *
     * @throws UnsupportedOperationException if the provided SVD algorithm is
     *         unavailable
     */
    public static Matrix[] svd(File matrix, Algorithm alg, int dimensions) {
	return svd(matrix, alg, Format.MATLAB_SPARSE, dimensions);
    }
    
    /**
     * Returns U, S, V<sup>T</sup> matrices for the SVD of the matrix file in
     * specified format using the the fastst SVD algorithm available to generate
     * the specified number of singular values.
     * 
     * @param matrix a file containing a matrix
     * @param format the format of the input matrix file
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and
     *         V<sup>T</sup> matrices in that order
     *
     * @throws UnsupportedOperationException if no SVD algorithm is available
     */
    public static Matrix[] svd(File matrix, Format format, int dimensions) {
	return svd(matrix, Algorithm.ANY, format, dimensions);
    }

    /**
     * Returns U, S, V<sup>T</sup> matrices for the SVD of the matrix file in
     * specified format using the the specified SVD algorithm available to
     * generate the specified number of singular values.
     *
     * @param matrix a file containing a matrix
     * @param alg which algorithm to use for computing the SVD
     * @param format the format of the input matrix file
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and
     *         V<sup>T</sup> matrices in that order
     *
     * @throws UnsupportedOperationException if the provided SVD algorithm is
     *         unavailable
     */
    public static Matrix[] svd(File matrix, Algorithm alg, 
			       Format format, int dimensions) {
	try {
	    switch (alg) {
	    case SVDLIBC:
		File converted = MatrixIO.convertFormat(
		    matrix, format, Format.SVDLIBC_SPARSE_TEXT);
		return svdlibc(converted, dimensions, 
			       Format.SVDLIBC_SPARSE_TEXT);
	    case JAMA:
		return jamaSVD(matrix, format, dimensions);
	    case MATLAB:
		return matlabSVDS(matrix, dimensions);
	    case OCTAVE:
		return octaveSVDS(matrix, dimensions);
	    case ANY:

		// Keep copies of these around in case they are MATLAB and we
		// end up convert for SVDLIBC which ends up failing.  This
		// ensures that we don't do an unnecessary matrix conversion
		Format originalFormat = format;
		File originalMatrix = matrix;

		// Try to peform the SVD with any installed algorithm.  Go in
		// order of speed.  If any algorithm causes an error, go on to
		// the next until all are exhausted.
		if (isSVDLIBCavailable()) {
		    try {
			// check whether the input matrix is in an
			// SVDLIBC-acceptable format already and if not convert
			switch (format) {
			case SVDLIBC_DENSE_BINARY:
			case SVDLIBC_DENSE_TEXT:
			case SVDLIBC_SPARSE_TEXT:
			case SVDLIBC_SPARSE_BINARY:
			    converted = matrix;
			    break;
			default:
			    converted = MatrixIO.convertFormat(
				matrix, format, Format.SVDLIBC_SPARSE_TEXT);
			    format = Format.SVDLIBC_SPARSE_TEXT;
			    break;
			}
			return svdlibc(converted, dimensions, format);		
		    } catch (UnsupportedOperationException uoe) { }

		    // If SVDLIBC didn't work reset the matrix and format back
		    // to what it orignally was
		    format = originalFormat;
		    matrix = originalMatrix;
		}


		if (isMatlabAvailable()) {
		    try {
			converted = MatrixIO.convertFormat(
			    matrix, format, Format.MATLAB_SPARSE);
		    
			return matlabSVDS(converted, dimensions);
		    } catch (UnsupportedOperationException uoe) { }
		}

		if (isOctaveAvailable()) {
		    try {
			converted = MatrixIO.convertFormat(
			    matrix, format, Format.MATLAB_SPARSE);
			return octaveSVDS(converted, dimensions);
		    } catch (UnsupportedOperationException uoe) { }
		}

		if (isJAMAavailable()) {
		    try {
			return jamaSVD(matrix, format, dimensions);
		    } catch (UnsupportedOperationException uoe) { }
		}

		// if none of the algoritms were available throw an exception to
		// let the user know that the SVD cannot be done under any
		// circumstances
		throw new UnsupportedOperationException(
		    "No SVD algorithms are available");
	    }
	}
	catch (IOException ioe) {
	    SVD_LOGGER.log(Level.SEVERE, "convertFormat", ioe);
	}

	// required for compilation
	throw new UnsupportedOperationException("Unknown algorithm: " + alg);
    }

    /**
     * Returns {@code true} if the JAMA library is available
     */
    private static boolean isJAMAavailable() {
	try {
	    Class<?> clazz = loadJamaMatrixClass();
	} catch (ClassNotFoundException cnfe) {
	    return false;
	}
	return true;
    }

    /**
     * Reflectively loads the JAMA Matrix class.  If the class is not
     * immediately loadable from the existing classpath, then this method will
     * attempt to use the {@code jama.path} system environment variable to load
     * it from an external resource.
     */
    private static Class<?> loadJamaMatrixClass() 
	    throws ClassNotFoundException {

	try {
	    Class<?> clazz = Class.forName("Jama.Matrix");
	    return clazz;
	    
	} catch (ClassNotFoundException cnfe) {

	    // If we see a CNFE, don't immediately give up.  It's most likely
	    // that this class is being invoked from inside a .jar, so see if
	    // the user specified where JAMA is manually.
	    String jamaProp = System.getProperty("jama.path");
	    
	    // if not, rethrow since the class does not exist
	    if (jamaProp == null)
		throw cnfe;

	    File jamaJarFile = new File(jamaProp);	    
	    try {
		// Otherwise, try to load the class from the specified .jar file
		java.net.URLClassLoader classLoader = 
		    new java.net.URLClassLoader(
		        new java.net.URL[] { jamaJarFile.toURI().toURL() });
		
		Class<?> clazz = Class.forName("Jama.Matrix", true,
					       classLoader);
		return clazz;
	    } catch (Exception e) {
		// fall through and rethrow original
	    }
	    
	    throw cnfe;
	}
    }

    /**
     * Returns {@code true} if the SVDLIBC library is available
     */
    private static boolean isSVDLIBCavailable() {
	try {
	    Process svdlibc = Runtime.getRuntime().exec("svd");	    
	} catch (IOException ioe) {
	    return false;
	}
	return true;
    }

    /**
     * Returns {@code true} if Octave is available
     */
    private static boolean isOctaveAvailable() {
	try {
	    Process svdlibc = Runtime.getRuntime().exec("octave -v");
	} catch (IOException ioe) {
	    return false;
	}
	return true;	
    }

    /**
     * Returns {@code true} if Matlab is available
     */
    private static boolean isMatlabAvailable() {
	try {
	    Process svdlibc = Runtime.getRuntime().exec("matlab -h");
	} catch (IOException ioe) {
	    return false;
	}
	return true;
    }
    
    /**
     *
     *
     * @param matrix a file containing a matrix
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and V matrices
     *         in that order
     *
     * @throws UnsupportedOperationException if the JAMA SVD algorithm is
     *         unavailable or if any error occurs during the process
     */
    static Matrix[] jamaSVD(File matrix, Format format, int dimensions) {
	// Use reflection to load the JAMA classes and perform all the
	// operation in order to avoid any compile-time dependencies on the
	// package.
	try {
	    System.out.println("running JAMA");
	    isJAMAavailable();
	    double[][] inputMatrix = MatrixIO.readMatrixArray(
		matrix, format);

	    int rows = inputMatrix.length;
	    int cols = inputMatrix[0].length; // assume at least one row

	    Class<?> clazz = loadJamaMatrixClass();
	    Constructor<?> c = clazz.getConstructor(double[][].class);
	    Object jamaMatrix = 
		c.newInstance(new Object[] { inputMatrix } );
	    Method svdMethod = clazz.getMethod("svd", new Class[] {});
	    Object svdObject = svdMethod.invoke(jamaMatrix, new Object[] {});
	    
	    // covert the JAMA u,s,v matrices to our matrices
 	    String[] matrixMethods = new String[] {"getU", "getS", "getV"};
	    String[] matrixNames = new String[] {"JAMA-U", "JAMA-S", "JAMA-V"};
	    
	    Matrix[] usv = new Matrix[3];
	    
	    // Loop to avoid repeating reflection code
	    for (int i = 0; i < 3; ++i) {
		Method matrixAccessMethod = svdObject.getClass().
		    getMethod(matrixMethods[i], new Class[] {});
		Object matrixObject = matrixAccessMethod.invoke(
		    svdObject, new Object[] {});
		Method toArrayMethod = matrixObject.getClass().
		    getMethod("getArray", new Class[] {});
		double[][] matrixArray = (double[][])(toArrayMethod.
		    invoke(matrixObject, new Object[] {}));

		// JAMA computes the full SVD, so the output matrices need to be
		// truncated to the desired number of dimensions
		resize:
		switch (i) {
		case 0: { // U array
		    Matrix u = Matrices.create(rows, dimensions, 
					       Type.DENSE_IN_MEMORY);
		    // fill the U matrix by copying over the values
		    for (int row = 0; row < rows; ++row) {
			for (int col = 0; col < dimensions; ++col) {
			    u.set(row, col, matrixArray[row][col]);
			}
		    }
		    usv[i] = u;
		    break resize;
		}

		case 1: { // S array
		    // special case for the diagonal matrix
		    Matrix s = new DiagonalMatrix(dimensions);
		    for (int diag = 0; diag < dimensions; ++diag) {
			s.set(diag, diag, matrixArray[diag][diag]);
		    }
		    usv[i] = s;
		    break resize;
		}

		case 2: { // V array

		    // create it on disk since it's not expected that people
		    // will access this matrix
		    Matrix v = Matrices.create(dimensions, cols,
					       Type.DENSE_ON_DISK);

		    // Fill the V matrix by copying over the values.  Note that
		    // we manually transpose the matrix because JAMA returns the
		    // result transposed from what we specify.
		    for (int row = 0; row < dimensions; ++row) {
			for (int col = 0; col < cols; ++col) {
			    v.set(row, col, matrixArray[col][row]);
			}
		    }
		    usv[i] = v;
		}
		}
	    }

	    return usv;
	} catch (ClassNotFoundException cnfe) {
	    SVD_LOGGER.log(Level.SEVERE, "JAMA", cnfe);
	} catch (NoSuchMethodException nsme) {
	    SVD_LOGGER.log(Level.SEVERE, "JAMA", nsme);
	} catch (InstantiationException ie) {
	    SVD_LOGGER.log(Level.SEVERE, "JAMA", ie);
	} catch (IllegalAccessException iae) {
	    SVD_LOGGER.log(Level.SEVERE, "JAMA", iae);
	} catch (InvocationTargetException ite) {
	    SVD_LOGGER.log(Level.SEVERE, "JAMA", ite);
	} catch (IOException ioe) {
	    SVD_LOGGER.log(Level.SEVERE, "JAMA", ioe);
	}
	
	throw new UnsupportedOperationException(
	    "JAMA-based SVD is not available on this system");
    }

    /**
     *
     *
     * @param matrix a file containing a matrix
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and V matrices in
     *         that order
     *
     * @throws UnsupportedOperationException if the JAMA SVD algorithm is
     *         unavailable or if any error occurs during the process
     */
    static Matrix[] svdlibc(File matrix, int dimensions, Format format) {
	try {
	    String formatString = "";
	    
	    // output the correct formatting flags based on the matrix type
	    switch (format) {
	    case SVDLIBC_DENSE_BINARY:
		formatString = " -r db ";
		break;
	    case SVDLIBC_DENSE_TEXT:
		formatString = " -r dt ";
		break;
	    case SVDLIBC_SPARSE_BINARY:
		formatString = " -r sb ";
		break;
	    case SVDLIBC_SPARSE_TEXT:
		// Do nothing since it's the default format.
		break;
	    default:
		throw new UnsupportedOperationException(
		    "Format type is not accepted");
	    }

	    File outputMatrixFile = File.createTempFile("svdlibc", "dat");
	    outputMatrixFile.deleteOnExit();
	    String outputMatrixPrefix = outputMatrixFile.getAbsolutePath();

	    SVD_LOGGER.fine("creating SVDLIBC factor matrices at: " + 
			      outputMatrixPrefix);
	    String commandLine = "svd -o " + outputMatrixPrefix + formatString +
		" -d " + dimensions + " " + matrix.getAbsolutePath();
	    SVD_LOGGER.fine(commandLine);
	    Process svdlibc = Runtime.getRuntime().exec(commandLine);

	    BufferedReader br = new BufferedReader(
		new InputStreamReader(svdlibc.getInputStream()));
	    StringBuilder output = new StringBuilder("SVDLIBC output:\n");
	    for (String line = null; (line = br.readLine()) != null; ) {
		output.append(line).append("\n");
	    }
	    SVD_LOGGER.fine(output.toString());
	    
	    int exitStatus = svdlibc.waitFor();
	    SVD_LOGGER.fine("svdlibc exit status: " + exitStatus);

	    // If SVDLIBC was successful in generating the files, return them.
	    if (exitStatus == 0) {

		File Ut = new File(outputMatrixPrefix + "-Ut");
		File S  = new File(outputMatrixPrefix + "-S");
		File Vt = new File(outputMatrixPrefix + "-Vt");
		    
		// SVDLIBC returns the matrices in U', S, V' with U and V of
		// transposed.  To ensure consistence, transpose the U matrix
		return new Matrix[] { 
		    // load U in memory, since that is what most algorithms will
		    // be using (i.e. it is the word space).  SVDLIBC returns
		    // this as U transpose, so correct it.
		    Matrices.transpose(MatrixIO.readMatrix(Ut,
				Format.SVDLIBC_DENSE_TEXT, Type.DENSE_IN_MEMORY)),
		    // Sigma only has n values for an n^2 matrix, so make it
		    // sparse
		    readSVDLIBCsingularVector(S),
		    // V could be large, so just keep it on disk.  
		    MatrixIO.readMatrix(Vt, Format.SVDLIBC_DENSE_TEXT, 
					Type.DENSE_ON_DISK)
		};
	    }
	    else {
		// warning or error?
	    }
	} catch (IOException ioe) {
	    SVD_LOGGER.log(Level.SEVERE, "SVDLIBC", ioe);
	} catch (InterruptedException ie) {
	    SVD_LOGGER.log(Level.SEVERE, "SVDLIBC", ie);
	}

	throw new UnsupportedOperationException(
	    "SVDLIBC is not correctly installed on this system");
    }

    /**
     * Generates a diagonal {@link Matrix} from the special-case file format
     * that SVDLIBC uses to output the &Sigma; matrix.
     */
    private static Matrix readSVDLIBCsingularVector(File sigmaMatrixFile)
	    throws IOException {

	BufferedReader br = 
	    new BufferedReader(new FileReader(sigmaMatrixFile)); 

	int dimension = -1;
	int valsSeen = 0;
	Matrix m = null;
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] vals = line.split("\\s+");
	    for (int i = 0; i < vals.length; ++i) {
		// the first value seen should be the number of singular values
		if (dimension == -1) {
		    dimension = Integer.parseInt(vals[i]);
		    m = new DiagonalMatrix(dimension);
		}
		else {
		    m.set(valsSeen, valsSeen, Double.parseDouble(vals[i]));
		    ++valsSeen;
		}
	    }
	}
	return m;
    }

    /**
     *
     *
     * @param matrix a file containing a matrix
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and V matrices in
     *         that order
     *
     * @throws UnsupportedOperationException if the JAMA SVD algorithm is
     *         unavailable or if any error occurs during the process
     */
    static Matrix[] matlabSVDS(File matrix, int dimensions) {
	try {
	    // create the matlab file for executing
	    File uOutput = File.createTempFile("matlab-svds-U",".dat");
	    File sOutput = File.createTempFile("matlab-svds-S",".dat");
	    File vOutput = File.createTempFile("matlab-svds-V",".dat");
	    uOutput.deleteOnExit();
	    sOutput.deleteOnExit();
	    vOutput.deleteOnExit();

	    String commandLine = "matlab -nodisplay -nosplash -nojvm";
	    SVD_LOGGER.fine(commandLine);
	    Process matlab = Runtime.getRuntime().exec(commandLine);
	    
	    // capture the input so we know then Matlab is finished
	    BufferedReader br = new BufferedReader(
		new InputStreamReader(matlab.getInputStream()));

	    // pipe Matlab the program to execute
	    PrintWriter pw = new PrintWriter(matlab.getOutputStream());
	    pw.println(
		"Z = load('" + matrix.getAbsolutePath() + "','-ascii');\n" +
		"A = spconvert(Z);\n" + 
		"% Remove the raw data file to save space\n" +
		"clear Z;\n" + 
		"[U, S, V] = svds(A, " + dimensions + " );\n" +
		"save " + uOutput.getAbsolutePath() + " U -ASCII\n" +
		"save " + sOutput.getAbsolutePath() + " S -ASCII\n" +
		"save " + vOutput.getAbsolutePath() + " V -ASCII\n" + 
		"fprintf('Matlab Finished\\n');");
	    pw.close();

	    // capture the output
	    StringBuilder output = new StringBuilder("Matlab svds output:\n");
	    for (String line = null; (line = br.readLine()) != null; ) {
		output.append(line).append("\n");
		if (line.equals("Matlab Finished")) {
		    matlab.destroy();
		}
	    }
	    SVD_LOGGER.fine(output.toString());
	    
	    int exitStatus = matlab.waitFor();
	    SVD_LOGGER.fine("Matlab svds exit status: " + exitStatus);

	    // If Matlab was successful in generating the files, return them.
	    if (exitStatus == 0) {

 		// Matlab returns the matrices in U, S, V, with none of
		// transposed.  To ensure consistence, transpose the V matrix
		return new Matrix[] { 
		// load U in memory, since that is what most algorithms will be
		// using (i.e. it is the word space)
		MatrixIO.readMatrix(uOutput, Format.DENSE_TEXT, 
				    Type.DENSE_IN_MEMORY),
		// Sigma only has n values for an n^2 matrix, so make it sparse
		MatrixIO.readMatrix(sOutput, Format.DENSE_TEXT, 
				    Type.SPARSE_ON_DISK),
		// V could be large, so just keep it on disk.  Furthermore,
		// Matlab does not transpose V, so transpose it
		Matrices.transpose(MatrixIO.readMatrix(vOutput,
			    Format.DENSE_TEXT, Type.DENSE_ON_DISK))
		};
	    }

	} catch (IOException ioe) {
	    SVD_LOGGER.log(Level.SEVERE, "Matlab svds", ioe);
	} catch (InterruptedException ie) {
	    SVD_LOGGER.log(Level.SEVERE, "Matlab svds", ie);
	}
	
	throw new UnsupportedOperationException(
	    "Matlab svds is not correctly installed on this system");
    }

    /**
     *
     *
     * @param matrix a file containing a matrix
     * @param dimensions the number of singular values to calculate
     *
     * @return an array of {@code Matrix} objects for the U, S, and V matrices in
     *         that order
     *
     * @throws UnsupportedOperationException if the JAMA SVD algorithm is
     *         unavailable or if any error occurs during the process
     */
    static Matrix[] octaveSVDS(File matrix, int dimensions) {
	try {
	    // create the octave file for executing
	    File octaveFile = File.createTempFile("octave-svds",".m");
	    File uOutput = File.createTempFile("octave-svds-U",".dat");
	    File sOutput = File.createTempFile("octave-svds-S",".dat");
	    File vOutput = File.createTempFile("octave-svds-V",".dat");
	    octaveFile.deleteOnExit();
	    uOutput.deleteOnExit();
	    sOutput.deleteOnExit();
	    vOutput.deleteOnExit();

	    // Print the customized Octave program to a file.
	    PrintWriter pw = new PrintWriter(octaveFile);
	    pw.println(
		"Z = load('" + matrix.getAbsolutePath() + "','-ascii');\n" +
		"A = spconvert(Z);\n" + 
		"% Remove the raw data file to save space\n" +
		"clear Z;\n" + 
		"[U, S, V] = svds(A, " + dimensions + " );\n" +
		"save(\"-ascii\", \"" + uOutput.getAbsolutePath() + "\", \"U\");\n" +
		"save(\"-ascii\", \"" + sOutput.getAbsolutePath() + "\", \"S\");\n" +
		"save(\"-ascii\", \"" + vOutput.getAbsolutePath() + "\", \"V\");\n" +
		"fprintf('Octave Finished\\n');\n");
	    pw.close();
	    
	    // build a command line where octave executes the previously
	    // constructed file
	    String commandLine = "octave " + octaveFile.getAbsolutePath();
	    SVD_LOGGER.fine(commandLine);
	    Process octave = Runtime.getRuntime().exec(commandLine);

	    BufferedReader br = new BufferedReader(
		new InputStreamReader(octave.getInputStream()));
	    // capture the output
	    StringBuilder output = new StringBuilder("Octave svds output:\n");
	    for (String line = null; (line = br.readLine()) != null; ) {
		output.append(line).append("\n");
	    }
	    SVD_LOGGER.fine(output.toString());
	    
	    int exitStatus = octave.waitFor();
	    SVD_LOGGER.fine("Octave svds exit status: " + exitStatus);

	    // If Octave was successful in generating the files, return them.
	    if (exitStatus == 0) {

 		// Octave returns the matrices in U, S, V, with none of
		// transposed.  To ensure consistence, transpose the V matrix
		return new Matrix[] { 
		// load U in memory, since that is what most algorithms will be
		// using (i.e. it is the word space)
		MatrixIO.readMatrix(uOutput, Format.DENSE_TEXT, 
				    Type.DENSE_IN_MEMORY),
		// Sigma only has n values for an n^2 matrix, so make it sparse
		MatrixIO.readMatrix(sOutput, Format.DENSE_TEXT, 
				    Type.SPARSE_ON_DISK),
		// V could be large, so just keep it on disk.  Furthermore,
		// Octave does not transpose V, so transpose it
		Matrices.transpose(MatrixIO.readMatrix(vOutput,
			    Format.DENSE_TEXT, Type.DENSE_ON_DISK))
		};
	    }

	} catch (IOException ioe) {
	    SVD_LOGGER.log(Level.SEVERE, "Octave svds", ioe);
	} catch (InterruptedException ie) {
	    SVD_LOGGER.log(Level.SEVERE, "Octave svds", ie);
	}
	
	throw new UnsupportedOperationException(
	    "Octave svds is not correctly installed on this system");
    }
}
