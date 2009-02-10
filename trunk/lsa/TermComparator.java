import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class TermComparator {

    public static final int MAX_SIMILAR_ITEMS = 100;

    public static final int NUM_THREADS = 4;

    private static class StrPair {
	
	final String s1;
	final String s2;

	public StrPair(String st1, String st2) {
	    if (st1.compareTo(st2) < 0) {
		s1 = st1;
		s2 = st2;
	    }
	    else {
		s1 = st2;
		s2 = st2;
	    }
	}

	public boolean equals(Object o) {
	    if (o instanceof StrPair) {
		StrPair p = (StrPair)o;
		return p.s1.equals(s1) && p.s2.equals(s2);
	    }
	    return false;
	}

	public int hashCode() {
	    return s1.hashCode() ^ s2.hashCode();
	}
    }

    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage <input-dir> <output-dir>");
	    return;
	}
	
	try {

	    final File inputDir = new File(args[0]);	    
	    final File outputDir = new File(args[1]);
	    System.out.println("input dir: " + inputDir);
	    System.out.println("output dir: " + outputDir);
// 	    final Map<StrPair,Double> termsToDistances = 
// 		new ConcurrentHashMap<StrPair,Double>();

	    final Map<String,double[]> termToVector = 
		new HashMap<String,double[]>();
	    
	    int loaded = 0;
	    for (File termFile : inputDir.listFiles()) {
		//if (++loaded % 500 == 0)
		    System.out.printf("loaded %d terms%n", loaded);
		String term = termFile.getName().split("\\.")[0];
		BufferedReader br = 
		    new BufferedReader(new FileReader(termFile));
		System.out.println(termFile);
		String[] valueStrs = (br.readLine()).trim().split("\\s+");
		//String[] valueStrs = (br.readLine()).split(",");
		//System.out.println(term + ": " + valueStrs.length);
		//System.out.println("values: " + java.util.Arrays.toString(valueStrs));
		double[] values = new double[valueStrs.length];
		for (int i = 0; i < valueStrs.length; ++i) {
		    values[i] = Double.parseDouble(valueStrs[i]);
		}
		termToVector.put(term,values);
		br.close();
	    }
	    System.out.printf("loaded %d terms total%n", loaded);

	    ThreadPoolExecutor executor = 
		new ScheduledThreadPoolExecutor(NUM_THREADS);

	    
	    for (String term2 : termToVector.keySet()) {
		final String term = term2;
		executor.submit(new Runnable() {
			public void run() {
			    System.out.println(term);
			    
			    double[] vector = termToVector.get(term);
			    //BoundedSortedMap<Double,String> mostSimilar =
			    SortedMap<Double,String> mostSimilar =
				new BoundedSortedMap<Double,String>(100);
			    //termToMostSimilar.get(term);
			    new TreeMap<Double,String>();
			    
			    try {
				for (String other : termToVector.keySet()) {
				    //System.out.println("\t" + other);
				    if (term.equals(other)) 
					continue;
				    
				    StrPair pair = new StrPair(term, other);
				    
				    // see if we already ave the terms distance cached
				    double dist;
				    double[] otherVec = termToVector.get(other);
				    
				    //System.out.printf("%s -> %s%n", term, other);
				    dist = cosDist(vector, otherVec);
				    // double dist = euclidianDist(vector, otherVec);
				    //double dist = absCosDist(vector, otherVec);
				    
				    mostSimilar.put(1 - dist,other);
				}
			    }
			    catch (Throwable t) {
				t.printStackTrace();
			    }

			    File outputFile = new File(outputDir, term + ".mostSimilar");
			    
			    try {
				PrintWriter pw = new PrintWriter(outputFile);
				// write similarities to file.
				for (Map.Entry<Double,String> e : mostSimilar.entrySet()) {
				    String s = e.getValue();
				    Double d = e.getKey();
				    pw.printf("%s\t%f%n", s, 1 - d.doubleValue());
				}
				pw.close();
			    } catch (Throwable t) {
				t.printStackTrace();
			    }
			}
		    });
	    }

	    executor.shutdown();

	    // wait until all the documents have been parsed
	    try {
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	    } catch (InterruptedException ie) {
		ie.printStackTrace();
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    private static double euclidianDist(double[] a, double[] b) {
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

    private static double cosDist(double[] a, double[] b) {
	if (a == null || b == null || a.length != b.length)
	    throw new IllegalArgumentException("a: " + ((a == null) 
							? "null"
							: "a[" + a.length + "]")
					       + "; b: " + 
					       ((b == null)? "null"
						: "b[" + b.length + "]"));
	
	double dotProduct = 0;
	double aMagSum = 0;
	double bMagSum = 0;
	for (int i = 0; i < a.length; ++i) {
	    double d = a[i];
	    aMagSum += d * d;
	    double e = b[i];
	    bMagSum += e * e;
	    dotProduct += d * e;
	}

	double aMag = Math.sqrt(aMagSum);
	double bMag = Math.sqrt(bMagSum);
	
	return dotProduct / (aMag * bMag);
    }

    private static double absCosDist(double[] a, double[] b) {
	if (a == null || b == null || a.length != b.length)
	    throw new IllegalArgumentException("a: " + a + "; b: " + b);
	
	double dotProduct = 0;
	double aMagSum = 0;
	double bMagSum = 0;
	for (int i = 0; i < a.length; ++i) {
	    double d = Math.abs(a[i]);
	    aMagSum += d * d;
	    double e = Math.abs(b[i]);
	    bMagSum += e * e;
	    dotProduct += d * e;
	}

	double aMag = Math.sqrt(aMagSum);
	double bMag = Math.sqrt(bMagSum);
	
	return dotProduct / (aMag * bMag);
    }

    private static class BoundedSortedMap<K,V> extends TreeMap<K,V> {

	private final int bound;

	public BoundedSortedMap(int bound) {
	    super();
	    this.bound = bound;
	}

	public V put(K key, V value) {
	    V old = super.put(key, value);
	    if (size() > bound) {
		remove(lastKey());
	    }
	    return old;
	}

	public void putAll(Map<? extends K,? extends V> m) {
	    for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
		put(e.getKey(), e.getValue());
	    }
	}
    }


}