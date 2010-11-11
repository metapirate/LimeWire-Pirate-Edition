package org.limewire.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.limewire.util.ExceptionUtils;

/** An extension to ThreadPoolExecutor that catches unexpected exceptions and reports them. */
class ErrorCatchingThreadPoolExecutor extends ThreadPoolExecutor {
    
    public ErrorCatchingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ErrorCatchingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public ErrorCatchingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ErrorCatchingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
            TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }
    
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
