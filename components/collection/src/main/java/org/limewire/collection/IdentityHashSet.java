package org.limewire.collection;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
    A set equivalent of {@link IdentityHashMap}.
<pre>
    IdentityHashSet&lt;String&gt; ihs = new IdentityHashSet&lt;String&gt;(4);

    String s = new String("Abby");
    ihs.add(s);

    if(!ihs.add(s))
        System.out.println("Already contained ("+ s + "); contents: " + ihs);
    
    if(ihs.add("Abby"))
        System.out.println("Was able to add a new String(\"Abby\") since it's a separate object.");

    System.out.println("Contents: " + ihs);
        
    ihs.add("Bob");
    ihs.add("Chris");
    ihs.add("Dan");
    System.out.println("Size is: " + ihs.size() + " contents: " + ihs); 

    Output:
        Already contained (Abby); contents: [Abby]
        Was able to add a new String("Abby") since it's a separate object.
        Contents: [Abby, Abby]
        Size is: 5 contents: [Dan, Chris, Abby, Abby, Bob]
</pre>
 * 
 */
public class IdentityHashSet<E>
extends AbstractSet<E>
implements Set<E>, Cloneable, java.io.Serializable
{
	static final long serialVersionUID = -5024744406713321677L;

	private transient IdentityHashMap<E,Object> map;

//	Dummy value to associate with an Object in the backing Map
	private static final Object PRESENT = new Object();

	/**
	 * Constructs a new, empty set; the backing <tt>IdentityHashMap</tt> instance has
	 * default initial capacity (16) and load factor (0.75).
	 */
	public IdentityHashSet() {
		map = new IdentityHashMap<E,Object>();
	}

	/**
	 * Constructs a new set containing the elements in the specified
	 * collection.  The <tt>IdentityHashMap</tt> is created with default load factor
	 * (0.75) and an initial capacity sufficient to contain the elements in
	 * the specified collection.
	 *
	 * @param c the collection whose elements are to be placed into this set.
	 * @throws NullPointerException   if the specified collection is null.
	 */
	public IdentityHashSet(Collection<? extends E> c) {
		map = new IdentityHashMap<E,Object>(Math.max((int) (c.size()/.75f) + 1, 16));
		addAll(c);
	}



	/**
	 * Constructs a new, empty set; the backing <tt>IdentityHashMap</tt> instance has
	 * the specified initial capacity and default load factor, which is
	 * <tt>0.75</tt>.
	 *
	 * @param      initialCapacity   the initial capacity of the hash table.
	 * @throws     IllegalArgumentException if the initial capacity is less
	 *             than zero.
	 */
	public IdentityHashSet(int initialCapacity) {
		map = new IdentityHashMap<E,Object>(initialCapacity);
	}



	/**
	 * Returns an iterator over the elements in this set.  The elements
	 * are returned in no particular order.
	 *
	 * @return an Iterator over the elements in this set.
	 * @see ConcurrentModificationException
	 */
	@Override
    public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	/**
	 * Returns the number of elements in this set (its cardinality).
	 *
	 * @return the number of elements in this set (its cardinality).
	 */
	@Override
    public int size() {
		return map.size();
	}

	/**
	 * Returns <tt>true</tt> if this set contains no elements.
	 *
	 * @return <tt>true</tt> if this set contains no elements.
	 */
	@Override
    public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns <tt>true</tt> if this set contains the specified element.
	 *
	 * @param o element whose presence in this set is to be tested.
	 * @return <tt>true</tt> if this set contains the specified element.
	 */
	@SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
    public boolean contains(Object o) {
		return map.containsKey(o);
	}

	/**
	 * Adds the specified element to this set if it is not already
	 * present.
	 *
	 * @param o element to be added to this set.
	 * @return <tt>true</tt> if the set did not already contain the specified
	 * element.
	 */
	@Override
    public boolean add(E o) {
		return map.put(o, PRESENT)==null;
	}

	/**
	 * Removes the specified element from this set if it is present.
	 *
	 * @param o object to be removed from this set, if present.
	 * @return <tt>true</tt> if the set contained the specified element.
	 */
	@Override
    public boolean remove(Object o) {
		return map.remove(o)==PRESENT;
	}

	/**
	 * Removes all of the elements from this set.
	 */
	@Override
    public void clear() {
		map.clear();
	}

	/**
	 * Returns a shallow copy of this <tt>IdentityHashSet</tt> instance: the elements
	 * themselves are not cloned.
	 *
	 * @return a shallow copy of this set.
	 */
	@Override
    @SuppressWarnings("unchecked")
    public Object clone() throws CloneNotSupportedException {
		try { 
			IdentityHashSet<E> newSet = (IdentityHashSet<E>) super.clone();
			newSet.map = (IdentityHashMap<E, Object>) map.clone();
			return newSet;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * Save the state of this <tt>IdentityHashSet</tt> instance to a stream (that is,
	 * serialize this set).
	 *
	 * @serialData The capacity of the backing <tt>IdentityHashMap</tt> instance
	 *		   (int), and its load factor (float) are emitted, followed by
	 *		   the size of the set (the number of elements it contains)
	 *		   (int), followed by all of its elements (each an Object) in
	 *             no particular order.
	 */
	private void writeObject(java.io.ObjectOutputStream s)
	throws java.io.IOException {
//		Write out any hidden serialization magic
		s.defaultWriteObject();

		// Write out size
		s.writeInt(map.size());

//		Write out all elements in the proper order.
        for (Object o : map.keySet()) {
            s.writeObject(o);
        }
	}

	/**
	 * Reconstitute the <tt>IdentityHashSet</tt> instance from a stream (that is,
	 * deserialize it).
	 */
	@SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
	throws java.io.IOException, ClassNotFoundException {
//		Read in any hidden serialization magic
		s.defaultReadObject();

		// Read in HashMap capacity and load factor and create backing HashMap

		map = new IdentityHashMap<E,Object>();

		// Read in size
		int size = s.readInt();

//		Read in all elements in the proper order.
		for (int i=0; i<size; i++) {
			E e = (E) s.readObject();
			map.put(e, PRESENT);
		}
	}
}

