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

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A pseudo word based {@link DependencyContextExtractor}.  Given a mapping from
 * raw tokens to pseudo words, this extractor will automatically change the text
 * for any dependency node that has a valid pseudo word mapping.  The pseudo
 * word will serve as the primary key for assignments and the original token
 * will serve as the secondary key.
 *
 * @author Keith Stevens
 */
public class PseudoWordDependencyContextExtractor 
    extends DependencyContextExtractor  {

  /** 
   * The set of pseudo words.
   */
  private Set<String> pseudoWords;

  /**
   * A mapping from {@link DependencyTreeNode}s to the original token for that
   * node.
   */
  private Map<DependencyTreeNode, String> replacementMap;

  /**
   * Creates a new {@link PseudoWordDependencyContextExtractor}.
   *
   * @param extractor The {@link DependencyExtractor} that parses the document
   *        and returns a valid dependency tree
   * @param basisMapping A mapping from dependency paths to feature indices
   * @param weighter A weighting function for dependency paths
   * @param acceptor An accepting function that validates dependency paths which
   *        may serve as features
   * @param pseudoWordMap A mapping from raw tokens to pseudo words
   */
  public PseudoWordDependencyContextExtractor(
      DependencyExtractor extractor,
      DependencyContextGenerator generator,
      Map<String, String> pseudoWordMap) {
    super(new PseudoWordDependencyExtractor(extractor, pseudoWordMap),
          generator, true);

    // Convert the dependency tree extractor to the known pseudo word extractor
    // and get a pointer to the replacement map.
    PseudoWordDependencyExtractor pwExtractor =
      (PseudoWordDependencyExtractor) this.extractor;
    this.replacementMap = pwExtractor.replacementMap;

    // Create the set of pseudo words.
    pseudoWords = new HashSet<String>();
    pseudoWords.addAll(pseudoWordMap.values());
  }

  /**
   * Returns true if {@code focusWord} is a known pseudo word.
   */
  protected boolean acceptWord(DependencyTreeNode focusNode,
                               String contextHeader,
                               Wordsi wordsi) {
    return pseudoWords.contains(focusNode.word());
  }

  /**
   * Returns the pseudo word replacement for the word for {@code focusNode} if
   * there is a mapping, or {@code null}.
   */
  protected String getSecondaryKey(DependencyTreeNode focusNode,
                                   String contextHeader) {
    String replacement = replacementMap.get(focusNode);
    if (replacement == null)
      return focusNode.word();

    replacementMap.remove(focusNode);
    return replacement;
  }

  /**
   * A private pseudo word based {@lin DependencyExtractor}.  This class
   * automatically changes out the text for dependency nodes if they have a
   * valid pseudo word mapping.  The reverse mapping is automatically
   * maintained.
   */
  private static class PseudoWordDependencyExtractor 
      implements DependencyExtractor {

    /**
     * The raw {@link DependencyExtractor}.
     */
    final DependencyExtractor extractor;

    /**
     * The mapping from raw tokens to their pseudo words.
     */
    final Map<String, String> pseudoWordMap;

    /**
     * A mapping from dependency tree nodes to the original text for that node.
     */
    final Map<DependencyTreeNode, String> replacementMap;

    /**
     * Creates a new {@link PseudoWordDependencyExtractor}.
     */
    public PseudoWordDependencyExtractor(DependencyExtractor extractor,
                                         Map<String, String> pseudoWordMap) {
      this.extractor = extractor;
      this.pseudoWordMap = pseudoWordMap;
      this.replacementMap = new ConcurrentHashMap<DependencyTreeNode, String>();
    }

    /**
     * Returns a new tree of {@link DependencyTreeNode}s.  If any node is for a
     * word that has a pseudo word mapping, that node's text is replaced with
     * the pseudo word.  A mapping is then created for the node to the pseudo
     * word.
     */
    public DependencyTreeNode[] readNextTree(BufferedReader reader)
        throws IOException { 
      reader.readLine();
      DependencyTreeNode[] tree = extractor.readNextTree(reader);
      for (int i = 0; i < tree.length; ++i) {
        // Get the mapping for the node's text.  If there is a mapping, replace
        // the node with a pseudo word version and make a mapping from the node
        // to the original term.
        String realToken = tree[i].word();
        String replacement = pseudoWordMap.get(realToken);
        if (replacement != null) {
          tree[i] = new SimpleDependencyTreeNode(
              replacement, tree[i].pos(), tree[i].neighbors());
          replacementMap.put(tree[i], realToken);
        }
      }
      return tree;
    }
  }
}
