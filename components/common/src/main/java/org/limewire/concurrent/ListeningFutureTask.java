package org.limewire.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListenerList.EventListenerListContext;

/**
 * A FutureTask that can notify listeners of results. The preferred way of
 * using a {@link ListeningFuture} is by using a
 * {@link ListeningExecutorService} or {@link ScheduledListeningExecutorService},
 * in which case the <code>submit</code> methods and one-time <code>schedule</code>
 * methods will returning a {@link ListeningFuture} or a {@link ScheduledListeningFuture}.
 * 
 * However, if you are using a standard {@link ExecutorService}, you can create
 * a {@link ListeningFuture} by the following:
 * <pre>
 *      ListeningRunnableFuture future = new ListeningFutureTask(callable);
 *      executorService.execute(future);
 *      return future;
 * </pre>
 * Whereas previously you would have done this: 
 * <pre>
 *      return executorService.submit(callable);
 * </pre>
 */
public class ListeningFutureTask<V> extends FutureTask<V> implements RunnableListeningFuture<V> {

    private final AtomicReference<EventListenerList<FutureEvent<V>>> listenersRef =
        new AtomicReference<EventListenerList<FutureEvent<V>>>(new EventListenerList<FutureEvent<V>>());
    
    // The listenerContext is required to make sure that listeners are notified in the correct
    // threads.  We eagerly clear the listenerRef to release old listeners, but need to keep
    // the context around to make sure future listeners reuse the context.
    private final EventListenerListContext listenerContext = listenersRef.get().getContext();

    public ListeningFutureTask(Callable<V> callable) {
        super(callable);
    }

    public ListeningFutureTask(Runnable runnable, V result) {
        super(runnable, result);
    }

    @Override
    protected void done() {
        EventListenerList<FutureEvent<V>> listeners = listenersRef.getAndSet(null);
        assert listeners != null;

        if (listeners.size() > 0) {
            listeners.broadcast(FutureEvent.createEvent(this));
        }
    }

    @Override
    public void addFutureListener(EventListener<FutureEvent<V>> listener) {
        boolean added = false;
        EventListenerList<FutureEvent<V>> listeners = listenersRef.get();
        // Add the listener & set it back -- we add a proxy listener
        // because there's a chance that we add it to the list
        // before another thread sets it to null, leaving us
        // to potentially call methods on the listener twice.
        // (Once from the done() thread, and once from this thread.)
        if (!isDone() && listeners != null) {
            listeners.addListener(new ProxyListener<V>(listener, listenerContext));
            added = listenersRef.compareAndSet(listeners, listeners);
        }

        if (!added) {
            EventListenerList.dispatch(listener, FutureEvent.createEvent(this), listenerContext);
        }
    }

    private static class ProxyListener<V> implements EventListener<FutureEvent<V>> {
        private final AtomicBoolean called = new AtomicBoolean(false);
        private final EventListenerListContext listenerContext;

        private final EventListener<FutureEvent<V>> delegate;

        public ProxyListener(EventListener<FutureEvent<V>> delegate, EventListenerListContext listenerContext) {
            this.delegate = delegate;
            this.listenerContext = listenerContext;
        }

        @Override
        public void handleEvent(FutureEvent<V> event) {
            if (!called.getAndSet(true)) {
                // Dispatch via EventListenerList to support annotations.
                EventListenerList.dispatch(delegate, event, listenerContext);
            }
        }
    }

}
