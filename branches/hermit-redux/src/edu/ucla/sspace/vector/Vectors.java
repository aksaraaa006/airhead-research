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


/**
 * A collection of static methods that operate on or return {@link Vector}
 * instances, following a format similar to that of {@link
 * java.util.Collections}.  <p>
 *
 * The methods of this class all throw a {@code NullPointerException} if the
 * {@link Vector} objects provided to them are {@code null}.
 *
 * @author Keith Stevens
 */
public class Vectors {

    /**
     * A private constructor to make this class uninstantiable.
     */
    private Vectors() { }

    /**
     * Returns an immutable view of the given {@code Vector}.
     *
     * @param vector The {@code Vector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static Vector immutable(Vector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an immutable " +
                                           "null vector");
        return new ViewVector(vector);
    }

    /**
     * Returns a thread-safe version of a {@code Vector} that guarantees atomic
     * access to all operations.
     *
     * @param vector The {@code Vector} to decorate as atomic.
     * @return An atomic version of {@code vector}.
     */
    public static Vector atomic(Vector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an atomic " +
                                           "null vector");
        return new AtomicVector(vector);
    }

    /**
     * Returns an immutable view for the given {@code Vector} where its contents
     * may be shifted by an offset and its length may be altered.  All indices
     * not mapped to the provided vector will return 0 when accessed.
     *
     * @param vector The {@code Vector} to decorate as embedded within a View.
     * @param offset The offset at which values in {@code Vector} should be
     *               mapped.
     * @param length The length of {@code vector}.
     */
    public static Vector view(Vector vector, int offset, int length) {
        if (vector == null)
            throw new NullPointerException("Cannot create a view for a " +
                                           "null vector");
        return new ViewVector(vector, offset, length);
    }

    /**
     * Copies all of the values from second {@code Vector} into the first.
     * After the operation, all of the values in {@code dest} will be the same
     * as that of {@code source}.  The legnth of {@code dest} must be as long as
     * the length of {@code source}.  Once completed {@code dest} is returned.
     *
     * @param dest The {@code Vector} to copy values into.
     * @param source The {@code Vector} to copy values from.
     * 
     * @return {@code dest} after being copied from {@code source}.
     *
     * @throws IllegalArgumentException if the length of {@code dest} is less
     *                                  than that of {@code source}.
     */
    public static void copy(Vector dest, Vector source) {
        if (dest.length() < source.length())
            throw new IllegalArgumentException("Vector lengths do not match");

        if (source instanceof SparseVector) {
            SparseVector sv = (SparseVector)source;
            int[] nz = sv.getNonZeroIndices();
            for (int i : nz)
                dest.set(i, sv.get(i));
        }
        else {
            int length = source.length();
            for (int i = 0; i < length; ++i) 
                dest.set(i, source.get(i));
        }
    }
}
