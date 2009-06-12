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

package edu.ucla.sspace.hal;

import java.util.BitSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.WordIterator;
import edu.ucla.sspace.common.matrix.GrowingSparseMatrix;


public class HAL implements SemanticSpace
{
	/**
	 * Sets the default window size to 5
	 */
	private static final int DEFAULT_WINDOW_SIZE = 10;
	
	public static final String VECTOR_THRESHOLD_PROPERTY =
	"edu.ucla.sspace.hal.HAL.threshold";
	
	/**
	 * The Set that contains all of the words from the document
	 */
	private final Set<String> termList;
	
	/**
	 * Map that pairs the word with it's position in the matrix
	 */
	private Map<String,Integer> termToIndex;
	
	/**
	 * The  Matrix that contains all of the Co-occurence values
	 * for words following the focus
	 */
	private GrowingSparseMatrix followVals;
	
	/**
	 * The  Matrix that contains all of the Co-occurence values
	 * for words preceding the focus
	 */
	private GrowingSparseMatrix precVals;
	
		
	/**
	 * The number that keeps track of the index values of words
	 */
	private int indexNumber;
	
	public HAL()
	{
		termList = new LinkedHashSet<String>();
		
		followVals = new GrowingSparseMatrix();
		precVals = new GrowingSparseMatrix();
		
		termToIndex = new ConcurrentHashMap<String,Integer>();
		
		indexNumber = 0;
	}
	
	public void  processDocument(BufferedReader document) throws IOException
	{
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
					
					//Get the current value corresponding to the focus and the word in the Iterator
					double temp = followVals.get(termToIndex.get(focus), termToIndex.get(nextIter.next()));
					
					//add the value to it
					temp += value;
					
					//Put the new value back in
					followVals.set(termToIndex.get(focus), termToIndex.get(nextIter.next()), temp);
										
				}
				
				else
				{
					//Get the current value corresponding to the focus and the word in the Iterator
					double temp = followVals.get(termToIndex.get(focus), termToIndex.get(nextIter.next()));
					
					//add the value to it
					temp += value;
					
					//Put the new value back in
					followVals.set(termToIndex.get(focus), termToIndex.get(nextIter.next()), temp);
										
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
				
				//Get the current value corresponding to the focus and the word in the Iterator
				double temp = precVals.get(termToIndex.get(focus), termToIndex.get(nextIter.next()));
				
				//add the value to it
				temp += value;
				
				//Put the new value back in
				precVals.set(termToIndex.get(focus), termToIndex.get(nextIter.next()), temp);
											
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
	
	
	//Returns the Set of words taken from the documents
	public Set<String> getWords()
	{				
		//If no documents have been processed, it will be empty		
		return termList;			
	}		
	

	//Returns the constructed vector for the given word
	public double[] getVectorFor(String word)
	{
		//Clears the vector of any previous values
		double[] semVector = new double[followVals.columns()*2];
		double[] row1 = followVals.getRow(termToIndex.get(word));
		double[] row2 = precVals.getRow(termToIndex.get(word));
		
		if(termToIndex.containsValue(word))
		{
			//Puts the semantic values of the following words for the focus into the array to be returned
			System.arraycopy(row1, 0, semVector, 0, row1.length);
			//Puts the semantic values of the words preceding the focus into the array to be returned
			System.arraycopy(row2, 0, semVector, row1.length, row2.length);			
		}
			
		
		return semVector;
		
	}
	
	
	public void processSpace(Properties properties)
	{
		//Default
		int threshold = 50;
		//Get threshold value defined by user
		String userDefinedThresh = properties.getProperty(VECTOR_THRESHOLD_PROPERTY);
		if(userDefinedThresh != null)
		{
			try 
			{
			    threshold = Integer.parseInt(userDefinedThresh);
			} 
			catch (NumberFormatException nfe) 
			{				
			    throw new IllegalArgumentException(
			    VECTOR_THRESHOLD_PROPERTY + " is not an integer: " +
			    userDefinedThresh);
			}
		}
		
		//Drop Rows with low entropy out of followVals		
		BitSet colsToDrop = new BitSet(followVals.columns());
		for(int col = 0; col < followVals.columns();col++)
		{
			//set column count to zero
			int entropy = 0;
			
			//Calculate column entropy
			for(int rows = 0;rows<followVals.rows();rows++)
			{
				entropy += followVals.get(rows,col);
			}
			
			//If the entropy is not above the requested threshold
			if( entropy < threshold )
			{
				colsToDrop.set(col);				
			}	
		}
		

		//Drop out any columns with low entropy from the precVals
		for(int col = 0; col < precVals.columns();col++)
		{
			//set column count to zero
			int entropy = 0;
			
			//Calculate column entropy
			for(int rows = 0;rows<precVals.rows();rows++)
			{
				entropy += precVals.get(rows,col);
			}
			
			//If the entropy is not above the requested threshold
			if( entropy < threshold )
			{
				colsToDrop.set(col);
				
			}	
		}
		
		
		//Create a new double[] row to add into the new, smaller matrix
		double[] newRows = new double[followVals.columns()-colsToDrop.cardinality()];
		GrowingSparseMatrix smallerF = new GrowingSparseMatrix();
		//For every row
		for(int i = 0;i<followVals.rows();i++)
		{
			//And every column in that row
			for(int j = 0;j<followVals.columns();j++)
			{
				//If the column is not to be dropped
				if(!colsToDrop.get(j))
				{
					//Add it to the new row
					newRows[i] = followVals.get(i,j);
				}
			}
			//Put the new row into the matrix
			smallerF.setRow(i, newRows);
		}
		
		//Switch the original Matrices to the new, smaller Matrices
		followVals = smallerF;
		

		GrowingSparseMatrix smallerP = new GrowingSparseMatrix();
		//For every row
		for(int i = 0;i<precVals.rows();i++)
		{
			//And every column in that row
			for(int j = 0;j<precVals.columns();j++)
			{
				//If the column is not to be dropped
				if(!colsToDrop.get(j))
				{
					//Add it to the new row
					newRows[i] = precVals.get(i,j);
				}
			}
			//Put the new row into the matrix
			smallerP.setRow(i, newRows);
		}
			
		//Switch the original Matrices to the new, smaller Matrices
		precVals=smallerP;
		
		
		//Change the values of the termToIndex to match the smaller matrices
		Map<String,Integer> termToNewIndex = new ConcurrentHashMap<String,Integer>();
		
		//Set of keys to put into new index, put into an array for easier access
		Set<String> keystemp = termToIndex.keySet();
		String[] keys = new String[precVals.columns()];
		keystemp.toArray(keys);
		
		int newIndexCount = 0;
		for(int i=0;i<precVals.columns();i++)
		{
			//If not dropping the word
			if(!colsToDrop.get(i))
			{
				//Add the word to the new termToIndex
				termToNewIndex.put(keys[i], newIndexCount);
						
				//Increase the new Index Count
				newIndexCount++;
			}
			
		}
		
		termToIndex = termToNewIndex;
		
		
	}
	

	public String getSpaceName()
	{
		return "hal-semantic-space";
	}
}