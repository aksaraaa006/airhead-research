package edu.ucla.sspace.dependency;

import edu.ucla.sspace.index.DefaultPermutationFunction;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.Vector;

import java.io.Serializable;

import java.util.LinkedList;


/**
 * An default {@link DependencyPermutationFunction} for permuting a {@link
 * Vector} based on a dependecny path, represented as a list of word,relation
 * {@link Pair}s.  A passed in {@link PermutationFunction} is used to permute
 * the {@link Vector}s based on the path length.
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
    public T permute(T vector, LinkedList<Pair<String>> path) {
        return permFunc.permute(vector, path.size());
    }
}
