package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.collection.IntSet;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.URN;

abstract class AbstractFileView implements FileView {
    
    private final IntSet indexes = new IntSet();
    protected final LibraryImpl library;
    
    public AbstractFileView(LibraryImpl library) {
        this.library = library;
    }
    
    /**
     * Returns the {@link IntSet} that is internally used to store
     * the index of {@link FileDesc}s contained in this view.
     * 
     * Subclasses can use this to perform operations in bulk.
     * The read lock should be held while using this, or if a subclass
     * modifies it it should consistently hold a write lock
     * while mutating it.
     */
    protected IntSet getInternalIndexes() {
        return indexes;
    }
    
    @Override
    public boolean contains(File file) {
        return getFileDesc(file) != null;
    }

    @Override
    public boolean contains(FileDesc fileDesc) {
        getReadLock().lock();
        try {
            return indexes.contains(fileDesc.getIndex());
        } finally {
            getReadLock().unlock();
        }
    }

    @Override
    public FileDesc getFileDesc(URN urn) {
        List<FileDesc> descs = getFileDescsMatching(urn);
        if(descs.isEmpty()) {
            return null;
        } else {
            return descs.get(0);
        }
    }

    @Override
    public FileDesc getFileDesc(File f) {
        FileDesc fd = library.getFileDesc(f);
        if(fd != null && contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }

    @Override
    public FileDesc getFileDescForIndex(int index) {
        FileDesc fd = library.getFileDescForIndex(index);
        if(fd != null && contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }

    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        List<FileDesc> fds = null;
        List<FileDesc> matching = library.getFileDescsMatching(urn);
        
        // Optimal case.
        if(matching.size() == 1 && contains(matching.get(0))) {
            return matching;
        } else {
            for(FileDesc fd : matching) {
                if(contains(fd)) {
                    if(fds == null) {
                        fds = new ArrayList<FileDesc>(matching.size());
                    }
                    fds.add(fd);
                }
            }
            
            if(fds == null) {
                return Collections.emptyList();
            } else {
                return fds;
            }
        }
    }

    @Override
    public int size() {
        getReadLock().lock();
        try {
            return indexes.size();
        } finally {
            getReadLock().unlock();
        }
    }

    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        // Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(Objects.nonNull(directory, "directory"));
        } catch (IOException e) { // invalid directory ?
            return Collections.emptyList();
        }

        List<FileDesc> list = new ArrayList<FileDesc>();
        getReadLock().lock();
        try {
            for(FileDesc fd : this) {
                if(directory.equals(fd.getFile().getParentFile())) {
                    list.add(fd);
                }
            }
        } finally {
            getReadLock().unlock();
        }
        return list;
    }    

}
