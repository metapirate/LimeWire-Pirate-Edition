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
 
package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.handler.ResponseHandler;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.Result;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;


/**
 * An abstract base class for ResponseHandlers.
 */
public abstract class AbstractResponseHandler<V extends Result> implements ResponseHandler, DHTTask<V> {
    
    private static final Log LOG = LogFactory.getLog(AbstractResponseHandler.class);
    
    /** The number of errors that have occurred. */
    private int errors = 0;
    
    /** The total time that has elapsed since the request was sent. */
    private long elapsedTime;
    
    /** The timeout of this handler. */
    private long timeout;
    
    /** The maximum number of errors that may occur. */
    private int maxErrors;
    
    /** A handle to Context. */
    protected final Context context;
    
    private volatile DHTFuture<V> future;
    
    /** The time of the last response we received. */
    protected long lastResponseTime = 0L;
    
    public AbstractResponseHandler(Context context) {
        this(context, -1L, -1);
    }
    
    public AbstractResponseHandler(Context context, long timeout) {
        this(context, timeout, -1);
    }
    
    public AbstractResponseHandler(Context context, int maxErrors) {
        this(context, -1L, maxErrors);
    }
    
    public AbstractResponseHandler(Context context, long timeout, int maxErrors) {
        this.context = context;
        
        setTimeout(timeout);
        setMaxErrors(maxErrors);
    }
    
    /**
     * Returns the lock Object for the ResponseHandler.
     */
    protected Object getLock() {
    	if (future == null) {
    	    throw new IllegalStateException("Lock is null");
    	}
    	return future;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTTask#start(org.limewire.mojito.util.OnewayExchanger)
     */
    public void start(DHTFuture<V> future) {
    	this.future = future;
    	
    	synchronized (getLock()) {
            if (isDone()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Cannot start " + this + " because it's already done");
                }
                return;
            }

            try {
                start();
            } catch (DHTException t) {
                setException(t);
            }
        }
    }
    
    /**
     * Override this method to start the ResponseHandler.
     */
    protected abstract void start() throws DHTException;
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTTask#cancel()
     */
    public void cancel() {
    	future.cancel(true);
    }
    
    /**
     * Sets the timeout of this handler.
     */
    public void setTimeout(long timeout) {
        if (timeout < 0L) {
            this.timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue();
        } else {
            this.timeout = timeout;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.ResponseHandler#timeout()
     */
    public long getTimeout() {
        return timeout;
    }
    
    /**
     * Returns the total time that has elapsed since this
     * ResponseHandler is active.
     */
    protected long getElapsedTime() {
        return elapsedTime;
    }
    
    /**
     * Returns the time when the last response was received.
     */
    protected long getLastResponseTime() {
        return lastResponseTime;
    }
    
    /**
     * Resets the error counter.
     */
    protected void resetErrors() {
        errors = 0;
    }
    
    /**
     * Returns the number of errors that have occurred.
     */
    protected int getErrors() {
        return errors;
    }
    
    /**
     * Sets the maximum number of errors that may occur before
     * we're giving up to re-send a request.
     */
    public void setMaxErrors(int maxErrors) {
        if (maxErrors < 0) {
            this.maxErrors = NetworkSettings.MAX_ERRORS.getValue();
        } else {
            this.maxErrors = maxErrors;
        }
    }
    
    /**
     * Returns the maximum number of errors that may occur before
     * we're giving up to re-send a request.
     */
    public int getMaxErrors() {
        return maxErrors;
    }
    
    /**
     * The maximum time we're waiting on a lock. Subclasses
     * may overwrite this method to customize the timeout.
     */
    public long getWaitOnLockTimeout() {
        return ContextSettings.getWaitOnLock(getTimeout());
    }
    
    /**
     * See handleResponse().
     */
    protected abstract void response(ResponseMessage message, long time) throws IOException;
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.ResponseHandler#handleResponse(com.limegroup.mojito.messages.ResponseMessage, long)
     */
    public void handleResponse(ResponseMessage response, long time) throws IOException {
        synchronized (getLock()) {
            if (isCancelled() || isDone()) {
                return;
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Received " + response + " from "
                        + response.getContact() + " after " + getErrors()
                        + " errors and a total time of " + getElapsedTime()
                        + "ms");
            }

            elapsedTime += time;
            lastResponseTime = System.currentTimeMillis();
            response(response, time);
        }
    }

    /**
     * See handleTimeout().
     */
    protected abstract void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException;
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.ResponseHandler#handleTimeout(com.limegroup.mojito.KUID, java.net.SocketAddress, com.limegroup.mojito.messages.RequestMessage, long)
     */
    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage request, long time) throws IOException {
        
    	synchronized (getLock()) {
            if (isCancelled() || isDone()) {
                return;
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace(request + " to " + ContactUtils.toString(nodeId, dst)
                        + " failed after " + time + "ms");
            }

            elapsedTime += time;

            try {
                if (errors < maxErrors) {
                    resend(nodeId, dst, request);
                } else {
                    timeout(nodeId, dst, request, getElapsedTime());
                }
            } finally {
                errors++;
            }
        }
    }
    
    /**
     * Resends the given Message to nodeId/dst.
     */
    protected void resend(KUID nodeId, SocketAddress dst, RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending " + message + " to " + ContactUtils.toString(nodeId, dst));
        }
        
        context.getMessageDispatcher().send(nodeId, dst, message, this);
    }
    
    /**
     * See handleError().
     */
    protected abstract void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e);
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.ResponseHandler#handleError(com.limegroup.mojito.KUID, java.net.SocketAddress, com.limegroup.mojito.messages.RequestMessage, java.lang.Exception)
     */
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
        
    	synchronized (getLock()) {
            if (isCancelled() || isDone()) {
                return;
            }

            if (LOG.isErrorEnabled()) {
                LOG.error("Sending a " + message + " to "
                        + ContactUtils.toString(nodeId, dst) + " failed", e);
            }

            error(nodeId, dst, message, e);
        }
    }
    
    /**
     * See handleTick().
     */
    protected void tick() {
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.ResponseHandler#handleTick()
     */
    public void handleTick() {
    	synchronized (getLock()) {
            if (isCancelled() || isDone()) {
                return;
            }

            tick();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.handler.ResponseHandler#isCancelled()
     */
    public boolean isCancelled() {
        return future.isCancelled();            
    }
    
    /**
     * Returns whether or not this handler is done which
     * means if it has returned a result or threw an
     * Exception.
     */
    public boolean isDone() {
        return future.isDone();
    }
    
    /**
     * Sets the return value which will be returned by the 
     * call() method.
     */
    protected void setReturnValue(V value) {
        future.setValue(value);
    }
    
    /**
     * Sets the Exception which will be thrown by the
     * call() method.
     */
    protected void setException(DHTException ex) {
        ExecutionException e = new ExecutionException(ex);
        e.setStackTrace(ex.getStackTrace());
        future.setException(e);
    }
    
    /**
     * A helper method to throw Timeout Exceptions.
     */
    protected void fireTimeoutException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time) {
        setException(createTimeoutException(nodeId, address, request, time));
    }
    
    /**
     * A helper method to create Timeout Exceptions.
     */
    protected DHTTimeoutException createTimeoutException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time) {
        return new DHTTimeoutException(nodeId, address, request, time);
    }
}
