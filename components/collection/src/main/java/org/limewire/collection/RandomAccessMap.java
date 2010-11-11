package org.limewire.collection;

import java.util.Map;
import java.util.RandomAccess;

/** A Map that can be retrieved through getAt methods. */
public interface RandomAccessMap<K, V> extends RandomAccess, Map<K, V> {

    /** Retrieves the key at index i. */
    public K getKeyAt(int i);
    
    /** Retrieves the value at index i. */
    public V getValueAt(int i);    
    
    /** Retrieves the entry at index i. */
    public Map.Entry<K, V> getEntryAt(int i);
}
