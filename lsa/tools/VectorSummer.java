import java.io.*;

public class VectorSummer {
    public static void main(String[] args) {
	try {
	    for (String file : args) {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String[] vals = br.readLine().split(" ");
		double sum = 0d;
		for (String val : vals) 
		    sum += Double.parseDouble(val);
		System.out.printf("%s (%d): %.5f%n",
				  file, vals.length, sum);
		br.close();
	    }
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }
}