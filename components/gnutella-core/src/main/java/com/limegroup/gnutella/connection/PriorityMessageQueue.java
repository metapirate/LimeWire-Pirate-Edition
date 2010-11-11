package com.limegroup.gnutella.connection;

import org.limewire.collection.BucketQueue;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.QueryReply;


/**
 * A message queue that prioritizes messages.  These are intended to be
 * homogeneous, i.e., to only contain one type of message at a time, though
 * that is not strictly enforced.  Message are preferenced as follows:
 *
 * <ol>
 * <li>QueryReply: messages with low GUID volume are preferred, i.e., GUID's
 *     for which few replies have already been routed.
 * <li>PingReply: messages with high hops [sic] are preferred, since they 
 *     contain addresses of hosts less likely to be in your horizon.
 * <li>Others: messages with low hops are preferred, since they have travelled
 *     down fewer redundant paths and have received fewer responses.
 * </ol>
 *
 * Then, within any given priority level, newer messages are preferred to
 * older ones (LIFO).<p>
 * 
 * Currently this is implemented with a BucketQueue, which provides LIFO
 * ordering within any given bucket.  BinaryHeap could make sense for
 * QueryReply's, but the replacement policy is undefined if the queue
 * fills up.
 */
public class PriorityMessageQueue extends AbstractMessageQueue {
    /** One priority level for each hop.  For query replies, we break reply
     *  volumes into this many buckets.  You could use different numbers of
     *  priorities according to the type of message, but this is convenient. */
    private static final int PRIORITIES=8;
    private BucketQueue<Message> _queue;

    /**
     * @param cycle the number of messages to return per cycle, i.e., between 
     *  calls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param capacity the maximum number of elements this can store.
     */
    public PriorityMessageQueue(int cycle, 
                                int timeout, 
                                int capacity) {
        super(cycle, timeout);
        //Note that this allocates PRIORITIES*capacity storage.
        this._queue=new BucketQueue<Message>(PRIORITIES, capacity);
    }

    @Override
    protected Message addInternal (Message m) {
        return _queue.insert(m, priority(m));
    }

    /** Calculates a m's priority according to its message type.  Larger values
     *  correspond to higher priorities.  */
    private static final int priority(Message m) {
        if (m instanceof QueryReply)
            return priority((QueryReply)m);         //Prefer low GUID volume
        else if (m instanceof PingReply)
            return bound(m.getHops());              //Prefer high hops
        else
            return bound(PRIORITIES-1-m.getHops()); //Prefer low hops
    }
    
    /** Picks a priority from 0 to PRIORITIES-1 roughly according to m's GUID
     *  volume, i.e., m.getPriority (). */
    private static final int priority(QueryReply m) {
        //The distribution of reply volumes has a long tale, with most GUID's
        //having a moderate number of results but a few GUID's having 400KB+
        //results.  This suggests calculating the priority from the logarithm of
        //the reply volume.  While this scheme may result in equal numbers of
        //messages in each bucket, it does not sufficiently distinguish between
        //high volume replies, which is the most important case.  Hence the
        //following algorithm.  See ConnectionManager.MAX_REPLY_ROUTE_BYTES.
        int volume=m.getPriority();
        if (volume==0)           //No replies
            return 7;
        else if (volume<1000)    //10 or fewer replies
            return 6;
        else if (volume<5000)    //50 or fewer replies
            return 5;
        else if (volume<10000)   //100 or fewer replies
            return 4;
        else if (volume<20000)   //200 or fewer replies
            return 3;
        else if (volume<30000)   //300 or fewer replies
            return 2;
        else if (volume<40000)   //400 or fewer replies
            return 1; 
        else 
            return 0;            //Anything else!
    }

    /** Ensures that x a valid priority. */
    private static final int bound(int priority) {
        if (priority<0)
            return 0;
        else if (priority>=PRIORITIES)
            return PRIORITIES-1;
        else 
            return priority;
    }

    @Override
    protected Message removeNextInternal() {        
        if (_queue.isEmpty())
            return null;
        else
            return _queue.extractMax();
    }
    
    @Override
    public int size() {
        return _queue.size();
    }
}
