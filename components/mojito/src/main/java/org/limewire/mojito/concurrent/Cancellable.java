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

/**
 * An interface for Cancellable Tasks.
 */
public interface Cancellable {
    
    /**
     * Returns true if this task was cancelled before it completed normally.
     * 
     * @return true if task was cancelled before it completed
     */
    public boolean isCancelled();
    
    /**
     * Attempts to cancel execution of this task. This attempt will fail if 
     * the task has already completed, already been cancelled, or could not 
     * be cancelled for some other reason. If successful, and this task has 
     * not started when cancel is called, this task should never run. If the 
     * task has already started, then the mayInterruptIfRunning parameter 
     * determines whether the thread executing this task should be interrupted 
     * in an attempt to stop the task.
     * 
     * @param mayInterruptIfRunning true if the thread executing this task should 
     *      be interrupted; otherwise, in-progress tasks are allowed to complete
     *      
     * @return false if the task could not be cancelled, typically because it 
     *      has already completed normally; true otherwise
     */
    public boolean cancel(boolean mayInterruptIfRunning);
}
