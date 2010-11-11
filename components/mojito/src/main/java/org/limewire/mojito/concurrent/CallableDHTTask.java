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
import java.util.concurrent.ExecutionException;

/**
 * A CallableDHTTask takes a DHTTask, starts it and waits for
 * the result.
 */
public class CallableDHTTask<T> implements Callable<T> {
    
    private final DHTValueFuture<T> future = new DHTValueFuture<T>();
    
    private final DHTTask<T> task;
    
    private boolean started = false;
    
    public CallableDHTTask(DHTTask<T> task) {
        this.task = task;
    }

    public T call() throws InterruptedException, ExecutionException {
        synchronized (future) {
            if (!started) {
                task.start(future);
                started = true;
            }
            
            return future.get();
        }
    }
}
