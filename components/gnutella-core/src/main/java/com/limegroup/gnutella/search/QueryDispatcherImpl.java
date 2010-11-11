package com.limegroup.gnutella.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.GUID;
import org.limewire.service.ErrorService;

import com.google.inject.Singleton;
import com.limegroup.gnutella.ReplyHandler;

/**
 * Manages dynamic querying for Ultrapeers.
 * <p>
 * This maintains the data for all active queries for this Ultrapeer and any
 * of its leaves, also providing an interface for removing active queries.
 * Queries may be removed, for example, when a leaf node with an active query
 * disconnects from the Ultrapeer.
 */
@Singleton
public final class QueryDispatcherImpl implements QueryDispatcher {

    /**
     * <tt>Map</tt> of outstanding queries.  
     */
    private final Map<GUID, QueryHandler> QUERIES = new HashMap<GUID, QueryHandler>();

    /**
     * <tt>List</tt> of new queries to add.
     * LOCKING: Thread-safe, although you must obtain a lock on NEW_QUERIES if
     * it's ever iterated over.  
     */
	private final List<QueryHandler> NEW_QUERIES =
        Collections.synchronizedList(new LinkedList<QueryHandler>());
    
    /**
     * The ProcessingQueue that handles sending queries out.
     *
     * Items are added to this only if it's not already processing anything.
     */
    private final ExecutorService PROCESSOR = ExecutorsHelper.newProcessingQueue("QueryDispatcher");
    
    /**
     * Whether or not processing is already active.  If it is, we don't start it up again
     * when adding new queries.
     */
    private boolean _active;
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryDispatcher#addQuery(com.limegroup.gnutella.search.QueryHandler)
     */
	public void addQuery(QueryHandler handler) {
        handler.sendQuery();  // immediately send out one query.
        synchronized(NEW_QUERIES) {
            NEW_QUERIES.add(handler);
            if(NEW_QUERIES.size() == 1 && !_active) {
                _active = true;
                PROCESSOR.execute(this);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryDispatcher#removeReplyHandler(com.limegroup.gnutella.ReplyHandler)
     */
    public void removeReplyHandler(ReplyHandler handler) {
        // if it's not a leaf connection, we don't care that it's closed
        if(!handler.isSupernodeClientConnection())
            return;
            
        remove(handler);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryDispatcher#updateLeafResultsForQuery(com.limegroup.gnutella.GUID, int)
     */
    public void updateLeafResultsForQuery(GUID queryGUID, int numResults) {
        synchronized (QUERIES) {
            QueryHandler qh = QUERIES.get(queryGUID);
            if (qh != null)
                qh.updateLeafResults(numResults);
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryDispatcher#getLeafResultsForQuery(com.limegroup.gnutella.GUID)
     */
    public int getLeafResultsForQuery(GUID queryGUID) {
        synchronized (QUERIES) {
            QueryHandler qh = QUERIES.get(queryGUID);
            if (qh == null)
                return -1;
            else
                return qh.getNumResultsReportedByLeaf();
        }
    }

    /**
     * Removes all queries using the specified <tt>ReplyHandler</tt>
     * from NEW_QUERIES & QUERIES.
     *
     * @param handler the <tt>ReplyHandler</tt> to remove
     */
    private void remove(ReplyHandler handler) {
        synchronized(NEW_QUERIES) {
            Iterator<QueryHandler> iter = NEW_QUERIES.iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getReplyHandler() == handler)
                    iter.remove();
            }
        }
        
        synchronized(QUERIES) {
            Iterator<QueryHandler> iter = QUERIES.values().iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getReplyHandler() == handler)
                    iter.remove();
            }
        }
    }
    
    /**
     * Removes the specified <tt>ReplyHandler</tt> from NEW_QUERIES & QUERIES.
     */
    private void remove(GUID guid) {
        synchronized(NEW_QUERIES) {
            Iterator<QueryHandler> iter = NEW_QUERIES.iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getGUID().equals(guid))
                    iter.remove();
            }
        }
        
        synchronized(QUERIES) {
            Iterator<QueryHandler> iter = QUERIES.values().iterator();
            while(iter.hasNext()) {
                QueryHandler qh = iter.next();
                if(qh.getGUID().equals(guid))
                    iter.remove();
            }
        }
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryDispatcher#run()
     */
	public void run() {
        while(true) {
            try {
                Thread.sleep(400);
            } catch(InterruptedException ignored) {}
            
            try {
                // If there are no more queries to process...
                if(!processQueries()) {
                    synchronized(NEW_QUERIES) {
                        // If there are no new queries to add,
                        // set active to false & leave.
                        if(NEW_QUERIES.isEmpty()) {
                            _active = false;
                            return;
                        }
                        // else, loop.
                    }
                }
                // else, loop.
            } catch(Throwable t) {
                ErrorService.error(t);
            }
        }
    }

	/**
     * Processes current queries.
     */
    private boolean processQueries() {
        synchronized (NEW_QUERIES) {
            synchronized (QUERIES) {
                for (QueryHandler qh : NEW_QUERIES)
                    QUERIES.put(qh.getGUID(), qh);
            }
            NEW_QUERIES.clear();
        }

    
        synchronized(QUERIES) {
            Iterator<QueryHandler> iter = QUERIES.values().iterator();
            while(iter.hasNext()) {
                QueryHandler handler = iter.next();
                handler.sendQuery();
                if(handler.hasEnoughResults())
                    iter.remove();
            }
            
            return !QUERIES.isEmpty();
        }
	}

    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryDispatcher#addToRemove(com.limegroup.gnutella.GUID)
     */
    public void addToRemove(GUID g) {
        remove(g);
    }
}



