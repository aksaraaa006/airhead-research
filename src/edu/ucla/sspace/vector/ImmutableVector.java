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
 * An immutable {@code Vector}.  This class allows a {@code SemanticSpace} or a
 * {@code Matrix} to return a {@code Vector} stored internally without allowing
 * other classes the ability to alter the {@code Vector}.  Note that the
 * original {@code Vector} can still be alterned by the object owning it, but
 * classes given a {@code ImmutableVector} cannot make modififications.
 *
 * @author Keith Stevens
 */
public class ImmutableVector implements Vector {

    /**
     * The actual vector this {@code ImmutableVector} is decorating.
     */
    private final Vector vector;

    /**
     * Create a new {@code ImmutableVector} around an already existing {@code
     * Vector} providing read only access.
     *
     * @param v The {@code Vector} to decorate.
     */
    public ImmutableVector(Vector v) {
        vector = v;
    }

    /**
     * Method not implemented.
     */
    public double add(int index, double delta) {
        throw new UnsupportedOperationException("add is not supported in an " +
                "ImmutableVector");
    }

    /**
     * Method not implemented.
     */
    public void set(int index, double value) {
        throw new UnsupportedOperationException("set is not supported in an " +
                "ImmutableVector");
    }

    /**
     * Method not implemented.
     */
    public void set(double[] values) {
        throw new UnsupportedOperationException("set is not supported in an " +
                "ImmutableVector");
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray(int size) {
        return vector.toArray(size);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length();
    }

    public String toString() {
        return vector.toString();
    }
}
