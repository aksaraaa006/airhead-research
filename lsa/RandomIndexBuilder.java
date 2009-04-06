import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import Jama.Matrix;

class RandomIndexBuilder {
  private static final int DEFAULT_INDEX_VECTOR_SIZE = 2048;

  private HashMap<String, Matrix> termToRandomIndex;
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
    termToRandomIndex = new HashMap<String, Matrix>();
    indexVectorSize = s;
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
    for (Map.Entry<String, Matrix> m : termToRandomIndex.entrySet()) {
      System.out.println(m.getKey());
      m.getValue().print(4, 4);
    }
  }

  public void addTermIfMissing(String term) {
    if (!termToRandomIndex.containsKey(term)) {
      double[][] termVector = new double[indexVectorSize][1];
      for (int i = 0; i < indexVectorSize; i++)
        termVector[i][0] = randomGenerator.nextGaussian() * stdev;
      termToRandomIndex.put(term, new Matrix(termVector));
    }
  }

  // Context must have one word before the term being considered, and 4 words
  // after it.  If nothing is available, simply add empty strings.
  // Additionally, they term itself should be replaced with the empty string.
  public void updateMeaningWithTerm(Matrix meaning,
                                    String[] context) {
    Matrix contextVector = newVector(0); 
    for (String term: context)
      contextVector.plusEquals(termToRandomIndex.get(term));
    meaning.plusEquals(contextVector);
    Matrix orderVector = newVector(0);
    orderVector.plusEquals(groupConvolution(context, 0));
    orderVector.plusEquals(groupConvolution(context, 1));
    meaning.plusEquals(orderVector);
  }

  public Matrix groupConvolution(String[] context, int start) {
    Matrix result = newVector(0);
    Matrix tempConvolution = newVector(0);
    tempConvolution = convolute(termToRandomIndex.get(context[start]),
                                termToRandomIndex.get(context[start+1]));
    result.plusEquals(tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[start+2]));
    result.plusEquals(tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[start+3]));
    result.plusEquals(tempConvolution);
    tempConvolution = convolute(tempConvolution,
                                termToRandomIndex.get(context[start+4]));
    result.plusEquals(tempConvolution);
    return result;
  }

  private Matrix convolute(Matrix left, Matrix right) {
    Matrix result = newVector(0);
    left = changeVector(left,permute1);
    right = changeVector(right,permute2);
    //reorderVector(left, order1);
    //reorderVector(right, order2);
    fft(left);
    fft(right);
    result = left.arrayTimes(right);

    reverseFFT(result);
    return result;
  }

  private Matrix changeVector(Matrix data, int[] orderVector) {
    Matrix result = new Matrix(indexVectorSize, 1, 0);
    for (int i = 0; i < indexVectorSize; i++)
      result.set(i, 0, data.get(orderVector[i], 0));
    return result;
  }

  private void fft(Matrix data) {
    int i0 = 0;
    int stride = 1;
    int n = indexVectorSize;

    int p, p_1, q;

    /* bit reverse the ordering of input data for decimation in time algorithm */
    bitreverse(data, i0, stride);

    /* apply fft recursion */
    p = 1; q = n ;
    int logn = log2(indexVectorSize);
    for (int i = 1; i <= logn; i++) {
      int a, b;

      p_1 = p ;
      p = 2 * p ;
      q = q / 2 ;

      /* a = 0 */

      for (b = 0; b < q; b++) {
        double t0_real = data.get(i0+stride*b*p,0) + data.get(i0+stride*(b*p + p_1),0);
        double t1_real = data.get(i0+stride*b*p,0) - data.get(i0+stride*(b*p + p_1),0);
	  
        data.set(i0+stride*b*p,0,t0_real);
        data.set(i0+stride*(b*p + p_1),0,t1_real);
      }

      /* a = 1 ... p_{i-1}/2 - 1 */

      {
        double w_real = 1.0;
        double w_imag = 0.0;

        double theta = - 2.0 * Math.PI / p;
	
        double s = Math.sin(theta);
        double t = Math.sin(theta / 2.0);
        double s2 = 2.0 * t * t;
	
        for (a = 1; a < (p_1)/2; a++) {
          /* trignometric recurrence for w-> exp(i theta) w */
          {
            double tmp_real = w_real - s * w_imag - s2 * w_real;
            double tmp_imag = w_imag + s * w_real - s2 * w_imag;
            w_real = tmp_real;
            w_imag = tmp_imag;
          }
          
          for (b = 0; b < q; b++) {
            double z0_real = data.get(i0+stride*(b*p + a),0);
            double z0_imag = data.get(i0+stride*(b*p + p_1 - a),0);
            double z1_real = data.get(i0+stride*(b*p + p_1 + a),0);
            double z1_imag = data.get(i0+stride*(b*p + p - a),0);
          
            /* t0 = z0 + w * z1 */
            data.set(i0+stride*(b*p + a),0, z0_real + w_real * z1_real - w_imag * z1_imag);
            data.set(i0+stride*(b*p + p - a),0,z0_imag + w_real * z1_imag + w_imag * z1_real);
            /* t1 = -(z0 - w * z1) */
            data.set(i0+stride*(b*p + p_1 - a),0,z0_real - w_real * z1_real + w_imag * z1_imag);
            data.set(i0+stride*(b*p + p_1 + a),0,-(z0_imag - w_real * z1_imag - w_imag * z1_real));
          }
        }
      }

      if (p_1 >  1) {
        for (b = 0; b < q; b++) {
          /* a = p_{i-1}/2 */
          int xkcd = i0+stride*(b*p + p - p_1/2);
          data.set(xkcd,0, data.get(xkcd,0) * -1);
        }
      }
    }
  }

  private void reverseFFT(Matrix data) {
    int i0 = 0;
    int stride = 1;
    int n = indexVectorSize;
    int p, p_1, q;

    p = n; q = 1 ; p_1 = n/2 ;

    int logn = log2(indexVectorSize);
    for (int i = 1; i <= logn; i++) {
      int a, b;

      /* a = 0 */
      for (b = 0; b < q; b++) {
        double z0 = data.get(i0+stride*b*p,0);
        double z1 = data.get(i0+stride*(b*p + p_1),0);
        data.set(i0+stride*b*p,0, z0 + z1);
        data.set(i0+stride*(b*p + p_1),0, z0 - z1);
      }

      /* a = 1 ... p_{i-1}/2 - 1 */

      {
        double w_real = 1.0;
        double w_imag = 0.0;

        double theta = 2.0 * Math.PI / p;
        
        double s = Math.sin(theta);
        double t = Math.sin(theta / 2.0);
        double s2 = 2.0 * t * t;
	
        for (a = 1; a < (p_1)/2; a++) {
        /* trignometric recurrence for w-> exp(i theta) w */
          double tmp_real = w_real - s * w_imag - s2 * w_real;
          double tmp_imag = w_imag + s * w_real - s2 * w_imag;
          w_real = tmp_real;
          w_imag = tmp_imag;

          for (b = 0; b < q; b++) {
            double z0_real =  data.get(i0+stride*(b*p + a),0);
            double z0_imag =  data.get(i0+stride*(b*p + p - a),0);
            double z1_real =  data.get(i0+stride*(b*p + p_1 - a),0);
            double z1_imag = -data.get(i0+stride*(b*p + p_1 + a),0);
		
            /* t0 = z0 + z1 */		
            data.set(i0+stride*(b*p + a),0, z0_real + z1_real);
            data.set(i0+stride*(b*p + p_1 - a),0, z0_imag + z1_imag);

            /* t1 = (z0 - z1) */
            double t1_real = z0_real -  z1_real;
            double t1_imag = z0_imag -  z1_imag;
            data.set(i0+stride*(b*p + p_1 + a), 0, (w_real * t1_real - w_imag * t1_imag));
            data.set(i0+stride*(b*p + p - a), 0, (w_real * t1_imag + w_imag * t1_real));
          }
        }
      }

      if (p_1 >  1) {
        for (b = 0; b < q; b++) {
          data.set(i0+stride*(b*p + p_1/2),0,
                               data.get(i0+stride*(b*p + p_1/2),0) * 2);
          data.set(i0+stride*(b*p + p_1 + p_1/2),0,
                               data.get(i0+stride*(b*p + p_1 + p_1/2),0) * -2);
        }
      }

      p_1 = p_1 / 2 ;
      p = p / 2 ;
      q = q * 2 ;
    }
    bitreverse(data, i0, stride);
  }

  private void bitreverse(Matrix data, int i0, int stride) {
    /* This is the Goldrader bit-reversal algorithm */
    int n = indexVectorSize;
    for (int i = 0,j = 0; i < n - 1; i++) {
      int k = n / 2;
      if (i < j) {
        double tmp = data.get(i0+stride*i,0);
        data.set(i0+stride*i,0, data.get(i0+stride*j,0));
        data.set(i0+stride*j,0, tmp);
      }
      while (k <= j) {
        j = j - k;
        k = k / 2;
      }
      j += k;
    }
  }

  private static int log2 (int n){
    int log = 0;

    for(int k=1; k < n; k *= 2, log++);

    if (n != (1 << log))
      return -1 ; /* n is not a power of 2 */
    return log;
  }

  private Matrix newVector(double v) {
    return new Matrix(indexVectorSize, 1, v);
  }
}
