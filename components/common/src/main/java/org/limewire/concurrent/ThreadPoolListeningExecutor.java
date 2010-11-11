package org.limewire.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.limewire.listener.EventListener;

/**
 * A ThreadPoolExecutor that creates a {@link ListeningFuture} instead of
 * {@link Future} for submitted events.
 * 
 * This allows an {@link EventListener} of {@link FutureEvent} to be added to
 * the Futures with {@link ListeningFuture#addFutureListener(EventListener)}.
 */
public class ThreadPoolListeningExecutor extends ErrorCatchingThreadPoolExecutor implements
        ListeningExecutorService {

    public ThreadPoolListeningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ThreadPoolListeningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public ThreadPoolListeningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ThreadPoolListeningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected <T> RunnableListeningFuture<T> newTaskFor(Callable<T> callable) {
        return new ListeningFutureTask<T>(callable);
    }

    @Override
    protected <T> RunnableListeningFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ListeningFutureTask<T>(runnable, value);
    }

    @Override
    public <T> ListeningFuture<T> submit(Callable<T> task) {
        return (ListeningFuture<T>)super.submit(task);
    }

    @Override
    public ListeningFuture<?> submit(Runnable task) {
        return (ListeningFuture<?>)super.submit(task);
    }

    @Override
    public <T> ListeningFuture<T> submit(Runnable task, T result) {
        return (ListeningFuture<T>)super.submit(task, result);
    }
}
