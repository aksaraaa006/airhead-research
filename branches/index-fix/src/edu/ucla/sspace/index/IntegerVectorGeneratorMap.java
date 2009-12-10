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


/**
 * A Mapping from Strings to {@code IntegerVector}s.  If a value does not exist
 * for a given string, one will be automatically generated using a {@code
 * IntegerVectorGenerator}.
 *
 * @author Keith Stevens
 */
public class IntegerVectorGeneratorMap implements Map<String, IntegerVector> {

    /**
     * The {@code IntegerVectorGenerator} for generating new vectors.
     */
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

    /**
     * Creates a new Map using a {@code ConcurrentHashMap}.  Vectors will be
     * generated of lenght {@code vectorLength}.
     */
    public IntegerVectorGeneratorMap(IntegerVectorGenerator generator, 
                                     int vectorLength) {
        this(generator, vectorLength,
             new ConcurrentHashMap<String, IntegerVector>());
    }

    /**
     * Creates a new Map with the given Map.  Vectors will be generated of
     * lenght {@code vectorLength}.
     */
    public IntegerVectorGeneratorMap(IntegerVectorGenerator generator, 
                                     int vectorLength,
                                     Map<String, IntegerVector> map) {
        termToVector = map;
        indexVectorLength = vectorLength;
        this.generator = generator;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        termToVector.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return termToVector.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return termToVector.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<String, IntegerVector>> entrySet() {
        return termToVector.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return termToVector.equals(o);
    }

    /**
     * Returns a {@code IntegerVector} for the given term, if no mapping for
     * {@code term} then a new vaue is generated, stored, and returned.
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

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return termToVector.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return termToVector.isEmpty();
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<String> keySet() {
        return termToVector.keySet();
    }

    /**
     * Unsupported.
     */
    public IntegerVector put(String key, IntegerVector vector) {
        throw new UnsupportedOperationException(
                "Vectors may not be inserted into this VectorGeneratorMap.");
    }

    /**
     * Unsupported.
     */
    public void putAll(Map<? extends String, ? extends IntegerVector> m) {
        throw new UnsupportedOperationException(
                "Vectors may not be inserted into this VectorGeneratorMap.");
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return termToVector.size();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<IntegerVector> values() {
        return termToVector.values();
    }

    /**
     * {@inheritDoc}
     */
    public IntegerVector remove(Object key) {
        return termToVector.remove(key);
    }

    /**
     * Serializes a given map to a file.
     */
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

    /**
     * Returns a serialized map stored in the given file.
     */
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
