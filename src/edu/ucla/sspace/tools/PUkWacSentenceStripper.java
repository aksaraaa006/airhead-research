
package edu.ucla.sspace.tools;


import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.UkWacDependencyFileIterator;

import java.util.Iterator;

import java.io.IOException;
import java.io.PrintWriter;


/**
 * @author Keith Stevens
 */
public class PUkWacSentenceStripper {

    public static void main(String[] args) throws IOException {
        Iterator<Document> ukWacIter = new UkWacDependencyFileIterator(args[0]);

        PrintWriter writer = new PrintWriter(args[1]);
        StringBuilder builder = new StringBuilder();
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        while (ukWacIter.hasNext()) {
            DependencyTreeNode[] tree = extractor.readNextTree(
                    ukWacIter.next().reader());
            for (DependencyTreeNode node : tree)
                builder.append(node.word()).append(" ");
            writer.println(builder.toString());
            builder = new StringBuilder();
        }
    }
}
