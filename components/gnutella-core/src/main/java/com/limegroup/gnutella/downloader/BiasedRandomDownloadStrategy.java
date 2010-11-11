package com.limegroup.gnutella.downloader;


import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.SystemUtils;


/** 
 * This SelectionStrategy sometimes selects the first available chunk and
 * sometimes selects a random chunk from availableIntervals.  This balances
 * the user's need to preview a file against the network's need to maximize
 * distribution of the most rare block.
 * 
 * If the user is idle MIN_IDLE_MILLISECONDS or more, a random chunk is always
 * selected.  Otherwise, the following strategy is used: if the first 
 * MIN_PRIVIEW_BYTES or MIN_PREVIEW_FRACTION of the file has not yet been 
 * assigned to Downloaders, the first chunk is selected.  If the first 50% of the
 * file has not been assigned to Downloaders, there's a 50% chance that the first
 * available chunk will be assigned and a 50% chance that a random chunk will be
 * assigned.  Otherwise, a random chunk is assigned. 
 */
public class BiasedRandomDownloadStrategy extends RandomDownloadStrategy {
    
    private static final Log LOG = LogFactory.getLog(BiasedRandomDownloadStrategy.class);
    
    /**
     *  The minimum number of bytes for a reasonable preview.
     *  This is used as a goal for the random downloader code.
     */
    private static final int MIN_PREVIEW_BYTES = 1024 * 1024; //1 MB
    
    /**
     *  The minimum fraction of bytes for a reasonable preview.
     *  This is used as a goal for the random downloader code.
     */
    private static final float MIN_PREVIEW_FRACTION = 0.1f; // 10 percent of the file
    
    /**
     *  Once this fraction of the file is previewable, we switch to a fully
     *  random download strategy.
     */
    private static final float MAX_PREVIEW_FRACTION = 0.5f; // 50 percent of the file
    
    /**
     * Number of milliseconds the user has to be idle before being considered idle.
     * This factors into the download order strategy.
     */
    /* package */ static final int MIN_IDLE_MILLISECONDS = 5 * 60 * 1000; // 5 minutes
    
    public BiasedRandomDownloadStrategy(long fileSize) {
        super(fileSize);
    }
    
    @Override
    public synchronized Range pickAssignment(IntervalSet candidateBytes,
            IntervalSet neededBytes,
            long blockSize) throws java.util.NoSuchElementException {
        long lowerBound = neededBytes.getFirst().getLow();
        long upperBound = neededBytes.getLast().getHigh();
        // Input validation
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegalArgumentException("First needed byte must be >= 0, "+lowerBound+"<0");
        if (upperBound >= completedSize)
            throw new IllegalArgumentException("neededBytes contains bytes beyond the end of the file."+
                    upperBound + " >= " + completedSize);
        if (candidateBytes.isEmpty())
            throw new NoSuchElementException();
        
        // Determine if we should return a uniformly distributed Interval
        // or the first interval.
        // nextFloat() returns a float on [0.0 1.0)
        if (getIdleTime() >= MIN_IDLE_MILLISECONDS // If the user is idle, always use random strategy
                || pseudoRandom.nextFloat() >= getBiasProbability(lowerBound, completedSize)) {
            return super.pickAssignment(candidateBytes, neededBytes, blockSize);
        }
        
        Range candidate = candidateBytes.getFirst();

        // Calculate what the high byte offset should be.
        // This will be at most blockSize-1 bytes greater than the low.
        long alignedHigh = alignHigh(candidate.getLow(), blockSize);

        // alignedHigh >= candidate.low, and therefore we
        // only have to check if alignedHigh > candidate.high.
        if (alignedHigh > candidate.getHigh())
            alignedHigh = candidate.getHigh();

        // Our ideal interval is [candidate.low, alignedHigh]
        
        // Optimize away creation of new objects, if possible
        Range ret = candidate;
        if (ret.getHigh() != alignedHigh)
            ret = Range.createRange(candidate.getLow(), alignedHigh);

        if (LOG.isDebugEnabled())
            LOG.debug("Non-random download, probability="
                    +getBiasProbability(lowerBound, completedSize)
                    +", range=" + ret + " out of choices "
                    + candidateBytes);

        return ret;
    }

    
    /////////////////// Private Helper Methods ////////////////////////////
    
    /**
     * Calculates the probability that the next block assigned should be
     * guaranteed to be from the beginning of the file. This is calculated as a
     * step function that is at 100% until max(MIN_PREVIEW_BYTES,
     * MIN_PREVIEW_FRACTION * completedSize), then drops down to 50% until
     * MAX_PREVIEW_FRACTION of the file is downloaded. Above
     * MAX_PREVIEW_FRACTION, the function returns 0%, indicating that a fully
     * random downloading strategy should be used.
     * 
     * @return the probability that the next chunk should be forced to be
     *         downloaded from the beginning of the file.
     */
    private float getBiasProbability(long previewBytesDownloaded, long fileSize) {
        long goal = Math.max(MIN_PREVIEW_BYTES, (long)(MIN_PREVIEW_FRACTION * fileSize));
        // If we don't have our goal yet, devote 100% of resources to extending 
        // the previewable length
        if (previewBytesDownloaded < goal) 
            return 1.0f;
        
        // If we have less than the cutoff (currently 50% of the file) 
        if (previewBytesDownloaded < MAX_PREVIEW_FRACTION * fileSize)
            return 0.5f;
        
        return 0.0f;
    }
    
    /**
     * Gets the number of milliseconds that the user has been idle.
     * The actual granularity of this time measurement is likely much 
     * greater than one millisecond.
     * 
     * This is stubbed out in some tests.
     * 
     * @return the number of milliseconds that the user has been idle.
     */
    protected long getIdleTime() {
        return SystemUtils.getIdleTime();
    }
}
