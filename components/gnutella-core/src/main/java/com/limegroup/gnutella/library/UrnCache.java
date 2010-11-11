package com.limegroup.gnutella.library;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.listener.EventBroadcaster;
import org.limewire.util.CommonUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.hashing.AudioHashingUtils;

/**
 * This class contains a systemwide URN cache that persists file URNs (hashes)
 * across sessions.
 * <p>
 * Modified by Gordon Mohr (2002/02/19): Added URN storage, calculation, caching
 * Repackaged by Greg Bildson (2002/02/19): Moved to dedicated class.
 * 
 * @see URN
 */
@EagerSingleton
public final class UrnCache {

    private static final Log LOG = LogFactory.getLog(UrnCache.class);

    /**
     * File where urns (currently SHA1 urns) for files are stored.
     */
    private static final File URN_CACHE_FILE = new File(CommonUtils.getUserSettingsDir(),
            "fileurns.cache");

    /**
     * Last good version of above.
     */
    private static final File URN_CACHE_BACKUP_FILE = new File(CommonUtils.getUserSettingsDir(),
            "fileurns.bak");

    /**
     * The ProcessingQueue that Files are hashed in.
     */
    private final ListeningExecutorService QUEUE;

    /**
     * Whether or not data is dirty since the last time we saved.
     */
    private volatile boolean dirty = false;

    /** The future that will contain the URN_MAP when it is done. */
    private final Future<Map<UrnSetKey, Set<URN>>> deserializer;
    
    private final EventBroadcaster<FileProcessingEvent> broadcaster;

    /**
     * Create and initialize urn cache.
     */
    @Inject
    UrnCache(@DiskIo ListeningExecutorService diskIoExecutor, EventBroadcaster<FileProcessingEvent> broadcaster) {
        this.QUEUE = diskIoExecutor;
        this.broadcaster = broadcaster;
        deserializer = QUEUE.submit(new Callable<Map<UrnSetKey, Set<URN>>>() {
            @SuppressWarnings("unchecked")
            public Map<UrnSetKey, Set<URN>> call() {
                // This cannot be inside a synchronized block, otherwise other
                // methods
                // can block its construction.
                Map map = createMap();
                dirty = scanAndRemoveOldEntries(map);
                return map;
            }
        });
    }

    @Inject
    void register(@Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService,
            ServiceScheduler serviceScheduler) {
        serviceScheduler.scheduleWithFixedDelay("urncache persister", new Runnable() {
            @Override
            public void run() {
                persistCache();
            }
        }, 30, 30, TimeUnit.SECONDS, scheduledExecutorService);
    }

    /**
     * Calculates the given File's URN and caches it. The callback will be
     * notified of the URNs. If they're already calculated, the callback will be
     * notified immediately. Otherwise, it will be notified when hashing
     * completes, fails, or is interrupted.
     */
    public ListeningFuture<Set<URN>> calculateAndCacheSHA1(File file) {
        Set<URN> urns;
        synchronized (this) {
            urns = getUrns(file);
            // check that a SHA1 doesn't yet exist for this file.
            if (UrnSet.getSha1(urns) == null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Adding: " + file + " to be hashed.");
                return QUEUE.submit(new SHA1Processor(file));
            }
        }

        assert !urns.isEmpty();
        return new SimpleFuture<Set<URN>>(urns);
    }
    
    /**
     * Calculates the NonMetaData SHA1 for a File and caches it. The callback will be
     * notified of the URN. If its already calculated, the callback will be
     * notified immediately. Otherwise, it will be notified when hashing
     * completes, fails, or is interrupted.
     */
    public ListeningFuture<URN> calculateAndCacheNMS1(File file) {
        URN nms1 = null;
        synchronized(this) {
            nms1 = UrnSet.getNMS1(getUrns(file));
            // calculate nms1 if it doesn't exist already
            if(nms1 == null) {
                return QUEUE.submit(new NMS1Processor(file));
            }
        }
        return new SimpleFuture<URN>(nms1);
    }

    /**
     * Find any URNs remembered from a previous session for the specified
     * <tt>File</tt> instance. The returned <tt>Set</tt> is guaranteed to be
     * non-null, but it may be empty.
     * 
     * @param file the <tt>File</tt> instance to look up URNs for
     * @return a new <tt>Set</tt> containing any cached URNs for the specified
     *         <tt>File</tt> instance, guaranteed to be non-null and
     *         unmodifiable, but possibly empty
     */
    public synchronized Set<URN> getUrns(File file) {
        long modified = file.lastModified();
        // don't trust failed mod times
        if (modified == 0L) {
            return Collections.emptySet();
        }

        UrnSetKey key = new UrnSetKey(file);
        if (key._modTime != modified) {
            return Collections.emptySet();
        } else {
            Set<URN> cachedUrns = getUrnMap().get(key);
            if (cachedUrns == null) {
                return Collections.emptySet();
            } else {
                return cachedUrns;
            }
        }
    }

    /**
     * Removes any URNs that associated with a specified file.
     */
    public synchronized void removeUrns(File f) {
        UrnSetKey k = new UrnSetKey(f);
        getUrnMap().remove(k);
        dirty = true;
    }

    /**
     * Add URNs for the specified <tt>FileDesc</tt> instance to URN_MAP.
     * 
     * @param file the <tt>File</tt> instance containing URNs to store
     */
    public synchronized void addUrns(File file, Set<? extends URN> urns) {
        UrnSetKey key = new UrnSetKey(file);
        getUrnMap().put(key, UrnSet.unmodifiableSet(urns));
        dirty = true;
    }

    /**
     * Loads values from cache file, if available. If the cache file is not
     * readable, tries the backup.
     */
    private static Map createMap() {
        Map result;
        result = readMap(URN_CACHE_FILE);
        if (result == null)
            result = readMap(URN_CACHE_BACKUP_FILE);
        if (result == null)
            result = new HashMap<Object, Object>();
        return result;
    }

    /**
     * Loads values from cache file, if available.
     * 
     * @return null if the file does not exist or there was an error reading the
     *         map from the file.
     */
    private static Map readMap(File file) {
        if (!file.exists()) {
            return null;
        }
        ConverterObjectInputStream ois = null;
        try {
            ois = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            // Allow for refactoring from gnutella -> gnutella.library
            ois.addLookup("com.limegroup.gnutella.UrnCache$UrnSetKey", UrnSetKey.class.getName());
            return (Map) ois.readObject();
        } catch (Throwable t) {
            LOG.error("Unable to read UrnCache", t);
            return null;
        } finally {
            IOUtils.close(ois);
        }
    }

    /**
     * Removes any stale entries from the map so that they will automatically be
     * replaced.
     * 
     * @param map the <tt>Map</tt> to check
     */
    private static boolean scanAndRemoveOldEntries(Map<Object, Object> map) {
        // discard outdated info
        boolean dirty = false;
        for (Iterator<Map.Entry<Object, Object>> i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry<Object, Object> entry = i.next();
            if (!(entry.getKey() instanceof UrnSetKey)) {
                i.remove();
                dirty = true;
                continue;
            }

            UrnSetKey key = (UrnSetKey) entry.getKey();
            File f = new File(key._path);
            if (!f.exists() || f.lastModified() != key._modTime) {
                dirty = true;
                i.remove();
                continue;
            }

            if (!(entry.getValue() instanceof Set)) {
                i.remove();
                dirty = true;
                continue;
            }

            Set<URN> set = GenericsUtils.scanForSet(entry.getValue(), URN.class,
                    GenericsUtils.ScanMode.NEW_COPY_REMOVED, UrnSet.class);
            if (set.isEmpty()) {
                i.remove();
                dirty = true;
                continue;
            }

            if (set != entry.getValue()) { // if it changed, replace the value
                                           // w/ unmodifiable
                dirty = true;
                entry.setValue(UrnSet.unmodifiableSet(set));
            }
        }
        return dirty;
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    synchronized void persistCache() {
        LOG.debug("persist cache");

        if (!dirty) {
            LOG.debug("not dirty");
            return;
        }
        
        if(FileUtils.writeWithBackupFile(getUrnMap(), URN_CACHE_BACKUP_FILE, URN_CACHE_FILE, LOG)) {
            dirty = false;
        }
    }

    private Map<UrnSetKey, Set<URN>> getUrnMap() {
        boolean interrupted = Thread.interrupted();
        try {
            while (true) {
                try {
                    return deserializer.get();
                } catch (InterruptedException tryAgain) {
                    interrupted = true;
                }
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    private class SHA1Processor implements Callable<Set<URN>> {
        private final File file;

        SHA1Processor(File f) {
            file = f;
        }

        public Set<URN> call() {
            if(broadcaster != null) {
                broadcaster.broadcast(new FileProcessingEvent(FileProcessingEvent.Type.PROCESSING, file));
            }
            Set<URN> urns;

            synchronized (UrnCache.this) {
                urns = getUrns(file); // already calculated?
            }

            // If not calculated, calculate OUTSIDE OF LOCK.
            if(UrnSet.getSha1(urns) == null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Hashing sha1 file: " + file);
                try {
                    UrnSet calculatedUrns = URN.generateUrnsFromFile(file);
                    UrnSet set = new UrnSet();
                    synchronized (UrnCache.this) {
                        set.addAll(getUrns(file));
                        set.addAll(calculatedUrns);
                        addUrns(file, set);
                    }
                    urns = set;
                } catch (IOException ignored) {
                    LOG.warn("Unable to calculate SHA1", ignored);
                } catch (InterruptedException ignored) {
                    LOG.warn("Unable to calculate SHA1", ignored);
                }
            }

            return urns;
        }
    }
    
    /**
     * Tries to calculate the Non-metadata sha1 of this file. If a 
     * SHA1 is successfully created, the URNSet is updated and saved
     * to disk.
     */
    private class NMS1Processor implements Callable<URN> {
        private final File file;

        NMS1Processor(File file) {
            this.file = file;
        }

        public URN call() {
            Set<URN> urns;
            URN nms1 = null;

            synchronized (UrnCache.this) {
                urns = getUrns(file); // already calculated?
            }

            // if the sha1 has not been calculated yet, don't calculate the
            // non-metadata hash.
            if (UrnSet.getNMS1(urns) == null) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Hashing nmsa file: " + file);
                if(AudioHashingUtils.canCreateNonMetaDataSHA1(file)) {
                    try {
                        nms1 = AudioHashingUtils.generateNonMetaDataSHA1FromFile(file);
                        if(nms1 != null) {
                            UrnSet set = new UrnSet();
                            synchronized (UrnCache.this) {
                                set.addAll(getUrns(file));
                                set.add(nms1);
                                addUrns(file, set);
                            }
                        }
                    } catch (InterruptedException ignored) {
                        LOG.warn("Unable to calculate NMS1", ignored);
                    }
                }
            } else {
            	nms1 = UrnSet.getNMS1(urns);
            }
            return nms1;
        }
    }

    /**
     * Private class for the key for the set of URNs for files.
     */
    private static class UrnSetKey implements Serializable {

        private static final long serialVersionUID = -7183232365833531645L;

        /**
         * Constant for the file modification time.
         * 
         * @serial
         */
        transient long _modTime;

        /**
         * Constant for the file path.
         * 
         * @serial
         */
        transient String _path;

        /**
         * Constant cached hash code, since this class is used exclusively as a
         * hash key.
         * 
         * @serial
         */
        transient int _hashCode;

        /**
         * Constructs a new <tt>UrnSetKey</tt> instance from the specified
         * <tt>File</tt> instance.
         * 
         * @param file the <tt>File</tt> instance to use in constructing the key
         */
        UrnSetKey(File file) {
            _modTime = file.lastModified();
            _path = file.getAbsolutePath();
            _hashCode = calculateHashCode();
        }

        /**
         * Helper method to calculate the hash code.
         * 
         * @return the hash code for this instance
         */
        int calculateHashCode() {
            int result = 17;
            result = result * 37 + _path.hashCode();
            return result;
        }

        /**
         * Overrides Object.equals so that keys with equal paths will be
         * considered equal.
         * 
         * @param o the <tt>Object</tt> instance to compare for equality
         * @return <tt>true</tt> if the specified object is the same instance as
         *         this object, or if it has the same path, otherwise returns
         *         <tt>false</tt>
         */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof UrnSetKey))
                return false;
            UrnSetKey key = (UrnSetKey) o;

            // note that the path is guaranteed to be non-null
            return _path.equals(key._path);
        }

        /**
         * Overrides Object.hashCode to meet the specification of Object.equals
         * and to make this class functions properly as a hash key.
         * 
         * @return the hash code for this instance
         */
        @Override
        public int hashCode() {
            return _hashCode;
        }

        /**
         * Serializes this instance.
         * 
         * @serialData the modification time followed by the file path
         */
        private void writeObject(ObjectOutputStream s) throws IOException {
            s.defaultWriteObject();
            s.writeLong(_modTime);
            s.writeObject(_path);
        }

        /**
         * Deserializes this instance, restoring all invariants.
         */
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            _modTime = s.readLong();
            _path = ((String) s.readObject()).intern();
            _hashCode = calculateHashCode();
        }
    }
}