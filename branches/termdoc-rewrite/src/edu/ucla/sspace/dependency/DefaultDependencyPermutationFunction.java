/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.dependency;

import edu.ucla.sspace.index.DefaultPermutationFunction;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.vector.Vector;

import java.io.Serializable;

import java.util.LinkedList;


/**
 * An default {@link DependencyPermutationFunction} for permuting a {@link
 * Vector} based on a dependecny path, represented as a list of {@link
 * DependencyRelations}s.  A passed in {@link PermutationFunction} is used to
 * permute the {@link Vector}s based on the path length.
 *
 * @see edu.ucla.sspace.index.PermutationFunction
 *
 * @author Keith Stevens
 */
public class DefaultDependencyPermutationFunction <T extends Vector>
        implements DependencyPermutationFunction<T>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The internal {@link PermutationFunction} to use for permuting vectors.
     */
    private final PermutationFunction<T> permFunc;

    /**
     * Creates a new {@link DefaultDependencyPermutationFunction} that wraps an
     * existing {@link PermutationFunction}.
     */
    public DefaultDependencyPermutationFunction(
            PermutationFunction<T> permFunc) {
        this.permFunc = permFunc;
    }

    /**
     * {@inheritDoc}
     */
    public T permute(T vector, DependencyPath path) {
        return permFunc.permute(vector, path.length());
    }
}
