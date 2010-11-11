package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureDelegator;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

import com.google.common.base.Predicate;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileViewChangeEvent;

abstract class LocalFileListImpl implements LocalFileList {
    
    private static final Log LOG = LogFactory.getLog(LocalFileListImpl.class);
    
    protected final EventList<LocalFileItem> baseList;
    protected final TransformedList<LocalFileItem, LocalFileItem> threadSafeList;
    protected final TransformedList<LocalFileItem, LocalFileItem> readOnlyList;
    protected volatile TransformedList<LocalFileItem, LocalFileItem> swingEventList;    

    private final CoreLocalFileItemFactory fileItemFactory;
    
    LocalFileListImpl(EventList<LocalFileItem> eventList, CoreLocalFileItemFactory fileItemFactory) {
        this.baseList = eventList;
        this.threadSafeList = GlazedListsFactory.threadSafeList(eventList);
        this.readOnlyList = GlazedListsFactory.readOnlyList(threadSafeList);
        this.fileItemFactory = fileItemFactory;

    }
    
    /** Returns the FileCollection this should mutate. */
    protected abstract FileCollection getCoreCollection();
    
    @Override
    public ListeningFuture<LocalFileItem> addFile(File file) {
        return new Wrapper((getCoreCollection().add(file)));
    }

    @Override
    public void removeFile(File file) {
        getCoreCollection().remove(file);
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<LocalFileItem>>> addFolder(File folder, FileFilter fileFilter) {
        return new ListWrapper((getCoreCollection().addFolder(folder, fileFilter)));
    }

    @Override
    public boolean contains(File file) {
        return getCoreCollection().contains(file);
    }
    
    @Override
    public boolean contains(URN urn) {
        if(urn instanceof com.limegroup.gnutella.URN) {
            return containsCoreUrn((com.limegroup.gnutella.URN)urn);
        } else {
            return false;
        }
    }
    
    protected boolean containsCoreUrn(com.limegroup.gnutella.URN urn) {
        return !getCoreCollection().getFileDescsMatching(urn).isEmpty();
    }

    @Override
    public EventList<LocalFileItem> getModel() {
        return readOnlyList;
    }
    
    @Override
    public EventList<LocalFileItem> getSwingModel() {
        assert EventQueue.isDispatchThread();
        if(swingEventList == null) {
            swingEventList =  GlazedListsFactory.swingThreadProxyEventList(readOnlyList);
        }
        return swingEventList;
    }
    
    void dispose() {
        if(swingEventList != null) {
            swingEventList.dispose();
        }
        threadSafeList.dispose();
        readOnlyList.dispose();
    }
    
    @Override
    public int size() {
        return threadSafeList.size();
    }
    
    /**
     * Adds <code>fd</code> as {@link LocalFileItem} to this list. 
     */
    protected void addFileDesc(FileDesc fd) {
        threadSafeList.add(getOrCreateLocalFileItem(fd));
    }
    
    private LocalFileItem getOrCreateLocalFileItem(FileDesc fileDesc) {
        LocalFileItem item;
        Object object = fileDesc.getClientProperty(FILE_ITEM_PROPERTY);
        if(object != null) {
            item = (LocalFileItem)object;
        } else {
            item = fileItemFactory.createCoreLocalFileItem(fileDesc);
            fileDesc.putClientProperty(FILE_ITEM_PROPERTY, item);
        }
        return item;
    }
    
    /**
     * Adds all <code>fileDescs</code> as {@link LocalFileItem} to this list.
     * <p>
     * Caller is responsible for locking the iterable.
     */
    protected void addAllFileDescs(Iterable<FileDesc> fileDescs) {
        List<LocalFileItem> fileItems = new ArrayList<LocalFileItem>();
        for (FileDesc fileDesc : fileDescs) {
            fileItems.add(getOrCreateLocalFileItem(fileDesc));
        }
        threadSafeList.addAll(fileItems);
    }
    
    /** Notification that meta information has changed in the filedesc. */
    protected void updateFileDesc(FileDesc fd) {
        LocalFileItem item = (LocalFileItem)fd.getClientProperty(FILE_ITEM_PROPERTY);
        if(item != null) {
            threadSafeList.getReadWriteLock().writeLock().lock();
            try {
                int idx = threadSafeList.indexOf(item);
                if(idx > 0) {
                    threadSafeList.set(idx, item);
                } else {
                    LOG.warnf("Attempted to update FD w/ LocalFileItem that is not in list anymore. Item {0}", item);
                }
            } finally {
                threadSafeList.getReadWriteLock().writeLock().unlock();
            }
        } else {
            LOG.warnf("Attempted to update FD without LocalFileItem, FD {0}", fd);
        }
    }
    
    protected void changeFileDesc(FileDesc old, FileDesc now) {
        removeFileDesc(old);
        addFileDesc(now);
    }
    
    protected void removeFileDesc(FileDesc fd) {
        LocalFileItem item = (LocalFileItem)fd.getClientProperty(FILE_ITEM_PROPERTY);
        threadSafeList.remove(item);
    }
    
    protected void clearFileDescs() {
        threadSafeList.clear();
    }
    
    /** Constructs a new EventListener for list change events. */
    protected EventListener<FileViewChangeEvent> newEventListener() {
        return new EventListener<FileViewChangeEvent>() {
            @Override
            public void handleEvent(FileViewChangeEvent event) {              
                switch(event.getType()) {
                case FILE_META_CHANGED:
                    updateFileDesc(event.getFileDesc());
                    break;
                case FILE_ADDED:
                    addFileDesc(event.getFileDesc());
                    break;
                case FILE_CHANGED:
                    changeFileDesc(event.getOldValue(), event.getFileDesc());
                    break;
                case FILE_REMOVED:
                    removeFileDesc(event.getFileDesc());
                    break;
                case FILES_CLEARED:
                    clearFileDescs();
                    break;     
                }
            }
        };
    }
    
    private static class ListWrapper extends ListeningFutureDelegator<List<ListeningFuture<FileDesc>>, List<ListeningFuture<LocalFileItem>>> {
        public ListWrapper(ListeningFuture<List<ListeningFuture<FileDesc>>> delegate) {
            super(delegate);
        }
        
        @Override
        protected List<ListeningFuture<LocalFileItem>> convertSource(List<ListeningFuture<FileDesc>> source) {
            List<ListeningFuture<LocalFileItem>> replaced = new ArrayList<ListeningFuture<LocalFileItem>>(source.size());
            for(ListeningFuture<FileDesc> future : source) {
                replaced.add(new Wrapper(future));
            }
            return replaced;
        }
        
        @Override
        protected List<ListeningFuture<LocalFileItem>> convertException(ExecutionException ee)
                throws ExecutionException {
            throw ee;
        }
    }
    
    private static class Wrapper extends ListeningFutureDelegator<FileDesc, LocalFileItem> {
        public Wrapper(ListeningFuture<FileDesc> delegate) {
            super(delegate);
        }
        
        @Override
        protected LocalFileItem convertSource(FileDesc source) {
            return (LocalFileItem)source.getClientProperty(FILE_ITEM_PROPERTY);
        }
        
        @Override
        protected LocalFileItem convertException(ExecutionException ee) throws ExecutionException {
            throw ee;
        }
    }
    
    @Override
    public LocalFileItem getFileItem(File file) {
      FileDesc fileDesc = getCoreCollection().getFileDesc(file);
      if(fileDesc != null) {
          return (LocalFileItem)fileDesc.getClientProperty(FILE_ITEM_PROPERTY);
      }
      return null;
    }

    @Override
    public LocalFileItem getFileItem(URN urn) {
        if (urn instanceof com.limegroup.gnutella.URN) {
            FileDesc fd = getCoreCollection().getFileDesc((com.limegroup.gnutella.URN)urn);

            if (fd != null) {
                return (LocalFileItem)fd.getClientProperty(FILE_ITEM_PROPERTY);
            }
        }
        return null;
    }
    
    @Override
    public boolean isFileAllowed(File file) {
       return getCoreCollection().isFileAllowed(file);
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return getCoreCollection().isDirectoryAllowed(folder);
    }
    
    @Override
    public void removeFiles(Predicate<LocalFileItem> filter) {
        List<LocalFileItem> files = new ArrayList<LocalFileItem>();
        
        getModel().getReadWriteLock().readLock().lock();
        try {
            for (LocalFileItem localFileItem : getModel()) {
                if (filter.apply(localFileItem)) {
                    files.add(localFileItem);
                }
            }
        } finally {
            getModel().getReadWriteLock().readLock().unlock();
        }
        
        for (LocalFileItem localFileItem : files) {
            removeFile(localFileItem.getFile());
        }
    }
    

    @Override
    public void clear() {
       getCoreCollection().clear();
    }
}