package sspace.holograph;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class RandomIndexBuilder {
  private static final int DEFAULT_INDEX_VECTOR_SIZE = 2048;

  private HashMap<String, double[]> termToRandomIndex;
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
    fft(left);
    fft(right);
    double[] result = arrayTimes(left, right);

    reverseFFT(result);
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

  private void fft(double[] data) {
    int i0 = 0;
    int stride = 1;
    int n = indexVectorSize;
    int logn = log2(indexVectorSize);

    int p, p_1, q;

    /* bit reverse the ordering of input data for decimation in time algorithm */
    bitreverse(data, i0, stride);

    /* apply fft recursion */
    p = 1; q = n ;
    for (int i = 1; i <= logn; i++) {
      int a, b;

      p_1 = p ;
      p = 2 * p ;
      q = q / 2 ;

      /* a = 0 */

      for (b = 0; b < q; b++) {
	double t0_real = data[i0+stride*b*p] + data[i0+stride*(b*p + p_1)];
	double t1_real = data[i0+stride*b*p] - data[i0+stride*(b*p + p_1)];
	  
	data[i0+stride*b*p] = t0_real;
	data[i0+stride*(b*p + p_1)] = t1_real;
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
	    double z0_real = data[i0+stride*(b*p + a)];
	    double z0_imag = data[i0+stride*(b*p + p_1 - a)];
	    double z1_real = data[i0+stride*(b*p + p_1 + a)];
	    double z1_imag = data[i0+stride*(b*p + p - a)];
		
	    /* t0 = z0 + w * z1 */
	    data[i0+stride*(b*p + a)]    =z0_real + w_real * z1_real - w_imag * z1_imag;
	    data[i0+stride*(b*p + p - a)]=z0_imag + w_real * z1_imag + w_imag * z1_real;
	    /* t1 = -(z0 - w * z1) */
	    data[i0+stride*(b*p + p_1 - a)]=z0_real - w_real * z1_real + w_imag * z1_imag;
	    data[i0+stride*(b*p + p_1 + a)]=-(z0_imag - w_real * z1_imag - w_imag * z1_real);
	  }
	}
      }

      if (p_1 >  1) {
	for (b = 0; b < q; b++) {
	  /* a = p_{i-1}/2 */
	  data[i0+stride*(b*p + p - p_1/2)] *= -1 ;
	}}
    }
  }

  private void reverseFFT(double[] data) {
    int i0 = 0;
    int stride = 1;
    int n = indexVectorSize;
    int logn = log2(indexVectorSize);
    int p, p_1, q;

    /* apply fft recursion */

    p = n; q = 1 ; p_1 = n/2 ;

    for (int i = 1; i <= logn; i++) {
      int a, b;

      /* a = 0 */

      for (b = 0; b < q; b++) {
	double z0 = data[i0+stride*b*p];
	double z1 = data[i0+stride*(b*p + p_1)];
	data[i0+stride*b*p]         = z0 + z1 ;
	data[i0+stride*(b*p + p_1)] = z0 - z1 ; }

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
	    double z0_real =  data[i0+stride*(b*p + a)];
	    double z0_imag =  data[i0+stride*(b*p + p - a)];
	    double z1_real =  data[i0+stride*(b*p + p_1 - a)];
	    double z1_imag = -data[i0+stride*(b*p + p_1 + a)];
		
	    /* t0 = z0 + z1 */		
	    data[i0+stride*(b*p + a)]       = z0_real + z1_real;
	    data[i0+stride*(b*p + p_1 - a)] = z0_imag + z1_imag;

	    /* t1 = (z0 - z1) */
	    double t1_real = z0_real -  z1_real;
	    double t1_imag = z0_imag -  z1_imag;
	    data[i0+stride*(b*p + p_1 + a)] = (w_real * t1_real - w_imag * t1_imag) ;
	    data[i0+stride*(b*p + p - a)]   = (w_real * t1_imag + w_imag * t1_real) ;
	  }
	}
      }

      if (p_1 >  1) {
	for (b = 0; b < q; b++) {
	  data[i0+stride*(b*p + p_1/2)] *= 2 ;
	  data[i0+stride*(b*p + p_1 + p_1/2)] *= -2 ;
	}
      }

      p_1 = p_1 / 2 ;
      p = p / 2 ;
      q = q * 2 ;
    }

    /* bit reverse the ordering of output data for decimation in
       frequency algorithm */
    bitreverse(data, i0, stride);
  }

  private void bitreverse(double[] data, int i0, int stride) {
    /* This is the Goldrader bit-reversal algorithm */
    int n = indexVectorSize;

    for (int i = 0,j = 0; i < n - 1; i++) {
      int k = n / 2 ;
      if (i < j) {
	double tmp        = data[i0+stride*i];
	data[i0+stride*i] = data[i0+stride*j];
	data[i0+stride*j] = tmp; }

      while (k <= j) {
	j = j - k ;
	k = k / 2 ; }
      j += k ;
    }
  }

  private static int log2 (int n){
    int log = 0;

    for(int k=1; k < n; k *= 2, log++);

    if (n != (1 << log))
      return -1 ; /* n is not a power of 2 */
    return log;
  }

  private double[] newVector(double v) {
    double[] r = new double[indexVectorSize];
    for (int i = 0; i < indexVectorSize; ++i)
      r[i] = v;
    return r;
  }
}
