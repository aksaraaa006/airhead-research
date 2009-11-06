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
        JACCARD_INDEX
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
     * Returns the cosine similarity of the two {@code Vector}.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
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
    public static double correlation(Vector arr1, Vector arr2) {
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

    public static double euclideanDistance(double[] a, double[] b) {
        check(a, b);

        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("a: " + a + "; b: " + b);
        
        double sum = 0;
        for (int i = 0; i < a.length; ++i)
            sum += Math.pow((a[i] - b[i]), 2);
        return Math.sqrt(sum);
    }

    public static double euclideanDistance(int[] a, int[] b) {
        check(a, b);
        
        long sum = 0;
        for (int i = 0; i < a.length; ++i)
            sum += Math.pow(a[i] - b[i], 2);
        return Math.sqrt(sum);
    }

    public static double euclideanDistance(Vector a, Vector b) {
        check(a, b);
        
        double sum = 0;
        for (int i = 0; i < a.length(); ++i)
            sum += Math.pow((a.get(i) - b.get(i)), 2);
        return Math.sqrt(sum);
    }

    public static double euclideanSimilarity(int[] a, int[] b) {
        return 1 / (1 + euclideanDistance(a,b));
    }

    public static double euclideanSimilarity(double[] a, double[] b) {
        return 1 / (1 + euclideanDistance(a,b));
    }

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
     * Vector}s when viewed as sets of samples.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double jaccardIndex(Vector a, Vector b) {
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
     * Vector}s.  If there is a tie in the ranking of {@code a}, then Pearson's
     * product-moment coefficient is returned instead.
     *
     * @throws IllegaleArgumentException when the length of the two vectors are
     *                                   not the same.
     */
    public static double spearmanRankCorrelationCoefficient(Vector a, 
                                                            Vector b) {
        check(a, b);

        SortedMap<Double,Double> ranking = new TreeMap<Double,Double>();
        for (int i = 0; i < a.length(); ++i) {
            ranking.put(a.get(i), b.get(i));
        }
        
        double[] sortedB = b.toArray(b.length());
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
}
