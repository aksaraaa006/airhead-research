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
	    getStatistics(listOfXLists, verbs, new PrintWriter(args[1]));
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private static void getStatistics(List<List<String>> listOfXLists,
				      Set<String> verbs, 
				      PrintWriter outfile) throws Exception {
	
	// randomize to avoid 503
	Collections.shuffle(listOfXLists);

	int listNum = 0;

	for (List<String> xList : listOfXLists) {

	    System.out.println("processing list: " + listNum);
	    listNum++;

	    final Map<String,Integer> pairToCount = new HashMap();
	    ArrayList<Runnable> queries = 
		new ArrayList<Runnable>(xList.size() *verbs.size());

	    for (String pair : xList) {
	       	
		// split into individual components
		String[] words = pair.split(",");
		System.out.println(pair);

		// sanity check to ensure that if either of the words is a
		// compound noun, that our google * query will still work
		String first = words[0].trim().replaceAll(" ", "+");
		String last = words[1].trim().replaceAll(" ", "+");

		Map<String,Integer> verbToPageCountSum = 
		    new HashMap<String,Integer>();

		final String pairKey = first + "," + last;
		pairToCount.put(pairKey, Integer.valueOf(0));
		
		
		// for each verb, see how many google hits we get within a
		// sliding range of wildcard (*) words
		for (String verb : verbs) {
		    
		    // fetch all of the pages in parallel
		    Collection<Thread> threads = new LinkedList<Thread>();
		    final AtomicInteger pageCount = new AtomicInteger(0);	  
		    
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
			    
			    // one thread per query
			    queries.add(new Runnable() {
 				    public void run() {
					System.out.println(query);
					int curCount = 
					    pairToCount.get(pairKey).intValue();
					int count = getPageCount(query);
					pairToCount.put(pairKey, 
							curCount + count);
 				    }
 				});
// 			    t.start();
// 			    threads.add(t);			    
			}
		    }
		    
		    try {
			for (Thread t : threads)
			    t.join();
		    } catch (Throwable t) { 
			t.printStackTrace();
		    }

		    verbToPageCountSum.put(verb, pageCount.intValue());
		}

		// generate the vector for this pair 	       
	    }
	    
	    System.out.println("executing queries for x-list");
	    
	    // We need to shuffle to ensure that successive queries don't look
	    // too similar, otherwise Google thinks we're a DOS attack and we
	    // get nothing but 503 errors for all the queries.
	    Collections.shuffle(queries);
	    for (Runnable r : queries) {
		// add a 1 second sleep in just in case latency factors into
		// Google's 503-response threshold.
		Thread.sleep(1000 + (long)(Math.random() * 2000));
		r.run();
	    }

	    //
	    // THIS IS WHERE WE WOULD GENERATE THE pair x verb MATRIX AND EITHER
	    // PRINT IT OUT OR COMPUTE THE SVD OR SOMETHING AWESOME.
	    //
	    // REMINDER: how do we name the x-lists for output?
	    //
	     
	    
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