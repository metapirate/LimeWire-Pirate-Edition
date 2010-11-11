package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.Collections;

import org.limewire.collection.MultiCollection;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.util.Visitor;

import com.limegroup.gnutella.PushEndpoint;

/**
 * Delegates Gnutella rfds to {@link PingRanker} and non-gnutella rfds
 * to the {@link LegacyRanker}.
 */
public class FriendsFirstSourceRanker extends AbstractSourceRanker {

    private final LegacyRanker legacyRanker = new LegacyRanker();
    
    private final PingRanker pingRanker;
    
    public FriendsFirstSourceRanker(PingRanker pingRanker) {
        this.pingRanker = pingRanker;
    }
    
    @Override
    public void stop() {
        legacyRanker.stop();
        pingRanker.stop();
    }
    
    @Override
    protected void clearState() {
        legacyRanker.clearState();
        pingRanker.clearState();
    }
    
    @Override
    public void setMeshHandler(MeshHandler handler) {
        legacyRanker.setMeshHandler(handler);
        pingRanker.setMeshHandler(handler);
    }
    
    @Override
    public MeshHandler getMeshHandler() {
        return pingRanker.getMeshHandler();
    }

    @Override
    public boolean addToPool(Collection<? extends RemoteFileDescContext> hosts) {
        boolean added = false;
        for (RemoteFileDescContext rfdContext : hosts) {
            Address address = rfdContext.getAddress();
            if (address instanceof Connectable || address instanceof PushEndpoint) {
                added |= pingRanker.addToPool(rfdContext);
            } else {
                added |= legacyRanker.addToPool(rfdContext);
            }
        }
        return added;
    }

    @Override
    public boolean addToPool(RemoteFileDescContext host) {
        return addToPool(Collections.singleton(host));
    }

    @Override
    public RemoteFileDescContext getBest() {
        RemoteFileDescContext best = legacyRanker.getBest();
        return best != null ? best : pingRanker.getBest();
    }
    
    @Override
    protected boolean visitSources(Visitor<RemoteFileDescContext> contextVisitor) {
        if(pingRanker.visitSources(contextVisitor)) {
            return legacyRanker.visitSources(contextVisitor);
        } else {
            return false;
        }
    }

    @Override
    public Collection<RemoteFileDescContext> getShareableHosts() {
        return new MultiCollection<RemoteFileDescContext>(pingRanker.getShareableHosts(), legacyRanker.getShareableHosts());
    }

    @Override
    public boolean hasMore() {
        return pingRanker.hasMore() || legacyRanker.hasMore();
    }

}
