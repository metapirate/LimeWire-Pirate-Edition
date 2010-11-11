package org.limewire.concurrent;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;

/** An extension to {@link AbstractExecutorService} that uses {@link ListeningFuture}. */
public abstract class AbstractListeningExecutorService extends AbstractErrorCatchingExecutorService implements ListeningExecutorService {
    
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
