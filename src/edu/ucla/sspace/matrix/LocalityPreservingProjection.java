/*
 * Copyright 2010 David Jurgens
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

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.Matrix.Type;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the Locality Preserving Projection, which is a
 * linear-time subspace projection.  This implementation is based on the paper
 * by He and Niygo:
 * <ul>

 *   <li style="font-family:Garamond, Georgia, serif">Xiaofei He and Partha
 *     Niyogi, "Locality Preserving Projections," in <i>Proceedings of Advances
 *     in Neural Information Processing Systems 16 (NIPS 2003)</i>. Vancouver
 *     Canada. 2003.  Available <a
 *     href="http://books.nips.cc/papers/files/nips16/NIPS2003_AA20.pdf">here</a></li>

 * </ul>
 * This class requires the availability of Matlab or Octave.
 *
 *
 *
 * @see SVD
 */
public class LocalityPreservingProjection {

    /**
     * Methods by which the affinity matrix is constructed.
     */
    public enum EdgeType { 
        /**
         * An edge will be added between two data points, i and j, if j is in
         * the <i>k</i> nearest neighbors of i.  This relationship is not
         * symmetric.
         */
        NEAREST_NEIGHBORS,
        
        /**
         * An edge will be added between two data points, i and j, if the
         * similarity between them is above a certain threshold.  This
         * relationship is symmetric.
         */
        MIN_SIMILARITY 
    }

    /**
     * Options to weight edges in the affinity matrix.
     */
    public enum EdgeWeighting {
        /**
         * The weight is 1 if the data points are connected and 0 otherwise.
         */
        BINARY,

        /**
         * The edges are weighted by a Gaussian kernel (also known as a Heat
         * kernel), which is parameterized by a value <i>t</i>
         */
        GAUSSIAN_KERNEL,

        /**
         * The edges for two data points, i and j, are weighted by
         * (x<sup>T</sup><sub>i</sub>x<sub>j</sub> + 1)<sup>d</sup>, where
         * <i>d</i> indicates the degree of the polynomial kernel.
         */
        POLYNOMIAL_KERNEL,

        /**
         * The edge between two data points has the value of their dot product.
         */
        DOT_PRODUCT,

        /**
         * The edge between two data points has the value of their cosine
         * similarity.  Note that this case is equivalent to a dot product
         * normalized to 1.
         */
        COSINE_SIMILARITY,
    }

    private static final Logger LOGGER = 
	Logger.getLogger(LocalityPreservingProjection.class.getName());

    /**
     * The generic Matlab/Octave implementation of LPP where the files and
     * language-specific I/O calls have been left open as printf formatting
     * arguments to be later specified.
     */
    private static final String LPP_M =
        "%% LPP code based on the Matlab implementation by Deng Cai (dengcai2 AT\n" +
        "%% cs.uiuc.edu) available at\n" +
        "%% http://www.cs.uiuc.edu/homes/dengcai2/Data/code/LPP.m\n" +
        "\n" +
        "%% Load the data matrix from file\n" +
        "Tmp = load('%s','-ascii');\n" +
        "data = spconvert(Tmp);\n" +
        "%% Remove the raw data file to save space\n" +
        "clear Tmp;\n" +   
        "[nSmp,nFea] = size(data);\n" +
        // NOTE: the following 5 lines subtract out the mean from the data.
        // This process might not be feasible for very large data sets, so it
        // might be worth making this configurable in the future
        "%% Subtract out the mean fromm the data.  See page 7 of the LPI paper\n" +
        "if issparse(data)\n" +
        "    data = full(data);\n" +
        "end\n" +
        "sampleMean = mean(data);\n" +
        "data = (data - repmat(sampleMean,nSmp,1));\n" +
        "\n" +
        "%% Load the affinity matrix from file\n" +
        "Tmp = load('%s','-ascii');\n" +
        "data = spconvert(Tmp);\n" +
        "%% Remove the raw data file to save space\n" +
        "clear Tmp;\n" +
        "\n" +
        "%% If 0, all of the dimensions in the adj. matrix are used\n" +
        "ReducedDim = %d\n" +
        "\n" +
        "options = [];\n" +
        "\n" +
        "D = full(sum(W,2));\n" +
        "options.ReguAlpha = options.ReguAlpha*sum(D)/length(D);\n" +
        "D = sparse(1:nSmp,1:nSmp,D,nSmp,nSmp);\n" +
        "\n" +
        "DPrime = data'*D*data;\n" +
        "DPrime = max(DPrime,DPrime');\n" +
        "\n" +
        "WPrime = data'*W*data;\n" +
        "WPrime = max(WPrime,WPrime');\n" +
        "\n" +
        "dimMatrix = size(WPrime,2);\n" +
        "\n" +
        "if Dim > dimMatrix\n" +
        "    Dim = dimMatrix;\n" + 
        "end\n" +
        "\n" +
        "if (dimMatrix > 1000 & Dim < dimMatrix/10) | (dimMatrix > 500 & Dim < dimMatrix/20) | (dimMatrix > 250 & Dim < dimMatrix/30)\n" +
        "    bEigs = 1;\n" +
        "else\n" +
        "    bEigs = 0;\n" +
        "end\n" +
        "\n" +
        "\n" +
        "if bEigs\n" +
        "    %disp('using eigs to speed up!');\n" +
        "    [eigvector, eigvalue] = eigs(WPrime,DPrime,Dim,'la',option);\n" +
        "    eigvalue = diag(eigvalue);\n" +
        "else\n" +
        "    [eigvector, eigvalue] = eig(WPrime,DPrime);\n" +
        "    eigvalue = diag(eigvalue);\n" +
        "\n" +
        "    [junk, index] = sort(-eigvalue);\n" +
        "    eigvalue = eigvalue(index);\n" +
        "    eigvector = eigvector(:,index);\n" +
        "\n" +
        "    if Dim < size(eigvector,2)\n" +
        "        eigvector = eigvector(:, 1:Dim);\n" +
        "        eigvalue = eigvalue(1:Dim);\n" +
        "    end\n" +
        "end\n" +
        "\n" +
        "for i = 1:size(eigvector,2)\n" +
        "    eigvector(:,i) = eigvector(:,i)./norm(eigvector(:,i));\n" +
        "end\n" +
        "\n" +
        "eigIdx = find(eigvalue < 1e-3);\n" +
        "eigvalue (eigIdx) = [];\n" +
        "eigvector(:,eigIdx) = [];\n" +
        "\n" +
        "%% Compute the projection\n" +
        "projection = fea*eigvector;\n" +
        "\n" +
        "%% Save the projection as a matrix\n" +
        "%s\n" +
        "printf('Finished\\n');" +
        "\n";
      
    /**
     * Uninstantiable
     */
    private LocalityPreservingProjection() { }

    /**
     * Returns the Gaussing kernel weighting of the two vectors using the
     * parameter to weight the distance between the two vectors.
     */
    private static double gaussianKernel(Vector v1, Vector v2, 
                                         double gaussianKernelParam) {
        double euclideanDist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(euclideanDist / gaussianKernelParam));
    }

    /**
     * Returns the Gaussing kernel weighting of the two vectors using the
     * parameter to specify the degree of the polynomial.
     *
     * @param degree the degree of the polynomial
     */
    private static double polynomialKernel(Vector v1, Vector v2, 
                                           double degree) {
        double dotProduct = VectorMath.dotProduct(v1, v2);
        return Math.pow(dotProduct + 1, degree);
    }

    /**
     * Returns the weight of the connection from {@code x} to {@code y}.
     *
     * @param x the first vector that is connected
     * @param y the vector that is connected to {@code x}
     * @param w the method to use in deciding the weight value
     * @param param an optional parameter to use in weighting
     *
     * @return the edge weight
     */
    private static double getWeight(Vector x, Vector y, 
                                    EdgeWeighting w, double param) {
        switch (w) {
        case BINARY:
            return 1;

        case GAUSSIAN_KERNEL:
            return gaussianKernel(x, y, param);

        case POLYNOMIAL_KERNEL:
            return polynomialKernel(x, y, param);

        case DOT_PRODUCT:
            return VectorMath.dotProduct(x, y);

        case COSINE_SIMILARITY:
            return Similarity.cosineSimilarity(x, y);
        default:
            assert false : "unhandled edge weighting type: " + w;
        }
        throw new IllegalArgumentException(
            "unhandled edge weighting type: " + w);
    }

    /**
     * Projects the rows of the matrix into a lower dimensional subspace using
     * the Locality Preserving Projection (LPP) algorithm.
     *
     * <p><i>Implementation Note</i>: This method makes a best effort at keeping
     * the matrix data on disk.  Accordingly, it converts in the input matrix
     * into a suitable format for fast access to the row data.  For best
     * results, use {@code SVDLIBC_SPARSE_BINARY} with the data transposed (the
     * data points become columns in the input matrix).
     *
     *
     * @param dataSimilarityMetric the metric by which two data points should be
     *        compared when constructing the affinity matrix.
     * @param isDataTransposed {@code true} if the data in the original matrix
     *        has been transposed in the provided matrix file, i.e. the data
     *        point as rows are now columns.
     * @param edgeType the process to use when deciding whether two data points
     *        are connected by an edge in the affinity matrix.
     * @param edgeTypeParam an optional parameter to the {@link EdgeType}
     *        selection process.  If the selected {@code EdgeType} does not take
     *        a parameter, this value is unused.
     * @param edgeWeight the weighting scheme to use for edges in the affinity
     *        matrix
     * @param edgeWeightParam an optional parameter to the {@link EdgeWeight}
     *        when deciding on the weighting for an edge.  If the selected
     *        {@code EdgeWeight} does not take a parameter, this value is
     *        unused.
     *
     * @return a file containing the LPP-reduced data in {@code MATLAB_SPARSE}
     *         format.  Note that if the data was transposed on input, the data
     *         is returned non-tranposed (i.e. the data format is the same
     *         regardless of whether it was tranposed on input).
     */
    public static File project(File matrixFile, MatrixIO.Format format,
                                boolean isDataTransposed, 
                                int dimensions, 
                                SimType dataSimilarityMetric,
                                EdgeType edgeType,
                                double edgeTypeParam,
                                EdgeWeighting weighting,
                                double edgeWeightParam) {
        
        // IMPLEMENTATION NOTE: since the user has requested the matrix be dealt
        // with as a file, we need to keep the matrix on disk.  However, the
        // input matrix format may not be conducive to efficiently comparing
        // rows with each other (e.g. MATLAB_SPARSE is inefficient), so convert
        // the matrix to a better format.
        try {
            LOGGER.fine("Converting data matrix for easier " +
                        "processing of the affinity matrix");
            // Keep the matrix on disk, but convert it to a transposed SVDLIBC
            // sparse binary, which allows for easier efficient row-by-row
            // comparisons (which are really columns).  Note that if the data is
            // already in this format, the conversion is a no-op.
            File converted = 
                MatrixIO.convertFormat(matrixFile, format, 
                                       MatrixIO.Format.SVDLIBC_SPARSE_BINARY,
                                       !isDataTransposed);
            LOGGER.fine("Calculating the affinity matrix");
            // Read off the matrix dimensions
            DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(converted)));
            int rows = dis.readInt();
            int cols = dis.readInt();
            dis.close();
            
            // Once we know the matrix dimensions, create an iterator over the
            // data, and repeatedly loop through the columns (which are really
            // rows in the original matrix) to create the affinity matrix.
            File affMatrixFile = File.createTempFile("lcc-adj-matrix",".dat");
            PrintWriter affMatrixWriter = new PrintWriter(affMatrixFile);
            
            // Keep track of the first row and have a reference to the next row.
            // The nextRow reference avoid us having to advance into data
            // unnecessarily to retrieval the vector for processing to start
            SparseDoubleVector curRow = null;
            SparseDoubleVector nextRow = null;
            for (int row = 0; row < rows; ++row) {
                Iterator<SparseDoubleVector> matrixIter = 
                    new SvdlibcSparseBinaryFileRowIterator(converted);

                // This map is only used if k-nearest neighbors option is being
                // used.  The map is to the row and its weighted affinity
                // value.  We need to store the potential value at the time of
                // the similarity calculation because that is the only time the
                // two row vectors are in memory
                int k = (edgeType.equals(EdgeType.NEAREST_NEIGHBORS))
                    ? (int)edgeTypeParam : 1;
                MultiMap<Double,Duple<Integer,Double>> neighbors = 
                    new BoundedSortedMultiMap<Double,Duple<Integer,Double>>(
                        k, false);

                // Loop through each of the rows, gathering the statistics
                // necessary to compute the affinity matrix.
                for (int other = 0; other < rows; ++other) {
                    // Special case for the very first row
                    if (row == 0) {
                        curRow = matrixIter.next();
                        continue;
                    }
                    SparseDoubleVector otherRow = matrixIter.next();
                    // Special case for the similarity threshold, which is
                    // symmetric.  In this case, we can skip over processing any
                    // rows that occur before the current row
                    if (edgeType.equals(EdgeType.MIN_SIMILARITY)
                            && other < row) 
                        continue;

                    // Save the row that will be used next so we have it to do
                    // comparisons with for earlier rows in the file
                    if (other == row + 1)
                        nextRow = otherRow;

                    // Determine if the current row and the other row should be
                    // linked in the affinity matrix.  For code simplicity,
                    // both the k-nearest neighbors and the similarity threshold
                    // code are supported within the I/O, with the caller
                    // specifying which to use.
                    double dataSimilarity = Similarity.getSimilarity(
                        dataSimilarityMetric, curRow, otherRow);
                    
                    switch (edgeType) {
                    case NEAREST_NEIGHBORS: {
                        double edgeWeight = 
                            getWeight(curRow, otherRow, 
                                      weighting, edgeWeightParam);
                        neighbors.put(dataSimilarity,
                            new Duple<Integer,Double>(other, edgeWeight));
                        break;
                    }
                    // Use the similarity threshold to decide if the rows are
                    // linked
                    case MIN_SIMILARITY: {
                        if (dataSimilarity > edgeTypeParam) {
                            double edgeWeight = 
                                getWeight(curRow, otherRow, 
                                          weighting, edgeWeightParam);
                            // Print out the symmetric edges
                            affMatrixWriter.println(
                                row + " " + other  + " " + edgeWeight);
                            affMatrixWriter.println(
                                other + " " + row  + " " + edgeWeight);
                        }
                        break;
                    }
                    default:
                        assert false : "unhandled edge type: " + edgeType;
                    }
                }
                if (edgeType.equals(EdgeType.NEAREST_NEIGHBORS)) {
                    // If using k-nearest neighbors, once the row has been
                    // processed, report all the k-nearest as being adjacent
                    for (Duple<Integer,Double> t : neighbors.values()) {
                        // Note that the two rows may not have a symmetric
                        // connection so only one value needs to be written
                        affMatrixWriter.println(row + " " + t.x  + " " + t.y);
                    }
                }
                curRow = nextRow;
            }

            File outputFile = File.createTempFile("lcc-output-matrix", ".dat");
            execute(matrixFile, affMatrixFile, dimensions, outputFile);
            return outputFile;
        } catch (IOException ioe) { 
            throw new IOError(ioe);
        }
    }

    public static Matrix project(Matrix m,
                                 int dimensions, 
                                 SimType dataSimilarityMetric,
                                 EdgeType edgeType,
                                 double edgeTypeParam,
                                 EdgeWeighting weighting,
                                 double edgeWeightParam) {
        try {
            File affMatrixFile = File.createTempFile("lcc-aff-matrix",".dat");
            PrintWriter affMatrixWriter = new PrintWriter(affMatrixFile);
            
            int rows = m.rows();
            LOGGER.fine("Calculating the affinity matrix");
            switch (edgeType) {
            case NEAREST_NEIGHBORS: {
                RowComparator rc = new RowComparator();
                for (int i = 0; i < rows; ++i) {
                    MultiMap<Double,Integer> neighborMap = 
                        rc.getMostSimilar(m, i, (int)edgeTypeParam, 
                                          dataSimilarityMetric);
                    Vector row = m.getRowVector(i);
                    for (int n : neighborMap.values()) {
                        double edgeWeight = 
                            getWeight(row, m.getRowVector(n),
                                      weighting, edgeWeightParam);
                        affMatrixWriter.println(i + " " + n  + " " +edgeWeight);
                    }
                }
                break;
            }
            case MIN_SIMILARITY: {
                for (int i = 0; i < rows; ++i) {
                    Vector row1 = m.getRowVector(i);
                    // NOTE: we can compute the upper triangular and report the
                    // symmetric values.
                    for (int j = i+1; j < rows; ++j) {
                        Vector row2 = m.getRowVector(j);

                        double dataSimilarity = Similarity.getSimilarity(
                            dataSimilarityMetric, row1, row2);

                        if (dataSimilarity > edgeTypeParam) {
                            double edgeWeight = 
                                getWeight(row1, row2, 
                                          weighting, edgeWeightParam);
                            // Print out the symmetric edges
                            affMatrixWriter.println(
                                i + " " + j  + " " + edgeWeight);
                            affMatrixWriter.println(
                                j + " " + i  + " " + edgeWeight);
                        }
                    }
                }
                break;
            }
            default:
                assert false : 
                "Cannot construct matrix due to unknown edge type: " + edgeType;
            }
            // Finish writing the affinity matrix so it can be sent to matlab
            affMatrixWriter.close();
            return execute(m, affMatrixFile, dimensions);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }        
    }

    /**
     *
     *
     * @param dataMatrix a matrix where each row is a data points to be
     *        projected
     * @param affMatrixFile the file containing the affinity matrix that
     *        connects data points in the {@code dataMatrixFile}
     * @param dims the number of dimensions to which the matrix should be
     *        reduced
     */
    private static Matrix execute(Matrix dataMatrix, File affMatrixFile, 
                                  int dims) throws IOException {
        // Write the input matrix to a file for Matlab/Octave to use
        File mInput = File.createTempFile("lpp-intput-matrix",".dat");
        mInput.deleteOnExit();
        MatrixIO.writeMatrix(dataMatrix, mInput, MatrixIO.Format.MATLAB_SPARSE);
        // Upon finishing, read the matrix back into memory.
        File output = File.createTempFile("lpp-output-matrix",".dat");
        execute(mInput, affMatrixFile, dims, output);
        return MatrixIO.readMatrix(output, MatrixIO.Format.MATLAB_SPARSE);
    }

    /**
     *
     *
     * @param dataMatrixFile a file containing the original data points to be
     *        projected
     * @param affMatrixFile the file containing the affinity matrix that
     *        connects data points in the {@code dataMatrixFile}
     * @param dims the number of dimensions to which the matrix should be
     *        reduced
     * @param outputMatrix the file to which the output matrix should be written
     */
    private static void execute(File dataMatrixFile, File affMatrixFile, 
                                int dims, File outputMatrix) 
            throws IOException {
        // Decide whether to use Matlab or Octave
        if (isMatlabAvailable())
            invokeMatlab(dataMatrixFile, affMatrixFile, dims, outputMatrix);
            // Ensure that if Matlab isn't present that we can at least use Octave
        else if (isOctaveAvailable())
            invokeOctave(dataMatrixFile, affMatrixFile, dims, outputMatrix);
        else
            throw new IllegalStateException(
                "Cannot find Matlab or Octave to invoke LPP");
    }

    private static void invokeMatlab(File dataMatrixFile, File affMatrixFile, 
                                     int dimensions, File outputFile) 
            throws IOException {

        String commandLine = "matlab -nodisplay -nosplash -nojvm";
        LOGGER.fine(commandLine);
        Process matlab = Runtime.getRuntime().exec(commandLine);
	    
        // Capture the input so we know then Matlab is finished
        BufferedReader br = new BufferedReader(
            new InputStreamReader(matlab.getInputStream()));

        // Create the Matlab-specified output code for the saving the matrix
        String outputStr =
            "save " + outputFile.getAbsolutePath() + " projection -ASCII\n";
        
        // Fill in the Matlab-specific I/O 
        String matlabProgram = LPP_M.format(dataMatrixFile.getAbsolutePath(), 
                                            affMatrixFile.getAbsolutePath(),
                                            dimensions, outputStr);

        // Pipe the program to Matlab for execution
        PrintWriter stdin = new PrintWriter(matlab.getOutputStream());
        BufferedReader stdout = new BufferedReader(
            new InputStreamReader(matlab.getInputStream()));
        BufferedReader stderr = new BufferedReader(
            new InputStreamReader(matlab.getErrorStream()));

        stdin.println(matlabProgram);
        stdin.close();

        // Capture the output.  Matlab will not automatically finish executing
        // after the script ends, so look for the "Finished" text printed at the
        // end to know when to stop the process manually.
        StringBuilder output = new StringBuilder("Matlab LPP output:\n");
        for (String line = null; (line = stdout.readLine()) != null; ) {
            output.append(line).append("\n");
            if (line.equals("Finished")) {
                matlab.destroy();
            }
        }
        LOGGER.fine(output.toString());
	
        int exitStatus = -1;
        try {
            exitStatus = matlab.waitFor();
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        LOGGER.fine("Octave LPP exit status: " + exitStatus);
        
        // If Matlab was not successful throw an error to indicate the output
        // file may be in an inconsistent state
        if (exitStatus != 0) {
            StringBuilder sb = new StringBuilder();
            for (String line = null; (line = stderr.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
            throw new IllegalStateException(
                "Matlab LPP did not finish normally: " + sb);
        }
    }


    private static void invokeOctave(File dataMatrixFile, File affMatrixFile, 
                                     int dimensions, File outputFile) 
            throws IOException {

        // Create the octave file for executing
        File octaveFile = File.createTempFile("octave-LPP",".m");
        // Create the Matlab-specified output code for the saving the matrix
        String outputStr = 
            "save(\"-ascii\", \"" + outputFile.getAbsolutePath()
            + "\", \"projection\");\n";
        
        // Fill in the Matlab-specific I/O 
        String octaveProgram = LPP_M.format(dataMatrixFile.getAbsolutePath(), 
                                            affMatrixFile.getAbsolutePath(),
                                            dimensions, outputStr);
        
        PrintWriter pw = new PrintWriter(octaveFile);
        pw.println(octaveProgram);
        pw.close();
        
        // build a command line where octave executes the previously constructed
        // file
        String commandLine = "octave " + octaveFile.getAbsolutePath();
        LOGGER.fine(commandLine);
        Process octave = Runtime.getRuntime().exec(commandLine);

        BufferedReader stdout = new BufferedReader(
            new InputStreamReader(octave.getInputStream()));
        BufferedReader stderr = new BufferedReader(
            new InputStreamReader(octave.getErrorStream()));

        // Capture the output for logging
        StringBuilder output = new StringBuilder("Octave LPP output:\n");
        for (String line = null; (line = stdout.readLine()) != null; ) {
            output.append(line).append("\n");
        }
        LOGGER.fine(output.toString());
	    
        int exitStatus = -1;
        try {
            exitStatus = octave.waitFor();
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        LOGGER.fine("Octave LPP exit status: " + exitStatus);

        // If Octave wasn't successful, throw an exception with the output
        if (exitStatus != 0) {
            StringBuilder sb = new StringBuilder();
            for (String line = null; (line = stderr.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
            throw new IllegalStateException(
                "Octave LPP did not finish normally: " + sb);
        }
    }
     
    /**
     * Returns {@code true} if Octave is available
     */
    private static boolean isOctaveAvailable() {
	try {
	    Process octave = Runtime.getRuntime().exec("octave -v");
            octave.waitFor();
	} catch (Exception e) {
	    return false;
	}
	return true;	
    }

    /**
     * Returns {@code true} if Matlab is available
     */
    private static boolean isMatlabAvailable() {
	try {
	    Process matlab = Runtime.getRuntime().exec("matlab -h");
            matlab.waitFor();
	} catch (Exception ioe) {
	    return false;
	}
	return true;
    }    
}