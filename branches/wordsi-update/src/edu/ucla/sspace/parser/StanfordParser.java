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

package edu.ucla.sspace.parser;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

import edu.stanford.nlp.process.DocumentPreprocessor;

import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A {@link Parser} wrapper around the Stanford Parser. 
 *
 * @author Keith Stevens
 */
public class StanfordParser implements Parser {

  /**
   * The default location of the stanford parser information.
   */
  public static final String PARSER_MODEL = "data/englishPCFG.ser";

  /**
   * The {@link LexicalizedParser} responsible for parsing sentences.
   */
  private final LexicalizedParser parser;

  /**
   * A utility class used to extract dependency parse trees from PCFG parse
   * trees.
   */
  private final GrammaticalStructureFactory gsf;

  /**
   * Creates a new {@link StanfordParser} using the default model location.
   */
  public StanfordParser() {
    this(PARSER_MODEL, false);
  }

  /**
   * Creates a new {@link StanfordParser} using the provided model location.  If
   * {@code loadFromJar} is {@code true}, then the path is assumed to refer to a
   * file within the currently running jar.  This {@link Parser} can readily by
   * used within a map reduce job by setting {@code loadFromJar} to true and
   * including the parser model within the map reduce jar.
   */
  public StanfordParser(String parserModel, boolean loadFromJar) {
    LexicalizedParser p = null;
    GrammaticalStructureFactory g = null;
    try {
      InputStream inStream = (loadFromJar)
        ? this.getClass().getResourceAsStream("/"+parserModel)
        : new FileInputStream(parserModel);
      p = new LexicalizedParser(new ObjectInputStream(inStream));

      g = p.getOp().tlpParams.treebankLanguagePack().grammaticalStructureFactory();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    parser = p;
    gsf = g;
  }

  /**
   * {@inheritDoc}
   */
  public String parseText(String header, String document) {
    StringBuilder builder = new StringBuilder();

    DocumentPreprocessor processor = new DocumentPreprocessor(
                new StringReader(document));

    for (List<HasWord> sentence : processor) {
      // Parse the sentence.  If the sentence has no tokens or the parser fails,
      // simply return an empty string.
      if (sentence.size() == 0 || 
          sentence.size() > 100 ||
          !parser.parse(sentence))
        continue;

      // Get the parse tree and tagged words for the sentence.
      Tree tree = parser.getBestParse();
      List<TaggedWord> taggedSent = tree.taggedYield();

      // Convert the tree to a collection of dependency links.
      GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
      Collection<TypedDependency> dep = gs.typedDependencies();

      // Conver the dependency links into the expected default CoNLL format.
      String[] parseLines = new String[sentence.size()];
      for (TypedDependency dependency : dep) {
        int nodeIndex = dependency.dep().index();
        int parentIndex = dependency.gov().index();
        String relation = dependency.reln().toString();
        String token = taggedSent.get(nodeIndex-1).word();
        String pos = taggedSent.get(nodeIndex-1).tag();

        parseLines[nodeIndex-1] = String.format("%d\t%s\t_\t%s\t%s\t_\t%s\t%s",
              nodeIndex, token, pos, pos, parentIndex, relation);
      }

      // Add a header for the sentence.
      if (header != null && !header.equals(""))
        builder.append(header).append("\n");

      // Write out the text for each node in the dependency tree.  If there is
      // no text for the node, then it is assumed to be a root node.
      for (int i = 0; i < parseLines.length; ++i) {
        String parseLine = parseLines[i];

        // Assume that nodes without text are root nodes, so create a line for
        // the root node.
        if (parseLine == null) {
          String token = taggedSent.get(i).word();
          String pos = taggedSent.get(i).tag();
          builder.append(String.format("%d\t%s\t_\t%s\t%s\t_\t0\tnull",
                i+1, token, pos, pos));
        } else {
          builder.append(parseLine);
        }

        // Pad the tree with a new line.
        builder.append("\n");
      }
      builder.append("\n");
    }

    return builder.toString();
  }

  public static void main(String[] args) {
    Parser parser = new StanfordParser(StanfordParser.PARSER_MODEL, true);
    System.out.println(parser.parseText("conclude.n.1", "Reason alone , for Wesley , is further incapable of inculcating within those who exercise it the love of God , for such love is dep endent upon both faith and hope which reason can not produce. Wesley draws upon his own experiences for support : I collected the finest hymns , prayers , and m ediations , which I could find in any language; and I said , sung , or read them over and over , with all possible seriousness and attention. But still I was li ke the bones in Ezekiel ' s vision : ' the skin covered them above; but there wa s no breath in them ' ( qtd. in Wesley 129-30 ) . He therefore asks , what can c old reason do in this matter [ of God ' s love for sinners ] ? It may present us with fair ideas; it can draw a fine picture of love; but this is only a painted fire. And farther than this , reason can not go ( 131 ) . Since reason can not produce the love of God , it can not empower people to love their neighbors , wh ich , as such love forms the foundation of virtue for Wesley , prevents reason b y extension from producing virtue. Wesley concludes this discussion by stating t hat reason can not produce happiness , for happiness is dependent upon faith , h ope , love , and virtue ( 132 ) . "));
  }
}
