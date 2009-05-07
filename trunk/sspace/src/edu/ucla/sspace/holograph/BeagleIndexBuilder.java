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

package edu.ucla.sspace.holograph;

import edu.ucla.sspace.common.IndexBuilder;

import jnt.FFT.RealDoubleFFT_Radix2;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Queue;

import java.util.concurrent.ConcurrentHashMap;

public class BeagleIndexBuilder implements IndexBuilder {
  private static final int DEFAULT_INDEX_VECTOR_SIZE = 512;

  private ConcurrentHashMap<String, double[]> termToRandomIndex;
  private RealDoubleFFT_Radix2 fft;
  private int indexVectorSize;
  private double[] placeHolder;
  private double stdev;
  private Random randomGenerator;
  private int[] permute1;
  private int[] permute2;
  private double[] newestRandomVector;

  public BeagleIndexBuilder() {
    init(DEFAULT_INDEX_VECTOR_SIZE);
  }

  public BeagleIndexBuilder(int s) {
    init(s);
  }

  private void init(int s) {
    randomGenerator = new Random();
    termToRandomIndex = new ConcurrentHashMap<String, double[]>();
    indexVectorSize = s;
    fft = new RealDoubleFFT_Radix2(indexVectorSize);
    newestRandomVector = generateRandomVector(); 
    // Enter the zero vector for the empty string.
    termToRandomIndex.put("", newVector(0));
    stdev = 1 / Math.sqrt(indexVectorSize);
    permute1 = new int[indexVectorSize];
    permute2 = new int[indexVectorSize];
    randomPermute(permute1);
    placeHolder = generateRandomVector();
    randomPermute(permute2);
  }

  public int expectedSizeOfPrevWords() {
    return 1;
  }

  public int expectedSizeOfNextWords() {
    return 5;
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

  private double[] getBeagleVector(String term) {
    double[] v = termToRandomIndex.get(term);
    if (v == null) {
      synchronized (term) {
        v = termToRandomIndex.get(term);
        if (v == null) {
          v = generateRandomVector();
          termToRandomIndex.put(term, v);
        }
      }
    }
    return v;
  }

  // Context must have one word before the term being considered, and 4 words
  // after it.  If nothing is available, simply add empty strings.
  // Additionally, they term itself should be replaced with the empty string.
  public void updateMeaningWithTerm(double[] meaning,
                                    Queue<String> prevWords,
                                    Queue<String> nextWords) {
    double[] contextVector = newVector(0); 
    for (String term: prevWords)
      plusEquals(contextVector, getBeagleVector(term));
    for (String term: nextWords)
      plusEquals(contextVector, getBeagleVector(term));
    plusEquals(meaning, contextVector);
    double[] orderVector = newVector(0);
    plusEquals(orderVector, groupConvolution(prevWords, nextWords));
    plusEquals(meaning, orderVector);
  }

  private double[] groupConvolution(Queue<String> prevWords,
                                    Queue<String> nextWords) {
    double[] result = newVector(0);

    // Do the convolutions starting at index 0.
    double[] tempConvolution = convolute(getBeagleVector(prevWords.peek()),
                                         placeHolder);
    plusEquals(result, tempConvolution);

    for (String term : nextWords) {
      tempConvolution = convolute(tempConvolution,
                                  getBeagleVector(term));
      plusEquals(result, tempConvolution);
    }
    tempConvolution = placeHolder;
    // Do the convolutions starting at index 1.
    for (String term : nextWords) {
      tempConvolution = convolute(tempConvolution, getBeagleVector(term));
      plusEquals(result, tempConvolution);
    }
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
