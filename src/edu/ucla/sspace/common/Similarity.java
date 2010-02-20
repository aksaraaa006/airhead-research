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

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


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
        case LIN:
            methodName = "linSimilarity";
            break;
        case KL_DIVERGENCE:
            methodName = "klDivergence";
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
            case LIN:
                return linSimilarity(a, b);
            case KL_DIVERGENCE:
                return klDivergence(a, b);
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
            case LIN:
                return linSimilarity(a, b);
            case KL_DIVERGENCE:
                return klDivergence(a, b);
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

        if (numerator == 0d || xSqSum == 0d || ySqSum == 0d)
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
            ranking.put(a[i], b[i]);
        }
        
        double[] sortedB = Arrays.copyOf(b, b.length);
        Arrays.sort(sortedB);
        Map<Double,Integer> otherRanking = new HashMap<Double,Integer>();
        for (int i = 0; i < b.length; ++i) {
            otherRanking.put(sortedB[i], i);
        }
        
        // keep track of the last value we saw in the key set so we can check
        // for ties.  If there are ties then the Pearson's product-moment
        // coefficient should be returned instead.
        Double last = null;

        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Double,Double> e : ranking.entrySet()) {
            Double x = e.getKey();
            Double y = e.getValue();
            // check that there are no tied rankings
            if (last == null)
                last = x;
            else if (last.equals(x))
                // if there was a tie, return the correlation instead.
                return correlation(a,b);
            else 
                last = x;

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
            ranking.put(a[i], b[i]);
        }
        
        int[] sortedB = Arrays.copyOf(b, b.length);
        Arrays.sort(sortedB);
        Map<Integer,Integer> otherRanking = new HashMap<Integer,Integer>();
        for (int i = 0; i < b.length; ++i)
            otherRanking.put(sortedB[i], i);
        
        // keep track of the last value we saw in the key set so we can check
        // for ties.  If there are ties then the Pearson's product-moment
        // coefficient should be returned instead.
        Integer last = null;

        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Integer,Integer> e : ranking.entrySet()) {
            Integer x = e.getKey();
            Integer y = e.getValue();
            // check that there are no tied rankings
            if (last == null)
                last = x;
            else if (last.equals(x))
                // if there was a tie, return the correlation instead.
                return correlation(a,b);
            else
                last = x;

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
            ranking.put(a.get(i), b.get(i));
        }
        
        double[] sortedB = b.toArray();
        Arrays.sort(sortedB);
        Map<Double,Integer> otherRanking = new HashMap<Double,Integer>();
        for (int i = 0; i < b.length(); ++i) {
            otherRanking.put(sortedB[i], i);
        }
        
        // keep track of the last value we saw in the key set so we can check
        // for ties.  If there are ties then the Pearson's product-moment
        // coefficient should be returned instead.
        Double last = null;

        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Double,Double> e : ranking.entrySet()) {
            Double x = e.getKey();
            Double y = e.getValue();
            // check that there are no tied rankings
            if (last == null)
                last = x;
            else if (last.equals(x))
                // if there was a tie, return the correlation instead.
                return correlation(a,b);
            else 
                last = x;

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
            ranking.put(a.get(i), b.get(i));
        }
        
        int[] sortedB = b.toArray();
        Arrays.sort(sortedB);
        Map<Integer,Integer> otherRanking = new HashMap<Integer,Integer>();
        for (int i = 0; i < b.length(); ++i) {
            otherRanking.put(sortedB[i], i);
        }
        
        // keep track of the last value we saw in the key set so we can check
        // for ties.  If there are ties then the Pearson's product-moment
        // coefficient should be returned instead.
        Integer last = null;

        // sum of the differences in rank
        double diff = 0d;

        // the current rank of the element in a that we are looking at
        int curRank = 0;

        for (Map.Entry<Integer,Integer> e : ranking.entrySet()) {
            Integer x = e.getKey();
            Integer y = e.getValue();
            // check that there are no tied rankings
            if (last == null)
                last = x;
            else if (last.equals(x))
                // if there was a tie, return the correlation instead.
                return correlation(a,b);
            else 
                last = x;

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
}
