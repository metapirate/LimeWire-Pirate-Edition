package org.limewire.listener;

import java.util.concurrent.Executor;

import org.limewire.logging.Log;

/**
 * A <code>CachingEventMulticasterImpl</code> that dispatches events to listeners asynchronously.
 * @param <E>
 */
public class AsynchronousCachingEventMulticasterImpl<E> extends CachingEventMulticasterImpl<E> implements AsynchronousEventMulticaster<E> {

    /**
     * Creates a new AsynchronousCachingEventMulticasterImpl with a
     * <code>BroadcastPolicy</code> of ALWAYS and no Logging.
     * @param executor
     */
    public AsynchronousCachingEventMulticasterImpl(Executor executor) {
        this(executor, BroadcastPolicy.ALWAYS);
    }

    /**
     * Creates a new AsynchronousCachingEventMulticasterImpl with a
     * <code>BroadcastPolicy</code> of ALWAYS.
     * @param executor
     * @param log
     */
    public AsynchronousCachingEventMulticasterImpl(Executor executor, Log log) {
        this(executor, BroadcastPolicy.ALWAYS, log);
    }

    /**
     * Creates a new AsynchronousCachingEventMulticasterImpl with no Logging.
     * @param executor
     * @param broadcastPolicy
     */
    public AsynchronousCachingEventMulticasterImpl(Executor executor, BroadcastPolicy broadcastPolicy) {
        super(broadcastPolicy, new AsynchronousMulticasterImpl<E>(executor));
    }

    /**
     * Creates a new AsynchronousCachingEventMulticasterImpl.
     * @param executor
     * @param broadcastPolicy
     */
    public AsynchronousCachingEventMulticasterImpl(Executor executor, BroadcastPolicy broadcastPolicy, Log log) {
        super(broadcastPolicy, new AsynchronousMulticasterImpl<E>(executor, log));
    }
}
