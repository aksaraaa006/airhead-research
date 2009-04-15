package edu.ucla.sspace.common;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author David Jurgens
 */
public class TrieMap<V> implements Map<CharSequence,V> {

    private Node<V> root;

    private int size = 0;
    
    public TrieMap() {
	root = null;
	size = 0;
    }

    public void clear() {
	root = null;
	size = 0;
    }
    
    /**
     *
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
	    return lookup((CharSequence)key) != null;
	}
	else {
	    throw new ClassCastException("The provided key does not implement" +
					 " CharSequence: " + key);
	}
    }

    public boolean containsValue(Object value) {
	return false;
    }

    public Set<Map.Entry<CharSequence,V>> entrySet() {
	return new EntryView();
    }

    public boolean equals(Object o) {
	return false;
    }

    public V get(Object key) {
	if (key != null && key instanceof CharSequence) {
	    CharSequence cs = (CharSequence)key;
	    Node<V> n = lookup(cs);
	    return (n == null) ? null : n.value;
	}
	else {
	    throw new ClassCastException("The provided key does not implement" +
					 " CharSequence: " + key);
	}
    }

    public int hashCode() {
	return -1;
    }

    public boolean isEmpty() {
	return root == null;
    }

    public Set<CharSequence> keySet() {
	return new KeyView();
    }

    private Node<V> lookup(CharSequence key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	}
	if (root == null) {
	    return null;
	}
	//System.out.println("lookup root: " + root);
	
	int length = key.length();	
	Node<V> cur = root;
	for (int pos = 0; pos < length; ++pos) {
	    //System.out.println("checking node: " + cur);
	    
	    //System.out.printf("%s ?= %s%n", key.charAt(pos), cur.head);

	    // see if the current node as the same character as the current
	    // position in the string
	    if (key.charAt(pos) == cur.head) {
		
		// case 1: this node is a terminal node, and we have reached it
		if (pos + 1 == length) {
		    //System.out.println("examining last character in key");
		    return (cur.isTerminal()) ? cur : null;
		}
		else {
		    // case 2: this node points to a node with the next sequence
		    Node<V> next = cur.getChild(key.charAt(pos + 1));
		    //System.out.printf("next for %s: %s%n", key.charAt(pos + 1), next);
		    if (next != null) {
			cur = next;
		    }
		    else {
			// case 3: this node has the same tail as the rest of
			// the key

// 			System.out.printf("\tchecking remaining key and tail, %s ?= %s%n",
// 					  cur.tail,
// 					  key.subSequence(pos + 1, length));

			if (cur.tail != null &&  
			        cur.tail.equals(key.subSequence(pos + 1, length))) {
			    return cur;
			}
			// case 4: this node had no children and the tail was
			// not a match, so no Node was found
			else {
			    return null;
			}
		    }
		}
	    }
	}
	return null;
    }

    public V put(CharSequence key, V value) {
	
	if (key == null) {
	    throw new NullPointerException("keys cannot be null");
	}
	if (key.length() == 0) {
	    throw new IllegalArgumentException(
		"keys must contain at least one character");
	}

	// Base case: the trie is empty
	if (root == null) {
	    char head = key.charAt(0);
	    CharSequence tail = key.subSequence(1, key.length());
	    root = new Node<V>(head, tail, value);
	    size++;
	    return null;
	}

	int length = key.length();
	Node<V> cur = root;

	for (int pos = 0; pos < length; ++pos) {
	    // see if the current node as the same character as the current
	    // position in the string
	    if (key.charAt(pos) == cur.head) {
		
		// case 1: The char sequence was originally in the trie.  Update
		// the value and return the old one.
		if (pos + 1 == length) {
		    return replaceValue(cur, value);
		}
		else {
		    int remainingKeyPos = pos + 1;
		    
		    // case 2: This node points to a child with the next
		    // characte in the sequence, so continue to iterate.
		    Node<V> next = cur.getChild(key.charAt(remainingKeyPos));
		    if (next != null) {
			cur = next;
		    }
		    // If no children exist, then three cases exist depending on
		    // the overlap between the tail of the node and the
		    // remaining characters in the key.
		    else {
			int tailLength = (cur.tail != null) 
			    ? cur.tail.length() : 0;
			int tailPos = 0;

			// loop over the tail to see how much of it overlaps
			for (; remainingKeyPos < length && tailPos < tailLength;
			         ++tailPos, ++remainingKeyPos) {
// 			    System.out.printf("put: %s ?= %s%n",
// 					      cur.tail.charAt(tailPos),
// 					      key.charAt(remainingKeyPos));
			    if (cur.tail.charAt(tailPos) != key.charAt(remainingKeyPos)) {
				break;
			    }
			}
			
			// case 3: If we iterated all the way through the tail,
			// this node has the same tail as the rest of the key
			if (tailPos == tailLength && remainingKeyPos == length) {
			    return replaceValue(cur, value);
			}
			else {
			    // create the tail sequence that will be used to
			    // create a new Node after splitting the current
			    // node tailPos number of times.
			    CharSequence newTail = key.
				subSequence(remainingKeyPos, length);
			    splitAndInsert(cur, tailPos, newTail, value);
			}
		    }
		}
	    }
	}
	return null;
	
    }

    public void putAll(Map<? extends CharSequence, ? extends V> m) {
	for (Map.Entry<? extends CharSequence, ? extends V> e : m.entrySet()) {
	    put(e.getKey(), e.getValue());
	}
    }

    public V remove(Object key) {
	return null;
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
// 	    System.out.println("replacing value of terminal node");
	    V old = node.value;
	    node.value = newValue;
	    return old;
	}
	// the node wasn't already a terminal node (i.e. this char
	// sequence is a substring of an existing sequence), mark
	// this node as terminal
	else {
// 	    System.out.println("adding value to previously non-terminal node");
	    node.value = newValue;
	    return null; // no old value
	}
    }
    
    public int size() {
	return size;
    }

    private void splitAndInsert(Node<V> node, int nodesToCreate,
				CharSequence newTail, V value) {
	size++;
// 	System.out.println("inserting --");
// 	System.out.println("\t node: " + node);
// 	System.out.println("\t char: " + nodesToCreate);
// 	System.out.println("\t new tail: " + newTail);
	Node<V> cur = node;

	CharSequence oldTail = node.tail;
// 	System.out.println("\t old tail: " + oldTail);
	
	// create |nodesToCreate| new child nodes from the current node.  At
	// the end, create two new nodes to point to the remaining portion of
	// the previous tail and the new tail from that point.
	for (int i = 0; i < nodesToCreate; ++i) {
	    char c = oldTail.charAt(i);
	    Node<V> n = new Node<V>(c, "", null);
// 	    System.out.println("created child: " + n);
	    cur.children.put(c, n);
	    cur = n;
	}
	
// 	System.out.println("\tcur: " + cur);

	// If the split was for the entire length of the old tail, then the
	// final node should actually contain the value.
	if (nodesToCreate == oldTail.length()) {
// 	    System.out.println("old value goes on end node; new tail gets appended");
	    cur.value = node.value;

	    char newChildChar = newTail.charAt(0);
	    Node<V> newNode = new Node<V>(
		newChildChar, newTail.subSequence(1, newTail.length()), value);
	    cur.children.put(newChildChar, newNode);
	}
	// 
	else if (newTail.length() == 0) {
// 	    System.out.println("new value goes on end node; old tail gets appended");
	    char old = oldTail.charAt(nodesToCreate);
	    Node<V> remainingTail = 
		new Node<V>(old, oldTail.subSequence(nodesToCreate + 1, 
						     oldTail.length()),
			    node.value);
	    cur.value = value;
	    cur.children.put(old, remainingTail);
	    cur.tail = "";
	}
	//
	else {
// 	    System.out.println("two tails diverged");
	    char old = oldTail.charAt(nodesToCreate);	   
	    Node<V> remainingTail = 
		new Node<V>(old, oldTail.subSequence(nodesToCreate + 1, 
						     oldTail.length()),
			    node.value);
	    char newChildChar = newTail.charAt(0);
	    Node<V> newNode = new Node<V>(newChildChar, 
					  newTail.subSequence(1, newTail.length()),
					  value);
	    
// 	    System.out.println("head1: " +  old);
// 	    System.out.println("head2: " +  newChildChar);

	    cur.children.put(old, remainingTail);
	    cur.children.put(newChildChar, newNode);	

	}
	
	if (node != cur) {
	    // Remove the old value and mark the node as no longer being a
	    // terminal node
	    node.tail = "";
	    node.value = null;
	}

// 	System.out.println("after insert, node: " + node);	
    }

    public Collection<V> values() {
	return new ValueView();
    }

    private static class Node<V> {

	private char head;

	private CharSequence tail;

	private V value;

	private Map<Character,Node<V>> children;

	public Node(char head, CharSequence tail, V value) {
	    this.head = head;
	    this.tail = tail;
	    this.value = value;
	    children = new HashMap<Character,Node<V>>();
	}

	public Node<V> getChild(char c) {
	    return children.get(c);
	}

	public boolean isTerminal() {
	    return value != null;
	}

	public V setValue(V newValue) {
	    if (newValue == null) {
		throw new NullPointerException("TrieMap values cannot be null");
	    }
	    V old = value;
	    value = newValue;
	    return old;
	}

	public String toString() {
	    return "(" + head + "," + 
		((tail.length() == 0) ? "\"\"" : tail) + ": " + 
		value + ", children: " +
		children + ")";
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
	    if (root != null)
		dfsFrontier.push(new AnnotatedNode<V>(root, ""));

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

	    // REMINDER: simplify this loop behavior
	    AnnotatedNode<V> n = null;

	    // repeatedly expand a new frontier until we either run out of nodes
	    // or we find a terminal node
	    do {
		// remove the top of the stack and add its children
		AnnotatedNode<V> top = dfsFrontier.pollFirst();	    
		if (top == null) {
		    break;
		}
		for (Node<V> child : top.node.children.values()) {
		    dfsFrontier.push(new AnnotatedNode<V>(
				     child, top.prefix + top.node.head));
		}
		n = dfsFrontier.peek();
		// ensure that the stack still has nodes left and check whether
		// the top of the stack is a terminal node.  If so, it should be
		// the next one we return
		System.out.println("advance: n = " + n);
	    } while (n != null && !n.node.isTerminal());
		    
	    next = (n == null) ? null : createEntry(n);
	    System.out.println("next = " + next);
	}

	/**
	 * Creates a new {@code Entry} that is backed by the provided {@code
	 * AnnotatedNode}.  Changes to the returned entry are pass through to
	 * the node.
	 */
	private Map.Entry<CharSequence,V> createEntry(AnnotatedNode<V> node) {
	    // determine the String key that makes up this entry based on what
	    // nodes have been traversed thus far.
	    String key = node.prefix + node.node.head + node.node.tail;
	    return new TrieEntry<V>(key, node.node);
	}
	
	/**
	 *
	 */
	public boolean hasNext() {
	    return next != null;
	}

	/**
	 * Returns the next {@code Entry} from the trie.
	 */
	public Map.Entry<CharSequence,V> nextEntry() {
	    if (next == null) {
		throw new IllegalStateException("no further elements");
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

    
}