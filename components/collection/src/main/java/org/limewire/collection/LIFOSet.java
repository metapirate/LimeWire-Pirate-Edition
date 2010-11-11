package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 
 * A hash-based <code>Set</code> ordered with the last added element is the 
 * first item in the list.
 * 
<pre>
    LIFOSet&lt;String&gt; s = new LIFOSet&lt;String&gt;(1);
    s.add("Abby");
    s.add("Bob");
    s.add("Chris");
    s.add("Dan");
    s.add("Eric");  
    s.add("Fred");
    System.out.println(s);

    Output:
        [Fred, Eric, Dan, Chris, Bob, Abby]

</pre>
 */
public class LIFOSet<E> implements Set<E>{
    
    private final Set<E> set;
    
    private final List<E> list;
    
    public LIFOSet() {
        this(50, 0.75F);
    }
    
    public LIFOSet(int initialCapacity){
        this(initialCapacity, 0.75F);
    }
    
    public LIFOSet(int initialCapacity, float loadFactor){
        set = new HashSet<E>(initialCapacity, loadFactor);
        list = new ArrayList<E>(initialCapacity);
    }
    
    /**
     * Adds the given element to the head of the set.
     */
    public boolean add(E o) {
        if(set.add(o)) {
            list.add(o);
            return true;
        }
        list.remove(o);
        list.add(o);
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean ret = false;
        for(E e : c) {
            ret |= add(e);
        }
        return ret;
    }

    public void clear() {
        set.clear();
        list.clear();
    }

    public boolean contains(Object o) {
        return set.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public Iterator<E> iterator() {
        return new LIFOSetIterator();
    }

    public boolean remove(Object o) {
    	if (set.remove(o)) {
    		list.remove(o);
    		return true;
    	}
        return false;
    }
    
    /**
     * Removes the first (eldest) element from the ordered set
     * 
     * @return true if the set was changed
     */
    protected boolean removeEldest() {
        if (list.isEmpty()) {
            return false;
        }
        return remove(0);
    }

    /**
     * Removes the last (newest) element from the ordered set
     * 
     * @return true if the set was changed
     */
    protected boolean removeNewest() {
        if(list.isEmpty()) {
            return false;
        }
        return remove(list.size()-1);
    }
    
    /**
     * Removes the element at the given index from the ordered Set
     */
    protected boolean remove(int index) {
    	return set.remove(list.remove(index));
    }
    
    public boolean removeAll(Collection<?> c) {
        list.removeAll(c);
        return set.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        list.retainAll(c);
        return set.retainAll(c);
    }

    public int size() {
        return set.size();
    }

    public Object[] toArray() {
        List<E> reverse = new ArrayList<E>(list);
        Collections.reverse(reverse);
        return reverse.toArray();
    }

    @SuppressWarnings({"SuspiciousToArrayCall"})
    public <T> T[] toArray(T[] a) {
        List<E> reverse = new ArrayList<E>(list);
        Collections.reverse(reverse);
        return reverse.toArray(a);
    }
    
    @Override
    public String toString() {
        List<E> reverse = new ArrayList<E>(list);
        Collections.reverse(reverse);
        return reverse.toString();
    }



    private class LIFOSetIterator implements Iterator<E> {
        
        private E  current;
        private int index = 0;
        
        public LIFOSetIterator() {
            index = list.size() - 1;
        }
        
        public boolean hasNext() {
            return index >= 0;
        }
        
        public E next() {
            if(index < 0) {
                throw new NoSuchElementException();
            }
            current = list.get(index);
            --index;
            return current;
        }
        
        public void remove() {
            if(index == (list.size() - 1)) {
               throw new IllegalStateException(); 
            }
            set.remove(current);
            E removed = list.remove(index + 1);
            if(removed != current) {
                throw new ConcurrentModificationException();
            }
            current=null;
        }
     }
}
