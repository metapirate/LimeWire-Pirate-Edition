
package org.limewire.collection;

/**
 *  Contains an int property key and its corresponding value type {@link Object}.
 <pre>
    Pair p1 = new Pair(0, "Abby");
    System.out.println("Compare Abby to Bob: " + p1.compareTo(new Pair(10, "Bob")));
    System.out.println("Get element p1: " + p1.getElement());   

    Output:
        Compare Abby to Bob: -10
        Get element p1: Abby
     
 </pre>
 */
public class Pair implements Comparable<Pair> {
    private int _key;
    private Object _elem;
	
    public Pair (int key, Object elem) {
		_key = key;
		_elem = elem;
    }
    
    public int getKey() {return _key;}
    public Object getElement() {return _elem;}
    public void setKey(int key) {_key = key;}
    public void setElement(Object elem) {_elem = elem;}

	public int compareTo(Pair p) {
		return _key - p._key;
	}
}
