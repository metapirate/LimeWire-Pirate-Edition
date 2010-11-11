package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.listener.ListenerSupport;

import com.limegroup.gnutella.URN;

/** A read-only view of a collection of files. */
public interface FileView extends Iterable<FileDesc>, ListenerSupport<FileViewChangeEvent> {
        
    /** Gets the current name of this collection. */
    String getName();
    
    /** Returns the size of all files within this view, in <b>bytes</b>. */
    long getNumBytes();
    
    /**
     * Returns a list of all the file descriptors in this list that exist in the
     * given directory, in any order.
     * 
     * This method is not recursive; files in any of the directory's children
     * are not returned.
     * 
     * This operation is <b>not</b> efficient, and should not be done often.
     */
    List<FileDesc> getFilesInDirectory(File directory);
   
    /**
     * Returns the <tt>FileDesc</tt> for the specified URN. This only returns
     * one <tt>FileDesc</tt>, even though multiple indices are possible.
     * 
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *         <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
     */
    FileDesc getFileDesc(URN urn);        

    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt> or
     * null if the file is not shared or not a store file.
     */
    FileDesc getFileDesc(File f);

    /**
     * Returns all FileDescs that match this URN.
     */
    List<FileDesc> getFileDescsMatching(URN urn);

    /**
     * Returns the FileDesc at the given index. This returns the FileDesc for
     * which FileDesc.getIndex == index. This is supported as an optimization so
     * that classes can efficiently locate matches.
     */
    FileDesc getFileDescForIndex(int index);

    /** Returns true if this list contains a FileDesc for the given file. */
    boolean contains(File file);

    /**
     * Return true if this list contains this FileDesc, false otherwise.
     */
    boolean contains(FileDesc fileDesc);

    /**
     * Returns an iterator over all FileDescs. The returned iterator is *NOT*
     * thread safe. You must lock on FileList while acquiring and using it.
     */
    Iterator<FileDesc> iterator();

    /**
     * Returns an iterable that is thread-safe and can be used over a period of
     * time (iterating through it piecemeal, with time lapses). The returned
     * iterable is much slower and more inefficient than the default iterator,
     * though, so only use it if absolutely necessary.
     */
    Iterable<FileDesc> pausableIterable();

    /**
     * Returns the number of files in this list.
     */
    int size();

    /** Returns a lock to use when iterating over this FileList. */
    Lock getReadLock();
    
}
