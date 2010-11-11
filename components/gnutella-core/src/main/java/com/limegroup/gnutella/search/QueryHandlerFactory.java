package com.limegroup.gnutella.search;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.QueryRequest;

public interface QueryHandlerFactory {

    /**
     * Factory constructor for generating a new <tt>QueryHandler</tt> 
     * for the given <tt>QueryRequest</tt>.
     *
     * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
     * @return the <tt>QueryHandler</tt> instance for this query
     */
    public QueryHandler createHandler(QueryRequest query, ReplyHandler handler,
            ResultCounter counter);

    /**
     * Factory constructor for generating a new <tt>QueryHandler</tt> 
     * for the given <tt>QueryRequest</tt>.  Used by supernodes to run
     * their own queries (ties up to ForMeReplyHandler.instance()).
     *
     * @param guid the <tt>QueryRequest</tt> instance containing data
     *  for this set of queries
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
     * @return the <tt>QueryHandler</tt> instance for this query
     */
    public QueryHandler createHandlerForMe(QueryRequest query,
            ResultCounter counter);

    /**
     * Factory constructor for generating a new <tt>QueryHandler</tt> 
     * for the given <tt>QueryRequest</tt>.
     *
     * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
     * @return the <tt>QueryHandler</tt> instance for this query
     */
    public QueryHandler createHandlerForOldLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter);

    /**
     * Factory constructor for generating a new <tt>QueryHandler</tt> 
     * for the given <tt>QueryRequest</tt>.
     *
     * @param handler the <tt>ReplyHandler</tt> for routing the replies
     * @param counter the <tt>ResultCounter</tt> that keeps track of how
     *  many results have been returned for this query
     * @return the <tt>QueryHandler</tt> instance for this query
     */
    public QueryHandler createHandlerForNewLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter);

}