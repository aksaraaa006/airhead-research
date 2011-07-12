package edu.ucla.sspace.svs;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.Serializable;


/**
 * Returns the point wise multiplication between two {@link
 * SparseDoubleVector}s.  The first component vector <b>is not</b> modified
 * during the combination.
 *
 * @author Keith Stevens
 */
public class PointWiseCombinor implements VectorCombinor, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector combine(SparseDoubleVector v1, 
                                      SparseDoubleVector v2) {
        return VectorMath.multiplyUnmodified(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector combineUnmodified(SparseDoubleVector v1, 
                                                SparseDoubleVector v2) {
        return VectorMath.multiplyUnmodified(v1, v2);
    }
}
