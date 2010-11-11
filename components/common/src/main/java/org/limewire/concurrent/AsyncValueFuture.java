package org.limewire.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

/**
 * The base implementation of {@link AsyncFuture}.
 * 
 * <p>NOTE: The documentation for {@link FutureTask} is lying a 
 * bit. A {@link FutureTask} is not the result of an asynchronous
 * computation. It's an asynchronously scheduled but otherwise 
 * totally synchronous computation.
 * 
 * <p>That means the underlying computation (represented by a
 * {@link Runnable} or {@link Callable} task) is synchronous 
 * and it's assumed there is a result available at the end
 * of the computation.
 * 
 * <p>But what if we want to start a computation and let an
 * another {@link Thread} deliver the result at some future 
 * point in time. In the mean time we want the {@link Thread} 
 * that  started this computation do other things and not 
 * wait for the result?
 * 
 * <p>{@link AsyncFuture}s allow us to decouple the staring 
 * and finishing of operations.
 * 
 * <p>Example: Sending a message over the Network from one
 * {@link Thread} and an another {@link Thread} delivers the
 * response at some future point in time.
 * 
 * @see AsyncFuture
 * @see AsyncFutureTask
 */
public class AsyncValueFuture<V> implements AsyncFuture<V> {
    
    /**
     * {@link List} of {@link EventListener}s that were added
     * before the {@link AsyncValueFuture} completed.
     */
    private final List<EventListener<FutureEvent<V>>> listeners 
        = new ArrayList<EventListener<FutureEvent<V>>>();
    
    /**
     * 
     */
    private final OnewayExchanger<V, ExecutionException> exchanger 
        = new OnewayExchanger<V, ExecutionException>(this, true);
    
    /**
     * Creates a {@link AsyncValueFuture}
     */
    public AsyncValueFuture() {
    }
    
    /**
     * Creates a {@link AsyncValueFuture} with the given value
     */
    public AsyncValueFuture(V value) {
        setValue(value);
    }
    
    /**
     * Creates a {@link AsyncValueFuture} with the given {@link Throwable}
     */
    public AsyncValueFuture(Throwable exception) {
        setException(exception);
    }
    
    @Override
    public boolean setValue(V value) {
        boolean success = exchanger.setValue(value);
        
        if (success) {
            complete();
        }
        
        return success;
    }
    
    @Override
    public boolean setException(Throwable exception) {
        boolean success = exchanger.setException(wrap(exception));
        
        if (success) {
            complete();
        }
        
        return success;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean success = exchanger.cancel();
        
        if (success) {
            complete();
        }
        
        return success;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        checkIfEventThread();
        return exchanger.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, 
            ExecutionException, TimeoutException {
        checkIfEventThread();
        return exchanger.get(timeout, unit);
    }

    @Override
    public boolean isCancelled() {
        return exchanger.isCancelled();
    }

    @Override
    public boolean isDone() {
        return exchanger.isDone();
    }
    
    @Override
    public boolean isCompletedAbnormally() {
        return exchanger.throwsException();
    }

    /**
     * Notifies all {@link EventListener}s and calls {@link #done()}.
     */
    private void complete() {
        fireOperationComplete();
        done();
    }
    
    /**
     * Protected method invoked when this task transitions to state
     * <tt>isDone</tt> (whether normally or via cancellation). The
     * default implementation does nothing. Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    protected void done() {
        // Override
    }

    @Override
    public void addFutureListener(EventListener<FutureEvent<V>> listener) {
        Objects.nonNull(listener, "listener");
        
        boolean done = false;
        synchronized (this) {
            done = isDone();
            if (!done) {
                listeners.add(listener);
            }
        }
        
        if (done) {
            fireOperationComplete(listener);
        }
    }
    
    /**
     * Checks if the caller {@link Thread} is the event {@link Thread}.
     * The default implementation is always returning false, custom
     * implementations may override this method.
     */
    protected boolean isEventThread() {
        return false;
    }
    
    /**
     * Checks if it's called from the event {@link Thread} and
     * throws an {@link IllegalStateException} if {@link #isDone()}
     * is returning false. This is a safe-guard to ensure that
     * the event {@link Thread} cannot call any of the get()
     * methods if the {@link AsyncFuture} isn't done yet.
     */
    private void checkIfEventThread() {
        if (!isDone() && isEventThread()) {
            throw new IllegalStateException();
        }
    }
    
    /**
     * Notifies all {@link EventListener}s that were added before
     */
    @SuppressWarnings("unchecked")
    private void fireOperationComplete() {
        FutureEvent<V> event = null;
        EventListener<FutureEvent<V>>[] l = null;
        
        synchronized (this) {
            if (!listeners.isEmpty()) {
                event = FutureEvent.createEvent(this);
                l = listeners.toArray(new EventListener[0]); 
            }
        }
        
        if (l != null) {
            fireOperationComplete(l, event);
        }
    }
    
    /**
     * Notifies the given {@link EventListener}.
     */
    @SuppressWarnings("unchecked")
    private void fireOperationComplete(EventListener<FutureEvent<V>> listener) {
        FutureEvent<V> event = FutureEvent.createEvent(this);
        fireOperationComplete(new EventListener[] { listener }, event);
    }
    
    /**
     * Notifies the given {@link EventListener}s.
     */
    protected void fireOperationComplete(
            EventListener<FutureEvent<V>>[] listeners,
            FutureEvent<V> event) {
        
        for (EventListener<FutureEvent<V>> l : listeners) {
            l.handleEvent(event);
        }
    }
    
    /**
     * Takes the given {@link Throwable} and wraps it in an 
     * {@link ExecutionException} if it isn't already.
     */
    private static ExecutionException wrap(Throwable t) {
        if (t instanceof ExecutionException) {
            return (ExecutionException)t;
        }
        
        return new ExecutionException(t);
    }
}
