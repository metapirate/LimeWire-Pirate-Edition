package com.limegroup.gnutella.tigertree;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Tuple;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.Library;

/** This class maps SHA1_URNs to hash trees and roots. */
/* This is public for tests, but only the interface should be used. */
@Singleton
public final class HashTreeCacheImpl implements HashTreeCache {
    
    private static final Log LOG = LogFactory.getLog(HashTreeCacheImpl.class);
    
    /**
     * The ProcessingQueue to do the hashing.
     */
    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("TreeHashTread"); 
    
    /** A copy of the SHA1 -> Tiger Tree Root */
    private final Map<URN /* sha1 */, Future<URN> /* ttroot */> SHA1_TO_ROOT_MAP = new HashMap<URN, Future<URN>>();
    
    /** TigerTreeCache container. */
    private final Map<URN /* sha1 */, Future<HashTree>> TTREE_MAP = new HashMap<URN, Future<HashTree>>();
    
    /** Where the SHA1 -> ttRoot info is stored. */
    private final File ROOTS_FILE = new File(CommonUtils.getUserSettingsDir(), "ttroot.cache");
    
    /** File where tiger tree data is stored. */
    private final File DATA_FILE = new File(CommonUtils.getUserSettingsDir(), "ttdata.cache"); 
        
    /** Whether or not data dirtied since the last time we saved. */
    private volatile boolean dirty = false;
    
    private final HashTreeFactory tigerTreeFactory;
    private final Library managedFileList;
    
    @Inject
    HashTreeCacheImpl(HashTreeFactory tigerTreeFactory, Library managedFileList) {
        this.tigerTreeFactory = tigerTreeFactory;
        this.managedFileList = managedFileList;
        Tuple<Map<URN, URN>, Map<URN, HashTree>> tuple = loadCaches();
        for(Map.Entry<URN, URN> entry : tuple.getFirst().entrySet()) {
            SHA1_TO_ROOT_MAP.put(entry.getKey(), new SimpleFuture<URN>(entry.getValue()));
        }        
        for(Map.Entry<URN, HashTree> entry : tuple.getSecond().entrySet()) {
            TTREE_MAP.put(entry.getKey(), new SimpleFuture<HashTree>(entry.getValue()));
        }
    }
    
    public HashTree getHashTreeAndWait(FileDesc fd, long timeout) throws InterruptedException, TimeoutException, ExecutionException {
        if (fd instanceof IncompleteFileDesc) {
            throw new IllegalArgumentException("fd must not inherit from IncompleFileDesc");
        }
        
        if(fd.getSHA1Urn() != null) {
            Future<HashTree> futureTree = getOrScheduleHashTreeFuture(fd);
            return futureTree.get(timeout, TimeUnit.MILLISECONDS);
        } else {
            return null;
        }
    }
    
    @Override
    public synchronized HashTree getHashTree(FileDesc fd) {
        if(fd.getSHA1Urn() != null) {
            Future<HashTree> futureTree = getOrScheduleHashTreeFuture(fd);
            return getTreeFromFuture(fd.getSHA1Urn(), futureTree);
        } else {
            return null;
        }
    }
    
    private HashTree getTreeFromFuture(URN sha1, Future<HashTree> futureTree) {
        if(futureTree.isDone()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Future tree exists for: " + sha1 + " and is finished");
            }
            try {
                return futureTree.get();
            } catch (InterruptedException e) {
                LOG.debug("interrupted while hashing tree", e);
                TTREE_MAP.remove(sha1);
            } catch (ExecutionException e) {
                LOG.debug("error while hashing tree", e);
                TTREE_MAP.remove(sha1);
            } catch(CancellationException e) {
                LOG.debug("cancelled while hashing tree", e);
                TTREE_MAP.remove(sha1);                
            }
            return null;
        } else {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Future tree exists for: " + sha1 + " but is not finished");
            }
            return null;
        }
    }
    
    private URN getRootFromFuture(URN sha1, Future<URN> futureRoot) {
        if(futureRoot.isDone()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Future root exists for: " + sha1 + " and is finished");
            }
            try {
                return futureRoot.get();
            } catch (InterruptedException e) {
                LOG.debug("interrupted while hashing root", e);
                SHA1_TO_ROOT_MAP.remove(sha1);
            } catch (ExecutionException e) {
                LOG.debug("error while hashing root", e);
                SHA1_TO_ROOT_MAP.remove(sha1);
            } catch(CancellationException e) {
                LOG.debug("cancelled while hashing root", e);
                SHA1_TO_ROOT_MAP.remove(sha1);                
            }
            return null;
        } else {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Future root exists for: " + sha1 + " but is not finished");
            }
            return null;
        }
    }
    
    @Override
    public synchronized URN getOrScheduleHashTreeRoot(FileDesc fd) {
        URN sha1 = fd.getSHA1Urn();
        if(sha1 != null) {
            Future<HashTree> futureTree = TTREE_MAP.get(sha1);
            Future<URN> futureRoot = SHA1_TO_ROOT_MAP.get(sha1);
            HashTree tree = futureTree == null ? null : getTreeFromFuture(sha1, futureTree);        
            URN root = futureRoot == null ? null : getRootFromFuture(sha1, futureRoot);
            if(tree != null) {
                if(LOG.isDebugEnabled()) 
                    LOG.debug("Returning root from tree");
                return tree.getTreeRootUrn();
            } else if(root != null) {
                if(LOG.isDebugEnabled()) 
                    LOG.debug("Returning root from future");
                return root;
            } else {
                if(!(fd instanceof IncompleteFileDesc)) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Scheduling: " + sha1 + " for tree root");
                    }
                    futureRoot = QUEUE.submit(new RootRunner(fd));
                    SHA1_TO_ROOT_MAP.put(sha1, futureRoot);
                } else {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring: " + sha1 + " because FD is incomplete.");
                    }
                }
                return null;
            }
        } else {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Returning null for root because no sha1 for fd: " + fd);
            }
            return null;
        }
    }
    
    private synchronized Future<HashTree> getOrScheduleHashTreeFuture(FileDesc fd) {
        URN sha1 = fd.getSHA1Urn();
        Future<HashTree> futureTree = TTREE_MAP.get(sha1);
        if(futureTree == null && !(fd instanceof IncompleteFileDesc)) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Scheduling: " + sha1 + " for full tree");
            }
            // Cancel any pending root, since we're going to do the full tree.
            Future<URN> futureRoot = SHA1_TO_ROOT_MAP.get(sha1);
            if(futureRoot != null && !futureRoot.isDone()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Cancelling: " + sha1 + " from root schedule");
                }
                futureRoot.cancel(true);
            }
            futureTree = QUEUE.submit(new HashTreeRunner(fd));
            TTREE_MAP.put(sha1, futureTree);
        } 
        
        return futureTree;
    }

    @Override
    public synchronized HashTree getHashTree(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        
        Future<HashTree> futureTree = TTREE_MAP.get(sha1);
        if(futureTree != null) {
            return getTreeFromFuture(sha1, futureTree);
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("No future tree exists for: " + sha1);
        }

        return null;
    }
    
    @Override
    public synchronized URN getHashTreeRootForSha1(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        
        HashTree tree = getHashTree(sha1);
        if(tree != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Retrieving root from tree for: " + sha1);
            }
            return tree.getTreeRootUrn();
        } else {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Retrieving root from root map for: " + sha1);
            }
            Future<URN> urnFuture = SHA1_TO_ROOT_MAP.get(sha1);
            if(urnFuture != null) {
                return getRootFromFuture(sha1, urnFuture);
            } else {
                if(LOG.isDebugEnabled())
                    LOG.debug("No future root exists for: " + sha1);
                return null;
            }
        }
    }
    
    @Override
    public synchronized void purgeTree(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        Future<HashTree> futureTree = TTREE_MAP.remove(sha1);
        if(futureTree != null) {
            futureTree.cancel(true);
            dirty = true;
        }
    }

    @Override
    public synchronized HashTree addHashTree(URN sha1, HashTree tree) {
        boolean shouldAdd = hashTreeCalculated(sha1, tree);
        if(shouldAdd) {
            Future<HashTree> oldFuture = TTREE_MAP.put(sha1, new SimpleFuture<HashTree>(tree));
            if(oldFuture != null) {
                oldFuture.cancel(true);
            }
            return tree;
        }
        return null;
    }
    
    private synchronized boolean hashTreeCalculated(URN sha1, HashTree tree) {
        URN root = tree.getTreeRootUrn();
        addRoot(sha1, root);
        if (tree.isGoodDepth()) {
            Future<URN> futureRoot = SHA1_TO_ROOT_MAP.remove(sha1);
            if(futureRoot != null) {
                futureRoot.cancel(true);
            }
            
            dirty = true;
            if (LOG.isDebugEnabled())
                LOG.debug("added hashtree for urn " +
                          sha1 + ";" + tree.getRootHash());
            return true;
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("hashtree for urn " + sha1 + " had bad depth");
            }
            return false;
        }
    }
    
    @Override
    public synchronized void addRoot(URN sha1, URN ttroot) {
        if (!sha1.isSHA1() || !ttroot.isTTRoot()) {
            throw new IllegalArgumentException();
        }
        Future<URN> oldFuture = SHA1_TO_ROOT_MAP.put(sha1, new SimpleFuture<URN>(ttroot));
        if(oldFuture != null) {
            oldFuture.cancel(true);
        }
        dirty = true;
    }
    
    /**
     * Loads values from the root and tree caches
     */
    private Tuple<Map<URN, URN>, Map<URN, HashTree>> loadCaches() {
        Object roots;
        Object trees;
        try {
            roots = ROOTS_FILE.exists() ? FileUtils.readObject(ROOTS_FILE) : new HashMap();
            trees = DATA_FILE.exists() ? FileUtils.readObject(DATA_FILE) : new HashMap();
        } catch (Throwable t) {
            LOG.debug("Error reading from disk.", t);
            roots = new HashMap();
            trees = new HashMap();
        }

        Map<URN,URN> rootsMap = GenericsUtils.scanForMap(roots, URN.class, URN.class, GenericsUtils.ScanMode.REMOVE);
        Map<URN,HashTree> treesMap = GenericsUtils.scanForMap(trees, URN.class, HashTree.class, GenericsUtils.ScanMode.REMOVE);
                
        // remove roots which we have a tree for, 
        // because we don't need them
        rootsMap.keySet().removeAll(treesMap.keySet());                
        
        // and make sure urns are the correct type
        for (Iterator<Map.Entry<URN, URN>> iter = rootsMap.entrySet().iterator();iter.hasNext();) {
            Map.Entry<URN, URN> e = iter.next();
            if (!e.getKey().isSHA1() || !e.getValue().isTTRoot()) {
                iter.remove();
            }
        }
        
        for (Iterator<URN> iter = treesMap.keySet().iterator(); iter.hasNext();) {
            URN urn = iter.next();
            if (!urn.isSHA1()) {
                iter.remove();
            }
        }
        
        // Note: its ok to have roots without trees.
        return new Tuple<Map<URN,URN>, Map<URN,HashTree>>(rootsMap, treesMap);
    }

    /**
     * Removes any stale entries from the map so that they will automatically
     * be replaced.
     * 
     * @param map
     *            the <tt>Map</tt> to check
     */
    private Set<URN> removeOldEntries(Map<URN,URN> roots, Map <URN, HashTree> map, Library library, DownloadManager downloadManager) {
        Set<URN> removed = new HashSet<URN>();
        // discard outdated info
        Iterator<URN> iter = roots.keySet().iterator();
        while (iter.hasNext()) {
            URN sha1 = iter.next();
            if (!library.getFileDescsMatching(sha1).isEmpty()) {
                continue;
            } else if (downloadManager.getIncompleteFileManager().getFileForUrn(sha1) != null) {
                continue;
            } else if (Math.random() > map.size() / 200) {
                // lazily removing entries if we don't have
                // that many anyway. Maybe some of the files are
                // just temporarily unshared.
                continue;
            } else {
                removed.add(sha1);
                iter.remove();
                map.remove(sha1);
                dirty = true;
            }
        }
        return removed;
    }

    @Override
    public void persistCache(Library library, DownloadManager downloadManager) {
        if(!dirty)
            return;
        
        Map<URN,URN> roots;
        Map<URN, HashTree> trees;
        synchronized(this) {
            trees = new HashMap<URN,HashTree>(TTREE_MAP.size());
            for(Map.Entry<URN, Future<HashTree>> entry : TTREE_MAP.entrySet()) {
                if(entry.getValue().isDone()) {
                    try {
                        trees.put(entry.getKey(), entry.getValue().get());
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    } catch (CancellationException e) {
                    }
                }
            }
            roots = new HashMap<URN,URN>(SHA1_TO_ROOT_MAP.size());
            for(Map.Entry<URN, Future<URN>> entry : SHA1_TO_ROOT_MAP.entrySet()) {
                if(entry.getValue().isDone()) {
                    try {
                        roots.put(entry.getKey(), entry.getValue().get());
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    } catch (CancellationException e) {
                    }
                }
            }
        }
        
        Set<URN> removed = removeOldEntries(roots, trees, library, downloadManager);
        if(!removed.isEmpty()) {        
            synchronized(this) {
                SHA1_TO_ROOT_MAP.keySet().removeAll(removed);
                TTREE_MAP.keySet().removeAll(removed);
            }
        }
        
        try {
            FileUtils.writeObject(ROOTS_FILE, roots);
            FileUtils.writeObject(DATA_FILE, trees);
            dirty = false;
        } catch (IOException e) {} 
        // this may any roots added while writing to get lost
    }

    /** Simple runnable that processes the hash of a FileDesc. */
    private class HashTreeRunner implements Callable<HashTree> {
        private final FileDesc FD;
        
        HashTreeRunner(FileDesc fd) {
            FD = fd;
        }
        
        public HashTree call() throws IOException {
            URN sha1 = FD.getSHA1Urn();
            HashTree tree = tigerTreeFactory.createHashTree(FD); // BLOCKING
            hashTreeCalculated(sha1, tree);
            return tree;
        }
    }
    
    /** Simple runnable that processes the hash tree root of a FileDesc. */
    private class RootRunner implements Callable<URN> {        
        private final FileDesc FD;
        
        RootRunner(FileDesc fd) {
            FD = fd;
        }
        
        public URN call() throws IOException, InterruptedException {
            if(managedFileList.getFileDescsMatching(FD.getSHA1Urn()).isEmpty()) {
                throw new IOException("no FDs with SHA1 anymore.");
            }
            
            URN ttRoot = URN.createTTRootFile(FD.getFile()); // BLOCKING
            List<FileDesc> fds = managedFileList.getFileDescsMatching(FD.getSHA1Urn());
            for(FileDesc fd : fds) {
                fd.addUrn(ttRoot);
            }
            dirty = true;
            return ttRoot;
        }
    }    
}

