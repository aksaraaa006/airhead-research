/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.ri;

import java.util.Properties;


/**
 * An class that generates {@link RandomIndexVector} instances based on
 * configurable properties.  This class supports two properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #VALUES_TO_SET_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_INDEX_VECTOR_VALUES}
 *
 * <dd style="padding-top: .5em">This variable sets the number of bits to set in
 *      an index vector. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #INDEX_VECTOR_VARIANCE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_INDEX_VECTOR_VARIANCE}
 *
 * <dd style="padding-top: .5em">This variable sets the variance in the number
 *      of bits to set in an index vector.  For example, having {@value
 *      #VALUES_TO_SET_PROPERTY}{@code =4} and setting this property to {@code
 *      2} would mean that {@code 4 &plusmn; 2} value would be randomly set in
 *      each index vector. <p>
 *
 * </dl>
 *
 * @see RandomIndexVector
 * @see RandomIndexing
 */
public class RandomIndexVectorGenerator implements IndexVectorGenerator {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
	"edu.ucla.sspace.ri.RandomIndexVectorGenerator";

    /**
     * The property to specify the number of values to set in an {@link
     * IndexVector}.
     */
    public static final String VALUES_TO_SET_PROPERTY = 
	PROPERTY_PREFIX + ".values";

    /**
     * The property to specify the variance in the number of values to set in an
     * {@link IndexVector}.
     */
    public static final String INDEX_VECTOR_VARIANCE_PROPERTY = 
	PROPERTY_PREFIX + ".variance";

    /**
     * The default number of values to set in an {@link IndexVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_VALUES = 4;

    /**
     * The default random variance in the number of values that are set in an
     * {@code IndexVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_VARIANCE = 0;

    /**
     * The number of values to set in an {@link IndexVector}.
     */
    private final int numVectorValues;

    /**
     * The variance in the number of values that are set in an {@code
     * IndexVector}.
     */
    private final int variance;

    /**
     * Constructs this instance using the system properties.
     */
    public RandomIndexVectorGenerator() {
	this(System.getProperties());
    }

    /**
     * Constructs this instance using the provided properties.
     */
    public RandomIndexVectorGenerator(Properties properties) {

	String numVectorValuesProp = 
	    properties.getProperty(VALUES_TO_SET_PROPERTY);
	numVectorValues = (numVectorValuesProp != null)
	    ? Integer.parseInt(numVectorValuesProp)
	    : DEFAULT_INDEX_VECTOR_VALUES;

	String varianceProp =
	    properties.getProperty(INDEX_VECTOR_VARIANCE_PROPERTY);
	variance = (varianceProp != null)
	    ? Integer.parseInt(varianceProp)
	    : DEFAULT_INDEX_VECTOR_VARIANCE;
    }

    /**
     * Creates an {@code IndexVector} with the provided length.
     *
     * @param length the length of the index vector
     *
     * @return an index vector
     */
    public IndexVector create(int length) {
	return new RandomIndexVector(length, numVectorValues, variance);
    }

}