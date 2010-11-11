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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.util.CommonUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.Inject;
import com.limegroup.gnutella.QueryCategoryFilterer;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * This class contains a systemwide File creation time cache that persists these
 * times across sessions. Very similar to UrnCache but less complex.
 * <p>
 * This class is needed because we don't want to consult File.lastModifiedTime()
 * all the time. We want to preserve creation times across the Gnutella network.
 * <p>
 * In order to be speedy, this class maintains two data structures - one for
 * fast URN to creation time lookup, another for fast 'youngest' file lookup.
 * <p>
 * IMPLEMENTATION NOTES:
 * <p>
 * The two data structures do not reflect each other's internal representation -
 * specifically, the URN->Time lookup may have more URNs than the Time->URNSet
 * lookup. This is a consequence of partial file sharing. It is the case that
 * the URNs in the sets of the Time->URNSet lookup are a subset of the URNs in
 * the URN->Time lookup. For more details, see addTime and commitTime.
 */
@EagerSingleton
public class CreationTimeCache {

    private static final Log LOG = LogFactory.getLog(CreationTimeCache.class);

    /**
     * File where creation times for files are stored.
     */
    private final File CTIME_CACHE_FILE = new File(CommonUtils.getUserSettingsDir(),
            "createtimes.cache");

    /**
     * Whether or not data is dirty since the last time we saved.
     */
    private volatile boolean dirty = false;

    private final ExecutorService deserializeQueue = ExecutorsHelper
            .newProcessingQueue("CreationTimeCacheDeserializer");

    private final Library library;
    private final FileView gnutellaFileView;
    private final QueryCategoryFilterer mediaTypeAggregator;

    private final Future<Maps> deserializer;

    @Inject
    CreationTimeCache(Library library, @GnutellaFiles FileView gnutellaFileView, QueryCategoryFilterer mediaTypeAggregator) {
        this.gnutellaFileView = gnutellaFileView;
        this.library = library;
        this.mediaTypeAggregator = mediaTypeAggregator;
        this.deserializer = deserializeQueue.submit(new Callable<Maps>() {
            public Maps call() throws Exception {
                Map<URN, Long> urnToTime = createMap();
                SortedMap<Long, Set<URN>> timeToUrn = constructURNMap(urnToTime);
                return new Maps(urnToTime, timeToUrn);
            }
        });
    }

    @Inject
    void register(ServiceRegistry registry) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return "What's New Manager";
            }

            @Override
            public void initialize() {
                CreationTimeCache.this.initialize();
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        });
    }

    void initialize() {
        library.addManagedListStatusListener(new EventListener<LibraryStatusEvent>() {
            @Override
            public void handleEvent(LibraryStatusEvent event) {
                handleManagedListStatusEvent(event);
            }
        });
        // TODO Currently creation time cache is used to get creation times for
        // CoreLocalFileItem.
        // By only registering events for gnutella share list changes in
        // CreationTimeCache, the creation time field for Non gnutella shared
        // files is being set to -1.
        // In the future we will probably want to register for events on the
        // managed fileList instead
        gnutellaFileView.addListener(new EventListener<FileViewChangeEvent>() {
            @Override
            public void handleEvent(FileViewChangeEvent event) {
                handleFileListEvent(event);
            }
        });
    }

    /**
     * Package private for testing.
     */
    Map<URN, Long> getUrnToTime() {
        return getMaps().getUrnToTime();
    }

    /**
     * Package private for testing.
     */
    SortedMap<Long, Set<URN>> getTimeToUrn() {
        return getMaps().getTimeToUrn();
    }

    private Maps getMaps() {
        boolean interrupted = false;
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

    /** Returns the number of URNS stored. */
    public synchronized int getSize() {
        return getUrnToTime().size();
    }

    /**
     * Get the Creation Time of the file.
     * 
     * @param urn <tt>URN</tt> to look up Creation Time for
     * @return A Long that represents the creation time of the urn. Null if
     *         there is no association.
     */
    public synchronized Long getCreationTime(URN urn) {
        return getUrnToTime().get(urn);
    }

    /**
     * Get the Creation Time of the file.
     * 
     * @param urn <tt>URN</tt> to look up Creation Time for
     * @return A long that represents the creation time of the urn. -1 if no
     *         time exists.
     */
    public long getCreationTimeAsLong(URN urn) {
        Long l = getCreationTime(urn);
        if (l == null)
            return -1;
        else
            return l.longValue();
    }

    /**
     * Removes the CreationTime that is associated with the specified URN.
     */
    synchronized void removeTime(URN urn) {
        Long time = getUrnToTime().remove(urn);
        removeURNFromURNSet(urn, time);
        if (time != null)
            dirty = true;
    }

    /**
     * Clears away any URNs for files that do not exist anymore.
     * 
     * @param shouldClearURNSetMap true if you want to clear TIME_TO_URNSET_MAP
     *        too
     */
    private void pruneTimes(boolean shouldClearURNSetMap) {
        synchronized (this) {
            Iterator<Map.Entry<URN, Long>> iter = getUrnToTime().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<URN, Long> currEntry = iter.next();
                URN currURN = currEntry.getKey();
                Long cTime = currEntry.getValue();

                // check to see if file still exists
                // NOTE: technically a URN can map to multiple FDs, but I only
                // want
                // to know about one. getFileDescForUrn prefers FDs over iFDs.
                FileDesc fd = gnutellaFileView.getFileDesc(currURN);
                if ((fd == null) || (fd.getFile() == null) || !fd.getFile().exists()) {
                    dirty = true;
                    iter.remove();
                    if (shouldClearURNSetMap)
                        removeURNFromURNSet(currURN, cTime);
                }
            }
        }
    }

    /**
     * Clears away any URNs for files that do not exist anymore.
     */
    private void pruneTimes() {
        pruneTimes(true);
    }

    /**
     * Add a CreationTime for the specified <tt>URN</tt> instance. Can be called
     * for any type of file (complete or partial). Partial files should be
     * committed upon completion via commitTime.
     * 
     * @param urn the <tt>URN</tt> instance containing Time to store
     * @param time The creation time of the urn.
     * @throws IllegalArgumentException If urn is null or time is invalid.
     */
    public synchronized void addTime(URN urn, long time) throws IllegalArgumentException {
        if (urn == null)
            throw new IllegalArgumentException("Null URN.");
        if (time <= 0)
            throw new IllegalArgumentException("Bad Time = " + time);
        Long cTime = Long.valueOf(time);

        // populate urn to time
        Long existing = getUrnToTime().get(urn);
        if (existing == null || !existing.equals(cTime)) {
            dirty = true;
            getUrnToTime().put(urn, cTime);
        }
    }

    /**
     * Commits the CreationTime for the specified <tt>URN</tt> instance. Should
     * be called for complete files that are shared. addTime() for the input URN
     * should have been called first (otherwise you'll get a
     * IllegalArgumentException)
     * 
     * @param urn the <tt>URN</tt> instance containing Time to store
     * @throws IllegalArgumentException If urn is null or the urn was never
     *         added in addTime();
     */
    public synchronized void commitTime(URN urn) throws IllegalArgumentException {
        if (urn == null)
            throw new IllegalArgumentException("Null URN.");
        Long cTime = getUrnToTime().get(urn);
        if (cTime == null)
            throw new IllegalArgumentException("Never added URN via addTime()");

        // populate time to set of urns
        Set<URN> urnSet = getTimeToUrn().get(cTime);
        if (urnSet == null) {
            urnSet = new HashSet<URN>(); // purposely not a UrnSet -- we need to have multiple SHA1s in the list.
            getTimeToUrn().put(cTime, urnSet);
        }
        urnSet.add(urn);
    }

    /**
     * Returns an List of URNs, from 'youngest' to 'oldest'.
     * 
     * @param max the maximum number of URNs you want returned. if you want all,
     *        give Integer.MAX_VALUE.
     * @return a List ordered by younger URNs.
     */
    public Collection<URN> getFiles(final int max) throws IllegalArgumentException {
        return getFiles(null, max);
    }

    /**
     * Returns an List of URNs, from 'youngest' to 'oldest'.
     * 
     * @param request in case the query has meta-flags, you can give it to me.
     *        null is fine though.
     * @param max the maximum number of URNs you want returned. if you want all,
     *        give Integer.MAX_VALUE.
     * @return a List ordered by younger URNs.
     */
    public Collection<URN> getFiles(final QueryRequest request, final int max)
            throws IllegalArgumentException {
        synchronized (this) {
            if (max < 1)
                throw new IllegalArgumentException("bad max = " + max);
            Predicate<String> filter = request == null ? 
                    Predicates.<String>alwaysTrue() :
                        mediaTypeAggregator.getPredicateForQuery(request);

            // may be non-null at loop end
            List<URN> toRemove = null;
            Set<URN> urnList = new LinkedHashSet<URN>();
            
            // we bank on the fact that the TIME_TO_URNSET_MAP iterator returns
            // the
            // entries in descending order....
            for (Set<URN> urns : getTimeToUrn().values()) {
                if (urnList.size() >= max) {
                    break;
                }

                for (URN currURN : urns) {
                    if (urnList.size() >= max) {
                        break;
                    }

                    // we only want shared FDs
                    FileDesc fd = gnutellaFileView.getFileDesc(currURN);
                    if (fd == null) {
                        if (toRemove == null) {
                            toRemove = new ArrayList<URN>();
                        }
                        toRemove.add(currURN);
                        continue;
                    }

                    if (filter.apply(FileUtils.getFileExtension(fd.getFileName()))) {
                        urnList.add(currURN);
                    }
                }
            }

            // clear any ifd's or unshared files that may have snuck into
            // structures
            if (toRemove != null) {
                for (URN currURN : toRemove) {
                    removeTime(currURN);
                }
            }

            return urnList;
        }
    }

    /**
     * Returns all of the files URNs, from youngest to oldest.
     */
    public Collection<URN> getFiles() {
        return getFiles(Integer.MAX_VALUE);
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    synchronized void persistCache() {
        if (!dirty)
            return;

        // It's not ideal to hold a lock while writing to disk, but I doubt
        // think
        // it's a problem in practice.
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    CTIME_CACHE_FILE)));
            oos.writeObject(getUrnToTime());
        } catch (IOException e) {
            LOG.error("Unable to write creation cache", e);
        } finally {
            IOUtils.close(oos);
        }

        dirty = false;
    }

    /**
     * Evicts the urn from the TIME_TO_URNSET_MAP.
     * 
     * @param refTime if is non-null, will try to eject from set referred to by
     *        refTime. Otherwise will do an iterative search.
     */
    private synchronized void removeURNFromURNSet(URN urn, Long refTime) {
        if (refTime != null) {
            Set<URN> urnSet = getTimeToUrn().get(refTime);
            if (urnSet != null && urnSet.remove(urn))
                if (urnSet.size() < 1)
                    getTimeToUrn().remove(refTime);
        } else { // search everything
            // find the urn in the map:
            // 1) get rid of it
            // 2) get rid of the empty set if it exists
            for (Iterator<Set<URN>> i = getTimeToUrn().values().iterator(); i.hasNext();) {
                Set<URN> urnSet = i.next();
                if (urnSet.contains(urn)) {
                    urnSet.remove(urn); // 1)
                    if (urnSet.size() < 1)
                        i.remove(); // 2)
                    break;
                }
            }
        }
    }

    /**
     * Constructs the TIME_TO_URNSET_MAP, which is based off the entries in the
     * URN_TO_TIME_MAP.
     */
    private SortedMap<Long, Set<URN>> constructURNMap(Map<URN, Long> urnToTime) {
        SortedMap<Long, Set<URN>> timeToUrn = new TreeMap<Long, Set<URN>>(Comparators
                .inverseLongComparator());

        for (Map.Entry<URN, Long> currEntry : urnToTime.entrySet()) {
            // for each entry, get the creation time and the urn....
            Long cTime = currEntry.getValue();
            URN urn = currEntry.getKey();

            // put the urn in a set of urns that have that creation time....
            Set<URN> urnSet = timeToUrn.get(cTime);
            if (urnSet == null) {
                urnSet = new HashSet<URN>(); // purposely not a UrnSet -- we need multiple SHA1s in the list
                // populate the reverse mapping
                timeToUrn.put(cTime, urnSet);
            }

            urnSet.add(urn);
        }

        return timeToUrn;
    }

    /**
     * Loads values from cache file, if available.
     */
    Map<URN, Long> createMap() {
        if (!CTIME_CACHE_FILE.exists()) {
            dirty = true;
            return new HashMap<URN, Long>();
        }
        ObjectInputStream ois = null;
        try {
            ois = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(
                    CTIME_CACHE_FILE)));
            Map<URN, Long> map = GenericsUtils.scanForMap(ois.readObject(), URN.class, Long.class,
                    GenericsUtils.ScanMode.REMOVE);
            return map;
        } catch (Throwable t) {
            dirty = true;
            LOG.error("Unable to read creation time file", t);
            return new HashMap<URN, Long>();
        } finally {
            IOUtils.close(ois);
        }
    }

    private static class Maps {
        /** URN -> Creation Time (Long) */
        private final Map<URN, Long> urnToTime;

        /** Creation Time (Long) -> Set of URNs */
        private final SortedMap<Long, Set<URN>> timeToUrn;

        Maps(Map<URN, Long> urnToTime, SortedMap<Long, Set<URN>> timeToUrn) {
            this.urnToTime = urnToTime;
            this.timeToUrn = timeToUrn;
        }

        public SortedMap<Long, Set<URN>> getTimeToUrn() {
            return timeToUrn;
        }

        public Map<URN, Long> getUrnToTime() {
            return urnToTime;
        }
    }

    private void fileAdded(FileDesc fd) {
        URN sha1 = fd.getSHA1Urn();
        if (!LibraryUtils.isForcedShare(fd) && sha1 != null) {
            synchronized (this) {
                Long cTime = getCreationTime(sha1);
                if (cTime == null) {
                    cTime = Long.valueOf(fd.lastModified());
                }
                // if cTime is non-null but 0, then the IO subsystem is
                // letting us know that the file was FNF or an IOException
                // occurred - the best course of action is to
                // ignore the issue and not add it to the CTC, hopefully
                // we'll get a correct reading the next time around...
                if (cTime.longValue() > 0) {
                    // these calls may be superfluous but are quite fast....
                    addTime(sha1, cTime.longValue());
                    commitTime(sha1);
                }
            }
        }
    }

    private void fileChanged(URN oldUrn, URN newUrn) {
        // re-populate the ctCache
        synchronized (this) {
            long creationTime = getCreationTimeAsLong(oldUrn);
            removeTime(oldUrn);
            if (creationTime != -1) {
                addTime(newUrn, creationTime);
                commitTime(newUrn);
            }
        }
    }

    /**
     * Listens for events from the FileManager.
     */
    private void handleManagedListStatusEvent(LibraryStatusEvent evt) {
        switch (evt.getType()) {
        case LOAD_FINISHING:
            pruneTimes();
            break;
        case SAVE:
            persistCache();
            break;
        }
    }

    private void handleFileListEvent(FileViewChangeEvent evt) {
        switch (evt.getType()) {
        case FILE_META_CHANGED:
            // fallthrough & pretend this was an add incase this was the URN
            // meta notification -- no big if it doesn't exist.
        case FILE_ADDED:
            // Commit the time in the CreationTimeCache, but don't share
            // the installer.
            fileAdded(evt.getFileDesc());
            break;
        case FILE_REMOVED:
            if(evt.getFileDesc().getSHA1Urn() != null) {
                removeTime(evt.getFileDesc().getSHA1Urn());
            }
            break;
        case FILE_CHANGED:
            if(evt.getOldValue().getSHA1Urn() == null) {
                fileAdded(evt.getFileDesc());
            } else if(evt.getFileDesc().getSHA1Urn() != null) {
                fileChanged(evt.getOldValue().getSHA1Urn(), evt.getFileDesc().getSHA1Urn());
            } else {
                removeTime(evt.getOldValue().getSHA1Urn());
            }
            break;
        }
    }
}