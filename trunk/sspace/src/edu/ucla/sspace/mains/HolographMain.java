package edu.ucla.sspace.mains;

import edu.ucla.sspace.holograph.Holograph;

import java.io.BufferedReader;
import java.io.FileReader;

class HolographMain {
  public static void usage() {
    System.out.println("use --docsFile");
  }

  public static void main(String[] args) {
    Holograph holographBuilder = new Holograph();
	String input = null;
	if (args.length == 0)
	    usage();
	else { 
      if (args[0].startsWith("--docsFile=")) {
        try {
            String docsFile = 
            args[0].substring("--docsFile=".length());
            input = docsFile;
            BufferedReader br = 
            new BufferedReader(new FileReader(docsFile));
            String document = null;
            int count = 0;
            while ((document = br.readLine()) != null) {
            System.out.print("parsing document " + (count++) + ": "
                   + document + " ...");
            long startTime = System.currentTimeMillis();
            holographBuilder.parseDocument(document);
            long endTime = System.currentTimeMillis();
            System.out.printf("complete (%.3f seconds)%n",
                      (endTime - startTime) / 1000d);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
      }
      else {
        System.out.println("unrecognized argument: " + args[0]);
        usage();
        System.exit(0);
      }
      System.out.println("similarity between Eat and Eaten: " + 
                         holographBuilder.computeSimilarity("eat", "eaten"));
      System.out.println("similarity between Eat and buy: " + 
                         holographBuilder.computeSimilarity("eat", "buy"));
      System.out.println("similarity between Eat and feed: " + 
                         holographBuilder.computeSimilarity("eat", "feed"));
      System.out.println("similarity between car and driver: " + 
                         holographBuilder.computeSimilarity("car", "driver"));
      System.out.println("similarity between car and driver: " + 
                         holographBuilder.computeSimilarity("car", "boat"));
      System.out.println("similarity between car and driver: " + 
                         holographBuilder.computeSimilarity("car", "truck"));
      holographBuilder.lutherTest();
    }
  }
}
