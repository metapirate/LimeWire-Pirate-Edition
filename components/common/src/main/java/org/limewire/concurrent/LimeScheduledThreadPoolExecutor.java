package org.limewire.concurrent;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * An impl of ScheduledExecutorService which ensures that
 * the uncaught exception handler is called, so that an error can
 * be properly reported.
 */
public class LimeScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    public LimeScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize);
    }

    public LimeScheduledThreadPoolExecutor(int corePoolSize, 
                                           ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public LimeScheduledThreadPoolExecutor(int corePoolSize, 
                                           RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public LimeScheduledThreadPoolExecutor(int corePoolSize, 
                                           ThreadFactory threadFactory, 
                                           RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }


    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        
        if (r instanceof FutureTask) {
            FutureTask task = (FutureTask)r;
            try {
                if (task.isDone() && !task.isCancelled()) {
                    task.get();
                }
            } catch (InterruptedException e) {
                // do nothing here. let code which is interested 
                // in the result of the task deal with this
            } catch (ExecutionException e) {
                Throwable badStuff = e.getCause();
                if (badStuff instanceof RuntimeException || badStuff instanceof Error) {
                    Thread currThread = Thread.currentThread();
                    Thread.UncaughtExceptionHandler handler = currThread.getUncaughtExceptionHandler();
                    
                    // should not be null
                    if (handler != null) {
                        handler.uncaughtException(currThread, badStuff);    
                    } else {
                        // default uncaught exception handler is set early on in Initializer
                        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(currThread, badStuff);
                    }
                }
            }
        }
    }
}
