package org.limewire.nio.timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Keeps track of {@link Timeoutable Timeoutables}.
 */
public class TimeoutController {
    
    private static final Log LOG = LogFactory.getLog(TimeoutController.class);
    
    private final PriorityQueue<Timeout> items = new PriorityQueue<Timeout>(20);
    
    // used to store timed out items to notify outside of lock.
    private final List<Timeout> timedout = new ArrayList<Timeout>(100);
    
    public synchronized int getNumPendingTimeouts() {
        return items.size();
    }
    
    /** Adds a <code>Timeoutable</code> to be timed out at timeout + now. */
    public synchronized void addTimeout(Timeoutable t, long now, long timeout) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding timeoutable: " + t + ", now: " + now + ", timeout: " + timeout);
        items.offer(new Timeout(t, now, timeout));
    }

    /**
     * Processes all the items that can be timed out.
     */
    public void processTimeouts(long now) {
        synchronized(this) {
            while(!items.isEmpty()) {
                Timeout t = items.peek();
                if(t != null && now >= t.expireTime) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Timing out: " + t + ", expired: " + t.expireTime + ", now: " + now + ", length: " + t.timeoutLength);
                    timedout.add(t);
                } else {
                    if(t != null && LOG.isDebugEnabled())
                        LOG.debug("Breaking -- next timeout at: " + t.expireTime + ", now: " + now);
                    break;
                }
                items.poll(); // remove it -- we only peeked before.
            }
        }
        
        for(int i = 0; i < timedout.size(); i++) {
            Timeout t = timedout.get(i);
            t.timeoutable.notifyTimeout(now, t.expireTime, t.timeoutLength);
        }
        timedout.clear();
    }
    
    /** Gets the time the next <code>Timeoutable</code> expires or -1 for never. */
    public synchronized long getNextExpireTime() {
        if(items.isEmpty())
            return -1;
        else
            return items.peek().expireTime;
    }
    
    /** Keep an <code>expireTime</code> and <code>timeoutable</code> together as one happy couple. */
    private static class Timeout implements Comparable<Timeout> {
        private final long expireTime;
        private final Timeoutable timeoutable;
        private final long timeoutLength;
        
        Timeout(Timeoutable timeoutable, long now, long timeout) {
            if (timeout > 0 && Long.MAX_VALUE - timeout < now)
                this.expireTime = Long.MAX_VALUE;
            else
                this.expireTime = now + timeout;
            this.timeoutLength = timeout;
            this.timeoutable = timeoutable;
        }
        
        /**
         * Makes items that expire sooner considered 'larger' so the max BinaryHeap is
         * sorted correctly.
         */
        public int compareTo(Timeout b) {
            return expireTime > b.expireTime ? 1 : expireTime < b.expireTime ? -1 : 0;
        }
        
        @Override
        public String toString() {
            return "TimeoutWrapper for: " + timeoutable;
        }
    }
}
