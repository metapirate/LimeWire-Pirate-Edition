package com.limegroup.gnutella.search;

import org.limewire.io.GUID;

import com.limegroup.gnutella.ReplyHandler;

public interface QueryDispatcher extends Runnable {

    /**
     * Adds the specified <tt>QueryHandler</tt> to the list of queries to
     * process.
     *
     * @param handler the <tt>QueryHandler</tt> instance to add
     */
    public void addQuery(QueryHandler handler);

    /**
     * This method removes all queries for the given <tt>ReplyHandler</tt>
     * instance.
     *
     * @param handler the handler that should have it's queries removed
     */
    public void removeReplyHandler(ReplyHandler handler);

    /** Updates the relevant QueryHandler with result stats from the leaf.
     */
    public void updateLeafResultsForQuery(GUID queryGUID, int numResults);

    /** Gets the number of results the Leaf has reported so far.
     *  @return a non-negative number if the guid exists, else -1.
     */
    public int getLeafResultsForQuery(GUID queryGUID);

    /**
     * Processes queries until there is nothing left to process,
     * or there are no new queries to process.
     */
    public void run();

    /**
     * Removes all queries that match this GUID.
     * 
     * @param g the <tt>GUID</tt> of the search to remove
     */
    public void addToRemove(GUID g);

}