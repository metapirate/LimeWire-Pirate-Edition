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
import java.math.BigInteger;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTBadResponseException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.messages.PingResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.PingSettings;
import org.limewire.mojito.util.ContactUtils;


/**
 * This class pings a given number of hosts in parallel 
 * and returns the first successful ping.
 */
public class PingResponseHandler extends AbstractResponseHandler<PingResult> {
    
    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    /** The number of pings to send in parallel. */
    private int parallelism;
    
    private int maxParallelPingFailures;
    
    private final Contact sender;
    
    private int active = 0;
    
    private int failures = 0;
    
    private final PingIterator pinger;
    
    public PingResponseHandler(Context context, PingIterator pinger) {
        this(context, null, pinger);
    }
    
    public PingResponseHandler(Context context, Contact sender, PingIterator pinger) {
        super(context);
        
        this.sender = sender;
        this.pinger = pinger;
        
        setParallelism(-1);
        setMaxParallelPingFailures(-1);
    }

    public void setParallelism(int parallelism) {
        if (parallelism < 0) {
            this.parallelism = PingSettings.PARALLEL_PINGS.getValue();
        } else if (parallelism > 0) {
            this.parallelism = parallelism;
        } else {
            throw new IllegalArgumentException("parallelism=" + parallelism);
        }
    }
    
    public int getParallelism() {
        return parallelism;
    }
    
    public void setMaxParallelPingFailures(int maxParallelPingFailures) {
        if (maxParallelPingFailures < 0) {
            this.maxParallelPingFailures 
                = PingSettings.MAX_PARALLEL_PING_FAILURES.getValue();
        } else {
            this.maxParallelPingFailures = maxParallelPingFailures;
        }
    }
    
    public int getMaxParallelPingFailures() {
        return maxParallelPingFailures;
    }
    
    @Override
    protected void start() throws DHTException {
        
        if (!pinger.hasNext()) {
            throw new DHTException("No hosts to ping");
        }
        
        try {
            pingNextAndThrowIfDone(new DHTException(
                "All SocketAddresses were invalid and there are no Hosts left to Ping: "
                    + context.getLocalNode() + "; " + pinger));
        } catch (IOException e) {
            throw new DHTException(e);
        }
    }

    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
    	decrementActive();
    	
    	PingResponse response = (PingResponse)message;
        
        Contact node = response.getContact();
        SocketAddress externalAddress = response.getExternalAddress();
        BigInteger estimatedSize = response.getEstimatedSize();
        
        if (node.getContactAddress().equals(externalAddress)) {
            pingNextAndThrowIfDone(new DHTBadResponseException(node 
                    + " is trying to set our external address to its address!"));
            return;
        }
        
        // Check if the other Node has the same ID as we do
        if (context.isLocalNodeID(node.getNodeID())) {
            
            // If so check if this was a Node ID collision
            // test ping. To do so see if we've set a customized
            // sender which has a different Node ID than our
            // actual Node ID
            
            if (sender == null) {
                pingNextAndThrowIfDone(new DHTBadResponseException(node 
                        + " is trying to spoof our Node ID"));
            } else {
                setReturnValue(new PingResult(node, externalAddress, estimatedSize, time));
            }
            return;
        }
        
        context.setExternalAddress(externalAddress);
        context.addEstimatedRemoteSize(estimatedSize);
        
        setReturnValue(new PingResult(node, externalAddress, estimatedSize, time));
    }

    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
    	decrementActive();
        incrementFailures();
        
    	if (LOG.isInfoEnabled()) {
            LOG.info("Timeout: " + ContactUtils.toString(nodeId, dst));
        }
        
        if (giveUp()) {
            if (!hasActive()) {
                fireTimeoutException(nodeId, dst, message, time);
            } // else wait for the last response, timeout or error
        } else {
            pingNextAndThrowIfDone(createTimeoutException(nodeId, dst, message, time));
        }
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
    	decrementActive();
    	incrementFailures();
    	
        if(e instanceof SocketException && !giveUp()) {
            try {
                timeout(nodeId, dst, message, -1L);
            } catch (IOException err) {
                LOG.error("IOException", err);
                
                if (!pinger.hasNext()) {
                    setException(new DHTException(err));
                }
            }
        } else {
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }
    
    private void pingNextAndThrowIfDone(DHTException e) throws IOException {
        while(pinger.hasNext() && canMore()) {
            if (pinger.pingNext(context, this)) {
            	incrementActive();
            }
        }
        
        if (!hasActive()) {
            setException(e);
        }
    }
    
    private void decrementActive() {
        active--;
    }

    private void incrementActive() {
        active++;
    }

    private void incrementFailures() {
        failures++;
    }
    
    private boolean canMore() {
        return active < getParallelism();
    }
    
    private boolean hasActive() {
        return active > 0;
    }
    
    private boolean giveUp() {
        return (!pinger.hasNext() || failures >= getMaxParallelPingFailures());
    }
    
    /**
     * The PingIterator interfaces allows PingResponseHandler to
     * send ping requests to any type of contacts like SocketAddress
     * or an actual Contact.
     */
    public static interface PingIterator {
        
        /**
         * Returns true if there are more elements to ping.
         */
        public boolean hasNext();
        
        /**
         * Sends a ping to the next element.
         */
        public boolean pingNext(Context context, 
                PingResponseHandler responseHandler) throws IOException;
    }
}
