package com.limegroup.gnutella.downloader;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.MultiIterable;
import org.limewire.collection.Range;
import org.limewire.io.DiskException;
import org.limewire.util.FileUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.tigertree.HashTree;


/**
 * A control point for all access to the file being downloaded to, also does 
 * on-the-fly verification.
 * <p>
 * Every region of the file can be in one of five states, and can move from one
 * state to another only in the following order:
 * <pre>
 *   1. available for download 
 *   2. currently being downloaded 
 *   3. waiting to be written.
 *   4. written (and immediately into, if possible..)
 *   5. verified, or if it doesn't verify back to
 *   1. available for download   
 * </pre>
 * In order to maintain these constraints, the only possible operations are:<pre>
 *   Lease a block - find an area which is available for download and claim it
 *   Write a block - report that the specified block has been read from the network.
 *   Release a block - report that the specified block will not be downloaded.</pre>
 */
public class VerifyingFile {
    
    static final Log LOG = LogFactory.getLog(VerifyingFile.class);
    
    /**
     * If the number of corrupted data gets over this, assume the file will not be recovered
     */
    static final float MAX_CORRUPTION = 0.9f;
    
    /**
     * If the number of corrupted data for a specific tree exceeds this ratio, 
     * invalidate the tree and look for a new one
     */
    static final float MAX_CORRUPTION_FOR_TREE = 0.1f;
    
    /** The default chunk size - if we don't have a tree we request chunks this big.
     * <p>
     *  This is a power of two in order to minimize the number of small partial chunk
     *  downloads that will be required after we learn the chunk size from the TigerTree,
     *  since the chunk size will always be a power of two.
     */
    static final int DEFAULT_CHUNK_SIZE = 131072; //128 KB = 128 * 1024 B = 131072 bytes
    
    /** How much to verify at a time. */
    private static final int VERIFYABLE_CHUNK = 64 * 1024; // 64k
    
    /**
     * The file we're writing to / reading from.
     */
    private volatile RandomAccessFile fos;
    
    /**
     * Whether this file is open for writing.
     */
    private volatile boolean isOpen;

    /**
     * The eventual completed size of the file we're writing.
     */
    private final long completedSize;

    /**
     * How much data did we lose due to corruption.
     */
    private long lostSize;
    
    /**
     * How much data did we lose using the current tree.
     */
    private long lostSizeForTree;
    
    /**
     * The VerifyingFile uses an IntervalSet to keep track of the blocks written
     * to disk and find out which blocks to check before writing to disk.
     */
    private final IntervalSet verifiedBlocks;
    
    /**
     * Ranges that are currently being written by the ManagedDownloader. 
     * <p>
     * Replaces the IntervalSet of needed ranges previously stored in the 
     * ManagedDownloader but which could get out of sync with the verifiedBlocks
     * IntervalSet and is therefore replaced by a more failsafe implementation.
     */
    private IntervalSet leasedBlocks;
    
    /**
     * Ranges that are currently written to disk, but do not form complete chunks
     * so cannot be verified by the HashTree.
     */
    private IntervalSet partialBlocks;
    
    /**
     * Ranges that are discarded (but verification was attempted).
     */
    private IntervalSet savedCorruptBlocks;
    
    /**
     * Ranges which are pending writing & verification.
     */
    private IntervalSet pendingBlocks;
    
    /**
     * Decides which blocks to start downloading next.
     */
    private SelectionStrategy blockChooser = null;
    
    /**
     * The hashtree we use to verify chunks, if any.
     */
    private HashTree hashTree;
    
    /**
     * The expected TigerTree root (null if we'll accept any).
     */
    private String expectedHashRoot;
    
    /**
     * Whether someone is currently requesting the hash tree.
     */
    private boolean hashTreeRequested;
    
    /**
     * Whether we are actually verifying chunks.
     */
    private boolean discardBad = true;
    
    /**
     * The IOException, if any, we got while writing.
     */
    private IOException storedException;
    
    /**
     * The size of the file on disk if we're going to scan for completed
     * blocks.  Otherwise -1.
     */
    private long existingFileSize = -1;
    
    /**
     * Additional counter to keep track of scheduled chunks in each single file.
     * Needed to prevent premature closing of underlying RandomAccessFile.
     */
    private int chunksScheduledPerFile = 0;
    
    /**
     * Holds the iterable for all blocks, is lazily instantiated when
     * needed for the first time.
     */
    private MultiIterable<Range> allBlocksIterable = null;
    
    /** The controller for doing disk reads/writes. */
    private final Provider<DiskController> diskController;
        
    /**
     * Constructs a new VerifyingFile for the specified size.
     * If checkOverlap is true, will scan for overlap corruption.
     */
    VerifyingFile(long completedSize, Provider<DiskController> diskController) {
        this.completedSize = completedSize;
        verifiedBlocks = new IntervalSet();
        leasedBlocks = new IntervalSet();
        pendingBlocks = new IntervalSet();
        partialBlocks = new IntervalSet();
        savedCorruptBlocks = new IntervalSet();
        this.diskController = diskController;
    }
    
    /**
     * Opens this VerifyingFile for writing.
     * MUST be called before anything else.
     * <p>
     * If there is no completion size, this fails.
     */
    public void open(File file) throws IOException {
        if(completedSize == -1)
            throw new IllegalStateException("cannot open for unknown size.");
        
        // Ensure that the directory this file is in exists & is writeable.
        File parentFile = file.getParentFile();
        if( parentFile != null ) {
            parentFile.mkdirs();
            if(!parentFile.exists())
                throw new IOException("permission denied");
            FileUtils.setWriteable(parentFile);
        }
        FileUtils.setWriteable(file);
        this.fos =  new RandomAccessFile(file,"rw");
        SelectionStrategy myStrategy = SelectionStrategyFactory.getStrategyFor(
                FileUtils.getFileExtension(file), completedSize);
        
        synchronized(this) {
            storedException = null;
            
            // Figure out which SelectionStrategy to use
            blockChooser = myStrategy;
            isOpen = true;
        }
    }

    /**
     * Used to add blocks directly. Blocks added this way are marked
     * partial.
     */
    public synchronized void addInterval(Range interval) {
        //delegates to underlying IntervalSet
        partialBlocks.add(interval);
    }

    public void registerWriteCallback(WriteRequest request, WriteCallback callback) {
        request.startScheduling();
    
        if (writeBlockImpl(request)) {
            callback.writeScheduled();
        } else {
            diskController.get().addDelayedWrite(new VerifyingFileDelayedWrite(request, callback, this));
        }
    }

    /**
     * Writes bytes to the underlying file.
     */
    public boolean writeBlock(WriteRequest request) {
        if(!validateState(request))
            return true;
        
        request.startProcessing();
        updateState(request.in);
        boolean canWrite = diskController.get().canWriteNow();
        
        if(canWrite)
            return writeBlockImpl(request);
         else  // do not try to write if something else is waiting.
            return false;
    }

    /**
     * Writes bytes to the underlying file.
     * 
     * @return true if this scheduled a write or wasn't open, false if it couldn't.
     */
    private boolean writeBlockImpl(WriteRequest request) {
        if (LOG.isTraceEnabled())
            LOG.trace("trying to write block at offset " + request.currPos + " with size " + request.length);
        
        if (!validateState(request))
        	return true;
        
        byte[] temp = diskController.get().getWriteChunk();
        if(temp == null)
            return false;
        
        request.setDone();
        
        assert temp.length >= request.length :
                "bad length: " + request.length + ", needed <= " + temp.length;
        System.arraycopy(request.buf, request.start, temp, 0, request.length);
        
        synchronized (this) {
            chunksScheduledPerFile++;
        }
        
        diskController.get().addDiskJob(new ChunkHandler(temp, request.in));
        return true;
    }
    
    private synchronized void updateState(Range intvl) {
        // / some stuff to help debugging ///
        assert leasedBlocks.contains(intvl) : "trying to write an interval " + intvl
                + " that wasn't leased.\n" + dumpState();

        assert !(partialBlocks.contains(intvl) || savedCorruptBlocks.contains(intvl) || pendingBlocks.contains(intvl)) :
            "trying to write an interval "+intvl+ " that was already written"+dumpState();
            
        leasedBlocks.delete(intvl);
        
        // add only the ranges that aren't already verified into pending.
        // this is necessary because full-scanning may have added unforeseen
        // blocks into verified.
        if(verifiedBlocks.containsAny(intvl)) {
            // technically the code in this if block would work for all cases,
            // but it's kind of inefficient to do lots of work all the time,
            // when the if is only necessary after a full-scan.
            IntervalSet remaining = new IntervalSet();
            remaining.add(intvl);
            remaining.delete(verifiedBlocks);
            pendingBlocks.add(remaining);
        } else {
            pendingBlocks.add(intvl);
        }
    }

    /**
     * @return false if this request should return immediately
     */
    private boolean validateState(WriteRequest request) {
        if (request.length == 0) // nothing to write? return
            return false;

        if (fos == null)
            throw new IllegalStateException("no fos!");

        if (!isOpen())
            return false;

        return true;
    }
    
    
    /**
     * Set whether or not we're going to do a one-time full scan
     * on this file for verified blocks once we find a
     * hash tree.
     */
    public void setScanForExistingBlocks(boolean scan, long length) 
    throws IOException {
        if(scan && length != 0) {
        	if (length > completedSize)
        		throw new IOException("invalid completed size or length");
            existingFileSize = length;
        } else {
            existingFileSize = -1;
        }
    }

    public synchronized String dumpState() {
        return "verified:"+verifiedBlocks+"\npartial:"+partialBlocks+
            "\ndiscarded:"+savedCorruptBlocks+
        	"\npending:"+pendingBlocks+"\nleased:"+leasedBlocks;
    }
    
    /**
     * Returns a block of data that needs to be written.
     * <p>
     * This method will not break up contiguous chunks into smaller chunks.
     */
    public Range leaseWhite() throws NoSuchElementException {
        return leaseWhiteHelper(null, completedSize);
    }
    
    /**
     * Returns a block of data that needs to be written.
     * The returned block will NEVER be larger than chunkSize.
     */
    public Range leaseWhite(long chunkSize) 
      throws NoSuchElementException {
        return leaseWhiteHelper(null, chunkSize);
    }
    
    /**
     * Returns a block of data that needs to be written
     * and is within the specified set of ranges.
     * The parameter IntervalSet is modified
     */
    public Range leaseWhite(IntervalSet ranges)
      throws NoSuchElementException {
        return leaseWhiteHelper(ranges, DEFAULT_CHUNK_SIZE);
    }
    
    /**
     * Returns a block of data that needs to be written
     * and is within the specified set of ranges.
     * The returned block will NEVER be larger than chunkSize.
     */
    public Range leaseWhite(IntervalSet ranges, long chunkSize)
      throws NoSuchElementException {
        return leaseWhiteHelper(ranges, chunkSize);
    }

    /**
     * Removes the specified internal from the set of leased intervals.
     */
    public synchronized void releaseBlock(Range in) {
        assert leasedBlocks.contains(in) :
            "trying to release an interval " + in + " that wasn't leased " + dumpState();
        
        if(LOG.isInfoEnabled())
            LOG.info("Releasing interval: " + in+" state "+dumpState());
        leasedBlocks.delete(in);
    }
    
    /**
     * Returns all verified blocks with an Iterator.
     */
    public synchronized Iterable<Range> getVerifiedBlocks() {
        return verifiedBlocks;
    }
    
    /**
     * @return the verified IntervalSet.
     */
    public synchronized IntervalSet getVerifiedIntervalSet() {
        return verifiedBlocks;
    }
    
    /**
     * @return the partial IntervalSet.
     */
    public synchronized IntervalSet getPartialIntervalSet() {
        return partialBlocks;
    }
    
    /**
     * @return byte-packed representation of the verified blocks.
     */
    public synchronized IntervalSet.ByteIntervals toBytes() {
    	return verifiedBlocks.toBytes();
    }
    
    @Override
    public String toString() {
        return dumpState();
    }

    /**
     * @return List of Intervals that should be serialized.  Excludes pending intervals.
     */
    public synchronized List<Range> getSerializableBlocks() {
        IntervalSet ret = new IntervalSet();
        for(Range next : new MultiIterable<Range>(verifiedBlocks, partialBlocks, savedCorruptBlocks))
            ret.add(next);
        return ret.getAllIntervalsAsList();
        
    }
    /**
     * While iterating over the result a lock to the verifying file should
     * be held to ensure the interval lists are not modified elsewhere.
     * @return all downloaded blocks as list
     */
    public synchronized Iterable<Range> getBlocks() {
        if (allBlocksIterable == null) {
        	allBlocksIterable = new MultiIterable<Range>(verifiedBlocks, partialBlocks, savedCorruptBlocks, pendingBlocks); 
        }
        return allBlocksIterable;
    }
    
    /**
     * @return the offset of the first contiguous downloaded
     * region of the file 
     */
    public synchronized long getOffsetForPreview() {
        // if we have any savedCorrupt intervals, we need to lump everything
        // together to get the longest continuous interval
        if (!savedCorruptBlocks.isEmpty()) {
            IntervalSet lump = new IntervalSet();
            lump.add(savedCorruptBlocks);
            lump.add(verifiedBlocks);
            lump.add(partialBlocks);
            if (lump.getFirst().getLow() != 0)
                return 0;
            return lump.getFirst().getHigh();
        }
        
        /*
         * we know that the intervals are contiguous, mutually exclusive
         * and sorted.  Also, we know that a partial interval must be smaller
         * than a full chunk.  So all we need to look at are the 
         * first verified & first partial intervals.
         */
        IntervalSet firsts = new IntervalSet();
        if (!verifiedBlocks.isEmpty())
            firsts.add(verifiedBlocks.getFirst());
        if (!partialBlocks.isEmpty())
            firsts.add(partialBlocks.getFirst());
        if (firsts.isEmpty() || firsts.getFirst().getLow() != 0)
            return 0;
        return firsts.getFirst().getHigh();
    }
    
    /**
     * Returns all verified blocks as a List.
     */ 
    public synchronized List<Range> getVerifiedBlocksAsList() {
        return verifiedBlocks.getAllIntervalsAsList();
    }

    /**
     * Returns the total number of bytes written to disk.
     */
    public synchronized long getBlockSize() {
        return verifiedBlocks.getSize() +
        	partialBlocks.getSize() +
        	savedCorruptBlocks.getSize() +
        	pendingBlocks.getSize();
    }
    
    public synchronized long getPendingSize() {
        return pendingBlocks.getSize();
    }
    
    /**
     * Returns the total number of verified bytes written to disk.
     */
    public synchronized long getVerifiedBlockSize() {
        return verifiedBlocks.getSize();
    }
  
    /**
     * @return how much data was lost due to corruption
     */
    public synchronized long getAmountLost() {
        return lostSize;
    }

    /**
     * Determines if all blocks have been written to disk and verified.
     */
    public synchronized boolean isComplete() {
        if (hashTree != null)
            return verifiedBlocks.getSize() + savedCorruptBlocks.getSize() == completedSize;
        else {
            return verifiedBlocks.getSize() + savedCorruptBlocks.getSize() + 
            partialBlocks.getSize()== completedSize;
        }
    }
    
    /** Returns all missing pieces. */
    public synchronized String listMissingPieces() {
        IntervalSet all = new IntervalSet();
        all.add(Range.createRange(0, completedSize-1));
        all.delete(verifiedBlocks);
        all.delete(savedCorruptBlocks);
        if(hashTree == null)
            all.delete(partialBlocks);
        return all.toString() + ", pending: " + pendingBlocks.toString() + ", has tree? " + (hashTree != null) + ", verified: " + verifiedBlocks + ", savedCorrupt: " + savedCorruptBlocks + ", partial: " + partialBlocks;
    }
    
    /**
     * If the last remaining chunks of the file are currently pending writing & verification,
     * wait until it finishes.
     */
    public synchronized void waitForPendingIfNeeded() throws InterruptedException, DiskException {
        if(storedException != null)
            throw new DiskException(storedException);
        
        while (!isComplete() && getBlockSize() == completedSize) {
            if(storedException != null)
                throw new DiskException(storedException);
            if (LOG.isInfoEnabled())
                LOG.info("waiting for a pending chunk to verify or write..");
            wait();
        }
    }

    /**
     * Waits until all pending write requests have been completed. 
     */
    public synchronized void waitForPending(int timeout) throws InterruptedException, DiskException {
        if(storedException != null)
            throw new DiskException(storedException);

        synchronized (this) {
            while (chunksScheduledPerFile > 0) {
                this.wait(timeout);
            }
        }
    }

    /**
     * @return whether we think we will not be able to complete this file
     */
    public synchronized boolean isHopeless() {
        return lostSize >= MAX_CORRUPTION * completedSize;
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    /**
     * Determines if there are any blocks that are not assigned
     * or written.
     */
    public synchronized long hasFreeBlocksToAssign() {
        return  completedSize - (verifiedBlocks.getSize() + 
                leasedBlocks.getSize() +
                partialBlocks.getSize() +
                savedCorruptBlocks.getSize() +
                pendingBlocks.getSize()); 
    }
    
    /**
     * Closes the file output stream.
     */
    public void close() {
        isOpen = false;
        if(fos==null)
            return;
        try { 
            synchronized (this) {
                while (chunksScheduledPerFile > 0) {
                    try {
                        wait();
                    } catch (InterruptedException ignore) { }
                }
            }
            fos.close();
        } catch (IOException ignore) {}
    }
    
    /////////////////////////private helpers//////////////////////////////
    /**
     * Determines which interval should be assigned next, leases that interval,
     * and returns that interval.
     * 
     * @param availableRanges if ranges is non-null, the return value will be a chosen 
     *      from within availableRanges
     * @param chunkSize if greater than zero, the return value will end one byte before 
     *      a chunkSize boundary and will be at most chunkSize bytes large.
     * @return the leased interval
     */
    private synchronized Range leaseWhiteHelper(IntervalSet availableBytes, long chunkSize) throws NoSuchElementException {
        if (LOG.isDebugEnabled())
            LOG.debug("leasing white, state:\n"+dumpState());
      
        // If ranges is null, make ranges represent the entire file
        if (availableBytes == null)
            availableBytes = IntervalSet.createSingletonSet(0, completedSize-1);
        
        // Figure out which blocks we still need to assign
        IntervalSet neededBytes = IntervalSet.createSingletonSet(0, completedSize-1);
        neededBytes.delete(verifiedBlocks);
        neededBytes.delete(leasedBlocks);
        neededBytes.delete(partialBlocks);
        neededBytes.delete(savedCorruptBlocks);
        neededBytes.delete(pendingBlocks);
        
        if (LOG.isDebugEnabled())
            LOG.debug("needed bytes: "+neededBytes);
        
        // Calculate the intersection of neededBytes and availableBytes
        availableBytes.delete(neededBytes.invert(completedSize));
        
        Range ret = blockChooser.pickAssignment(availableBytes, neededBytes,
                chunkSize);
        
        leaseBlock(ret);
        
        if (LOG.isDebugEnabled())
            LOG.debug("leasing white interval "+ret+"\nof available intervals "+
                    neededBytes);
        
        return ret;
    }

    /**
     * Leases the specified interval.
     */
    private synchronized void leaseBlock(Range in) {
        //if(LOG.isDebugEnabled())
            //LOG.debug("Obtaining interval: " + in);
        leasedBlocks.add(in);
    }
    
    /**
     * Sets the expected hash tree root.  If non-null, we'll only accept
     * hash trees whose root hash matches this.
     */
    public synchronized void setExpectedHashTreeRoot(String root) {
        expectedHashRoot = root;
    }
    
    public synchronized HashTree getHashTree() {
        return hashTree;
    }
    
    /**
     * sets the HashTree the current download will use.  That affects whether
     * we do overlap checking.
     * @return true if the new tree was accepted.
     */
    public synchronized boolean setHashTree(HashTree tree) {
        // doesn't match our expected tree, bail.
        if (expectedHashRoot != null && tree != null && !tree.getRootHash().equalsIgnoreCase(expectedHashRoot))
            return false;

        // if the tree is of incorrect size, ignore it
        if (tree != null && tree.getFileSize() != completedSize)
            return false;

        HashTree previous = hashTree;
        
        // if the roots are different
        if (previous != null && tree != null &&
                !previous.getRootHash().equals(tree.getRootHash())){
            
            // and we have verified at least two default chunks, don't change
            if (verifiedBlocks.getSize() > 2 * DEFAULT_CHUNK_SIZE)
                return false;
            
            // else trigger verification
            if (verifiedBlocks.getSize() > 0) {
                partialBlocks.add(verifiedBlocks);
                verifiedBlocks.clear();
                diskController.get().addDiskJobWithoutChunk(new EmptyVerifier(existingFileSize));
            }
        }
        
        hashTree = tree;
        
        // if we did not have a tree previously
        // and we do have a hash tree now
        // and either we want to scan the whole file once
        // or we don't have pending blocks but do have partial blocks,
        // trigger verification.
        if (previous == null && tree != null && (existingFileSize != -1 ||
                (pendingBlocks.getSize() == 0 && partialBlocks.getSize() > 0))
           ) {
            diskController.get().addDiskJobWithoutChunk(new EmptyVerifier(existingFileSize));
            existingFileSize = -1;
        }
        return true;
    }
    
    /**
     * Flags that someone is currently requesting the tree.
     */
    public synchronized void setHashTreeRequested(boolean yes) {
        hashTreeRequested = yes;
    }
    
    public synchronized boolean isHashTreeRequested() {
        return hashTreeRequested;
    }
    
    public synchronized void setDiscardUnverified(boolean yes) {
        discardBad = yes;
    }
    
    public synchronized int getChunkSize() {
        return hashTree == null ? DEFAULT_CHUNK_SIZE : hashTree.getNodeSize();
    }
    
    /**
     * Stub for calling verifyChunks(-1).
     */
    private void verifyChunks() {
        verifyChunks(-1);
    }
    
    /**
     * Schedules those chunks that can be verified against the hash tree for
     * verification.
     */
    private void verifyChunks(long existingFileSize) {
        boolean fullScan = existingFileSize != -1;
        HashTree tree = getHashTree(); // capture the tree.
        // if we have a tree, see if there is a completed chunk in the partial list
        if(tree != null) {
            for(Range i : findVerifyableBlocks(existingFileSize, tree.getNodeSize())) {
                byte[] tmp = diskController.get().getPowerOf2Chunk(Math.min(VERIFYABLE_CHUNK,tree.getNodeSize()));
                boolean good = !tree.isCorrupt(i, fos, tmp);
                synchronized (this) {
                    partialBlocks.delete(i);
                    if (good)
                        verifiedBlocks.add(i);
                    else {
                        if (!fullScan) {
                            if (!discardBad) {
                                savedCorruptBlocks.add(i);
                            }
                            long intervalSize = i.getLength();
                            lostSize += intervalSize;
                            lostSizeForTree += intervalSize;
                        }
                    } 
                }
            }
            checkTree();
        }
    }
    
    synchronized IntervalSet getDownloadedBlocks() {
        IntervalSet full = new IntervalSet();
        full.add(verifiedBlocks);
        full.add(partialBlocks);
        full.add(pendingBlocks);
        if(!discardBad) {
            full.add(savedCorruptBlocks);
        }
        return full;
    }
    
    synchronized IntervalSet getLeasedBlocks() {
        return leasedBlocks.clone();
    }
        
    /**
     * iterates through the pending blocks and checks if the recent write has created
     * some (verifiable) full chunks.  Its not possible to verify more than two chunks
     * per method call unless the downloader is being deserialized from disk.
     */
    private synchronized List<Range> findVerifyableBlocks(long existingFileSize, int chunkSize) {
        if (LOG.isTraceEnabled())
            LOG.trace("trying to find verifyable blocks out of "+partialBlocks);
        
        boolean fullScan = existingFileSize != -1;
        List<Range> verifyable = new ArrayList<Range>(2);
        List<Range> partial;
        
        if(fullScan) {
            IntervalSet temp = partialBlocks.clone();
            temp.add(Range.createRange(0, existingFileSize));
            partial = temp.getAllIntervalsAsList();
        } else {
            partial = partialBlocks.getAllIntervalsAsList();
        }

        for (Range current : partial) {
            // find the beginning of the first chunk offset
            long lowChunkOffset = current.getLow() - current.getLow() % chunkSize;
            if (current.getLow() % chunkSize != 0)
                lowChunkOffset += chunkSize;
            while (current.getHigh() >= lowChunkOffset + chunkSize - 1) {
                Range complete = Range.createRange(lowChunkOffset, lowChunkOffset + chunkSize - 1);
                verifyable.add(complete);
                lowChunkOffset += chunkSize;
            }
        }
        
        // special case for the last chunk
        if (!partial.isEmpty()) {
            long lastChunkOffset = completedSize - (completedSize % chunkSize);
            if (lastChunkOffset == completedSize)
                lastChunkOffset-=chunkSize;
            Range last = partial.get(partial.size() - 1);
            if (last.getHigh() == completedSize-1 && last.getLow() <= lastChunkOffset ) {
                if(LOG.isDebugEnabled())
                     LOG.debug("adding the last chunk for verification");
                
                verifyable.add(Range.createRange(lastChunkOffset, last.getHigh()));
            }
        }
        
        return verifyable;
    }
    
    long getCompletedSize() {
        return completedSize;
    }
    
    private synchronized void checkTree() {
        if (lostSizeForTree > MAX_CORRUPTION_FOR_TREE * completedSize) {
            hashTree = null;
            lostSizeForTree = 0;
        }
    }
    
    /**
     * Runnable that writes chunks to disk & verifies partial blocks.
     */
    private class ChunkHandler extends ChunkDiskJob {
        
        /** The interval that we are about to write */
        private final Range intvl;
        
        /** Whether or not running the job freed a pending block. */
        private boolean freedPending = false;
        
        public ChunkHandler(byte[] buf, Range intvl) {
            super(buf);
            this.intvl = intvl;
            long length = intvl.getHigh() - intvl.getLow() + 1;
            assert length <= buf.length : 
                "invalid length "+length+ " vs buf "+buf.length;
        }
        
        @Override
        public void runChunkJob(byte[] buf) {
            try {
                if (LOG.isTraceEnabled())
                    LOG.trace("Writing intvl: " + intvl);

                synchronized (fos) {
                    fos.seek(intvl.getLow());
                    fos.write(buf, 0, (int) (intvl.getHigh() - intvl.getLow() + 1));
                }

                synchronized (VerifyingFile.this) {
                    pendingBlocks.delete(intvl);
                    partialBlocks.add(intvl);
                    freedPending = true;
                }

                verifyChunks();
            } catch (IOException diskIO) {
                synchronized(VerifyingFile.this) {
                    pendingBlocks.delete(intvl);
                    storedException = diskIO;
                }
            }
        }
        
        @Override
        public void finish() {
            synchronized(VerifyingFile.this) {
                try {
                    if (!freedPending)
                        pendingBlocks.delete(intvl);
                
                } finally {
                    --chunksScheduledPerFile;
                    VerifyingFile.this.notifyAll();
                }
            }
        }
    }

    /** A simple Runnable that schedules a verification of the file. */
    private class EmptyVerifier implements Runnable {
        private final long existingFileSize;

        EmptyVerifier(long existingFileSize) {
            this.existingFileSize = existingFileSize;
        }

        public void run() {
            verifyChunks(existingFileSize);
            synchronized (VerifyingFile.this) {
                VerifyingFile.this.notify();
            }
        }
    }
    

    static interface WriteCallback {
        public void writeScheduled();
    }
    
    private static class VerifyingFileDelayedWrite implements DelayedWrite {
        
    	private final WriteRequest request;
        private final WriteCallback callback;
        private final VerifyingFile vf;
        
        VerifyingFileDelayedWrite(WriteRequest request, WriteCallback callback, VerifyingFile vf) {
            this.request = request;
            this.callback = callback;
            this.vf = vf;
        }
        
        public boolean write() {
            if(vf.writeBlockImpl(request)) {
                callback.writeScheduled();
                return true;
            } else {
                return false;
            }
        }
    }

    public static class WriteRequest {
        public final long currPos;

        public final int start;

        public final int length;

        public final byte[] buf;

        public final Range in;

        private boolean processed, done, scheduled;

        WriteRequest(long currPos, int start, int length, byte[] buf) {
            this.currPos = currPos;
            this.start = start;
            this.length = length;
            this.buf = buf;
            in = Range.createRange(currPos, currPos + length - 1);
        }

        private synchronized void startProcessing() {
            if (isInvalidForWriting())
                throw new IllegalStateException("invalid request state");
            processed = true;
        }

        private synchronized void startScheduling() {
            if (isInvalidForCallback())
                throw new IllegalStateException("invalid request state");
            scheduled = true;
        }

        private synchronized void setDone() {
            if (done)
                throw new IllegalStateException("invalid request state");
            done = true;
        }

        public synchronized boolean isInvalidForCallback() {
            return !processed || done || scheduled;
        }

        public synchronized boolean isInvalidForWriting() {
            return done || processed;
        }

    }
}

