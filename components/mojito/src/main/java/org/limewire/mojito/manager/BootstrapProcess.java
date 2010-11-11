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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.SyncWrapper;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.handler.response.FindNodeResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler;
import org.limewire.mojito.handler.response.PingResponseHandler.PingIterator;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.BootstrapResult.ResultType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTable.PurgeMode;
import org.limewire.mojito.settings.BootstrapSettings;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.RouteTableUtils;
import org.limewire.mojito.util.TimeAwareIterable;

/**
 * The BootstrapProcess controls the whole process of bootstrapping.
 * The sequence looks like this:
 * <pre>
 *     0) Find a Node that's connected to the DHT
 * +--->
 * |   1) Lookup own Node ID
 * |---2) If there are any Node ID collisions then check 'em,
 * |      change or Node ID is necessary and start over
 * |   3) Refresh all Buckets with prefixed random IDs
 * +---4) Prune RouteTable and restart if too many errors in #3
 *     5) Done
 * </pre>
 */     
 /* TODO: Step 3 can be done in parallel! It would speed up bootstrapping
 * a lot!
 */
class BootstrapProcess implements DHTTask<BootstrapResult> {

    private static final Log LOG = LogFactory.getLog(BootstrapProcess.class);
    
    private enum Status { BOOTSTRAPPING, RETRYING_BOOTSTRAP, FINISHED};
    
    private DHTFuture<BootstrapResult> exchanger;
    
    private final Context context;
    
    private final BootstrapManager manager;
    
    /** Serial tasks such as sending collision ping and finding nearest node */
    private final List<DHTTask<?>> tasks = new ArrayList<DHTTask<?>>();
    
    /** List of parallel workers executing paralellizable tasks */
    private final List<BootstrapWorker> workers = new ArrayList<BootstrapWorker>();
    
    private final SyncWrapper<Status> status = new SyncWrapper<Status>(null);
    
    private volatile boolean foundNewContacts = false;
    
    private int routeTableFailureCount;
    
    private boolean cancelled = false;
    
    private Iterator<KUID> bucketsToRefresh;
    
    private Contact node;
    
    private Set<? extends SocketAddress> dst;
    
    private long startTime = -1L;
    
    private final long waitOnLock;
    
    public BootstrapProcess(Context context, BootstrapManager manager, Contact node) {
        this.context = context;
        this.manager = manager;
        this.node = node;
        waitOnLock = BootstrapSettings.getWaitOnLock(true);
    }
    
    public BootstrapProcess(Context context, BootstrapManager manager, 
            Set<? extends SocketAddress> dst) {
        this.context = context;
        this.manager = manager;
        this.dst = dst;
        waitOnLock = BootstrapSettings.getWaitOnLock(false);
    }
    
    public long getWaitOnLockTimeout() {
        return waitOnLock;
    }

    public void start(DHTFuture<BootstrapResult> exchanger) {
        
        synchronized(status.getLock()) {
            if (status.get() != null)
                return;
            status.set(Status.BOOTSTRAPPING);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("starting bootstrap "+getPercentage(context.getRouteTable())+"% alive");
        
        this.exchanger = exchanger;

        startTime = System.currentTimeMillis();
        if (node == null) {
            findInitialContact();
        } else {
            findNearestNodes();
        }
    }
    
    private void findInitialContact() {
        DHTFuture<PingResult> c = new DHTValueFuture<PingResult>() {
            @Override
            public synchronized boolean setValue(PingResult value) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Found initial bootstrap Node: " + value);
                }
                
                if (super.setValue(value)) {
                    handlePong(value);
                    return true;
                }
                return false;
            }
            
            @Override
            public synchronized boolean setException(Throwable exception) {
                LOG.info("ExecutionException", exception);
                
                if (super.setException(exception)) {
                    exchanger.setException(exception);
                    return true;
                }
                return false;
            }
        };
        
        PingResponseHandler handler = new PingResponseHandler(context, 
                new PingIteratorFactory.SocketAddressPinger(dst));
        handler.setMaxErrors(0);
        start(handler, c);
    }
    
    private void handlePong(PingResult result) {
        this.node = result.getContact();
        findNearestNodes();
    }
    
    private void findNearestNodes() {
        DHTFuture<FindNodeResult> c = new DHTValueFuture<FindNodeResult>() {
            @Override
            public synchronized boolean setValue(FindNodeResult value) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Found nearest Nodes: " + value);
                }
                
                if (super.setValue(value)) {
                    handleNearestNodes(value);
                    return true;
                }
                return false;
            }
            
            @Override
            public synchronized boolean setException(Throwable exception) {
                LOG.info("ExecutionException", exception);
                
                if (super.setException(exception)) {
                    handleExecutionException(exception);
                    return true;
                }
                return false;
            }
        };

        FindNodeResponseHandler handler = new FindNodeResponseHandler(
                context, node, context.getLocalNodeID());
        start(handler, c);
    }
    
    void handleExecutionException(Throwable ee) {
        LOG.info("ExecutionException", ee);
        exchanger.setException(ee);
    }
    
    private void handleNearestNodes(FindNodeResult result) {
        Collection<? extends Contact> collisions = result.getCollisions();
        if (!collisions.isEmpty()) {
            checkCollisions(collisions);
        } else {
            Collection<? extends Contact> path = result.getPath();
            
            // Make sure we found some Nodes
            if (path == null || path.isEmpty()) {
                bootstrapped(false);
                
            // But other than our local Node
            } else if (path.size() == 1 
                    && path.contains(context.getLocalNode())) {
                bootstrapped(false);
            
            // Great! Everything is fine and continue with
            // refreshing/filling up the RouteTable by doing
            // lookups for random IDs
            } else {
                refreshAllBuckets();
            }
        }
    }
    
    private void checkCollisions(Collection<? extends Contact> collisions) {
        DHTFuture<PingResult> c = new DHTValueFuture<PingResult>() {
            @Override
            public synchronized boolean setValue(PingResult value) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(context.getLocalNode() + " collides with " + value.getContact());
                }
                
                if (super.setValue(value)) {
                    handleCollision(value);
                    return true;
                }
                return false;
            }
            
            @Override
            public synchronized boolean setException(Throwable exception) {
                LOG.info("ExecutionException", exception);
                
                if (super.setException(exception)) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof DHTTimeoutException) {
                        // Ignore, everything is fine! Nobody did respond
                        // and we can keep our Node ID which is good!
                        // Continue with finding random Node IDs
                        refreshAllBuckets();

                    } else {
                        exchanger.setException(exception);
                    }
                    
                    return true;
                }
                return false;
            }
        };

        Contact sender = ContactUtils.createCollisionPingSender(context.getLocalNode());
        PingIterator pinger = new PingIteratorFactory.CollisionPinger(
                context, sender, org.limewire.collection.CollectionUtils.toSet(collisions));
        
        PingResponseHandler handler 
            = new PingResponseHandler(context, sender, pinger);
        start(handler, c);
    }
    
    private void handleCollision(PingResult result) {
        // Change our Node ID
        context.changeNodeID();
        
        // Start over!
        findNearestNodes();
    }
    
    /**
     * Refresh all Buckets (Phase two)
     * 
     * When we detect that the routing table is stale, we purge it
     * and start the bootstrap all over again. 
     * A stale routing table can be detected by a high number of failures
     * during the lookup (alive hosts to expected result set size ratio).
     * Note: this only applies to routing tables with more than 1 buckets,
     * i.e. routing tables that have more than k nodes.
     */
    private void refreshAllBuckets() {
        routeTableFailureCount = 0;
        foundNewContacts = false;
        
        Collection<KUID> bucketIds = getBucketsToRefresh();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Buckets to refresh: " + CollectionUtils.toString(bucketIds));
        }
        
        bucketsToRefresh = new TimeAwareIterable<KUID>(
                BootstrapSettings.BOOTSTRAP_TIMEOUT.getValue(),
                bucketIds).iterator();
        
        for (int i = 0; i < BootstrapSettings.BOOTSTRAP_WORKERS.getValue(); i++) {
            BootstrapWorker worker = new BootstrapWorker(context, this);
            synchronized(this) {
                workers.add(worker);
            }
            context.getDHTExecutorService().execute(worker);
        }
    }
    
    private Collection<KUID> getBucketsToRefresh() {
        List<KUID> bucketIds = org.limewire.collection.CollectionUtils.toList(
                context.getRouteTable().getRefreshIDs(true));
        Collections.reverse(bucketIds);
        return bucketIds;
    }
    
    
    KUID getNextBucket() {
        synchronized(this) {
            if (cancelled)
                return null;
            synchronized(bucketsToRefresh) {
                if (bucketsToRefresh.hasNext()) 
                    return bucketsToRefresh.next();
            }
        }
        
        boolean determinate = false;
        synchronized(status.getLock()) {
            if (status.get() != Status.FINISHED) {
                status.set(Status.FINISHED);
                determinate = true;
            }
        }
        
        if (determinate)
            determinateIfBootstrapped();
        return null;
    }
    
    private void handleStaleRouteTable() {
        LOG.debug("handling stale route table");
        // The RouteTable is stale! Remove all non-alive Contacts,
        // rebuild the RouteTable and start over!
        context.getRouteTable().purge(
                PurgeMode.DROP_CACHE,
                PurgeMode.PURGE_CONTACTS, 
                PurgeMode.MERGE_BUCKETS,
                PurgeMode.STATE_TO_UNKNOWN);
        
        // And Start over!
        findNearestNodes();
    }
    
    /**
     * Notification that a refresh operation has completed.
     * @param failures how many of the pinged nodes failed to respond.
     * @param newContacts true if new contacts were discovered.
     */
    void refreshDone(int failures, boolean newContacts) {
            
        foundNewContacts |= newContacts;
        
        boolean retry = false;
        boolean terminate = false;
        
        synchronized(status.getLock()) {
            boolean highFailures = false;
            switch(status.get()) {
            case BOOTSTRAPPING :
            case RETRYING_BOOTSTRAP :
                routeTableFailureCount += failures;
                if (routeTableFailureCount >= BootstrapSettings.MAX_BOOTSTRAP_FAILURES.getValue()) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("high failures: "+routeTableFailureCount);
                    highFailures = true;
                }
            }

            /*
             * at this point we can either retry bootstrapping or terminate it.
             */
            if (highFailures) {
                switch(status.get()) {
                case BOOTSTRAPPING :
                    routeTableFailureCount = 0;
                    status.set(Status.RETRYING_BOOTSTRAP);
                    retry = true;
                    break;
                case RETRYING_BOOTSTRAP :
                    terminate = true;
                    status.set(Status.FINISHED);
                }
            }
        }
        
        if (retry)
            handleStaleRouteTable();
        if (terminate) {
            cancel();
            determinateIfBootstrapped();
        }
    }
    
    /**
     * Determines whether or not we're bootstrapped.
     */
    private void determinateIfBootstrapped() {
        
        boolean bootstrapped = false;
        float alive = purgeAndGetPercenetage();
        
        // Check what percentage of the Contacts are alive
        if (alive >= BootstrapSettings.IS_BOOTSTRAPPED_RATIO.getValue()) {
            bootstrapped = true;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Bootstrapped: " + alive + " >= " 
                    + BootstrapSettings.IS_BOOTSTRAPPED_RATIO.getValue() 
                    + " -> " + bootstrapped);
        }
        
        bootstrapped(bootstrapped);
    }
    
    private float purgeAndGetPercenetage() {
        RouteTable routeTable = context.getRouteTable();
        synchronized (routeTable) {
            routeTable.purge(PurgeMode.DROP_CACHE,
                            PurgeMode.PURGE_CONTACTS, 
                            PurgeMode.MERGE_BUCKETS);
            
            return getPercentage(routeTable);
        }
    }
    
    private float getPercentage(RouteTable table) {
        return RouteTableUtils.getPercentageOfAliveContacts(table);
   }
    
    private void bootstrapped(boolean bootstrapped) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Finishing bootstrapping: " + bootstrapped);
        }
        
        ResultType type = ResultType.BOOTSTRAP_FAILED;
        if (bootstrapped) {
            manager.setBootstrapped(true);
            type = ResultType.BOOTSTRAP_SUCCEEDED;
        }
        
        long time = System.currentTimeMillis() - startTime;
        
        exchanger.setValue(new BootstrapResult(node, time, type));
    }
    
    private <T> void start(DHTTask<T> task, DHTFuture<T> c) {
        boolean doStart = false;
        synchronized (this) {
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
        status.set(Status.FINISHED);
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Canceling BootstrapProcess");
        }
        
        List<DHTTask<?>> copy = null;
        List<BootstrapWorker> workerCopy = null;
        synchronized (this) {
            if (!cancelled) {
                copy = new ArrayList<DHTTask<?>>(tasks);
                tasks.clear();
                workerCopy = new ArrayList<BootstrapWorker>(workers);
                workers.clear();
                cancelled = true;
            }
        }

        if (copy != null) {
            for (DHTTask<?> task : copy) 
                task.cancel();
        }
        if (workerCopy != null) {
            for (BootstrapWorker worker : workerCopy) 
                worker.shutdown();
        }
    }
}
