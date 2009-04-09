package sspace.common;

public class Similarity {
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

  public static double cosineDistance(double[] a, double[] b) {
    return Math.acos(cosineSimilarity(a,b));
  }
}
