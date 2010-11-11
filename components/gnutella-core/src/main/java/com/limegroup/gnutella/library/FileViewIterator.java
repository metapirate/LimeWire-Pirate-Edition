package com.limegroup.gnutella.library;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;

/**
 * This class assumes that a lock is held on the FileList being iterated.
 */
class FileViewIterator implements Iterator<FileDesc> {
    
    private final FileView fileList;
    private final IntSetIterator iter;
    private FileDesc preview;
    
    public FileViewIterator(FileView fileList, IntSet intSet) {
        this.fileList = fileList;
        this.iter = intSet.iterator();
    }

    /**
     * Peeks at the next non-null FileDesc in the IntSet, and stores its value.
     *
     * @return true if the list has a next value, false otherwise.
     */
    private boolean getPreview() {
        assert preview == null;
        while (iter.hasNext() && preview == null) {
            preview = fileList.getFileDescForIndex(iter.next());
        }
        return preview != null;
    }

    @Override
    public boolean hasNext() {
       return preview != null || getPreview();
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
