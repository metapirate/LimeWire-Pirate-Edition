package org.limewire.collection;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a fixed size {@link HashMap}. If <code>FixedsizeHashMap</code> 
 * gets full, no new entry can be inserted into it, except by removing an 
 * entry first. An attempt to add new entry throws a {@link NoMoreStorageException}.
 * 
<pre>
    try{
        
        FixedsizeHashMap&lt;String, String&gt; fhm = new FixedsizeHashMap&lt;String, String&gt;(3);
        fhm.put("myKey1", "Abby");
        fhm.put("myKey2", "Bob");
        fhm.put("myKey3", "Chris");
        System.out.println(fhm);    

        String ret;
        ret = fhm.put("myKey3", "replace");
        if(ret != null)
            System.out.println("put returned: " + ret);
        System.out.println(fhm);    

        fhm.put("myKey4", "Dan");
    } catch (Exception e) {
        System.out.println("Exception because of maximum size upon put myKey4 ... " + e.toString() );
    }   
    
    Output:
        {myKey2=Bob, myKey3=Chris, myKey1=Abby}
        put returned: Chris
        {myKey2=Bob, myKey3=replace, myKey1=Abby}
        Exception because of maximum size upon put myKey4 ... org.limewire.collection.NoMoreStorageException
    
</pre>
 * 
 */
public class FixedsizeHashMap<K, V> {
    
    /**
     * The underlying storage
     */
    private final Map<K, V> hashMap;
    
    /**
     * The max number of elements that can be stored
     */
    private final int maxSize;
    
    /**
     * Create a new hashMap that stores only the specified number of entries.
     *
     * @param size the number of entries to hold
     * @exception IllegalArgumentException if size is less < 1.
     */
    public FixedsizeHashMap(int size)
    {
        hashMap = new HashMap<K, V>(size * 4/3);
        this.maxSize = size;
    }
    
    /**
     * Maps the given key to the given value. If adding the key
     * would make this contain more elements than the size given at
     * construction, the passed entry is not stored and NoMoreStorageException
     * gets thrown.
     * @exception NoMoreStorageException when no more space left in the storage
     * ideally, before calling put method, it should be checked whether the map is
     * already full or not
     */
    public synchronized V put(K key, V value) throws NoMoreStorageException
    {
        V retValue = null;
        
        //check if the count is less than size, in that case no problem
        //inserting this new entry
        if(hashMap.size() < maxSize) 
            retValue = hashMap.put(key,value);
        else {
            //if the entry already existed, we can safely add this new pair
            //without affecting the size
            retValue = hashMap.get(key);
            
            if(retValue != null) //mapping existed, so update the mapping 
                retValue = hashMap.put(key,value);
            else //no space to enter anything more 
                throw new NoMoreStorageException();
        }
        
        return retValue;
    }
    
    /**
     * Returns the value mapped to the given key.
     * @param key the given key
     * @return the value given key maps to
     */
    public synchronized V get(K key) {
        return hashMap.get(key);
    }
    
    /**
     * Clears all entries from the map.
     */
    public synchronized void clear() {
        hashMap.clear();
    }
    
    /**
     * @return the string representation of the mappings
     */
    @Override
    public synchronized String toString() {
        return hashMap.toString();
    }
    
    
}
