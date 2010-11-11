package org.limewire.collection;

import java.util.Iterator;
import java.util.Map;

/**
 * A variant of <tt>FixedSizeArrayHashMap</tt> that allows iterations over
 * its elements in random order. 
 */
public class RandomOrderHashMap<K, V> extends FixedSizeArrayHashMap<K, V> {
    
    public RandomOrderHashMap(Map<? extends K, ? extends V> m) {
        super(m);
    }
    
    public RandomOrderHashMap(int maxCapacity, Map<? extends K, ? extends V> m) {
        super(maxCapacity, m);
    }

    public RandomOrderHashMap(int maxSize, int initialCapacity, float loadFactor) {
        super(maxSize, initialCapacity, loadFactor);
    }

    public RandomOrderHashMap(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    protected Iterator<Map.Entry<K, V>> newEntryIterator() {
        return new RandomIterator();
    }
    
    private class RandomIterator extends UnmodifiableIterator<Map.Entry<K, V>> {
        private final Iterator<Integer> sequence = new RandomSequence(size()).iterator();
        
        public boolean hasNext() {
            return sequence.hasNext();
        }
        
        public Map.Entry<K, V> next() {
            return getEntryAt(sequence.next());
        }
    }

}
