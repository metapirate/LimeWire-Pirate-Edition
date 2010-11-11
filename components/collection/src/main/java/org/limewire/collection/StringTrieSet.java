package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

/**
 * Provides a set-like interface designed specifically for <code>String</code>s.
 * Uses a Trie as the backing map and provides an implementation specific to
 * <code>String</code>s. Has the same retrieval/insertion times as the backing 
 * Trie. Stores the value as the string, for easier retrieval.
 * The goal is to efficiently find Strings that can branch off a prefix. 
 * <p>
 * Primarily designed as an {@link AutoCompleteDictionary}.
 * <p> 
 * See <a href="http://en.wikipedia.org/wiki/Trie">Trie</a> for more information.
 * <p>
 * @modified David Soh (yunharla00@hotmail.com)
 * <pre>
 *      1. added getIterator() & getIterator(String) for enhanced AutoCompleteTextField use.
 *      2. disallowed adding duplicates
 *</pre>
 */
public class StringTrieSet implements AutoCompleteDictionary, Iterable<String> {
    /**
     * The backing map. A binary-sorted Trie.
     */
    private final transient Trie<String, String> map;

    public StringTrieSet(boolean ignoreCase) {
        if(ignoreCase) {
            map = new CaseIgnoredTrie<String>();
        } else {
            map = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        }
    }
    
    @Override
    public boolean isImmediate() {
        return true;
    }

    /**
     * Adds a value to the set.  Different letter case of values is always
     * kept and significant.  If the TrieSet is made case-insensitive,
     * it will not store two Strings with different case but will update
     * the stored values with the case of the last entry.
     */
    public void addEntry(String data) {
        if (!contains(data))    //disallow adding duplicates
            map.put(data, data);
    }

    /**
     * Determines whether or not the Set contains this String.
     */
    public boolean contains(String data) {
        return map.get(data) != null;
    }

    /**
     * Removes a value from the Set.
     *
     * @return <tt>true</tt> if a value was actually removed.
     */
    public boolean removeEntry(String data) {
        return map.remove(data) != null;
    }

    /**
     * Return all the Strings that can be prefixed by this String.
     * All values returned by the iterator have their case preserved.
     */
    public Collection<String> getPrefixedBy(String data) {
        return map.getPrefixedBy(data).values();
    }

    /**
     * Return the last String in the set that can be prefixed by this String
     * (Trie's are stored in alphabetical order).
     * Return null if no such String exist in the current set.
     */
    public String lookup(String data) {
        Iterator<String> it = map.getPrefixedBy(data).values().iterator();
        if (!it.hasNext())
            return null;
        return it.next();
    }

    /**
     * Returns all values (entire TrieSet).
     */
    public Iterator<String> iterator() {
        return map.values().iterator();
    }
    
    /**
     * Clears all items in the dictionary.
     */
    public void clear() {
        List<String> l = new ArrayList<String>(map.size());
        for (String string : this) {
            l.add(string);
        }
        for (String string : l) {
            removeEntry(string);
        }
    }
    
    private static class CaseIgnoredTrie<V> extends PatriciaTrie<String, V> {        
        public CaseIgnoredTrie() {
            super(new CharSequenceKeyAnalyzer());
        }
        
        private String canonicalize(final String s) {
            return s.toUpperCase(Locale.US).toLowerCase(Locale.US);
        }

        @Override
        public boolean containsKey(Object k) {
            return super.containsKey(canonicalize((String)k));
        }

        @Override
        public V get(Object k) {
            return super.get(canonicalize((String)k));
        }

        @Override
        public SortedMap<String, V> getPrefixedBy(String key, int offset, int length) {
            return super.getPrefixedBy(canonicalize(key), offset, length);
        }

        @Override
        public SortedMap<String, V> getPrefixedBy(String key, int length) {
            return super.getPrefixedBy(canonicalize(key), length);
        }

        @Override
        public SortedMap<String, V> getPrefixedBy(String key) {
            return super.getPrefixedBy(canonicalize(key));
        }

        @Override
        public SortedMap<String, V> getPrefixedByBits(String key, int bitLength) {
            return super.getPrefixedByBits(canonicalize(key), bitLength);
        }

        @Override
        public SortedMap<String, V> headMap(String toKey) {
            return super.headMap(canonicalize(toKey));
        }

        @Override
        public V put(String key, V value) {
            return super.put(canonicalize(key), value);
        }

        @Override
        public V remove(Object k) {
            return super.remove(canonicalize((String)k));
        }

        @Override
        public Entry<String, V> select(String key, Cursor<? super String, ? super V> cursor) {
            return super.select(canonicalize(key), cursor);
        }

        @Override
        public V select(String key) {
            return super.select(canonicalize(key));
        }

        @Override
        public SortedMap<String, V> subMap(String fromKey, String toKey) {
            return super.subMap(canonicalize(fromKey), canonicalize(toKey));
        }

        @Override
        public SortedMap<String, V> tailMap(String fromKey) {
            return super.tailMap(canonicalize(fromKey));
        }
        
        
    }
}
