/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

/*
 * Note: originally released under the GNU LGPL v2.1, 
 * but rereleased by the original author under the ASF license (above).
 */
package org.limewire.collection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/*
 * http://fisheye5.cenqua.com/viewrep/~raw,r=1.2/dwr/java/uk/ltd/getahead/dwr/lang/IntHashMap.java
 */

/**
 * <p>A hash map that uses primitive ints for the key rather than objects.</p>
 *
 * <p>Note that this class is for internal optimization purposes only, and may
 * not be supported in future releases of Jakarta Commons Lang.  Utilities of
 * this sort may be included in future releases of Jakarta Commons Collections.</p>
 *
 * @author Justin Couch
 * @author Alex Chaffee (alex@apache.org)
 * @author Stephen Colebourne
 * @since 2.0
 * @version $Revision: 1.7 $
 * @see java.util.HashMap
 */
public class IntHashMap<V> implements Serializable {
    
    private static final long serialVersionUID = 2514013526418191636L;

    /**
     * The hash table data.
     */
    private transient Entry<V> table[];

    /**
     * The total number of entries in the hash table.
     */
    private transient int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     * @serial
     */
    private int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    private final float loadFactor;

    /**
     * <p>Inner class that acts as a data structure to create a new entry in the
     * table.</p>
     */
    private static class Entry<V> {
        int hash;
        int key;
        V value;
        Entry<V> next;

        /**
         * <p>Create a new entry with the given values.</p>
         *
         * @param hash the code used to hash the object with
         * @param key the key used to enter this in the table
         * @param value the value for this key
         * @param next a reference to the next entry in the table
         */
        protected Entry(int hash, int key, V value, Entry<V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    /**
     * <p>Constructs a new, empty hash table with a default capacity and load
     * factor, which is <code>20</code> and <code>0.75</code> respectively.</p>
     */
    public IntHashMap() {
        this(20, 0.75f);
    }

    /**
     * <p>Constructs a new, empty hash table with the specified initial capacity
     * and default load factor, which is <code>0.75</code>.</p>
     *
     * @param  initialCapacity the initial capacity of the hash table.
     * @throws IllegalArgumentException if the initial capacity is less
     *   than zero.
     */
    public IntHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * <p>Constructs a new, empty hash table with the specified initial
     * capacity and the specified load factor.</p>
     *
     * @param initialCapacity the initial capacity of the hash table.
     * @param loadFactor the load factor of the hash table.
     * @throws IllegalArgumentException  if the initial capacity is less
     *             than zero, or if the load factor is nonpositive.
     */
    @SuppressWarnings("unchecked")
    public IntHashMap(int initialCapacity, float loadFactor) {
        super();
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity); //$NON-NLS-1$
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal Load: " + loadFactor); //$NON-NLS-1$
        }
        if (initialCapacity == 0) {
            initialCapacity = 1;
        }

        this.loadFactor = loadFactor;
        table = new Entry[initialCapacity];
        threshold = (int) (initialCapacity * loadFactor);
    }

    /**
     * Copy constructor.
     * 
     * @param m the IntHashMap to copy
     */
    public IntHashMap(IntHashMap<? extends V> m) {
        // Allow for a bit of growth
        this((int) ((1 + m.size()) * 1.1));
        putAll(m);
    }
    
    /**
     * Adds all elements from m to this.
     */
    public void putAll(IntHashMap<? extends V> m) {
        Entry<? extends V> tab[] = m.table;
        for (int i = tab.length; i-- > 0;) {
            for (Entry<? extends V> e = tab[i]; e != null; e = e.next) {
                put(e.key, e.value);
            }
        }
    }
    
    /**
     * <p>Returns the number of keys in this hash table.</p>
     *
     * @return  the number of keys in this hash table.
     */
    public int size() {
        return count;
    }

    /**
     * <p>Tests if this hash table maps no keys to values.</p>
     *
     * @return  <code>true</code> if this hash table maps no keys to values;
     *          <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * <p>Tests if some key maps into the specified value in this hash table.
     * This operation is more expensive than the <code>containsKey</code>
     * method.</p>
     *
     * <p>Note that this method is identical in functionality to containsValue,
     * (which is part of the Map interface in the collections framework).</p>
     *
     * @param      value   a value to search for.
     * @return     <code>true</code> if and only if some key maps to the
     *             <code>value</code> argument in this hash table as
     *             determined by the <tt>equals</tt> method;
     *             <code>false</code> otherwise.
     * @throws  NullPointerException  if the value is <code>null</code>.
     * @see        #containsKey(int)
     * @see        #containsValue(Object)
     * @see        java.util.Map
     */
    public boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        Entry tab[] = table;
        for (int i = tab.length; i-- > 0;) {
            for (Entry e = tab[i]; e != null; e = e.next) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>Returns <code>true</code> if this HashMap maps one or more keys
     * to this value.</p>
     *
     * <p>Note that this method is identical in functionality to contains
     * (which predates the Map interface).</p>
     *
     * @param value value whose presence in this HashMap is to be tested.
     * @return true/false
     * @see    java.util.Map
     * @since JDK1.2
     */
    public boolean containsValue(Object value) {
        return contains(value);
    }

    /**
     * <p>Tests if the specified object is a key in this hash table.</p>
     *
     * @param  key  possible key.
     * @return <code>true</code> if and only if the specified object is a
     *    key in this hash table, as determined by the <tt>equals</tt>
     *    method; <code>false</code> otherwise.
     * @see #contains(Object)
     */
    public boolean containsKey(int key) {
        Entry tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Returns the value to which the specified key is mapped in this map.</p>
     *
     * @param   key   a key in the hash table.
     * @return  the value to which the key is mapped in this hash table;
     *          <code>null</code> if the key is not mapped to any value in
     *          this hash table.
     * @see     #put(int, Object)
     */
    public V get(int key) {
        Entry<V> tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                return e.value;
            }
        }
        return null;
    }

    /**
     * <p>Increases the capacity of and internally reorganizes this
     * hash table, in order to accommodate and access its entries more
     * efficiently.</p>
     *
     * <p>This method is called automatically when the number of keys
     * in the hash table exceeds this hashtable's capacity and load
     * factor.</p>
     */
    @SuppressWarnings("unchecked")
    protected void rehash() {
        int oldCapacity = table.length;
        Entry<V> oldMap[] = table;

        int newCapacity = oldCapacity * 2 + 1;
        Entry<V> newMap[] = new Entry[newCapacity];

        threshold = (int) (newCapacity * loadFactor);
        table = newMap;

        for (int i = oldCapacity; i-- > 0;) {
            for (Entry<V> old = oldMap[i]; old != null;) {
                Entry<V> e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

    /**
     * <p>Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hash table. The key cannot be
     * <code>null</code>. </p>
     *
     * <p>The value can be retrieved by calling the <code>get</code> method
     * with a key that is equal to the original key.</p>
     *
     * @param key     the hash table key.
     * @param value   the value.
     * @return the previous value of the specified key in this hash table,
     *         or <code>null</code> if it did not have one.
     * @throws  NullPointerException  if the key is <code>null</code>.
     * @see     #get(int)
     */
    public V put(int key, V value) {
        // Makes sure the key is not already in the hashtable.
        Entry<V> tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash) {
                V old = e.value;
                e.value = value;
                return old;
            }
        }

        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();

            tab = table;
            index = (hash & 0x7FFFFFFF) % tab.length;
        }

        // Creates the new entry.
        Entry<V> e = new Entry<V>(hash, key, value, tab[index]);
        tab[index] = e;
        count++;
        return null;
    }

    /**
     * <p>Removes the key (and its corresponding value) from this
     * hash table.</p>
     *
     * <p>This method does nothing if the key is not present in the
     * hash table.</p>
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the key had been mapped in this hash table,
     *          or <code>null</code> if the key did not have a mapping.
     */
    public V remove(int key) {
        Entry<V> tab[] = table;
        int hash = key;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (Entry<V> e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                V oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        return null;
    }

    /**
     * <p>Clears this hash table so that it contains no keys.</p>
     */
    public synchronized void clear() {
        Entry tab[] = table;
        for (int index = tab.length; --index >= 0;) {
            tab[index] = null;
        }
        count = 0;
    }
    
    private void writeObject(ObjectOutputStream oos) 
            throws IOException {
        
        // Write out the threshold, loadfactor, and any hidden stuff
        oos.defaultWriteObject();

        // Write out number of buckets
        oos.writeInt(table.length);

        // Write out size (number of Mappings)
        oos.writeInt(count);

        // Write the Entries
        for (Entry<V> entry : table) {
            while(entry != null) {
                oos.writeInt(entry.key);
                oos.writeObject(entry.value);
                entry = entry.next;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream ois) 
            throws IOException, ClassNotFoundException {
        
        // Read in the threshold, loadfactor, and any hidden stuff
        ois.defaultReadObject();

        // Read in number of buckets and allocate the bucket array
        int numBuckets = ois.readInt();
        table = new Entry[numBuckets];
        
        // Read in size (number of Mappings)
        int size = ois.readInt();

        // Read the keys and values, and put the mappings in the IntHashMap
        for (int i = 0; i < size; i++) {
            int key = ois.readInt();
            V value = (V)ois.readObject();
            put(key, value);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("{");
        buffer.append("size=").append(size()).append("; ");
        for (Entry<V> entry : table) {
            while(entry != null) {
                buffer.append(entry.key).append("=").append(entry.value).append(", ");
                entry = entry.next;
            }
        }
        buffer.append("}");
        return buffer.toString();
    }
}
