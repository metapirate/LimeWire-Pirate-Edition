package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** 
 * Provides a discrete-case priority queue. <code>BucketQueue</code> is designed 
 * to be a replacement for a binary heap for the special case when there 
 * are only a small number of positive priorities. (Priorities are zero based 
 * and therefore, when priority is set to 3, the values are [0, 1, 2]. Also, larger 
 * numbers are higher priority.)
 * <p>
 * You determine how many element cases per priority are allowed in the queue. 
 * When the queue attempts to add an element more than the maximum capacity for 
 * the priority, the first element is removed upon {@link #insert(Object, int)}. 
 * <p>
 * This class is not thread-safe.
 * 
 <pre>
                                        //priorities, capacity
    BucketQueue&lt;String&gt; bq = new BucketQueue&lt;String&gt;(3,2);

    bq.insert("Abby", 1);
    bq.insert("Bob", 1);
    System.out.println(bq);

    String returnFromInsert ;
    returnFromInsert = bq.insert("Chris", 1);
    if(returnFromInsert != null)
        System.out.println("Element " + returnFromInsert + " popped because there are already 2 elements of priority 1.");

    System.out.println(bq);
    bq.insert("Dan", 2);
    bq.insert("Eric", 2);
    bq.insert("Fred", 0);

    System.out.println("Max: " + bq.getMax().toString() + " and bq is " + bq);      

    Output:    
        [[], [Bob, Abby], []]
        Element Abby popped because there are already 2 elements of priority 1.
        [[], [Chris, Bob], []]
        Max: Eric and bq is [[Eric, Dan], [Chris, Bob], [Fred]]

 </pre>
 
 * 
 */
public class BucketQueue<E> implements Cloneable, Iterable<E> {
    /** 
     * Within each bucket, elements at the FRONT are newer then the back.  It is
     * assumed that buckets is very small; otherwise additional state could
     * speed up some of the operations.  
     */
    private Buffer<E>[] buckets;
    /**
     * The size, stored for efficiency reasons.
     * INVARIANT: size=buckets[0].size()+...+buckets[buckets.length-1].size()
     */
    private int size=0;

    /** 
     * @effects a new queue with the given number of priorities, and
     *  the given number of entries PER PRIORITY.  Hence 0 through 
     *  priorities-1 are the legal priorities, and there are up to
     *  capacityPerPriority*priorities elements in the queue.
     * @exception IllegalArgumentException priorities or capacityPerPriority
     *  is non-positive.
     */
    @SuppressWarnings("unchecked")
    public BucketQueue(int priorities, int capacityPerPriority) 
            throws IllegalArgumentException {
        if (priorities<=0)
            throw new IllegalArgumentException(
                "Bad priorities: "+priorities);
        if (capacityPerPriority<=0)
            throw new IllegalArgumentException(
                "Bad capacity: "+capacityPerPriority);

        this.buckets = new Buffer[priorities];
        for (int i=0; i<buckets.length; i++) {
            buckets[i] = new Buffer<E>(capacityPerPriority);
        }
    }

    /**
     * @effects makes a new queue that will hold up to capacities[i]
     *  elements of priority i.  Hence the legal priorities are 0
     *  through capacities.length-1
     * @exception IllegalArgumentException capacities.length<=0 or 
     *  capacities[i]<=0 for any i
     */
    @SuppressWarnings("unchecked")
    public BucketQueue(int[] capacities) throws IllegalArgumentException {
        if (capacities.length<=0)
            throw new IllegalArgumentException();
        this.buckets = new Buffer[capacities.length];

        for (int i=0; i<buckets.length; i++) {
            if (capacities[i]<=0)
                throw new IllegalArgumentException(
                    "Non-positive capacity: "+capacities[i]);
            buckets[i]=new Buffer<E>(capacities[i]);
        }
    }

    /** "Copy constructor": constructs a a new shallow copy of other. */
    @SuppressWarnings("unchecked")
    public BucketQueue(BucketQueue<? extends E> other) {
        //Note that we can't just shallowly clone other.buckets
        this.buckets = new Buffer[other.buckets.length];
        for (int i=0; i<this.buckets.length; i++) {
            this.buckets[i]=new Buffer<E>(other.buckets[i]); //clone
        }
        this.size=other.size;
    }

    /**
     * Removes all elements from the queue.
     */
    public void clear() {
        repOk();
        for (Buffer<E> bucket : buckets) {
            bucket.clear();
        }
        size=0;
        repOk();
    }

    /**
     * @modifies this.
     * @effects adds o to this, removing and returning some older element of
     *  same or lesser priority as needed
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public E insert(E o, int priority) {
        repOk();
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }

        E ret = buckets[priority].addFirst(o);
        if (ret == null)
            size++;     //Maintain invariant

        repOk();
        return ret;
    }

    /**
     * @modifies this.
     * @effects removes all o' such that o'.equals(o).  Note that p's
     *  priority is ignored.  Returns true if any elements were removed.
     */
    public boolean removeAll(Object o) {
        repOk();
        //1. For each bucket, remove o, noting if any elements were removed.
        boolean ret=false;
        for (Buffer<E> bucket : buckets) {
            ret = ret | bucket.removeAll(o);
        }
        //2.  Maintain size invariant.  The problem is that removeAll() can
        //remove multiple elements from this.  As a slight optimization, we
        //could incrementally update size by looking at buckets[i].getSize()
        //before and after the call to removeAll(..).  But I favor simplicity.
        if (ret) {
            this.size=0;
            for (Buffer<E> bucket : buckets) {
                this.size += bucket.getSize();
            }
        }
        repOk();
        return ret;
    }

    public E extractMax() throws NoSuchElementException {
        repOk();
        try {
            for (int i=buckets.length-1; i>=0 ;i--) {
                if (! buckets[i].isEmpty()) {
                    size--;
                    return buckets[i].removeFirst();
                }
            }
            throw new NoSuchElementException();
        } finally {
            repOk();
        }
    }

    public E getMax() throws NoSuchElementException {
        //TODO: we can optimize this by storing the position of the first
        //non-empty bucket.
        for (int i=buckets.length-1; i>=0 ;i--) {
            if (! buckets[i].isEmpty()) {
                return buckets[i].first();
            }
        }
        throw new NoSuchElementException();
    }

    public int size() {
        return size;
    }

    /** 
     * @effects returns the number of entries with the given priority. 
     * @exception IllegalArgumentException priority is not a legal priority, 
     *  as determined by this' constructor
     */
    public int size(int priority) throws IllegalArgumentException {
        if(priority < 0 || priority >= buckets.length) {
            throw new IllegalArgumentException("Bad priority: "+priority);
        }

        return buckets[priority].getSize();
    }

    public boolean isEmpty() {
        return size()==0;
    }

    /** 
     * @requires this not modified while iterator in use
     * @effects yields the elements of this exactly once, from highest priority
     *  to lowest priority. Within each priority level, newer elements are
     *  yielded before older ones.  
     */
    public Iterator<E> iterator() {
        return new BucketQueueIterator(buckets.length-1, this.size());
    }

    /** 
     * @requires this not modified while iterator in use
     * @effects yields the best n elements from startPriority down to to lowest
     *  priority.  Within each priority level, newer elements are yielded before
     *  older ones, and each element is yielded exactly once.  May yield fewer
     *  than n elements.
     * @exception IllegalArgumentException startPriority is not a legal priority
     *  as determined by this' constructor
     */
    public Iterator<E> iterator(int startPriority, int n) 
            throws IllegalArgumentException {
        if (startPriority<0 || startPriority>=buckets.length)
            throw new IllegalArgumentException("Bad priority: "+startPriority);

        return new BucketQueueIterator(startPriority, n);
    }

    private class BucketQueueIterator extends UnmodifiableIterator<E> {
        private Iterator<E> currentIterator;
        private int currentBucket;
        private int left;

        /**
         * @requires buckets.length>0
         * @effects creates an iterator that yields the best
         *  n elements.
         */
        public BucketQueueIterator(int startPriority, int n) {
            this.currentBucket=startPriority;
            this.currentIterator=buckets[currentBucket].iterator();
            this.left=n;
        }

        public synchronized boolean hasNext() {
            if (left<=0)
                return false;
            if (currentIterator.hasNext())
                return true;
            if (currentBucket<0)
                return false;

            //Find non-empty bucket.  Note the "benevolent side effect".
            //(Changes internal state, but not visible to caller.)
            for (currentBucket-- ; currentBucket>=0 ; currentBucket--) {
                currentIterator=buckets[currentBucket].iterator();
                if (currentIterator.hasNext())
                    return true;
            }
            return false;
        }

        public synchronized E next() {
            //This relies on the benevolent side effects of hasNext.
            if (! hasNext())
                throw new NoSuchElementException();
            
            left--;
            return currentIterator.next();
        }
    }

    /** Returns a shallow copy of this, of type BucketQueue. */
    @Override
    public BucketQueue<E> clone() throws CloneNotSupportedException {
        return new BucketQueue<E>(this);        
    }

    private void repOk() {
        /*
        int count=0;
        for (int i=0; i<buckets.length; i++) {
            count+=buckets[i].getSize();
        }
        Assert.that(count==size);
        */
    }

    @Override
    public String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("[");
        for (int i=buckets.length-1; i>=0; i--) {
            if (i!=buckets.length-1)
                buf.append(", ");
            buf.append(buckets[i].toString());
        }
        buf.append("]");
        return buf.toString();            
    }
}
