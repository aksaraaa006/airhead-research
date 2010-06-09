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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

/**
 * A simple {@link DependencyPath} that is created from a list
 */
public class SimpleDependencyPath implements DependencyPath {
    
    /**
     * The list of terms and relations.
     */
    private final List<DependencyRelation> path;

    /**
     * {@code true} if the head of this path is the head node of the first
     * relation.  Conversely, if {@code}, the path begins at the dependent node
     * in the first relation.
     */
    private final boolean isHeadFirst;
        
    /**
     * Creates a {@link SimpleDependencyPath} starting at the head node of the
     * first relation in the list.
     */
    public SimpleDependencyPath(List<DependencyRelation> path) {
        this(path, true);
    }

    /**
     * Creates a {@link SimpleDependencyPath} that starts at either the head
     * node or dependent node of the first relation in the list.
     */
    public SimpleDependencyPath(List<DependencyRelation> path, 
                                boolean isHeadFirst) {
        if (path == null || path.size() == 0)
            throw new IllegalArgumentException("Cannot provide empty path");
        this.path = path;        
        this.isHeadFirst = isHeadFirst;
    }

    /**
     * Creates new {@link SimpleDependencyPath} as a copy of the provided path.
     */
    public SimpleDependencyPath(DependencyPath path) {
        if (path == null || path.length() == 0)
            throw new IllegalArgumentException("Cannot provide empty path");

        // Special case if we're cloning an instances of this class
        if (path instanceof SimpleDependencyPath) {
            SimpleDependencyPath p = (SimpleDependencyPath)path;
            // Copy over the relations
            this.path = new ArrayList<DependencyRelation>(p.path);
            // Ensure the iteration order stays the same.
            this.isHeadFirst = p.isHeadFirst;
        }
        else {
            this.path = new ArrayList<DependencyRelation>(path.length());
            for (DependencyRelation r : path)
                this.path.add(r);
            // Decide whether the provided path starts with the head element of
            // the relation not
            DependencyRelation r = path.firstRelation();
            DependencyTreeNode n = path.first();
            this.isHeadFirst = r.headNode().equals(n);
        }
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode first() {
        DependencyRelation r = path.get(0);
        // Check whether the relation path starts at either the head or
        // dependent node
        return (isHeadFirst) ? r.headNode() : r.dependentNode();
    }

    /**
     * {@inheritDoc}
     */
    public DependencyRelation firstRelation() {
        return path.get(0);
    }    

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode getNode(int position) {
        if (position < 0 || position > path.size())
            throw new IndexOutOfBoundsException("Invalid node: " + position);
        // Special case for if only one relation exists in the path
        if (path.size() == 1)
            return ((isHeadFirst && position == 1) 
                    || (!isHeadFirst && position == 0))
                ? path.get(0).dependentNode() 
                : path.get(0).headNode();
        path.get(position);
        DependencyRelation prev = path.get(position-1);
        DependencyRelation cur = path.get(position);
        return getNextNode(prev, cur);
    }

    /**
     * Given the nodes in the previous relation, determine which of the nodes in
     * the next relation is new and return that.
     *
     * @param prev the dependency relation that was previously seen in the path
     * @param cur the current dependency relation 
     */
    private DependencyTreeNode getNextNode(DependencyRelation prev,
                                           DependencyRelation cur) {
        return (prev.headNode() == cur.headNode() 
                || prev.dependentNode() ==  cur.headNode())
            ? cur.dependentNode()
            : cur.headNode();
    }

    /**
     * {@inheritDoc}
     */
    public String getRelation(int position) {
        if (position < 0 || position > (path.size() - 1))
            throw new IndexOutOfBoundsException("Invalid relation: " +position);
        DependencyRelation r = path.get(position);
        return r.relation();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DependencyRelation> iterator() {
        return path.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode last() {
        if (path.size() == 1)
            return (isHeadFirst)
                ? path.get(0).dependentNode()
                : path.get(0).headNode();
        DependencyRelation prev = path.get(path.size() - 2);
        DependencyRelation last = path.get(path.size() - 1);
        return getNextNode(prev, last);
    }

    /**
     * {@inheritDoc}
     */
    public DependencyRelation lastRelation() {
        return path.get(path.size() - 1);
    }    
    
    /**
     * {@inheritDoc}
     */
    public int length() {
        // +1 because path.size() == number of relations.  There is one more
        // node than there are relations.
        return path.size() + 1;
    }    
}