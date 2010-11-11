package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureTask;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.FileProcessingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.XmlController;

@Singleton
class LibraryImpl implements Library, FileCollection {
    
    private static final Log LOG = LogFactory.getLog(LibraryImpl.class);
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster;
    private final EventMulticaster<LibraryStatusEvent> managedListListenerSupport;
    private final EventMulticaster<FileViewChangeEvent> fileListListenerSupport;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final UrnCache urnCache;
    private final FileDescFactory fileDescFactory;
    private final ListeningExecutorService folderLoader;
    private final ListeningExecutorService diskIoService;
    private final PropertyChangeSupport changeSupport;
    private final DangerousFileChecker dangerousFileChecker;
    private final EventListenerList<FileProcessingEvent> fileProcessingListeners;
    private final XmlController xmlController;
    private final URNFilter urnFilter;
    private final CategoryManager categoryManager;
    
    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer managed.
     * INVARIANT: for all i, files[i]==null, or files[i].index==i and either
     *  files[i]._path is in a managed directory with a managed extension or
     *  files[i]._path is the incomplete directory if files[i] is an IncompleteFileDesc.
     */
    private final List<FileDesc> files;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    private final Map<File, FileDesc> fileToFileDescMap;
    
    /**
     * The map of pending calculations for each File.
     */
    private final Map<File, Future> fileToFutures;
 
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in urnMap, for all i in urnMap.get(k),
     * files[i].containsUrn(k).  Likewise for all i, for all k in
     * files[i].getUrns(), rnMap.get(k) contains i.
     */
    private final Map<URN, IntSet> urnMap;
    
    /** All the library data for this library -- loaded on-demand. */
    private final LibraryFileData fileData;  
    
    /** The revision this finished loading. */
    private volatile boolean loadingFinished = false;
    
    /** The number of files that are pending calculation. */
    private final AtomicInteger pendingFiles = new AtomicInteger(0);
    
    @Inject
    LibraryImpl(SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster,
                UrnCache urnCache,
                FileDescFactory fileDescFactory,
                EventMulticaster<LibraryStatusEvent> managedListSupportMulticaster,
                DangerousFileChecker dangerousFileChecker,
                XmlController xmlController,
                @DiskIo ListeningExecutorService diskIoService,
                EventListenerList<FileProcessingEvent> processingListenerList,
                URNFilter urnFilter, CategoryManager categoryManager
                ) {
        this.fileData = new LibraryFileData(categoryManager);
        this.urnCache = urnCache;
        this.fileDescFactory = fileDescFactory;
        this.fileDescMulticaster = fileDescMulticaster;
        this.managedListListenerSupport = managedListSupportMulticaster;
        this.fileListListenerSupport = new EventMulticasterImpl<FileViewChangeEvent>();
        this.folderLoader = ExecutorsHelper.newProcessingQueue("Library Folder Loader"); 
        this.files = new ArrayList<FileDesc>();
        this.urnMap = new HashMap<URN, IntSet>();
        this.fileToFileDescMap = new HashMap<File, FileDesc>();
        this.fileToFutures = new HashMap<File, Future>();
        this.changeSupport = new SwingSafePropertyChangeSupport(this);
        this.dangerousFileChecker = dangerousFileChecker;
        this.xmlController = xmlController;
        this.diskIoService = diskIoService;
        this.fileProcessingListeners = processingListenerList;
        this.urnFilter = urnFilter;
        this.categoryManager = categoryManager;
    }
    
    @Override
    public String getName() {
        return "Library";
    }
    
    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public long getNumBytes() {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /** Gets the library data, loading it if necessary. */
    LibraryFileData getLibraryData() {
        if(!fileData.isLoaded()) {
            fileData.load();
        }
        return fileData;
    }
    
    /** Initializes all listeners. */
    void initialize() {
    }

    /** 
     * Creates a {@link FileViewChangeFailedException} with the proper info.
     */
    private FileViewChangeFailedException createFailureException(File newFile, FileDesc oldFd, FileViewChangeFailedException.Reason reason) {
        if(oldFd != null) {
            return new FileViewChangeFailedException(oldFd.getFile(), FileViewChangeEvent.Type.FILE_CHANGE_FAILED, reason);
        } else {
            return new FileViewChangeFailedException(newFile, FileViewChangeEvent.Type.FILE_ADD_FAILED, reason);
        }
    }
    
    /**
     * Dispatches a failure, sending a CHANGE_FAILED & REMOVE event if
     * oldFileDesc is non-null, an ADD_FAILED otherwise.
     */
    private void dispatchFailure(File file, FileDesc oldFileDesc) {
        if(oldFileDesc != null) {
            // First dispatch a CHANGE_FAILED for the new event
            dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGE_FAILED, oldFileDesc.getFile(), oldFileDesc, file));
            // Then dispatch a REMOVE for the old FD.
            dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_REMOVED, oldFileDesc));
        } else {
            // Just dispatch an ADD_FAIL for the file.
            dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_ADD_FAILED, file));
        }
    }
    
    void dispatch(FileViewChangeEvent event) {
        fileListListenerSupport.broadcast(event);
    }
    
    void dispatch(LibraryStatusEvent event) {
        managedListListenerSupport.broadcast(event);
    }

    @Override
    public FileDesc getFileDesc(File file) {
        file = FileUtils.canonicalize(file);        
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.get(file);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public FileDesc getFileDescForIndex(int index) {
        rwLock.readLock().lock();
        try {
            if(index < 0 || index >= files.size()) {
                return null;
            }
            return files.get(index);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean contains(File file) {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.containsKey(FileUtils.canonicalize(file));
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        return getFileDescForIndex(fileDesc.getIndex()) == fileDesc;
    }
    
    /** Adds this incomplete file to the list of managed files */
    void addIncompleteFile(File incompleteFile,
                           Set<? extends URN> urns,
                           String name,
                           long size,
                           VerifyingFile vf) {
        // Note -- Purposely not using canonicalize so we can fail on invalid ones.
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } catch(IOException ioe) {
            //invalid file?... don't add incomplete file.
            return;
        }
        
        FileDesc fd = null;
        rwLock.writeLock().lock();
        try {
            if(!fileToFileDescMap.containsKey(incompleteFile)) {
                // no indices were found for any URN associated with this
                // IncompleteFileDesc... add it.
                fd = fileDescFactory.createIncompleteFileDesc(incompleteFile, urns, files.size(), name, size, vf);
                files.add(fd);
                fileToFileDescMap.put(incompleteFile, fd);
                updateUrnIndex(fd);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(fd != null) {
            dispatch(new FileViewChangeEvent(LibraryImpl.this, FileViewChangeEvent.Type.FILE_ADDED, fd));
        }
    }

    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {
        fileListListenerSupport.addListener(listener);
    }
    
    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
        return fileListListenerSupport.removeListener(listener);
    }
    
    @Override
    public void addManagedListStatusListener(EventListener<LibraryStatusEvent> listener) {
        managedListListenerSupport.addListener(listener);
    }
    
    @Override
    public void removeManagedListStatusListener(EventListener<LibraryStatusEvent> listener) {
        managedListListenerSupport.removeListener(listener);
    }

    @Override
    public void clear() {
        clearImpl();
        getLibraryData().clearFileData();
    }
    
    /** Actually performs the clear & dispatches an event. Does not save the clear to LibraryFileData. */
    private void clearImpl() {
        Map<File, Future> fileFutures;
        rwLock.writeLock().lock();
        try {
            fileFutures = new HashMap<File, Future>(fileToFutures);
            fileToFutures.clear();
            files.clear();
            urnMap.clear();
            fileToFileDescMap.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
        
        for(Map.Entry<File, Future> entry : fileFutures.entrySet()) {
            if(entry.getValue().cancel(true)) {
                broadcastFinished(entry.getKey());
            }
        }
        
        dispatch(new FileViewChangeEvent(LibraryImpl.this, FileViewChangeEvent.Type.FILES_CLEARED, true));
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        List<FileDesc> matching = getFileDescsMatching(urn);
        if(matching.isEmpty()) {
            return null;
        } else {
            return matching.get(0);
        }
    }
    
    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        rwLock.readLock().lock();
        try {
            IntSet urnsMatching = urnMap.get(urn);
            if(urnsMatching == null || urnsMatching.size() == 0) {
                return Collections.emptyList();
            } else if(urnsMatching.size() == 1) { // Optimal case
                return Collections.singletonList(files.get(urnsMatching.iterator().next()));
            } else {
                return CollectionUtils.listOf(new FileViewIterator(this, urnsMatching));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return CollectionUtils.readOnlyIterator(fileToFileDescMap.values().iterator());
    }
    
    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                rwLock.readLock().lock();
                try {
                    return new ThreadSafeLibraryIterator();
                } finally {
                    rwLock.readLock().unlock();
                }
            }
        };
    }

    @Override
    public boolean remove(FileDesc fileDesc) {
        return remove(fileDesc.getFile());
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        // an FD should never exist unless it is already in the library
        assert contains(fileDesc);
        return true;
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        return add(file, LimeXMLDocument.EMPTY_LIST);
    }    
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> list) {
        return add(file, list, null);
    }
    
    /**
     * Adds a managed file.  Returns a future that can be used to get the FD or failure
     * event from adding the file.  Failures are throws as ExecutionExceptions from the Future.
     * 
     * @param file - the file to be added
     * @param metadata - any LimeXMLDocs associated with this file
     * @param rev - current  version of LimeXMLDocs being used
     * @param oldFileDesc the old FileDesc this is replacing
     */
    private ListeningFuture<FileDesc> add(File originalFile, 
            final List<? extends LimeXMLDocument> metadata,
            final FileDesc oldFileDesc) {
        LOG.debugf("Attempting to load file: {0}", originalFile);

        File file = null;
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(originalFile);
        } catch (IOException e) {
            LOG.debugf("Not adding {0} because canonicalize failed", originalFile);
            dispatchFailure(originalFile, null);            
            return new SimpleFuture<FileDesc>(createFailureException(originalFile, oldFileDesc, FileViewChangeFailedException.Reason.CANT_CANONICALIZE));
        }
        
        rwLock.readLock().lock();
        try {
            // Exit if already added.
            if(fileToFileDescMap.containsKey(file)) {
                LOG.debugf("Not loading because file already loaded {0}", file);
                dispatchFailure(file, oldFileDesc);
                return new SimpleFuture<FileDesc>(createFailureException(file, oldFileDesc, FileViewChangeFailedException.Reason.ALREADY_MANAGED));
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        //make sure a FileDesc can be created from this file
        if (!LibraryUtils.isFilePhysicallyManagable(file)) {
            LOG.debugf("Not adding {0} because file isn't physically manageable", file);
            dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(createFailureException(file, oldFileDesc, FileViewChangeFailedException.Reason.NOT_MANAGEABLE));
        }
        
        if (!LibraryUtils.isFileAllowedToBeManaged(file, categoryManager)) {
            LOG.debugf("Not adding {0} because files of this type are not allowed to be managed", file);
            dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(createFailureException(file, oldFileDesc, FileViewChangeFailedException.Reason.FILE_TYPE_NOT_ALLOWED));
        }
        
        PendingFuture task = new PendingFuture();
        
        // Prefer the original file if the canonical file is the same
        if (file.getPath().equals(originalFile.getPath())) {
            getLibraryData().addManagedFile(originalFile);
            startLoadingFileDesc(originalFile, urnCache.getUrns(file), metadata, oldFileDesc, task);
        }
        else { // Otherwise replace the non canonical file with the new one and use that
            
            // Make sure the new canonical is also interned
            file = new File(file.getPath().intern());
            
            getLibraryData().addOrRenameManagedFile(file, originalFile);
            startLoadingFileDesc(file, urnCache.getUrns(file), metadata, oldFileDesc, task);
        }
                
        return task;
    }
    
    private void setFutureForFile(File file, Future future) {
        rwLock.writeLock().lock();
        try {
            fileToFutures.put(file, future);
        } finally {
            rwLock.writeLock().unlock();
        }    
    }
    
    private void removeFutureForFile(File file) {
        rwLock.writeLock().lock();
        try {
            fileToFutures.remove(file);
        } finally {
            rwLock.writeLock().unlock();
        } 
    }
    
    /**
     * Create the FD with or without URNs or XML.
     * Step 1 of loading a filedesc.
     * 
     * Then proceed to either step 2 (calculate URNs if none exist) or step 3 (check if dangerous & load XML)
     */
    private void startLoadingFileDesc(final File file, Set<URN> urns,
            final List<? extends LimeXMLDocument> metadata, final FileDesc oldFileDesc,
            final PendingFuture task) {
        final FileDesc fd = createAndAddFileDesc(file, metadata, urns, oldFileDesc, task);
        // We were able to succesfully create an FD -- now finish it off!
        if(fd != null) {
            if(UrnSet.getSha1(urns) == null) {
                // Create a FileDesc & add it before we have a set of URNs for it.
                ListeningFuture<Set<URN>> urnFuture = urnCache.calculateAndCacheSHA1(file);
                setFutureForFile(file, urnFuture);  
                LOG.debugf("Submitting URN future for {0}", file);
                broadcastQueued(file);
                urnFuture.addFutureListener(new EventListener<FutureEvent<Set<URN>>>() {
                    @Override
                    public void handleEvent(FutureEvent<Set<URN>> event) {
                        LOG.debugf("Running URN future for {0}", file);
                        removeFutureForFile(file);
                        if(contains(fd)) {
                            addUrnsToFileDesc(fd, metadata, event, task, oldFileDesc);
                        }
                        
                        if(event.getType() != FutureEvent.Type.CANCELLED) {
                            broadcastFinished(file);
                        }
                    }
                });
            } else {
                // We may be able to exit immediately if all the following is true:
                // 1) The file is already "safe" checked
                // 2) Cached XML was loaded, or the file format doesn't allow for XML
                boolean safe = getLibraryData().isFileSafe(fd.getSHA1Urn().toString());
                boolean loadedXML = !fd.getLimeXMLDocuments().isEmpty();
                boolean allowsXML = xmlController.canConstructXml(fd);
                if(safe && (loadedXML || !allowsXML)) {
                    // We're done immediately, woohooo!!
                    LOG.debugf("File loaded immediately! {0}", file);
                    removeFutureForFile(file);
                    task.set(fd);
                } else {
                    LOG.debugf("URNs precalculated for {0}, but needs safe-check or XML", file);
                    broadcastQueued(file);
                    rwLock.writeLock().lock();
                    try {
                        LOG.debugf("Submitting finish loading FD for {0}", fd.getFile());
                        setFutureForFile(fd.getFile(), diskIoService.submit(new Runnable() {
                            @Override
                            public void run() {
                                broadcastProcessing(fd.getFile());
                                LOG.debugf("Running finish loading FD for {0}", fd.getFile());
                                removeFutureForFile(fd.getFile());
                                if(contains(fd)) {
                                    finishLoadingFileDesc(fd, metadata, task, false, oldFileDesc);
                                }
                                broadcastFinished(file);
                            }
                        }));
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
            }
        } else {
            LOG.debugf("Unable to create FileDesc for {0}", file);
        }
    }
    
    private void broadcastProcessing(File file) {
        fileProcessingListeners.broadcast(new FileProcessingEvent(FileProcessingEvent.Type.PROCESSING, file));
    }
    
    private void broadcastQueued(File file) {
        fileProcessingListeners.broadcast(new FileProcessingEvent(FileProcessingEvent.Type.QUEUED, file));
    }
    
    private void broadcastFinished(File file) {
        fileProcessingListeners.broadcast(new FileProcessingEvent(FileProcessingEvent.Type.FINISHED, file));
    }
    
    /** Step 2 of loading FDs. */
    private void addUrnsToFileDesc(FileDesc fd, List<? extends LimeXMLDocument> metadata,
            FutureEvent<Set<URN>> urnEvent, PendingFuture task, FileDesc oldFileDesc) {
        Set<URN> urns = urnEvent.getResult();
        // If the URN couldn't be calculated
        if (urns == null || urns.isEmpty()) {
            remove(fd.getFile());
            Exception ex = createFailureException(fd.getFile(), oldFileDesc, FileViewChangeFailedException.Reason.ERROR_LOADING_URNS);
            ex.initCause(urnEvent.getException());
            task.setException(ex);
        } else {
            // Add URNs.
            for(URN urn : urns) {
                fd.addUrn(urn);
            }
            rwLock.writeLock().lock();
            try {
                updateUrnIndex(fd);
            } finally {
                rwLock.writeLock().unlock();
            }
            
            // Now that we have a URN, preload the cached XML for it.
            xmlController.loadCachedXml(fd, metadata);
            // Finish loading it immediately, since we're in a blocking thread already
            finishLoadingFileDesc(fd, metadata, task, true, oldFileDesc);
        }
    }
    
    /** Returns the newly created FD & dispatches events about its creation. */
    private FileDesc createAndAddFileDesc(File file, List<? extends LimeXMLDocument> metadata, Set<URN> urns,
            FileDesc oldFileDesc, PendingFuture task) {
        FileDesc fd = null;
        FileDesc newFD = null;
        boolean failed = false;
        
        rwLock.writeLock().lock();
        try {
            newFD = createFileDesc(file, urns, files.size());
            fd = newFD;
            if(fd != null) {
                if(contains(file)) {
                    failed = true;
                    fd = getFileDesc(file);
                } else {
                    files.add(fd);
                    fileToFileDescMap.put(file, fd);
                    updateUrnIndex(fd);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(fd == null) {
            dispatchFailure(file, oldFileDesc);
            task.setException(createFailureException(file, oldFileDesc, FileViewChangeFailedException.Reason.INVALID_URN));
        } else if(failed) {
            LOG.debugf("Couldn't load FD because FD with file {0} exists already.  FD: {1}", file, fd);
            dispatchFailure(file, oldFileDesc);
            task.setException(createFailureException(file, oldFileDesc, FileViewChangeFailedException.Reason.ALREADY_MANAGED));
        } else {
            assert newFD != null; 
            
            // If we had a SHA1 urn, attempt to load cached XML so it exists
            // before the first FILE_ADDED event is sent.
            if (fd.getSHA1Urn() != null) {
                xmlController.loadCachedXml(fd, metadata);
            }
            
            // It is very important that the events get dispatched
            // prior to setting the value on the task, so that other FileLists
            // listening to these events can receive & process the event
            // prior to the future.get() returning.
            if(oldFileDesc == null) {
                LOG.debugf("Added file: {0}", file);
                dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_ADDED, fd));
            } else {
                LOG.debugf("Changed to new file: {0}", file);
                dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGED, oldFileDesc, fd));
            }
            
            // DO NOT SET THE TASK UNTIL THE FD FINISHES ENTIRELY!
        }
        
        return newFD;
    }

    /** Finishes the process of loading the FD. */
    private void finishLoadingFileDesc(FileDesc fd, List<? extends LimeXMLDocument> metadata,
            PendingFuture task, boolean alwaysSendMetaChange, FileDesc oldFileDesc) {
        // Note: Dangerous file checking may block for a period of time.
        URN sha1 = fd.getSHA1Urn();
        boolean dangerous = false;
        if(!getLibraryData().isFileSafe(sha1.toString())) {
            dangerous = dangerousFileChecker.isDangerous(fd.getFile());
            getLibraryData().setFileSafe(sha1.toString(), !dangerous);
        }
        
        if(dangerous) {
            remove(fd.getFile());
            task.setException(createFailureException(fd.getFile(), oldFileDesc, FileViewChangeFailedException.Reason.DANGEROUS_FILE));
        } else {
            boolean loaded = xmlController.loadXml(fd);
            if(alwaysSendMetaChange || loaded) {
                dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_META_CHANGED, fd));
            }
            task.set(fd);
        }
        
        LOG.debugf("Finished loading FD for {0}", fd.getFile());
    }
  
    /**
     * Creates an FD for the file.  Returns null if the FD cannot be created
     * (because the URN validator says it's not valid, for example).
     */
    private FileDesc createFileDesc(File file, Set<? extends URN> urns, int index){
        if(urnFilter.isBlacklisted(UrnSet.getSha1(urns))) {
            return null;
        } else {
            return fileDescFactory.createFileDesc(file, urns, index);
        }
    }

    @Override
    public boolean remove(File file) {
        LOG.debugf("Removing file: {0}", file);                

        file = FileUtils.canonicalize(file);
        FileDesc fd = removeInternal(file);        
        if(fd != null) {
            dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_REMOVED, fd));
        }
        return fd != null;
    }
    
    /**
     * Removes the FD for this file, returning the FD that was removed.
     * This does NOT dispatch a remove event.  It will update the libraryData
     * to signify the file should not be shared, though.
     * 
     * The file should be canonicalized already.
     */
    private FileDesc removeInternal(File file) {
        FileDesc fd;
        boolean cancelled = false;
        rwLock.writeLock().lock();
        try {
            fd = fileToFileDescMap.get(file);
            if(fd != null) {
                removeFileDesc(file, fd);
            }
            Future future = fileToFutures.remove(file);
            if(future != null) {
                cancelled = future.cancel(true);
                
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        if(fd != null) {
            getLibraryData().removeManagedFile(file);
        }
        if(cancelled) {
            broadcastFinished(file);
        }
        return fd;
    }
    
    /** Removes the given FD. */
    private void removeFileDesc(File file, FileDesc fd) {
        removeUrnIndex(fd);
        FileDesc rm = files.set(fd.getIndex(), null);
        assert rm == fd;
        rm = fileToFileDescMap.remove(file);
        assert rm == fd;
        fileDescMulticaster.removeListeners(fd);
    }

    /** Generic method for adding a fileDesc's URNS to a map */
    private void updateUrnIndex(FileDesc fileDesc) {
        URN sha1 = fileDesc.getSHA1Urn();
        if(sha1 != null) {
            IntSet indices = urnMap.get(sha1);
            if (indices == null) {
                indices = new IntSet();
                urnMap.put(sha1, indices);
            }
            indices.add(fileDesc.getIndex());
        }
    }
    
    /** 
     * Removes stored indices for a URN associated with a given FileDesc
     */
    private void removeUrnIndex(FileDesc fileDesc) {
        URN sha1 = fileDesc.getSHA1Urn();
        if(sha1 != null) {
            // Lookup each of desc's URN's ind _urnMap.
            IntSet indices = urnMap.get(sha1);
            if (indices == null) {
                assert fileDesc instanceof IncompleteFileDesc;
                return;
            }
    
            // Delete index from set. Remove set if empty.
            indices.remove(fileDesc.getIndex());
            if(indices.size() == 0) {
                urnMap.remove(sha1);
            }
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> fileRenamed(File oldName, final File newName) {
        LOG.debugf("Attempting to rename: {0} to: {1}", oldName, newName);      
        
        oldName = FileUtils.canonicalize(oldName);
        FileDesc fd = removeInternal(oldName);        
        if (fd != null) {
            // TODO: It's dangerous to prepopulate, because we might actually
            //       be called with wrong data, giving us wrong URNs.
            // Prepopulate the cache with new URNs.
            urnCache.addUrns(newName, fd.getUrns());
            List<LimeXMLDocument> xmlDocs = new ArrayList<LimeXMLDocument>(fd.getLimeXMLDocuments());
            return add(newName, xmlDocs, fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(oldName, FileViewChangeEvent.Type.FILE_CHANGE_FAILED, FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED));
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        LOG.debugf("File Changed: {0}", file);

        file = FileUtils.canonicalize(file);
        FileDesc fd = removeInternal(file);
        if (fd != null) {
            urnCache.removeUrns(file); // Explicitly remove URNs to force recalculating.
            return add(file, xmlDocs, fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(file, FileViewChangeEvent.Type.FILE_CHANGE_FAILED, FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED));
        }
    }
    
    /**
     * Loads all files from prior sessions.
     * This returns immediately with a Future that contains
     * the list of all Future FileDescs that will be added.
     */  
    ListeningFuture<List<ListeningFuture<FileDesc>>> loadManagedFiles() {
        ListeningFuture<List<ListeningFuture<FileDesc>>> future = diskIoService.submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() {
                return loadSettingsInternal();
            }
        });
        return future;
    }

    /** 
     * Loads all shared files, putting them in a queue for being added.
     *
     * If the current revision ever changed from the expected revision, this returns
     * immediately.
     */
    private List<ListeningFuture<FileDesc>> loadSettingsInternal() {
        LOG.debugf("Loading Library");
        
        loadingFinished = false;
        clearImpl();
        fireLoading();
        final List<ListeningFuture<FileDesc>> futures = loadManagedFilesInternal();
        addLoadingListener(futures);
        return futures;
    }
    
    void fireLoading() {
        changeSupport.firePropertyChange("hasPending", false, true);
    }
    
    private void addLoadingListener(final List<ListeningFuture<FileDesc>> futures) {
        if(futures.isEmpty() && pendingFiles.get() == 0) {
            loadFinished();
        } else if(!futures.isEmpty()) {
            pendingFiles.addAndGet(futures.size());
            
            EventListener<FutureEvent<FileDesc>> listener = new EventListener<FutureEvent<FileDesc>>() {
                @Override
                public void handleEvent(FutureEvent<FileDesc> event) {
                    if(pendingFiles.addAndGet(-1) == 0) {
                        loadFinished(); 
                    }
                }
            };
            
            for(ListeningFuture<FileDesc> future : futures) {
                future.addFutureListener(listener);
            }
        }
    }
    
    /** Kicks off necessary stuff for loading being done. */
    private void loadFinished() {
        changeSupport.firePropertyChange("hasPending", true, false);
        if(!loadingFinished) {
            loadingFinished = true;
            LOG.debugf("Finished loading revision");
            dispatch(new LibraryStatusEvent(this, LibraryStatusEvent.Type.LOAD_FINISHING));
            save();
            dispatch(new LibraryStatusEvent(this, LibraryStatusEvent.Type.LOAD_COMPLETE));
        }
    }
    
    @Override
    public boolean isLoadFinished() {
        return loadingFinished;
    }

    private List<ListeningFuture<FileDesc>> loadManagedFilesInternal() {
        List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
        
        // TODO: We want to always share this stuff, not just approved extensions.
//        addManagedDirectory(extensions, LibraryUtils.PROGRAM_SHARE, rev, false, true, true, futures);
//        addManagedDirectory(extensions, LibraryUtils.PREFERENCE_SHARE, rev, false, true, true, futures);
        
        // A listener that will remove individually managed files if they can't load.
        EventListener<FutureEvent<FileDesc>> indivListeners = new EventListener<FutureEvent<FileDesc>>() {
            @Override
            public void handleEvent(FutureEvent<FileDesc> event) {
                switch(event.getType()) {
                case EXCEPTION:
                    if(event.getException().getCause() instanceof FileViewChangeFailedException) {
                        FileViewChangeFailedException ex = (FileViewChangeFailedException)event.getException().getCause();
                        switch(ex.getReason()) {
                        case CANT_CANONICALIZE:
                        case INVALID_URN:
                        case NOT_MANAGEABLE:
                        case FILE_TYPE_NOT_ALLOWED:
                            getLibraryData().removeManagedFile(ex.getFile());
                            break;
                        }
                    }
                    break;
                }
            }
        };
        int i = 0;
        for(File file : getLibraryData().getManagedFiles()) {
            // don't hog CPU...
            if(i % 2 == 0) {
                Thread.yield();
            }
            ListeningFuture<FileDesc> future = add(file, LimeXMLDocument.EMPTY_LIST, null);
            future.addFutureListener(indivListeners);
            futures.add(future);
            i++;
        }
        
        return futures;
    }

    /** Dispatches a SAVE event & tells library data to save. */
    void save() {
        dispatch(new LibraryStatusEvent(this, LibraryStatusEvent.Type.SAVE));
        urnCache.persistCache();
        getLibraryData().save();
    }

    /**
     * Returns true if this folder is can have files from it added.
     */
    private boolean isFolderManageable(File folder) {
        folder = FileUtils.canonicalize(folder);
        
        if (!folder.isDirectory() || !folder.canRead() || !folder.exists() && folder.getParent() != null) {
            return false;
        }

        if (getLibraryData().isIncompleteDirectory(folder)) {
            return false;
        }

        if (LibraryUtils.isFolderBanned(folder)) {
            return false;
        }
        
        if(LibraryUtils.isSensitiveDirectory(folder)) {
            return false;
        }

        return true;
    }
    
    /** An iterator that works over changes to the list. */
    private class ThreadSafeLibraryIterator implements Iterator<FileDesc> {        
        /** Points to the index that is to be examined next. */
        private int index = 0;
        private FileDesc preview;
        
        private boolean preview() {
            assert preview == null;

            rwLock.readLock().lock();
            try {
                while (index < files.size()) {
                    preview = files.get(index);
                    index++;
                    if (preview != null) {
                        return true;
                    }
                }            
                return false;
            } finally {
                rwLock.readLock().unlock();
            }
            
        }
        
        @Override
        public boolean hasNext() {
            if (preview != null) {
                if (!contains(preview)) {
                    // file was removed in the meantime
                    preview = null;
                }
            }
            return preview != null || preview();
        }
        
        @Override
        public FileDesc next() {
            if (hasNext()) {
                FileDesc item = preview;
                preview = null;
                return item;
            }
            throw new NoSuchElementException();     
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        if(!folder.isDirectory()) {
            return false;
        }
        
        if(!isFolderManageable(folder)) {
            return false;
        }
        
        //reject OSX app folders when program managing is not allowed.
        if(!isProgramManagingAllowed() && "app".equalsIgnoreCase(FileUtils.getFileExtension(folder))) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean isProgramManagingAllowed() {
        return getLibraryData().isProgramManagingAllowed();
    }

    /** A simple empty callable for use in PendingFuture. */
    private final static Callable<FileDesc> EMPTY_CALLABLE = new Callable<FileDesc>() {
        @Override
        public FileDesc call() { return null; }
    };
    
    /** A future that delegates on another future, occasionally. */
    private static class PendingFuture extends ListeningFutureTask<FileDesc> {
        public PendingFuture() {
            super(EMPTY_CALLABLE);
        }
        
        @Override
        public void run() {
            // Do nothing -- there is nothing to run.
        }

        @Override
        // Raise access so we can set the FD. */
        public void set(FileDesc v) {
            super.set(v);
        }

        @Override
        // Raise access so we can set the error. */
        public void setException(Throwable t) {
            super.setException(t);
        }
    }

    /** Removes all items from an IntSet that do not pass the filter. */
    void filterIndexes(IntSet indexes, Predicate<FileDesc> filter) {
        List<Integer> removeList = null;
        rwLock.readLock().lock();
        try {
            IntSetIterator iter = indexes.iterator();
            for(; iter.hasNext(); ) {
                int i = iter.next();
                FileDesc fd = files.get(i);
                if(!filter.apply(fd)) {
                    if(removeList == null) {
                        removeList = new ArrayList<Integer>();
                    }
                    removeList.add(i);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        if(removeList != null) {
            for(Integer i : removeList) {
                indexes.remove(i);
            }
        }
    }
    
    ListeningFuture<List<ListeningFuture<FileDesc>>> scanFolderAndAddToCollection(
            final File folder, final FileFilter fileFilter, final FileCollection collection) {
        Objects.nonNull(folder, "folder");
        Objects.nonNull(fileFilter, "fileFilter");
        Objects.nonNull(collection, "collection");
        return folderLoader.submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            private final List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
            
            @Override
            public List<ListeningFuture<FileDesc>> call() throws Exception {
                if(isDirectoryAllowed(folder)) {
                    File[] files = folder.listFiles(fileFilter);
                    if(files != null) {
                        //must null check files because it can return null if there was an error accessing 
                        //the files the interface is wrong about returning an empty list for all circumstances
                        addFiles(new ArrayList<File>(Arrays.asList(files)));
                    }
                }
                return futures;
            }
            
            private void addFiles(List<File> accumulator) {
                while(accumulator.size() > 0) {
                    File folderOrFile = accumulator.remove(0);
                    if(isDirectoryAllowed(folderOrFile)) {
                        File[] files = folderOrFile.listFiles(fileFilter);
                        if(files != null) {
                            //must null check files because it can return null if there was an error accessing 
                            //the files the interface is wrong about returning an empty list for all circumstances
                            accumulator.addAll(Arrays.asList(files));
                        }
                    } else {
                        futures.add(collection.add(folderOrFile));
                    }
                }
            }
        });
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(final File folder, final FileFilter fileFilter) {
        return scanFolderAndAddToCollection(folder, fileFilter, this);
    }

    @Override
    public boolean isFileAllowed(File file) {
        return LibraryUtils.isFileManagable(file, categoryManager);
    }

    @Override
    public void addFileProcessingListener(EventListener<FileProcessingEvent> listener) {
        fileProcessingListeners.addListener(listener);
    }

    @Override
    public void removeFileProcessingListener(EventListener<FileProcessingEvent> listener) {
        fileProcessingListeners.removeListener(listener);    
    }
    
    @Override
    public void cancelPendingTasks() {
        rwLock.readLock().lock();
        List<File> files = null;
        try {
            files = new ArrayList<File>(fileToFutures.keySet());
        } finally {
            rwLock.readLock().unlock();       
        }
        
        for(File file : files) {
            remove(file);
        }
    }
    
    public int peekPublicSharedListCount() {
        return fileData.peekPublicSharedListCount();
    }
}
