package edu.ucla.sspace.esa;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import edu.ucla.sspace.common.StringUtils;

/**
 * An implementation of Explicit Semanic Analysis proposed by Evgeniy
 * Gabrilovich and Shaul Markovitch.  For full details see:
 *
 * <p> Evgeniy Gabrilovich and Shaul Markovitch. (2007). "Computing Semantic
 * Relatedness using Wikipedia-based Explicit Semantic Analysis," Proceedings of
 * The 20th International Joint Conference on Artificial Intelligence (IJCAI),
 * Hyderabad, India, January 2007. </p>
 *
 * @author David Jurgens
 */
public class ExplicitSemanticAnalysis {

    public static class FileCache {
	
	public static final int CACHE_SIZE = 32;

	private final Queue<String> fileNames;

	private final BlockingQueue<OpenedFile> cachedFiles;

	public FileCache(Queue<String> fileNamesP) {
	    this.fileNames = fileNamesP;
	    cachedFiles = new LinkedBlockingQueue<OpenedFile>();
	    
	    for (int i = 0; i < CACHE_SIZE; ++i) 
		new Thread() {
		    public void run() {
			String fileName = fileNames.poll();
			if (fileName != null) {
			    try {
				cachedFiles.
				    offer(new OpenedFile(fileName,
							 new FileReader(fileName)));
			    } 
			    catch (Throwable t) {
				t.printStackTrace();
			    }
			}
		    }
		}.start();

	}

	public boolean hasNext() {
	    return !cachedFiles.isEmpty() || !fileNames.isEmpty();
	}
	
	public OpenedFile next() throws InterruptedException {
	    if (cachedFiles.isEmpty() && fileNames.isEmpty())
		return null;
	    // start a thread to open up the next file.
	    new Thread() {
		public void run() {
		    String fileName = fileNames.poll();
		    if (fileName != null) {
			try {
			    cachedFiles.
				offer(new OpenedFile(fileName,
						     new FileReader(fileName)));
			} 
			catch (Throwable t) {
			    t.printStackTrace();
			}	
		    }
		}
	    }.start();	    
	    return cachedFiles.poll(10000L, TimeUnit.MILLISECONDS);
	    
	}
    }

    private static final class OpenedFile {

	public final String fileName;
	public final FileReader reader;

	public OpenedFile(String fileName, FileReader reader) {
	    this.fileName = fileName;
	    this.reader = reader;
	}

    }

    private static class DocumentBufferedQueue {
	
	private static final int DOCS_TO_CACHE = 100;

	private static final int TITLE_HTML_LENGTH = "    <title>".length();

	private final BufferedReader wikiReader;

	private final BlockingQueue<WikiDoc> cachedDocs;

	private final AtomicBoolean isReaderOpen;
	
	public DocumentBufferedQueue(String wikipediaFile) 
	    throws IOException {

	    wikiReader = new BufferedReader(new FileReader(wikipediaFile));
	    cachedDocs = new LinkedBlockingQueue<WikiDoc>();
	    isReaderOpen = new AtomicBoolean(true);

	    for (int i = 0; i < DOCS_TO_CACHE; ++i) {
		WikiDoc d = cacheDoc();
		if (d != null)
		    cachedDocs.offer(d);
	    }
	}

	private synchronized WikiDoc cacheDoc() throws IOException {
	    StringBuilder sb = new StringBuilder();
	    String articleTitle = null;
	    article_grab:
	    for (String line = null; (line = wikiReader.readLine()) != null;) {

		if (line.startsWith("</mediawiki>")) {
		    // end of input
		    isReaderOpen.set(false);
		}
		if (line.startsWith("  <page>")) {
		    try {
			// title immediately follows page declaration
			String titleLine = wikiReader.readLine();
			// titles start with '    <title>'		    
			String rem = titleLine.substring(TITLE_HTML_LENGTH);
			int index = rem.indexOf("<");
			if (index < 0)
			    throw new Error("Malformed title: " + line);
			articleTitle = rem.substring(0, index);
			articleTitle = 
			    articleTitle.replaceAll("/"," ").toLowerCase();
			// System.out.println("cached: " + articleTitle);

			// read in the rest of the page until we see the end tag
			while ((line = wikiReader.readLine()) != null && 
			       !line.startsWith("  </page>")) {
			    sb.append(line);
			}
			break article_grab;
		    }
		    catch (Throwable t) {
			t.printStackTrace();
			break;
		    }
		}		
	    }
	    return (articleTitle == null) 
		? null 
		: new WikiDoc(articleTitle, sb.toString());
	}

	public boolean hasNext() {
	    return cachedDocs.size() > 0 || isReaderOpen.get();
	}

	public WikiDoc next() throws InterruptedException {
	    new Thread() {
		public void run() {
		    try {
			WikiDoc d = cacheDoc();
			if (d != null)
			    cachedDocs.offer(d);		    
		    }
		    catch (IOException ioe) {
			ioe.printStackTrace();
		    }
		}
	    }.start();
	    // Don't block.  Wait up to 10 minutes (in case of GC) to poll. 
	    return cachedDocs.poll(60 * 10 * 1000L, TimeUnit.MILLISECONDS);
	}
    }

    private static class WikiDoc {
	
	public final String name;
	public final String text;

	public WikiDoc(String name, String text) {
	    this.name = name;
	    this.text = text;
	}
    }

    public static void main(String[] args) {
	if (args.length < 3) {
	    System.out.println("usage java <raw-wikipedia-dump> "
			       + "<output-file> <valid-terms-file>");
	    return;
	}
	try {
	    // read in the list of valid words
	    System.out.print("reading in valid terms...");
	    NavigableSet<String> validWords = new TreeSet<String>();
	    BufferedReader wordReader = 
		new BufferedReader(new FileReader(args[2]));	
	    for (String line = null; (line = wordReader.readLine()) != null; ) {
		validWords.add(line.toLowerCase().trim());
	    }
	    System.out.println(validWords.size() + " total terms");
	    
	    /*
	    Queue<String> rawFileNames = new ConcurrentLinkedQueue<String>();
	    BufferedReader rawFileReader = 
		new BufferedReader(new FileReader(args[0]));
	    for (String line = null; (line = rawFileReader.readLine()) != null; ) {
		rawFileNames.add(line.trim());
	    }
	    rawFileReader.close();
	    System.out.printf("preparing to read in %d files%n",
			      rawFileNames.size());
	    FileCache fileCache = new FileCache(rawFileNames);
	    */

	    DocumentBufferedQueue docQueue = 
		new DocumentBufferedQueue(args[0]);
	    
	    String outputFile = args[1];
	    PrintWriter tmpOutput = new PrintWriter(outputFile + ".tmp");
	    PrintWriter termsOutput = new PrintWriter(outputFile + ".terms");

	    // NOTE: this map only works because we call .intern() on all the
	    // strings to be used as keys
	    Map<String,Integer> articleToIncomingLinkCount = 
		new IdentityHashMap<String,Integer>(8000000);	    	    
	    
	    int files = 0;
	    // next go through the raw articles and look for link counts
	    //Iterator<String> fileNameIter = rawFileNames.iterator();
	    //for (String rawFileName : rawFileNames) {
	    //while (fileNameIter.hasNext()) {
	    while (docQueue.hasNext()) {
		//OpenedFile openedFile = fileCache.next();
		WikiDoc doc = docQueue.next();
		
		if (doc == null) {
		    System.out.println("race condition in the document " +
				       "caching; continuing...");
		    break;
		}

		String rawArticleName = doc.name;
		// sanity check in case we didn't get a valid document
		if (rawArticleName == null || doc.text == null) {
		    System.out.println("race condition in the document " +
				       "caching; continuing...");
		    break;
		}
		String articleName = StringUtils.unescapeHTML(rawArticleName,0).
		    replaceAll("/"," ").toLowerCase().trim();
		
		try {
		    // skip articles that are not text-based or are
		    // wikipedia-specific
		    if (articleName.startsWith("image:") ||
			articleName.startsWith("wikipedia:") ||
			articleName.startsWith("template:") ||
			articleName.startsWith("category:") ||
			articleName.contains("(disambiguation)")) {
			
			System.out.printf("skipping file %d: %s%n", 
					  ++files, articleName);
			continue;
		    }
		    else if (doc.text.contains("#REDIRECT")) {
			System.out.printf("skipping redirect %d: %s%n", 
					  ++files, articleName);
		    }
		    else {
			// intern the article name since it will be around a
			// while
			articleName = articleName.intern();
		    }
			
		    System.out.printf("parsing file %d: %s ", 
				      ++files, articleName);
		    
		    //System.out.println(articleToIncomingLinkCount);
		    //System.out.println(articleToOutgoingLinkCount);
		    
		    //System.out.println("Removing HTML");
		    
		    String article = doc.text;;
		    //System.out.println("RAW: " + article);
		    int lastGoodIndex = 0;				
		    StringBuilder noHtml = new StringBuilder(article.length());
		    
		    // remove all html tags before we unescape the text itself
		    // and possibly introduce non-html < characters
		    int startOfTextTag = article.indexOf("<text");
		    int endOfStart  = article.indexOf(">", startOfTextTag);
		    
		    int closingTextTag = article.indexOf("</text");
		    String noHtmlStr = article.substring(endOfStart+1, 
							 closingTextTag);
		    
		    /*
		     * now get rid of {{wiki-stuff}} tags
		     */
		    
		    //System.out.println("Removing {{wiki}} tags");
		    
		    String phase2 = noHtmlStr;
		    //System.out.println("NO HTML OR LINKS: "	 
		    // + noHtmlOrLinksStr);
		    StringBuilder phase3sb = 
			new StringBuilder(phase2.length());
		    lastGoodIndex = 0;
		    
		    if (phase2.indexOf("{{") >= 0) {
			// remove all html tags before we unescape the text
			// itself and possibly introduce non-html < characters
			for (int i = 0; (i = phase2.indexOf("{{", i)) >= 0; ) {
			    String s = phase2.substring(lastGoodIndex,i);
			    //System.out.println("clean substr: " + s);
			    phase3sb.append(s);
			    lastGoodIndex = phase2.indexOf("}}", i) + 2;
			    i = lastGoodIndex;
			    // REMINDER: this is doing extra work, this loop
			    // should be rewritten
			    if (phase2.indexOf("{{", i) == -1) {
				phase3sb.append(phase2.
						substring(lastGoodIndex));
				break;
			    }
			} 
		    }
		    // in case there is no wiki mark-up
		    else
			phase3sb.append(phase2);
		    
		    
		    /*
		     * Replace [[link]] tags with link name.
		     *
		     * Also update link counts
		     */
		    //System.out.println("NO HTML: " + noHtmlStr);
		    //System.out.println("Replacing [[link]] tags");
		    String phase3 = phase3sb.toString();
		    StringBuilder phase4sb = new StringBuilder(phase2.length());
		    lastGoodIndex = 0;
		    int outgoingLinks = 0;
		    int prevTotalIncomingLinks = 
			articleToIncomingLinkCount.size();
		    //System.out.println(phase3);

		    // remove all html tags before we unescape the text itself
		    // and possibly introduce non-html < characters
		    for (int i = 0; (i = phase3.indexOf("[[", i)) > 0; ) {
			
			phase4sb.append(phase3.substring(lastGoodIndex,i));
			
			// grab the linked article name which is all text to the
			// next ]], or to the next | in the case where the
			// article is given a different name in the text, e.g.
			// [[article title|link text]]
			int j = phase3.indexOf("]]", i);
			int k = phase3.indexOf("|", i);
			int linkEnd = (k > 0) ? (j < 0) ? k : Math.min(j,k) : j;
			
			//System.out.printf("i: %d, j: %d, k: %d, end: %d%n",
			// i,j,k,linkEnd);
			
			
			// transform the file name to get rid of any special
			// characters
			String linkedArticleRawName = 
			    phase3.substring(i+2,linkEnd);
			String linkedArticleTitle = linkedArticleRawName.
			    replaceAll("/", " ").toLowerCase();
			linkedArticleTitle = 
			    StringUtils.unescapeHTML(linkedArticleTitle, 0);
			
			// don't include Image, foreign language or
			// disambiguation links
			if (!(linkedArticleTitle.startsWith("image:") ||
			      linkedArticleTitle.startsWith("wikipedia:") ||
			      linkedArticleTitle.startsWith("template:") ||
			      linkedArticleTitle.startsWith("category:") ||
			      (linkedArticleTitle.length() >= 3 && 
			       linkedArticleTitle.charAt(2) == ':') ||
			      linkedArticleTitle.contains("(disambiguation)")))
			    {
			    
			    // if the artile is actually a reasonable
			    // (e.g. non-image) article, then intern its string
			    // to save memory
			    linkedArticleTitle = linkedArticleTitle.intern();

			    // print out the link name so that it gets included
			    // in the term list
			    phase4sb.append(linkedArticleTitle).append(" ");

			    // increase the link counts accordingly
			    //
			    // NOTE: we have linkedArticleTitle is the canonical
			    // copy of the string (via intern()), and so this
			    // put() call works correctly, i.e. doesn't create
			    // duplicate keys
			    Integer incomingLinks = articleToIncomingLinkCount.
				get(linkedArticleTitle);

			    articleToIncomingLinkCount.
				put(linkedArticleTitle, (incomingLinks == null)
				    ? Integer.valueOf(1)
				    : Integer.valueOf(1 + incomingLinks));

			    ++outgoingLinks;
			    
			}
			
			lastGoodIndex = phase3.indexOf("]]", i) + 2;
			i = lastGoodIndex;
			
			// the "j < 0" condition is for malformed wiki pages
			// where there is no closing ]] for the link.
			if (phase3.indexOf("[[", i) < 0 || j < 0) {
			    phase4sb.append(phase3.substring(lastGoodIndex));
			    break;
			}
		    } // end [[ loop    
	   	    
		    String scrubbed = phase4sb.toString();
		    scrubbed = StringUtils.unescapeHTML(scrubbed,0).
			//replaceAll("[^A-Za-z0-9'\u00E0-\u00FF]", " ").
			replaceAll("#REDIRECT","");
		    
		    //System.out.println("CLEANED:" + scrubbed);
		    //articleToText.put(articleName,scrubbed);
		    //System.out.println("ARTICLE NAME: " + articleName);

		    tmpOutput.println(articleName + "|" + outgoingLinks
				      + "|" + scrubbed);		 
		    tmpOutput.flush();
		    
		    int newIncomingLinks = articleToIncomingLinkCount.size() -
			prevTotalIncomingLinks;
		    Integer curIncoming = 
			articleToIncomingLinkCount.get(articleName);
		    
		    System.out.println("link summary: " + outgoingLinks + 
				       " outgoing, (" + newIncomingLinks + 
				       " new docs); "
				       + ((curIncoming == null) ? "0" :
					  curIncoming.toString())
				       + " incoming");
		} 
		catch (Throwable t) {
		    t.printStackTrace();
		}
	    } // end file loop

	    tmpOutput.close();

	    System.out.println("REMOVING DOCUMENTS");
	    PrintWriter output = new PrintWriter(outputFile);
	    PrintWriter incomingLinkCounts = 
		new PrintWriter(outputFile + ".incoming");
	    //System.out.println("INCOMING: " + articleToIncomingLinkCount);
	    //System.out.println("OUTGOING: " + articleToOutgoingLinkCount);

	    // now read it back in and decide which of the term documents should
	    // actually get included in the output
	    int removed = 0 ;
	    BufferedReader br = 
		new BufferedReader(new FileReader(outputFile + ".tmp"));
	    for (String line = null; (line = br.readLine()) != null; ) {
		try {
		    String[] arr = line.split("\\|");
		    String term = arr[0].intern();
		    int outgoing = Integer.parseInt(arr[1]);
		    
		    String doc = (arr.length < 2) ? "" : arr[2];
		    Integer incoming = articleToIncomingLinkCount.get(term);
		    if (incoming == null)
			incoming = Integer.valueOf(0);
		    incomingLinkCounts.println(term + "|" + incoming);
		    
		    termsOutput.println(term);
		    termsOutput.flush();
		    output.println(term + "|" + incoming + "|" + 
				   outgoing + "|" + doc);
		    output.flush();
		    incomingLinkCounts.flush();
		    
		} catch (Throwable t) {
		    t.printStackTrace();
		}
	    }

	    incomingLinkCounts.close();
	    termsOutput.close();
	    output.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}	
    }

}
