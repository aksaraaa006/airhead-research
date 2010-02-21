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

package edu.ucla.sspace.common;

import edu.ucla.sspace.vector.Vector;

import java.util.Map;


/**
 * A sub interface for {@link SemanticSpace} algorithms that use index vectors
 * to approximate occurrence frequencies.  This interface is designed to permit
 * the serialization and de-serialization of token to index vector mappings so
 * that multiple runs of a {@link ApproximationSpace} over the same corpus can
 * re-use the mappings and produce reasonably consistent results.
 *
 * @author Keith Stevens
 */
public interface ApproximationSpace<T extends Vector> extends SemanticSpace {

    /**
     * Returns the token to {@link Vector} mapping used by the {@link
     * ApproximationSpace}.  This allows for mappings to be serialized.
     */
    Map<String,T> getWordToIndexVector();

    /**
     * Assigns the token to {@link Vector} mapping to be used by this {@link
     * ApproximationSpace}.  The {@link ApproximationSpace} should take
     * ownership of the map.  Typically, the provided map will be from a
     * previous run of the same {@link SemanticSpace} and may be a {@link
     * edu.ucla.sspace.index.DoubleVectorGeneratorMap DoubleVectorGeneratorMap}
     * or a {@link edu.ucla.sspace.index.IntegerVectorGeneratprMap
     * IntegerVectorGeneratorMap}.
     */
    void setWordToIndexVector(Map<String,T> m);
}
