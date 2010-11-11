package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.util.Visitor;

/**
 * A class that ranks sources for a download. 
 * 
 * It uses a factory pattern to provide the best ranker based on system
 * conditions.
 */
public abstract class AbstractSourceRanker implements SourceRanker {

    /** The mesh handler to inform when altlocs fail */
    private MeshHandler meshHandler;
    
    /** A visitor to verify if RFDs are OK to use. */
    private Visitor<RemoteFileDescContext> rfdVisitor;

    public boolean addToPool(Collection<? extends RemoteFileDescContext> hosts) {
        boolean ret = false;
        for(RemoteFileDescContext host : hosts) {
            if (addToPool(host))
                ret = true;
        }
        return ret;
    }
    
    public abstract boolean addToPool(RemoteFileDescContext host);
	
    public abstract boolean hasMore();
    
    public abstract RemoteFileDescContext getBest();
    
    /**
     * @return the collection of hosts that can be shared with other rankers
     */
    public abstract Collection<RemoteFileDescContext> getShareableHosts();
    
    /** Returns true if there's atleast one usable host. */
    public synchronized boolean hasUsableHosts() {
        final long now = System.currentTimeMillis();        
        final Visitor<RemoteFileDescContext> rfdValidator = getRfdVisitor();
        final AtomicBoolean usable = new AtomicBoolean(false);
        visitSources(new Visitor<RemoteFileDescContext>() {
            @Override
            public boolean visit(RemoteFileDescContext context) {
                if((rfdValidator == null || rfdValidator.visit(context)) && !context.isBusy(now)) {
                    usable.set(true);
                    return false; // short-circuit, there's a valid one.
                }
                return true; // keep looking.
            }
        });
        return usable.get();
    }
    
    public synchronized int calculateWaitTime() {
        if (!hasMore()) {
            return 0;
        }
        
        // waitTime is in seconds
        final AtomicInteger waitTime = new AtomicInteger(Integer.MAX_VALUE);
        final long now = System.currentTimeMillis();
        final Visitor<RemoteFileDescContext> rfdValidator = getRfdVisitor();
        visitSources(new Visitor<RemoteFileDescContext>() {
            @Override
            public boolean visit(RemoteFileDescContext context) {
                if((rfdValidator == null || rfdValidator.visit(context)) && context.isBusy(now)) {
                    waitTime.set(Math.min(waitTime.get(), context.getWaitTime(now)));
                }
                return true;
            }
        });
        
        // Nothing was busy -- no wait time.
        if (waitTime.get() == Integer.MAX_VALUE) {
            return 0;
        } else {
            // waitTime was in seconds
            return (waitTime.get() * 1000);
        }
    }
    
    public synchronized void stop() {
        clearState();
        meshHandler = null;
    }
    
    protected void clearState() {}
    
    public synchronized void setMeshHandler(MeshHandler handler) {
        meshHandler = handler;
    }
    
    public synchronized MeshHandler getMeshHandler() {
        return meshHandler;
    }
    
    public synchronized void setRfdVisitor(Visitor<RemoteFileDescContext> rfdVisitor) {
        this.rfdVisitor = rfdVisitor;
    }
    
    public synchronized Visitor<RemoteFileDescContext> getRfdVisitor() {
        return rfdVisitor;
    }
    
    /**
     * Visits each source in the ranker with the visitor.
     * When the visitor returns false, iteration stops and this method returns false.
     * If all sources are visited (with the iterator returning true for each one),
     * then this method returns true.
     */
    abstract protected boolean visitSources(Visitor<RemoteFileDescContext> contextVisitor);
}
