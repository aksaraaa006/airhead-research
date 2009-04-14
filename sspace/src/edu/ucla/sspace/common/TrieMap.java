package edu.ucla.sspace.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
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
	    return lookup((CharSequence)key) == null;
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
	return null;
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
	return null;
    }

    private Node<V> lookup(CharSequence key) {
	int length = key.length();
	Node<V> cur = root;
	for (int pos = 0; pos < length; ++pos) {
	    // see if the current node as the same character as the current
	    // position in the string
	    if (key.charAt(pos) == cur.head) {
		
		// case 1: this node is a terminal node, and we have reached it
		if (pos + 1 == length) {
		    return (cur.isTerminal) ? cur : null;
		}
		else {
		    // case 2: this node points to a node with the next sequence
		    Node<V> next = cur.getChild(key.charAt(pos));
		    if (next != null) {
			cur = next;
		    }
		    else {
			// case 3: this node has the same tail as the rest of
			// the key
			if (cur.tail != null &&  
			        cur.tail.equals(key.subSequence(pos, length))) {
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
		    // case 2: This node points to a child with the next
		    // characte in the sequence, so continue to iterate.
		    Node<V> next = cur.getChild(key.charAt(pos));
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
			// move to the next character in the key sequence in
			// order to compare it to the tail
			pos++;

			// loop over the tail to see how much of it overlaps
			for (; pos < length && tailPos < tailLength;
			         ++tailPos, ++pos) {
			    if (cur.tail.charAt(tailPos) != key.charAt(pos)) {
				break;
			    }
			}
			
			// case 3: If we iterated all the way through the tail,
			// this node has the same tail as the rest of the key
			if (tailPos == tailLength) {
			    return replaceValue(cur, value);
			}
			
			// we will need to split the node at this point based on
			// the currently consider key
			else {
			    // create the tail sequence that will be used to
			    // create a new Node after splitting the current
			    // node tailPos number of times.
			    CharSequence newTail = key.
				subSequence(pos - tailPos, length);
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
	if (node.isTerminal) {
	    V old = node.value;
	    node.value = newValue;
	    return old;
	}
	// the node wasn't already a terminal node (i.e. this char
	// sequence is a substring of an existing sequence), mark
	// this node as terminal
	else {
	    node.isTerminal = true;
	    node.value = newValue;
	    return null; // no old value
	}
    }
    
    public int size() {
	return size;
    }

    private void splitAndInsert(Node<V> node, int charsToSeparate,
				CharSequence newTail, V value) {
	Node<V> cur = node;
	CharSequence oldTail = node.tail;

	// create |charsToSeparate| new child nodes from the current node.  At
	// the end, create two new nodes to point to the remaining portion of
	// the previous tail and the new tail from that point.
	for (int i = 0; i < charsToSeparate; ++i) {
	    char c = oldTail.charAt(i);
	    Node<V> n = new Node<V>(c, null, null);
	    cur.children.put(c, n);
	    cur = n;
	}
	
	char old = oldTail.charAt(charsToSeparate);
	Node<V> remainingTail = 
	    new Node<V>(old, oldTail.subSequence(charsToSeparate, 
						 oldTail.length()),
			node.value);
	char newChildChar = newTail.charAt(0);
	Node<V> newNode = new Node<V>(newChildChar, 
				      newTail.subSequence(1, newTail.length()),
				      value);
	
	cur.children.put(old, remainingTail);
	cur.children.put(newChildChar, newNode);
	
	// Remove the old value and mark the node as no longer being a terminal
	// node
	node.isTerminal = false;
	node.value = null;
    }

    public Collection<V> values() {
	return null;
    }

    private static final class Node<V> {

	private char head;

	private CharSequence tail;

	private V value;

	private Map<Character,Node<V>> children;

	private boolean isTerminal;

	public Node(char head, CharSequence tail, V value) {
	    
	    this.value = value;
	    children = new HashMap<Character,Node<V>>();
	    isTerminal = true;
	}

	public Node<V> getChild(char c) {
	    return children.get(c);
	}
    }
    
}