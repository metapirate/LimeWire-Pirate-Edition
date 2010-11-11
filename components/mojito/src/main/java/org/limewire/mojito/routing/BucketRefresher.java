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

package org.limewire.mojito.routing;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.manager.BootstrapManager;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.BucketRefresherSettings;
import org.limewire.mojito.settings.KademliaSettings;

/**
 * The BucketRefresher goes in periodic intervals through all Buckets
 * and refreshes every Bucket that hasn't been touched for a certain
 * amount of time.
 */
public class BucketRefresher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(BucketRefresher.class);
    
    private final Context context;
    
    private final RefreshTask refreshTask = new RefreshTask();
    
    private ScheduledFuture future;
    
    public BucketRefresher(Context context) {
        this.context = context;
    }
    
    /**
     * Starts the BucketRefresher.
     */
    public void start() {
        synchronized (refreshTask) {
            if (future == null) {
                long delay = BucketRefresherSettings.BUCKET_REFRESHER_DELAY.getValue();
                long initialDelay = delay;
                
                if (BucketRefresherSettings.UNIFORM_BUCKET_REFRESH_DISTRIBUTION.getValue()) {
                    initialDelay = delay + (long)(delay * Math.random());
                }
                
                future = context.getDHTExecutorService()
                    .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * Stops the BucketRefresher.
     */
    public void stop() {
        synchronized (refreshTask) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            refreshTask.stop();
        }
    }
    
    @Override
    public void run() {
        synchronized (refreshTask) {
            
            if(LOG.isTraceEnabled()) {
                LOG.trace("Random bucket refresh");
            }
            
            // Running the BucketRefresher w/o being bootstrapped is
            // pointless. Try to bootstrap from the RouteTable if 
            // it's possible.
            
            BootstrapManager bootstrapManager = context.getBootstrapManager();
            synchronized (bootstrapManager) {
                if (!bootstrapManager.isBootstrapped()) {
                    if (!bootstrapManager.isBootstrapping()) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Bootstrap " + context.getName());
                        }
                        
                        // If we are not bootstrapped and have some 
                        // Nodes in our RouteTable, try to bootstrap
                        // from the RouteTable
                        
                        DHTFutureAdapter<PingResult> listener 
                                = new DHTFutureAdapter<PingResult>() {
                            @Override
                            protected void operationComplete(FutureEvent<PingResult> event) {
                                if (event.getType() == Type.SUCCESS) {
                                    context.bootstrap(event.getResult().getContact());
                                }
                            }
                        };
                        
                        DHTFuture<PingResult> future = context.findActiveContact();
                        future.addFutureListener(listener);
                        
                    } else {
                        if (LOG.isInfoEnabled()) {
                            LOG.info(context.getName() + " is bootstrapping");
                        }
                    }
                    
                    // In any case exit here!
                    return;
                }
            }
            
            
            // Refresh but make sure the task from the previous
            // run() call has finished as we don't want to run
            // refresher tasks in parallel
            if (refreshTask.isDone()) {
                
                long pingNearest = BucketRefresherSettings.BUCKET_REFRESHER_PING_NEAREST.getValue();
                if (pingNearest > 0L) {
                    Collection<Contact> nodes = context.getRouteTable().select(
                            context.getLocalNodeID(), 
                            KademliaSettings.REPLICATION_PARAMETER.getValue(), 
                            SelectMode.ALL);
                    
                    for (Contact node : nodes) {
                        if (context.isLocalNode(node)) {
                            continue;
                        }
                        
                        // Ping only Nodes that haven't responded/contacted us
                        // for a certain amount of time
                        long timeStamp = node.getTimeStamp();
                        if ((System.currentTimeMillis() - timeStamp) >= pingNearest) {
                            context.ping(node);
                        }
                    }
                }
                
                refreshTask.refresh();
            }
        }
    }
    
    /**
     * The RefreshTask iterates one-by-one through a List of KUIDs
     * and does a lookup for the ID. Every time a lookup finishes it
     * starts a new lookup for the next ID until all KUIDs have been
     * looked up.
     */
    private class RefreshTask extends DHTFutureAdapter<FindNodeResult> {
        
        private Iterator<KUID> bucketIds = null;
        
        private DHTFuture<FindNodeResult> future = null;
        
        /**
         * Returns whether or not the refresh task has 
         * finished (initial state is true)
         */
        public synchronized boolean isDone() {
            return bucketIds == null || !bucketIds.hasNext();
        }
        
        /**
         * Stops the RefreshTask.
         */
        public synchronized void stop() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            
            bucketIds = null;
        }
        
        /**
         * Starts the refresh.
         */
        public synchronized boolean refresh() {
            Collection<KUID> list = context.getRouteTable().getRefreshIDs(false);
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " has " + list.size() + " Buckets to refresh");
            }
            
            bucketIds = list.iterator();
            return next();
        }
        
        /**
         * Lookup the next KUID.
         */
        private synchronized boolean next() {
            if (isDone()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(context.getName() + " finished Bucket refreshes");
                }
                return false;
            }
            
            KUID lookupId = bucketIds.next();
            future = context.lookup(lookupId);
            future.addFutureListener(this);
            
            if (LOG.isInfoEnabled()) {
                LOG.info(context.getName() + " started a Bucket refresh lookup for " + lookupId);
            }
            
            return true;
        }
        
        @Override
        protected void operationComplete(FutureEvent<FindNodeResult> event) {
            switch (event.getType()) {
                case SUCCESS:
                    handleFutureSuccess(event.getResult());
                    break;
                case CANCELLED:
                    handleCancellationException();
                    break;
                case EXCEPTION:
                    handleExecutionException(event.getException());
                    break;
            }
        }

        private void handleFutureSuccess(FindNodeResult result) {
            if (LOG.isInfoEnabled()) {
                LOG.info(result);
            }
            
            if (!next()) {
                stop();
            }
        }
        
        private void handleExecutionException(ExecutionException e) {
            LOG.error("ExecutionException", e);
            
            if (!next()) {
                stop();
            }
        }
        
        private void handleCancellationException() {
            LOG.trace("Cancelled");
            stop();
        }
    }
}
