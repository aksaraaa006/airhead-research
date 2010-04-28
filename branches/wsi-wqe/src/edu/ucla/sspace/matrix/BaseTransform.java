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
 * adhere to the standard {@link Transform} interface.
 */
public abstract class BaseTransform implements Transform {

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public void transform(File inputMatrixFile, MatrixIO.Format format, 
                          File outputMatrixFile) throws IOException {
        GlobalTransform transform = getTransform(inputMatrixFile, format);
        FileTransformer transformer = MatrixIO.fileTransformer(format);
        transformer.transform(inputMatrixFile, outputMatrixFile, transform);
    }
    
    /**
     * {@inheritDoc}
     *
     * </p> Note that this transformation method modifies {@code matrix} in
     * place.
     */
    public Matrix transform(Matrix matrix) {
        return transform(matrix, false);
    }

    /**
     * {@inheritDoc}
     */
    public Matrix transform(Matrix matrix, boolean createNewMatrix) {
        GlobalTransform transform = getTransform(matrix);
        Matrix transformed;

        if (matrix instanceof SparseMatrix) {
            SparseMatrix smatrix = (SparseMatrix) matrix;
            transformed = getTransformMatrix(matrix, true, createNewMatrix);

            // Transform a sparse matrix by only iterating over the non zero
            // values in each row.
            for (int row = 0; row < matrix.rows(); ++row) {
                SparseDoubleVector rowVec = smatrix.getRowVector(row);
                for (int col : rowVec.getNonZeroIndices()) {
                    double newValue = 
                            transform.transform(row, col, rowVec.get(col));
                    transformed.set(row, col, newValue);
                }
            }
        } else {
            transformed = getTransformMatrix(matrix, true, createNewMatrix);

            // Transform dense matrices by inspecting each value in the matrix
            // and having it transformed.
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
     * Returns the matrix in which transformed values should be stored.  When
     * {@code createNew} is false, this simply returns {@code base}.  When
     * {@code createNew} is true, a new matrix with the same dimensions as
     * {@code base} and the requested sparsity is returned.
     */
    private static Matrix getTransformMatrix(Matrix base, 
                                             boolean isSparse,
                                             boolean createNew) {
        if (!createNew)
            return base;
        Matrix.Type type = (isSparse) 
            ? Matrix.Type.SPARSE_IN_MEMORY 
            : Matrix.Type.DENSE_IN_MEMORY;
        return Matrices.create(base.rows(), base.columns(), type);
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
