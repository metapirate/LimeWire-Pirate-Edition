package org.limewire.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * An extension of {@link ExecutorService} that allows Futures to be listened
 * to.
 */
public interface ListeningExecutorService extends ExecutorService {

    @Override
    public <T> ListeningFuture<T> submit(Callable<T> task);

    @Override
    public ListeningFuture<?> submit(Runnable task);

    @Override
    public <T> ListeningFuture<T> submit(Runnable task, T result);

}
