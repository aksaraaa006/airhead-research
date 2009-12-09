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

package edu.ucla.sspace.vector;

import java.util.Arrays;

import java.lang.reflect.Constructor;


/**
 * A collection of methods on {@code Vector}s, following a format similar to
 * that of {@link java.util.Collections}.  The methods collected here will allow
 * decoratored {@code Vector}s to be generated, along with basis algabraic
 * methods.
 *
 * <p>Unless otherwise noted, all returned {@link Vector} instances implement
 * {@code Serializable}.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class Vectors {

    /**
     * A private constructor to make this class uninstantiable.
     */
    private Vectors() { }

    /**
     * Returns a view over the given {@code Vector} as a {@code DoubleVector}.
     * The returned vector is mutable but any changes will be converted into its
     * internal data-type, e.g. {@code int}, which may result in information
     * loss.
     *
     * @param v The {@code Vector} to return as a {@code DoubleVector}.
     *
     * @return a mutable {@code DoubleVector} view of {@code v}
     */
    public static DoubleVector asDouble(Vector v) {
        if (v instanceof IntegerVector) {
            return (v instanceof SparseVector)
                ? new IntAsSparseDoubleVector((SparseIntegerVector)v)
                : new IntAsDoubleVector((IntegerVector)v);
        } 
        else 
            return (v instanceof DoubleVector)
                ? (DoubleVector)v
                : new ViewVectorAsDoubleVector(v);
    }

    /**
     * Returns a vector backed by the specified array.  Any changes to the
     * vector are written through to the array.  This method acts a bridge
     * between array-based and {@code Vector}-based computation.  
     *
     * @param array the array backing the vector
     *
     * @return a {@code Vector} view of the array
     */
    public static DoubleVector asVector(double[] array) {
        return new DoubleArrayAsVector(array);
    }

    /**
     * Returns a vector backed by the specified array.  Any changes to the
     * vector are written through to the array.  This method acts a bridge
     * between array-based and {@code Vector}-based computation.  
     *
     * @param array the array backing the vector
     *
     * @return a {@code Vector} view of the array
     */
    public static IntegerVector asVector(int[] array) {
        return new IntArrayAsVector(array);
    }

    /**
     * Returns an immutable view of the given {@code DoubleVector}.
     *
     * @param vector The {@code DoubleVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static DoubleVector immutableVector(DoubleVector vector) {
        return (vector != null) ? new DoubleVectorView(vector, true) : null;
    }

    /**
     * Returns an immutable view of the given {@code Vector}.
     *
     * @param vector The {@code DoubleVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static Vector immutableVector(Vector vector) {
        return new VectorView(vector, true);
    }

    /**
     * Returns a thread-safe version of a {@code DoubleVector} that guarantees
     * atomic access to all operations.
     *
     * @param vector The {@code DoubleVector} to decorate as atomic.
     * @return An atomic version of {@code vector}.
     */
    public static DoubleVector atomicVector(DoubleVector vector) {
        return (vector != null) ? new AtomicVector(vector) : null;
    }

    /**
     * Returns a syncrhonized view of a given {@code DoubleVector}.  This may
     * show slightly better performance than using an {@code AtomicVector} in
     * some use cases.
     *
     * @param vector The {@code DoubleVector} to decorate as synchronized.
     * @return An atomic version of {@code vector}.
     */
    public static DoubleVector synchronizedVector(DoubleVector vector) {
        return (vector != null) ? new SynchronizedVector(vector) : null;
    }

    /**
     * Returns a subview for the given {@code DoubleVector} with a specified
     * offset and length.
     *
     * @param vector the {@code Vector} whose values will be shown in the view
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code length} is less than zero<li>the sum of {@code
     *         offset} plus {@code length} is greater than the length of {@code
     *         vector}</ul>
     */
    public static DoubleVector viewVector(DoubleVector vector,
                                          int offset,
                                          int length) {
        return (vector != null)
            ? new DoubleVectorView(vector, offset, length)
            : null;
    }

    /**
     * Adds the second {@code Vector} to the first {@code Vector} and returns 
     * the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector add(Vector vector1, Vector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        if (vector2 instanceof IntegerVector &&
            vector1 instanceof DoubleVector)
            return add(vector1, Vectors.asDouble(vector2));
        if (vector2 instanceof SparseVector)
            addSparseValues(vector1, vector2);
        else {
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.getValue(i).doubleValue() +
                               vector1.getValue(i).doubleValue();
                vector1.set(i, value);
            }
        }

        return vector1;
    }

    /**
     * Adds the second {@code DoubleVector} to the first {@code DoubleVector}
     * and returns the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    private static DoubleVector add(DoubleVector vector1,
                                    DoubleVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            addSparseValues(vector1, vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, value);
            }
        }
        return vector1;
    }

    /**
     * Adds the second {@code IntegerVector} to the first {@code IntegerVector}
     * and returns the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    private static IntegerVector add(IntegerVector vector1,
                                     IntegerVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector) 
            addSparseValues(vector1, vector2);
        if (vector2 instanceof TernaryVector)
            addTernaryValues(vector1, (TernaryVector)vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                int value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, value);
            }
        }
        return vector1;
    }

    /**
     * Returns a new {@code Vector} which is the summation of {@code vector2}
     * and {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector addUnmodified(Vector vector1, Vector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        return addUnmodified(Vectors.asDouble(vector1),
                             Vectors.asDouble(vector2));
    }

    /**
     * Returns a new {@code DoubleVector} which is the summation of {@code
     * vector2} and {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    private static DoubleVector addUnmodified(DoubleVector vector1,
                                              DoubleVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        DoubleVector finalVector = Vectors.copyOf(vector1);
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            addSparseValues(finalVector, vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.get(i) + vector1.get(i);
                if (value != 0d)
                    finalVector.add(i, value);
            }
        }
        return finalVector;
    }

    /**
     * Returns a new {@code IntegerVector} which is the summation of {@code
     * vector2} and {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    private static IntegerVector addUnmodified(IntegerVector vector1,
                                               IntegerVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        IntegerVector finalVector = Vectors.copyOf(vector1);
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            addSparseValues(finalVector, vector2);
        else if (vector2 instanceof TernaryVector)
            addTernaryValues(finalVector, (TernaryVector)vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                int value = vector2.get(i) + vector1.get(i);
                if (value != 0d)
                    finalVector.add(i, value);
            }
        }
        return finalVector;
    }

    /**
     * Adds two {@code Vector}s with some scalar weight for each {@code Vector}.
     *
     * @param vector1 The vector values should be added to.
     * @param weight1 The weight of values in {@code vector1}
     * @param vector2 The vector values that should be added to {@code vector1}
     * @param weight2 The weight of values in {@code vector2}
     *
     * @param {@code vector1}
     */
    public static Vector addWithScalars(Vector vector1, double weight1,
                                        Vector vector2, double weight2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        for (int i = 0; i < vector2.length(); ++i) {
            double value = vector1.getValue(i).doubleValue() * weight1 +
                           vector2.getValue(i).doubleValue()  * weight2;
            vector1.set(i, value);
        }
        return vector1;
    }

    /**
     * Adds two {@code DoubleVector}s with some scalar weight for each {@code
     * DoubleVector}.
     *
     * @param vector1 The vector values should be added to.
     * @param weight1 The weight of values in {@code vector1}
     * @param vector2 The vector values that should be added to {@code vector1}
     * @param weight2 The weight of values in {@code vector2}
     *
     * @param {@code vector1}
     */
    public static Vector addWithScalars(DoubleVector vector1, double weight1,
                                        DoubleVector vector2, double weight2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        for (int i = 0; i < vector2.length(); ++i) {
            double value = vector1.get(i) * weight1 +
                           vector2.get(i) * weight2;
            vector1.set(i, value);
        }
        return vector1;
    }

    /**
     * Adds two {@code IntegerVector}s with some scalar weight for each {@code
     * Vector}.
     *
     * @param vector1 The vector values should be added to.
     * @param weight1 The weight of values in {@code vector1}
     * @param vector2 The vector values that should be added to {@code vector1}
     * @param weight2 The weight of values in {@code vector2}
     *
     * @param {@code vector1}
     */
    public static Vector addWithScalars(IntegerVector vector1, int weight1,
                                        IntegerVector vector2, int weight2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        for (int i = 0; i < vector2.length(); ++i) {
            double value = vector1.get(i) * weight1 +
                           vector2.get(i) * weight2;
            vector1.set(i, value);
        }
        return vector1;
    }

    /**
     * Copy all of the values from one {@code Vector} into another.  After the
     * operation, all of the values in {@code dest} will be the same as that of
     * {@code source}.  The legnth of {@code dest} must be as long as the length
     * of {@code source}.  Once completed {@code dest} is returned.
     *
     * @param dest The {@code Vector} to copy values into.
     * @param source The {@code Vector} to copy values from.
     * 
     * @return {@code dest} after being copied from {@code source}.
     *
     * @throws IllegalArgumentException if the length of {@code dest} is less
     *                                  than that of {@code source}.
     */
    public static Vector copy(Vector dest, Vector source) {
        for (int i = 0; i < source.length(); ++i)
            dest.set(i, source.getValue(i).doubleValue());
        return dest;
    }

    /**
     * Create a copy of a given {@code DoubleVector} with the same type as the
     * original.
     *
     * @param source The {@code Vector} to copy.
     *
     * @return A copy of {@code source} with the same type.
     */
    public static DoubleVector copyOf(DoubleVector source) {
        DoubleVector result = null;

        if (source instanceof DenseVector) {
            result = new DenseVector(source.length());
            for (int i = 0; i < source.length(); ++i)
                result.set(i, source.get(i));
        } else if (source instanceof CompactSparseVector) {
            result = new CompactSparseVector(source.length());
            copyFromSparseVector(result, source);
        } else if (source instanceof AmortizedSparseVector) {
            result = new AmortizedSparseVector(source.length());
            copyFromSparseVector(result, source);
        } else {
            // Create a copy of the given class using reflection.  This code
            // assumes that the given implemenation of Vector has a constructor
            // which accepts another Vector.
            try {
                Class<? extends DoubleVector> sourceClazz = source.getClass();
                Constructor<? extends DoubleVector> constructor =
                    sourceClazz.getConstructor(DoubleVector.class);
                result = (DoubleVector) constructor.newInstance(source);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        return result;
    }

    /**
     * Create a copy of a given {@code Vector}.
     *
     * @param source The {@code Vector} to copy.
     *
     * @return A copy of {@code source} with the same type.
     */
    public static Vector copyOf(Vector source) {
        Vector result = new DenseVector(source.length());
        for (int i = 0; i < source.length(); ++i) 
            result.set(i, result.getValue(i));
        return result;
    }

    /**
     * Create a copy of a given {@code IntegerVector} with the same type as the
     * original.
     *
     * @param source The {@code Vector} to copy.
     *
     * @return A copy of {@code source} with the same type.
     */
    public static IntegerVector copyOf(IntegerVector source) {
        IntegerVector result = null;

        if (source instanceof TernaryVector) {
            TernaryVector v = (TernaryVector) source;
            int[] pos = v.positiveDimensions();
            int[] neg = v.negativeDimensions();
            result = new TernaryVector(source.length(),
                                       Arrays.copyOf(pos, pos.length),
                                       Arrays.copyOf(neg, neg.length));
        } else if (source instanceof SparseVector) {
            result = new SparseIntVector(source.length());
            copyFromSparseVector(result, source);
        } else {
            result = new DenseIntVector(source.length());
            for (int i = 0; i < source.length(); ++i)
                result.set(i, source.get(i));
        }
        return result;
    }

    /**
     * Creates a {@code Vector} instance of the same type and length of the
     * provided vector.
     *
     * @param vector a vector whose type and length should be used when creating
     *        a new vector with the same properties
     *
     * @return a vector with the same type and length as the provided vector
     *
     * @throw IllegalArgumentException if <ul><li>the class of the provided
     *        vector does not have a constructor that takes in an {@code int} to
     *        specify the length <li>the class of the provided vector cannot be
     *        instantiated</ul>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Vector> T instanceOf(T vector) {
        // Check for known vector types to avoid reflection overhead
        if (vector instanceof SparseIntVector) {
            return (T)(new SparseIntVector(vector.length()));
        }
        // Remaining cases of vector types is being left unfinished until the
        // vector name refactoring is finished.  -jurgens 12/7/09
        else {
            try {
                Class<T> clazz = (Class<T>)vector.getClass();
                Constructor<T> c = clazz.getConstructor(Integer.TYPE);
                T copy = c.newInstance(new Object[] { vector.length() });
                return copy;
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate a vector"
                    + " of type " + vector.getClass(), e);
            }
        }
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in {@code left}.  This is an element by element multiplication.
     *
     * @param left The left {@code Vector} to multiply, and contain the result
     *             values.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static Vector multiply(Vector left, Vector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        for (int i = 0; i < left.length(); ++i)
            left.set(i, left.getValue(i).doubleValue() *
                        right.getValue(i).doubleValue());
        return left;
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in {@code left}.  This is an element by element multiplication.
     *
     * @param left The left {@code Vector} to multiply, and contain the result
     *             values.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static DoubleVector multiply(DoubleVector left, DoubleVector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        for (int i = 0; i < left.length(); ++i)
            left.set(i, left.get(i) * right.get(i));
        return left;
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in {@code left}.  This is an element by element multiplication.
     *
     * @param left The left {@code Vector} to multiply, and contain the result
     *             values.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static IntegerVector multiply(IntegerVector left,
                                         IntegerVector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        for (int i = 0; i < left.length(); ++i)
            left.set(i, left.get(i) * right.get(i));
        return left;
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in a new {@code Vector}.  This is an element by element
     * multiplication.
     *
     * @param left The left {@code Vector} to multiply.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static DoubleVector multiplyUnmodified(DoubleVector left,
                                                  DoubleVector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        DoubleVector result;
        if (left instanceof SparseVector ||
            right instanceof SparseVector)
            result = new CompactSparseVector(left.length());
        else
            result = new DenseVector(left.length());

        for (int i = 0; i < left.length(); ++i)
            result.set(i, left.get(i) * right.get(i));
        return result;
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(DoubleVector destination,
                                        DoubleVector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices)
            destination.add(index, source.get(index));
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(IntegerVector destination,
                                        IntegerVector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices)
            destination.add(index, source.get(index));
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(Vector destination,
                                        Vector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices) {
            double value = destination.getValue(index).doubleValue() +
                           source.getValue(index).doubleValue();
            destination.set(index, value);
        }
    }

    /**
     * Adds the values from a {@code TernaryVector} to an {@code IntegerVector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addTernaryValues(IntegerVector destination,
                                         TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.add(p, 1);
        for (int n : source.negativeDimensions())
            destination.add(n, -1);
    }

    /**
     * Adds the values from a {@code TernaryVector} to a {@code DoubleVector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addTernaryValues(DoubleVector destination,
                                         TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.add(p, 1);
        for (int n : source.negativeDimensions())
            destination.add(n, -1);
    }

    /**
     * Adds the values from a {@code TernaryVector} to a {@code Vector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addTernaryValues(Vector destination,
                                         TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.set(p, 1 + destination.getValue(p).doubleValue());
        for (int n : source.negativeDimensions())
            destination.set(n, -1 + destination.getValue(n).doubleValue());
    }

    /**
     * Copy values from a {@code SparseVector} into another vector
     *
     * @param destination The {@code Vector to copy values into.
     * @param source The {@code @SparseVector} to copy values from.
     */
    private static void copyFromSparseVector(DoubleVector destination,
                                             DoubleVector source) {
        int[] nonZeroIndices = ((SparseVector) source).getNonZeroIndices();
        for (int i = 0; i < nonZeroIndices.length; ++i)
            destination.set(i, source.get(i));
    }

    /**
     * Copy values from a {@code SparseVector} into another vector
     *
     * @param destination The {@code Vector to copy values into.
     * @param source The {@code @SparseVector} to copy values from.
     */
    private static void copyFromSparseVector(IntegerVector destination,
                                             IntegerVector source) {
        int[] nonZeroIndices = ((SparseVector) source).getNonZeroIndices();
        for (int i = 0; i < nonZeroIndices.length; ++i)
            destination.set(i, source.get(i));
    }


    /**
     * A utility interface for joining the {@link SparseVector} and {@link
     * IntegerVector} interfaces into a castable type.
     */
    static interface SparseIntegerVector 
        extends SparseVector<Integer>, IntegerVector { }

    /**
     * A utility interface for joining the {@link SparseVector} and {@link
     * DoubleVector} interfaces into a castable type.
     */
    static interface SparseDoubleVector 
        extends SparseVector<Double>, DoubleVector { }

}
