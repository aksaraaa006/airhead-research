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
 * An interface for semantic vectors.  This interface allows subclasses to
 * implement the vector with any kind of underlying data type, but the input and
 * output data types must be doubles.
 */
// TODO: Rename to Vector.
public interface Vector {
    /**
     * Change the value in the semantic vector by a specified amount.  If there is
     * not a value set at index, delta should be set to the actual value.
     *
     * @param index index to change.
     * @param delta the amount to change by.
     */
    public double add(int index, double delta);

    /**
     * Add the contents of {@code vector} to the current {@code Vector}.
     * Underlying implemntations may have a more efficient method of adding
     * vectors together based on what type of Vector is passed in.
     *
     * @param vector A Vector to be summed into the current vector.
     */
    public void addVector(Vector vector);

    /**
     * Set the value in the semantic vector.
     *
     * @param index index to set.
     * @param value value to set in the vector.
     */
    public void set(int index, double value);

    /**
     * Set all the values in the vector.
     *
     * @param values Values to set for this vector.
     */
    public void set(double[] values);

    /**
     * Return the value of the semantic vector at the given index.
     *
     * @param index index to retrieve.
     * @return value at index.
     */
    public double get(int index);

    /**
     * Return a double array representing this semantic vector.
     *
     * @param size The maximum size of the array returned.
     * @return A double array of this vector.
     */
    public double[] toArray(int size);

    /**
     * Return the size of the {@code Vector}.
     *
     * @return Size of the vector.
     */
    public int length();
}
