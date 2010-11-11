package com.limegroup.gnutella;

import java.util.HashSet;
import java.util.Set;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Keeps track of requests sent by a client.
 */
public class RequestCache {
    
    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(RequestCache.class);
    
    // we don't allow more than 1 request per 5 seconds
    private static final double MAX_REQUESTS = 5 * 1000;

    // time to wait before checking for hammering: 30 seconds.
    // if the averge number of requests per time frame exceeds MAX_REQUESTS
    // after FIRST_CHECK_TIME, the downloader will be banned.
    static long FIRST_CHECK_TIME = 30 * 1000;

    /**
     * The set of sha1 requests we've seen in the past WAIT_TIME.
     */
    private final Set<URN> ACTIVE_TRANSFERS;

    /**
     * The number of requests we've seen from this host so far.
     */
    private double _numRequests;

    /**
     * The time of the last request.
     */
    private long _lastRequest;

    /**
     * The time of the first request.
     */
    private long _firstRequest;

    /**
     * Constructs a new RequestCache.
     */
    public RequestCache() {
        ACTIVE_TRANSFERS = new HashSet<URN>();
        _numRequests = 0;
        _lastRequest = _firstRequest = System.currentTimeMillis();
    }

    /**
     * Tells the cache that an upload to the host has started.
     * 
     * @param sha1 the urn of the file being uploaded.
     */
    void startedTransfer(URN sha1) {
        ACTIVE_TRANSFERS.add(sha1);
    }

    /**
     * Determines whether or not the host is hammering.
     */
    boolean isHammering() {
        if (_lastRequest - _firstRequest <= FIRST_CHECK_TIME) {
            return false;
        } else {
            return ((_lastRequest - _firstRequest) / _numRequests) < MAX_REQUESTS;
        }
    }

    /**
     * Adds a new request.
     */
    void countRequest() {
        _numRequests++;
        _lastRequest = System.currentTimeMillis();
    }

    /**
     * Checks whether the given URN is a duplicate request
     */
    boolean isDupe(URN sha1) {
        return ACTIVE_TRANSFERS.contains(sha1);
    }

    /**
     * Informs the request cache that the given URN is no longer actively
     * uploaded.
     */
    void transferDone(URN sha1) {
        ACTIVE_TRANSFERS.remove(sha1);
    }
}