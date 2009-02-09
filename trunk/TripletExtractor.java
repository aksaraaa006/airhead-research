import java.util.*;
import java.io.*;

public class TripletExtractor {

    private static final boolean DEBUG = false;

    private static final boolean SHOW_ERRORS = false;

    public static void main(String[] args) {
	try {
	    if (args.length < 1) {
		System.out.println("usage: java <tagged file>");
		return;
	    }
	    new TripletExtractor().parseFile(args[0]);
	} catch (Throwable t) {

	}
    }

    /**
     * What word we are currently looking at in the sentence
     */
    private int curWord;
    
    // the SVO words.  These may be compound nouns and verbs
    private String subject = null;
    private String verb = null;
    private String object = null;
    int phraseStart = 0;
    int phraseEnd = 0;
    

    public TripletExtractor() {
	curWord = 0;
    }

    public void parseFile(String fileName) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(fileName));

	for (String line = null; (line = br.readLine()) != null; ) {
	    try {
		parseSentence(line);
	    }
	    catch (Throwable t) {
		t.printStackTrace();
	    }
	}
    }

    public void parseSentence(String line) {
	// reset the sentence counters
	curWord = 0;
	phraseStart = -1;
	phraseEnd = -1;
	subject = null;
	verb = null;
	object = null;

	String[] taggedWords = line.split(" ");
	// skip blank lines and short sentences
	if (taggedWords.length < 3)
	    return;
	subject = extractNoun(taggedWords, true);
	verb = extractVerb(taggedWords);
	object = extractNoun(taggedWords, false);

	if (subject != null && verb != null && object != null)
	    System.out.println(subject + "|" + verb + "|" + object
			       + "|" + makeString(taggedWords, 
						  phraseStart, 
						  phraseEnd));

    }

    public String extractNoun(String[] taggedWords, boolean isSubject) {

	for (; curWord < taggedWords.length; ++curWord) {
	    String[] wordAndPOS = taggedWords[curWord].split("_");
	    if (!isValidPOS(wordAndPOS))
		return null;

	    String word = wordAndPOS[0];
	    String partOfSpeech = wordAndPOS[1];
		    
	    // check for prepositions, as we might end up with a noun
	    // embedded in a prepositional phrase
	    if (isPreposition(partOfSpeech))
		skipPrepositionalPhrase(taggedWords);
		    
	    // try to remove noun prhases like "the job [that was horrible]
	    // ..."
	    else if (isStartOfNounPhrase(partOfSpeech))
		skipNounPhrase(taggedWords);

	    else if (isStartOfInfinitivePhrase(partOfSpeech)) 
		skipInfinitivePhrase(taggedWords);

	    else if (isNoun(partOfSpeech)) {
		    
		// word is a noun.  check forward for two cases:
		// 1) compound nouns - use all the nounds
		// 2) possessives - use only the words after the 's

		String noun = word;

		// check for "kind of", as we skip this phrase
		if (noun.equals("kind") ||
		    noun.equals("kinds")) {
		    String[] next = taggedWords[curWord + 1].split("_");
		    if (next.length == 2 &&
			next[0].equals("of")) {
			// skip over the "of" so we pick up the embedded noun,
			// which is actually useful
			++curWord;
			continue;
		    }
		}

		if (isSubject)
		    phraseStart = curWord;
		    
		for (curWord += 1; curWord < taggedWords.length; ++curWord) {
		    String[] nextWordAndPOS = taggedWords[curWord].split("_");
		    if (!isValidPOS(nextWordAndPOS))
			return null;
			
		    String nextPOS = nextWordAndPOS[1];
		    // if the next word indicates that the first word had a
		    // possessive ending, then select the next noun phrase as
		    // the actual subject
		    if (isPossessive(nextPOS)) {
			// recursively call the method to avoid code duplication
			++curWord;
			return extractNoun(taggedWords, isSubject);
		    }
		    // if the next word is also a noun, append it to the
		    // previous noun, which will indicate it is a
		    // compound noun for later stages of processing
		    else if (isNoun(nextPOS)) {
			noun += " " + nextWordAndPOS[0];
		    }
		    else 
			break;
			
		}
		if (!isSubject)
		    phraseEnd = curWord - 1;
		    
		return noun;
	    }
	} 
	return null;
    }
    
    public String extractVerb(String[] taggedWords) {
	    
	// now search foward for the verb
	for (; curWord < taggedWords.length; ++curWord) {
	    String[] wordAndPOS = taggedWords[curWord].split("_");
	    if (!isValidPOS(wordAndPOS))
		return null;
		
	    String word = wordAndPOS[0];
	    String partOfSpeech = wordAndPOS[1];

	    // check for prepositions, as we might end up with a noun
	    // embedded in a prepositional phrase
	    if (isPreposition(partOfSpeech))
		skipPrepositionalPhrase(taggedWords);
		    
	    // try to remove noun prhases like "the job [that was horrible]
	    // ..."
	    else if (isStartOfNounPhrase(partOfSpeech))
		skipNounPhrase(taggedWords);
	    
	    else if (isStartOfInfinitivePhrase(partOfSpeech)) 
		skipInfinitivePhrase(taggedWords);
	    
	    if (isVerb(partOfSpeech)) {
		String verb = word;
			
		// check a few words ahead to see if this was a compound
		// verb, e.g. was held, had checked, etc.  Also, we
		// should ideally only allow adverbs and the like
		// between the two
		for (curWord += 1; curWord < taggedWords.length; ++curWord) {
		    String[] nextWordAndPOS = taggedWords[curWord].split("_");
		    if (!isValidPOS(nextWordAndPOS))
			return null;
			    
		    String nextPOS = nextWordAndPOS[1];
		    // skip over adverbs
		    if (isAdverb(nextPOS)) 
			continue;
		    else if (isVerb(nextPOS)) {
			// NOTE: could there be a compound verb with
			// more than two words?
			verb += " " + nextWordAndPOS[0]; 
		    }

		    break;			    
		}

		return verb;
	    }
	}
	return null;
    }



    private void skipPrepositionalPhrase(String[] taggedWords) {
	    
	int prepStart = curWord, prepEnd = -1;
	
	// skip past end of prepositional phrase
	boolean nounStarted = false;
	for (curWord += 1; curWord < taggedWords.length; ++curWord) {
	    String[] nextWordAndPOS = taggedWords[curWord].split("_");
	    if (!isValidPOS(nextWordAndPOS))
		return;
	    
	    String nextPOS = nextWordAndPOS[1];
	    if (isNoun(nextPOS)) {
		// mark that we've seen a noun, but keep searching in case the
		// prepositional phrase ends in a compound noun
		nounStarted = true;
		continue;  
	    }
	    // if we have yet to see a noun, then keep scanning forward until we
	    // find one, at which point nounStarted = true
	    else if (!nounStarted)
		continue;
	    // this case happens when we've already seen at least one noun and
	    // then see a non-noun POS.
	    else {
		prepEnd = curWord-1;
		curWord--;
		break;
	    }
	}
		
	if (prepStart >= 0 && prepEnd >= 0)
	    debug("skipped prepositional phrase: " +
		  makeString(taggedWords, prepStart, prepEnd));
    }

    private void skipInfinitivePhrase(String[] taggedWords) {
	debug("skipping infinive phrase");
	int infStart = curWord, infEnd = -1;
	
	inf_search:
	for (; curWord < taggedWords.length; ++curWord) {

	    // read until after the next verb
	    String[] wordAndPOS = taggedWords[curWord].split("_");
	    if (!isValidPOS(wordAndPOS))
		return;
	    
	    String word = wordAndPOS[0];
	    String partOfSpeech = wordAndPOS[1];

	    if (isVerb(partOfSpeech)) {
		
		// search forward for compound verbs
		compound:
		for (curWord += 1; curWord < taggedWords.length; ++curWord) {
		    String[] nextWordAndPOS = taggedWords[curWord].split("_");
		    if (!isValidPOS(nextWordAndPOS))
			return;
			    
		    String nextPOS = nextWordAndPOS[1];
		    // skip over adverbs and adverbs
		    if (isAdverb(nextPOS) || 
			isVerb(nextPOS)) {
			continue compound;
		    }

		    infEnd = curWord - 1;
		    break inf_search;			    
		}
	    }	    
	}

	if (infEnd > 0) {
	    debug("skipped noun phrase: " +
		  makeString(taggedWords, infStart, infEnd));	    
	}	
    }

    private void skipNounPhrase(String[] taggedWords) {
	debug("skipping noun phrase");
	int npStart = curWord, npEnd = -1;
	
	np_search:
	for (; curWord < taggedWords.length; ++curWord) {

	    // read until after the next verb
	    String[] wordAndPOS = taggedWords[curWord].split("_");
	    if (!isValidPOS(wordAndPOS))
		return;
	    
	    String word = wordAndPOS[0];
	    String partOfSpeech = wordAndPOS[1];

	    if (isVerb(partOfSpeech)) {
		
		// search forward for compound verbs
		compound:
		for (curWord += 1; curWord < taggedWords.length; ++curWord) {
		    String[] nextWordAndPOS = taggedWords[curWord].split("_");
		    if (!isValidPOS(nextWordAndPOS))
			return;
			    
		    String nextPOS = nextWordAndPOS[1];
		    // skip over adverbs and adverbs
		    if (isAdverb(nextPOS) || 
			isVerb(nextPOS)) {
			continue compound;
		    }

		    npEnd = curWord - 1;
		    break np_search;			    
		}
	    }	    
	}

	if (npEnd > 0) {
	    debug("skipped noun phrase: " +
		  makeString(taggedWords, npStart, npEnd));	    
	}
    }		

    
    private static boolean isValidPOS(String[] wordAndPOS) {
	if (wordAndPOS.length != 2) {
	    if (SHOW_ERRORS) {
		System.err.println("malformed POS tag: " + 
				   Arrays.toString(wordAndPOS));
	    }
	    return false;
	}
	return true;
    }
    
    private static boolean isNoun(String partOfSpeech) {
	return partOfSpeech.equals("NN") ||
	    partOfSpeech.equals("NNP") ||
	    partOfSpeech.equals("NNPS") ||
	    partOfSpeech.equals("NNS");
    }

    private static boolean isVerb(String partOfSpeech) {
	return partOfSpeech.equals("VB") ||
	    partOfSpeech.equals("VBD") ||
	    partOfSpeech.equals("VBG") ||
	    partOfSpeech.equals("VBN") ||
	    partOfSpeech.equals("VBP") ||
	    partOfSpeech.equals("VBZ");
    }

    private static boolean isPreposition(String partOfSpeech) {
	return partOfSpeech.equals("IN");
    }

    private static boolean isStartOfNounPhrase(String partOfSpeech) {
	return partOfSpeech.equals("WDT");
    }

    private static boolean isStartOfInfinitivePhrase(String partOfSpeech) {
	return partOfSpeech.equals("TO");
    }

    private static boolean isAdverb(String partOfSpeech) {
	return partOfSpeech.equals("RB");
    }

    private static boolean isPossessive(String partOfSpeech) {
	return partOfSpeech.equals("POS");
    }

    private static void debug(String mesg) {
	if (DEBUG)
	    System.out.println(mesg);
    }
    
    private static void debug(String format, Object... args) {
	if (DEBUG) 
	    System.out.println(String.format(format,args));
    }

    private static String makeString(String[] words, int start, int end) {
	StringBuffer sb = new StringBuffer(8 * (end-start));
	for (int i = start; i <= end; ++i) {	    
	    String[] wordAndPOS = words[i].split("_");
	    sb.append((wordAndPOS.length == 2) 
		      ? wordAndPOS[0]
		      : words[i]).append(" ");
	}
	return sb.toString();
    }
}