package edu.ucla.sspace.holograph;

import jnt.FFT.RealDoubleFFT_Radix2;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class RandomIndexBuilder {
  private static final int DEFAULT_INDEX_VECTOR_SIZE = 2048;

  private HashMap<String, double[]> termToRandomIndex;
  private RealDoubleFFT_Radix2 fft;
  private int indexVectorSize;
  private double stdev;
  private Random randomGenerator;
  private int[] permute1;
  private int[] permute2;

  public RandomIndexBuilder() {
    init(DEFAULT_INDEX_VECTOR_SIZE);
  }

  public RandomIndexBuilder(int s) {
    init(s);
  }

  private void init(int s) {
    termToRandomIndex = new HashMap<String, double[]>();
    indexVectorSize = s;
    fft = new RealDoubleFFT_Radix2(indexVectorSize);
    // Enter the zero vector for the empty string.
    termToRandomIndex.put("", newVector(0));
    randomGenerator = new Random();
    stdev = 1 / Math.sqrt(indexVectorSize);
    permute1 = new int[indexVectorSize];
    permute2 = new int[indexVectorSize];
    randomPermute(permute1);
    randomPermute(permute2);
  }

  private void randomPermute(int[] permute) {
    for (int i = 0; i < indexVectorSize; i++)
      permute[i] = i;
    for (int i = indexVectorSize - 1; i > 0; i--) {
      int w = (int) Math.floor(Math.random() * (i+1));
      int temp = permute[w];
      permute[w] = permute[i];
      permute[i] = permute[w];
    }
  }

  public void printAll() {
    for (Map.Entry<String, double[]> m : termToRandomIndex.entrySet()) {
      System.out.println(m.getKey());
    }
  }

  public void addTermIfMissing(String term) {
    if (!termToRandomIndex.containsKey(term)) {
      double[] termVector = new double[indexVectorSize];
      for (int i = 0; i < indexVectorSize; i++)
        termVector[i] = randomGenerator.nextGaussian() * stdev;
      termToRandomIndex.put(term, termVector);
    }
  }

  // Context must have one word before the term being considered, and 4 words
  // after it.  If nothing is available, simply add empty strings.
  // Additionally, they term itself should be replaced with the empty string.
  public void updateMeaningWithTerm(double[] meaning,
                                    String[] context) {
    double[] contextVector = newVector(0); 
    for (String term: context)
      plusEquals(contextVector, termToRandomIndex.get(term));
    plusEquals(meaning, contextVector);
    double[] orderVector = newVector(0);
    plusEquals(orderVector, groupConvolution(context, 0));
    plusEquals(orderVector, groupConvolution(context, 1));
    plusEquals(meaning, orderVector);
  }

  public double[] groupConvolution(String[] context, int start) {
    double[] result = newVector(0);
    double[] tempConvolution = convolute(termToRandomIndex.get(context[start]),
                                termToRandomIndex.get(context[start+1]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[start+2]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[start+3]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[start+4]));
    plusEquals(result, tempConvolution);
    return result;
  }

  private void plusEquals(double[] left, double[] right) {
    for (int i = 0; i < indexVectorSize; ++i)
      left[i] += right[i];
  }

  private double[] convolute(double[] left, double[] right) {
    left = changeVector(left,permute1);
    right = changeVector(right,permute2);
    fft.transform(left, 0, 1);
    fft.transform(right, 0, 1);
    double[] result = arrayTimes(left, right);

    fft.backtransform(result, 0, 1);
    return result;
  }

  private double[] arrayTimes(double[] left, double[] right) {
    double[] result = newVector(0);
    for (int i = 0; i < indexVectorSize; ++i)
      result[i] = left[i] * right[i];
    return result;
  }

  private double[] changeVector(double[] data, int[] orderVector) {
    double[] result = new double[indexVectorSize];
    for (int i = 0; i < indexVectorSize; i++)
      result[i] = data[orderVector[i]];
    return result;
  }

  private double[] newVector(double v) {
    double[] r = new double[indexVectorSize];
    for (int i = 0; i < indexVectorSize; ++i)
      r[i] = v;
    return r;
  }
}
