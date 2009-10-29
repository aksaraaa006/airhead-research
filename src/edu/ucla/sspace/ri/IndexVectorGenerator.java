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

/**
 * An object that generates {@link IndexVector} instances on demand.
 * Implementations should conform to two requirements:
 *
 * <ol> 
 *
 *   <li> Provide a set of publicly accessible constant {@code String} values
 *     that defined the configurable properties of the generator, as well as
 *     provide the default value of those parameters.
 *
 *   <li> Provide a constructor that takes in a {@link java.util.Properties
 *     Properties} object, which contains any of the user-defined values for the
 *     configurable parameters.
 *
 * </ol> 
 *
 */
public interface IndexVectorGenerator {

    /**
     * Creates an {@code IndexVector} with the provided length.
     *
     * @param length the length of the index vector
     *
     * @return an index vector
     */
    IndexVector create(int length);

}