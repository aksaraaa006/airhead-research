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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.util.TreeMultiMap;
import edu.ucla.sspace.util.MultiMap;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Collections;


/**
 * A collection of static methods for computing the similarity between different
 * vectors.  {@link SemanticSpace} implementations should use this class.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class Similarity {
    
    /**
     * A type of similarity function to use when generating a {@link Method}
     */
    public enum SimType {
        COSINE,
        PEARSON_CORRELATION,
        EUCLIDEAN,
        SPEARMAN_RANK_CORRELATION,
        JACCARD_INDEX,
        LIN,
        KL_DIVERGENCE,
        AVERAGE_COMMON_FEATURE_RANK,
        WEIGHTED_FEATURE_RATIO
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
    @Deprecated
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
        case SPEARMAN_RANK_CORRELATION:
            methodName = "spearmanRankCorrelationCoefficient";
            break;
        case JACCARD_INDEX:
            methodName = "jaccardIndex";
            break;
        case AVERAGE_COMMON_FEATURE_RANK:
            methodName = "averageCommonFeatureRank";
            break;
        case LIN:
            methodName = "linSimilarity";
            break;
        case KL_DIVERGENCE:
            methodName = "klDivergence";
            break;
        case WEIGHTED_FEATURE_RATIO:
            methodName = "weightedFeatureRatio";
            break;
        default:
            assert false : similarityType;
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
     * Calculates the similarity of the two vectors using the provided
     * similarity measure.
     *
     * @param similarityType the similarity evaluation to use when comparing
     *        {@code a} and {@code b}
     * @param a a vector
     * @param b a vector
     *
     * @return the similarity according to the specified measure
     */
    public static double getSimilarity(SimType similarityType, 
                                       double[] a, double[] b) {
        switch (similarityType) {
            case COSINE:
                return cosineSimilarity(a, b);
            case PEARSON_CORRELATION:
                return correlation(a, b);
            case EUCLIDEAN:
                return euclideanSimilarity(a, b);
            case SPEARMAN_RANK_CORRELATION:
                return spearmanRankCorrelationCoefficient(a, b);
            case JACCARD_INDEX:
                return jaccardIndex(a, b);
            case AVERAGE_COMMON_FEATURE_RANK:
                return averageCommonFeatureRank(a, b);
            case LIN:
                return linSimilarity(a, b);
            case KL_DIVERGENCE:
                return klDivergence(a, b);
            case WEIGHTED_FEATURE_RATIO:
                return weightedFeatureRatio(a, b);
        }
        return 0;
    }

    /**
     * Calculates the similarity of the two vectors using the provided
     * similarity measure.
     *
     * @param similarityType the similarity evaluation to use when comparing
     *        {@code a} and {@code b}
     * @param a a {@code Vector}
     * @param b a {@code Vector}
     *
     * @return the similarity according to the specified measure
     */
    public static double getSimilarity(SimType similarityType, 
                                       Vector a, Vector b) {
        switch (similarityType) {
            case COSINE:
                return cosineSimilarity(a, b);
            case PEARSON_CORRELATION:
                return correlation(a, b);
            case EUCLIDEAN:
                return euclideanSimilarity(a, b);
            case SPEARMAN_RANK_CORRELATION:
                return spearmanRankCorrelationCoefficient(a, b);
            case JACCARD_INDEX:
                return jaccardIndex(a, b);
            case AVERAGE_COMMON_FEATURE_RANK:
                return averageCommonFeatureRank(a, b);
            case LIN:
                return linSimilarity(a, b);
            case KL_DIVERGENCE:
                return klDivergence(a, b);
            case WEIGHTED_FEATURE_RATIO:
                return weightedFeatureRatio(a, b);
        }
        return 0;
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
     * Throws an exception if either {@code Vector} is {@code null} or if the
     * {@code Vector} lengths do not match.
     */
    private static void check(Vector a, Vector b) {
        if (a.length() != b.length())
            throw new IllegalArgumentException(
                    "input vector lengths do not match");
    }

    /**
     * Returns the cosine similarity of the two arrays.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
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
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double cosineSimilarity(int[] a, int[] b) {
        check(a, b);

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
     * Returns the cosine similarity of the two {@code DoubleVector}.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double cosineSimilarity(DoubleVector a, DoubleVector b) {
        check(a,b);

        double dotProduct = 0.0;
        double aMagnitude = 0.0;
        double bMagnitude = 0.0;

        // Check whether both vectors are sparse.  If so, use only the non-zero
        // indices to speed up the computation by avoiding zero multiplications
        if (a instanceof SparseVector && b instanceof SparseVector) {
            SparseVector svA = (SparseVector)a;
            SparseVector svB = (SparseVector)b;
            int[] nzA = svA.getNonZeroIndices();
            int[] nzB = svB.getNonZeroIndices();
            // Choose the smaller of the two to use in computing the dot
            // product.  Because it would be more expensive to compute the
            // intersection of the two sets, we assume that any potential
            // misses would be less of a performance hit.
            if (nzA.length < nzB.length) {
                // Compute A's maginitude and the dot product
                for (int nz : nzA) {
                    double aValue = a.get(nz);
                    double bValue = b.get(nz);
                    aMagnitude += aValue * aValue;
                    dotProduct += aValue * bValue;
                }
                // Then compute B's magnitude
                for (int nz : nzB) {
                    double bValue = b.get(nz);
                    bMagnitude += bValue * bValue;                                
                }
            }
            else {
                // Compute B's maginitude and the dot product
                for (int nz : nzB) {
                    double aValue = a.get(nz);
                    double bValue = b.get(nz);
                    bMagnitude += bValue * bValue;
                    dotProduct += aValue * bValue;
                }
                // Then compute A's magnitude
                for (int nz : nzA) {
                    double aValue = a.get(nz);
                    aMagnitude += aValue * aValue;                                
                }
            }
        }
        // Otherwise, just assume both are dense and compute the full amount
        else {
            for (int i = 0; i < b.length(); i++) {
                double aValue = a.get(i);
                double bValue = b.get(i);
                aMagnitude += aValue * aValue;
                bMagnitude += bValue * bValue;
                dotProduct += aValue * bValue;
            }
        }
        aMagnitude = Math.sqrt(aMagnitude);
        bMagnitude = Math.sqrt(bMagnitude);
        return (aMagnitude == 0 || bMagnitude == 0)
            ? 0 : dotProduct / (aMagnitude * bMagnitude);
    }

    /**
     * Returns the cosine similarity of the two {@code DoubleVector}.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double cosineSimilarity(IntegerVector a, IntegerVector b) {
        check(a,b);

        double dotProduct = 0.0;
        double aMagnitude = 0.0;
        double bMagnitude = 0.0;

        // Check whether both vectors are sparse.  If so, use only the non-zero
        // indices to speed up the computation by avoiding zero multiplications
        if (a instanceof SparseVector && b instanceof SparseVector) {
            SparseVector svA = (SparseVector)a;
            SparseVector svB = (SparseVector)b;
            int[] nzA = svA.getNonZeroIndices();
            int[] nzB = svB.getNonZeroIndices();
            // Choose the smaller of the two to use in computing the dot
            // product.  Because it would be more expensive to compute the
            // intersection of the two sets, we assume that any potential
            // misses would be less of a performance hit.
            if (nzA.length < nzB.length) {
                // Compute A's maginitude and the dot product
                for (int nz : nzA) {
                    double aValue = a.get(nz);
                    double bValue = b.get(nz);
                    aMagnitude += aValue * aValue;
                    dotProduct += aValue * bValue;
                }
                // Then compute B's magnitude
                for (int nz : nzB) {
                    double bValue = b.get(nz);
                    bMagnitude += bValue * bValue;                                
                }
            }
            else {
                // Compute B's maginitude and the dot product
                for (int nz : nzB) {
                    double aValue = a.get(nz);
                    double bValue = b.get(nz);
                    bMagnitude += bValue * bValue;
                    dotProduct += aValue * bValue;
                }
                // Then compute A's magnitude
                for (int nz : nzA) {
                    double aValue = a.get(nz);
                    aMagnitude += aValue * aValue;                                
                }
            }
        }

        // Otherwise, just assume both are dense and compute the full amount
        else {
            for (int i = 0; i < b.length(); i++) {
                double aValue = a.get(i);
                double bValue = b.get(i);
                aMagnitude += aValue * aValue;
                bMagnitude += bValue * bValue;
                dotProduct += aValue * bValue;
            }
        }
        aMagnitude = Math.sqrt(aMagnitude);
        bMagnitude = Math.sqrt(bMagnitude);
        return (aMagnitude == 0 || bMagnitude == 0)
            ? 0 : dotProduct / (aMagnitude * bMagnitude);
    }

    /**
     * Returns the cosine similarity of the two {@code DoubleVector}.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double cosineSimilarity(Vector a, Vector b) {
        return cosineSimilarity(Vectors.asDouble(a), Vectors.asDouble(b));
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * arrays.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double correlation(double[] arr1, double[] arr2) {
        check(arr1, arr2);

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
        if(numerator == 0)
            return 0;
        return numerator / Math.sqrt(xSqSum * ySqSum);
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * arrays.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double correlation(int[] arr1, int[] arr2) {
        check(arr1, arr2);

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
        if(numerator == 0)
            return 0;
        return numerator / Math.sqrt(xSqSum * ySqSum);
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * {@code Vector}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double correlation(DoubleVector arr1, DoubleVector arr2) {
        check(arr1, arr2);

        check(arr1, arr2);

        // REMINDER: this could be made more effecient by not looping
        double xSum = 0;
        double ySum = 0;
        for (int i = 0; i < arr1.length(); ++i) {
            xSum += arr1.get(i);
            ySum += arr2.get(i);
        }
        
        double xMean = xSum / arr1.length();
        double yMean = ySum / arr1.length();
    
        double numerator = 0, xSqSum = 0, ySqSum = 0;
        for (int i = 0; i < arr1.length(); ++i) {
            double x = arr1.get(i) - xMean;
            double y = arr2.get(i) - yMean;
            numerator += x * y;
            xSqSum += (x * x);
            ySqSum += (y * y);
        }
        if(numerator == 0)
            return 0;
        return numerator / Math.sqrt(xSqSum * ySqSum);
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * {@code Vector}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double correlation(IntegerVector arr1, DoubleVector arr2) {
        check(arr1, arr2);

        // REMINDER: this could be made more effecient by not looping
        double xSum = 0;
        double ySum = 0;
        for (int i = 0; i < arr1.length(); ++i) {
            xSum += arr1.get(i);
            ySum += arr2.get(i);
        }
        
        double xMean = xSum / arr1.length();
        double yMean = ySum / arr1.length();
    
        double numerator = 0, xSqSum = 0, ySqSum = 0;
        for (int i = 0; i < arr1.length(); ++i) {
            double x = arr1.get(i) - xMean;
            double y = arr2.get(i) - yMean;
            numerator += x * y;
            xSqSum += (x * x);
            ySqSum += (y * y);
        }
        if(numerator == 0)
            return 0;
        return numerator / Math.sqrt(xSqSum * ySqSum);
    }

    /**
     * Returns the Pearson product-moment correlation coefficient of the two
     * {@code Vector}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double correlation(Vector a, Vector b) {
        return correlation(Vectors.asDouble(a), Vectors.asDouble(b));
    }

    /**
     * Returns the euclidian distance between two arrays of {code double}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanDistance(double[] a, double[] b) {
        check(a, b);

        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("a: " + a + "; b: " + b);
        
        double sum = 0;
        for (int i = 0; i < a.length; ++i)
            sum += Math.pow((a[i] - b[i]), 2);
        return Math.sqrt(sum);
    }

    /**
     * Returns the euclidian distance between two arrays of {code double}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanDistance(int[] a, int[] b) {
        check(a, b);
        
        long sum = 0;
        for (int i = 0; i < a.length; ++i)
            sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }

    /**
     * Returns the euclidian distance between two {@code DoubleVector}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanDistance(DoubleVector a, DoubleVector b) {
        check(a, b);
        
        double sum = 0;
        for (int i = 0; i < a.length(); ++i)
            sum += Math.pow((a.get(i) - b.get(i)), 2);
        return Math.sqrt(sum);
    }

    /**
     * Returns the euclidian distance between two {@code DoubleVector}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanDistance(IntegerVector a, IntegerVector b) {
        check(a, b);
        
        double sum = 0;
        for (int i = 0; i < a.length(); ++i)
            sum += Math.pow((a.get(i) - b.get(i)), 2);
        return Math.sqrt(sum);
    }

    /**
     * Returns the euclidian distance between two {@code Vector}s.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanDistance(Vector a, Vector b) {
        return euclideanDistance(Vectors.asDouble(a), Vectors.asDouble(b));
    }

    /**
     * Returns the euclidian similiarty between two arrays of values.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanSimilarity(int[] a, int[] b) {
        return 1 / (1 + euclideanDistance(a,b));
    }

    /**
     * Returns the euclidian similiarty between two arrays of values.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanSimilarity(double[] a, double[] b) {
        return 1 / (1 + euclideanDistance(a,b));
    }

    /**
     * Returns the euclidian similiarty between two arrays of values.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double euclideanSimilarity(Vector a, Vector b) {
        return 1 / (1 + euclideanDistance(a,b));
    }

    /**
     * Computes the Jaccard index comparing the similarity both arrays when
     * viewed as sets of samples.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double jaccardIndex(double[] a, double[] b) {
        check(a, b);
        
        Set<Double> intersection = new HashSet<Double>();
        Set<Double> union = new HashSet<Double>();
        for (double d : a) {
            intersection.add(d);
            union.add(d);
        }
        Set<Double> tmp = new HashSet<Double>();
        for (double d : b) {
            tmp.add(d);
            union.add(d);
        }

        intersection.retainAll(tmp);
        return ((double)(intersection.size())) / union.size();
    }

    /**
     * Computes the Jaccard index comparing the similarity both arrays when
     * viewed as sets of samples.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double jaccardIndex(int[] a, int[] b) {
        check(a, b);

        // The BitSets should be faster than a HashMap since it's back by an
        // array and operations are just logical bit operations and require no
        // auto-boxing.  However, if a or b contains large values, then the cost
        // of creating the necessary size for the BitSet may outweigh its
        // performance.  At some point, it would be useful to profile the two
        // methods and their associated worst cases. -jurgens
        BitSet c = new BitSet();
        BitSet d = new BitSet();
        BitSet union = new BitSet();
        for (int i : a) {
            c.set(i);
            union.set(i);
        }
        for (int i : b) {
            d.set(i);
            union.set(i);
        }
        
        // get the intersection
        c.and(d); 
        return ((double)(c.cardinality())) / union.cardinality();
    }

    /**
     * Computes the Jaccard index comparing the similarity both {@code
     * DoubleVector}s when viewed as sets of samples.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double jaccardIndex(DoubleVector a, DoubleVector b) {
        check(a, b);
        
        Set<Double> intersection = new HashSet<Double>();
        Set<Double> union = new HashSet<Double>();
        for (int i = 0; i < a.length(); ++i) {
            double d = a.get(i);
            intersection.add(d);
            union.add(d);
        }
        Set<Double> tmp = new HashSet<Double>();
        for (int i = 0; i < b.length(); ++i) {
            double d = b.get(i);
            tmp.add(d);
            union.add(d);
        }

        intersection.retainAll(tmp);
        return ((double)(intersection.size())) / union.size();
    }

    /**
     * Computes the Jaccard index comparing the similarity both {@code
     * DoubleVector}s when viewed as sets of samples.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double jaccardIndex(IntegerVector a, IntegerVector b) {
        check(a, b);
        
        Set<Integer> intersection = new HashSet<Integer>();
        Set<Integer> union = new HashSet<Integer>();
        for (int i = 0; i < a.length(); ++i) {
            int d = a.get(i);
            intersection.add(d);
            union.add(d);
        }
        Set<Integer> tmp = new HashSet<Integer>();
        for (int i = 0; i < b.length(); ++i) {
            int d = b.get(i);
            tmp.add(d);
            union.add(d);
        }

        intersection.retainAll(tmp);
        return ((double)(intersection.size())) / union.size();
    }

    /**
     * Computes the Jaccard index comparing the similarity both {@code
     * Vector}s when viewed as sets of samples.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double jaccardIndex(Vector a, Vector b) {
        return jaccardIndex(Vectors.asDouble(a), Vectors.asDouble(b));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two arrays.
     * If there is a tie in the ranking of {@code a}, then Pearson's
     * product-moment coefficient is returned instead.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double spearmanRankCorrelationCoefficient(double[] a, 
                                                            double[] b) {
        check(a, b);
        SortedMap<Double,Double> ranking = new TreeMap<Double,Double>();
        for (int i = 0; i < a.length; ++i) {
            if (null != ranking.put(a[i], b[i])) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        double[] sortedB = Arrays.copyOf(b, b.length);
        Arrays.sort(sortedB);
        Map<Double,Integer> otherRanking = new HashMap<Double,Integer>();
        for (int i = 0; i < b.length; ++i) {
            if (null != otherRanking.put(sortedB[i], i)) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Double,Double> e : ranking.entrySet()) {
            Double x = e.getKey();
            Double y = e.getValue();

            // determine the difference in the ranks for both values
            int rankDiff = curRank - otherRanking.get(y).intValue();
            diff += rankDiff * rankDiff;

            curRank++;
        }

        return 1 - ((6 * diff) / (a.length * (a.length * a.length - 1)));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two arrays.
     * If there is a tie in the ranking of {@code a}, then Pearson's
     * product-moment coefficient is returned instead.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double spearmanRankCorrelationCoefficient(int[] a, int[] b) {
        check(a,b);

        SortedMap<Integer,Integer> ranking = new TreeMap<Integer,Integer>();
        for (int i = 0; i < a.length; ++i) {
            if (null != ranking.put(a[i], b[i])) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        int[] sortedB = Arrays.copyOf(b, b.length);
        Arrays.sort(sortedB);
        Map<Integer,Integer> otherRanking = new HashMap<Integer,Integer>();
        for (int i = 0; i < b.length; ++i) {
            if (null != otherRanking.put(sortedB[i], i)) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Integer,Integer> e : ranking.entrySet()) {
            Integer x = e.getKey();
            Integer y = e.getValue();

            // determine the difference in the ranks for both values
            int rankDiff = curRank - otherRanking.get(y).intValue();
            diff += rankDiff * rankDiff;

            curRank++;
        }

        return 1 - ((6d * diff) / (a.length * (a.length * a.length - 1d)));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two {@code
     * DoubleVector}s.  If there is a tie in the ranking of {@code a}, then
     * Pearson's product-moment coefficient is returned instead.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double spearmanRankCorrelationCoefficient(DoubleVector a, 
                                                            DoubleVector b) {
        check(a, b);

        SortedMap<Double,Double> ranking = new TreeMap<Double,Double>();
        for (int i = 0; i < a.length(); ++i) {
            if (null != ranking.put(a.get(i), b.get(i))) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        double[] sortedB = b.toArray();
        Arrays.sort(sortedB);
        Map<Double,Integer> otherRanking = new HashMap<Double,Integer>();
        for (int i = 0; i < b.length(); ++i) {
            if (null != otherRanking.put(sortedB[i], i)) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Double,Double> e : ranking.entrySet()) {
            Double x = e.getKey();
            Double y = e.getValue();

            // determine the difference in the ranks for both values
            int rankDiff = curRank - otherRanking.get(y).intValue();
            diff += rankDiff * rankDiff;

            curRank++;
        }

        return 1 - ((6 * diff) / (a.length() * (a.length() * a.length() - 1)));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two {@code
     * DoubleVector}s.  If there is a tie in the ranking of {@code a}, then
     * Pearson's product-moment coefficient is returned instead.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double spearmanRankCorrelationCoefficient(IntegerVector a, 
                                                            IntegerVector b) {
        check(a, b);

        SortedMap<Integer,Integer> ranking = new TreeMap<Integer,Integer>();
        for (int i = 0; i < a.length(); ++i) {
            if (null != ranking.put(a.get(i), b.get(i))) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        int[] sortedB = b.toArray();
        Arrays.sort(sortedB);
        Map<Integer,Integer> otherRanking = new HashMap<Integer,Integer>();
        for (int i = 0; i < b.length(); ++i) {
            if (null != otherRanking.put(sortedB[i], i)) {
                // if there are ties, must compute the more expensive way
                return rankedCorrelation(a, b);
            }
        }
        
        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Integer,Integer> e : ranking.entrySet()) {
            Integer x = e.getKey();
            Integer y = e.getValue();

            // determine the difference in the ranks for both values
            int rankDiff = curRank - otherRanking.get(y).intValue();
            diff += rankDiff * rankDiff;

            curRank++;
        }

        return 1 - ((6 * diff) / (a.length() * (a.length() * a.length() - 1)));
    }

    /**
     * Computes the Spearman rank correlation coefficient for the two {@code
     * Vector}s.  If there is a tie in the ranking of {@code a}, then Pearson's
     * product-moment coefficient is returned instead.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double spearmanRankCorrelationCoefficient(Vector a, 
                                                            Vector b) {
        return spearmanRankCorrelationCoefficient(Vectors.asDouble(a),
                                                  Vectors.asDouble(b));
    }

    /**
     * Computes the Average Common Feature Rank between the two feature arrays.
     * Uses the top 20 features for comparison.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double averageCommonFeatureRank(double[] a, 
                                                  double[] b) {

        class Pair{
            public int i;
            public double v;
            public Pair(int index, double value){i=index;v=value;}
            public void set(int index, double value){
                i=index;
                v=value;
            }
        }
        class PairCompare implements Comparator<Pair>{
            // note that 1 and -1 have been switched so that sort will sort in
            // descending order
            // this method sorts by value first, then by index
            public int compare(Pair o1, Pair o2){
                if(o1.v < o2.v) return 1;
                else if(o1.v > o2.v) return -1;
                else{
                    if(o1.i < o2.i) return 1;
                    else if(o1.i > o2.i) return -1;
                    return 0;
                }
            }

            public boolean equals(Pair o1, Pair o2){
                return (compare(o1,o2)==0)?true:false;
            }
        }


        check(a, b);
        int size=a.length;

        // number of features to compare
        // calculate how much 10% is, rounded up.
        //int n = (int)Math.ceil(a.length/10.0);
        int n = 20;

        // generate array of index-value pairs for a
        Pair[] a_index = new Pair[size];
        for(int i=0;i<size;i++)
            a_index[i] = new Pair(i,a[i]);
        // generate array of index-value pairs for b
        Pair[] b_index = new Pair[size];
        for(int i=0;i<size;i++)
            b_index[i] = new Pair(i,b[i]);

        // sort the features in a_rank by weight
        Arrays.sort(a_index, new PairCompare());
        // sort the features in b_rank by weight
        Arrays.sort(b_index, new PairCompare());

        // a_index are index-value pairs, ordered by rank
        // this loop changes to a_rank which are rank-value, ordered by index
        // make indices start at 1 so inv(ind) is defined for all indices
        Pair[] a_rank = new Pair[size];
        int last_i = 1;
        for(int i=0;i<size;i++){
            Pair x = a_index[i];
            // share rank if tied
            if(i>0 && a_index[i].v==a_index[i-1].v)
                a_rank[x.i] = new Pair(last_i,x.v);
            else{
                a_rank[x.i] = new Pair(i+1,x.v);
                last_i=i+1;
            }
        }
        // do the same for b_index and b_rank
        last_i=1;
        Pair[] b_rank = new Pair[size];
        for(int i=0;i<size;i++){
            Pair x = b_index[i];
            // share rank if tied
            if(i>0 && b_index[i].v==b_index[i-1].v)
                b_rank[x.i] = new Pair(last_i,x.v);
            else{
                b_rank[x.i] = new Pair(i+1,x.v);
                last_i=i+1;
            }
        }

        // get best ranked n elements
        // nTop will be the top n ranking dimensions by weight
        // where nTop[i] is the ith highest ranking dimension (i.e. feature)
        int[] nTop = new int[n];
        boolean[] seenbefore = new boolean[size];
        Arrays.fill(seenbefore,false);
        int a_i=0;
        int b_i=0;
        for(int i=0;i<n;i++){
            // skip over features already encountered
            while(a_i<size && seenbefore[a_index[a_i].i])
                a_i++;
            while(b_i<size && seenbefore[b_index[b_i].i])
                b_i++;

            // assign rank by highest weight
            //  select the index from A when max(A)>max(B)
            if(a_i<size
                && 1 == (new PairCompare()).compare(a_index[a_i],b_index[b_i])
              ){
                nTop[i] = a_index[a_i].i;
                seenbefore[nTop[i]]=true;
                a_i++;
            }
            else{
                nTop[i] = b_index[b_i].i;
                seenbefore[nTop[i]]=true;
                b_i++;
            }
        }

        // computer the sum of the average rank for each top feature and divide
        //    by the number of top features
        double sum = 0;
        for(int i=0;i<n;i++){
            sum += 0.5*(a_rank[nTop[i]].i+b_rank[nTop[i]].i);
        }
        //return sum/n;
        return n/sum;

    }

    /**
     * Computes the Average Common Feature Rank between the two feature arrays.
     * Uses the top 20 features for comparison. Converts types and calls
     * averageCommonFeatureRank(double[],double[])
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double averageCommonFeatureRank(Vector a, Vector b) {
        return averageCommonFeatureRank(Vectors.asDouble(a).toArray(),
                                        Vectors.asDouble(b).toArray());
    }

    /**
     * Computes the Average Common Feature Rank between the two feature arrays.
     * Uses the top 20 features for comparison. Converts types and calls
     * averageCommonFeatureRank(Vector,Vector)
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double averageCommonFeatureRank(int[] a, 
                                                  int[] b) {
        return averageCommonFeatureRank(Vectors.asVector(a),
                                        Vectors.asVector(b));
    }


    /**
     * Computes the lin similarity measure, which is motivated by information
     * theory priniciples.  This works best if both vectors have already been
     * weighted using point-wise mutual information.  This similarity measure is
     * described in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">D. Lin, "Automatic
     *   Retrieval and Clustering of Similar Words" <i> Proceedings of the 36th
     *   Annual Meeting of the Association for Computational Linguistics and
     *   17th International Conference on Computational Linguistics, Volume 2
     *   </i>, Montreal, Quebec, Canada, 1998.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double linSimilarity(DoubleVector a, DoubleVector b) {
        check(a, b);

        // The total amount of information contained in a.
        double aInformation = 0;
        // The total amount of information contained in b.
        double bInformation = 0;
        // The total amount of information contained in both vectors.
        double combinedInformation = 0;

        // Special case when both vectors are sparse vectors.
        if (a instanceof SparseVector &&
            b instanceof SparseVector) {
            // Convert both vectors to sparse vectors.
            SparseVector sa = (SparseVector) a;
            int[] aNonZeros = sa.getNonZeroIndices();

            SparseVector sb = (SparseVector) b;
            int[] bNonZeros = sb.getNonZeroIndices();

            // If b is the smaller vector swap it with a.  This allows the dot
            // product to work over the vector with the smallest number of non
            // zero values.
            if (bNonZeros.length < aNonZeros.length) {
                SparseVector temp = sa;
                int[] tempNonZeros = aNonZeros;

                sa = sb;
                aNonZeros = bNonZeros;

                sb = temp;
                bNonZeros = tempNonZeros;
            }

            // Compute the combined information by iterating over the vector
            // with the smallest number of non zero values.
            for (int index : aNonZeros) {
                double aValue = a.get(index);
                double bValue = b.get(index);
                aInformation += aValue;
                combinedInformation += aValue + bValue;
            }

            // Compute the information from the other vector by iterating over
            // it's non zero values.
            for (int index : bNonZeros) {
                bInformation += b.get(index);
            }
        }
        else {
            // Compute the information between the two vectors by iterating over
            // all known values.
            for (int i = 0; i < a.length(); ++i) {
                double aValue = a.get(i);
                aInformation += aValue;

                double bValue = b.get(i);
                bInformation += bValue;

                if (aValue != 0d && bValue != 0d)
                    combinedInformation += aValue + bInformation;
            }
        }
        return combinedInformation / (aInformation + bInformation);
    }

    /**
     * Computes the lin similarity measure, which is motivated by information
     * theory priniciples.  This works best if both vectors have already been
     * weighted using point-wise mutual information.  This similarity measure is
     * described in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">D. Lin, "Automatic
     *   Retrieval and Clustering of Similar Words" <i> Proceedings of the 36th
     *   Annual Meeting of the Association for Computational Linguistics and
     *   17th International Conference on Computational Linguistics, Volume 2
     *   </i>, Montreal, Quebec, Canada, 1998.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double linSimilarity(IntegerVector a, IntegerVector b) {
        check(a, b);

        // The total amount of information contained in a.
        double aInformation = 0;
        // The total amount of information contained in b.
        double bInformation = 0;
        // The total amount of information contained in both vectors.
        double combinedInformation = 0;

        // Special case when both vectors are sparse vectors.
        if (a instanceof SparseVector &&
            b instanceof SparseVector) {
            // Convert both vectors to sparse vectors.
            SparseVector sa = (SparseVector) a;
            int[] aNonZeros = sa.getNonZeroIndices();

            SparseVector sb = (SparseVector) b;
            int[] bNonZeros = sb.getNonZeroIndices();

            // If b is the smaller vector swap it with a.  This allows the dot
            // product to work over the vector with the smallest number of non
            // zero values.
            if (bNonZeros.length < aNonZeros.length) {
                SparseVector temp = sa;
                int[] tempNonZeros = aNonZeros;

                sa = sb;
                aNonZeros = bNonZeros;

                sb = temp;
                bNonZeros = tempNonZeros;
            }

            // Compute the combined information by iterating over the vector
            // with the smallest number of non zero values.
            for (int index : aNonZeros) {
                double aValue = a.get(index);
                double bValue = b.get(index);
                aInformation += aValue;
                combinedInformation += aValue + bValue;
            }

            // Compute the information from the other vector by iterating over
            // it's non zero values.
            for (int index : bNonZeros) {
                bInformation += b.get(index);
            }
        }
        else {
            // Compute the information between the two vectors by iterating over
            // all known values.
            for (int i = 0; i < a.length(); ++i) {
                double aValue = a.get(i);
                aInformation += aValue;

                double bValue = b.get(i);
                bInformation += bValue;

                if (aValue != 0d && bValue != 0d)
                    combinedInformation += aValue + bInformation;
            }
        }
        return combinedInformation / (aInformation + bInformation);
    }

    /**
     * Computes the lin similarity measure, which is motivated by information
     * theory priniciples.  This works best if both vectors have already been
     * weighted using point-wise mutual information.  This similarity measure is
     * described in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">D. Lin, "Automatic
     *   Retrieval and Clustering of Similar Words" <i> Proceedings of the 36th
     *   Annual Meeting of the Association for Computational Linguistics and
     *   17th International Conference on Computational Linguistics, Volume 2
     *   </i>, Montreal, Quebec, Canada, 1998.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double linSimilarity(Vector a, Vector b) {
        check(a, b);

        // The total amount of information contained in a.
        double aInformation = 0;
        // The total amount of information contained in b.
        double bInformation = 0;
        // The total amount of information contained in both vectors.
        double combinedInformation = 0;

        // Special case when both vectors are sparse vectors.
        if (a instanceof SparseVector &&
            b instanceof SparseVector) {
            // Convert both vectors to sparse vectors.
            SparseVector sa = (SparseVector) a;
            int[] aNonZeros = sa.getNonZeroIndices();

            SparseVector sb = (SparseVector) b;
            int[] bNonZeros = sb.getNonZeroIndices();

            // If b is the smaller vector swap it with a.  This allows the dot
            // product to work over the vector with the smallest number of non
            // zero values.
            if (bNonZeros.length < aNonZeros.length) {
                SparseVector temp = sa;
                int[] tempNonZeros = aNonZeros;
                sa = sb;
                aNonZeros = bNonZeros;
                sb = temp;
                bNonZeros = tempNonZeros;
            }

            // Compute the combined information by iterating over the vector
            // with the smallest number of non zero values.
            for (int index : aNonZeros) {
                double aValue = a.getValue(index).doubleValue();
                double bValue = b.getValue(index).doubleValue();
                aInformation += aValue;
                combinedInformation += aValue + bValue;
            }

            // Compute the information from the other vector by iterating over
            // it's non zero values.
            for (int index : bNonZeros) {
                bInformation += b.getValue(index).doubleValue();
            }
        }
        else {
            // Compute the information between the two vectors by iterating over
            // all known values.
            for (int i = 0; i < a.length(); ++i) {
                double aValue = a.getValue(i).doubleValue();
                aInformation += aValue;

                double bValue = b.getValue(i).doubleValue();
                bInformation += bValue;

                if (aValue != 0d && bValue != 0d)
                    combinedInformation += aValue + bInformation;
            }
        }
        return combinedInformation / (aInformation + bInformation);
    }

    /**
     * Computes the lin similarity measure, which is motivated by information
     * theory priniciples.  This works best if both vectors have already been
     * weighted using point-wise mutual information.  This similarity measure is
     * described in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">D. Lin, "Automatic
     *   Retrieval and Clustering of Similar Words" <i> Proceedings of the 36th
     *   Annual Meeting of the Association for Computational Linguistics and
     *   17th International Conference on Computational Linguistics, Volume 2
     *   </i>, Montreal, Quebec, Canada, 1998.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double linSimilarity(double[] a, double[] b) {
        check(a, b);

        // The total amount of information contained in a.
        double aInformation = 0;
        // The total amount of information contained in b.
        double bInformation = 0;
        // The total amount of information contained in both vectors.
        double combinedInformation = 0;

        // Compute the information between the two vectors by iterating over
        // all known values.
        for (int i = 0; i < a.length; ++i) {
            aInformation += a[i];
            bInformation += b[i];
            if (a[i] != 0d && b[i] != 0d)
                combinedInformation += a[i] + b[i];
        }
        return combinedInformation / (aInformation + bInformation);
    }

    /**
     * Computes the lin similarity measure, which is motivated by information
     * theory priniciples.  This works best if both vectors have already been
     * weighted using point-wise mutual information.  This similarity measure is
     * described in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">D. Lin, "Automatic
     *   Retrieval and Clustering of Similar Words" <i> Proceedings of the 36th
     *   Annual Meeting of the Association for Computational Linguistics and
     *   17th International Conference on Computational Linguistics, Volume 2
     *   </i>, Montreal, Quebec, Canada, 1998.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double linSimilarity(int[] a, int[] b) {
        check(a, b);

        // The total amount of information contained in a.
        double aInformation = 0;
        // The total amount of information contained in b.
        double bInformation = 0;
        // The total amount of information contained in both vectors.
        double combinedInformation = 0;

        // Compute the information between the two vectors by iterating over
        // all known values.
        for (int i = 0; i < a.length; ++i) {
            aInformation += a[i];
            bInformation += b[i];
            if (a[i] != 0d && b[i] != 0d)
                combinedInformation += a[i] + b[i];
        }
        return combinedInformation / (aInformation + bInformation);
    }

    /**
     * Computes the K-L Divergence of two probability distributions {@code A}
     * and {@code B} where the vectors {@code a} and {@code b} correspond to
     * {@code n} samples from each respective distribution.  The divergence
     * between two samples is non-symmetric and is frequently used as a distance
     * metric between vectors from a semantic space.  This metric is described
     * in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">S. Kullback and R. A.
     *   Leibler, "On Information and Sufficiency", <i>The Annals of
     *   Mathematical Statistics</i> 1951.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double klDivergence(DoubleVector a, DoubleVector b) {
        check(a, b);

        double divergence = 0;

        // Iterate over just the non zero values of a if it is a sparse vector.
        if (a instanceof SparseVector) {
            SparseVector sa = (SparseVector) a;
            int[] aNonZeros = sa.getNonZeroIndices();

            for (int index : aNonZeros) {
                double aValue = a.get(index);
                double bValue = b.get(index);

                // Ignore values from b that are zero, since they would cause a
                // divide by zero error.
                if (bValue != 0d)
                    divergence += aValue * Math.log(aValue / bValue);
            }
        }
        // Otherwise iterate over all values and ignore any that are zero.
        else {
            for (int i = 0; i < a.length(); ++i) {
                double aValue = a.get(i);
                double bValue = b.get(i);

                // Ignore values from b that are zero, since they would cause a
                // divide by zero error.
                if (aValue != 0d && bValue != 0d)
                    divergence += aValue * Math.log(aValue / bValue);
            }
        }

        return divergence;
    }

    /**
     * Computes the K-L Divergence of two probability distributions {@code A}
     * and {@code B} where the vectors {@code a} and {@code b} correspond to
     * {@code n} samples from each respective distribution.  The divergence
     * between two samples is non-symmetric and is frequently used as a distance
     * metric between vectors from a semantic space.  This metric is described
     * in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">S. Kullback and R. A.
     *   Leibler, "On Information and Sufficiency", <i>The Annals of
     *   Mathematical Statistics</i> 1951.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double klDivergence(IntegerVector a, IntegerVector b) {
        check(a, b);

        double divergence = 0;

        // Iterate over just the non zero values of a if it is a sparse vector.
        if (a instanceof SparseVector) {
            SparseVector sa = (SparseVector) a;
            int[] aNonZeros = sa.getNonZeroIndices();

            for (int index : aNonZeros) {
                double aValue = a.get(index);
                double bValue = b.get(index);

                // Ignore values from b that are zero, since they would cause a
                // divide by zero error.
                if (bValue != 0d)
                    divergence += aValue * Math.log(aValue / bValue);
            }
        }
        // Otherwise iterate over all values and ignore any that are zero.
        else {
            for (int i = 0; i < a.length(); ++i) {
                double aValue = a.get(i);
                double bValue = b.get(i);

                // Ignore values from b that are zero, since they would cause a
                // divide by zero error.
                if (aValue != 0d && bValue != 0d)
                    divergence += aValue * Math.log(aValue / bValue);
            }
        }

        return divergence;
    }

    /**
     * Computes the K-L Divergence of two probability distributions {@code A}
     * and {@code B} where the vectors {@code a} and {@code b} correspond to
     * {@code n} samples from each respective distribution.  The divergence
     * between two samples is non-symmetric and is frequently used as a distance
     * metric between vectors from a semantic space.  This metric is described
     * in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">S. Kullback and R. A.
     *   Leibler, "On Information and Sufficiency", <i>The Annals of
     *   Mathematical Statistics</i> 1951.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double klDivergence(Vector a, Vector b) {
        check(a, b);

        double divergence = 0;

        // Iterate over just the non zero values of a if it is a sparse vector.
        if (a instanceof SparseVector) {
            SparseVector sa = (SparseVector) a;
            int[] aNonZeros = sa.getNonZeroIndices();

            for (int index : aNonZeros) {
                double aValue = a.getValue(index).doubleValue();
                double bValue = b.getValue(index).doubleValue();

                // Ignore values from b that are zero, since they would cause a
                // divide by zero error.
                if (bValue != 0d)
                    divergence += aValue * Math.log(aValue / bValue);
            }
        }
        // Otherwise iterate over all values and ignore any that are zero.
        else {
            for (int i = 0; i < a.length(); ++i) {
                double aValue = a.getValue(i).doubleValue();
                double bValue = b.getValue(i).doubleValue();

                // Ignore values from b that are zero, since they would cause a
                // divide by zero error.
                if (aValue != 0d && bValue != 0d)
                    divergence += aValue * Math.log(aValue / bValue);
            }
        }

        return divergence;
    }

    /**
     * Computes the K-L Divergence of two probability distributions {@code A}
     * and {@code B} where the vectors {@code a} and {@code b} correspond to
     * {@code n} samples from each respective distribution.  The divergence
     * between two samples is non-symmetric and is frequently used as a distance
     * metric between vectors from a semantic space.  This metric is described
     * in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">S. Kullback and R. A.
     *   Leibler, "On Information and Sufficiency", <i>The Annals of
     *   Mathematical Statistics</i> 1951.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double klDivergence(double[] a, double[] b) {
        check(a, b);

        double divergence = 0;

        // Iterate over all values and ignore any that are zero.
        for (int i = 0; i < a.length; ++i) {
            // Ignore values from b that are zero, since they would cause a
            // divide by zero error.
            if (a[i] != 0d && b[i] != 0d)
                divergence += a[i] * Math.log(a[i]/ b[i]);
        }

        return divergence;
    }

    /**
     * Computes the K-L Divergence of two probability distributions {@code A}
     * and {@code B} where the vectors {@code a} and {@code b} correspond to
     * {@code n} samples from each respective distribution.  The divergence
     * between two samples is non-symmetric and is frequently used as a distance
     * metric between vectors from a semantic space.  This metric is described
     * in more detail in the following paper:
     *
     *   <li style="font-family:Garamond, Georgia, serif">S. Kullback and R. A.
     *   Leibler, "On Information and Sufficiency", <i>The Annals of
     *   Mathematical Statistics</i> 1951.
     *   </li>
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double klDivergence(int[] a, int[] b) {
        check(a, b);

        double divergence = 0;

        // Iterate over all values and ignore any that are zero.
        for (int i = 0; i < a.length; ++i) {
            // Ignore values from b that are zero, since they would cause a
            // divide by zero error.
            if (a[i] != 0d && b[i] != 0d)
                divergence += a[i] * Math.log(a[i]/ b[i]);
        }

        return divergence;
    }

    /**
     * Computes the ranking of two variables, then finds the Pearson's
     * product-moment coefficient.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double rankedCorrelation(double[] a, double[] b) {
   
        check(a, b);

        MultiMap<Double,Integer> sortingTreeA =
            new TreeMultiMap<Double,Integer>(Collections.reverseOrder());
        for (int i = 0; i < a.length; ++i) {
            sortingTreeA.put(a[i], i);
        }
        double[] ranksA = new double[a.length];
        int index = 1;
        for (Double key : sortingTreeA.keySet()) {
            Set<Integer> ties = sortingTreeA.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksA[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        MultiMap<Double,Integer> sortingTreeB =
            new TreeMultiMap<Double,Integer>(Collections.reverseOrder());
        for (int i = 0; i < b.length; ++i) {
            sortingTreeB.put(b[i], i);
        }
        double[] ranksB = new double[b.length];
        index = 1;
        for (Double key : sortingTreeB.keySet()) {
            Set<Integer> ties = sortingTreeB.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksB[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        for(int i=0;i<ranksA.length;i++){
            System.out.print(ranksA[i]+"\t");
            System.out.print(ranksB[i]+"\n");
        }

        return correlation(ranksA,ranksB);

    }

    /**
     * Computes the ranking of two variables, then finds the Pearson's
     * product-moment coefficient.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double rankedCorrelation(int[] a, int[] b) {
        check(a, b);

        MultiMap<Integer,Integer> sortingTreeA =
            new TreeMultiMap<Integer,Integer>(Collections.reverseOrder());
        for (int i = 0; i < a.length; ++i) {
            sortingTreeA.put(a[i], i);
        }
        double[] ranksA = new double[a.length];
        int index = 1;
        for (Integer key : sortingTreeA.keySet()) {
            Set<Integer> ties = sortingTreeA.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksA[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        MultiMap<Integer,Integer> sortingTreeB =
            new TreeMultiMap<Integer,Integer>(Collections.reverseOrder());
        for (int i = 0; i < b.length; ++i) {
            sortingTreeB.put(b[i], i);
        }
        double[] ranksB = new double[b.length];
        index = 1;
        for (Integer key : sortingTreeB.keySet()) {
            Set<Integer> ties = sortingTreeB.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksB[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        return correlation(ranksA,ranksB);

    }

    /**
     * Computes the ranking of two variables, then finds the Pearson's
     * product-moment coefficient.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double rankedCorrelation(DoubleVector a, DoubleVector b) {
        check(a, b);

        MultiMap<Double,Integer> sortingTreeA =
            new TreeMultiMap<Double,Integer>(Collections.reverseOrder());
        for (int i = 0; i < a.length(); ++i) {
            sortingTreeA.put(a.get(i), i);
        }
        double[] ranksA = new double[a.length()];
        int index = 1;
        for (Double key : sortingTreeA.keySet()) {
            Set<Integer> ties = sortingTreeA.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksA[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        MultiMap<Double,Integer> sortingTreeB =
            new TreeMultiMap<Double,Integer>(Collections.reverseOrder());
        for (int i = 0; i < b.length(); ++i) {
            sortingTreeB.put(b.get(i), i);
        }
        double[] ranksB = new double[b.length()];
        index = 1;
        for (Double key : sortingTreeB.keySet()) {
            Set<Integer> ties = sortingTreeB.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksB[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        return correlation(ranksA,ranksB);

    }

    /**
     * Computes the ranking of two variables, then finds the Pearson's
     * product-moment coefficient.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double rankedCorrelation(IntegerVector a, IntegerVector b) {
        check(a, b);

        MultiMap<Integer,Integer> sortingTreeA =
            new TreeMultiMap<Integer,Integer>(Collections.reverseOrder());
        for (int i = 0; i < a.length(); ++i) {
            sortingTreeA.put(a.get(i), i);
        }
        double[] ranksA = new double[a.length()];
        int index = 1;
        for (Integer key : sortingTreeA.keySet()) {
            Set<Integer> ties = sortingTreeA.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksA[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        MultiMap<Integer,Integer> sortingTreeB =
            new TreeMultiMap<Integer,Integer>(Collections.reverseOrder());
        for (int i = 0; i < b.length(); ++i) {
            sortingTreeB.put(b.get(i), i);
        }
        double[] ranksB = new double[b.length()];
        index = 1;
        for (Integer key : sortingTreeB.keySet()) {
            Set<Integer> ties = sortingTreeB.get(key);
            int size = ties.size();
            // calculate the average rank of tied entries
            // which are consectuive ints in the range [i,i+n-1]
            // using the fact that the sum of 1 through n is (n^2+n)/2
            // and [i,i+n-1] = i-1 + sum([1,n])
            // sum([i,i+n-1) = i-1 + (n^2+n)/2
            // avg([i,i+n-1) = i-1 + (n^2+n)/(2n)
            double rank = (index-1.0d) + size*(size+1)/(2.0d*size);
            for (Integer i : ties) {
                // write the average rank to each of the tied values
                ranksB[i] = rank;
                // skip over the tied ranks
                index++;
            }
        }

        return correlation(ranksA,ranksB);

    }

    public static double weightedFeatureRatio(Vector a, Vector b) {
        check(a, b);

        // special handling for Sparse Vectors
        if (a instanceof SparseVector && b instanceof SparseVector) {
            SparseVector svA = (SparseVector)a;
            SparseVector svB = (SparseVector)b;

            HashSet<Integer> nzCom = new HashSet<Integer>();

            int[] nzA = svA.getNonZeroIndices();
            double max_a = 0;
            for (int i=0; i<nzA.length; i++) {
                double val = Math.abs(a.getValue(nzA[i]).doubleValue());
                if (max_a < val) {
                    max_a = val;
                }
                nzCom.add(nzA[i]);
            }
            int[] nzB = svB.getNonZeroIndices();
            double max_b = 0;
            for (int i=0; i<nzB.length; i++) {
                double val = Math.abs(b.getValue(nzB[i]).doubleValue());
                if (max_b < val) {
                    max_b = val;
                }
                nzCom.add(nzB[i]);
            }
            // prevent divide by zero
            if (max_a==0) max_a=1;
            if (max_b==0) max_b=1;

            // sum weighted ratio
            // sum is over the union of all nonzero elements
            // the intersection would suffice since the ratio is 0 when one of
            // the elements is zero, but this is more flexible if this changes
            double sum = 0;
            for (Integer i : nzCom) {
                double ai = Math.abs(a.getValue(i).doubleValue())/max_a;
                double bi = Math.abs(b.getValue(i).doubleValue())/max_b;
                double ratio = 0;
                if (bi==0 && ai==0) {
                    ratio = 0;
                }
                else if (ai < bi) {
                    ratio = ai/bi;
                }
                else if (bi < ai) {
                    ratio = bi/ai;
                }
                else {
                    ratio = 1;
                }

                double weight = Math.exp((ai+bi)/2 - Math.abs(ai-bi) - 1);

                sum += ratio*weight;
            }
            return sum;
        }

        // calculate the normalization factor
        // using the max scales the vector to fit in the unit hypercube
        double max_a = 0;
        double max_b = 0;
        for (int i=0; i<a.length(); i++) {
            if (max_a < Math.abs(a.getValue(i).doubleValue())) {
                max_a = Math.abs(a.getValue(i).doubleValue());
            }
            if (max_b < Math.abs(b.getValue(i).doubleValue())) {
                max_b = Math.abs(b.getValue(i).doubleValue());
            }
        }
        // prevent divide by zero
        if (max_a==0) max_a=1;
        if (max_b==0) max_b=1;

        // sum weighted ratio
        double sum = 0;
        for (int i=0; i<a.length(); i++) {
            double ai = Math.abs(a.getValue(i).doubleValue())/max_a;
            double bi = Math.abs(b.getValue(i).doubleValue())/max_b;
            double ratio = 0;
            if (bi==0 && ai==0) {
                ratio = 0;
            }
            else if (ai < bi) {
                ratio = ai/bi;
            }
            else if (bi < ai) {
                ratio = bi/ai;
            }
            else {
                ratio = 1;
            }

            double weight = Math.exp((ai+bi)/2 - Math.abs(ai-bi) - 1);

            sum += ratio*weight;
        }
        return sum;
    }

    public static double weightedFeatureRatio(double[] a, double[] b) {
        check(a, b);

        // calculate the normalization factor
        // using the max scales the vector to fit in the unit hypercube
        double max_a = 0;
        double max_b = 0;
        for (int i=0; i<a.length; i++) {
            if (max_a < Math.abs(a[i])) {
                max_a = Math.abs(a[i]);
            }
            if (max_b < Math.abs(b[i])) {
                max_b = Math.abs(b[i]);
            }
        }
        // prevent divide by zero
        if (max_a==0) max_a=1;
        if (max_b==0) max_b=1;

        // sum weighted ratio
        double sum = 0;
        for (int i=0; i<a.length; i++) {
            double ai = Math.abs(a[i])/max_a;
            double bi = Math.abs(b[i])/max_b;
            double ratio = 0;
            if (bi==0 && ai==0) {
                ratio = 0;
            }
            else if (ai < bi) {
                ratio = ai/bi;
            }
            else if (bi < ai) {
                ratio = bi/ai;
            }
            else {
                ratio = 1;
            }

            double weight = Math.exp((ai+bi)/2 - Math.abs(ai-bi) - 1);

            sum += ratio*weight;
        }
        return sum;
    }
}
