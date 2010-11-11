package com.limegroup.gnutella.search;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

@Singleton
public class QueryHandlerFactoryImpl implements QueryHandlerFactory {



    /**
     * Ultrapeers seem to get less results - lets give them a little boost.
     */
    private static final double UP_RESULT_BUMP = 1.15;
    
    /**
     * The number of results to try to get if the query came from an old
     * leaf -- they are connected to 2 other Ultrapeers that may or may
     * not use this algorithm.
     */
    private static final int OLD_LEAF_RESULTS = 20;

    /**
     * The number of results to try to get for new leaves -- they only 
     * maintain 2 connections and don't generate as much overall traffic,
     * so give them a little more.
     */
    private static final int NEW_LEAF_RESULTS = 38;

    private final QueryRequestFactory queryRequestFactory;

    private final ForMeReplyHandler forMeReplyHandler;

    private final Provider<ConnectionManager> connectionManager;

    private final Provider<MessageRouter> messageRouter;
    private final QuerySettings querySettings;

    @Inject
    public QueryHandlerFactoryImpl(QueryRequestFactory queryRequestFactory,
            ForMeReplyHandler forMeReplyHandler,
            Provider<ConnectionManager> connectionManager,
            Provider<MessageRouter> messageRouter,
            QuerySettings querySettings) {
        this.queryRequestFactory = queryRequestFactory;
        this.forMeReplyHandler = forMeReplyHandler;
        this.connectionManager = connectionManager;
        this.messageRouter = messageRouter;
        this.querySettings = querySettings;
    }

    public QueryHandlerImpl createHandler(QueryRequest query, ReplyHandler handler,
            ResultCounter counter) {
        return new QueryHandlerImpl(query, querySettings.getUltrapeerResults(), handler,
                counter, queryRequestFactory, connectionManager.get(), messageRouter.get());
    }

    public QueryHandlerImpl createHandlerForMe(QueryRequest query,
            ResultCounter counter) {
        // because UPs seem to get less results, give them more than usual
        return new QueryHandlerImpl(
                query,
                (int) (querySettings.getUltrapeerResults() * UP_RESULT_BUMP),
                forMeReplyHandler, counter, queryRequestFactory, connectionManager.get(),
                messageRouter.get());
    }

    public QueryHandlerImpl createHandlerForOldLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter) {
        return new QueryHandlerImpl(query,
                OLD_LEAF_RESULTS, handler, counter,
                queryRequestFactory, connectionManager.get(), messageRouter.get());
    }

    public QueryHandlerImpl createHandlerForNewLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter) {
        return new QueryHandlerImpl(query,
                NEW_LEAF_RESULTS, handler, counter,
                queryRequestFactory, connectionManager.get(), messageRouter.get());
    }

}
