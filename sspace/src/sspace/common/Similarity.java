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
}
