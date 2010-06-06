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

import edu.ucla.sspace.util.Pair;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Deque;


/**
 * A traversal class for iterating over a dependency tree of {@link Relation}s.
 * Given the tree and a starting index, the traverser will find all paths that
 * satisfy several different criteria: length of the path and accepted relations
 * in the path.  
 *
 * </p>
 *
 * Note that this class is <b>NOT</b> thread safe.
 */
public class DependencyIterator implements Iterator<DependencyPath> {

    /**
     * The relation graph to use for iterating through all paths rooted at term
     * {@code currentTerm}.
     */
    private DependencyTreeNode[] relations;

    /**
     * The current term to traverse for neighbors.  Initially this will be the
     * requested term.
     */
    private int currentTerm;

    /**
     * The maximum length of the returned paths.  The length is considedered to
     * not include the first term.
     */
    private int maxPathLength;

    /**
     * The stack of words and relations.  This is used to compose the returned
     * {@link DependencyPath}s.  The first string in the pairs is the word for
     * the node and the second is the word's relation to the next node.
     */
    private LinkedList<DependencyRelation> linkDeque;

    /**
     * The stack of terms that are being processed.  This correponds to nodes
     * in the graph whose neighbors still need to be explored.
     */
    private Deque<Integer> termDeque;

    /**
     * The stack of neighbors to be explored.  For each node, the neighbors are
     * stored in this stack so that when the iterator backtracks to another path
     * it can restore the list of neighbors.
     */
    private Deque<Queue<DependencyLink>> neighboorDeque;

    /**
     * The {@link DependencyPathAcceptor} that validates each link before it is
     * traversed and returned as part of a {@link DependencyPath}.
     */
    private DependencyPathAcceptor acceptor;

    /**
     * The {@link DependencyPathWeight} that scores each link before it is
     * traversed and returned as part of a {@link DependencyPath}.
     */
    private DependencyPathWeight weighter;

    /**
     * The next {@link DependencyPath} to return.
     */
    private DependencyPath next;

    /**
     * Creates a new {@link DependencyIterator} that will return all {@link
     * DependencyPath}s rooted at the term with index {@code startTerm}.  Each
     * link in the path will be valied with {@code acceptor} and weighted with
     * {@code weighter}.  Each path will have length 1 + {@code maxPathLength}.
     *
     * @param relationTree The array of {@link dependencyRelation}s that compose
     *        a dependnecy parse graph
     * @param acceptor The {@link DependencyPathAcceptor} that will validate
     *        each link the a path
     * @param weighter The {@link DependencyPathWeight} that will score  
     *        each returned path
     * @param 
     */
    public DependencyIterator(DependencyTreeNode[] relationTree,
                              DependencyPathAcceptor acceptor,
                              DependencyPathWeight weighter,
                              int startTerm,
                              int maxPathLength) {
        this.relations = relationTree;
        this.acceptor = acceptor;
        this.weighter = weighter;
        this.maxPathLength = maxPathLength;
        this.currentTerm = startTerm;

        linkDeque = new LinkedList<DependencyRelation>();
        termDeque = new LinkedList<Integer>();
        neighboorDeque = new LinkedList<Queue<DependencyLink>>();
        termDeque.addLast(startTerm);

        addLayer();
        next = advance();
    }

    private void addLayer() {
        int previousTerm = (termDeque.size() == 0) ? -1 : termDeque.peekLast();

        // Get the index and relation of the current term to be inspected.
        DependencyTreeNode currentRelation = relations[currentTerm];

        // Create a new layer.
        Queue<DependencyLink> currentLayer = new LinkedList<DependencyLink>();

        // Add the neighbors that don't link back to where the current term came
        // from.
        List<DependencyLink> neighbors = currentRelation.neighbors();
        for (DependencyLink link : neighbors) {
            if (link.neighbor() == previousTerm)
                continue;
            currentLayer.add(link);
        }

        neighboorDeque.addLast(currentLayer);
    }

    private DependencyPath advance() {
        if (neighboorDeque.size() == 0)
            return null;

        Queue<DependencyLink> currentLayer = neighboorDeque.peekLast();

        if (currentLayer.size() == 0) {
            neighboorDeque.removeLast();
            currentTerm = termDeque.peekLast();
            termDeque.removeLast();

            // The link deque has one fewer elements than all the others.  So
            // when they have their last elements removed, the link deque should
            // already be empty.
            if (linkDeque.size() > 0)
                linkDeque.removeLast();

            return advance();
        }

        while (currentLayer.size() > 0) {
            DependencyLink link = currentLayer.remove();
            DependencyTreeNode term = relations[currentTerm];

            DependencyTreeNode neighbor = relations[link.neighbor()];
            boolean linkAccepted = acceptor.acceptLink(
                    term.pos(), link.relation(), neighbor.pos()) &&
                (linkDeque.size() + 1) <= maxPathLength;
            if (linkAccepted) {
                linkDeque.addLast(new SimpleDependencyRelation(
                            term.word(), link.relation(), link.isHeadNode()));
                LinkedList<DependencyRelation> path =
                    new LinkedList<DependencyRelation>(linkDeque);
                path.add(new SimpleDependencyRelation(
                            neighbor.word(), "", !link.isHeadNode()));

                termDeque.addLast(currentTerm);
                currentTerm = link.neighbor();

                addLayer();
                return new SimpleDependencyPath(path, weighter.scorePath(path));
            }
        }

        return advance();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyPath next() {
        DependencyPath path = next;
        next = advance();
        return path;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove is not permited on " +
                "a DependencyIterator");
    }

    /**
     * A simple {@link DependencyPath} that is created from a list and a score.
     */
    public class SimpleDependencyPath implements DependencyPath {

        /**
         * The list of terms and relations.
         */
        private LinkedList<DependencyRelation> path;

        /**
         * The score of the path.
         */
        private double score;

        /**
         * Creates a {@link SimpleDependencyPath}.
         */
        public SimpleDependencyPath(LinkedList<DependencyRelation> path,
                                    double score) {
            this.path = path;
            this.score = score;
        }

        /**
         * {@inheritDoc}
         */
        public LinkedList<DependencyRelation> path() {
            return path;
        }

        /**
         * {@inheritDoc}
         */
        public double score() {
            return score;
        }
    }
}
