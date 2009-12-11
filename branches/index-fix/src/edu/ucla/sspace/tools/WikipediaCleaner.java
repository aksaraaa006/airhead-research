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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.util.Duple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A simple parser for wikipedia documents.
 *
 * @author David Jurgens
 * @author Keith Stevens
 */
public class WikipediaCleaner {

    /**
     * A pointer to a temporary file which will contain each lines of the
     * pattern "article Title | outgoing link count | article content".  This
     * file will be deleted when the cleaner finishes.
     */
    private File tempFile;

    /**
     * A pointer to a persistent file which lists all of the article titles
     * found, for use in a {@link CompoundWordIterator}.
     */
    private File articleTitleFile;

    /**
     * A {@code PrintWriter} for writing to {@code tempFile}.
     */
    private PrintWriter tmpOutput;

    /**
     * A {@code PrintWriter} for writing the final article content output.
     */
    private PrintWriter output;

    /**
     * A {@code PrintWriter} for writing to {@code articleTitleFile}.
     */
    private PrintWriter articleOutput;

    /**
     * A {@code Map} tracking how many incoming links each wikipedia article
     * contains.
     */
    //private Map<String,Integer> articleToIncomingLinkCount;

    /**
     * The minimum number of incoming links each article should have.  Articles
     * with fewer links are rejected.
     */
    private int minIncomingCount;

    /**
     * The minimum number of outgoing links each article should have.  Articles
     * with fewer links are rejected.
     */
    private int minOutgoingCount;

    /**
     * Create a new {@code WikipediaCleaner} which will read articles from
     * {@code outputFileName}, with the given thresholds for link requirements.
     */
    public WikipediaCleaner(String outputFileName,
                            int minIncoming,
                            int minOutgoing) {
        String outputFile = outputFileName;

        try {
            // Create the file pointers needed.
            //tempFile = File.createTempFile(outputFile, "tmp");
            //tempFile.deleteOnExit();
            //articleTitleFile = File.createTempFile(outputFile, "articles");

            // Create the PrintWriters needed.
            tmpOutput = new PrintWriter(outputFileName);
            //output = new PrintWriter(outputFile);
            //articleOutput = new PrintWriter(articleTitleFile);

            // Create the map to track incoming links.  NOTE: For this map to
            // work, .intern() must be called on all strings used as keys.
            //articleToIncomingLinkCount =
            //    new IdentityHashMap<String, Integer>(8000000);                
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Process the content of the given {@code WikiDoc}.  Html tags, wiki links,
     * and wiki markups will all be removed.  The cleaned content, along with
     * meta data for the article will be stored temporarily in {@code
     * tempFile}.  This will also track the number of incoming links for each
     * article and the set of article titles encountered.  False is returned
     * when doc is in an invalid state, otherwise True is returned.
     *
     * @param doc The {@code WikiDoc} to process.
     *
     * @return True if document processing should continue, false if the cleaner
     *         has entered an invalid state.
     */
    public boolean processDocument(WikiDoc doc) {
        String rawArticleName = doc.name;
        // sanity check in case we didn't get an invalid document
        if (rawArticleName == null || doc.text == null) {
            System.out.println("race condition in the document " +
                               "caching; continuing...");
            return false;
        }

        String articleName =
            unescapeHTML(rawArticleName,0).replaceAll("/"," ");
        articleName = articleName.toLowerCase().trim();

        // skip articles that are not text-based or are
        // wikipedia-specific
        if (!shouldProcessArticle(articleName)) {
            System.out.printf("skipping file %s\n", articleName);
            return true;
        } else if (doc.text.contains("#REDIRECT")) {
            System.out.printf("skipping redirect %s\n", articleName);
            return true;
        }
        System.out.println("Processing article: "  + articleName +
                           " of size: " + doc.text.length());

        // intern the article name since it will be around a while
        //articleName = articleName.intern();
    
        // Clean article content.
        String rawArticleText = extractArticle(doc.text);
        rawArticleText = removeWikiStuff(rawArticleText);

            String wikiFreeText = removeWikiStuff(rawArticleText);
        Duple<String, Integer> tagFreeAndLinkCount = 
            replaceAndCountLinks(wikiFreeText);
        String scrubbed = removeRedirect(tagFreeAndLinkCount.x);
        tmpOutput.println(articleName //+ "|" + tagFreeAndLinkCount.y.intValue() 
                          + "|" + scrubbed);
        tmpOutput.flush();

        // Print article content and meta data to the temporary file.

        return true;
    }

    /**
     * Extract the article content from {@code text} markup tags.
     *
     * @param text Raw article text.
     *
     * @return Article text extracted from {@code text} tags.
     */
    public static String extractArticle(String text) {
        String article = text;;
        // remove all html tags before we unescape the text itself
        // and possibly introduce non-html < characters
        int startOfTextTag = article.indexOf("<text");
        int endOfStart  = article.indexOf(">", startOfTextTag);
        
        int closingTextTag = article.indexOf("</text");
        return article.substring(endOfStart+1, closingTextTag);
    }

    /**
     * Remove wiki markup tags of the form "{{ stuff }}".
     *
     * @param text The article text to clean.
     *
     * @return Article text without wiki tags.
     */
    public static String removeWikiStuff(String text) {
        if (text.indexOf("{{") >= 0) {
            StringBuilder tagCleanBuilder = new StringBuilder(text.length());
            int lastGoodIndex = 0;
            // remove all wiki tags before we unescape the text
            // itself and possibly introduce non-html < characters
            for (int i = 0; (i = text.indexOf("{{", i)) >= 0; ) {
                String s = text.substring(lastGoodIndex,i);
                System.out.println(i);
                System.out.println("{{:" + text.indexOf("}}", i));
                tagCleanBuilder.append(s);
                lastGoodIndex = text.indexOf("}}", i) + 2;
                
                // Some articles have no ending }}, indicating that {{ was
                // somehow part of the text, so cut out early.
                if (lastGoodIndex == 1) {
                    tagCleanBuilder.append("OMGFWDF").append(text.substring(i));
                    break;
                }

                i = lastGoodIndex;
            }
            if (lastGoodIndex != 1)
                tagCleanBuilder.append(text.substring(lastGoodIndex));
            return tagCleanBuilder.toString();
        }

        // If there are no wiki tags, just return the given text.
        return text;
    }

    /**
     * Replace [[link]] tags with link name and track what articles this article
     * links to.
     *
     * @param text The article text to clean and process link structure of.
     *
     * @return A Duple containing the cleaned text and the outgoing link count.
     */
    public Duple<String, Integer> replaceAndCountLinks(String text) {
        StringBuilder linkCleanBuilder = new StringBuilder(text.length());
        int lastGoodIndex = 0;
        int outgoingLinks = 0;
        //int prevTotalIncomingLinks = articleToIncomingLinkCount.size();
        for (int i = 0; (i = text.indexOf("[[", i)) > 0; ) {
            linkCleanBuilder.append(text.substring(lastGoodIndex,i));
            
            // grab the linked article name which is all text to the
            // next ]], or to the next | in the case where the
            // article is given a different name in the text, e.g.
            // [[article title|link text]]
            int j = text.indexOf("]]", i);
            int k = text.indexOf("|", i);
            int linkEnd = (k > 0) ? (j < 0) ? k : Math.min(j,k) : j;
            
            // transform the file name to get rid of any special
            // characters
            String linkedArticleRawName = text.substring(i+2,linkEnd);
            String linkedArticleTitle =
                linkedArticleRawName.replaceAll("/", " ");
            linkedArticleTitle =
                unescapeHTML(linkedArticleTitle.toLowerCase(), 0);
            
            /*
            // don't include Image, foreign language or
            // disambiguation links
            if (shouldProcessArticle(linkedArticleTitle)) {
                // (e.g. non-image) article, then intern its string
                // to save memory
                linkedArticleTitle = linkedArticleTitle.intern();

                // print out the link name so that it gets included
                // in the term list
                linkCleanBuilder.append(linkedArticleTitle).append(" ");

                // increase the link counts accordingly
                //
                // NOTE: we have linkedArticleTitle is the canonical
                // copy of the string (via intern()), and so this
                // put() call works correctly, i.e. doesn't create
                // duplicate keys
                Integer incomingLinks =
                    articleToIncomingLinkCount.get(linkedArticleTitle);

                articleToIncomingLinkCount.put(
                        linkedArticleTitle, (incomingLinks == null)
                        ? Integer.valueOf(1)
                        : Integer.valueOf(1 + incomingLinks));
                ++outgoingLinks;
            }
            */

            lastGoodIndex = text.indexOf("]]", i) + 2;
            i = lastGoodIndex;
            
            // the "j < 0" condition is for malformed wiki pages
            // where there is no closing ]] for the link.
            if (text.indexOf("[[", i) < 0 || j < 0) {
                linkCleanBuilder.append(text.substring(lastGoodIndex));
                break;
            }
        }

        return new Duple<String, Integer>(linkCleanBuilder.toString(),
                                          outgoingLinks);
    }

    /**
     * Unescape any html and remove any redirect tags.
     *
     * @param text The article text to clean.
     *
     * @return The cleaned article content.
     */
    public static String removeRedirect(String text) {
        return unescapeHTML(text, 0).replaceAll("#REDIRECT", "");
    }

    /**
     * Finalize all processing of the corpus.  For {@code WikipediaCleaner},
     * this will read in cleaned documents from {@code tempOutput} and simply
     * reject any articles which do not meet the link requirements.  All valid
     * articles will be written to {@code output} in the form "article title |
     * incoming link count | outgoing link count | article content".
     */
    public void finalizeCorpus() {
        // Read through all the stored cleaned documents, and reject any
        // which have less than 5 incoming links or outgoing links.
        // Articles which are saved will have the entire text written to a
        // single line in the format "title | incoming count | outgoing
        // count | article content".
        int removed = 0 ;
        try {
            BufferedReader br = new BufferedReader(new FileReader(tempFile));
            for (String line = null; (line = br.readLine()) != null; ) {
                String[] titleCountDoc = line.split("\\|");
                String title = titleCountDoc[0].intern();

                /*
                // Reject articles with fewer than the minimum outgoing link
                // count.
                int outgoing = Integer.parseInt(titleCountDoc[1]);
                if (outgoing < minOutgoingCount)
                    continue;
                */
                
                // Reject articles with no content.
                if (titleCountDoc.length != 3)
                    continue;
                String doc = titleCountDoc[2];

                /*
                // Reject articles with fewer than the minimum incoming link
                // count.
                Integer incoming = articleToIncomingLinkCount.get(title);
                if (incoming == null ||
                    incoming.intValue() < minIncomingCount)
                    continue;
                */

                //articleOutput.println(title);
                //articleOutput.flush();

                output.println(title + // "|" + incoming + "|" + 
                              // outgoing + 
                               "|" + doc);
                output.flush();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        articleOutput.close();
        output.close();
    }

    public static void main(String[] args) {
        ArgOptions options = new ArgOptions();
        options.addOption('w', "wikiDump", "The wikipedia snapshot to process",
                          true, "FILE", "Required");
        options.addOption('v', "validTerms", "The set of valid terms",
                          true, "FILE", "Required");
        options.parseOptions(args);

        if (options.numPositionalArgs() != 1 ||
            !options.hasOption('v') ||
            !options.hasOption('w')) {
            System.out.println("usage java [OPTIONS] <output-file>\n"+ 
                               options.prettyPrint());
            return;
        }

        int minIncomingCount = 5;
        int minOutgoingCount = 5;

        DocumentBufferedQueue docQueue = null;
        try {
            docQueue = new DocumentBufferedQueue(options.getStringOption('w'));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        String outFileName = options.getPositionalArg(0);
        WikipediaCleaner cleaner = new WikipediaCleaner(
                outFileName, minIncomingCount, minOutgoingCount);

        while (docQueue.hasNext()) {
            WikiDoc doc = null;
            try {
                doc = docQueue.next();
            } catch (InterruptedException ie) {
            }
        
            if (doc == null) {
                System.out.println("race condition in the document " +
                           "caching; continuing...");
                break;
            }

            if (!cleaner.processDocument(doc))
                break;
        }

        //cleaner.finalizeCorpus();
    }

    /**
     * A queue representing a series of wikipedia documents which have been
     * read.
     */
    private static class DocumentBufferedQueue {
        
        /**
         * The number of documents which will be cached in this Queue.
         */
        private static final int DOCS_TO_CACHE = 100;

        /**
         * The lenght of an html title line.
         */
        private static final int TITLE_HTML_LENGTH = "    <title>".length();

        /**
         * A {@code BufferedReader} for an opened wikipedia document.
         */
        private final BufferedReader wikiReader;

        /**
         * A thread safe queue of wikipedia documents which have been read into
         * memory.
         */
        private final BlockingQueue<WikiDoc> cachedDocs;

        /**
         * A flag signalling that {@code wikiReader} is open and ready to be
         * read from.
         */
        private final AtomicBoolean isReaderOpen;
        
        /**
         * Create a new {@code DocumentBufferedQueue} from a wikipedia file
         * name.
         */
        public DocumentBufferedQueue(String wikipediaFile) throws IOException {
            wikiReader = new BufferedReader(new FileReader(wikipediaFile));
            cachedDocs = new LinkedBlockingQueue<WikiDoc>();
            isReaderOpen = new AtomicBoolean(true);

            for (int i = 0; i < DOCS_TO_CACHE; ++i) {
                WikiDoc d = cacheDoc();
                if (d != null)
                    cachedDocs.offer(d);
            }
        }

        /**
         * Create a new {@code WikiDoc} from the the content provided by {@code
         * wikiReader}.
         */
        private synchronized WikiDoc cacheDoc() throws IOException {
            StringBuilder sb = new StringBuilder();
            String articleTitle = null;

            for (String line = null; (line = wikiReader.readLine()) != null;) {
                // Ignore wikipedia documents which are media pages.
                if (line.startsWith("</mediawiki>")) {
                    // end of input
                    isReaderOpen.set(false);
                } else if (line.startsWith("  <page>")) {
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

                        // read in the rest of the page until we see the end tag
                        while ((line = wikiReader.readLine()) != null && 
                               !line.startsWith("  </page>")) {
                            sb.append(line);
                        }

                        return new WikiDoc(articleTitle, sb.toString());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        break;
                    }
                }
            }
            return null;
        }

        /**
         * Check that the queue has more documents to be read.
         */
        public boolean hasNext() {
            return cachedDocs.size() > 0 || isReaderOpen.get();
        }

        /**
         * Return the next available {@code WikiDoc} stored in the queue.  If
         * there are still documents which need to be put on the queue, read one
         * and add it to {@code cachedDocs}.
         */
        public WikiDoc next() throws InterruptedException {
            new Thread() {
                public void run() {
                    try {
                        WikiDoc d = cacheDoc();
                        if (d != null)
                            cachedDocs.offer(d);            
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }.start();
            // Don't block.  Wait up to 10 minutes (in case of GC) to poll. 
            return cachedDocs.poll(60 * 10 * 1000L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * A simple struct storing a wikipedia article.
     */
    private static class WikiDoc {

        /**
         * The article's title.
         */
        public final String name;

        /**
         * The article's content.
         */
        public final String text;

        /**
         * Create a new {@code WikiDoc} with the given name and content.
         */
        public WikiDoc(String name, String text) {
            this.name = name;
            this.text = text;
        }
    }

    /**
     * A translation map for html encoding of several special characters.
     */
    private static final Map<String,String> HTML_ENTITIES;
    static {
        HTML_ENTITIES = new HashMap<String,String>();
        HTML_ENTITIES.put("&lt;","<")    ; HTML_ENTITIES.put("&gt;",">");
        HTML_ENTITIES.put("&amp;","&")   ; HTML_ENTITIES.put("&quot;","\"");
        HTML_ENTITIES.put("&agrave;","à"); HTML_ENTITIES.put("&Agrave;","À");
        HTML_ENTITIES.put("&acirc;","â") ; HTML_ENTITIES.put("&auml;","ä");
        HTML_ENTITIES.put("&Auml;","Ä")  ; HTML_ENTITIES.put("&Acirc;","Â");
        HTML_ENTITIES.put("&aring;","å") ; HTML_ENTITIES.put("&Aring;","Å");
        HTML_ENTITIES.put("&aelig;","æ") ; HTML_ENTITIES.put("&AElig;","Æ" );
        HTML_ENTITIES.put("&ccedil;","ç"); HTML_ENTITIES.put("&Ccedil;","Ç");
        HTML_ENTITIES.put("&eacute;","é"); HTML_ENTITIES.put("&Eacute;","É" );
        HTML_ENTITIES.put("&egrave;","è"); HTML_ENTITIES.put("&Egrave;","È");
        HTML_ENTITIES.put("&ecirc;","ê") ; HTML_ENTITIES.put("&Ecirc;","Ê");
        HTML_ENTITIES.put("&euml;","ë")  ; HTML_ENTITIES.put("&Euml;","Ë");
        HTML_ENTITIES.put("&iuml;","ï")  ; HTML_ENTITIES.put("&Iuml;","Ï");
        HTML_ENTITIES.put("&ocirc;","ô") ; HTML_ENTITIES.put("&Ocirc;","Ô");
        HTML_ENTITIES.put("&ouml;","ö")  ; HTML_ENTITIES.put("&Ouml;","Ö");
        HTML_ENTITIES.put("&oslash;","ø") ; HTML_ENTITIES.put("&Oslash;","Ø");
        HTML_ENTITIES.put("&szlig;","ß") ; HTML_ENTITIES.put("&ugrave;","ù");
        HTML_ENTITIES.put("&Ugrave;","Ù"); HTML_ENTITIES.put("&ucirc;","û");
        HTML_ENTITIES.put("&Ucirc;","Û") ; HTML_ENTITIES.put("&uuml;","ü");
        HTML_ENTITIES.put("&Uuml;","Ü")  ; HTML_ENTITIES.put("&nbsp;"," ");
        HTML_ENTITIES.put("&copy;","\u00a9");
        HTML_ENTITIES.put("&reg;","\u00ae");
        HTML_ENTITIES.put("&euro;","\u20a0");
    }

    static final String unescapeHTML(String source, int start){
        int i,j;
    
        i = source.indexOf("&", start);
        if (i > -1) {
            j = source.indexOf(";" ,i);
            if (j > i) {
                    String entityToLookFor = source.substring(i , j + 1);
                    String value = HTML_ENTITIES.get(entityToLookFor);
                if (value != null) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(source.substring(0 , i));
                    sb.append(value);
                    sb.append(source.substring(j + 1));
                    source = sb.toString();
                    return unescapeHTML(source, start + 1); // recursive call
                }
            }
        }
        return source;
    }

    /**
     * Check if the artile is actually reasonable.
     */
    public static boolean shouldProcessArticle(String linkedArticleTitle) {
        return !(linkedArticleTitle.startsWith("image:") ||
                 linkedArticleTitle.startsWith("wikipedia:") ||
                 linkedArticleTitle.startsWith("template:") ||
                 linkedArticleTitle.startsWith("category:") ||
                (linkedArticleTitle.length() >= 3 && 
                 linkedArticleTitle.charAt(2) == ':') ||
                 linkedArticleTitle.contains("(disambiguation)"));
    }
}
