package com.limegroup.gnutella.connection;

import java.util.concurrent.Future;

public interface ConnectionCheckerManager {

    /**
     * If a checker is active, returns the active checker.
     * Otherwise, creates a new checker that will check for connections.
     * If the checker determines that there is no active 
     * connection, it will notify the <tt>ConnectionManager</tt> to take
     * appropriate action.
     */
    public Future<Boolean> checkForLiveConnection();

    public int getNumWorkarounds();

}