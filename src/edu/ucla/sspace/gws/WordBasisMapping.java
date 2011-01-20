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
 * A {@link BasisMapping} implementation where each word corresponds to a unique
 * dimension regardless of its word position.  
 *
 * @author David Jurgens
 */
class WordBasisMapping 
        implements BasisMapping<Duple<String,Integer>,String>, 
                   java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A map that represents the word space by mapping raw strings to which
     * dimension they are represented by.
     */
    private final Map<String,Integer> termToIndex;
    
    /**
     * A cache of the reverse {@code termToIndex} mapping.  This field is only
     * updated on calls to {@link #getDimensionDescription(int)} when the
     * mapping has chanaged since the previous call.
     */
    private String[] indexToTermCache;

    /**
     * Creates an empty {@code WordBasisMapping}.
     */
    public WordBasisMapping() {
        termToIndex = new HashMap<String,Integer>();
        indexToTermCache = new String[0];
    }

    /**
     * Returns the dimension number corresponding to the word regardless of its
     * position.
     *
     * @param wordAndPosition a word and its relative position from the focus
     *        word
     *
     * @return the dimension for the word, ignoring its positoion,
     */
    public int getDimension(Duple<String,Integer> wordAndPosition) {       
        String token = wordAndPosition.x;
        Integer index = termToIndex.get(token);
        if (index == null) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termToIndex.get(token);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = termToIndex.size();
                    termToIndex.put(token, i);
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
        if (dimension < 0 || dimension > termToIndex.size())
            throw new IllegalArgumentException(
                "invalid dimension: " + dimension);
        // If the cache is out of date, rebuild the reverse mapping.
        if (termToIndex.size() > indexToTermCache.length) {
            // Lock to ensure safe iteration
            synchronized(this) {
                indexToTermCache = new String[termToIndex.size()];
                for (Map.Entry<String,Integer> e : termToIndex.entrySet())
                    indexToTermCache[e.getValue()] = e.getKey();
            }
        }
        return indexToTermCache[dimension];
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() { 
        return termToIndex.size();
    }
}