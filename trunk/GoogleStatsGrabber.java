import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class GoogleStatsGrabber {

    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage: <x-list file> <outfile>");
	    return;
	}
	try {
	    File outputDir = new File(args[1]);
	    if (!outputDir.isDirectory()) {
		System.out.println("second arg must be a directory");
		return;
	    }

	    List<List<String>> listOfXLists = new ArrayList<List<String>>(5000);
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    ArrayList<String> xList = null;
	    Set<String> verbs = new TreeSet<String>();
	    for (String line = null; (line = br.readLine()) != null;) {
		// look for start of X-list
		if (line.startsWith("X-LIST:")) {
		    // if there was xList already, add it to the list-of-lists
		    // and create a new one
		    if (xList != null && xList.size() > 0)
			listOfXLists.add(xList);
		    
		    xList = new ArrayList<String>(300);
		}
		// verbs should be at the end, so just read util we're done
		else if (line.startsWith("VERBS:")) {
		    
		    // first add in the last x-list
		    if (xList != null && xList.size() > 0)
			listOfXLists.add(xList);
		    
		    for (; (line = br.readLine()) != null;) {
			verbs.add(line.trim());
		    }
		}
		// otherwsise, we're not creating a new x-list or enumerating
		// verbs, so add the current pair to the current x-list
		else {
		    xList.add(line.trim());
		}
	    }

	    System.out.println("saw " + listOfXLists.size() + " x-lists");

	    
	    // once we've finished parsing the x-lists and verbs, generate
	    // statistics for each one
	    getStatistics(listOfXLists, verbs, outputDir);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private static void getStatistics(List<List<String>> listOfXLists,
				      Set<String> verbs, 
				      File outputDir) throws Exception {
	
	Map<Integer,Map<String,Map<String,Integer>>> xListIndexToPairCountMap =
	    new HashMap<Integer,Map<String,Map<String,Integer>>>();

	ArrayList<Runnable> queries = 
	    new ArrayList<Runnable>(1000000);


	int listIndex = 0;
	for (List<String> xList : listOfXLists) {

	    System.out.println("building queries for: " + listIndex);
	    final Integer listId = Integer.valueOf(listIndex);

	    final Map<String,Map<String,Integer>> pairToVerbCounts = 
		new HashMap<String,Map<String,Integer>>();

	    xListIndexToPairCountMap.put(listId, pairToVerbCounts);

	    for (String pair : xList) {
	       	
		// split into individual components
		String[] words = pair.split(",");
		// System.out.println(pair);

		// sanity check to ensure that if either of the words is a
		// compound noun, that our google * query will still work
		String first = words[0].trim().replaceAll(" ", "+");
		String last = words[1].trim().replaceAll(" ", "+");

		final Map<String,Integer> verbToPageCountSum = 
		    new HashMap<String,Integer>();

		final String pairKey = first + "," + last;
		pairToVerbCounts.put(pairKey, verbToPageCountSum);
		
		
		// for each verb, see how many google hits we get within a
		// sliding range of wildcard (*) words
		for (String verb : verbs) {
		    		  
		    final String v = verb; // for inner class
		    verbToPageCountSum.put(v, Integer.valueOf(0));
		    
		    int NUM_WILDCARDS = 3;
		    for (int stars = 0; stars < NUM_WILDCARDS; ++stars) {
			// build the query based on the first string
			String queryPrefix = 
			    "http://www.google.com/search?hl=en&q=\"" +
			    first + "+";
			// add different combinatiosn of wild cards around the
			// verb
			for (int verbPos = 0; verbPos <= stars; ++verbPos) {
			    
			    String leading = "", trailing = "";
			    for (int i = 0; i < verbPos; ++i)
				leading += "*+";
			    for (int i = verbPos; i < stars; ++i)
				trailing += "+*";
			    
			    final String query = queryPrefix + leading + verb + 
				trailing + "+" + last + "\"&btnG=Search";
			    
			    // one runnable per query
			    queries.add(new Runnable() {
 				    public void run() {
					System.out.println(query);
					int curCount = verbToPageCountSum.
					    get(v).intValue();
					int count = getPageCount(query);
					verbToPageCountSum.put(v, 
							       curCount+count);
 				    }
 				});
			}
		    }		    
		}
	    }


	    // increment the list index
	    listIndex++;

	    double numQueries = queries.size();	    
	    System.out.printf("executing %d queries for x-list", 
			      (int)numQueries);

	    long SLEEP_TIME = 2000;
	    long RANDOM_BUFFER = 1000;
	    long QUERIES_PER_MIN = 60 / (SLEEP_TIME / 1000 + 
					 RANDOM_BUFFER / 2000); // expected value
	    System.out.printf("estimating %.0f minutes (%.2f hours (%.2f days))",
			      (numQueries / QUERIES_PER_MIN), 
			      (numQueries / (QUERIES_PER_MIN * 60)),
			      (numQueries / (QUERIES_PER_MIN * 1440)));
	    
	    // We need to shuffle to ensure that successive queries don't look
	    // too similar, otherwise Google thinks we're a DOS attack and we
	    // get nothing but 503 errors for all the queries.
	    Collections.shuffle(queries);
	    for (Runnable r : queries) {
		// add a 1 second sleep in just in case latency factors into
		// Google's 503-response threshold.
		Thread.sleep(SLEEP_TIME + (long)(Math.random()* RANDOM_BUFFER));
		r.run();
	    }	    
	}

	for (Map.Entry<Integer,Map<String,Map<String,Integer>>> e : 
		 xListIndexToPairCountMap.entrySet()) {

	    Integer xListId = e.getKey();
	    Map<String,Map<String,Integer>> pairToVerbVector =
		e.getValue();
	    PrintWriter xListPrinter = new PrintWriter(
		new File(outputDir, xListId + ".vectors"));

	    
	    for (Map.Entry<String,Map<String,Integer>> e2 :
		     pairToVerbVector.entrySet()) {
		
		String pair = e2.getKey();
		Map<String,Integer> verbToCount = e2.getValue();

		StringBuffer vectorStr = new StringBuffer();
		Iterator<Map.Entry<String,Integer>> it = 
		    verbToCount.entrySet().iterator();
		while (it.hasNext()) {
		    Map.Entry<String,Integer> e3 = it.next();
		    vectorStr.append(e3.getValue());
		    if (it.hasNext())
			vectorStr.append(",");
		}
		xListPrinter.println(pair + "|" + vectorStr.toString());
	    }

	    xListPrinter.close();
	}
    }



    public static int getPageCount(String query) {
	BufferedReader br = null;
	int dummy = 0;
	try {
	    URL url = new URL(query);;
	    br = new BufferedReader(new InputStreamReader(url.openStream()));
		
	    for (String line = null; (line = br.readLine()) != null; ) {
		    
		int i = line.indexOf("</b> of about <b>");
		if (i >= 0) {
		    int start = i + 17; // length of string
		    int end = line.indexOf("</b>", start);
		    if (end > 0) {
			String count = line.substring(start,end);
			System.out.println(count);
			// google uses ',' to group triads, so replace them
			count = count.replaceAll(",","");
			return Integer.parseInt(count);
		    }
		}
	    }
	}
	catch (Throwable t) {
	    // don't print out errors when it's really that the page didn't
	    // exist in the first place
	    if (!t.getMessage().contains("403 for URL"))
		t.printStackTrace();
	}
	finally {
	    if (br != null) {
		try { br.close(); } catch (Throwable t) { t.printStackTrace(); }
	    }
	}
	return dummy;
    }


}