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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * The DHTExecutorService provides execution services for Mojito.
 */
public interface DHTExecutorService {
    
    /**
     * Starts the DHTExecutorService.
     */
    public void start();
    
    /**
     * Stops the DHTExecutorService.
     */
    public void stop();
    
    /**
     * Sets the ThreadFactory that will be used to create
     * all Thread. Passing null will reset it to the default
     * ThreadFactory.
     */
    public void setThreadFactory(ThreadFactory threadFactory);
    
    /**
     * Returns the ThreadFactory that's used to create Threads.
     */
    public ThreadFactory getThreadFactory();
    
    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * period. The action is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, 
            long delay, long period, TimeUnit unit);
    
    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * delay. The action is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, 
            long initialDelay, long delay, TimeUnit unit);
            
    /**
     * Creates and executes a ScheduledFuture that becomes enabled after the
     * given delay. The task is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> task, long delay, TimeUnit unit);
    
    /**
     * Creates and executes a ScheduledFuture that becomes enabled after the
     * given delay. The task is executed on Mojito DHTs internal scheduled 
     * Executor (an unbound ThreadPoolExecutor).
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);
    
    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task. The task is executed on
     * Mojito DHTs internal Executor (an unbound ThreadPoolExecutor).
     */
    public <V> Future<V> submit(Callable<V> task);
    
    /**
     * Executes the given command at some time in the future. The command 
     * is executed on Mojito DHTs internal Executor (an unbound ThreadPoolExecutor).
     */
    public void execute(Runnable command);
    
    /**
     * Executes the given command but does unlike execute() does not spawn
     * more than one new thread.
     */
    public void executeSequentially(Runnable command);
}
