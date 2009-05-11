/*
 * Copyright 2009 Alex Nau
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

package src.edu.ucla.sspace.hal;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.WordIterator;


public class HAL implements SemanticSpace
{
	/**
	 * Sets the default window size to 5
	 */
	private static final int DEFAULT_WINDOW_SIZE = 5;
	
	/**
	 * The set that contains all of the words from the document
	 */
	private final Set<String> termList;
	
	/**
	 * Map that pairs the word with it's position in the matrix
	 */
	private Map<String,Integer> termToIndex;
	
	/**
	 * The  Matrix that contains all of the Co-occurence values
	 */
	private int values[][];
	
	public HAL()
	{
		termList = new LinkedHashSet<String>();
		values =  new int[1000][1000];	
		termToIndex = new ConcurrentHashMap<String,Integer>();
	}
	
	public void  processDocument(BufferedReader document) throws IOException
	{
		//Integer to keep track of word indices.
		int indexNumber = 1; 
		
		Queue<String> nextWords = new LinkedList<String>();
		Queue<String> prevWords = new LinkedList<String>();
		
		WordIterator it = new WordIterator(document);
		
		Iterator<String> nextIter = nextWords.iterator();
		Iterator<String> prevIter = prevWords.iterator();
		
		String focus = null;
		
		
		//Load the first windowSize words into the Queue		
		for(int i = 0; i<DEFAULT_WINDOW_SIZE && it.hasNext();i++)
		{
			nextWords.offer(it.next());
		}
		
		while(!nextWords.isEmpty())
		{
			//Load the top of the nextWords Queue into the focus word
			focus = nextWords.remove();
			
			//Add the next word to nextWords queue (if possible)
			if (it.hasNext())
			{
				String windowEdge = it.next();
				nextWords.offer(windowEdge);
			}			
			
			//Iterate through the Queue and add values
			int value = 5;
			
			//Reinitialize the Queue iterator to the beginning of the Queue
			nextIter = nextWords.iterator();
			while(nextIter.hasNext())
			{
				//If the word is not already in the set
				if(termList.add(focus))
				{
					//Add it to the termToIndex Map with it's index value
					termToIndex.put(focus, indexNumber);
					indexNumber++;
				}
				
				//If the String in the Queue is not in the termList
				if(termList.add(nextIter.next()))
				{
					//Add it to the termToIndex Map with it's corresponding Index number
					termToIndex.put(nextIter.next(), indexNumber);
					indexNumber++;
					
					//Add the co-occurance value into the values matrix
					values[termToIndex.get(focus)][termToIndex.get(nextIter.next())] += value;					
				}
				
				else
				{
					//Add the co-occurance value into the values matrix
					values[termToIndex.get(focus)][termToIndex.get(nextIter.next())] += value;	
				}				
				
				//decrease the co-occurance value
				value--;
			}
				
			
			
			//Evaluate the words in prevIter queue
			value=1;
			prevIter = prevWords.iterator();
			while(prevIter.hasNext())
			{
				//Since any word in the prevIter is already in the word set and the termToIndex Map
				//There's no need to check for that, and we can just directly put in the co-occurance
				//values into the co-occurance Matrix
				values[termToIndex.get(focus)][termToIndex.get(prevIter.next())] +=value;
				
				//Since were counting up, increase the value
				value++;
			}
					
			
			// last, put this focus word in the prev words and shift off the
		    // front if it is larger than the window
		    prevWords.offer(focus);
		    if (prevWords.size() > DEFAULT_WINDOW_SIZE)
		    	prevWords.remove();
		}		
	}	
}


