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

package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.IndexVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

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
 */
public class RandomIndexGenerator implements IndexGenerator {

    /**
     * A random number generator that can be accessed to other classes which
     * will rely on the same source of random values.
     */
    public static final Random RANDOM = new Random();

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.index.RandomIndexGenerator";

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
     * The default number of dimensions to create in each {@code IndexVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_LENGTH = 20000;

    /**
     * The default random variance in the number of values that are set in an
     * {@code IndexVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_VARIANCE = 0;

    /**
     * The number of values to set in an {@link IndexVector}.
     */
    private int numVectorValues;

    /**
     * The number of dimensions created in each {@code IndexVector}.
     */
    private int indexVectorLength;

    /**
     * The variance in the number of values that are set in an {@code
     * IndexVector}.
     */
    private int variance;

    /**
     * A mapping from terms to their Index Vector, stored as a {@code
     * Vector}.
     */
    private Map<String, IndexVector> termToRandomIndex;

    /**
     * Constructs this instance using the system properties.
     */
    public RandomIndexGenerator() {
        this(System.getProperties());
    }

    /**
     * Constructs this instance using the provided properties.
     */
    public RandomIndexGenerator(Properties properties) {
        termToRandomIndex = new ConcurrentHashMap<String, IndexVector>();

        String indexVectorLengthProp =
            properties.getProperty(IndexGenerator.INDEX_VECTOR_LENGTH_PROPERTY);
        indexVectorLength = (indexVectorLengthProp != null)
            ? Integer.parseInt(indexVectorLengthProp)
            : DEFAULT_INDEX_VECTOR_LENGTH;

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
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void loadIndexVectors(File file) {
        // Erase any existing mappings since they are likely to conflict with
        // the existing index vectors.
        termToRandomIndex.clear();
        try {
            ObjectInputStream inStream = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(file)));
            termToRandomIndex = (Map<String, IndexVector>) inStream.readObject();
            /*
            // Read the required values defining this generator.  If the read
            // values do not match what was passed in during construction, throw
            // an error.
            numVectorValues = inStream.readInt();
            variance = inStream.readInt();
            indexVectorLength = inStream.readInt();

            // Read the mappings stored in the given file.  For each mapping
            // stored the following order will be read:
            //    1) The string being mapped
            //    2) the number of positive values
            //    3) the positive values
            //    4) the number of negative values
            //    5) the negative values
            // The read mappings will then be stored in termToRandomIndex.
            int numMappings = inStream.readInt();
            for (int i = 0; i > numMappings; ++i) {
                String term = inStream.readUTF();

                int numPositive = inStream.readInt();
                int[] positives = new int[numPositive];
                for (int j = 0; j < numPositive; ++j)
                    positives[j] = inStream.readInt();

                int numNegatives = inStream.readInt();
                int[] negatives = new int[numNegatives];
                for (int j = 0; j < numNegatives; ++j)
                    negatives[j] = inStream.readInt();
                IndexVector vector =
                    new IndexVector(indexVectorLength, positives, negatives);
                termToRandomIndex.put(term, vector);
            }
            */
            inStream.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("The given index vector file " +
                                               "cannot be deserialized");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void saveIndexVectors(File file) {
        try {
            ObjectOutputStream stream = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(file)));
            stream.writeObject(termToRandomIndex);
            /*
            // Write the required values which define how index vectors are
            // generated.  These won't be use when the vectors are loaded, but
            // instead used to ensure that the values match with what is
            // constructued. 
            stream.writeInt(numVectorValues);
            stream.writeInt(variance);
            stream.writeInt(indexVectorLength);

            // Write the index vector for each string mapped in this generator.
            // For each mapping the order will be:
            //   1) utf version of the string
            //   2) number of positive dimensions
            //   3) all positive dimensions
            //   4) number of negative dimensions
            //   5) all negative dimensions
            stream.writeInt(termToRandomIndex.size());
            for (Map.Entry<String, IndexVector> entry :
                    termToRandomIndex.entrySet()) {
                stream.writeUTF(entry.getKey());
                IndexVector v = entry.getValue();

                // Write the positive dimensions.
                int[] pos = v.positiveDimensions();
                stream.writeInt(pos.length);
                for (int p : pos)
                    stream.writeInt(p);

                // Write the negative dimensions.
                int[] neg = v.negativeDimensions();
                stream.writeInt(neg.length);
                for (int n : neg)
                    stream.write(n);
            }
            */
            stream.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Creates an {@code IndexVector} with the provided length.
     *
     * @param length the length of the index vector
     *
     * @return an index vector
     */
    public IndexVector generateRandomVector() {
        HashSet<Integer> pos = new HashSet<Integer>();
        HashSet<Integer> neg = new HashSet<Integer>();
        
        // Randomly decide how many bits to set in the index vector based on the
        // variance.
        int bitsToSet = numVectorValues +
            (int)(RANDOM.nextDouble() * variance *
                  ((RANDOM.nextDouble() > .5) ? 1 : -1));

        for (int i = 0; i < bitsToSet; ++i) {
            boolean picked = false;
            // loop to ensure we actually pick the full number of bits
            while (!picked) {
                // pick some random index
                int index = (int)(RANDOM.nextDouble() * indexVectorLength);
                    
                // check that we haven't already added this index
                if (pos.contains(index) || neg.contains(index))
                    continue;
                    
                // decide positive or negative
                ((RANDOM.nextDouble() > .5) ? pos : neg).add(index);
                picked = true;
            }
        }
            
        int[] positive = new int[pos.size()];
        int[] negative = new int[neg.size()];

        Iterator<Integer> it = pos.iterator();
        for (int i = 0; i < positive.length; ++i) 
            positive[i] = it.next();

        it = neg.iterator();
        for (int i = 0; i < negative.length; ++i) 
            negative[i] = it.next();                

        // sort so we can use a binary search in getValue()
        Arrays.sort(positive);
        Arrays.sort(negative);
        return new IndexVector(indexVectorLength, positive, negative);
    }

    /**
     * {@inheritDoc}
     */
    public Vector getIndexVector(String term) {
        if (term == "")
            return null;

        // Check that an index vector does not already exist.
        IndexVector v = termToRandomIndex.get(term);
        if (v == null) {
            synchronized (this) {
                // Confirm that some other thread has not created an index
                // vector for this term.
                v = termToRandomIndex.get(term);
                if (v == null) {
                    // Generate the index vector for this term and store it.
                    v = generateRandomVector();
                    termToRandomIndex.put(term, v);
                }
            }
        }
        return v;
    }
}
