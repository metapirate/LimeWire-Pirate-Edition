package com.limegroup.gnutella.downloader;

import java.util.Comparator;
import com.google.inject.Singleton;
import com.limegroup.gnutella.PushEndpoint;

/**
 * A RemoteFileDesc comparator for use with head pings. The comparator
 * uses five criteria to rank sources:
 * 1) Whether the source replied to a multicast query
 * 2) The source's queue status
 * 3) Whether the source is firewalled
 * 4) Whether the source has the complete file
 * 5) The round-trip time of the head ping
 */
@Singleton
class PingedRemoteFileDescComparator implements Comparator<RemoteFileDescContext> {

    private static boolean isFirewalled(RemoteFileDescContext rfdContext) {
        return rfdContext.getAddress() instanceof PushEndpoint;
    }

    public int compare(RemoteFileDescContext pongA, RemoteFileDescContext pongB) {
        // Multicasts are best
        if(pongA.isReplyToMulticast() != pongB.isReplyToMulticast()) {
            if(pongA.isReplyToMulticast())
                return -1;
            else
                return 1;
        }

        // Prefer sources with free slots (or at least short queues)
        if(pongA.getQueueStatus() > pongB.getQueueStatus())
            return 1;
        else if(pongA.getQueueStatus() < pongB.getQueueStatus())
            return -1;

        // Prefer firewalled sources - this is designed to balance load by
        // leaving non-firewalled sources for those who need them
        if(isFirewalled(pongA) != isFirewalled(pongB)) {
            if(isFirewalled(pongA))
                return -1;
            else 
                return 1;
        }

        // Prefer partial sources - this is designed to balance load by
        // leaving complete sources for those who need them
        if(pongA.isPartialSource() != pongB.isPartialSource()) {
            if(pongA.isPartialSource())
                return -1;
            else
                return 1;
        }

        // Prefer nearby sources (low round-trip time)
        if(pongA.getRoundTripTime() > pongB.getRoundTripTime())
            return 1;
        else if(pongA.getRoundTripTime() < pongB.getRoundTripTime())
            return -1;

        // No preference
        return pongA.hashCode() - pongB.hashCode();
    }
}
