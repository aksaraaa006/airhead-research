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

    private static final AlphabeticComparator ALPHABETIC_COMPARATOR = 
	new AlphabeticComparator();

    private final RootNode<V> rootNode;

    private int size = 0;
    
    public TrieMap() {
	// create the root mapping with an alphabetically sorted order
	rootNode = new RootNode<V>();
	size = 0;
    }

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

    public V get(Object key) {
	checkKey(key);
	
	CharSequence cs = (CharSequence)key;
	Node<V> n = lookup(cs);
	return (n == null) ? null : n.value;
    }

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
	
	int length = key.length();

	// special case for empty string
	if (length == 0) {
	    return rootNode;
	}

	char head = key.charAt(0);
	Node<V> cur = rootNode.getChild(head);
	for (int pos = 0; pos < length; ++pos) {

	    // see if the current node as the same character as the current
	    // position in the string
	    if (cur == null || key.charAt(pos) != cur.head) 
		break;
		
	    // case 1: this node is a terminal node, and we have reached it
	    if (pos + 1 == length) {
		return (cur.isTerminal()) ? cur : null;
	    }

	    // case 2: this node points to a node with the next sequence
	    Node<V> next = cur.getChild(key.charAt(pos + 1));
	    if (next != null) {
		cur = next;
	    }
	    else {
		// case 3: this node has the same tail as the rest of the key
		if (cur.tailMatches(key.subSequence(pos + 1, length))) {
		    return cur;
		}
		// case 4: this node had no children and the tail was not a
		// match, so no Node was found
		else {
		    return null;
		}
	    }
	}
	return null;

    }

    /**
     * Adds the mapping from the provided key to the value.
     *
     * @param key
     * @param value
     *
     * @throws NullPointerExceptio
     * @throws IllegalArgumentException
     */
    public V put(CharSequence key, V value) {

	if (key == null || value == null) {
	    throw new NullPointerException("keys and values cannot be null");
	}
	int keyLength = key.length();
	
	// special case for empty string
	if (keyLength == 0) {
	    V old = rootNode.setValue(value);
	    return old;
	}
	
	Node<V> curParent = rootNode;

	for (int keyChar = 0; keyChar < keyLength; ++keyChar) {

	    Map<Character,Node<V>> curTransition = curParent.getChildren();

	    // Check for an edge to a new node in the trie based on the current
	    // character of the key
	    char curChar = key.charAt(keyChar);
	    Node<V> n = curTransition.get(curChar);

	    // if there wasn't a transition, then add this key mapping at the
	    // current location
	    if (n == null) {
		Node<V> newNode = new Node<V>(curChar, key, keyChar + 1, value);
		curParent.addChild(curChar, newNode);		
		size++;
		return null;
	    }

	    int curTailIndex = keyChar + 1;

	    // otherwise determine if the node matches the key.
	    CharSequence nodeTail = n.getTail();
	    int charOverlap = overlap(key, curTailIndex, nodeTail, 0);

	    // if the entire remaining part of the key matches the tail of this
	    // node, then replace the value for this node.  
	    if (charOverlap == keyLength - curTailIndex &&
		    charOverlap == nodeTail.length()) {		
		return replaceValue(n, value);
	    }

	    // if some part did overlap, create intermediate nodes and split the
	    // trie until the two keys can be discriminated.  Note that this
	    // condition covers the case where head matches the node's head and
	    // the tail is the empty string.
	    else if (charOverlap > 0 || (keyLength - curTailIndex) == 0) {
		CharSequence newTail = 
		    key.subSequence(curTailIndex + charOverlap, keyLength);
		splitAndInsert(n, charOverlap, newTail, value);
		
		break;
	    }

	    // otherwise, if no part overlapped, then continue and see if the
	    // next key character can be used to transition to a new Node
	    else {
		curTransition = n.getChildren();
		curParent = n;
	    }
	}
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
     *
     * @param key
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
    
    public int size() {
	return size;
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
    private void splitAndInsert(Node<V> node, int nodesToCreate,
				CharSequence newTail, V value) {

	// since a new mapping is being added, increment the size
	size++;

	// Cur keeps track of the last child that is created in the chain
	Node<V> cur = node;

	// Keep track of the characters that were originally the tail of the
	// node that is being spit.
	CharSequence oldTail = node.getTail();
	
	// Create |nodesToCreate| new child nodes from the current node.  At
	// the end, create two new nodes to point to the remaining portion of
	// the previous tail and the new tail from that point.
	for (int i = 0; i < nodesToCreate; ++i) {
	    char c = oldTail.charAt(i);
	    Node<V> n = new Node<V>(c, "", null);
	    cur.addChild(c, n);
	    cur = n;
	}
	
	// Case: |newTail| > |oldTail| == nodesToCreate
	// 
	// If the split was for the entire length of the old tail, then the
	// final node should actually contain the value.
	if (nodesToCreate == oldTail.length()) {
	    cur.value = node.value;

	    char newChildChar = newTail.charAt(0);
	    Node<V> newNode = new Node<V>(newChildChar, newTail, 1, value);
	    cur.addChild(newChildChar, newNode);
	}

	// Case: |oldTail| > |newTail| == nodesToCreate
	//
	// If the new key-value mapping points directly to ending child that was
	// created, set the value based on the new key-value mapping and then
	// add a child for the remaining portion of the old tail.
	else if (newTail.length() == 0) {
	    char old = oldTail.charAt(nodesToCreate);
	    Node<V> remainingTail = 
		new Node<V>(old, oldTail, nodesToCreate + 1, node.value);
	    cur.value = value;
	    cur.addChild(old, remainingTail);
	    cur.setTail("");
	}

	// Case: |oldTail| > nodesToCreate && |newTail| > nodesToCreate
	//
	// If both the oldTail and newTail had more characters than were
	// required to distinguish them, create two new children at then end of
	// the sequence, one for each child.
	else {
	    char old = oldTail.charAt(nodesToCreate);	   
	    Node<V> remainingTail = 
		new Node<V>(old, oldTail, nodesToCreate + 1, node.value);
	    char newChildChar = newTail.charAt(0);
	    Node<V> newNode = new Node<V>(newChildChar, newTail, 1, value);
	    
	    cur.addChild(old, remainingTail);
	    cur.addChild(newChildChar, newNode);
	}

	// If the above procedure didn't add a value to the node that was being
	// split, then remove the old value and mark the node as no longer being
	// a terminal node
	if (node != cur) {
	    node.setTail("");
	    node.value = null;
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

	private char head;

	private char[] tailChars;

	private V value;

	protected Map<Character,Node<V>> children;

	/**
	 * Constructs a new {@code Node}.
	 * 
	 * @param head the character for this node in the trie
	 * @param seq the {@code CharSequence} to be stored at this node in the
	 *        trie
	 * @param tailStart the index into {@code seq} that denotes the
	 *        remaining characters of {@code seq} that are not a part of the
	 *        path to this node
	 * @param value the value associated with the key {@code seq}
	 */
	// IMPLEMENTATION NOTE: we use seq and tailStart instead of passing in
	// the remaining characters to avoid an unnecessary copy from
	// subSequence.  This class stores the remaining characters in a char[]
	// to save wasted space from unnecessary object overhead.
	public Node(char head, CharSequence seq, int tailStart, V value) {
	    this.head = head;
	    this.tailChars = toArray(seq, tailStart);
	    this.value = value;
	    children = null; // new TreeMap<Character,Node<V>>(ALPHABETIC_COMPARATOR);
	}

	public Node(char head, CharSequence tail, V value) {
	    this(head, tail, 0, value);
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

	public CharSequence getTail() {
	    return new ArraySequence(tailChars);
	}

	public boolean isTerminal() {
	    return value != null;
	}

	void setTail(CharSequence seq) {
	    tailChars = toArray(seq);
	}

	public V setValue(V newValue) {
	    if (newValue == null) {
		throw new NullPointerException("TrieMap values cannot be null");
	    }
	    V old = value;
	    value = newValue;
	    return old;
	}

	boolean tailMatches(CharSequence seq) {
	    if (seq.length() == tailChars.length) {
		for (int i = 0; i < tailChars.length; ++i) {
		    if (seq.charAt(i) != tailChars[i]) {
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
	    return "(" + head + "," + 
		((tailChars.length == 0) ? "\"\"" : new String(tailChars))
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
	    super('\0', "", null);
	    children = new TreeMap<Character,Node<V>>(ALPHABETIC_COMPARATOR);
	}

	public void clear() {
	    children.clear();
	}

	void setTail(CharSequence seq) {
	    throw new IllegalStateException("cannot set tail on root node");
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
	 *
	 */
	private final Deque<AnnotatedNode<V>> dfsFrontier;

	/**
	 *
	 */
	private Map.Entry<CharSequence,V> next;

	/**
	 * The node previously returned used for supporting the {@code remove}
	 * operation.
	 */
	private Map.Entry<CharSequence,V> prev;

	public TrieIterator() {
	    
	    dfsFrontier = new ArrayDeque<AnnotatedNode<V>>();
	    for (Node<V> n : rootNode.getChildren().values())
		dfsFrontier.push(new AnnotatedNode<V>(n, ""));

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
		for (Node<V> child : n.node.getChildren().values()) {
		    dfsFrontier.push(new AnnotatedNode<V>(
				     child, n.prefix + n.node.head));
		}
		n = dfsFrontier.pollFirst();
	    } 
		
	    if (n == null) {
		next = null;
	    }
	    else {
		next = createEntry(n);
		// add all of the children of the former top of the stack.
		for (Node<V> child : n.node.getChildren().values()) {
		    dfsFrontier.push(new AnnotatedNode<V>(
				     child, n.prefix + n.node.head));
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
	    String key = node.prefix + node.node.head + node.node.getTail();
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
     *
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
     *
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
     *
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