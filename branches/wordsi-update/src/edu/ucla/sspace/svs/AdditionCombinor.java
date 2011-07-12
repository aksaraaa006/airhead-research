package edu.ucla.sspace.svs;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.Serializable;


/**
 * Returns the point wise addition between two {@link SparseDoubleVector}s.  The
 * first vector <b>is</b> modified during the combination.
 *
 * @author Keith Stevens
 */
public class AdditionCombinor implements VectorCombinor, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector combine(SparseDoubleVector v1, 
                                      SparseDoubleVector v2) {
        return (SparseDoubleVector) VectorMath.add(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector combineUnmodified(SparseDoubleVector v1, 
                                                SparseDoubleVector v2) {
        return (SparseDoubleVector) VectorMath.addUnmodified(v1, v2);
    }
}
