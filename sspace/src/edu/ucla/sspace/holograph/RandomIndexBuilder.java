package edu.ucla.sspace.holograph;

import jnt.FFT.RealDoubleFFT_Radix2;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomIndexBuilder {
  private static final int DEFAULT_INDEX_VECTOR_SIZE = 2048;

  private HashMap<String, double[]> termToRandomIndex;
  private RealDoubleFFT_Radix2 fft;
  private int indexVectorSize;
  private double[] placeHolder;
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
    placeHolder = generateRandomVector();
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

  private double[] generateRandomVector() {
    double[] termVector = new double[indexVectorSize];
    for (int i = 0; i < indexVectorSize; i++)
      termVector[i] = randomGenerator.nextGaussian() * stdev;
    return termVector;
  }

  public void addTermIfMissing(String term) {
    if (!termToRandomIndex.containsKey(term))
      termToRandomIndex.put(term, generateRandomVector());
  }

  // Context must have one word before the term being considered, and 4 words
  // after it.  If nothing is available, simply add empty strings.
  // Additionally, they term itself should be replaced with the empty string.
  public void updateMeaningWithTerm(double[] meaning, String[] context) {
    double[] contextVector = newVector(0); 
    for (String term: context)
      plusEquals(contextVector, termToRandomIndex.get(term));
    plusEquals(meaning, contextVector);
    double[] orderVector = newVector(0);
    plusEquals(orderVector, groupConvolution(context));
    plusEquals(meaning, orderVector);
  }

  private double[] groupConvolution(String[] context) {
    double[] result = newVector(0);

    // Do the convolutions starting at index 0.
    double[] tempConvolution = convolute(termToRandomIndex.get(context[0]),
                                         placeHolder);
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[2]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[3]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[4]));
    plusEquals(result, tempConvolution);

    // Do the convolutions starting at index 1.
    tempConvolution = convolute(placeHolder, termToRandomIndex.get(context[2]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[3]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[4]));
    plusEquals(result, tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[5]));
    plusEquals(result, tempConvolution);

    return result;
  }

  public double[] decode(double[] meaning, boolean side) {
    double[] environ;
    if (side)
      environ = changeVector(placeHolder, permute1);
    else
      environ = changeVector(placeHolder, permute2);
    double[] result = circularCorrelation(environ, meaning);
    if (side)
      return demute(result, permute2);
    else 
      return demute(result, permute1);
  }

  private void plusEquals(double[] left, double[] right) {
    for (int i = 0; i < indexVectorSize; ++i)
      left[i] += right[i];
  }

  private double[] circularCorrelation(double[] arr1, double[] arr2) {
    double[] result = new double[arr1.length];
    for (int i = 0; i < arr1.length; ++i) {
      result[i] = 0;
      for (int j = 0; j < arr1.length; ++j) {
        result[i] += arr1[j] * arr2[(i+j) % arr1.length];
      }
    }
    return result;
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

  private double[] demute(double[] data, int[] orderVector) {
    double[] result = new double[indexVectorSize];
    for (int i = 0; i < indexVectorSize; ++i)
      result[orderVector[i]] = data[i];
    return result;
  }

  private double[] newVector(double v) {
    double[] r = new double[indexVectorSize];
    for (int i = 0; i < indexVectorSize; ++i)
      r[i] = v;
    return r;
  }
}
