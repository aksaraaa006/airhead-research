package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * @author Keith Stevens
 */
public class RowScaledSparseMatrix extends RowScaledMatrix
                                   implements SparseMatrix {

    /**
     * The backing instance of the matrix.
     */
    private final SparseMatrix m;

    private final DoubleVector scales;

    /**
     * Creates a {@code RowScaledSparseMatrix} that provides scaled read only
     * access to the provided {@code SparseMatrix} instance.
     */
    public RowScaledSparseMatrix(SparseMatrix matrix, DoubleVector v) {
        super(matrix, v);
        this.m = matrix;
        this.scales = v;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int row) {
        throw new UnsupportedOperationException("cannot get row");
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        return new ScaledSparseDoubleVector(
                m.getRowVector(row), scales.get(row));
    }
}
