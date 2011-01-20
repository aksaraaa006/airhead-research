/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.gws;

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.util.Duple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link BasisMapping} implementation where each word and position
 * corresponds to a unique dimension.
 *
 * @author David Jurgens
 */
class WordOrderBasisMapping 
        implements BasisMapping<Duple<String,Integer>,String>, 
                   java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A map that represents the word space by mapping string and their position
     * to a dimension.
     */
    private final Map<Duple<String,Integer>,Integer> termAndPositionToIndex;
    
    /**
     * A cache of the reverse {@code termAndPositionToIndex} mapping.  This
     * field is only updated on calls to {@link #getDimensionDescription(int)}
     * when the mapping has chanaged since the previous call.
     */
    private String[] indexToDescriptionCache;

    /**
     * Creates an empty {@code WordBasisMapping}.
     */
    public WordOrderBasisMapping() {
        termAndPositionToIndex = new HashMap<Duple<String,Integer>,Integer>();
        indexToDescriptionCache = new String[0];
    }

    /**
     * Returns the dimension number corresponding to the word in the provided
     * relative position.
     *
     * @param wordAndPosition a word and its relative position from the focus
     *        word
     *
     * @return the dimension number corresponding to the word in the provided
     *         relative position.
     */
    public int getDimension(Duple<String,Integer> wordAndPosition) {       
        Integer index = termAndPositionToIndex.get(wordAndPosition);
        if (index == null) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termAndPositionToIndex.get(wordAndPosition);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = termAndPositionToIndex.size();
                    termAndPositionToIndex.put(wordAndPosition, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Returns the word mapped to each dimension.
     */
    public String getDimensionDescription(int dimension) {
        if (dimension < 0 || dimension > termAndPositionToIndex.size())
            throw new IllegalArgumentException(
                "invalid dimension: " + dimension);
        // If the cache is out of date, rebuild the reverse mapping.
        if (termAndPositionToIndex.size() > indexToDescriptionCache.length) {
            // Lock to ensure safe iteration
            synchronized(this) {
                indexToDescriptionCache = new String[termAndPositionToIndex.size()];
                for (Map.Entry<Duple<String,Integer>,Integer> e 
                         : termAndPositionToIndex.entrySet()) {
                    Duple<String,Integer> d = e.getKey();
                    indexToDescriptionCache[e.getValue()] = 
                        "word \"" + d.x + "\" relative-position: " + d.y;
                }
            }
        }
        return indexToDescriptionCache[dimension];
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() { 
        return termAndPositionToIndex.size();
    }
}