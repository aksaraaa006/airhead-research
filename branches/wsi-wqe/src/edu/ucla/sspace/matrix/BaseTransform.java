/*
 * Copyright 2009 Keith Stevens
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

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.File;
import java.io.IOException;


/**
 * An abstract {@link Transform} implemenation that most transforms can extend.
 * Any transform that can be implemented as a {@link GlobalTransform} can simply
 * define the a {@link GlobalTransform} and then subclass this abstract class to
 * adhere to the standard {@link Transform} interface.  Additionally, for
 * transforms of any {@link Matrix} instance, the transformation will be done
 * with multiple threads.
 */
public abstract class BaseTransform implements Transform {

    /**
     * Transforms the matrix in the file using the log-entropy transform and
     * returns a temporary file containing the result.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the matrix
     *
     * @return a file with the transformed version of the input.  This file is
     *         marked to be deleted when the JVM exits.
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    public File transform(File inputMatrixFile, MatrixIO.Format format) 
             throws IOException {
        // create a temp file for the output
        File output = File.createTempFile(
                inputMatrixFile.getName() + ".matrix-transform", ".dat");
        transform(inputMatrixFile, format, output);
        return output;
    }

    /**
     * Transforms the input matrix using the log-entropy transform and
     * writes the result to the file for the output matrix.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the input matrix, and the format in which the
     *        output matrix will be written
     * @param outputMatrixFile the file to which the transformed matrix will be
     *        written
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    public void transform(File inputMatrixFile, MatrixIO.Format format, 
                          File outputMatrixFile) throws IOException {
        GlobalTransform transform = getTransform(inputMatrixFile, format);
        FileTransformer transformer = MatrixIO.fileTransformer(format);
        transformer.transform(inputMatrixFile, outputMatrixFile, transform);
    }
    
    /**
     * Returns the log-entropy transformm of the input matrix.
     *
     * @param matrix the matrix to be transformed
     *
     * @return the transformed version of the input matrix
     */
    public Matrix transform(Matrix matrix) {
        GlobalTransform transform = getTransform(matrix);

        Matrix transformed;

        if (matrix instanceof SparseMatrix) {
            SparseMatrix smatrix = (SparseMatrix) matrix;
            transformed = Matrices.create(matrix.rows(), matrix.columns(), 
                                          Matrix.Type.SPARSE_IN_MEMORY);

            for (int row = 0; row < matrix.rows(); ++row) {
                SparseDoubleVector rowVec = smatrix.getRowVector(row);
                for (int col : rowVec.getNonZeroIndices()) {
                    double newValue = 
                            transform.transform(row, col, rowVec.get(col));
                    transformed.set(row, col, newValue);
                }
            }
        } else {
            transformed = Matrices.create(matrix.rows(), matrix.columns(), 
                                          Matrix.Type.DENSE_IN_MEMORY);
            for (int row = 0; row < matrix.rows(); ++row) {
                for (int col = 0; col < matrix.columns(); ++col) {
                    double newValue = 
                            transform.transform(row, col, matrix.get(row, col));
                    transformed.set(row, col, newValue);
                }
            }
        }

        return transformed;
    }

    /**
     * Returns a {@link GlobalTransform} for a {@link Matrix}.
     */
    protected abstract GlobalTransform getTransform(Matrix matrix);

    /**
     * Returns a {@link GlobalTransform} for a File of the given format.
     */
    protected abstract GlobalTransform getTransform(File inputMatrixFile,
                                                    MatrixIO.Format format);
}
