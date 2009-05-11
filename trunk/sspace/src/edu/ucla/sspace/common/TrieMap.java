/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.common;

import java.io.Serializable;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

/**
 * A trie-based {@link Map} implementation that uses {@link CharSequence}
 * instances as keys.  Keys are returned in alphabetical order.
 *
 * <p>
 *
 * Two {@code CharSequence} keys are said to be equal if the characters and
 * ordering for each are identical.  This ensures that different {@code
 * CharSequence} implementations will map to the correct value even if their
 * {@code equals} methods would return {@code false}.
 *
 * This class stores an internal copy of each {@code CharSequence} key. This
 * allows users to use mutable {@code CharSequence} instances as keys and then
 * later mutate those instances without affecting the key-value mapping.
 *
 * <p>
 *
 * This class is optimized for space efficiency.
 *
 * <p>
 *
 * This class does not permit {@code null} keys or values.  However, this class
 * does permit the use of the empty string (a {@code CharSequence} of length
 * {@code 0}).
 *
 * <p>
 *
 * This class is not synchronized.  If concurrent updating behavior is required,
 * the map should be wrapped using {@link
 * java.util.Collections#synchronizedMap(Map)}.  This map will never throw a
 * {@link java.util.ConcurrentModificationException} during iteration.  The
 * behavior is unspecified if the map is modified while an iterator is being
 * used.
 *
 * @author David Jurgens
 */
public class TrieMap<V> extends AbstractMap<CharSequence,V> 
        implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The comparator used by all {@link Node} instances to keep their children
     * in alphabetic-sorted order
     */
    private static final AlphabeticComparator ALPHABETIC_COMPARATOR = 
	new AlphabeticComparator();

    /**
     * The root node of this trie
     */
    private final RootNode<V> rootNode;

    /**
     * The size of this trie
     */
    private int size = 0;
    
    /**
     * Constructs an empty trie
     */
    public TrieMap() {
	// create the root mapping with an alphabetically sorted order
	rootNode = new RootNode<V>();
	size = 0;
    }

    /**
     * Constructs this trie, adding all of the provided mappings
     */
    public TrieMap(Map<? extends CharSequence,? extends V> m) {
	this();
	if (m == null) {
	    throw new NullPointerException("map cannot be null");
	}
	putAll(m);
    }

    /**
     * Throws the appropriate {@code Exception} if the provided key is {@code
     * null}, is not an instance of {@code CharSequence}, or is the empty
     * string.
     */
    private void checkKey(Object key) {
	if (key == null) {
	    throw new NullPointerException("keys cannot be null");
	}
	if (!(key instanceof CharSequence)) {
	    throw new ClassCastException("key not an instance of CharSequence");
	}
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
	rootNode.clear();
	size = 0;
    }
    
    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @throws NullPointerException if key is {@code null}
     * @throws ClassCastException if key is not an instance of {@link
     *         CharSequence}
     */
    public boolean containsKey(Object key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	}
	else if (key instanceof CharSequence) {
	    Node<V> n = lookup((CharSequence)key);
	    return n != null && n.isTerminal();
	}
	else {
	    throw new ClassCastException("The provided key does not implement" +
					 " CharSequence: " + key);
	}
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     */
    public Set<Map.Entry<CharSequence,V>> entrySet() {
	return new EntryView();
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    public V get(Object key) {
	checkKey(key);
	
	CharSequence cs = (CharSequence)key;
	Node<V> n = lookup(cs);
	return (n == null) ? null : n.value;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     */
    public Set<CharSequence> keySet() {
	return new KeyView();
    }

    /**
     * Returns the trie node that maps to the provided key or {@code null} if
     * the key is not currently mapped.
     */
    private Node<V> lookup(CharSequence key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	}

	int keyLength = key.length();
	Node<V> n = rootNode;

	for (int curCharIndex = 0; curCharIndex <= keyLength; ++curCharIndex) {	    
	    
	    CharSequence nodePrefix = n.getPrefix();
	    int nextCharIndex = curCharIndex + 1;

	    // if the current node is an intermediate node, then we need to
	    // match all of the prefix characters to use its children.  
	    if (nodePrefix.length() > 0) {
		int charOverlap = overlap(key, curCharIndex, nodePrefix, 0);
		int remainingLength = keyLength - (nextCharIndex);
		int prefixLength = nodePrefix.length();

		// If this this key did not match then entire prefix, then it
		// must not be mapped to some node.
		if (charOverlap < prefixLength) {
		    return null;
		}

		// Otherwise, if all of the characters overlapped, then lookup
		// the transition to the next node based on the next character
		// after the matching prefix
		curCharIndex += prefixLength;
		nextCharIndex = curCharIndex + 1;
	    }

	    // If we have exhausted all the characters in the key, then the
	    // current node is associated with the key.
	    if (curCharIndex == keyLength) {
		return n;
	    }
	    	    
	    // Otherwise, more characters exist, so check to see if there is a
	    // transition from the next sequence of the key to node.  If so, we
	    // use this to keep searching for the key's node
	    else {
		Node<V> child = n.getChild(key.charAt(curCharIndex));

		// if there was no other node to transition to, then the the key
		// must not map to any node in the trie.
		if (child == null) {
		    return null;
		}

		// otherwise, update the current node to the child and repeat
		// the search process
		else {
		    n = child;
		}
	    }	    
	}
	
	// NOTE: we should never reach this case, as the one of the conditions
	// in the for loop will determine where the key goes.
	return null;
    }

    /**
     * Adds the mapping from the provided key to the value.
     *
     * @param key
     * @param value
     *
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public V put(CharSequence key, V value) {

	if (key == null || value == null) {
	    throw new NullPointerException("keys and values cannot be null");
	}

	int keyLength = key.length();
	Node<V> n = rootNode;

	for (int curCharIndex = 0; curCharIndex <= keyLength; ++curCharIndex) {	    

	    CharSequence nodePrefix = n.getPrefix();

	    int nextCharIndex = curCharIndex + 1;

	    // if the current node is an intermediate node, then we need to
	    // match all of the prefix characters to use its children.  
	    if (nodePrefix.length() > 0) {
		int charOverlap = overlap(key, curCharIndex, nodePrefix, 0);
		int remainingLength = keyLength - (nextCharIndex);
		int prefixLength = nodePrefix.length();
		
		// if 0 ore more characters overlapped, add this node to
		// somewhere in the middle
		if (charOverlap < prefixLength) {
		    addIntermediateNode(n,
					charOverlap,
					key,
					curCharIndex,
					value);
		    size++;
		    return null;
		}

		// if all of the characters overlapped, then lookup the
		// transition to the next node based on the next character after
		// the matching prefix
		curCharIndex += prefixLength;
		nextCharIndex = curCharIndex + 1;
	    }

	    // If we have exhausted all the characters in the key, then the
	    // current node should map to the value
	    if (curCharIndex == keyLength) {
		return replaceValue(n, value);
	    }
	    	    
	    // Otherwise, more characters exist, so check to see if there is a
	    // transition from the next sequence of the key to node.  If so, we
	    // use this to keep searching for the key's node
	    else {
		Node<V> child = n.getChild(key.charAt(curCharIndex));
		
		// if there was no other node to transition to, then the
		// remaining portion of the key is used to form a child node of
		// the current node.  Since this is a new mapping, we can return
		// null immediately.
		if (child == null) {
		    addChildNode(n, key, curCharIndex, value);
		    return null;
		}
		// otherwise, update the current node to the child and repeat
		// the search process
		else {
		    n = child;
		}
	    }	    
	}

	// NOTE: we should never reach this case, as the one of the conditions
	// in the for loop will determine where the key goes.
	return null;
    }

    /**
     * Returns the number of overlapping characters between {@code c1} and
     * {@code c2} starting and the provided indices into the sequences.
     *
     * @param c1 a character sequence
     * @param start1 the index into {@code c1} at which the overlap test should
     *        start
     * @param c2 a character sequence
     * @param start2 the index into {@code c2} at which the overlap test should
     *        start
     *
     * @return the number of characters shared by both sequences when viewed at
     *         the provided starting indices.
     */
    private int overlap(CharSequence c1, int start1, 
			CharSequence c2, int start2) {
	int minLength = Math.min(c1.length() - start1, c2.length() - start2);
	int overlap = 0;
	for (; overlap < minLength; ++overlap) {
	    if (c1.charAt(overlap + start1) != c2.charAt(overlap + start2)) {
		break;
	    }
	}
	return overlap;
    }

    /**
     * Removes the mapping for a key from this map if it is present and returns
     * the value to which this map previously associated the key, or {@code
     * null} if the map contained no mapping for the key.
     *
     * @param key key whose mapping is to be removed from the map 
     *
     * @return the previous value associated with key, or {@code null} if there
     * was no mapping for key.
     */
    public V remove(Object key) {
	checkKey(key);
	
	CharSequence cs = (CharSequence)key;
	Node<V> n = lookup(cs);
	if (n != null && n.isTerminal()) {
	    V old = n.value;
	    n.value = null;
	    size--;
	    return old;	    
	}
	else {
	    return (n == null) ? null : n.value;
	}
    }
    
    /**
     * Replaces the value of the provided {@code Node} and returns the old value
     * or {@code null} if one was not set.
     *
     * @param node
     * @param newValue
     *
     * @return
     */
    private V replaceValue(Node<V> node, V newValue) {
	if (node.isTerminal()) {
	    V old = node.value;
	    node.value = newValue;
	    return old;
	}
	// the node wasn't already a terminal node (i.e. this char
	// sequence is a substring of an existing sequence), mark
	// this node as terminal
	else {
	    node.value = newValue;
	    size++;
	    return null; // no old value
	}
    }

    /**
     * Returns the number of key-value mappings in this trie.
     */
    public int size() {
	return size;
    }


    /**
     * Adds a child {@link Node} node to the provided parent using the {@code
     * char} at the transition index to determine the link.
     *
     * @param parent the node to which the child will be added
     * @param key the key that is being mapped to the provided value
     * @param transitionCharIndex the character index in {@key} to which the new
     *        node should be linked from the parent node
     * @param value the value being mapped to the provided key
     */
    private void addChildNode(Node<V> parent,
			      CharSequence key,
			      int transitionCharIndex,
			      V value) {
	char transitionChar = key.charAt(transitionCharIndex);
	Node<V> child = new Node<V>(key, transitionCharIndex + 1, value);
	parent.addChild(transitionChar, child);
	size++;
    }

    /**
     * Creates a series of children under the provided node, moving the value
     * that was mapped to this node to the appropriate terminal node in the
     * series and finally creating a new node at the end to hold the new
     * key-value mapping.
     *
     * @param node a node under which a new key-value mapping is being added
     * @param nodesToCreate the number of characters that overlap between the
     *        key that maps to {@code} and the new key.  This determines the
     *        number of nodes that are needed to distinguish the two keys.
     * @param newTail the remaining portion of the new key that is being added,
     *        which will be appended to a new child node at the end of the
     *        sequence.
     * @param value the value for the new key-value mapping being added to the
     *        map
     */
    private void addIntermediateNode(Node<V> original, 
				     int numOverlappingCharacters,
				     CharSequence key, 
				     int indexOfStartOfOverlap,
				     V value) {	

	// get the current prefix for the node
	char[] originalPrefix = original.prefix;
		    
	// create the new prefix for the original node, which will be all the
	// non-overlapping characters.  Note that the first distinguish
	// character will be used as the map, so the prefix is shorter.
	char distinguishing = originalPrefix[numOverlappingCharacters];
	char[] remainingPrefix = 
	    Arrays.copyOfRange(originalPrefix, numOverlappingCharacters + 1,
			       originalPrefix.length);
	char[] overlappingPrefix =
	    Arrays.copyOfRange(originalPrefix, 0, numOverlappingCharacters);
	
	// Create a new Node, which will be a copy of the original node with
	// the remaining prefix.  This new Node will become a child once the
	// new key-value mapping is put in place
	Node<V> child = new Node<V>(remainingPrefix, original.value);
	// copy over the children as well
	child.children = original.children;

	// 
	original.prefix = overlappingPrefix;
	original.children = 
	    new TreeMap<Character,Node<V>>(ALPHABETIC_COMPARATOR);
	original.addChild(distinguishing, child);

	// Determine whether the remaining portion of the key was a substring of
	// the original prefix, or whether the two keys diverge but shared a
	// common, overlapping prefix
	int remainingKeyChars = key.length() - indexOfStartOfOverlap;
	
	// if the key was a substring, rework the original node to have the new
	// value and shorter prefix consisting of the overlapping characters
	if (numOverlappingCharacters == remainingKeyChars) {

	    original.value = value;	    
	}
	// Otherwise, the keys diverge, so create a new intermediate node with
	// no value mapping but that points to both the old original node and
	// a new node that contains the new value
	else {
	    int prefixStart = indexOfStartOfOverlap + 
		numOverlappingCharacters + 1;
	    char mappingKey = key.charAt(indexOfStartOfOverlap + 
					 numOverlappingCharacters);
	    char[] remainingKey = new char[key.length() - prefixStart];
	    for (int i = 0; i < remainingKey.length; ++i) {
		remainingKey[i] = key.charAt(prefixStart + i);
	    }
	    Node<V> newMapping = new Node<V>(remainingKey, value);
	    original.addChild(mappingKey, newMapping);

	    original.value = null;
	}	       
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    public Collection<V> values() {
	return new ValueView();
    }

    /**
     * The internal node class for creating the trie.
     */
    private static class Node<V> implements Serializable {

	private static final long serialVersionUID = 1;

	/**
	 * If this this node is a leaf node in the trie, these characters are
	 * the suffix of the string ending at this node; else, these characters
	 * a common substring prefix shared by the children of this node.
	 */
	private char[] prefix;

	/**
	 * The value mapped to the key that is represented by the path to this
	 * node.
	 */
	private V value;

	/**
	 * A mapping from each character transition to the child that has a key
	 * with that character.  If this node has node children, this map may be
	 * {@code null}.
	 */
	protected Map<Character,Node<V>> children;

	/**
	 * Constructs a new {@code Node}.
	 * 
	 * @param seq the {@code CharSequence} to be stored at this node in the
	 *        trie
	 * @param tailStart the index into {@code seq} that denotes the
	 *        preceding characters of {@code seq} that are not a part of the
	 *        path to this node
	 * @param value the value associated with the key {@code seq}
	 */
	// IMPLEMENTATION NOTE: we use seq and tailStart instead of passing in
	// the remaining characters to avoid an unnecessary copy from
	// subSequence.  This class stores the remaining characters in a char[]
	// to save wasted space from unnecessary object overhead.
	Node(CharSequence seq, int prefixStart, V value) {
	    this(toArray(seq, prefixStart), value);
	}

	Node(char[] prefix, V value) {
	    this.prefix = prefix;
	    this.value = value;
	    children = null;
	}

	public Node(CharSequence prefix, V value) {
	    this(prefix, 0, value);
	}

	public void addChild(char c, Node<V> child) {
	    if (children == null) {
		children = 
		    new TreeMap<Character,Node<V>>(ALPHABETIC_COMPARATOR);
	    }
	    children.put(c, child);
	}
	
	public Node<V> getChild(char c) {
	    return (children == null) ? null : children.get(c);
	}

	public Map<Character,Node<V>> getChildren() {
	    return (children == null) 
		? new HashMap<Character,Node<V>>() : children;
	}

	public CharSequence getPrefix() {
	    return new ArraySequence(prefix);
	}
	
	public boolean isTerminal() {
	    return value != null;
	}

	void setTail(CharSequence seq) {
	    prefix = toArray(seq);
	}

	public V setValue(V newValue) {
	    if (newValue == null) {
		throw new NullPointerException("TrieMap values cannot be null");
	    }
	    V old = value;
	    value = newValue;
	    return old;
	}

	boolean prefixMatches(CharSequence seq) {
	    if (seq.length() == prefix.length) {
		for (int i = 0; i < prefix.length; ++i) {
		    if (seq.charAt(i) != prefix[i]) {
			return false;
		    }
		}
		return true;
	    }
	    return false;
	}

	private static char[] toArray(CharSequence seq) {
	    return toArray(seq, 0);
	}

	private static char[] toArray(CharSequence seq, int start) {
	    char[] arr = new char[seq.length() - start];
	    for (int i = 0; i < arr.length; ++i) {
		arr[i] = seq.charAt(i + start);
	    }
	    return arr;
	}

	public String toString() {
	    return "(" +
		((prefix.length == 0) ? "\"\"" : new String(prefix))
		+ ": " + value + ", children: " + children + ")";
	}
    }

    /**
     * A special-case subclass for the root node of the trie, whose key the
     * empty string.
     */
    private static class RootNode<V> extends Node<V> {
	
	private static final long serialVersionUID = 1;

	public RootNode() {
	    super("", null);
	    children = new TreeMap<Character,Node<V>>(ALPHABETIC_COMPARATOR);
	}

	public void clear() {
	    children.clear();
	}

	void setTail(CharSequence seq) {
	    throw new IllegalStateException("cannot set tail on root node");
	}

	public V setValue(V newValue) {
	    return super.setValue(newValue);
	}

	boolean tailMatches(CharSequence seq) {
	    return seq.length() == 0;
	}	
    }

    /**
     * An immutable {@code CharSequence} implementation backed by an array.
     */
    private static class ArraySequence implements CharSequence {

	private final char[] sequence;

	public ArraySequence(char[] sequence) {
	    this.sequence = sequence;
	}
	
	public char charAt(int i) {
	    return sequence[i];
	}

	public boolean equals(Object o) {
	    if (o instanceof CharSequence) {
		CharSequence cs = (CharSequence)o;
		if (cs.length() != sequence.length) {
		    return false;
		}
		for (int i = 0; i < sequence.length; ++i) {
		    if (cs.charAt(i) != sequence[i]) {
			return false;
		    }
		}
		return true;
	    }
	    return false;
	}

	public int hashCode() {
	    return Arrays.hashCode(sequence);
	}

	public int length() {
	    return sequence.length;
	}

	public CharSequence subSequence(int start, int end) {
	    return new ArraySequence(Arrays.copyOfRange(sequence, start, end));
	}
	
	public String toString() {
	    return new String(sequence);
	}
    }
    
    /**
     * An internal decorator class on {@link TrieMap.Node} that records that
     * {@code Node} path and associated {@link CharSequence} prefix that leads
     * to a {@code Node}.  This class is only used by
     * {@link TrieMap.TrieIterator} class for constructing correct {@link
     * Map.Entry} instances.
     *
     * <p>
     *
     * This class allows for full on-the-fly recovery of the key of a map based
     * on a path in the trie.  We add a field in this class instead of directly
     * in the {@code Node} class to save memory.  The addition of a prefix would
     * negate all the memory savings of the trie.  However, {@code
     * AnnotatedNode} instances are short-lived (typically, only during
     * iteration), and therefore incur little memory overhead for reconstructing
     * the full key.
     *
     * @see TrieMap.TrieIterator
     */
    private static class AnnotatedNode<V> {
	
	private final String prefix;
	
	private final Node<V> node;
	
	public AnnotatedNode(Node<V> node, String prefix) {
	    this.prefix = prefix;
		this.node = node;
	}

	public String toString() {
	    return node.toString();
	}
    }    

    /**
     *
     */
    private abstract class TrieIterator<E>
	implements Iterator<E> {
	
	/**
	 * The a queue of nodes that reflect the current state of the
	 * depth-first traversal of the trie that is being done by this
	 * iterator.
	 */
	private final Deque<AnnotatedNode<V>> dfsFrontier;

	/**
	 * The next entry to return or {@code null} if there are no further
	 * entries.
	 */
	private Map.Entry<CharSequence,V> next;

	/**
	 * The node previously returned used for supporting the {@code remove}
	 * operation.
	 */
	private Map.Entry<CharSequence,V> prev;

	public TrieIterator() {
	    
	    dfsFrontier = new ArrayDeque<AnnotatedNode<V>>();
	    for (Entry<Character,Node<V>> child : 
		     rootNode.getChildren().entrySet())
		dfsFrontier.push(new AnnotatedNode<V>(child.getValue(),
						      child.getKey().toString()));
	    next = null;
	    prev = null;

	    // search for the first termial node
	    advance();
	}

	/**
	 * Increments the current state of the depth-first traversal of the trie
	 * and sets {@link TrieMap.TrieIterator#next} to the next terminal node
	 * in the trie or {@code null} if no such node exists.
	 */
	private void advance() {

	    AnnotatedNode<V> n = dfsFrontier.pollFirst();

	    // repeatedly expand a new frontier until we either run out of nodes
	    // or we find a terminal node
	    while (n != null && !n.node.isTerminal()) {
		// remove the top of the stack and add its children
 		for (Entry<Character,Node<V>> child : 
			 n.node.getChildren().entrySet()) {
 		    dfsFrontier.push(new AnnotatedNode<V>(
				     child.getValue(), n.prefix 
				     + n.node.getPrefix() + child.getKey()));
 		}
		n = dfsFrontier.pollFirst();
	    } 
		
	    if (n == null) {
		next = null;
	    }
	    else {
		next = createEntry(n);
		// add all of the children of the former top of the stack.
 		for (Entry<Character,Node<V>> child : 
			 n.node.getChildren().entrySet()) {
 		    dfsFrontier.push(new AnnotatedNode<V>(
			child.getValue(), n.prefix 
			+ n.node.getPrefix() + child.getKey()));
 		}
	    }
	}

	/**
	 * Creates a new {@code Entry} that is backed by the provided {@code
	 * AnnotatedNode}.  Changes to the returned entry are pass through to
	 * the node.
	 */
	private Map.Entry<CharSequence,V> createEntry(AnnotatedNode<V> node) {
	    // determine the String key that makes up this entry based on what
	    // nodes have been traversed thus far.
	    String key = node.prefix + node.node.getPrefix();
	    return new TrieEntry<V>(key, node.node);
	}
	
	/**
	 * Returns {@code true} if this iterator has more elements.
	 */
	public boolean hasNext() {
	    return next != null;
	}

	/**
	 * Returns the next {@code Entry} from the trie.
	 */
	public Map.Entry<CharSequence,V> nextEntry() {
	    if (next == null) {
		throw new NoSuchElementException("no further elements");
	    }
	    prev = next;
	    advance();
	    return prev;
	}

	/**
	 * Removes from the underlying collection the last element returned by
	 * the iterator.
	 *
	 * @throws IllegalStateException if the {@code next} method has not yet
	 *         been called, or the {@code remove} method has already been
	 *         called after the last call to the {@code next} method.
	 */
	public void remove() {
	    if (prev == null) {
		throw new IllegalStateException();
	    }
	    TrieMap.this.remove(prev.getKey());
	    prev = null;
	}
    }


    private class EntryIterator 
	    extends TrieIterator<Map.Entry<CharSequence,V>> {

	public Map.Entry<CharSequence,V> next() {
	    return nextEntry();
	}
	
    }

    private class KeyIterator extends TrieIterator<CharSequence> {

	public CharSequence next() {
	    return nextEntry().getKey();
	}
	
    }

    private class ValueIterator extends TrieIterator<V> {

	public V next() {
	    return nextEntry().getValue();
	}
	
    }
    

    /**
     * An implementation of {@link Map.Entry} backed by a {@link TrieMap.Node}
     * instance.  Changes to instances of this class are reflected in the trie.
     */
    private static class TrieEntry<V> 
	    extends AbstractMap.SimpleEntry<CharSequence,V> {
	
	private static final long serialVersionUID = 1;

	private final Node<V> node;

	public TrieEntry(String key, Node<V> node) {
	    super(key, node.value);
	    this.node = node;
	}

	public V getValue() {
	    return node.value;
	}

	public V setValue(V newValue) {
	    return node.setValue(newValue);
	}
    }
    
    /**
     * A {@link Set} view of the keys contained in this trie.
     */
    private class KeyView extends AbstractSet<CharSequence> {
	
	public void clear() {
	    clear();
	}

	public boolean contains(Object o) {
	    return containsKey(o);
	}

	public Iterator<CharSequence> iterator() {
	    return new KeyIterator();
	}
	
	public boolean remove(Object o) {
	    return TrieMap.this.remove(o) != null;
	}

	public int size() {
	    return size;
	}
    }

    /**
     * A {@link Collection} view of the values contained in this trie.
     */
    private class ValueView extends AbstractCollection<V> {
	
	public void clear() {
	    clear();
	}

	public boolean contains(Object o) {
	    return containsValue(o);
	}
	
	public Iterator<V> iterator() {
	    return new ValueIterator();
	}
	
	public int size() {
	    return size;
	}
    }

    /**
     * A {@link Set} view of the key value mappings contained in this trie.
     */
    private class EntryView extends AbstractSet<Map.Entry<CharSequence,V>> {
	
	public void clear() {
	    clear();
	}

	public boolean contains(Object o) {
	    if (o instanceof Map.Entry) {
		Map.Entry e = (Map.Entry)o;
		Object key = e.getKey();
		Object val = e.getValue();
		Object mapVal = TrieMap.this.get(key);
		return mapVal == val || (val != null && val.equals(mapVal));
	    }
	    return false;
	}

	public Iterator<Map.Entry<CharSequence,V>> iterator() {
	    return new EntryIterator();
	}
	
	public int size() {
	    return size;
	}
    }

    /**
     * A {@code Comparator} implementations that can be used to sort characters
     * alphabetically.
     */
    private static final class AlphabeticComparator 
	implements Comparator<Character>, Serializable {
	
	private static final long serialVersionUID = 1;

	public int compare(Character c1, Character c2) {
	    return -(c1.compareTo(c2));
	}
    }
   
}