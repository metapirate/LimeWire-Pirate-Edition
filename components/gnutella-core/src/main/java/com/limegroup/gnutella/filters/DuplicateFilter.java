package com.limegroup.gnutella.filters;

import org.limewire.collection.Buffer;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * A spam filter that tries to eliminate duplicate packets from overzealous
 * users. Since requests are not traceable, we have to use the following
 * heuristic: two pings or queries are considered duplicates if they have
 * similar GUIDs, arrived within BUF_SIZE messages of each other, and arrived
 * not more than LAG milliseconds apart.
 */
public class DuplicateFilter implements SpamFilter {
    
    private static Log LOG = LogFactory.getLog(DuplicateFilter.class);
    
    /**
     * The number of old messages to keep in memory.  If this is too small, we
     * won't be filtering properly.  If this is too large, lookup becomes
     * expensive. 
     */
    private static final int BUF_SIZE = 20;

    /** The time, in milliseconds, allowed between similar messages. */
    private static final int LAG = 500;

    /**
     * When comparing two messages, if the GUIDs of the two messages differ
     * in more than TOLERANCE bytes, the second message will be allowed.
     * If they differ in less than or equal to TOLERANCE bytes the second
     * message will not be allowed thro'
     */
    private static final int TOLERANCE = 2;

    /**
     * A list of the GUIDs of the last messages we saw, their timestamps and
     * hop counts.
     * INVARIANT: the youngest entries have largest timestamps
     */
    private final Buffer<GUIDPair> guids = new Buffer<GUIDPair>(BUF_SIZE);

    private int lag = LAG;

    @Override
    public boolean allow(Message m) {
        //Do NOT apply this filter to pongs, query replies, or pushes,
        //since many of those will (legally) have the same GUID.       
        if(!(m instanceof QueryRequest || m instanceof PingRequest))
            return true;

        GUIDPair me = new GUIDPair(m.getGUID(),
                System.currentTimeMillis(), m.getHops());

        //Consider all messages that came in within GUID_LAG milliseconds 
        //of this...
        int size = guids.getSize();
        for(int i = 0; i < size; i++) {             
            GUIDPair other = guids.get(i);
            //The following assertion fails for mysterious reasons on the
            //Macintosh.  Also, it can fail if the user adjusts the clock, e.g.,
            //for daylight savings time.  Luckily it need not hold for the code
            //to work correctly.  
            //  Assert.that(me.time>=other.time,"Unexpected clock behavior");
            if(me.time - other.time > lag)
                //All remaining pings have smaller timestamps.
                break;
            //If different hops, keep looking
            if(other.hops != me.hops)
                continue;
            //Are the GUIDs similar?
            int misses = 0;
            for(int j = 0; j < me.guid.length && misses <= TOLERANCE; j++) {
                if(me.guid[j] != other.guid[j])
                    misses++;
            }
            if(misses <= TOLERANCE) {//really close GUIDS
                if (LOG.isDebugEnabled()) 
                    LOG.debugf("not allowing: {0}", m);
                guids.add(me);
                return false;
            }
        }
        guids.add(me);
        return true;        
    }

    // For testing
    int getLag() {
        return lag;
    }

    // For testing
    void setLag(int lag) {
        this.lag = lag;
    }

    private static class GUIDPair {
        byte[] guid;
        long time;
        int hops;

        GUIDPair(byte[] guid, long time, int hops) {
            this.guid = guid;
            this.time = time;
            this.hops = hops;
        }

        @Override
        public String toString() {
            return "[" + (new GUID(guid)).toString() + ", " + time + "]";
        }
    }
}