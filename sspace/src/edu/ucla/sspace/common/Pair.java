package sspace.common;

public class Pair<T> {

    public final T x;
    
    public final T y;

    public Pair(T x, T y) {
	this.x = x;
	this.y = y;
    }

    public boolean equals(Object o) {
	if (o == null || !(o instanceof Pair))
	    return false;
	Pair p = (Pair)o;
	return (x == p.x || (x != null && x.equals(p.x))) &&
	    (y == p.y || (y != null && y.equals(p.y)));
    }
    
    public int hashCode() {
	return ((x == null) ? 0 : x.hashCode()) ^
	    ((y == null) ? 0 : y.hashCode());
    }

    public String toString() {
	return "{" + x + ", " + y + "}";
    }
}