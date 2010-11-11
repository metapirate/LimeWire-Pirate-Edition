package com.limegroup.gnutella.downloader;

import org.limewire.core.settings.DownloadSettings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class SourceRankerFactory {
    
    private final NetworkManager networkManager;
    private final Provider<PingRanker> pingRanker;
    
    @Inject
    public SourceRankerFactory(NetworkManager networkManager,
                               Provider<PingRanker> pingRanker) {
        this.networkManager = networkManager;
        this.pingRanker = pingRanker;
    }

    FriendsFirstSourceRanker createFriendsFirstSourceRanker() {
        return new FriendsFirstSourceRanker(pingRanker.get());
    }
    
    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    public SourceRanker getAppropriateRanker() {
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue())
            return createFriendsFirstSourceRanker();
        else 
            return new LegacyRanker();
    }

    /**
     * @param original the current ranker that we use
     * @return the ranker that should be used.  If different than the current one,
     * the current one is stopped.
     */
    public SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourceRanker better;
        if (networkManager.canReceiveSolicited() && DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof FriendsFirstSourceRanker) {
                return original;
            }
            better = createFriendsFirstSourceRanker();
        } else {
            if (original instanceof LegacyRanker) {
                return original;
            }
            better = new LegacyRanker();
        }

        better.setMeshHandler(original.getMeshHandler());
        better.setRfdVisitor(original.getRfdVisitor());
        better.addToPool(original.getShareableHosts());
        original.stop();
        return better;
    }
}
