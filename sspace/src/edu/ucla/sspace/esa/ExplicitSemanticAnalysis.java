/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.esa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ucla.sspace.common.Matrices;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StringUtils;

/**
 * An implementation of Explicit Semanic Analysis proposed by Evgeniy
 * Gabrilovich and Shaul Markovitch.  For full details see:
 *
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Evgeniy Gabrilovich and
 *     Shaul Markovitch. (2007). "Computing Semantic Relatedness using
 *     Wikipedia-based Explicit Semantic Analysis," Proceedings of The 20th
 *     International Joint Conference on Artificial Intelligence (IJCAI),
 *     Hyderabad, India, January 2007. </li>
 *
 * </ul>
 *
 * @author David Jurgens
 */
public class ExplicitSemanticAnalysis implements SemanticSpace {
    public static final String ESA_SSPACE_NAME =
    "esa-semantic-space";

    /**
     * The logger for this class based on the fully qualified class name
     */
    private static final Logger ESA_LOGGER = 
	Logger.getLogger(ExplicitSemanticAnalysis.class.getName());
    
    private final Map<String,Integer> wikipediaTermsToIndex;

    /**
     * The article co-occurrence matrix.  This field is set in {@link
     * processDocument(BufferedReader) processDocument} after the number of
     * valid articles is known.
     */
    private Matrix articleMatrix;

    public ExplicitSemanticAnalysis() {
	wikipediaTermsToIndex = new HashMap<String,Integer>();
	articleMatrix = null;
    }

    /**
     * Parses the provided Wikipedia snapshot.
     *
     * @param wikipediaSnapshot a reader to the file containing a Wikipedia
     *        snapshot
     */
    public void processDocument(BufferedReader wikipediaSnapshot) 
	    throws IOException {

	// parse and clean the wiki snapshot, while recording the incoming and
	// outgoing link counts for each article
	WikiParseResult fileAndLinkCount = 
	    parseWikipediaSnapshot(wikipediaSnapshot);	
	
	// threshold off rarely linked articles and small articles
	Set<String> validArticles = 
	    thresholdArticles(fileAndLinkCount.parsedWikiSnapshot,
			      fileAndLinkCount.incomingLinkCounts);
	
	// create the article co-occurrence matrix 
	articleMatrix = Matrices.create(validArticles.size(), 
					validArticles.size(), false);

	// create the mapping from each article title its index in the matrix
	int index = 0;
	for (String articleTitle : validArticles) {
	    wikipediaTermsToIndex.put(articleTitle, Integer.valueOf(index));
	    ++index;
	}
	
	// use the remaining articles for the ESA set
	computeESA(fileAndLinkCount.parsedWikiSnapshot, validArticles);
    }

    /**
     * Returns {@code true} if the article with the provided name has Wikipedia
     * specific or non-text content (e.g. an image article) and should therefore
     * be excluded from the ESA processing.
     */
    private static boolean skipArticle(String articleName) {
	return articleName.startsWith("image:") ||
	    articleName.startsWith("wikipedia:") ||
	    articleName.startsWith("template:") ||
	    articleName.startsWith("category:") ||
	    (articleName.length() >= 3 && 
	     articleName.charAt(2) == ':') ||
	    articleName.contains("(disambiguation)");
    }
    
    /**
     * Parses the Wikipedia snapshot file into a temporary file containing all
     * the incoming and outgoing link information, along with a normalized
     * string representation that is free of Wiki markup and escaped HTML
     *
     * @param wikiSnapshot a {@code File} containing a Wikipedia snapshot
     *
     * @return a pair of temporary {@code File} containing the parsed Wikipedia
     *         articles and a list of incoming link counts for each article
     *
     * @throws IOException on any error
     */
    private WikiParseResult parseWikipediaSnapshot(BufferedReader wikiSnapshot) 
	throws IOException {
	       
	ArticleIterator articleIterator = new ArticleIterator(wikiSnapshot);
	    
	File parsedOutput = File.createTempFile("esa-parsed-snapshot", ".tmp");
	PrintWriter parsedOutputWriter = new PrintWriter(parsedOutput);

	// NOTE: we are able to use an IdentityHashMap only because we call
	// .intern() on all the article name strings to be used as keys
	Map<String,Integer> articleToIncomingLinkCount = 
	    new IdentityHashMap<String,Integer>(8000000);	    	    
	    
	int fileNum = 0;
	
	// next go through the raw articles and look for link counts
	while (articleIterator.hasNext()) {
	    fileNum++;
	    
	    WikiArticle doc = articleIterator.next();
	    
	    if (doc == null) {
		// rare, but we guard against it.
		ESA_LOGGER.warning("race condition in the document caching...");
		break;
	    }

	    String rawArticleName = doc.title;
	    // sanity check in case we didn't get a valid document
	    if (rawArticleName == null || doc.text == null) {
		ESA_LOGGER.warning("race condition in the document caching...");
		break;
	    }

	    // NOTE: we only call .intern() if the article is a topic
	    String articleName = StringUtils.unescapeHTML(rawArticleName).
		replaceAll("/"," ").toLowerCase().trim();
	    
	    // skip articles that are not text-based or are
	    // wikipedia-specific
	    if (skipArticle(articleName)) {
		ESA_LOGGER.fine(String.format("skipping Wikipedia-specific " +
					      "file %d: %s%n", 
					      fileNum, articleName));
		continue;
	    }

	    else if (doc.text.contains("#REDIRECT")) {
		ESA_LOGGER.fine(String.format("skipping redirect file, %d:" +
					      " %s%n", fileNum, articleName));
		continue;
	    }

	    else {
		// intern the article name since it will be around a for the
		// lifetime of the program
		articleName = articleName.intern();
	    }
			
	    ESA_LOGGER.fine(String.format("parsing file %d: %s ", 
					  fileNum, articleName));
		    
		    
	    String article = doc.text;
	    
	    int lastGoodIndex = 0;				
	    StringBuilder noHtml = new StringBuilder(article.length());
		    
	    // remove all html tags before we unescape the text itself and
	    // possibly introduce non-html < characters
	    int startOfTextTag = article.indexOf("<text");
	    int endOfStart  = article.indexOf(">", startOfTextTag);
	    
	    int closingTextTag = article.indexOf("</text");
	    String noHtmlStr = article.substring(endOfStart+1, closingTextTag);
		    
	    /*
	     * now get rid of {{wiki-stuff}} tags
	     */
	    
	    ESA_LOGGER.finer("Removing {{wiki}} tags");
		    
	    String phase2 = noHtmlStr;
	    
	    StringBuilder phase3sb = new StringBuilder(phase2.length());
	    lastGoodIndex = 0;
		    
	    if (phase2.indexOf("{{") >= 0) {
		// remove all html tags before we unescape the text itself and
		// possibly introduce non-html < characters
		for (int i = 0; (i = phase2.indexOf("{{", i)) >= 0; ) {
		    String s = phase2.substring(lastGoodIndex,i);

		    phase3sb.append(s);
		    lastGoodIndex = phase2.indexOf("}}", i) + 2;
		    i = lastGoodIndex;
		    // REMINDER: this is doing extra work, this loop should be
		    // rewritten
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
	    ESA_LOGGER.finer("replacing [[link]] with link name");

	    String phase3 = phase3sb.toString();
	    StringBuilder phase4sb = new StringBuilder(phase2.length());
	    lastGoodIndex = 0;
	    int outgoingLinks = 0;
	    int prevTotalIncomingLinks = 
		articleToIncomingLinkCount.size();
	    
	    // remove all html tags before we unescape the text itself
	    // and possibly introduce non-html < characters
	    for (int i = 0; (i = phase3.indexOf("[[", i)) > 0; ) {
		
		phase4sb.append(phase3.substring(lastGoodIndex,i));
		
		// grab the linked article name which is all text to the next
		// ]], or to the next | in the case where the article is given a
		// different name in the text, e.g.  [[article title|link text]]
		int j = phase3.indexOf("]]", i);
		int k = phase3.indexOf("|", i);
		int linkEnd = (k > 0) ? (j < 0) ? k : Math.min(j,k) : j;
		
		// transform the file name to get rid of any special
		// characters
		String linkedArticleRawName = 
		    phase3.substring(i+2,linkEnd);
		String linkedArticleTitle = linkedArticleRawName.
		    replaceAll("/", " ").toLowerCase();
		linkedArticleTitle = 
		    StringUtils.unescapeHTML(linkedArticleTitle);
		
		// don't include Image, foreign language or disambiguation links
		if (!skipArticle(linkedArticleTitle)) {
			    
		    // if the artile is actually a reasonable (e.g. non-image)
		    // article, then intern its string to save memory
		    linkedArticleTitle = linkedArticleTitle.intern();
		    
		    // print out the link name so that it gets included
		    // in the term list
		    phase4sb.append(linkedArticleTitle).append(" ");
		    
		    // increase the link counts accordingly
		    //
		    // NOTE: we have linkedArticleTitle is the canonical copy of
		    // the string (via intern()), and so this put() call works
		    // correctly, i.e. doesn't create duplicate keys
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
		
		// the "j < 0" condition is for malformed wiki pages where there
		// is no closing ]] for the link.
		if (phase3.indexOf("[[", i) < 0 || j < 0) {
		    phase4sb.append(phase3.substring(lastGoodIndex));
		    break;
		}
	    } // end [[ loop    
	    
	    String scrubbed = phase4sb.toString();
	    scrubbed = StringUtils.unescapeHTML(scrubbed).
		replaceAll("#REDIRECT","");
		    
	    
	    /*
	     * END PARSING STEPS
	     */

	    parsedOutputWriter.println(articleName + "|" + outgoingLinks
				       + "|" + scrubbed);
	    parsedOutputWriter.flush();
	    
	    int newIncomingLinks = articleToIncomingLinkCount.size() -
		prevTotalIncomingLinks;
	    Integer curIncoming = 
		articleToIncomingLinkCount.get(articleName);
	    
	    ESA_LOGGER.fine("link summary: " + outgoingLinks + 
			    " outgoing, (" + newIncomingLinks + 
			    " new docs); "
			    + ((curIncoming == null) ? "0" :
			       curIncoming.toString())
			    + " incoming");
	} // end file loop


	// Once all the articles have been processed, write the incoming link
	// count for each article

	parsedOutputWriter.close();
	return new WikiParseResult(parsedOutput, articleToIncomingLinkCount);
    }

    /**
     * Removes Wikipedia articles from the ESA processes if they fail to meet a
     * minimum word count or incoming and outgoing link count.
     *
     * @param parsedWiki the {@code File} output form {@link
     *        #parseWikipediaSnapshot(File)}
     *
     * @return the set of Wikipedia articles that meet the minimum
     *         qualifications
     */
    private Set<String> thresholdArticles(File parsedWiki, 
					  Map<String,Integer> incomingLinkCounts)
	throws IOException {

	ESA_LOGGER.info("Thresholding Articles");
	
	Set<String> validArticles = new HashSet<String>();
	
	// now read it back in and decide which of the term documents should
	// actually get included in the output
	int removed = 0 ;

	BufferedReader br =  new BufferedReader(new FileReader(parsedWiki));

	for (String line = null; (line = br.readLine()) != null; ) {
	    
	    String[] arr = line.split("\\|");
	    String articleName = arr[0].intern();
	    int outgoing = Integer.parseInt(arr[1]);

	    // If there weren't any incoming links, then the map will be
	    // null for the article
	    Integer incoming = incomingLinkCounts.get(articleName);
	    if (incoming == null)
		incoming = Integer.valueOf(0);			       

	    if ((incoming + outgoing) < 5) {
		ESA_LOGGER.fine("excluding article " + articleName + " for " +
				"too few incoming and outgoing links: " + 
				(incoming + outgoing));
		continue;
	    }
		
	    // NOTE: For some articles, the document will be rended empty by the
	    //       preprocessing steps.
	    String articleText = (arr.length < 2) ? "" : arr[2];

	    // this is a very rough estimate
	    int wordCount = articleText.split("\\s+").length;

	    if (wordCount < 100) {
		ESA_LOGGER.fine("excluding article " + articleName + " for " +
				"too few words: " +  wordCount);
		continue;		
	    }
	    
	    validArticles.add(articleName);
	}
	br.close();
	
	return validArticles;
    }

    /**
     * 
     *
     * @param parsedWikiSnapshot
     * @param validArticles
     */
    private void computeESA(File parsedWikiSnapshot, Set<String> validArticles)
	throws IOException {
	
	BufferedReader br =
	    new BufferedReader(new FileReader(parsedWikiSnapshot));

	for (String line = null; (line = br.readLine()) != null; ) {

	    
	    String[] arr = line.split("\\|");
	    String articleTitle = arr[0].intern();
	    // skip any articles that aren't a part of the valid set
	    if (!validArticles.contains(articleTitle)) {
		continue;
	    }
	    ESA_LOGGER.info("searching article " + articleTitle);
	    int articleIndex = 
		wikipediaTermsToIndex.get(articleTitle).intValue();
	    String articleText = arr[2];

	    // Search the text for the number of occurrences of each valid
	    // article title.
	    for (String valid : validArticles) {
		int count = count(articleText, valid);
		articleMatrix.set(wikipediaTermsToIndex.get(valid).intValue(),
				  articleIndex, count);
		ESA_LOGGER.log(Level.FINE, "{0} contained {1} {2} times",
			       new Object[] { articleTitle, valid, 
					      Integer.valueOf(count)});
	    }
	}
	br.close();	
    }

    /**
     * Returns how many times {@code toCount} occurs in the provided {@code
     * text} String.
     */
    private static int count(String text, String toCount) {
	int count = 0;
	for (int i = -1; (i = text.indexOf(toCount, i + 1)) >= 0; ++count)
	    ;
	return count;
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {

    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
      return ESA_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String word) {
	Integer index = wikipediaTermsToIndex.get(word);
	return (index == null) ? null : articleMatrix.getRow(index.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return Collections.unmodifiableSet(wikipediaTermsToIndex.keySet());
    }    

    /**
     * A wrapper class for returning a file of the parsed Wikipedia snapshot as
     * well as the number of incoming links for each article.
     *
     * @see #parseWikipediaSnapshot(BufferedReader)
     */
    private static class WikiParseResult {

	public final File parsedWikiSnapshot;
	public final Map<String,Integer> incomingLinkCounts;

	public WikiParseResult(File parsedWikiSnapshot,
			       Map<String,Integer> incomingLinkCounts) {
	    this.parsedWikiSnapshot = parsedWikiSnapshot;
	    this.incomingLinkCounts = incomingLinkCounts;
	}
    }

    /**
     *
     */
    private final class SemanticVector {
	private final Map<Integer,Integer> sparseVector;

	public SemanticVector() {
	    sparseVector = new HashMap<Integer,Integer>();
	}

	public void increment(String articleName) {
	    Integer index = wikipediaTermsToIndex.get(articleName);
	    Integer count = sparseVector.get(index);
	    sparseVector.put(index, (count == null) ? 1 : count + 1);
	}

	public int[] toIntArray() {
	    int size = wikipediaTermsToIndex.size();
	    int[] full = new int[size];
	    for (int i = 0; i < size; ++i) {
		Integer count = sparseVector.get(Integer.valueOf(i));
		if (count != null) {
		    full[i] = count.intValue();
		}
	    }
	    return full;
	}
    }
   
}
