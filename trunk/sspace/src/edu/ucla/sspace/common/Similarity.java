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

import java.lang.reflect.Method;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
	PEARSON_CORRELATION,
	EUCLIDEAN,
        SPEARMAN_RANK_CORRELATION
    }

    /**
     * Uninstantiable
     */
    private Similarity() { }

    /**
     * Returns the {@link Method} for getting the similarity of two {@code
     * double[]} based on the specified similarity type.
     *
     * @throws Error if a {@link NoSuchMethodException} is thrown
     */
    public static Method getMethod(SimType similarityType) {

	String methodName = null;

	switch (similarityType) {
	case COSINE:
	    methodName = "cosineSimilarity";
	    break;
	case PEARSON_CORRELATION:
	    methodName = "correlation";
	    break;
	case EUCLIDEAN:
	    methodName = "euclideanSimilarity";
	    break;
	}
	
 	Method m = null;

	try { 
	    m = Similarity.class.getMethod(methodName,
					   new Class[] {double[].class, double[].class});
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
    private static void check(double[] a, double[] b) {
	if (a.length != b.length) {
	    throw new IllegalArgumentException(
		"input array lengths do not match");
	}
    }

    /**
     * Throws an exception if either array is {@code null} or if the array
     * lengths do not match.
     */
    private static void check(int[] a, int[] b) {
	if (a.length != b.length) {
	    throw new IllegalArgumentException(
		"input array lengths do not match");
	}
    }
    
    /**
     * Returns the cosine similarity of the two arrays.
     */
    public static double cosineSimilarity(double[] a, double[] b) {
	check(a,b);
	double dotProduct = 0.0;
	double aMagnitude = 0.0;
	double bMagnitude = 0.0;
	for (int i = 0; i < b.length ; i++) {
	    double aValue = a[i];
	    double bValue = b[i];
	    aMagnitude += aValue * aValue;
	    bMagnitude += bValue * bValue;
	    dotProduct += aValue * bValue;
	}
	aMagnitude = Math.sqrt(aMagnitude);
	bMagnitude = Math.sqrt(bMagnitude);
	return (aMagnitude == 0 || bMagnitude == 0)
	    ? 0
	    : dotProduct / (aMagnitude * bMagnitude);
    }
    
    /**
     * Returns the cosine similarity of the two arrays.
     */
    public static double cosineSimilarity(int[] a, int[] b) {
	if (a.length != b.length) {
	    return -1;
	}
	long dotProduct = 0;
	long aMagnitude = 0;
	long bMagnitude = 0;
	for (int i = 0; i < b.length ; i++) {
	    int aValue = a[i];
	    int bValue = b[i];
	    aMagnitude += aValue * aValue;
	    bMagnitude += bValue * bValue;
	    dotProduct += aValue * bValue;
	}
	
	double aMagnitudeSqRt = Math.sqrt(aMagnitude);
	double bMagnitudeSqRt = Math.sqrt(bMagnitude);
	return (aMagnitudeSqRt == 0 || bMagnitudeSqRt == 0)
	    ? 0
	    : dotProduct / (aMagnitude * bMagnitude);
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * arrays.
     */
    public static double correlation(double[] arr1, double[] arr2) {
	// REMINDER: this could be made more effecient by not looping
	double xSum = 0;
	double ySum = 0;
	for (int i = 0; i < arr1.length; ++i) {
	    xSum += arr1[i];
	    ySum += arr2[i];
	}
	
	double xMean = xSum / arr1.length;
	double yMean = ySum / arr1.length;
	
	double numerator = 0, xSqSum = 0, ySqSum = 0;
	for (int i = 0; i < arr1.length; ++i) {
	    double x = arr1[i] - xMean;
	    double y = arr2[i] - yMean;
	    numerator += x * y;
	    xSqSum += (x * x);
	    ySqSum += (y * y);
	}
	return numerator / Math.sqrt(xSqSum * ySqSum);
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * arrays.
     */
    public static double correlation(int[] arr1, int[] arr2) {
	// REMINDER: this could be made more effecient by not looping
	long xSum = 0;
	long ySum = 0;
	for (int i = 0; i < arr1.length; ++i) {
	    xSum += arr1[i];
	    ySum += arr2[i];
	}
	
	double xMean = xSum / (double)(arr1.length);
	double yMean = ySum / (double)(arr1.length);
	
	double numerator = 0, xSqSum = 0, ySqSum = 0;
	for (int i = 0; i < arr1.length; ++i) {
	    double x = arr1[i] - xMean;
	    double y = arr2[i] - yMean;
	    numerator += x * y;
	    xSqSum += (x * x);
	    ySqSum += (y * y);
	}
	return numerator / Math.sqrt(xSqSum * ySqSum);
    }

    public static double euclideanDistance(double[] a, double[] b) {
	if (a == null || b == null || a.length != b.length)
	    throw new IllegalArgumentException("a: " + a + "; b: " + b);
	
	double sum = 0;
	for (int i = 0; i < a.length; ++i) {
	    double d = a[i];
	    double e = b[i];
	    sum += (d - e) * (d - e);
	}
	return Math.sqrt(sum);
    }

    public static double euclideanDistance(int[] a, int[] b) {
	if (a == null || b == null || a.length != b.length)
	    throw new IllegalArgumentException("a: " + a + "; b: " + b);
	
	long sum = 0;
	for (int i = 0; i < a.length; ++i) {
	    int d = a[i];
	    int e = b[i];
	    sum += (d - e) * (d - e);
	}
	return Math.sqrt(sum);
    }

    public static double euclideanSimilarity(int[] a, int[] b) {
	return 1 / (1 + euclideanDistance(a,b));
    }

    public static double euclideanSimilarity(double[] a, double[] b) {
	return 1 / (1 + euclideanDistance(a,b));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two arrays.
     * If there is a tie in the ranking of {@code a}, then Pearson's
     * product-moment coefficient is returned instead.
     */
    public static double spearmanRankCorrelationCoefficient(double[] a, 
							    double[] b) {
	check(a,b);
	SortedMap<Double,Double> ranking = new TreeMap<Double,Double>();
	for (int i = 0; i < a.length; ++i) {
	    ranking.put(a[i], b[i]);
	}
	
	// keep track of the last value we saw in the key set so we can check
	// for ties.  If there are ties then the Pearson's product-moment
	// coefficient should be returned instead.
	Double last = null;

	double diff = 0d;

	for (Map.Entry<Double,Double> e : ranking.entrySet()) {
	    Double x = e.getKey();
	    Double y = e.getValue();
	    // check that there are no tied rankings
	    if (last == null) {
		last = x;
	    }
	    // if there was a tie, return the correlation instead.
	    else if (last.equals(x)) {
		return correlation(a,b);
	    }
	    else {
		last = x;
	    }

	    double d = x.doubleValue() - y.doubleValue();
	    diff += d * d;
	}
	return 1 - ((6 * diff) / (a.length * (a.length * a.length - 1)));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two arrays.
     * If there is a tie in the ranking of {@code a}, then Pearson's
     * product-moment coefficient is returned instead.
     */
    public static double spearmanRankCorrelationCoefficient(int[] a, int[] b) {
	check(a,b);
	SortedMap<Integer,Integer> ranking = new TreeMap<Integer,Integer>();
	for (int i = 0; i < a.length; ++i) {
	    ranking.put(a[i], b[i]);
	}
	
	// keep track of the last value we saw in the key set so we can check
	// for ties.  If there are ties then the Pearson's product-moment
	// coefficient should be returned instead.
	Integer last = null;

	long diff = 0l;

	for (Map.Entry<Integer,Integer> e : ranking.entrySet()) {
	    Integer x = e.getKey();
	    Integer y = e.getValue();
	    // check that there are no tied rankings
	    if (last == null) {
		last = x;
	    }
	    // if there was a tie, return the correlation instead.
	    else if (last.equals(x)) {
		return correlation(a,b);
	    }
	    else {
		last = x;
	    }

	    long d = x.longValue() - y.longValue();
	    diff += d * d;
	}
	return 1 - ((6d * diff) / (a.length * (a.length * a.length - 1d)));
    }

}
