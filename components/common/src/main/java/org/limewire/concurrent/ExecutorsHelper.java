package org.limewire.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.util.Objects;


/**
 * A factory that builds {@link ExecutorService}, {@link ThreadFactory}
 * and {@link ScheduledExecutorService} objects via static methods.
 * <p>
 * <code>ExecutorsHelper</code> differs from {@link Executors} since 
 * <code>ExecutorsHelper</code> returns the thread factory for daemon threads
 * and creates non-fixed size thread pools. Additionally, 
 * <code>ExecutorsHelper</code> guarantees the returned <code>ExeuctorService
 * </code> will allow worker threads to expire. On the other hand, 
 * <code>Executors</code> create <code>ExecutorService</code>s whose core-pool
 * of worker threads never die.
 */
public class ExecutorsHelper {
        
    /**
     * Creates a new "ProcessingQueue" using 
     * {@link #daemonThreadFactory(String)} as thread factory.
     * <p>
     * See {@link #newProcessingQueue(ThreadFactory)}.
     * 
     * @param name the name of the processing thread that is created 
     * with the daemon thread factory.
     */
    public static ListeningExecutorService newProcessingQueue(String name) {
        return newProcessingQueue(daemonThreadFactory(name));
    }
    
    /**
     * Creates a new "ProcessingQueue".
     * <p>
     * A <code>ProcessingQueue</code> is an <code>ExecutorService</code> that 
     * will process all Runnables/Callables sequentially, creating one thread 
     * for processing when it needs it.
     * <p>
     * See {@link #newSingleThreadExecutor(ThreadFactory)}.
     * 
     * @param factory the factory used for creating a new processing thread 
     */
    public static ListeningExecutorService newProcessingQueue(ThreadFactory factory) {
        return unconfigurableExecutorService(newSingleThreadExecutor(factory));
    }
    
    /**
     * A ProcessingQueue Executor is an <code>ExecutorService</code> that 
     * processes all Runnables/Callables sequentially, creating one thread 
     * for processing when it needs it.
     * <p>
     * This kind of Executor is ideal for long-lived tasks
     * that require processing rarely.
     * <p>
     * If there are no tasks the thread will be terminated after a timeout of
     * 5 seconds and a new one will be created when necessary.
     * 
     * @param factory the factory used for creating a new processing thread 
     */
    public static ThreadPoolListeningExecutor newSingleThreadExecutor(ThreadFactory factory) {
        ThreadPoolListeningExecutor tpe = new ThreadPoolListeningExecutor(1, 1,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                factory);
        tpe.allowCoreThreadTimeOut(true);
        return tpe;
    }
    
    /**
     * Creates a new ThreadPool.
     * The pool is tuned to begin with zero threads and maintain zero threads,
     * although an unlimited number of threads will be created to handle
     * the tasks.  Each thread is set to linger for a short period of time,
     * ready to handle new tasks, before the thread terminates.
     */
    public static ListeningExecutorService newThreadPool(String name) {
        return unconfigurableExecutorService(
                new ThreadPoolListeningExecutor(0, Integer.MAX_VALUE,
                        5L, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(),
                        daemonThreadFactory(name)));
    }
    
    /**
     * Creates a new ThreadPool.
     * The pool is tuned to begin with zero threads and maintain zero threads,
     * although an unlimited number of threads will be created to handle
     * the tasks.  Each thread is set to linger for a short period of time,
     * ready to handle new tasks, before the thread terminates.
     * 
     * @param factory the factory used for creating a new processing thread 
     */
    public static ListeningExecutorService newThreadPool(ThreadFactory factory) {
        return unconfigurableExecutorService(
                new ThreadPoolListeningExecutor(0, Integer.MAX_VALUE,
                        5L, TimeUnit.SECONDS,
                        new SynchronousQueue<Runnable>(),
                        factory));
    }
    
    /**
     * Creates a new ThreadPool with the maximum number of available threads.
     * Items added while no threads are available to process them will wait
     * until an executing item is finished and then be processed.
     */
    public static ListeningExecutorService newFixedSizeThreadPool(int size, String name) {
        ThreadPoolListeningExecutor tpe =  new ThreadPoolListeningExecutor(size, size,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                daemonThreadFactory(name));
        tpe.allowCoreThreadTimeOut(true);
        return unconfigurableExecutorService(tpe);
    }
    

    /**
     * Returns an object that delegates all defined {@link
     * ListeningExecutorService} methods to the given executor, but not any
     * other methods that might otherwise be accessible using
     * casts. This provides a way to safely "freeze" configuration and
     * disallow tuning of a given concrete implementation.
     * 
     * @return an <tt>ListeningExecutorService</tt> instance
     * @throws NullPointerException if executor null
     */
    public static ListeningExecutorService unconfigurableExecutorService(ListeningExecutorService es) {
        return new DelegatedExecutorService(Objects.nonNull(es, "es"));
    }
    
    
    /**
     * Returns the default thread factory, using the given name.
     */
    public static ThreadFactory defaultThreadFactory(String name) {
        return new DefaultThreadFactory(name, false);
    }
    
    /**
     * Returns the a thread factory of daemon threads, using the given name.
     */
    public static ThreadFactory daemonThreadFactory(String name) {
        return new DefaultThreadFactory(name, true);
    }

    /** A thread factory that can create threads with a name. */
    private static class DefaultThreadFactory implements ThreadFactory {
        /** The name created threads will use. */
        private final String name;
        /** Whether or not the created thread is a daemon thread. */
        private final boolean daemon;
        
        /** Constructs a thread factory that will created named threads. */
        public DefaultThreadFactory(String name, boolean daemon) {
            this.name = name;
            this.daemon = daemon;
        }
        
        public Thread newThread(Runnable r) {
            Thread t = new ManagedThread(r, name);
            if(daemon)
                t.setDaemon(true);
            return t;
        }
    }
    
    /**
     * A wrapper class that exposes only the ListeningExecutorService methods
     * of an ListeningExecutorService implementation.
     */
    private static class DelegatedExecutorService extends AbstractExecutorService implements ListeningExecutorService {
        private final ListeningExecutorService e;
        DelegatedExecutorService(ListeningExecutorService executor) { e = executor; }
        public void execute(Runnable command) { e.execute(command); }
        public void shutdown() { e.shutdown(); }
        public List<Runnable> shutdownNow() { return e.shutdownNow(); }
        public boolean isShutdown() { return e.isShutdown(); }
        public boolean isTerminated() { return e.isTerminated(); }
        public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }
        @Override
        public ListeningFuture<?> submit(Runnable task) {
            return e.submit(task);
        }
        @Override
        public <T> ListeningFuture<T> submit(Callable<T> task) {
            return e.submit(task);
        }
        @Override
        public <T> ListeningFuture<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
            return e.invokeAll(tasks);
        }
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                             long timeout, TimeUnit unit)
            throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }    
    
}
