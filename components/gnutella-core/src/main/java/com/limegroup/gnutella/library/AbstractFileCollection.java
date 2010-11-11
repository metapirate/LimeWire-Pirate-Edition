package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureDelegator;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A List of FileDescs that are grouped together 
 */
abstract class AbstractFileCollection extends AbstractFileView implements FileCollection {

    /**  A list of listeners for this list */
    private final SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster;
    
    /** The listener on the ManagedList, to synchronize changes. */
    private final EventListener<FileViewChangeEvent> libraryListener;
    
    /** A rw lock. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    /** The total size of all contained files. */
    private volatile long totalFileSize = 0;
    
    public AbstractFileCollection(LibraryImpl library,
            SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster) {
        super(library);
        this.multicaster = multicaster;
        this.libraryListener = new LibrarySynchonizer();
    }
    
    /** Initializes this list.  Until the list is initialized, it is not valid. */
    protected void initialize() {
        library.addListener(libraryListener);
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(final File folder, final FileFilter fileFilter) {
        return library.scanFolderAndAddToCollection(folder, fileFilter, this);
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        if(!isFileAllowed(file)) {
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(
                    file, FileViewChangeEvent.Type.FILE_ADD_FAILED, 
                    FileViewChangeFailedException.Reason.CANT_ADD_TO_LIST));
        }
        
        FileDesc fd = library.getFileDesc(file);

        if(fd == null) {
            saveChange(canonicalize(file), true); // Save early, will RM if it can't become FD.
            return wrapFuture(library.add(file));
        } else {
            add(fd);
            return futureFor(fd);
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {

        if(!isFileAllowed(file)) {
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(
                    file, FileViewChangeEvent.Type.FILE_ADD_FAILED, 
                    FileViewChangeFailedException.Reason.CANT_ADD_TO_LIST));
        }

        FileDesc fd = library.getFileDesc(file);

        if(fd == null) {
            saveChange(canonicalize(file), true); // Save early, will RM if it can't become FD.
            return wrapFuture(library.add(file, documents));
        } else {
            add(fd);
            return futureFor(fd);
        }
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        if(!isFileDescAllowed(fileDesc)) {
            return false;
        }
        
        if(addFileDescImpl(fileDesc)) {
            saveChange(fileDesc.getFile(), true);
            fireAddEvent(fileDesc);
            return true;
        } else {
            // Must rm from save-state, because we optimistically
            // inserted when adding as a file.
            if(!contains(fileDesc)) {
                saveChange(fileDesc.getFile(), false);
            }
            return false;
        }
    }
        
    /**
     * Performs the actual add. No notification is sent when this returns.
     * @return true if the fileDesc was added, false otherwise
     */
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        Objects.nonNull(fileDesc, "fileDesc");        
        rwLock.writeLock().lock();
        try {
            if(isFileDescAllowed(fileDesc) && getInternalIndexes().add(fileDesc.getIndex())) {
                totalFileSize += fileDesc.getFileSize();
                return true;
            } else {
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean remove(File file) {
        FileDesc fd = library.getFileDesc(file);
        if(fd != null) {
            return remove(fd);
        } else {
            saveChange(canonicalize(file), false);
            return false;
        }        
    }
    
    @Override
    public boolean remove(FileDesc fileDesc) {
        saveChange(fileDesc.getFile(), false);
        if(removeFileDescImpl(fileDesc)) {
            fireRemoveEvent(fileDesc);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Performs the actual remove. No notification is sent when this returns. 
     * @return true if the fileDesc was removed, false otherwise
     */
    protected boolean removeFileDescImpl(FileDesc fileDesc) {
        Objects.nonNull(fileDesc, "fileDesc");        
        rwLock.writeLock().lock();
        try {
            if(getInternalIndexes().remove(fileDesc.getIndex())) {
                totalFileSize -= fileDesc.getFileSize();
                return true;
            } else {
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public long getNumBytes() {
        return totalFileSize;
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return new FileViewIterator(AbstractFileCollection.this, getInternalIndexes());
    }
    
    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                return new ThreadSafeFileViewIterator(AbstractFileCollection.this);
            }
        };
    }

    @Override
    public void clear() {
        clear(false);
    }
    
    /** Clears the list of files.  If fromLibrary is true, the event is slightly different. */
    private void clear(boolean fromLibrary) {
        boolean needsClearing;
        rwLock.writeLock().lock();
        try {
            needsClearing = clearImpl();
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(needsClearing) {
            fireClearEvent(fromLibrary);
        }
    }
    
    /** Performs the actual clear -- returns true if anything was removed from this collection. */
    protected boolean clearImpl() {
        boolean needsClearing = getInternalIndexes().size() > 0;
        getInternalIndexes().clear();
        totalFileSize = 0;
        return needsClearing;
    }
        
    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }

    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {
        multicaster.addListener(this, listener);
    }

    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
        return multicaster.removeListener(this, listener);
    }
    
    /**
     * Fires an addFileDesc event to all the listeners
     * @param fileDesc that was added
     */
    protected void fireAddEvent(FileDesc fileDesc) {
        multicaster.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_ADDED, fileDesc));
    }
    
    /**
     * Fires a removeFileDesc event to all the listeners
     * @param fileDesc that was removed
     */
    protected void fireRemoveEvent(FileDesc fileDesc) {
        multicaster.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_REMOVED, fileDesc));
    }

    /**
     * Fires a changeEvent to all the listeners
     * @param oldFileDesc FileDesc that was there previously
     * @param newFileDesc FileDesc that replaced oldFileDesc
     */
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        multicaster.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGED, oldFileDesc, newFileDesc));
    }
    
    /** Fires a meta-change event to all listeners */
    protected void fireMetaChangeEvent(FileDesc fd) {
        multicaster.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_META_CHANGED, fd));
    }
    
    /** Fires a clear event to all listeners. */
    protected void fireClearEvent(boolean fromLibrary) {
        multicaster.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILES_CLEARED, fromLibrary));
    }
    
    /**
     * Updates the list if a containing file has been renamed
     */
    protected void updateFileDescs(FileDesc oldFileDesc, FileDesc newFileDesc) {     
        boolean failed = false;
        boolean success = false;
        
        // Unfortunately cannot lock between these, since rm & add can be overridden
        // and the overridden methods cannot be expected to be OK with locks.
        if (removeFileDescImpl(oldFileDesc)) {
            if(addFileDescImpl(newFileDesc)) {
            	// the internal LibraryFileData list is destroyed when a FILE_CHANGE occurs, 
            	// so resave ourselves to the list
                saveChange(newFileDesc.getFile(), true);
                success = true;
            } else {
                failed = true;
            }
        } // else nothing to remove -- neither success nor failure
        
        if(success) {
            fireChangeEvent(oldFileDesc, newFileDesc);
        } else if(failed) {
            // TODO: What do we want to do here?
            //       This will have the side effect of causing ripples
            //       if a rename/change fails for any reason.
            //saveChange(oldFileDesc.getFile(), false);
            fireRemoveEvent(oldFileDesc);
        }
    }
    
    /** Updates the list with new metadata about the file, possibly removing if it cannot be contained anymore. */
    protected void fileMetaChanged(FileDesc fd) {
        if(contains(fd)) {
            if(isFileDescAllowed(fd)) {
                fireMetaChangeEvent(fd);
            } else {
                remove(fd);
            }
        }
    }

    /**
     * Returns true if this list is allowed to add this FileDesc
     * @param fileDesc - FileDesc to be added
     */
    protected abstract boolean isFileDescAllowed(FileDesc fileDesc);

    void dispose() {
        clear();
        library.removeListener(libraryListener);
        multicaster.removeListeners(this);
    }
    
    /**
     * Returns true if a newly loaded file from the Managed List should be added to this list.
     * If FileDesc is non-null, it's the FileDesc that will be loaded.
     */
    protected abstract boolean isPending(File file, FileDesc fileDesc);
    
    /** Hook for saving changes to data. */
    protected abstract void saveChange(File file, boolean added);
    
    protected int getMaxIndex() {
        rwLock.readLock().lock();
        try {
            return getInternalIndexes().max();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    protected int getMinIndex() {
        rwLock.readLock().lock();
        try {
            return getInternalIndexes().min();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);
        } catch(IOException iox) {
            return file;
        }
    }
    
    private FileDesc throwExecutionExceptionIfNotContains(FileDesc fd) throws ExecutionException {            
        if(contains(fd)) {
            return fd;
        } else {
            throw new ExecutionException(new FileViewChangeFailedException(
                    fd.getFile(), FileViewChangeEvent.Type.FILE_ADD_FAILED, 
                    FileViewChangeFailedException.Reason.CANT_ADD_TO_LIST));
        }
    }
    
    private ListeningFuture<FileDesc> wrapFuture(final ListeningFuture<FileDesc> future) {
        return new ListeningFutureDelegator<FileDesc, FileDesc>(future) {
            @Override
            protected FileDesc convertSource(FileDesc source) throws ExecutionException {
                return throwExecutionExceptionIfNotContains(source);
            }
            
            @Override
            protected FileDesc convertException(ExecutionException ee) throws ExecutionException {
                // We can fail because we attempted to add a File that already existed --
                // if that's why we failed, then we return the file anyway (because it is added.)
                if(ee.getCause() instanceof FileViewChangeFailedException) {
                    FileViewChangeFailedException fe = (FileViewChangeFailedException)ee.getCause();
                    if(fe.getType() == FileViewChangeEvent.Type.FILE_ADD_FAILED) {
                        if(contains(fe.getFile())) {
                            return getFileDesc(fe.getFile());
                        }
                    }
                }
                throw ee;
            }
        };
    }    
    
    private ListeningFuture<FileDesc> futureFor(final FileDesc fd) {
        try {
            return new SimpleFuture<FileDesc>(throwExecutionExceptionIfNotContains(fd));
        } catch(ExecutionException ee) {
            return new SimpleFuture<FileDesc>(ee);
        }
    }
    
    private class LibrarySynchonizer implements EventListener<FileViewChangeEvent> {
        @Override
        public void handleEvent(FileViewChangeEvent event) {
            // Note: We only need to check for pending on adds,
            //       because that's the only kind that doesn't
            //       require it already exists.
            switch(event.getType()) {
            case FILE_ADDED:
                if(isPending(event.getFile(), event.getFileDesc())) {
                    add(event.getFileDesc());
                }
                break;
            case FILE_META_CHANGED:
                fileMetaChanged(event.getFileDesc());
                break;
            case FILE_CHANGED:
                updateFileDescs(event.getOldValue(), event.getFileDesc());
                break;
            case FILE_REMOVED:
                remove(event.getFileDesc());
                break;
            case FILES_CLEARED:
                clear(true);
                break;
            case FILE_CHANGE_FAILED:
            case FILE_ADD_FAILED:
                // This can fail for double-adds, meaning the FD really does exist.
                // If that's why it failed, we pretend this is really an add.
                FileDesc fd = library.getFileDesc(event.getFile());
                if(fd == null) { // File doesn't exist, it was a real failure.
                    if(isPending(event.getFile(), null) && !contains(event.getFile())) {
                        saveChange(event.getFile(), false);
                    }           
                } else if(isPending(event.getFile(), fd)) {
                    add(fd);
                }
                break;
            }
        }
    }
}
