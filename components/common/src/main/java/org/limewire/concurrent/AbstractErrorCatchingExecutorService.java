package org.limewire.concurrent;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.limewire.util.ExceptionUtils;

/** An extension to AbstractExecutorService that catches unexpected exceptions and reports them. */
abstract class AbstractErrorCatchingExecutorService extends AbstractExecutorService {
        
    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return super.submit(new Callable<T>() {
            public T call() throws Exception {
                try {
                    return task.call();
                } catch(RuntimeException re) {
                    ExceptionUtils.reportOrReturn(re);
                    throw re;
                } catch(Error error) {
                    ExceptionUtils.reportOrReturn(error);
                    throw error;
                }
            };
        });
    }
    
    @Override
    public Future<?> submit(final Runnable task) {
        return super.submit(new Runnable() {
            public void run() {
                try {
                    task.run();
                } catch (Throwable t) {
                    ExceptionUtils.reportOrReturn(t);
                    ExceptionUtils.rethrow(t);
                }
            }
        });
    }
    
    @Override
    public <T> Future<T> submit(final Runnable task, T result) {
        return super.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Throwable t) {
                    ExceptionUtils.reportOrReturn(t);
                    ExceptionUtils.rethrow(t);
                }
            }
        }, result);
    }

}
