package org.limewire.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.util.ExceptionUtils;
import org.limewire.util.Objects;

/**
 * An {@link AsyncFuture} that implements the {@link Runnable} interface.
 * 
 * @see AsyncValueFuture
 * @see FutureTask
 */
public class AsyncFutureTask<V> extends AsyncValueFuture<V> 
        implements RunnableAsyncFuture<V> {
    
    /**
     * This {@link Callable} is used by the default constructor.
     * It makes it very easy to extend {@link AsyncFutureTask}
     * and override the {@link #doRun()} method. The {@link Callable}
     * does nothing but throw an {@link UnsupportedOperationException}
     * and remind the user to override the {@link #doRun()} method.
     */
    private static Callable<Object> NOP = new Callable<Object>() {
        public Object call() {
            throw new UnsupportedOperationException("Override doRun()");
        }
    };
    
    /**
     * We use this {@link AtomicReference} to manage the {@link Thread}
     * that is executing this {@link AsyncFutureTask}.
     * 
     * <p>NOTE: It doesn't have to be an {@link AtomicReference}. We could
     * use a plain handle but this is very elegant.
     */
    private final AtomicReference<Interruptible> thread 
        = new AtomicReference<Interruptible>(Interruptible.INIT);
    
    /**
     * The {@link Callable} that will provide the result value
     * or throw an {@link Exception}.
     */
    private final Callable<V> callable;
    
    /**
     * Creates an {@link AsyncFutureTask}
     */
    @SuppressWarnings("unchecked")
    public AsyncFutureTask() {
        this((Callable<V>)NOP);
    }
    
    /**
     * Creates an {@link AsyncFutureTask} with the given 
     * {@link Runnable} and result value
     */
    public AsyncFutureTask(Runnable task, V result) {
        this(Executors.callable(task, result));
    }
    
    /**
     * Creates an {@link AsyncFutureTask} with the given {@link Callable}
     */
    public AsyncFutureTask(Callable<V> callable) {
        this.callable = Objects.nonNull(callable, "callable");
    }
    
    @Override
    public final void run() {
        if (preRun()) {
            try {
                doRun();
            } catch (Throwable t) {
                uncaughtException(t);
            } finally {
                postRun();
            }
        }
    }
    
    /**
     * Called before {@link #doRun()} to initialize the 
     * current {@link Thread}. Returns true upon success.
     */
    private boolean preRun() {
        return thread.compareAndSet(Interruptible.INIT, new CurrentThread());
    }
    
    /**
     * Called after {@link #doRun()} to cleanup the current {@link Thread}.
     */
    private void postRun() {
        
        // We must use synchronized here and in cancel(boolean) to
        // ensure that Threads are not being interrupted after 
        // completion of the Task.
        //
        // Example: Thread X enters the run() method and passes the
        // preRun() method. We call cancel(true) and the Interruptible
        // handle is being replaced but we haven't called the interrupt()
        // method yet. Thread X passes through the finally-block and
        // moves on to execute the next Runnable in its parent's
        // Executor queue. We call interrupt() and boom!
        //
        // The synchronized blocks make sure we're never calling after
        // completion of the task!
        
        synchronized (thread) {
            thread.set(Interruptible.DONE);
        }
    }
    
    /**
     * The actual implementation of the {@link AsyncFutureTask}'s 
     * run method.
     */
    protected void doRun() throws Exception {
        V value = callable.call();
        setValue(value);
    }
    
    /**
     * Called for all {@link Throwable}s that are caught in the 
     * {@link #run()} method. The default implementation passes it
     * on to {@link #setException(Throwable)} and it is forwarded
     * to {@link ExceptionUtils#reportOrReturn(Throwable)} if it's 
     * of type {@link RuntimeException} or {@link Error}.
     * 
     * <p>You may override this method to change the default behavior.
     */
    protected void uncaughtException(Throwable t) {
        setException(t);
        
        if (t instanceof RuntimeException
                || t instanceof Error) {
            ExceptionUtils.reportOrReturn(t);
        }
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean success = super.cancel(mayInterruptIfRunning);
        
        if (success && mayInterruptIfRunning) {
            
            // See postRun() regards the synchronized block!
            
            synchronized (thread) {
                thread.getAndSet(Interruptible.DONE).interrupt();
            }
        }
        
        return success;
    }

    /**
     * An interface for interruptible objects such as {@link Thread}s
     */
    private static interface Interruptible {
        
        /**
         * An initial value for {@link AtomicReference}s that does nothing but 
         * allows us to use {@link AtomicReference#compareAndSet(Object, Object)}.
         */
        public static final Interruptible INIT = new Interruptible() {
            @Override
            public void interrupt() {
            }
        };
        
        /**
         * An final value for {@link AtomicReference}s that does nothing but 
         * allows us to use {@link AtomicReference#compareAndSet(Object, Object)}.
         */
        public static final Interruptible DONE = new Interruptible() {
            @Override
            public void interrupt() {
            }
        };
        
        /**
         * Interrupt!
         */
        public void interrupt();
    }
    
    /**
     * An implementation of {@link Interruptible} that holds a reference
     * to the {@link Thread} (as returned by {@link Thread#currentThread()})
     * the created it.
     */
    private static class CurrentThread implements Interruptible {
        
        private final Thread currentThread = Thread.currentThread();
        
        @Override
        public void interrupt() {
            currentThread.interrupt();
        }
    }
}
