/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.SemanticSpace;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.io.BufferedReader;


/**
 * A base impelementation of {@link Wordsi}.  This base class manages the set of
 * acceptable words and secondary key assignments.
 *
 * @author Keith Stevens
 */
public abstract class BaseWordsi implements Wordsi, SemanticSpace {

  /**
   * The set of words which should be represented by {@link Wordsi}.
   */
  private final Set<String> acceptedWords;

  /**
   * A mapping from primary keys to secondary keys to the set of data point ids
   * that were assigned to each secondary key.
   */
  private final Map<String, Map<String, BitSet>> contextAssignments;
  
  /**
   * If true, the assignments made to secondary keys will be tracked, if false,
   * no tracking will be made.
   */
  private final boolean trackSecondaryKeys;

  /**
   * The {@link ContextExtractor} responsible for parsing documents and creating
   * context vectors.
   */
  private ContextExtractor extractor;

  /**
   * Creates a new {@link BaseWordsi}.
   *
   * @param acceptedWords The set of words which {@link Wordsi} should
   *        represent, may be {@code null} or empty.
   * @param trackSecondaryKeys If true, secondary key assignments will be
   *        tracked
   */
  public BaseWordsi(Set<String> acceptedWords,
                    ContextExtractor extractor,
                    boolean trackSecondaryKeys) {
    this.acceptedWords = acceptedWords;
    this.extractor = extractor;
    this.trackSecondaryKeys = trackSecondaryKeys;

    contextAssignments = new HashMap<String, Map<String, BitSet>>();
  }

  /**
   * {@inheritDoc}
   */
  public boolean acceptWord(String word) {
    return acceptedWords == null || 
           acceptedWords.isEmpty() ||
           acceptedWords.contains(word);
  }

  /**
   * {@inheritDoc}
   */
  public String getSpaceName() {
    return "Wordsi";
  }

  /**
   * {@inheritDoc}
   */
  public int getVectorLength() {
    return extractor.getVectorLength();
  }

  /**
   * {@inheritDoc}
   */
  public void processDocument(BufferedReader document) {
    extractor.processDocument(document, this);
  }

  /**
   * Records an assignment of {@code contextId} to {@code secondaryKey} and
   * {@code primaryKey}.
   */
  protected void mapSecondaryKey(String secondaryKey,
                                 String primaryKey,
                                 int contextId) {
    // Don't make the mapping if tracking is not set.
    if (!trackSecondaryKeys)
      return;

    // Get the mapping from secondary keys to context ids.
    Map<String, BitSet> termContexts = contextAssignments.get(primaryKey);
    if (termContexts == null) {
      synchronized (this) {
        termContexts = contextAssignments.get(primaryKey);
        if (termContexts == null) {
          termContexts = new HashMap<String, BitSet>();
          contextAssignments.put(primaryKey, termContexts);
        }
      }
    }

    // Get the set of context id's made to the secondary key.
    BitSet contextIds = termContexts.get(secondaryKey);
    if (contextIds == null) {
      synchronized (this) { 
        contextIds = termContexts.get(secondaryKey);
        if (contextIds == null) {
          contextIds = new BitSet();
          termContexts.put(secondaryKey, contextIds);
        }
      }
    }

    // Update the set of context ids assigned to the secondary key.
    synchronized (contextIds) {
      contextIds.set(contextId);
    }
  }

  /**
   * Return an array mapping context ids to secondary keys.  Returns an empty
   * array if there was no tracking done.
   */
  protected String[] contextLabels(String primaryKey) {
    Map<String, BitSet> termContexts = contextAssignments.get(primaryKey);
    if (termContexts == null)
      return new String[0];

    // Compute the total number of assignments made.
    int totalAssignments = 0;
    for (Map.Entry<String, BitSet> entry : termContexts.entrySet())
      totalAssignments = Math.max(totalAssignments, entry.getValue().length());
    //+= entry.getValue().cardinality();
    
    // Fill in each assignment with the secondary key attached to each context
    // id.
    String[] contextLabels = new String[totalAssignments];
    for (Map.Entry<String, BitSet> entry : termContexts.entrySet()) {
      BitSet contextIds = entry.getValue();
      for (int contextId = contextIds.nextSetBit(0); contextId >= 0;
           contextId = contextIds.nextSetBit(contextId+1))
        contextLabels[contextId] = entry.getKey();
    }
    return contextLabels;
  }
}

