package edu.ucla.sspace.common;

/**
 * A collection of static methods for computing the similarity between different
 * vectors.  Semantic space implementations should use this class.
 */
public class Similarity {
    
    /**
     * Uninstantiable
     */
    private Similarity() { }

    public static double cosineSimilarity(double[] a, double[] b) {
	if (a.length != b.length) {
	    return -1;
	}
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
	return dotProduct / (aMagnitude * bMagnitude);
    }
    
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
	return dotProduct / (aMagnitudeSqRt * bMagnitudeSqRt);
    }

    // REMINDER: this could be made more effecient by not looping
    public static double correlation(double[] arr1, double[] arr2) {
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

    // REMINDER: this could be made more effecient by not looping
    public static double correlation(int[] arr1, int[] arr2) {
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


}
