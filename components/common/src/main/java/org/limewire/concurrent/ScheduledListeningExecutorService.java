package org.limewire.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An extension of {@link ScheduledExecutorService} that allows Futures to be listened
 * to.
 */
public interface ScheduledListeningExecutorService extends ScheduledExecutorService, ListeningExecutorService {
    
    @Override
    public <V> ScheduledListeningFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
    
    @Override
    public ScheduledListeningFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
    
    

}
