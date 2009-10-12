/*
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
    private WikiParseResult parseWikipediaSnapshot(BufferedReader wikiSnapshot) 
  throws IOException {

         
  ArticleIterator articleIterator = new ArticleIterator(wikiSnapshot);
      
  File parsedOutput = File.createTempFile("esa-parsed-snapshot", ".tmp");
  parsedOutput.deleteOnExit();
  PrintWriter parsedOutputWriter = new PrintWriter(parsedOutput);

  GrowableArrayList<Integer> articleToIncomingLinkCount = 
      new GrowableArrayList<Integer>(50000000);

  // this is different than the articleTitleIndex since it includes
  // skipped documents
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
    //articleName = articleName.intern();
    
    // Add the article title to the index mapping
    int index = getArticleIndex(articleName);
      }
      
      ESA_LOGGER.info(String.format("parsing file %d: %s ", 
            fileNum, articleName));
        
        
      String article = doc.text;
      
      int lastGoodIndex = 0;        
      StringBuilder noHtml = new StringBuilder(article.length());
        
      // remove all html tags before we unescape the text itself and
      // possibly introduce non-html < characters
      int startOfTextTag = article.indexOf("<text");
      int endOfStart  = article.indexOf(">", startOfTextTag);
      
      int closingTextTag = article.indexOf("</text");
      // protect against malformatted XML
      if (closingTextTag < endOfStart+1) {
    continue;
      }
      String noHtmlStr = article.substring(endOfStart+1, closingTextTag);
        
      /*
       * now get rid of {{wiki-stuff}} tags
      
      ESA_LOGGER.finer("Removing {{wiki}} tags");
        
      String phase2 = noHtmlStr;
      
      StringBuilder phase3sb = null; 
      lastGoodIndex = 0;
        
      String phase3 = null;
      
      if (phase2.indexOf("{{") >= 0) {

    phase3sb = new StringBuilder(phase2.length());

    // remove all html tags before we unescape the text itself and
    // possibly introduce non-html < characters
    for (int i = -1; (i = phase2.indexOf("{{", i + 1)) >= 0; ) {

        String s = phase2.substring(lastGoodIndex,i);
        // append all the text from the last }} up to this {{
        phase3sb.append(s);

        // move the closing }}
        int closeBraces = phase2.indexOf("}}", i);

        // protect against illegally formatted Wiki 
        if (closeBraces < 0) {
      // if there weren't actually any closing braces, just
      // append the string and end it
      phase3sb.append(phase2.substring(i));
      break;
        }

        // mark the next position after the }}
        lastGoodIndex = closeBraces + 2;

        // update i to start the search after the }}
        i = lastGoodIndex + 1;

        // REMINDER: this is doing extra work, this loop should be
        // rewritten
        if (phase2.indexOf("{{", i) == -1) {
      phase3sb.append(phase2.
          substring(lastGoodIndex));
      break;
        }
    } 

    // once the wiki-markup has been removed, transfer the string
    // contents to the next phase
    phase3 = phase3sb.toString();
      }

      // in case there is no wiki mark-up
      else {
    // don't bother making a copy with the string buffer and instead
    // just change the references
    phase3 = phase2;
      }
        
        
      /*
       * Replace [[link]] tags with link name.
       *
       * Also update link counts
      ESA_LOGGER.finer("replacing [[link]] with link name");

      
      StringBuilder phase4sb = new StringBuilder(phase3.length());
      lastGoodIndex = 0;
      int outgoingLinks = 0;
      int prevTotalIncomingLinks = 
    articleToIncomingLinkCount.size();
      
      // remove all html tags before we unescape the text itself
      // and possibly introduce non-html < characters
      for (int i = 0; (i = phase3.indexOf("[[", i + 1)) > 0; ) {
    
    phase4sb.append(phase3.substring(lastGoodIndex,i));
    
    // grab the linked article name which is all text to the next
    // ]], or to the next | in the case where the article is given a
    // different name in the text, e.g.  [[article title|link text]]
    int j = phase3.indexOf("]]", i);
    int k = phase3.indexOf("|", i);

    // Guard against illegally formatted wiki links
    if (j < 0 && k < 0) {
        phase4sb.append(phase3.substring(i));
        break;
    }

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
        linkedArticleTitle = linkedArticleTitle; //.intern();
        
        // print out the link name so that it gets included
        // in the term list
        phase4sb.append(linkedArticleTitle).append(" ");
        
        // increase the link counts accordingly
        int linkedIndex = getArticleIndex(linkedArticleTitle);
        Integer val = articleToIncomingLinkCount.get(linkedIndex);
        articleToIncomingLinkCount.set(linkedIndex, (val == null)
          ? Integer.valueOf(1) 
          : Integer.valueOf(1 + val.intValue()));
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

      // this is a very rough estimate
      int wordCount = scrubbed.split("\\s+").length;

      if (wordCount >  100) {
    
    parsedOutputWriter.println(articleName + "|" + outgoingLinks
             + "|" + scrubbed);
    parsedOutputWriter.flush();
    
    if (ESA_LOGGER.isLoggable(Level.FINE)) {
        int newIncomingLinks = articleToIncomingLinkCount.size() -
      prevTotalIncomingLinks;
        Integer curIncoming = articleToIncomingLinkCount.get(
      getArticleIndex(articleName));
        
        ESA_LOGGER.fine("link summary: " + outgoingLinks + 
            " outgoing, (" + newIncomingLinks + 
            " new docs); "
            + ((curIncoming == null) ? "0" :
               curIncoming.toString())
            + " incoming");
    }
      }
  } // end file loop


  // Once all the articles have been processed, write the incoming link
  // count for each article
  parsedOutputWriter.close();

  // convert the link counts to an array
  int size = articleToIncomingLinkCount.size();
  int[] incomingLinkCountArray = new int[size];
  for (int i = 0; i < size; ++i) {
      Integer count = articleToIncomingLinkCount.get(i);
      incomingLinkCountArray[i] = (count == null) ? 0 : count.intValue();
  }

  return new WikiParseResult(parsedOutput, incomingLinkCountArray);
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
    private BitSet thresholdArticles(File parsedWiki, 
             int[] incomingLinkCounts)
  throws IOException {

  ESA_LOGGER.info("Thresholding Articles");
  //Set<String> validArticles = new LinkedHashSet<String>();
  BitSet validArticles = new BitSet(articleNameToIndex.size());
  
  // now read it back in and decide which of the term documents should
  // actually get included in the output
  int removed = 0 ;

  BufferedReader br =  new BufferedReader(new FileReader(parsedWiki));

  for (String line = null; (line = br.readLine()) != null; ) {
      
      String[] arr = line.split("\\|");
      String articleName = arr[0]; //.intern();
      int outgoing = Integer.parseInt(arr[1]);

      // If there weren't any incoming links, then the map will be
      // null for the article
      // Integer incoming = incomingLinkCounts.get(articleName);
      int index = articleNameToIndex.get(articleName);
      int incoming = incomingLinkCounts[index];
      // if (incoming == null)
      //    incoming = Integer.valueOf(0);             

      if ((incoming + outgoing) < 5) {
    ESA_LOGGER.fine("excluding article " + articleName + " for " +
        "too few incoming and outgoing links: " + 
        (incoming + outgoing));
    ++removed;
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
    ++removed;
    continue;    
      }
      
      //validArticles.add(articleName);
      validArticles.set(getArticleIndex(articleName));
  }
  br.close();

  ESA_LOGGER.info("retained " + validArticles.cardinality() + " articles;"
      + " removed " + removed + " articles");
  
  return validArticles;
    }

*/
