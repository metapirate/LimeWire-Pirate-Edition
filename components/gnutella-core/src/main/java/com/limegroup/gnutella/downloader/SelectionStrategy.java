package com.limegroup.gnutella.downloader;


import java.util.NoSuchElementException;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;


public interface SelectionStrategy {
    /**
     * Encapsulates an algorithm for deciding which block of a file to download next.  
     * 
     * For efficiency reasons attempts will be made to align the start and end of intervals
     * to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @param candidateBytes a representation of the set of 
     *      bytes that are candidates for downloading.  These are the
     *      bytes of the file that a given for download from a given server, minus
     *      the set of bytes that we already have (or have assigned)
     * @param neededBytes a representation of the set of bytes
     *      of the file that have not yet been leased, verified, etc.
     * @param blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignared.  The returned Interval will in no case span a blockSize boundary.
     *      Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Interval that should be assigned next, which does not span a blockSize boundary
     * @throws NoSuchElementException if passed an empty IntervalSet
     */
    public Range pickAssignment(IntervalSet candidateBytes, 
            IntervalSet neededBytes,
            long blockSize) throws java.util.NoSuchElementException;
}
