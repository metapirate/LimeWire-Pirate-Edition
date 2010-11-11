/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ExecutorsHelper;

/**
 * A default implementation of DHTExecutorService.
 */
public class DefaultDHTExecutorService implements DHTExecutorService {
    
    private volatile ThreadFactory threadFactory;
    
    private ScheduledExecutorService scheduledExecutor;
    
    private ExecutorService cachedExecutor;
    
    private ExecutorService sequentialExecutor;
    
    private final String name;
    
    private volatile boolean running = false;
    
    public DefaultDHTExecutorService(String name) {
        this.name = name;
        threadFactory = new DefaultThreadFactory(name);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.concurrent.DHTExecutorService#start()
     */
    public void start() {
        if (!running) {
            initScheduledExecutor();
            initCachedExecutor();
            initSequantialExecutor();
            running = true;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.concurrent.DHTExecutorService#stop()
     */
    public void stop() {
        if (running) {
            running = false;
            cancel(scheduledExecutor.shutdownNow());
            cancel(cachedExecutor.shutdownNow());
            cancel(sequentialExecutor.shutdownNow());
        }
    }

    /**
     * Calls cancel() on every element in the given Collection
     * that implements the Cancellable interface.
     */
    private void cancel(Collection<?> c) {
        for (Object o : c) {
            if (o instanceof Cancellable) {
                ((Cancellable)o).cancel(true);
            }
        }
    }
    
    /**
     * Initializes Context's scheduled Executor.
     */
    private void initScheduledExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = getThreadFactory().newThread(r);
                thread.setName(name + "-ContextScheduledThreadPool");
                thread.setDaemon(true);
                return thread;
            }
        };
        
        scheduledExecutor = Executors.newScheduledThreadPool(1, factory);
    }
    
    /**
     * Initializes Context's (regular) Executor.
     */
    private void initCachedExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = getThreadFactory().newThread(r);
                thread.setName(name + "-ContextCachedThreadPool");
                thread.setDaemon(true);
                return thread;
            }
        };
        
        cachedExecutor = ExecutorsHelper.newThreadPool(factory);
    }
    
    private void initSequantialExecutor() {
        sequentialExecutor = ExecutorsHelper.newProcessingQueue("DHT-Sequential-Executor");
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#getThreadFactory()
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#setThreadFactory(java.util.concurrent.ThreadFactory)
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, 
            long delay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, delay, period, unit);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, 
            long initialDelay, long delay, TimeUnit unit) {
        return scheduledExecutor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#schedule(java.util.concurrent.Callable, long, java.util.concurrent.TimeUnit)
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(task, delay, unit);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(command, delay, unit);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#submit(java.util.concurrent.Callable)
     */
    public <V> Future<V> submit(Callable<V> task) {
        return cachedExecutor.submit(task);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTExecutorService#execute(java.lang.Runnable)
     */
    public void execute(Runnable command) {
        if (running) {
            cachedExecutor.execute(command);
        }
    }
    
    public void executeSequentially(Runnable command) {
        if (running) {
            sequentialExecutor.execute(command);
        }
    }
}
