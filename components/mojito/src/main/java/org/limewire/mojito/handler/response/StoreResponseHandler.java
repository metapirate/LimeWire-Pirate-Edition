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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.mojito.messages.StoreResponse;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.security.SecurityToken;

/**
 * The StoreResponseHandler class handles/manages storing of
 * DHTValues on remote Nodes.
 */
public class StoreResponseHandler extends AbstractResponseHandler<StoreResult> {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    private final Collection<? extends DHTValueEntity> entities;
    
    /**
     * A list of all StoreProcesses.
     */
    private final List<StoreProcess> processes = new ArrayList<StoreProcess>();
    
    /** 
     * An Iterator of StoreProcesses (see processList). 
     */
    private Iterator<StoreProcess> toProcess = null;
    
    /** 
     * Map of currently active StoreProcesses (see parallelism). 
     */
    private Map<KUID, StoreProcess> activeProcesses = new HashMap<KUID, StoreProcess>();
    
    /** 
     * The number of parallel stores. 
     */
    private final int parallelism = StoreSettings.PARALLEL_STORES.getValue();
    
    public StoreResponseHandler(Context context, 
            Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> path, 
                    Collection<? extends DHTValueEntity> entities) {
        super(context);
        
        this.entities = entities;
        
        if (path.size() > KademliaSettings.REPLICATION_PARAMETER.getValue()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Path is longer than K: " + path.size() 
                        + " > " + KademliaSettings.REPLICATION_PARAMETER.getValue());
            }
        }
        
        for (Entry<? extends Contact, ? extends SecurityToken> entry : path) {
            Contact node = entry.getKey();
            SecurityToken securityToken = entry.getValue();
            
            if (context.isLocalNode(node)) {
                processes.add(new LocalStoreProcess(node, securityToken, entities));
            } else {
                processes.add(new RemoteStoreProcess(node, securityToken, entities));
            }
        }
    }
    
    @Override
    public void start() throws DHTException {
        toProcess = processes.iterator();
        sendNextAndExitIfDone();
    }
    
    @Override
    protected void response(ResponseMessage message, long time) throws IOException {
        
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        
        StoreProcess process = activeProcesses.get(nodeId);
        if (process != null) {
            if (process.response(message)) {
                activeProcesses.remove(nodeId);
            }
        }
        
        sendNextAndExitIfDone();
    }
    
    @Override
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        
        StoreProcess process = activeProcesses.get(nodeId);
        if (process != null) {
            if (process.timeout(message, time)) {
                activeProcesses.remove(nodeId);
            }
        }
        
        sendNextAndExitIfDone();
    }
    
    @Override
    protected void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e) {
        
        StoreProcess process = activeProcesses.get(nodeId);
        if (process != null) {
            if (process.error(message, e)) {
                activeProcesses.remove(nodeId);
            }
        }
        
        sendNextAndExitIfDone();
    }

    /**
     * Tries to maintain parallel store requests and fires
     * an event if storing is done.
     */
    private void sendNextAndExitIfDone() {
        while(activeProcesses.size() < parallelism && toProcess.hasNext()) {
            StoreProcess process = toProcess.next();
            
            try {
                boolean done = process.store();
                if (!done) {
                    Contact node = process.getContact();
                    activeProcesses.put(node.getNodeID(), process);
                }
            } catch (IOException err) {
                process.setIOException(err);
                process.finish();
                
                LOG.error("IOException", err);
            }
        }
        
        // No active processes left? We're done!
        if (activeProcesses.isEmpty()) {
            done();
        }
    }
    
    /**
     * Called if all values were stored.
     */
    private void done() {
        
        Map<Contact, Collection<StoreStatusCode>> map 
            = new LinkedHashMap<Contact, Collection<StoreStatusCode>>();
        
        Map<KUID, Collection<Contact>> locations 
            = new HashMap<KUID, Collection<Contact>>();
        
        for (StoreProcess process : processes) {
            Contact node = process.getContact();
            Collection<StoreStatusCode> statusCodes 
                = process.getStoreStatusCodes();
            
            map.put(node, statusCodes);
            
            for (StoreStatusCode statusCode : statusCodes) {
                if (!statusCode.getStatusCode().equals(StoreResponse.OK)) {
                    continue;
                }
                
                KUID secondaryKey = statusCode.getSecondaryKey();
                Collection<Contact> nodes = locations.get(secondaryKey);
                if (nodes == null) {
                    nodes = new ArrayList<Contact>();
                    locations.put(secondaryKey, nodes);
                }
                
                nodes.add(node);
            }
        }
        
        if (processes.size() == 1) {
            StoreProcess process = processes.get(0);
            
            IOException exception = process.getIOException();
            long timeout = process.getTimeout();
            
            if (exception != null) {
                setException(new DHTException(exception));
                return;
                
            } else if (timeout != -1L) {
                Contact node = process.getContact();
                KUID nodeId = node.getNodeID();
                SocketAddress dst = node.getContactAddress();
                fireTimeoutException(nodeId, dst, null, timeout);
                return;
            }
        }
        
        StoreResult result = new StoreResult(map, entities);
        setReturnValue(result);
    }
    
    /**
     * A StoreProcess process manages storing of n values at
     * a single Node.
     */
    private abstract class StoreProcess {
        
        private final Contact node;
        
        private final SecurityToken securityToken;
        
        private final Collection<? extends DHTValueEntity> entities;
        
        private final Iterator<? extends DHTValueEntity> iterator;
        
        private final Collection<StoreStatusCode> codes = new ArrayList<StoreStatusCode>();
        
        private IOException exception;
        
        private long timeout = -1L;
        
        private StoreProcess(Contact node, SecurityToken securityToken, 
                Collection<? extends DHTValueEntity> entities) {
            this.node = node;
            this.securityToken = securityToken;
            this.entities = entities;
            this.iterator = entities.iterator();
        }
        
        /**
         * The Contact where we're storing the values.
         */
        public Contact getContact() {
            return node;
        }

        /**
         * The SecurityToken we got from the Contact.
         */
        public SecurityToken getSecurityToken() {
            return securityToken;
        }
        
        /**
         * List of values we're storing at the Node.
         */
        public Collection<? extends DHTValueEntity> getEntities() {
            return entities;
        }
        
        /**
         * Returns true if there are more elements to sore.
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        /**
         * Returns the next element to store.
         */
        public DHTValueEntity next() {
            return iterator.next();
        }
        
        /**
         * Adds the StoreStatusCode to an internal list of StoreStatusCodes.
         */
        public void addStoreStatusCode(StoreStatusCode code) {
            codes.add(code);
        }
        
        /**
         * Returns all StoreStatusCodes.
         */
        public Collection<StoreStatusCode> getStoreStatusCodes() {
            return codes;
        }
        
        /**
         * Sets an IOException that may occurred.
         */
        public void setIOException(IOException exception) {
            this.exception = exception;
        }
        
        /**
         * Returns an IOException that may occurred.
         */
        public IOException getIOException() {
            return exception;
        }
        
        /**
         * Sets a timeout that may occurred.
         */
        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
        
        /**
         * Returns a timeout that may occurred.
         */
        public long getTimeout() {
            return timeout;
        }
        
        /**
         * Finishes the lookup process.
         */
        public void finish() {
            while(hasNext()) {
                DHTValueEntity entity = next();
                addStoreStatusCode(new StoreStatusCode(entity, StoreResponse.ERROR));
            }
        }
        
        /**
         * Starts the StoreProcess. Returns true if storing is done.
         */
        public abstract boolean store() throws IOException;
        
        /**
         * Handles a store response. Returns true if storing is done.
         */
        public abstract boolean response(ResponseMessage msg) throws IOException;

        /**
         * Handles a store error. Returns true if storing is done.
         */
        public abstract boolean error(RequestMessage msg, IOException err);
        
        /**
         * Handles a store timeout. Returns true if storing is done.
         */
        public abstract boolean timeout(RequestMessage msg, long timeout) throws IOException;
    }
    
    /**
     * Stores values at the local Node.
     */
    private class LocalStoreProcess extends StoreProcess {
        
        private LocalStoreProcess(Contact node, SecurityToken securityToken, 
                Collection<? extends DHTValueEntity> entities) {
            super(node, securityToken, entities);
        }
        
        @Override
        public boolean store() throws IOException {
            Database database = context.getDatabase();
            while(hasNext()) {
                DHTValueEntity entity = next();
                boolean stored = database.store(entity);
                if (stored) {
                    addStoreStatusCode(new StoreStatusCode(entity, StoreResponse.OK));
                } else {
                    addStoreStatusCode(new StoreStatusCode(entity, StoreResponse.ERROR));
                }
            }
            return true;
        }
        
        @Override
        public boolean response(ResponseMessage msg) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean error(RequestMessage msg, IOException err) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean timeout(RequestMessage msg, long time) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Stores values at a remote Node.
     */
    private class RemoteStoreProcess extends StoreProcess {
        
        private DHTValueEntity currentEntity = null;
        
        private RemoteStoreProcess(Contact node, SecurityToken securityToken, 
                Collection<? extends DHTValueEntity> entities) {
            super(node, securityToken, entities);
        }

        @Override
        public boolean store() throws IOException {
            currentEntity = null;
            
            // Nothing left? We're done!
            if (!hasNext()) {
                return true;
            }
            
            // Get the next value and try to store it
            currentEntity = next();
            StoreRequest request = context.getMessageHelper()
                .createStoreRequest(getContact().getContactAddress(), 
                        getSecurityToken(), Collections.singleton(currentEntity));
            
            context.getMessageDispatcher().send(getContact(), 
                    request, StoreResponseHandler.this);
            
            return false;
        }
        
        @Override
        public boolean response(ResponseMessage msg) throws IOException {
            StoreResponse response = (StoreResponse)msg;
            Collection<StoreStatusCode> codes = response.getStoreStatusCodes();
            
            // We store one value per request! If the remote Node
            // sends us a different number of StoreStatusCodes back
            // then there is something wrong!
            if (codes.size() != 1) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(getContact() + " sent a wrong number of StoreStatusCodes: " + codes);
                }
                
                // Exit
                finish();
                return true;
            }
            
            // The returned StoreStatusCode must have the same primary and
            // secondaryKeys as the value we requested to store.
            StoreStatusCode code = codes.iterator().next();
            if (!code.isFor(currentEntity)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(getContact() + " sent a wrong [" + code + "] for " + currentEntity
                            + "\n" + CollectionUtils.toString(getEntities()));
                }
                
                // Exit
                finish();
                return true;
            }
            
            // Store next value
            return store();
        }

        @Override
        public boolean error(RequestMessage msg, IOException err) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Couldn't store " + currentEntity + " at " + getContact(), err);
            }
            
            setIOException(err);
            addStoreStatusCode(new StoreStatusCode(currentEntity, StoreResponse.ERROR));
            
            try {
                return store();
            } catch (IOException iox) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("IOException", iox);
                }
                
                // Exit
                finish();
                return true;
            }
        }
        
        @Override
        public boolean timeout(RequestMessage msg, long timeout) throws IOException {
            if (LOG.isInfoEnabled()) {
                LOG.info("Couldn't store " + currentEntity + " at " + getContact());
            }
            
            setTimeout(timeout);
            addStoreStatusCode(new StoreStatusCode(currentEntity, StoreResponse.ERROR));
            return store();
        }

        @Override
        public void finish() {
            if (currentEntity != null) {
                addStoreStatusCode(new StoreStatusCode(currentEntity, StoreResponse.ERROR));
                currentEntity = null;
            }
            super.finish();
        }
    }
}
