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

package org.limewire.mojito.manager;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTBackendException;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.handler.response.AbstractResponseHandler;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.handler.response.FindValueResponseHandler;
import org.limewire.mojito.handler.response.LookupResponseHandler;
import org.limewire.mojito.handler.response.StoreResponseHandler;
import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.messages.SecurityTokenProvider;
import org.limewire.mojito.result.LookupResult;
import org.limewire.mojito.result.Result;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.ContactsScrubber;
import org.limewire.mojito.util.EntityUtils;
import org.limewire.mojito.util.EntryImpl;
import org.limewire.security.SecurityToken;

/**
 * Controls the whole process of storing
 * values along a path. There are 3 different options:
 * <pre>
 * 1) No Contact is specified.
 *    The StoreProcess will do a full lookup for the k-closest 
 *    Nodes and will store the value(s) along the path.
 *    
 * 2) A single Contact is specified but a SecurityToken is missing.
 *    The StoreProcess will try to obtain the SecurityToken
 *    and will store the value(s) at the given Node.
 *    
 * 3) A single Contact is specified and a SecurityToken is not required.
 *    This is a special case and will never occur in reality.
 *    It's meant for DHT implementations (say you want to create a
 *    DHT is a closed environment where SecurityTokens make little
 *    sense).
 * </pre>   
 * Regarding values and the first case, it's possible to store multiple
 * values in a batch but all values must have the same primary key.
 */
class StoreProcess implements DHTTask<StoreResult> {
    
    //private static final Log LOG = LogFactory.getLog(StoreProcess.class);
    
    private final Context context;
    
    private final List<DHTTask<?>> tasks = new ArrayList<DHTTask<?>>();
    
    private boolean cancelled = false;
    
    private DHTFuture<StoreResult> future;
    
    private final KUID primaryKey;
    
    private final Entry<? extends Contact, ? extends SecurityToken> node;
    
    private final Collection<? extends DHTValueEntity> entities;
    
    private final long waitOnLock;
    
    public StoreProcess(Context context, Collection<? extends DHTValueEntity> entities) {
        this(context, null, entities);
    }
    
    public StoreProcess(Context context, Entry<? extends Contact, ? extends SecurityToken> node,
            Collection<? extends DHTValueEntity> entities) {
        
        this.context = context;
        this.entities = entities;
        this.node = node;
        
        if (node != null && node.getKey() == null) {
            throw new IllegalArgumentException("Contact is null");
        }
        
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("No Values to store");
        }
        
        // If Node is null it means we've to search for the
        // k-closest Nodes first which only works if all
        // DHTValueEntities have the same primary key!
        if (node == null) {
            this.primaryKey = EntityUtils.getPrimaryKey(entities);
            if (primaryKey == null) {
                throw new IllegalArgumentException("All DHTValues must have the same primary key");
            }
        } else {
            this.primaryKey = null;
        }
        
        this.waitOnLock = StoreSettings.getWaitOnLock(node != null);
    }

    public long getWaitOnLockTimeout() {
        return waitOnLock;
    }

    public void start(DHTFuture<StoreResult> future) {
        
        this.future = future;
        
        // Regular store operation
        if (node == null) {
            findNearestNodes();
            
        // Get the SecurityToken and store the value(s) 
        // at the given Node 
        } else if (node.getValue() == null
                && StoreSettings.STORE_REQUIRES_SECURITY_TOKEN.getValue()) {
            doGetSecurityToken();
            
        } else {
            doStoreOnPath(Collections.singleton(node));
        }
    }
    
    private void findNearestNodes() {
        DHTFuture<LookupResult> c = new DHTValueFuture<LookupResult>() {
            @Override
            public synchronized boolean setValue(LookupResult value) {
                if (super.setValue(value)) {
                    handleNearestNodes(value);
                    return true;
                }
                return false;
            }
            
            @Override
            public synchronized boolean setException(Throwable exception) {
                if (super.setException(exception)) {
                    future.setException(exception);
                    return true;
                }
                return false;
            }
        };

        // Do a lookup for the k-closest Nodes where we're
        // going to store the value
        LookupResponseHandler<LookupResult> handler = createLookupResponseHandler();
        
        // Use only alive Contacts from the RouteTable
        handler.setSelectAliveNodesOnly(true);
        
        start(handler, c);
    }
    
    private void handleNearestNodes(LookupResult value) {
        doStoreOnPath(value.getEntryPath());
    }
    
    private void doGetSecurityToken() {
        DHTFuture<GetSecurityTokenResult> c = new DHTValueFuture<GetSecurityTokenResult>() {
            @Override
            public synchronized boolean setValue(GetSecurityTokenResult value) {
                if (super.setValue(value)) {
                    handleSecurityToken(value);
                    return true;
                }
                return false;
            }
            
            @Override
            public synchronized boolean setException(Throwable exception) {
                if (super.setException(exception)) {
                    future.setException(exception);
                    return true;
                }
                return false;
            }
        };

        GetSecurityTokenHandler handler 
            = new GetSecurityTokenHandler(context, node.getKey());
        
        start(handler, c);
    }
    
    private void handleSecurityToken(GetSecurityTokenResult result) {
        SecurityToken securityToken = result.getSecurityToken();
        if (securityToken == null) {
            future.setException(new ExecutionException(
                            new DHTException("Could not get SecurityToken from " + node)));
        } else {
            Entry<Contact, SecurityToken> entry 
                = new EntryImpl<Contact, SecurityToken>(node.getKey(), securityToken);
            
            doStoreOnPath(Collections.singleton(entry));
        }
    }
    
    private void doStoreOnPath(Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> path) {
        // And store the values along the path
        StoreResponseHandler handler 
            = new StoreResponseHandler(context, path, entities);
        start(handler, future);
    }
    
    private <T> void start(DHTTask<T> task, DHTFuture<T> c) {
        boolean doStart = false;
        synchronized (tasks) {
            if (!cancelled) {
                tasks.add(task);
                doStart = true;
            }
        }
        
        if (doStart) {
            task.start(c);
        }
    }
    
    public void cancel() {
        List<DHTTask<?>> copy = null;
        synchronized (tasks) {
            if (!cancelled) {
                copy = new ArrayList<DHTTask<?>>(tasks);
                tasks.clear();
                cancelled = true;
            }
        }

        if (copy != null) {
            for (DHTTask<?> task : copy) {
                task.cancel();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private LookupResponseHandler<LookupResult> createLookupResponseHandler() {
        LookupResponseHandler<? extends LookupResult> handler = null;
        if (LookupSettings.FIND_NODE_FOR_SECURITY_TOKEN.getValue()) {
            handler = new FindNodeResponseHandler(context, primaryKey);
        } else {
            EntityKey lookupKey = EntityKey.createEntityKey(primaryKey, DHTValueType.ANY);
            handler = new FindValueResponseHandler(context, lookupKey);
        }
        return (LookupResponseHandler<LookupResult>)handler;
    }
    
    /**
     * GetSecurityTokenHandler tries to get the SecurityToken of a Node
     */
    private static class GetSecurityTokenHandler extends AbstractResponseHandler<GetSecurityTokenResult> {
        
        private static final Log LOG = LogFactory.getLog(GetSecurityTokenHandler.class);
        
        private final Contact node;
        
        private GetSecurityTokenHandler(Context context, Contact node) {
            super(context);
            this.node = node;
        }

        @Override
        protected void start() throws DHTException {
            RequestMessage request = createLookupRequest();
            
            try {
                context.getMessageDispatcher().send(node, request, this);
            } catch (IOException err) {
                throw new DHTException(err);
            }
        }
        
        private RequestMessage createLookupRequest() {
            if (LookupSettings.FIND_NODE_FOR_SECURITY_TOKEN.getValue()) {
                return context.getMessageHelper()
                        .createFindNodeRequest(node.getContactAddress(), node.getNodeID());
            } else {
                Collection<KUID> noKeys = Collections.emptySet();
                return context.getMessageHelper()
                    .createFindValueRequest(node.getContactAddress(), node.getNodeID(), noKeys, DHTValueType.ANY);
            }
        }
        
        @Override
        protected void response(ResponseMessage message, long time) throws IOException {
            
            if (message instanceof FindNodeResponse) {
                FindNodeResponse response = (FindNodeResponse)message;
                
                Contact sender = response.getContact();
                Collection<? extends Contact> nodes = response.getNodes();
                
                if (!nodes.isEmpty()) {
                    
                    ContactsScrubber scrubber = ContactsScrubber.scrub(
                            context, sender, nodes, 
                            LookupSettings.CONTACTS_SCRUBBER_REQUIRED_RATIO.getValue());
                    
                    if (scrubber.isValidResponse()) {
                        // We did a FIND_NODE lookup use the info
                        // to fill/update our routing table
                        for(Contact node : scrubber.getScrubbed()) {
                            assert (node.isAlive() == false);
                            context.getRouteTable().add(node);
                        }
                    }
                }
            }
            
            SecurityToken securityToken = null;
            if (message instanceof SecurityTokenProvider) {
                securityToken = ((SecurityTokenProvider)message).getSecurityToken();
            }
            
            setReturnValue(new GetSecurityTokenResult(securityToken));
        }
        
        @Override
        protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
            fireTimeoutException(nodeId, dst, message, time);
        }

        @Override
        protected void error(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Getting the SecurityToken from " + ContactUtils.toString(nodeId, dst) + " failed", e);
            }
            
            setException(new DHTBackendException(nodeId, dst, message, e));
        }
    }

    /**
     * Returned by GetSecurityTokenHandler. Used only internally!
     */
    private static class GetSecurityTokenResult implements Result {
        
        private final SecurityToken securityToken;
        
        public GetSecurityTokenResult(SecurityToken securityToken) {
            this.securityToken = securityToken;
        }
        
        public SecurityToken getSecurityToken() {
            return securityToken;
        }
    }
}
