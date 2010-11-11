package com.limegroup.gnutella.library;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;


/**
 * A container for storing serialized objects to disk.
 * This supports only storing objects that fit in the Collections framework.
 * Either Collections or Maps.
 * All collections are returned as synchronized on this container.
 */
@Deprecated
class Container {
    
    private static final Log LOG = LogFactory.getLog(Container.class);
    
    private final Map<String, Collection<File>> STORED = new HashMap<String, Collection<File>>();
    private final String filename;
    
    /**
     * Constructs a new container with the given filename.
     * It will always save to this name in the user's
     * setting's directory, also loading the data from disk.
     */
    Container(String name) {
        filename = name;
        load();
    }
    
    /**
     * Loads data from disk.
     */
    void load() {
        // Read without grabbing the lock.
        Map<String, Collection<File>> read = readFromDisk();
        
        synchronized(this) {
            // Simple case -- no stored data yet.
            if(STORED.isEmpty()) {
                STORED.putAll(read);
            } else {
                // If data was stored, we can't replace, we have to refresh.
                for(Map.Entry<String, Collection<File>> entry : read.entrySet()) {
                    String k = entry.getKey();
                    Collection<File> v = entry.getValue();
                    Collection<File> storedV = STORED.get(k);
                    if(storedV == null) {
                        // Another simple case -- key wasn't stored yet.
                        STORED.put(k, v);
                    } else {
                        synchronized(storedV) {
                            storedV.clear();
                            storedV.addAll(v);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Retrieves a set from the Container.  If the object
     * stored is not null or is not a set, a Set is inserted instead.
     *
     * The returned sets are synchronized, but the serialized sets are NOT SYNCHRONIZED.
     * This means that the future can change what they synchronize on easily.
     */
    synchronized Set<File> getSet(String name) {
        Collection<File> data = STORED.get(name);
        if (data != null) {
            return (Set<File>)data;
        } else { 
            Set<File> set = Collections.synchronizedSet(new HashSet<File>());
            STORED.put(name, set);
            return set;
        }
    }
    
    /**
     * Returns true if a Set by this name exists, false otherwise
     */
    synchronized boolean contains(String name) {
        Collection<File> data = STORED.get(name);
        if(data == null)
            return false;
        else
            return true;
    }
    
    /**
     * Removes the set from the container.
     * @param name - name of Set to remove
     */
    synchronized void remove(String name) {
        STORED.remove(name);
    }
    
    /**
     * Clears all entries.
     */
    synchronized void clear() {
        for(Collection<File> data : STORED.values()) {
            data.clear();
        }
    }
        
    
    /**
     * Saves the data to disk.
     */
    void save() {
        Map<String, Collection<File>> toSave;
        
        synchronized(this) {
            toSave = new HashMap<String, Collection<File>>(STORED.size());
            for(Map.Entry<String, Collection<File>> entry : STORED.entrySet()) {
                String k = entry.getKey();
                Collection<File> v = entry.getValue();
                synchronized(v) {
                	if(v instanceof SortedSet)
            			toSave.put(k, new TreeSet<File>((SortedSet<File>)v));
            		else if(v instanceof Set)
            			toSave.put(k, new HashSet<File>(v));
            		else if(v instanceof List) {
            			if (v instanceof RandomAccess)
            				toSave.put(k, new ArrayList<File>(v));
            			else 
            				toSave.put(k, new LinkedList<File>(v));
            		} else {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Update to clone! key: " + k);
                        toSave.put(k, v);
                    }
                }
            }
        }
        
        writeToDisk(toSave);
    }
    
    /**
     * Saves the given object to disk.
     */
    private void writeToDisk(Object o) {
        File f = new File(CommonUtils.getUserSettingsDir(), filename);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            oos.writeObject(o);
            oos.flush();
        } catch(IOException iox) {
            LOG.warn("Can't write to disk!", iox);
        } finally {
            IOUtils.close(oos);
        }
    }
    
    /**
     * Reads a Map from disk.
     */
    private Map<String, Collection<File>> readFromDisk() {
        File file = new File(CommonUtils.getUserSettingsDir(), filename);
        if (!file.exists()) {
            return new HashMap<String, Collection<File>>();
        }
        ObjectInputStream ois = null;
        Map map = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            map = (Map)ois.readObject();
        } catch(ClassCastException cce) {
            LOG.warn("Not a map!", cce);
        } catch(IOException iox) {
            LOG.warn("Can't read from disk!", iox);
        } catch(Throwable x) {
            LOG.warn("Error reading!", x);
        } finally {
            IOUtils.close(ois);
        }
        
        if (map == null) {
            return new HashMap<String, Collection<File>>();
        }
        
        HashMap<String, Collection<File>> toReturn = new HashMap<String, Collection<File>>(map.size());
        for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            if(!(entry.getKey() instanceof String)) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Ignoring key: " + entry.getKey());
                continue;
            }
            String k = (String)entry.getKey();
            if(!(entry.getValue() instanceof Collection)) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Ignoring value: " + entry.getValue());
                continue;
            }
            Collection<File> v = GenericsUtils.scanForCollection(entry.getValue(), File.class,
                    GenericsUtils.ScanMode.REMOVE);
            if(v instanceof SortedSet)
                toReturn.put(k, Collections.synchronizedSortedSet((SortedSet<File>)v));
            else if(v instanceof Set)
                toReturn.put(k, Collections.synchronizedSet((Set<File>)v));
            else if(v instanceof List)
                toReturn.put(k, Collections.synchronizedList((List<File>)v));
            else {
                if(LOG.isWarnEnabled())
                    LOG.warn("Update to clone! key: " + k);
                toReturn.put(k, v);
            }
        }
        return toReturn;
    }

}