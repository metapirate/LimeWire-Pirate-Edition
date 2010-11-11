package org.limewire.collection;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;


/**
 * Provides an interval of ranges (a "range" version of {@link IntSet}). 
 * 
 <pre>
    IntervalSet is = new IntervalSet();
                    
    is.add(Range.createRange(1,4));
    is.add(Range.createRange(11,14));
    is.add(Range.createRange(21,24));
    System.out.println("Set is " + is + " intervals: " + 
                is.getNumberOfIntervals());

    is.add(Range.createRange(5,10));
    System.out.println("Set is " + is + " intervals: " + 
                is.getNumberOfIntervals());
    IntervalSet is2 = is.invert(50);
    System.out.println("Set is " + is2 + " intervals: " + 
                is2.getNumberOfIntervals());

    Output:
        Set is [1-4, 11-14, 21-24] intervals: 3
        Set is [1-14, 21-24] intervals: 2
        Set is [0, 15-20, 25-49] intervals: 3
</pre>
 * 
 */ 
 /* This is a first cut of the class and does
 * not support all the operations IntSet does, just the ones we need for now.
 */ 
public class IntervalSet implements Iterable<Range>, Serializable{
    
	private static final long serialVersionUID = -7791242963023638684L;
    
    /** 
     * size below which binary search is not worth it.  Total guess..
     */
    static final int LINEAR = 16;
    
    /**
     * Mask that tests if a range is at 1kb boundary.
     */
    private static final long KB_BOUNDARY = 0x3FF;
    
    /**
     * The sorted set of intervals this contains.
     */
    private final List<Range> intervals;
    
    //constructor.
    public IntervalSet() {
        intervals = new ArrayList<Range>();
    }

    /**
     * Creates an interval set with the given base range.
     * @param interval - range to create the interval set with.
     */
    public IntervalSet(Range interval) {
        this();
        add(interval);
    }

    /**
     * Creates an interval set representing a single Interval.
     * 
     * @param lowBound the lower bound of the represented Interval
     * @param highBound the upper bound of the represented Interval
     * @return an IntervalSet representing the range lowBound to highBound, inclusive.
     */
    public static IntervalSet createSingletonSet(long lowBound, long highBound) {
        IntervalSet ret = new IntervalSet();
        ret.add(Range.createRange(lowBound, highBound));
        return ret;
    }
    
    public void add(Range addInterval) {
        // trivial case.
        if (intervals.isEmpty()) {
            intervals.add(addInterval);
            return;
        }
        final long low = addInterval.getLow();
        final long high = addInterval.getHigh();
        Range lower=null;
        Range higher=null;
        int start = narrowStart(addInterval)[0];
        for(Iterator<Range> iter = intervals.subList(start, intervals.size()).iterator(); iter.hasNext(); ) {
            Range interval = iter.next();
            if (low<=interval.getLow() && interval.getHigh()<=high) {//  <low-------high>
                iter.remove();                             //      interval
                continue;
            }

            if (low >= interval.getLow() && interval.getHigh() >= high) //   <low, high>
                return;                                       // ....interval....
            
            if (low<=interval.getHigh() + 1 && interval.getLow() < low)    //     <low, high>
                lower=interval;                                  //  interval........

            if (interval.getLow() - 1 <=high && interval.getHigh() > high)  //     <low, high>
                higher=interval;                                  //  .........interval
            
            // if high < interval.low we must have found all overlaps since
            // intervals is sorted.
            if (higher != null || interval.getLow() > high)
                break;
        }

        //Add block.  Note that remove(..) is linear time.  That's not an issue
        //because there are typically few blocks.
        if (lower==null && higher==null) {
            //a) Doesn't overlap
            addImpl(Range.createRange(low, high));
        } else if (lower!=null && higher!=null) {
            //b) Join two blocks
            removeImpl(higher);
            removeImpl(lower);
            addImpl(Range.createRange(lower.getLow(), higher.getHigh()));
        } else if (higher!=null) {
            //c) Join with higher
            removeImpl(higher);
            addImpl(Range.createRange(low, higher.getHigh()));
        } else /*if (lower!=null)*/ {
            assert lower != null;
            //d) Join with lower
            removeImpl(lower);
            addImpl(Range.createRange(lower.getLow(), high));
        }   
    }
    
    /**
     * Adds a whole <code>IntervalSet</code> into this <code>IntervalSet</code>.
     */
    public void add(IntervalSet set) {
        for(Range interval : set)
            add(interval);
    }
    
    /**
     * Deletes any overlap of existing intervals with the Interval to delete.
     * @param deleteMe the Interval that should be deleted.
     */
    public void delete(Range deleteMe) {
        long low = deleteMe.getLow();
        long high = deleteMe.getHigh();
        Range lower = null;
        Range higher = null;
        int [] range = narrowRange(deleteMe);
        for (Iterator<Range> iter = intervals.subList(range[0],range[1]).iterator(); iter.hasNext();) {
            Range interval = iter.next();
            if (interval.getHigh() >= low && interval.getLow() <= high) { //found
                iter.remove();                                  // overlap
                if (interval.getHigh() <= high) {
                    if (interval.getLow() < low)
                        // interval.low < low <= interval.high <= high
                        lower = Range.createRange(interval.getLow(), low - 1);
                    // else 
                    // low <= interval.low <= interval.high <= high
                    // do nothing, the interval has already been removed
                        
                } else if (interval.getLow() >= low) {
                    // low <= interval.low <= high < interval.high
                    higher = Range.createRange(high + 1, interval.getHigh());
                    // safe to break here because intervals is sorted.
                    break;
                } else {
                    // interval.low < low <= high < interval.high
                    lower = Range.createRange(interval.getLow(), low - 1);
                    higher = Range.createRange(high + 1, interval.getHigh());
                    // we can break here because no other intervals will
                    // overlap with deleteMe
                    break;
                }
            }
            // stop here because intervals is sorted and all following 
            // intervals will be out of range:
            // low <= high < interval.low <= interval.high
            else if (interval.getLow() >= high)
                break;
        }
        if (lower != null)
            add(lower);
        if (higher != null)
            add(higher);
    }
    
    /**
     * Deletes all intervals in the specified set
     * from this set.
     */
    public void delete(IntervalSet set) {
        for(Range interval : set)
            delete(interval);
    }
    
    /**
     * Returns the first element without modifying this <code>IntervalSet</code>.
     * @throws <code>NoSuchElementException</code> if no intervals exist.
     */
    public Range getFirst() throws NoSuchElementException {
        if(intervals.isEmpty())
            throw new NoSuchElementException();
        
        return intervals.get(0);
    }
    
    /**
     * Returns the last element without modifying this <code>IntervalSet</code>.
     * @throws <code>NoSuchElementException</code> if no intervals exist.
     */
    public Range getLast() throws NoSuchElementException {
        if(intervals.isEmpty())
            throw new NoSuchElementException();
        
        Range ret = intervals.get(intervals.size()-1);
        return ret;
    }
    
    /** @return The number of Intervals in this <code>IntervalSet</code>. */
    public int getNumberOfIntervals() {
        return intervals.size();
    }

	/**
	 * @return Whether this interval set contains fully the given interval.
	 */
	public boolean contains(Range i) {
	    int [] range = narrowStart(i);
	    for(int j = range[0]; j < range[1]; j++) {
	        Range ours = intervals.get(j);
	        if (ours.getLow() <= i.getLow() && ours.getHigh() >= i.getHigh())
	            return true;
	        if (ours.getLow() > i.getHigh())
	            break;
	    }
        return false;        
    }
    
    /**
     * Narrows the index range where an interval would be found.
     * @return integer array with the start index at position 0 and end index at position 1.
     */
    private int [] narrowStart(Range i) {
        int size = intervals.size();
        // not worth doing binary search if too small
        if (size < LINEAR) 
            return new int[]{0,size};
        int point = Collections.binarySearch(intervals, i,IntervalComparator.INSTANCE);
        if (point < 0)
            point = -(point + 1);
        int low = Math.max(0, point - 1);
        int high = Math.min(size, point + 1);
        return new int[]{low, high};
    }
    
    /**
     * Narrows the index range where any interval overlapping with the provided interval 
     * would be found.
     * @return Integer array with the start index at position 0 and end index at position 1.
     */
    private int [] narrowRange(Range i) {
        int size = intervals.size();
        if (size < LINEAR) 
            return new int[]{0,size};
        
        int a = Collections.binarySearch(intervals, i,IntervalComparator.INSTANCE);
        if (a < 0)
            a = -(a + 1);
        int b = Collections.binarySearch(intervals, Range.createRange(i.getHigh(), i.getHigh()),IntervalComparator.INSTANCE);
        if (b < 0)
            b = -(b + 1);
        a = Math.max(0, a - 1);
        b = Math.min(size, b + 1);
        return new int[]{a,b};
    }
    
    /**
     * @return whether this interval set contains any part of the given interval.
     */
    public boolean containsAny(Range i) {
        long low = i.getLow();
        long high = i.getHigh();
        int [] range = narrowStart(i);
        for(int j = range[0]; j < range[1]; j++) {
            Range interval = intervals.get(j);
            if (low<=interval.getLow() && interval.getHigh()<=high)  //  <low-------high>
                return true;                               //      interval

            if (low >= interval.getLow() && interval.getHigh() >= high) //   <low, high>
                return true;                                  // ....interval....
            
            if (low<=interval.getHigh() + 1 && interval.getLow() < low)    //     <low, high>
                return true;                                     //  interval........

            if (interval.getLow() - 1 <=high && interval.getHigh() > high)  //     <low, high>
                return true;                                     //  .........interval
        }
        
        return false;
    }
	
    /**
     *@return a <code>List</code> of intervals that overlap 
     *<code>checkInterval</code>. For example
     * if Intervals contains{[1-4],[6-10]} and <code>checkInterval</code> is 
     * [3-8], this method returns a list of 2 intervals {[3-4],[6-8]}.
     * If there are no overlaps, this method returns an empty List.
     */
    public List<Range> getOverlapIntervals(Range checkInterval) {
        List<Range> overlapBlocks = new ArrayList<Range>(); //initialize for this write
        long high =checkInterval.getHigh();
        long low = checkInterval.getLow();
        if (low > high)
            return overlapBlocks;
        
        
        int []range = narrowRange(checkInterval);
        for(int j = range[0]; j < range[1]; j++) {
            Range interval = intervals.get(j);
            //case a:
            if(low <= interval.getLow() && interval.getHigh() <= high) {
                //Need to check the whole interval, starting point=interval.low
                overlapBlocks.add(interval);
                continue;
            }
            //case b:
            if(low<=interval.getHigh() && interval.getLow() < low) {
                overlapBlocks.add(Range.createRange(low,
                                           Math.min(high,interval.getHigh())));
            }
            //case c:
            if(interval.getLow() <= high && interval.getHigh() > high) {
                overlapBlocks.add(Range.createRange(Math.max(interval.getLow(),low),
                                               high));
            }
            //Note: There is one condition under which case b and c are both
            //true. In this case the same interval will be added twice. The
            //effect of this is that we will check the same overlap interval 
            //2 times. We are still doing it this way, because this condition
            //will not happen in practice, and the code looks better this way, 
            //and finally, it cannot do any harm - the worst that can happen is
            //that we check the exact same interval twice.
        }
        return overlapBlocks;
    }

    public Iterator<Range> getAllIntervals() {
        return intervals.iterator();
    }
    
    public Iterator<Range> iterator() {
        return intervals.iterator();
    }
    
    public List<Range> getAllIntervalsAsList() {
        return new ArrayList<Range>(intervals);
    }

    public long getSize() {
        long sum=0;
        for(Range block : intervals) {
            sum+=block.getHigh()-block.getLow()+1;
        }
        return sum;
    }
    
    public boolean isEmpty() {
        return intervals.isEmpty();
    }
    
    public void clear() {
        intervals.clear();
    }

    /**
     * Encodes the current interval set as defined in
     * http://www.limewire.org/wiki/index.php?title=HashTreeRangeEncoding.
     */
    public Collection<Integer> encode(long maxSize) {
        long numLeafs = getNumLeafs(maxSize);
        TreeStorage ts = new TreeStorage(null, new NodeGenerator.NullGenerator(), (int)numLeafs);
        ts.setAllowUnverifiedUse(true);
        for (Range r : intervals) {
            
            r = align(r, maxSize);
            
            if (r == null)
                continue;
            
            for (long i = r.getLow(); i <= r.getHigh(); i+= 1024) {
                int chunk = ts.fileToNodeId((int)(i >> 10));
                ts.add(chunk, null);
                ts.used(chunk);
            }
        }
        return ts.getUsedNodes();
    }
    
    /**
     * @param maxSize maximum size of the IntervalSet
     * @return a range aligned to 1KB boundaries, null if not possible
     */
    private Range align(Range r, long maxSize) {
        long low = r.getLow();
        long high = r.getHigh();
        
        // if this is not the last range
        if (high != maxSize - 1) {
            // if its too small, it can't be aligned
            if (high - low < 1023)
                return null;
        } else if (high % 1024 > (high - low))
            return null;
        
        if ((low & KB_BOUNDARY) != 0)
            low = (low & ~KB_BOUNDARY) + 1024;
        if (high != maxSize - 1 && ((high + 1) & KB_BOUNDARY) != 0)
            high =  ((high+1) & ~KB_BOUNDARY) - 1;
        
        if (low == r.getLow() && high == r.getHigh())
            return r;
        
        // possible for ranges less than 2k
        // low --- 1kb boundary --- end or LWC-1229
        if (high < low) 
            return null;
        
        return Range.createRange(low, high);
    }
    
    /**
     * Decodes an interval set encoded with:
     * http://www.limewire.org/wiki/index.php?title=HashTreeRangeEncoding.
     * 
     * @param maxSize the size of the file
     * @param id integers from the encoding
     */
    public void decode(long maxSize, Integer... id) {
        long numLeafs = getNumLeafs(maxSize);
        TreeStorage ts = new TreeStorage(null, new NodeGenerator.NullGenerator(), (int)numLeafs);
        for (int i : id) {
            int [] nodes = ts.nodeToFileId(i);
            if (nodes == null)
                continue;
            Range r = Range.createRange(nodes[0] * 1024L, Math.min((nodes[1]+1) * 1024L - 1, maxSize-1));
            add(r);
        }
    }
    
    private static int getNumLeafs(long size) {
        long numLeafs = size >> 10;
        if (size % 1024 != 0)
            numLeafs++;
        assert numLeafs <= Integer.MAX_VALUE;
        return (int)numLeafs;
    }
    
    /**
     * Creates an <code>IntervalSet</code> that is the negative to this 
     * <code>IntervalSet</code>.
     * @return <code>IntervalSet</code> containing all ranges not contained in this
     */
    public IntervalSet invert(long maxSize) {
        IntervalSet ret = new IntervalSet();
        if(maxSize < 1) 
            return ret; //return an empty IntervalSet
        if (intervals.size()==0) {//Nothing recorded?
            Range block=Range.createRange(0, maxSize-1);
            ret.add(block);
            return ret;
        }
            
        //Now step through list one element at a time, putting gaps into buf.
        //We take advantage of the fact that intervals are disjoint.  Treat
        //beginning specially.  
        //LOOP INVARIANT: interval!=null ==> low==interval.high
        long low=-1;
        Range interval=null;
        boolean fixed = false;
        for (Iterator<Range> iter=intervals.iterator(); iter.hasNext(); ) {
            interval = iter.next();
            if (interval.getLow()!=0 && low<interval.getLow()) {//needed for first interval
                if (low+1 > interval.getLow()-1) {
                    if(!fixed) {
                        fixed = true;
                        fix();
                        iter = intervals.iterator();
                        low = -1;
                        interval = null;
                        continue;
                    } else {
                        throw new IllegalArgumentException("constructing invalid interval "+
                                " while trying to invert \n"+toString()+
                                " \n with size "+maxSize+
                                " low:"+low+" interval.low:"+interval.getLow());
                    }
                }
                ret.add(Range.createRange(low+1, interval.getLow()-1));
            }
            low=interval.getHigh();
        }
        //Special case space between last block and end of file.
        assert interval!=null : "Null interval in getFreeBlocks";
        if (interval.getHigh() < maxSize-1)
            ret.add(Range.createRange(interval.getHigh()+1, maxSize-1));
        return ret;
    }
        
    /**
     * @return An iterator or intervals needed to fill in the holes in this
     * <code>IntervalSet</code>. Note that the <code>IntervalSet</code> does 
     * not know the maximum value of all the intervals.
     */
    public Iterator<Range> getNeededIntervals(long maxSize) {
        return this.invert(maxSize).getAllIntervals();
    }

    /**
     * Clones the <code>IntervalSet</code>. The underlying intervals are the same
     * (so they should never be modified), but the <code>TreeSet</code> this is
     * backed off of is new.
     */
    @Override
    public IntervalSet clone() {
        IntervalSet ret = new IntervalSet();
        for(Range interval : this)
            // access the internal TreeSet directly, - it's faster that way.
            ret.intervals.add(interval);
        return ret;
    }
    
    /**
     * Adds into the list, in order.
     */
    private void addImpl(Range  i) {
        int point = Collections.binarySearch(intervals, i, IntervalComparator.INSTANCE);
        if(point >= 0)
            throw new IllegalStateException("interval (" + i + ") already in list: " + intervals);
        point = -(point + 1);
        intervals.add(point, i);
    }
    
    /**
     * Removes from the list, quickly.
     */
    private void removeImpl(Range i) {
        int point = Collections.binarySearch(intervals, i, IntervalComparator.INSTANCE);
        if(point < 0)
            throw new IllegalStateException("interval (" + i + ") doesn't exist in list: " + intervals);
        intervals.remove(point);
    }

    /**
     * Comparator for intervals.
     */
    private static class IntervalComparator implements Comparator<Range> {
        private static final IntervalComparator INSTANCE = new IntervalComparator();
        public int compare(Range ia, Range ib) {
            if ( ia.getLow() > ib.getLow()) 
                return 1;
            else if (ia.getLow() < ib.getLow() )
                return -1;
            else
                return 0;
                
           // return ia.low-ib.low;
        }
    }
    
    /**
     * Lists the contained intervals.
     */
    @Override
    public String toString() {
        return intervals.toString();
    }
    
    /** Compares two <code>IntervalSet</code>s. */
    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(o instanceof IntervalSet) {
            IntervalSet s = (IntervalSet)o;
            if(intervals.size() == s.intervals.size()) {
                for(int i = 0; i < intervals.size(); i++) {
                    if(!intervals.get(i).equals(s.intervals.get(i)))
                        return false;
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     *
     * @return packed representation of the intervals.
     * at position 0 are all intervals that can fit in 31 bit representation,
     * at position 1 are the long ones (currently 40 bits).
     */
    public ByteIntervals toBytes() {
        int longRanges = 0;
        for (Range current: intervals)
            longRanges += current.isLong() ? 1 : 0;
    	byte [] ret = new byte[(intervals.size() - longRanges) *8];
        byte [] ret2 = new byte[longRanges * 10];
    	int pos = 0;
        int pos2 = 0;
        for(Range current : intervals) {
            if (current.isLong()) {
                current.toBytes(ret2, pos2);
                pos2 += 10;
            } else {
                current.toBytes(ret,pos);
                pos+=8;
            }
    	}
    	return new ByteIntervals(ret, ret2);
    }
    
    /**
     * Parses an <code>IntervalSet</code> from a byte array. At position 0 are
     * intervals that fit in 31 bits, at position 1 are those that
     * need 40 bits.
     */
    public static IntervalSet parseBytes(byte []ranges, byte []ranges5) throws IOException {
        if (ranges.length % 8 != 0 || ranges5.length % 10 != 0) 
            throw new IOException();
        
    	IntervalSet ret = new IntervalSet();
    	for (int i =0; i< ranges.length/8;i++) {
    		int low = (int)ByteUtils.uint2long(ByteUtils.beb2int(ranges,i*8));
    		int high = (int)ByteUtils.uint2long(ByteUtils.beb2int(ranges,i*8+4));
            if (high < low || low < 0)
                throw new IOException();
    		ret.add(Range.createRange(low,high));
    	}
        
        for (int i = 0; i < ranges5.length / 10; i++) {
            long low = ByteUtils.beb2long(ranges5, i * 10, 5);
            long high = ByteUtils.beb2long(ranges5, i * 10 + 5, 5);
            if (high < low || low < 0)
                throw new IOException();
            ret.add(Range.createRange(low, high));
        }
        
    	return ret;
    }
    
    /**
     * Recompose intervals to ensure that invariants are met.
     */
    private void fix() {
        String preIntervals = intervals.toString();
        
        List<Range> oldIntervals = new ArrayList<Range>(intervals);
        intervals.clear();
        for (Range oldInterval : oldIntervals) {
            add(oldInterval);
        }
        
        String postIntervals = intervals.toString();
        
        ErrorService.error(new IllegalStateException(
            "IntervalSet invariants broken.\n" + 
            "Pre  Fixing: " + preIntervals + "\n" +
            "Post Fixing: " + postIntervals));
    }
    
    /**
     * Allows you to keep int and long intervals in the same 
     * location. Lets you know how many bytes are needed to represent the set 
     * of ranges.
     *<pre>
        try{
        
            IntervalSet set = new IntervalSet();
    
            set.add(Range.createRange(55, 58));
            set.add(Range.createRange(90, 97));
            set.add(Range.createRange(3, 7));
            set.add(Range.createRange(52, 53));
            set.add(Range.createRange(28, 33));
            set.add(Range.createRange(60, 73));
            
            IntervalSet.ByteIntervals asByte = set.toBytes();
        
            System.out.println("Length of set = " + set.getSize() 
            + " and interval as a list " + set.getAllIntervalsAsList() );     
            
            //A length of zero means there haven't been any longs added to the set yet.
            System.out.println("Length of asByte's long =" + asByte.longs.length);
            
            //create a long range
            set.add(Range.createRange(0xFFFFFFFFF0l, 0xFFFFFFFFFFl));
            asByte = set.toBytes();
            System.out.println("New length of asByte's long =" 
                                + asByte.longs.length);

            System.out.println("Length of asByte=" + asByte.length() );

            //Now there will be one one long range from 
            //3 until 0xFFFFFFFFFFl (size: 10)
            set.add(Range.createRange(3, 0xFFFFFFFFFFl));
            asByte = set.toBytes();

            System.out.println("Length of set= " + set.getSize() 
            + " and interval as a list " + set.getAllIntervalsAsList() );
            
            System.out.println("Length of asByte=" + asByte.length() );

        }
        catch(Exception e){
            e.printStackTrace();
        }
        Output:
            Length of set = 39 and interval as a list [3-7, 28-33, 52-53, 55-58, 60-73, 90-97]
            Length of asByte's long =0
            New length of asByte's long =10
            Length of asByte=58
            Length of set= 1099511627773 and interval as a list [3-1099511627775]
            Length of asByte=10
     *
     </pre>
     */
    public static class ByteIntervals {
        public final byte[] ints, longs;
        private ByteIntervals(byte[] ranges, byte []ranges5) {
            this.ints = ranges;
            this.longs = ranges5;
        }
        public int length() {
            return ints.length + longs.length;
        }
    }
}
