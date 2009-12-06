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

package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.SparseIntVector;
import edu.ucla.sspace.vector.IntegerVector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOError;
import java.io.IOException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;


public class IntegerVectorGeneratorMap implements Map<String, IntegerVector> {

    private final IntegerVectorGenerator generator;

    /**
     * The number of dimensions created in each {@code Vector}.
     */
    private final int indexVectorLength;

    /**
     * A mapping from terms to their Index Vector, stored as a {@code
     * Vector}.
     */
    private final Map<String, IntegerVector> termToVector;

    public IntegerVectorGeneratorMap(IntegerVectorGenerator generator, 
                                     int vectorLength) {
        this(generator, vectorLength,
             new ConcurrentHashMap<String, IntegerVector>());
    }

    public IntegerVectorGeneratorMap(IntegerVectorGenerator generator, 
                                     int vectorLength,
                                     Map<String, IntegerVector> map) {
        termToVector = map;
        indexVectorLength = vectorLength;
        this.generator = generator;
    }

    public void clear() {
        termToVector.clear();
    }

    public boolean containsKey(Object key) {
        return termToVector.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return termToVector.containsValue(value);
    }

    public Set<Map.Entry<String, IntegerVector>> entrySet() {
        return termToVector.entrySet();
    }

    public boolean equals(Object o) {
        return termToVector.equals(o);
    }

    /**
     * Return a {@code Vector} for the given term, if no {@code Vector} exists
     * for {@code term}, then generate a new one. 
     *
     * @param term The term specifying the index {@code Vector} to return.
     *
     * @return A {@code Vector} corresponding to {@code term}.
     */
    public IntegerVector get(Object term) {
        // Check that an index vector does not already exist.
        IntegerVector v = termToVector.get(term);
        if (v == null) {
            synchronized (this) {
                // Confirm that some other thread has not created an index
                // vector for this term.
                v = termToVector.get(term);
                if (v == null) {
                    // Generate the index vector for this term and store it.
                    v = generator.generateRandomVector(indexVectorLength);
                    termToVector.put((String) term, v);
                }
            }
        }
        return v;
    }

    public int hashCode() {
        return termToVector.hashCode();
    }

    public boolean isEmpty() {
        return termToVector.isEmpty();
    }
    
    public Set<String> keySet() {
        return termToVector.keySet();
    }

    public IntegerVector put(String key, IntegerVector vector) {
        throw new UnsupportedOperationException(
                "Vectors may not be inserted into this VectorGeneratorMap.");
    }

    public void putAll(Map<? extends String, ? extends IntegerVector> m) {
        throw new UnsupportedOperationException(
                "Vectors may not be inserted into this VectorGeneratorMap.");
    }

    public int size() {
        return termToVector.size();
    }

    public Collection<IntegerVector> values() {
        return termToVector.values();
    }

    public IntegerVector remove(Object key) {
        return termToVector.remove(key);
    }

    public void saveMap(File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream outStream = new ObjectOutputStream(fos);
            outStream.writeObject(this);
            outStream.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public static IntegerVectorGeneratorMap loadMap(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream inStream = new ObjectInputStream(fis);
            IntegerVectorGeneratorMap vectorMap = 
                (IntegerVectorGeneratorMap) inStream.readObject();
            inStream.close();
            return vectorMap;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new Error(cnfe);
        }
    }
}
