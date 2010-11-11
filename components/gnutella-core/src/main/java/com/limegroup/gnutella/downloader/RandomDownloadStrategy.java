package com.limegroup.gnutella.downloader;


import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;


/**
 * This <code>SelectionStrategy</code> selects random Intervals from the availableIntervals.
 * <p>
 * If the number of Intervals contained in neededBytes is less than MAX_FRAGMENTS,
 * then a random location between the first and last bytes (inclusive) of neededBytes
 * is chosen. We find the last chunk before this location and the first chunk after.
 * We return one or the other chunk (as an Interval) with equal probability. Of
 * course, if there are only available bytes on one side of the location, then there
 * is only one choice for which chunk to return. For network efficiency, the random 
 * location is aligned to blockSize boundaries.
 * <p>
 * If the number of Intervals in neededBytes is greater than or equal to MAX_FRAGMENTS,
 * then the same algorithm is used, except that the location is chosen randomly from
 * an endpoint of one of the existing fragments, in an attempt to coalesce fragments.
 */
public class RandomDownloadStrategy implements SelectionStrategy {
    
    private static final Log LOG = LogFactory.getLog(RandomDownloadStrategy.class);
    
    /** Maximum number of file fragments we're willing to intentionally create */
    private static final int MAX_FRAGMENTS = 16;
    
    /**
     * A global pseudorandom number generator. We don't really care about values 
     * duplicated across threads, so don't bother serializing access.
     * 
     * This really should be final, except that making it non-final makes tests
     * much more simple.
     */
    protected static Random pseudoRandom = new Random();
    
    /** The size the download will be once completed. */
    protected final long completedSize;
    
    public RandomDownloadStrategy(long completedSize) {
        super();
        this.completedSize = completedSize;
    }
    
    /**
     * Picks a random block of a file to download next.
     * 
     * For efficiency reasons attempts will be made to align the start and end of 
     * intervals to block boundaries.  However, there are no guarantees on alignment.
     * 
     * @param candidateBytes a representation of the set of 
     *      bytes available for download from a given server, minus the set
     *      of bytes that have already been leased, verified, etc.
     *      This guarantees candidateBytes is a subset of neededBytes.
     * @param neededBytes a representation of the set of bytes
     *      of the file that have not been leased, verified, etc.
     * @param blockSize the maximum size of the returned Interval. Any values less than 1 will
     *      be ignored.  An attempt will be made to make the high end of the interval one less
     *      than a multiple of blockSize.  Any values less than 1 will generate IllegalArgumentExceptions.
     * @return the Interval that should be assigned next, with a size of at most blockSize bytes
     * @throws NoSuchElementException if passed an empty IntervalSet
     */
    public Range pickAssignment(IntervalSet candidateBytes,
            IntervalSet neededBytes,
            long blockSize) throws java.util.NoSuchElementException {
        long lowerBound = neededBytes.getFirst().getLow();
        long upperBound = neededBytes.getLast().getHigh();
        if (blockSize < 1)
            throw new IllegalArgumentException("Block size cannot be "+blockSize);
        if (lowerBound < 0)
            throw new IllegalArgumentException("lowerBound must be >= 0, "+lowerBound+"<0");
        if (upperBound >= completedSize)
            throw new IllegalArgumentException("Greatest needed byte must be less than completedSize "+
                    upperBound+" >= "+completedSize);
        if (candidateBytes.isEmpty())
            throw new NoSuchElementException();
            
        // The returned Interval will be the last chunk before idealLocation
        // or the first chunk after idealLocation
        long idealLocation = getIdealLocation(neededBytes, blockSize);
        
        // First aligned chunk after idealLocation
        Range intervalAbove = null;
        
        // Last aligned chunk before idealLocation
        Range intervalBelow = null;
        for(Range candidateInterval : candidateBytes) {
            if (candidateInterval.getLow() < idealLocation)
                intervalBelow = optimizeIntervalBelow(candidateInterval, idealLocation,
                        blockSize);
            if (candidateInterval.getHigh() >= idealLocation) {
                intervalAbove = optimizeIntervalAbove(candidateInterval,idealLocation,
                        blockSize);
                // Since we started iterating from the low end of candidateBytes,
                // the first intervalAbove is the one closest to idealLocation
                // and there will be no more changes in intervalBelow
                break;
            }
        }
        
        if (LOG.isDebugEnabled())
            LOG.debug("idealLocation="+idealLocation
                    +" intervalAbove="+intervalAbove
                    +" intervalBelow="+intervalBelow
                    +" out of possibilites:"+candidateBytes);
        // If candidateBytes is not empty, at least one of
        // intervalAbove or intervalBelow is not null.
        // If we don't have a choice, return the Interval that makes sense
        if (intervalAbove == null)
            return intervalBelow;
        if (intervalBelow == null)
            return intervalAbove;
        
        // We have a choice, so return each with equal probability.
        return ((pseudoRandom.nextInt()&1) == 1) ? intervalAbove : intervalBelow;
    }

    
    ///////////////////// Private Helper Methods /////////////////////////////////
    /** Aligns location to one byte before the next highest block boundary */
    protected long alignHigh(long location, long blockSize) {
        location += blockSize;
        location -= location % blockSize;
        return location - 1;
    }
    
    /** Aligns location to the nearest block boundary that is at or before location */
    protected long alignLow(long location, long blockSize) {
        location -= location % blockSize;
        return location;
    }
    
    /** 
     * Calculates the "ideal location" on which to base an assignment.
     */
    private long getIdealLocation(IntervalSet neededBytes, long blockSize) {
        int fragmentCount = neededBytes.getNumberOfIntervals();   
        
        if (fragmentCount >= MAX_FRAGMENTS) {
            // No fragments to spare, so attempt to reduce fragmentation by
            // setting idealLocation to the first byte of any fragment, or
            // the last byte of the last fragment.
            // Since we download on either side of the idealLocation, this has
            // the effect of "growing" our contiguous blocks of downloaded data
            // in both directions until they coalesce.
            int randomFragmentNumber = pseudoRandom.nextInt(fragmentCount + 1);
            if (randomFragmentNumber == fragmentCount)
                return neededBytes.getLast().getHigh() + 1;
            else
                return (neededBytes.getAllIntervalsAsList().get(randomFragmentNumber)).getLow();
        } else {
            // There are fragments to spare, so download from a random location
            return getRandomLocation(neededBytes.getFirst().getLow(), neededBytes.getLast().getHigh(), blockSize);
        }
    }
    
    /** Returns candidate or a sub-interval of candidate that best 
     * fits the following goals:
     * 
     * 1) returnInterval.low >= location
     * 2) returnInterval.low is as close to location as possible
     * 3) returnInterval does not span a blockSize boundary
     * 4) returnInterval is as large as possible without violating goals 1-3
     * 
     * Required precondition: candidate.high >= location
     */
    private Range optimizeIntervalAbove(Range candidate,
            long location, long blockSize) {
        
        // Calculate the most suitable low value contained
        // in candidate. (satisfying goals 1 & 2)
        long bestLow = candidate.getLow();
        if (bestLow < location) {
            bestLow = location;
        }
            
        // Calculate the most suitable high byte based on goal 3
        // This will be at most blockSize-1 bytes greater than bestLow
        long bestHigh = alignHigh(bestLow,blockSize);
      
        if (bestHigh > candidate.getHigh())
            bestHigh = candidate.getHigh();
                
        if (candidate.getHigh() == bestHigh && candidate.getLow() == bestLow)
            return candidate;
        return Range.createRange(bestLow,bestHigh); 
    }
    
    /** Returns candidate or a sub-interval of candidate that best 
     * fits the following goals:
     * 
     * 1) returnInterval.high <= location
     * 2) returnInterval.high is as close to location as possible
     * 3) returnInterval does not span a blockSize boundary
     * 4) returnInterval is as large as possible without violating goals 1-3
     * 
     * Required precondition: candidate.low < location
     */
    private Range optimizeIntervalBelow(Range candidate,
            long location, long blockSize) {
        
        // Calculate the most suitable low value contained
        // in candidate. (satisfying goals 1 & 2)
        long bestHigh = candidate.getHigh();
        if (bestHigh >= location) {
            bestHigh = location - 1;
        }
            
        // Calculate the most suitable high byte based on goal 3
        // This will be at most blockSize-1 bytes greater than bestLow
        long bestLow = alignLow(bestHigh,blockSize);
      
        if (bestLow < candidate.getLow())
            bestLow = candidate.getLow();
                
        if (candidate.getHigh() == bestHigh && candidate.getLow() == bestLow)
            return candidate;
        return Range.createRange(bestLow,bestHigh); 
    }
    
    /**
     * Calculates a random block-aligned byte offset into the file, 
     * at least minIndex bytes into the file.  If minIndex is less than blockSize
     * from maxIndex, minIndex will be returned, regardless of its alignment.
     * 
     * This function is safe for files larger than 2 GB, files with chunks larger than 2 GB,
     * and files containing more than 2 GiBi chunks.
     * 
     * This function is practically unbiased for files smaller than several terabytes.
     */
    private long getRandomLocation(long minIndex, long maxIndex, long blockSize) {
        // If minIndex is in the middle of a block, include the
        // beginning of that block.
        long minBlock = minIndex / blockSize;
        // If maxIndex is in the middle of a block, include that
        // partial block in our range
        long maxBlock = maxIndex / blockSize;
        
        // This may happen if there is only one block available to be assigned. 
        // ... just give back the minIndex
        if (minBlock >= maxBlock)
            return minIndex;  //No need to align the last partial block
        
        // Generate a random blockNumber on the range [minBlock, maxBlock]
        // return blockSize * blockNumber
        return blockSize * (minBlock + Math.abs(pseudoRandom.nextLong() % (maxBlock-minBlock+1)));
    }
}
