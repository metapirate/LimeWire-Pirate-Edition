package org.limewire.collection;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A fixed size HashMap that provides indexed access.  The replacement
 * policy is FIFO and the iteration order is from newest to oldest.
 * <p>
 * Adding an already existing element will postpone the ejection of that
 * element. 
 * <p>
 * It does not support the null element.
 */
public class FixedSizeArrayHashMap<K, V> extends HashMap<K, V> implements RandomAccessMap<K, V> {

    private Buffer<Map.Entry<K, V>> buf;
    
    /** Creates a FixedSizeArrayHashMap with the specified maximum capacity. */
    public FixedSizeArrayHashMap(int maxCapacity) {
        this.buf = new Buffer<Map.Entry<K, V>>(maxCapacity);
    }
    
    /**
     * Creates a new FixedSizeArrayHashMap with the provided maximum capacity
     * and adds elements from the specified Map.  If the capacity is less than
     * the size of the Map, elements will get ejected with FIFO policy.
     */
    public FixedSizeArrayHashMap(int maxCapacity, Map<? extends K, ? extends V> m) {
        this.buf = new Buffer<Map.Entry<K, V>>(maxCapacity);
        putAll(m);
    }
    
    /**
     * Creates a new FixedSizeArrayHashMap with the maximum capacity of the size
     * of the provided Map and adds all the elements of that Map.
     */
    public FixedSizeArrayHashMap(Map<? extends K, ? extends V> m) {
        this(m.size(), m);
    }
    
    public FixedSizeArrayHashMap(int maxCapacity, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.buf = new Buffer<Map.Entry<K, V>>(maxCapacity);
    }

    public FixedSizeArrayHashMap(int maxCapacity, int initialCapacity) {
        super(initialCapacity);
        this.buf = new Buffer<Map.Entry<K, V>>(maxCapacity);
    }
    

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        FixedSizeArrayHashMap<K, V> newSet = (FixedSizeArrayHashMap<K, V>)super.clone();
        try {
            newSet.buf = buf.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return newSet;
    }
    

    @Override
    public void clear() {
        buf.clear();
        super.clear();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if(entrySet == null)
            entrySet = new EntrySet();
        return entrySet;
    }

    public V getValueAt(int i) {
        return buf.get(i).getValue();
    }
    
    public K getKeyAt(int i) {
        return buf.get(i).getKey();
    }  
    
    public Map.Entry<K, V> getEntryAt(int i) {
        return buf.get(i);
    }

    @Override
    public V put(K key, V value) {
        if(key == null || value == null) 
            throw new IllegalArgumentException("null key/value not supported!");
        
        V existing = super.put(key, value);
        if(existing == null) {
            // eject oldest element if size reached
            Map.Entry<K, V> removed = buf.add(new FixedEntry<K, V>(key, value));
            if(removed != null) {
                Object removedValue = super.remove(removed.getKey());
                assert removedValue == removed.getValue();
            }
        } else {
            // refresh this element.
            FixedEntry<K, V> e = new FixedEntry<K, V>(key, value);
            boolean removed = buf.remove(e);
            assert removed;
            Object ejected = buf.add(e);
            assert ejected == null;
        }
        
        return existing;
    }

    @Override
    public V remove(Object key) {
        V removed = super.remove(key);
        if(removed != null) {
            boolean success = buf.remove(new FixedEntry<Object, V>(key, removed));
            assert success;
        }
        return removed;
    }
    
    
    /** A duplicate entry for storage in the buffer. */
    private static class FixedEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;
        FixedEntry(K k, V v) {
            this.key = k;
            this.value = v;
        }
        
        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean equals(Object obj) {
            FixedEntry e = (FixedEntry)obj;
            return key.equals(e.key) && value.equals(e.value);
        }
        
        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
    
    protected Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new ArrayHashMapEntryIterator();
    }
    
    private class ArrayHashMapEntryIterator extends UnmodifiableIterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<K,V>> iter = buf.iterator();

        public boolean hasNext() {
            return iter.hasNext();
        }
        
        public Map.Entry<K, V> next() {
            Map.Entry<K, V> current = iter.next();
            return current;
        }
    }
    

    private transient volatile Set<Map.Entry<K, V>>        entrySet = null;
    
    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        @Override
        public Iterator<Map.Entry<K,V>> iterator() {
            return newEntryIterator();
        }
        
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object key = e.getKey();
            Object value = e.getValue();
            return containsKey(key) && get(key).equals(value);
        }
        
        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int size() {
            return FixedSizeArrayHashMap.this.size();
        }
        
        @Override
        public void clear() {
            FixedSizeArrayHashMap.this.clear();
        }
    }
    
    ////////////////////////////////////////////
    // COPIED FROM AbstractMap!
    
    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     */
    private transient volatile Set<K>        keySet = null;
    private transient volatile Collection<V> values = null;

    /**
     * Returns a Set view of the keys contained in this map.  The Set is
     * backed by the map, so changes to the map are reflected in the Set,
     * and vice-versa.  (If the map is modified while an iteration over
     * the Set is in progress, the results of the iteration are undefined.)
     * The Set supports element removal, which removes the corresponding entry
     * from the map, via the Iterator.remove, Set.remove,  removeAll
     * retainAll, and clear operations.  It does not support the add or
     * addAll operations.<p>
     *
     * This implementation returns a Set that subclasses
     * AbstractSet.  The subclass's iterator method returns a "wrapper
     * object" over this map's entrySet() iterator.  The size method delegates
     * to this map's size method and the contains method delegates to this
     * map's containsKey method.<p>
     *
     * The Set is created the first time this method is called,
     * and returned in response to all subsequent calls.  No synchronization
     * is performed, so there is a slight chance that multiple calls to this
     * method will not all return the same Set.
     *
     * @return a Set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<K>() {
                @Override
                public Iterator<K> iterator() {
                    return new Iterator<K>() {
                        private Iterator<Map.Entry<K,V>> i = entrySet().iterator();
            
                        public boolean hasNext() {
                            return i.hasNext();
                        }
            
                        public K next() {
                            return i.next().getKey();
                        }
            
                        public void remove() {
                            i.remove();
                        }
                    };
                }
        
                @Override
                public int size() {
                    return FixedSizeArrayHashMap.this.size();
                }
        
                @Override
                public boolean contains(Object k) {
                    return FixedSizeArrayHashMap.this.containsKey(k);
                }
            };
        }
        return keySet;
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  (If the map is modified while an
     * iteration over the collection is in progress, the results of the
     * iteration are undefined.)  The collection supports element removal,
     * which removes the corresponding entry from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.<p>
     *
     * This implementation returns a collection that subclasses abstract
     * collection.  The subclass's iterator method returns a "wrapper object"
     * over this map's <tt>entrySet()</tt> iterator.  The size method
     * delegates to this map's size method and the contains method delegates
     * to this map's containsValue method.<p>
     *
     * The collection is created the first time this method is called, and
     * returned in response to all subsequent calls.  No synchronization is
     * performed, so there is a slight chance that multiple calls to this
     * method will not all return the same Collection.
     *
     * @return a collection view of the values contained in this map.
     */
    @Override
    public Collection<V> values() {
        if (values == null) {
            values = new AbstractCollection<V>() {
                @Override
                public Iterator<V> iterator() {
                    return new Iterator<V>() {
                        private Iterator<Map.Entry<K,V>> i = entrySet().iterator();
            
                        public boolean hasNext() {
                            return i.hasNext();
                        }
            
                        public V next() {
                            return i.next().getValue();
                        }
            
                        public void remove() {
                            i.remove();
                        }
                    };
                }
            
                @Override
                public int size() {
                    return FixedSizeArrayHashMap.this.size();
                }
        
                @Override
                public boolean contains(Object v) {
                    return FixedSizeArrayHashMap.this.containsValue(v);
                }
            };
        }
        return values;
    }
    
}
