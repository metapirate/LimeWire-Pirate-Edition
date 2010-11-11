package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;

/**
 * A <code>Collection</code> that delegates to other collections. <code>MultiCollection</code>
 * behaves as if it is all the collections combined.
 <pre>
    LinkedList&lt;String&gt; l1 = new LinkedList&lt;String&gt;();
    l1.add("Bob");
    l1.add("Abby");
    l1.add("Chris");        
    
    LinkedList&lt;String&gt; l2 = new LinkedList&lt;String&gt;();
    l2.add("Anderson");
    l2.add("Baker");
    l2.add("Cunningham");
                                                    
    System.out.println("MultiCollection contains 'Baker': " + new MultiCollection&lt;String&gt;(l1, l2).contains("Baker"));     

    Output:
        MultiCollection contains 'Baker': true
 </pre>
 * 
 */
public class MultiCollection<T> extends MultiIterable<T> implements Collection<T> {
	
	private final Iterable<? extends Collection<? extends T>> collections;

	public MultiCollection(Collection<? extends T> i1, Collection<? extends T> i2) {
		super(i1, i2);
		List<Collection<? extends T>> l = new ArrayList<Collection<? extends T>>(2);
		l.add(i1);
		l.add(i2);
		this.collections = l;
	}

	@SuppressWarnings("cast")
    public MultiCollection(Collection<? extends T>... collections) {
		super((Iterable<? extends T>[])collections);
		List<Collection<? extends T>> l = new ArrayList<Collection<? extends T>>(collections.length);
        l.addAll(Arrays.asList(collections));
		this.collections = l;
	}
	
	public MultiCollection(Iterable<? extends Collection<? extends T>> collections) {
	    super(collections);
	    this.collections = collections;
	}
	
	public boolean add(T o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();	
	}

	public void clear() {
		for (Collection c : collections)
			c.clear();
	}

	public boolean contains(Object o) {
		for (Collection c : collections) {
			if (c.contains(o))
				return true;
		}
		return false;
	}

    public boolean containsAll(Collection<?> c) { 
        for (Object obj : c) { 
            if (!contains(obj)) 
                return false; 
        } 
        return true; 
    } 

	public boolean isEmpty() {
		for (Collection c : collections) {
			if (!c.isEmpty())
				return false;
		}
		return true;
	}

	public boolean remove(Object o) {
		for (Collection c : collections) {
			if (c.remove(o))
				return true;
		}
		return false;
	}
	public boolean removeAll(Collection<?> c) {
		boolean ret = false;
		for (Object o : c) {
			if (remove(o))
				ret = true;
		}
		return ret;
	}

	public boolean retainAll(Collection<?> c) {
		boolean ret = false;
		for (Collection<? extends T> col : collections) {
			if (col.retainAll(c))
				ret = true;
		}
		return ret;
	}

	public int size() {
		int ret = 0;
		for (Collection c : collections) 
			ret += c.size();
		return ret;
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray() {
		List t = new ArrayList(size());
		for (Collection c : collections) {
			t.addAll(c);
		}
		return t.toArray();
	}

	@SuppressWarnings("unchecked")
	public <B>B[] toArray(B[] a) {
		List<B> t = new ArrayList<B>(size());
		for (Collection c : collections) {
			t.addAll(c);
		}
		return t.toArray(a);
	}
}
