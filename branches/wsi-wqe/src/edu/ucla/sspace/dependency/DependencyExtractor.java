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

package edu.ucla.sspace.dependency;

import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.HashMultiMap;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A parser for dependency parsed sentences in the <a
 * href="http://nextens.uvt.nl/depparse-wiki/DataFormat">CoNLL format</a>
 * generated by the Malt parser.  The ordering of the CoNLL format can be
 * specified with an xml file following the format specified <a
 * href="http://maltparser.org/userguide.html#inout">here</a>.  This
 * configuration file simply specifies the ordering of the dependency features.
 * By default, the extractor assumes the default format used by the <a
 * href="http://maltparser.org/index.html">Malt Parser</a>
 *
 * </p>
 *
 * The parsed result will be an array based
 * tree structure containing the relations between each word in the sentence,
 * ordered by the ordering of word occurrences.
 *
 * @author Keith Stevens
 */
public class DependencyExtractor {

    /**
     * The feature index for the node id.
     */
    private final int idIndex;

    /**
     * The feature index for the word's form.
     */
    private final int formIndex;

    /**
     * The feature index for the word's lemma.
     */
    private final int lemmaIndex;

    /**
     * The feature index for the word's part of speech tag.
     */
    private final int posIndex;

    /**
     * The feature index for the parent, or head, of the current node.
     */
    private final int parentIndex;

    /**
     * The feature index for the relation to the parent, or head, of the current
     * node.
     */
    private final int relationIndex;

    /**
     * Creates a new {@link DependencyExtractor} that assumes the default
     * ordering for {@code Malt} dependency parses.
     */
    public DependencyExtractor() {
        idIndex = 0;
        formIndex = 1;
        lemmaIndex = 2;
        posIndex = 3;
        parentIndex = 6;
        relationIndex = 7;
    }

    /**
     * Creates a new {@link DependencyExtractor} by parsing a {@code Malt}
     * configuration file, which specifies the order in which the output is
     * formatted.
     */
    public DependencyExtractor(String configFile) {
        // Set up non final index values for each feature of interest.
        int id = 0;
        int form = 1;
        int lemma = 2;
        int pos = 3;
        int head = 4;
        int rel = 5;

        try {
            // Set up an XML parser.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document currentXmlDoc = db.parse(configFile);

            // Traverse through each column entry in the configuration file.
            NodeList columnList = currentXmlDoc.getElementsByTagName("column");
            for (int i = 0; i < columnList.getLength(); ++i) {
                Element column = (Element) columnList.item(i);
                String name = column.getAttribute("name");

                // If the name attribute matches one of the features we need to
                // extract, set the index as the order in which the feature name
                // occurred.
                if (name.equals("ID"))
                    id = i;
                if (name.equals("FORM"))
                    form = i;
                if (name.equals("LEMMA"))
                    lemma = i;
                if (name.equals("POSTAG"))
                    pos = i;
                if (name.equals("HEAD"))
                    head = i;
                if (name.equals("DEPREL"))
                    rel= i;
            }
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (org.xml.sax.SAXException saxe) {
            saxe.printStackTrace();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Set the final indexes based on what was in the configuration file.
        idIndex = id;
        formIndex = form;
        lemmaIndex = lemma;
        posIndex = pos;
        parentIndex = head;
        relationIndex = rel;
    }
       
    /**
     * Extracts dependency relations from a document containing only the words
     * in a sentence, in the CoNLL format.  
     *
     * The CoNNLL features that are  of interest are ID, LEMMA or FORM, POSTAG,
     * HEAD, and DEPREL which are the id of the node, the string for the word at
     * this position, the part of speech tag, the parent of this node, and the
     * relation to the parent, respectively.  These features will be extracted
     * and returned as an array based tree structure ordered by the occurrence
     * of the words in the sentence.
     *
     * @param reader a reader for the document to parse
     *
     * @return an array of {@link DependencyRelation}s that compose a dependency
     *         tree
     *
     * @throws IOException when errors are encountered during reading
     */
    public DependencyRelation[] parse(BufferedReader reader) 
            throws IOException {
        List<SimpleDependencyRelation> relations =
            new ArrayList<SimpleDependencyRelation>();
        MultiMap<Integer, DependencyLink> childrenToAdd =
            new HashMultiMap<Integer, DependencyLink>();

        StringBuilder sb = new StringBuilder();

        // Read each line in the document to extract the feature set for each
        // word in the sentence.
        int id = 0;
        for (String line = null; ((line = reader.readLine()) != null); ) {
            // If a new line is encountered and no lines have been handled yet,
            // skip all new lines.
            if (line.length() == 0 && relations.size() == 0)
                continue;

            // If a new line is encountered and lines have already been
            // processed, we have finished processing the entire sentence and
            // can stop.
            if (line.length() == 0)
                break;

            sb.append(line).append("\n");

            // CoNLL formats using tabs between features.
            String[] nodeFeatures = line.split("\\s+");

            // Get the node id and the parent node id.
            int parent = Integer.parseInt(nodeFeatures[parentIndex]) - 1;

            // Get the lemma for the word.  If it is unspecified, i.e.
            // represented with a "_", then simply get the lower case version of
            // of the raw term.
            String lemma = nodeFeatures[lemmaIndex];
            String word = (lemma.equals("_"))
                ? nodeFeatures[formIndex].toLowerCase()
                : lemma;

            // Get the part of speech of the node.
            String pos = nodeFeatures[posIndex];

            // Get the relation between this node and it's head node.
            String rel = nodeFeatures[relationIndex];

            // Create the new relation.
            SimpleDependencyRelation relation = 
                new SimpleDependencyRelation(word, pos, parent, rel);
            relations.add(relation);

            // Set the dependency link between this node and it's parent node.
            // If the parent is negative then the node itself is a root node and
            // has no parent.
            if (parent >= 0) {
                // If the parent has already been processed, add a child link
                // from the parent node to this node.  Otherwise store the link
                // in a map to be processed later.
                if (parent < relations.size())
                    relations.get(parent).addNeighbor(
                            new DependencyLink(id, rel));
                else
                    childrenToAdd.put(parent, new DependencyLink(id, rel));
            }
            id++;
        }

        if (relations.size() == 0)
            return null;

        if (childrenToAdd.size() != 0) {
            // Process all the child links that were not handled during the
            // processing of the words.
            for (Map.Entry<Integer, DependencyLink> childLink :
                    childrenToAdd.entrySet()) {
                int childIndex = childLink.getKey();
                DependencyLink link = childLink.getValue();
                relations.get(childIndex).addNeighbor(link);
            }
        }

        return relations.toArray(
                new SimpleDependencyRelation[relations.size()]);
    }

    /**
     * A default implementation of a {@link DependencyRelation}
     */
    public static class SimpleDependencyRelation implements DependencyRelation {

        /**
         * The word stored at this node.
         */
        private String word;

        /**
         * The parent node part of speech tag.
         */
        private String pos;

        /**
         * The list of neighbord of this node.
         */
        private List<DependencyLink> neighbors;

        /**
         * Creates a new {@link SimpleDependencyRelation} node for the provided
         * word, with the provided parent link.  Initially the children list is
         * empty.
         */
        public SimpleDependencyRelation(String word, String pos,
                                        int parent, String relation) {
            neighbors = new LinkedList<DependencyLink>();
            if (parent >= 0)
                neighbors.add(new DependencyLink(parent, relation));
            this.word = word;
            this.pos = pos;
        }

        public List<DependencyLink> neighbors() {
            return neighbors;
        }

        /**
         * {@inheritDoc}
         */
        public String word() {
            return word;
        }

        /**
         * {@inheritDoc}
         */
        public String pos() {
            return pos;
        }

        /**
         * Adds a child node id to this node.
         */
        public void addNeighbor(DependencyLink neighbor) {
            neighbors.add(neighbor);
        }
    }
}
