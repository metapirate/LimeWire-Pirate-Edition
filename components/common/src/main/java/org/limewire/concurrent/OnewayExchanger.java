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

package org.limewire.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The OnewayExchanger is an one-way synchronization point
 * for Threads. One or more Threads can wait for the arrival
 * of a value by calling the get() method which will block
 * and suspend the Threads until an another Thread sets a
 * return value or an Exception which will be thrown by the
 * get() method.
 * <p>
 * The main differences between OnewayExchanger and 
 * java.util.concurrent.Exchanger are:
 * 
 * <ol>
 * <li> Multiple Threads can wait for a result from a single Thread.
 * <li> It's a one-way exchange.
 * <li> The setter Thread may set an exception causing this exception
 * to be thrown on the getter side.
 * <li> The OnewayExchanger is cancellable.
 * <li> The OnewayExchanger can be configured for a single shot. That
 * means once a return value or an exception has been set they cannot
 * be changed anymore.
 * </ol>
 */
public class OnewayExchanger<V, E extends Throwable> {
    
    /** A helper Object to represent 'this' at construction time */
    private static final Object THIS = new Object();
    
    /** The lock Object */
    private final Object lock;
    
    /** Flag for whether or not this is an one-shot exchanger */
    private final boolean oneShot;
    
    /** Flag for whether or not we're done */
    private boolean done = false;
    
    /** Flag for whether or not the exchanger was cancelled */
    private boolean cancelled = false;
    
    /** The value we're going to return */
    private V value;
    
    /** The Exception we're going to throw */
    private E exception;
    
    /**
     * Creates an {@link OnewayExchanger} with the default configuration.
     */
    public OnewayExchanger() {
        this(THIS, false);
    }
    
    /**
     * Creates an {@link OnewayExchanger} with the given lock.
     */
    public OnewayExchanger(Object lock) {
        this(lock, false);
    }
    
    /**
     * Creates an {@link OnewayExchanger} that is either re-usable
     * or not.
     */
    public OnewayExchanger(boolean oneShot) {
        this(THIS, oneShot);
    }
    
    /**
     * Creates an {@link OnewayExchanger} with the given lock
     * and whether or not it can be re-used.
     */
    public OnewayExchanger(Object lock, boolean oneShot) {
        this.lock = lock != THIS ? lock : this;
        this.oneShot = oneShot;
    }
    
    /**
     * Returns the lock Object.
     */
    public Object getLock() {
        return lock;
    }
    
    /**
     * Waits for another Thread for a value or an Exception
     * unless they're already set in which case this method
     * will return immediately.
     */
    public V get() throws InterruptedException, E {
        try {
            return get(0L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException cannotHappen) {
            throw new Error(cannotHappen);
        }
    }
    
    /**
     * Waits for another Thread for the given time for a value 
     * or an Exception unless they're already set in which case 
     * this method will return immediately.
     */
    public V get(long timeout, TimeUnit unit) 
            throws InterruptedException, TimeoutException, E {
        
        synchronized (lock) {
            if (!done) {
                if (timeout == 0L) {
                    lock.wait();
                } else {
                    unit.timedWait(lock, timeout);
                }
                
                // Not done? Must be a timeout!
                if (!done) {
                    throw new TimeoutException();
                }
            }
            
            if (cancelled) {
                throw new CancellationException();
            }
            
            // Prioritize Exceptions!
            if (exception != null) {
                throw exception;
            }
            
            return value;
        }
    }
    
    /**
     * Tries to get the value without blocking.
     */
    public V tryGet() throws InterruptedException, E {
        synchronized (lock) {
            if (done) {
                return get();
            } else {
                return null;
            }
        }
    }
    
    /**
     * Tries to cancel the OnewayExchanger and returns true
     * on success.
     */
    public boolean cancel() {
        synchronized (lock) {
            if (done) {
                return cancelled;
            }
            
            done = true;
            cancelled = true;
            lock.notifyAll();
            return true;
        }
    }
    
    /**
     * Returns true if the OnewayExchanger is cancelled.
     */
    public boolean isCancelled() {
        synchronized (lock) {
            return cancelled;
        }
    }
    
    /**
     * Returns true if the get() method will return immediately
     * by throwing an Exception or returning a value.
     */
    public boolean isDone() {
        synchronized (lock) {
            return done;
        }
    }
    
    /**
     * Returns true if calling the get() method will
     * throw an Exception.
     */
    public boolean throwsException() {
        synchronized (lock) {
            return cancelled || exception != null;
        }
    }
    
    /**
     * Returns true if this is an one-shot OnewayExchanger.
     */
    public boolean isOneShot() {
        return oneShot;
    }
    
    /**
     * Sets the value that will be returned by the get() method.
     */
    public boolean setValue(V value) {
        synchronized (lock) {
            if (cancelled) {
                return false;
            }
            
            if (done && oneShot) {
                return false;
            }
            
            done = true;
            this.value = value;
            lock.notifyAll();
            return true;
        }
    }
    
    /**
     * Sets the Exception that will be thrown by the get() method.
     */
    public boolean setException(E exception) {
        synchronized (lock) {
            if (exception == null) {
                throw new NullPointerException();
            }
            
            if (cancelled) {
                return false;
            }
            
            if (done && oneShot) {
                return false;
            }
            
            done = true;
            this.exception = exception;
            lock.notifyAll();
            return true;
        }
    }
    
    /**
     * Resets the OnewayExchanger so that it can be
     * reused unless it's configured for a single shot.
     */
    public boolean reset() {
        synchronized (lock) {
            if (!oneShot && done) {
                done = false;
                cancelled = false;
                value = null;
                exception = null;
                return true;
            }
            return false;
        }
    }
}
