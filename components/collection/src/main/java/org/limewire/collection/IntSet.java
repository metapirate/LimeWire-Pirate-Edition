package org.limewire.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a set of distinct integers. 
 * Like {@link Set}, <code>IntSet</code> is <b>not synchronized</b>.
 * <p>
 * Optimized to have an extremely compact representation
 * when the set is "dense", i.e., has many sequential elements. For example {1,
 * 2} and {1, 2, ..., 1000} require the same amount of space. All retrieval
 * operations run in O(log n) time, where n is the size of the set. Insertion
 * operations may be slower.
 * <p>
 * All methods have the same specification as the <code>Set</code> class, except 
 * that values are restricted to int' for the reason described  above. For
 * this reason, methods are not specified below.   
 * <p>
 * This class is not thread-safe.
 * <pre>
    IntSet s = new IntSet(10);
    s.add(1); s.add(1); 
    s.add(3); s.add(4); s.add(5);
    s.add(7);       
    System.out.println("Set is " + s);
    s.add(2);       
    System.out.println("Set is " + s);
    s.remove(3);        
    System.out.println("Set is " + s);

    Output:
        Set is [1, 3-5, 7]
        Set is [1-5, 7]
        Set is [1-2, 4-5, 7]

 * </pre>
 */

public class IntSet {
    /**
     * Our current implementation consists of a list of disjoint intervals,
     * sorted by starting location. As an example, the set {1, 3, 4, 5, 7} is
     * represented by
     *     [1, 3-5, 7]
     * Adding 2 turns the representation into
     *     [1-5, 7]
     * Note that [1-2 ,3-5, 7] is not allowed by the invariant below.  
     * Continuing with the example, removing 3 turns the rep into
     *     [1-2, 4-5, 7]
     *
     * We use a sorted List instead of a TreeSet because it has a more compact
     * memory footprint, and memory is at a premium here. It also makes
     * implementation much easier. Unfortunately it means that insertion 
     * and some set operations are more expensive because memory must be 
     * allocated and copied.
     *
     * INVARIANT: for all i<j, list[i].high < (list[j].low-1)
     */
    private ArrayList<Interval> list;

    /**
     * The size of this.  
     *
     * INVARIANT: size==sum over all i of (get(i).high-get(i).low+1)
     */
    private int size=0;


    /** The interval from low to high, inclusive on both ends. */
    private static class Interval {
        /** INVARIANT: low<=high */
        int low;
        int high;
        /** @requires that low<=high */
        Interval(int low, int high) {
            this.low=low;
            this.high=high;
        }
        Interval(int singleton) {
            this.low=singleton;
            this.high=singleton;
        }

        @Override
        public String toString() {
            if (low==high)
                return String.valueOf(low);
            else
                return String.valueOf(low)+"-"+String.valueOf(high);
        }
    }


    /** Checks rep invariant. */
    protected void repOk() {
        if (list.size()<2)
            return;

        int countedSize=0;
        for (int i=0; i<(list.size()-1); i++) {
            Interval lower=get(i);
            countedSize+=(lower.high-lower.low+1);
            Interval higher=get(i+1);
            assert lower.low<=lower.high :
                        "Backwards interval: "+toString();
            assert lower.high<(higher.low-1) :
                        "Touching intervals: "+toString();
        }
        
        //Don't forget to check last interval.
        Interval last=get(list.size()-1);
        assert last.low<=last.high :
                    "Backwards interval: " + this;
        countedSize+=(last.high-last.low+1);

        assert countedSize==size :
                    "Bad size.  Should be "+countedSize+" not "+size;
    }
    
    /** Returns the i'th Interval in this. */
    private Interval get(int i) {
        return list.get(i);
    }

    public int max() {
        if(list.isEmpty()) {
            return -1;
        } else {
            return list.get(list.size()-1).high;
        }
    }
    
    public int min() {
        if(list.isEmpty()) {
            return -1;
        } else {
            return list.get(0).low;
        }
    }

    /**
     * Returns the largest i s.t. list[i].low<=x, or -1 if no such i.
     * Note that x may or may not overlap the interval list[i].<p>
     *
     * This method uses binary search and runs in O(log N) time, where
     * N=list.size().  The standard Java binary search methods could not
     * be used because they only return exact matches.  Also, they require
     * allocating a dummy Interval to represent x.
     */
    private int search(int x) {
        int low=0;
        int high=list.size()-1;

        while (low<=high) {
            int i=(low+high)/2;
            int li=get(i).low;

            if (li<x)
                low=i+1;
            else if (x<li)
                high=i-1;
            else
                return i;
        }

        //Remarkably, this does the right thing.
        return high;
    }


    //////////////////////// Set-like Public Methods //////////////////////////

    public IntSet() {
        this.list=new ArrayList<Interval>();
    }

    public IntSet(int expectedSize) {
        this.list=new ArrayList<Interval>(expectedSize);
    }

    public IntSet(IntSet toCopy) {
        this(toCopy.size());
        addAll(toCopy);
    }

    public int size() {
        return this.size;
    }

    public void clear() {
        this.size = 0;
        this.list.clear();
    }

    public boolean contains(int x) {
        int i=search(x);
        if (i==-1)
            return false;

        Interval li=get(i);
        assert  li.low<=x : "Bad return value from search.";
        if (x<=li.high)
            return true;
        else
            return false;
    }


    /** @return true if the int was added, false if it already contained the int. */
    public boolean add(int x) {
        //This code is a pain--nine different return cases.  It could be
        //factored somewhat, but I believe the following is the easiest to
        //understand.  The cases are illustrated to the right.
        int i=search(x);   
        
        //Optimistically increment size.  Decrement it later if needed.
        size++;

        //Add x to beginning of list
        if (i==-1) {
            if ( list.size()==0 || x<(get(0).low-1) ) {
                //1. Add [x, x] to beginning of list.       x ---
                list.add(0, new Interval(x));
                return true;
            } else {
                //2. Merge x with beginning of list.        x----
                get(0).low=x;
                return true;
            }
        } 

        Interval lower=get(i);
        assert(lower.low<=x);
        if (x<=lower.high) {
            //3. x already in this.                         --x--
            size--;    //Undo previous increment.
            return false;          
        }
            
        //Adding x to end of the list.
        if (i==(list.size()-1)) {
            if (lower.high < (x-1)) {
                //4. Add x to end of list                   --- x
                list.add(new Interval(x));           
                return true;
            } else {
                //5. Merge x with end of list               ----x
                lower.high=x;
                return true;
            }
        }
                
        //Adding x to middle of the list
        Interval higher=get(i+1);
        boolean touchesLower=(lower.high==(x-1));
        boolean touchesHigher=(x==(higher.low-1));
        if (touchesLower) {
            if (touchesHigher) {
                //6. Merge lower and higher intervals       --x--
                lower.high=higher.high;
                list.remove(i+1);
                return true;
            } else {
                //7. Merge with lower interval              --x --
                lower.high=x;
                return true;
            }
        } else {
            if (touchesHigher) {
                //8. Merge with higher interval             -- x--
                higher.low=x;
                return true;
            } else {
                //9. Insert as new element                  -- x --
                list.add(i+1, new Interval(x));
                return true;
            }
        }
    }     

    
    public boolean remove(int x) {
        //Find the interval overlapping x.
        int i=search(x);
        if (i==-1 || x>get(i).high)
            //1. x not in this.                         ----
            return false;

        Interval interval=get(i);
        boolean touchesLow=(interval.low==x);
        boolean touchesHigh=(interval.high==x);
        if (touchesLow) {
            if (touchesHigh) {
                //2. Singleton interval.  Remove.       -- x --
                list.remove(i);
            } 
            else {
                //3. Modify low end.                    x---
                interval.low++;
            }
        } else {
            if (touchesHigh) {
                //4. Modify high end.                   ---x
                interval.high--;
            } else {
                //5. Split entire interval.             --x--
                Interval newInterval=new Interval(x+1, interval.high);
                interval.high=x-1;
                list.add(i+1, newInterval);
            }
        }
        size--;
        return true;
    }


    public boolean addAll(IntSet s) {
        //TODO2: implement more efficiently!
        boolean ret=false;
        for (IntSetIterator iter=s.iterator(); iter.hasNext(); ) {
            ret=(ret | this.add(iter.next()));
        }
        return ret;
    }

    public boolean removeAll(IntSet s) {
        //TODO2: implement more efficiently!
        boolean ret=false;
        for (IntSetIterator iter=s.iterator(); iter.hasNext(); ) {
            ret=(ret | this.remove(iter.next()));
        }
        return ret;
    }


    public boolean retainAll(IntSet s) {
        //We can't modify this while iterating over it, so we need to
        //maintain an external list of items that must go.
        //TODO2: implement more efficiently!
        List<Integer> removeList = new ArrayList<Integer>();
        for (IntSetIterator iter = this.iterator(); iter.hasNext(); ) {
            int x = iter.next();
            if (! s.contains(x))
                removeList.add(x);
        }
        //It's marginally more efficient to remove items from end to beginning.
        for (int i=removeList.size()-1; i>=0; i--) {
            int x = removeList.get(i);
            this.remove(x);
        }
        //Did we remove any items?
        return removeList.size()>0;
    }
    

    /** Ensures that this consumes the minimum amount of memory. This method
     *  should typically be called after the last call to add(..). Insertions
     *  can still be done after the call, but they might be slower.
     *
     *  Because this method only affects the performance of this, there
     *  is no modifies clause listed.  */
    public void trim() {
        list.trimToSize();
    }   


    /** 
     * Returns the values of this in order from lowest to highest, as int.
     *     @requires this not modified while iterator in use
     */
    public IntSetIterator iterator() {
        return new IntSetIterator();
    }

    /** Yields a sequence of int's (not Object's) in order, without removal
     *  support.  Otherwise exactly like an Iterator. */
    public class IntSetIterator {
        /** The next interval to yield */ 
        private int i;
        /** The next element to yield, from the i'th interval, or undefined
         *  if there are no more intervals to yield.
         *  INVARIANT: i<list.size() ==> get(i).low<=next<=get(i).high */
        private int next;
    
        private IntSetIterator() {
            i=0;
            if (i<list.size())
                next=get(i).low;
        }                       
    
        public boolean hasNext() {
            return i<list.size();
        }

        public int next() throws NoSuchElementException {
            if (! hasNext())
                throw new NoSuchElementException();

            int ret=next;
            next++;
            if (next>get(i).high) {
                //Advance to next interval.
                i++;
                if (i<list.size())
                    next=get(i).low;
            }
            return ret;
        }
    }


    @Override
    public String toString() {
        return list.toString();
    }
}
