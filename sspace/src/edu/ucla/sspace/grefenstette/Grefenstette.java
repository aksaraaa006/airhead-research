/*
 * Copyright 2009 Grace Park
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

package edu.ucla.sspace.grefenstette;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.IOError;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of a semantic space built from syntactic co-occurrence, as
 * described by Grefenstette.  See the following references for full details.
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">G. Grefenstette,
 *   <i>Explorations in Automatic Thesaurus Discovery</i>. Indiana University
 *   Press, 1994.</li>
 *
 * </ul>
 *
 *
 * @author Grace Park 
 */
public class Grefenstette implements SemanticSpace {

    private File wordRelations;
    private PrintWriter wordRelationsWriter;
    private BufferedReader document;
    private Map<String,Integer> objectTable;
    private Map<String,Integer> attributeTable;
    private Matrix matrix;
    private final AtomicInteger objectCounter;
    private final AtomicInteger attributeCounter;

    public Grefenstette() {
	try {
	    wordRelations = File.createTempFile("word-relation-list","txt");
	    wordRelationsWriter = new PrintWriter(wordRelations);
	  
	    objectTable = new HashMap<String,Integer>();
	    attributeTable = new HashMap<String,Integer>();
	  
	    matrix = new GrowingSparseMatrix();
	  
	    objectCounter = new AtomicInteger(0);
	    attributeCounter = new AtomicInteger(0);
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument (BufferedReader document) {
	ArrayList<Pair<String>> wordsInPhrase = new ArrayList<Pair<String>>();

	String nounPhrase = "";
	String lastNoun = "";
	String lastVerb = "";
	String secondPrevPhrase = "";
	String prevPhrase = "";

	try {
	    nounPhrase = document.readLine();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	for( String tag = getNextTag(nounPhrase); 
	     tag != null; tag = getNextTag(nounPhrase) ) {
	    String word;

	    int startOfTag = nounPhrase.indexOf(tag);

	    nounPhrase = nounPhrase.substring(startOfTag);
	    wordsInPhrase.clear();

	    if( tag.equals("NP") ) {
		while( nounPhrase.charAt(0) != ')' ) {

		    // extract tag of word in noun phrase
		    tag = getNextTag(nounPhrase); 

		    if( isPhraseOrClause(tag) || isPreposition(tag) ) {
			nounPhrase = nounPhrase.substring( nounPhrase.indexOf(tag)
							   + tag.length() );
			// stop processing NP
			break;
		    } else if( inStartSet(tag) || inReceiveSet(tag) ) {
			// note to self: find out why this broke
			try {
			    word = nounPhrase.substring( nounPhrase.indexOf(" ", 
									    nounPhrase.indexOf(tag))+1, nounPhrase.indexOf(")"));

			    wordsInPhrase.add( new Pair<String>(tag,word) );

			    nounPhrase = nounPhrase.substring(nounPhrase.indexOf(")",
										 nounPhrase.indexOf(word))+1);
			} catch (StringIndexOutOfBoundsException e) {
			    nounPhrase = nounPhrase.substring(nounPhrase.indexOf(")"));
			}
			// else it's not a tag I care about
		    } else {
			nounPhrase = nounPhrase.substring(nounPhrase.indexOf(")")+1);
		    }
		}

		// note to self: is this if statement represent the same thing
		// as the next if statement??
		if( !wordsInPhrase.isEmpty() ) {
		    // set head noun to last word in noun phrase
		    String headNoun = wordsInPhrase.get(wordsInPhrase.size()-1).y;

		    // create the relations from pass two
		    if( prevPhrase.equals("PP") && secondPrevPhrase.equals("NP") 
			&& lastNoun.length() != 0 ) {
// 			System.out.println(lastNoun + " " + headNoun);
			wordRelationsWriter.println(lastNoun + " " + headNoun);

			addRelation(lastNoun, headNoun);
		    }

		    // create relations from pass four
		    if( prevPhrase.equals("PP") && secondPrevPhrase.equals("VP")
			&& lastVerb.length() != 0 ) {
// 			System.out.println(headNoun + " " + lastVerb );
			wordRelationsWriter.println(lastVerb + " " + headNoun);

			addRelation(lastVerb, headNoun);
		    } else if( prevPhrase.equals("VP") ) {
// 			System.out.println(headNoun + " " + lastVerb );
			wordRelationsWriter.println(lastVerb + " " + headNoun);

			addRelation(lastVerb, headNoun);
		    }

		    lastNoun = headNoun;
		}

		// reached end of noun phrase
		if( nounPhrase.charAt(0) == ')' ) {
		    // create relations between words in noun phrase
		    // relations from pass one
		    processWordsInNP(wordsInPhrase);

		    if( !"NP".equals(prevPhrase) ) {
			secondPrevPhrase = prevPhrase;
			prevPhrase = "NP";
		    }
		}
	    } //end processing NP
	    else if( tag.equals("VP") ) {
		while( tag != null && tag.startsWith("V") ) {
		    // nonphrase verb
		    if( tag.startsWith("VB") ) {
			word = nounPhrase.substring( nounPhrase.indexOf(" ", 
									nounPhrase.indexOf(tag))+1, nounPhrase.indexOf(")"));
			lastVerb = word;
		    }

		    nounPhrase = nounPhrase.substring(nounPhrase.indexOf(tag)+1);
		    tag = getNextTag(nounPhrase);
		}

		// relations from pass three
		if( prevPhrase.equals("NP") && lastNoun.length() != 0 ) {
// 		    System.out.println(lastNoun + " " + lastVerb);
		    wordRelationsWriter.println(lastNoun + " " + lastVerb);

		    addRelation(lastNoun, lastVerb);
		}

		if( !prevPhrase.equals("VP") ) {
		    secondPrevPhrase = prevPhrase;
		    prevPhrase = "VP";
		}
	    }
	    else if( isPhraseOrClause(tag) || isPreposition(tag) ) {
		nounPhrase = nounPhrase.substring( nounPhrase.indexOf(tag)
						   + tag.length());
		if( !tag.equals(prevPhrase) ) {
		    secondPrevPhrase = prevPhrase;
		    prevPhrase = tag;
		}
	    }
	    else {
		nounPhrase = nounPhrase.substring( nounPhrase.indexOf(tag)
						   + tag.length());
	    }
	}
    }


    /**
     * Adds a relation pair to the matrix
     */
    private void addRelation(String object, String attribute) {
	double val;
	int row, col;

        object = object.toLowerCase();
        attribute = attribute.toLowerCase();

	// get row in matrix
	if( objectTable.containsKey(object) ) {
            // if the object already exists in matrix, find its index
	    row = objectTable.get(object);
	} else {
            // otherwise give the object a new index number
	    row = Integer.valueOf(objectCounter.getAndIncrement());
            // insert new object/index pair into lookup table
	    objectTable.put( object, row );
            System.out.println(object + " " + row);
	}

	// get column in matrix
	if( attributeTable.containsKey(attribute) ) {
	    col = attributeTable.get(attribute);
	} else {
	    col = Integer.valueOf(attributeCounter.getAndIncrement());
	    attributeTable.put( attribute, col );
	}

	// update entry in matrix which records how many times
        // the object/attribute pair has been seen
	if( row < matrix.rows() && col < matrix.columns() ) {
            // if there's already an entry for the object and attribute,
            // get the current value for the pair of words
	    val = matrix.get(row, col);
            // increment the current value by one and store in matrix
	    matrix.set(row, col, val+1);
	} else {
            // otherwise set the row, col value to 1
	    matrix.set(row, col, 1.0);
	}
    }

    /**
     * Creates relations between words in a noun phrase
     */
    private void processWordsInNP(ArrayList<Pair<String>> wordsInPhrase) {
	if( wordsInPhrase.size() > 1 ) {
	    // this is from Grefenstette's pseudo code
	    for (int i = 0; i < wordsInPhrase.size()-1; i++) {

		if (inStartSet(wordsInPhrase.get(i).x) ) {
		
		    for (int j = i+1; j < wordsInPhrase.size(); j++ ) {
			
			if (inReceiveSet( wordsInPhrase.get(j).x ) ) {

			    wordRelationsWriter.
				println(wordsInPhrase.get(j).y + " "
					+ wordsInPhrase.get(i).y);
			    
// 			    System.out.println(wordsInPhrase.get(j).y + " "
// 					       + wordsInPhrase.get(i).y);
			    
			    addRelation(wordsInPhrase.get(j).y, 
					wordsInPhrase.get(i).y);
			}
		    }
		}
	    }
	}
    }

  
    /**
     * Checks to see if the tag can modify another word
     *
     * @param tag A tag from the parsed corpus to be checked
     */
    private boolean inStartSet(String tag) {
	return
	    // noun
	    tag.startsWith("NN") ||
	    // adjective
	    tag.startsWith("JJ") ||
	    // adverb
	    tag.startsWith("RB") ||
	    // cardinal number
	    tag.startsWith("CD");
    }


    /**
     * Checks to see if tag can be modified by a word in StartSet
     */
    private boolean inReceiveSet(String tag) {
	return
	    tag.startsWith("NN") ||
	    tag.startsWith("VB");
    }


    /**
     * Checks to see if tag is a preposition
     */
    private boolean isPreposition(String tag) {
	return tag.startsWith("PP");
    }


    /**
     * Checks to see if tag marks a phrase or clause
     */
    private boolean isPhraseOrClause(String tag) {
	// find out why adding more reduced the number of relations
	return
	    (!tag.equals("SYM") &&
	     tag.startsWith("S")) ||
	    tag.equals("ADJP") ||
	    tag.equals("ADVP") ||
	    tag.equals("CONJP") ||
	    tag.equals("FRAG") ||
	    tag.equals("INTJ") ||
	    tag.equals("LST") ||
	    tag.equals("NAC") ||
	    tag.equals("NP") ||
	    tag.equals("NX") ||
	    tag.equals("PP") ||
	    tag.equals("PRN") ||
	    /* removing prt adds 1% more relations */
	    tag.equals("PRT") ||
	    tag.equals("QP") ||
	    tag.equals("RRC") ||
	    tag.equals("UCP") ||
	    tag.equals("VP") ||
	    tag.startsWith("WH") ||
	    tag.equals("X");
    }

    /**
     * Returns the next tag in the sentence or null if there are no more tags
     * @param str The sentence that the tag is extracted from
     */
    private String getNextTag(String str) {
	String tag;
	int endIndex;
	int tagIndex = str.indexOf("(");

	if( tagIndex < 0 ) {
	    return null;
	}

	// in case there's nothing in the sentence
	endIndex = str.indexOf(" ", tagIndex);

	if( endIndex < 0 ) {
	    return null;
	}

	tag = str.substring( tagIndex+1, endIndex ); 

	if( tag.length() > 0 ) {
	    return tag;
	} else {
	    str = str.substring( tagIndex+1 );
	    return getNextTag(str);
	}
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return Collections.unmodifiableSet(objectTable.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String word) {
	int wordIndex;
        double nullArray[] = {0.0};
        word = word.toLowerCase();
	if( objectTable.containsKey(word) ) {
	    wordIndex = objectTable.get(word);
            if( wordIndex < matrix.rows() ) {
                try {
	        return matrix.getRow(wordIndex);
                } catch (NullPointerException npe) {
                    return nullArray;
                } catch (ArrayIndexOutOfBoundsException e) {
                    return nullArray;
                }
            }
            else {
                return nullArray;
            }
	} else {
	    return nullArray;
	}
    }

    /**
     * Does nothing.
     */
    public void processSpace(Properties properties) {
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
	return "grefenstette-syntatic-analysis";
    }
}
