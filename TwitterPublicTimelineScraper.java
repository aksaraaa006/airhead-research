import java.io.*;
import java.net.*;
import java.util.*;

public class TwitterPublicTimelineScraper {

    public static void main(String[] args) {
	if (args.length != 1) {
	    System.out.println("usage: <output file>");
	    return;
	}
	try {
	    Object DUMMY = new Object(); // for the amp
	    BoundedMap<String,Object> recentMessages = 
		new BoundedMap<String,Object>(100);

	    PrintWriter pw = new PrintWriter(args[0]);

	    while (true) {

		try {
		    // get the most recent messages on the feed
		    Collection<String> newMessages = getPageScrape();
		    
		    // see which of them haven't been added and print those.
		    for (String mesg : newMessages) {
			if (!recentMessages.containsKey(mesg)) {
			    pw.println(mesg);
			    pw.flush();
			    System.out.println(System.currentTimeMillis() 
					       + " " + mesg);
			}
			else {
			    System.out.println("redundant");
			}
		    }
		    
		    // then add them to the set of recently seen messages
		    for (String mesg : newMessages) {
			recentMessages.put(mesg, DUMMY);
		    }
		    
		    // Anything less that 4 seconds and we seem to start getting
		    // redundant messages.  Clicking refresh in the browser
		    // seems to get new messages sooner, so I'm not sure what
		    // the issue is.
		    Thread.sleep(4000);
		} catch (Throwable t) {
		    t.printStackTrace();
		    break;
		}
	    }
	    
	    pw.close();
	} catch (Throwable t) {
	    t.printStackTrace();
	} 
    }

    public static Collection<String> getPageScrape() throws IOException {
	
	String marker = "    <h3 class=\"timeline-subheader\">What everyone on Twitter is talking about!</h3>";
	    
	URL recentFeed = new URL("http://twitter.com/public_timeline");

	BufferedReader br = new BufferedReader(
	    new InputStreamReader(recentFeed.openStream()));

	Collection<String> messages = new LinkedList<String>();
	
	String MESG_PREFIX = "entry-content\">";
	
	for (String line = null; (line = br.readLine()) != null; ) {
	    
	    // check that the URL crawl is still accessing valid pages
	    if(line.startsWith(marker)) {
		br.readLine(); // skip line
		String twits = br.readLine();
		
		for (int i = -1; (i = twits.indexOf(MESG_PREFIX, i+1)) >= 0; ) {

		    // grab the message
		    int mesgStart =  i + MESG_PREFIX.length();
		    int mesgEnd = twits.indexOf("</span>", mesgStart);
		    if (mesgEnd < 0) {
			break;
		    }
		    String mesg = twits.substring(mesgStart, mesgEnd);
		    messages.add(mesg);
		}
		break;
	    }
	}

	br.close();
	return messages;
   }


    private static class BoundedMap<K,V> extends LinkedHashMap<K,V> {

	private final int maxSize;

	public BoundedMap(int maxSize) {
	    this.maxSize = maxSize;
	}

	protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > maxSize;
	}
    }
}