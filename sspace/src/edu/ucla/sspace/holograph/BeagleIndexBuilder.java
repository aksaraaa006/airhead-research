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

import edu.ucla.sspace.vector.DenseSemanticVector;
import edu.ucla.sspace.vector.SemanticVector;

import jnt.FFT.RealDoubleFFT_Radix2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Queue;

import java.util.concurrent.ConcurrentHashMap;

public class BeagleIndexBuilder implements IndexBuilder {
  private static final int DEFAULT_INDEX_VECTOR_SIZE = 512;

  private ConcurrentHashMap<String, SemanticVector> termToRandomIndex;
  private RealDoubleFFT_Radix2 fft;
  private int indexVectorSize;
  private SemanticVector placeHolder;
  private double stdev;
  private Random randomGenerator;
  private int[] permute1;
  private int[] permute2;
  private SemanticVector newestRandomVector;

  public BeagleIndexBuilder() {
    init(DEFAULT_INDEX_VECTOR_SIZE);
  }

  public BeagleIndexBuilder(int s) {
    init(s);
  }

  private void init(int s) {
    randomGenerator = new Random();
    termToRandomIndex = new ConcurrentHashMap<String, SemanticVector>();
    indexVectorSize = s;
    fft = new RealDoubleFFT_Radix2(indexVectorSize);
    newestRandomVector = generateRandomVector(); 
    // Enter the zero vector for the empty string.
    termToRandomIndex.put("", getSemanticVector());
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

  public void loadIndexVectors(File file) {
    try {
      DataInputStream in = new DataInputStream(new FileInputStream(file));

      // Read in the permutation vectors.
      for (int i = 0; i < indexVectorSize; ++i)
        permute1[i] = in.readInt();
      for (int i = 0; i < indexVectorSize; ++i)
        permute2[i] = in.readInt();

      // Read in the mappings.  Each mapping starts off with the number of
      // letters in the word, the word, and then the index vector.
      int mappings = in.readInt();
      for (int i = 0; i < mappings; ++i) {
        int wordSize = in.readInt();
        byte[] word = new byte[wordSize];
        in.read(word);
        double[] vector = new double[indexVectorSize];
        for (int j = 0; j < indexVectorSize; ++j) {
          vector[j] = in.readDouble();
        }
        termToRandomIndex.put(new String(word), new DenseSemanticVector(vector));
      }
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  public void saveIndexVectors(File file) {
    try {
      DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
      // Write out the permutation vectors.
      for (int i = 0; i < indexVectorSize; ++i)
        out.writeInt(permute1[i]);
      for (int i = 0; i < indexVectorSize; ++i)
        out.writeInt(permute2[i]);

      out.writeInt(termToRandomIndex.size());
      // Write out each mapping in the form of:
      // word length, word as bytes, index vector.
      for (Map.Entry<String, SemanticVector> entry : termToRandomIndex.entrySet()) {
        String word = entry.getKey();
        SemanticVector vector = entry.getValue();
        out.writeInt(word.length());
        out.write(word.getBytes(), 0, word.length());
        for (int i = 0; i < indexVectorSize; ++i) {
          out.writeDouble(vector.get(i));
        }
      }
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  public SemanticVector getSemanticVector() {
    return new DenseSemanticVector(indexVectorSize);
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
    for (Map.Entry<String, SemanticVector> m : termToRandomIndex.entrySet()) {
      System.out.println(m.getKey());
    }
  }

  private SemanticVector generateRandomVector() {
    SemanticVector termVector = getSemanticVector();
    for (int i = 0; i < indexVectorSize; i++)
      termVector.set(i, randomGenerator.nextGaussian() * stdev);
    return termVector;
  }

  private SemanticVector getBeagleVector(String term) {
    SemanticVector v = termToRandomIndex.get(term);
    if (v == null) {
      synchronized (term) {
        System.out.println("adding new vector for: " + term);
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
  public void updateMeaningWithTerm(SemanticVector meaning,
                                    Queue<String> prevWords,
                                    Queue<String> nextWords) {
    SemanticVector contextVector = getSemanticVector(); 
    for (String term: prevWords)
      plusEquals(contextVector, getBeagleVector(term));
    for (String term: nextWords)
      plusEquals(contextVector, getBeagleVector(term));
    plusEquals(meaning, contextVector);
    SemanticVector orderVector = getSemanticVector();
    plusEquals(orderVector, groupConvolution(prevWords, nextWords));
    plusEquals(meaning, orderVector);
  }

  private SemanticVector groupConvolution(Queue<String> prevWords,
                                          Queue<String> nextWords) {
    SemanticVector result = getSemanticVector();

    // Do the convolutions starting at index 0.
    SemanticVector tempConvolution =
      convolute(getBeagleVector(prevWords.peek()), placeHolder);

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

  private void plusEquals(SemanticVector left, SemanticVector right) {
    for (int i = 0; i < indexVectorSize; ++i)
      left.add(i, right.get(i));
  }

  private SemanticVector convolute(SemanticVector left, SemanticVector right) {
    left = changeVector(left,permute1);
    right = changeVector(right,permute2);
    fft.transform(left.getVector(), 0, 1);
    fft.transform(right.getVector(), 0, 1);
    SemanticVector result = arrayTimes(left, right);

    fft.backtransform(result.getVector(), 0, 1);
    return result;
  }

  private SemanticVector arrayTimes(SemanticVector left, SemanticVector right) {
    SemanticVector result = getSemanticVector();
    for (int i = 0; i < indexVectorSize; ++i)
      result.set(i, left.get(i) * right.get(i));
    return result;
  }

  private SemanticVector changeVector(SemanticVector data, int[] orderVector) {
    SemanticVector result = getSemanticVector();
    for (int i = 0; i < indexVectorSize; i++)
      result.set(i, data.get(orderVector[i]));
    return result;
  }
}
