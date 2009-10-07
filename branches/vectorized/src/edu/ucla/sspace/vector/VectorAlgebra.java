package edu.ucla.sspace.vector;

public class VectorAlgebra {
    /**
     * Return the summation of {@code vector2} to {@code vector1}.  The values
     * of {@code vector1} will also be modified.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector addVector(Vector vector1, Vector vector2) {
        // Skip vectors of different lengths.
        if (vector2.length() != vector1.length())
            return null;

        if (vector2 instanceof SparseVector) {
            SparseVector v = (SparseVector) vector2;
            // If vector is a sparse vector, simply get the non zero values and
            // add them to this instance.
            int[] otherIndicies = v.getNonZeroIndicies();
            for (int index : otherIndicies)
                vector1.add(index, vector2.get(index));
        } else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.get(i);
                if (value != 0d)
                    vector1.add(i, value);
            }
        }
        return vector1;
    }
}
