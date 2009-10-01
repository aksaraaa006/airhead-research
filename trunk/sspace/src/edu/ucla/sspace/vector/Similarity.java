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

package edu.ucla.sspace.vector;

import java.lang.reflect.Method;


/**
 * A collection of static methods for computing the similarity between different
 * vectors.  {@link SemanticSpace} implementations should use this class.
 */
public class Similarity {
    
    /**
     * A type of similarity function to use when generating a {@link Method}
     */
    public enum SimType {
        COSINE,
        EUCLIDEAN,
    }

    /**
     * Uninstantiable
     */
    private Similarity() { }

    /**
     * Returns the {@link Method} for getting the similarity of two {@code
     * Vector} based on the specified similarity type.
     *
     * @throws Error if a {@link NoSuchMethodException} is thrown
     */
    public static Method getMethod(SimType similarityType) {
        String methodName = null;

        switch (similarityType) {
        case COSINE:
            methodName = "cosineSimilarity";
            break;
        case EUCLIDEAN:
            methodName = "euclideanSimilarity";
            break;
        }

        Method m = null;

        try { 
            m = Similarity.class.getMethod(methodName,
                    new Class[] {Vector.class, Vector.class});
        } catch (NoSuchMethodException nsme) {
            // rethrow
            throw new Error(nsme);
        }
        return m;
    }

    /**
     * Throws an exception if either array is {@code null} or if the array
     * lengths do not match.
     */
    private static void check(Vector a, Vector b) {
        if (a == null || b == null | a.length() != b.length()) {
            throw new IllegalArgumentException(
                    "input array lengths do not match");
        }
    }

    /**
     * Returns the cosine similarity of the two arrays.
     */
    public static double cosineSimilarity(Vector a, Vector b) {
        check(a,b);

        double dotProduct = 0.0;
        double aMagnitude = 0.0;
        double bMagnitude = 0.0;
        for (int i = 0; i < b.length(); i++) {
            double aValue = a.get(i);
            double bValue = b.get(i);
            aMagnitude += aValue * aValue;
            bMagnitude += bValue * bValue;
            dotProduct += aValue * bValue;
        }
        aMagnitude = Math.sqrt(aMagnitude);
        bMagnitude = Math.sqrt(bMagnitude);
        return (aMagnitude == 0 || bMagnitude == 0)
            ? 0 : dotProduct / (aMagnitude * bMagnitude);
    }
    
    public static double euclideanDistance(Vector a, Vector b) {
        check(a, b);
        
        double sum = 0;
        for (int i = 0; i < a.length(); ++i) {
            sum += Math.pow((a.get(i) - b.get(i)), 2) ;
        }
        return Math.sqrt(sum);
    }

    public static double euclideanSimilarity(Vector a, Vector b) {
        return 1 / (1 + euclideanDistance(a,b));
    }
}
