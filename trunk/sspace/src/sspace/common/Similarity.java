package sspace.common;

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
	long dotProduct = 0.0;
	long aMagnitude = 0.0;
	long bMagnitude = 0.0;
	for (int i = 0; i < b.length ; i++) {
	    int aValue = a[i];
	    int bValue = b[i];
	    aMagnitude += aValue * aValue;
	    bMagnitude += bValue * bValue;
	    dotProduct += aValue * bValue;
	}
	aMagnitude = Math.sqrt(aMagnitude);
	bMagnitude = Math.sqrt(bMagnitude);
	return dotProduct / (aMagnitude * bMagnitude);
    }

    // REMINDER: this could be made more effecient by not looping
    private static double computeCorrelation(double[] arr1, double[] arr2) {
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
    private static double computeCorrelation(int[] arr1, int[] arr2) {
	long xSum = 0;
	long ySum = 0;
	for (int i = 0; i < arr1.length; ++i) {
	    xSum += arr1[i];
	    ySum += arr2[i];
	}
	
	double xMean = xSum / (int)(arr1.length);
	double yMean = ySum / (int)(arr1.length);
	
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

}
