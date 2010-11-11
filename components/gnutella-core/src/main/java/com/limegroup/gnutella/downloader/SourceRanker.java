package com.limegroup.gnutella.downloader;

import java.util.Collection;

import org.limewire.util.Visitor;

public interface SourceRanker {

    /**
     * @param hosts a collection of remote hosts to rank
     * @return if we didn't know about at least one of the hosts
     */
    boolean addToPool(Collection<? extends RemoteFileDescContext> hosts);

    /**
     * @param host the host that the ranker should consider
     * @return if we did not already know about this host
     */
    boolean addToPool(RemoteFileDescContext host);

    /**
     * Returns true if the ranker has any more potential sources. This will
     * return true even if the sources are busy or not valid to be used.
     */
    boolean hasMore();

    /**
     * @return the source that should be tried next or <code>null</code>
     * if there is none
     */
    RemoteFileDescContext getBest();

    /**
     * @return the ranker knows about at least one potential source that is
     * not currently busy
     */
    boolean hasUsableHosts();

    /**
     * @return how much time we should wait before at least one host
     * will become non-busy
     */
    int calculateWaitTime();

    /**
     * Stops the ranker, clearing any state.
     */
    void stop();

    /** Sets the Mesh handler if any. */
    void setMeshHandler(MeshHandler handler);

    /** 
     * @return the Mesh Handler, if any
     */
    MeshHandler getMeshHandler();

    /**
     * @return the collection of hosts that can be shared with other rankers
     */
    Collection<RemoteFileDescContext> getShareableHosts();
    
    /** Sets a visitor that can verify if RFDs are OK. */
    void setRfdVisitor(Visitor<RemoteFileDescContext> rfdVisitor);
    
    /** Gets the current RFD visitor. */
    Visitor<RemoteFileDescContext> getRfdVisitor();
}