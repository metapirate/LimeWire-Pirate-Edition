package org.limewire.collection;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * Represents a set of distinct integers. 
 * Like {@link Set}, <code>SparseIntSet</code> is <b>not synchronized</b>.
 * <p>
 * The integers in this set are sorted in ascending order.  It would be nice for it
 * to implement the SortedSet interface eventually.
 * <p>
 * Optimized to have compact representation when the set is "sparse". (For "dense" sets 
 * you should use IntSet.)  Integers are stored as primitives, so you're guaranteed 4*N bytes
 * of memory used after calling the compact() method.
 * <p>
 * All retrieval and insertion operations run in O(log n) time, where n is the size of the set. 
 * <p>
 * This class is not thread-safe.
 */
public class SparseIntSet extends AbstractSet<Integer> {

    private int[] list = new int[0];
    
    private int size;
    
    private int modCount;
    
    /**
     * Creates an empty set with capacity = 8.
     * (similar to ArrayList)
     */
    public SparseIntSet() {
        this(8);
    }
    
    /**
     * Creates an empty set with the provided initial capacity.
     * @param initialCapacity the initial capacity desired.
     */
    public SparseIntSet(int initialCapacity) {
        ensureCapacity(initialCapacity);
    }
    
    /**
     * Creates a set containing all the elements of the provided
     * collection.
     */
    public SparseIntSet(Collection<? extends Integer> c) {
        addAll(c);
    }
    
    /**
     * Compacts this set to occupy 4*size() bytes of memory.
     */
    public void compact() {
        int oldCapacity = list.length;
        if (size < oldCapacity) {
            int [] copy = new int[size];
            System.arraycopy(list,0,copy,0,size);
            list = copy;
        }
    }
    
    /**
     * @return the actual memory used, in bytes.
     */
    public int getActualMemoryUsed() {
        return list.length * 4;
    }
    
    /**
     * @return the next element that is larger than the provided element
     */
    public int nextSetBit(int fromIndex) {
        int position = binarySearch(fromIndex);
        if (position < 0)
            position = -(position + 1);
        if (position == size)
            return -1;
        return list[position];
    }
    
    @Override
    public boolean add(Integer i) {
        int point = binarySearch(i);
        if (point >= 0)
            return false;
        point = -(point + 1);
        ensureCapacity(size + 1);
        size++;
        System.arraycopy(list,point,list,point+1,size-point-1);
        list[point] = i;
        modCount++;
        return true;
    }
    
    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        if (c.isEmpty())
            return false;
        
        // if we know the other collection is sorted, we can save a lot of time
        if (c instanceof SparseIntSet || c instanceof SortedSet) {
            if (isEmpty()) { 
                fillFromSorted(c);
                return true;
            }
            
            int [] newList = new int[size() + c.size()];
            Iterator<Integer> us = iterator();
            Iterator<? extends Integer> them = c.iterator();
            int index = 0;
            boolean modified = false;
            
            // slightly tweaked mergesort
            int biggest = Integer.MIN_VALUE;
            boolean lastUs = true;
            boolean lastThem = true;
            while (us.hasNext() || them.hasNext()) {
                int a = biggest;
                int b = biggest;
                if (lastUs && us.hasNext())
                    a = us.next();
                if (lastThem && them.hasNext())
                    b = them.next();

                biggest = Math.max(a, b);
                
                // this is where we differ from merge sort since this is a Set
                if (index > 0 && newList[index - 1] == Math.min(a,b))
                    continue;
                
                if (a < b) {
                    newList[index] = a;
                    lastUs = true;
                    lastThem = false;
                } else if ( a > b) {
                    modified = true;
                    newList[index] = b;
                    lastUs = false;
                    lastThem = true;
                } else {
                    newList[index] = a;
                    lastUs = true;
                    lastThem = true;
                }
                index++;
            }
            
            if (newList[index - 1] != biggest)
                newList[index++] = biggest;
            
            list = newList;
            size = index;
            compact();
            return modified;
        } else {
            // use regular addAll
            ensureCapacity(size() + c.size());
            boolean ret = super.addAll(c);
            compact();
            return ret;
        }
    }
    private void fillFromSorted(Collection<? extends Integer> c) {
        list = new int[c.size()];
        for (int i : c) 
            list[++size - 1] = i;
    }
    
    private void ensureCapacity(int minCapacity) {

        int oldCapacity = list.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3)/2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            // minCapacity is usually close to size, so this is a win:
            int [] copy = new int[newCapacity];
            System.arraycopy(list,0,copy,0,size);
            list = copy;
        }
    }
    
    private int binarySearch(int key) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int midVal = list[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
    
    @Override
    public boolean remove(Object o) {
        if (! (o instanceof Integer))
            return false;
        int i = (Integer)o;
        int point = binarySearch(i);
        if (point < 0)
            return false;
        int numMoved = size - point - 1;
        if (numMoved > 0)
            System.arraycopy(list, point+1, list, point,
                     numMoved);
        size--;
        modCount++;
        return true;
    }
    
    @Override
    public boolean contains(Object o) {
        if (! (o instanceof Integer))
            return false;
        int i = (Integer) o;
        int point = binarySearch(i);
        return point >= 0;
    }
    
    @Override
    public Iterator<Integer> iterator() {
        return new ArrayIterator();
    }

    @Override
    public int size() {
        return size;
    }
    
    @SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
    public boolean retainAll(Collection<?> o) {
        
        SparseIntSet toRemove = new SparseIntSet();
        for (int contained : this) {
            if (! o.contains(contained))
                toRemove.add(contained);
        }
        return removeAll(toRemove);
    }
    
    private class ArrayIterator extends UnmodifiableIterator<Integer> {
        
        private int index;
        private final int mod = modCount;
        
        private void checkModification() {
            if (modCount != mod)
                throw new ConcurrentModificationException();
        }
        
        public boolean hasNext() {
            return index < size;
        }

        public Integer next() {
            checkModification();
            if (!hasNext())
                throw new NoSuchElementException();
            return list[index++];
        }
    }
}
