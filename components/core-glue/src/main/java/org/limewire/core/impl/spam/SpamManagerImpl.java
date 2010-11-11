package org.limewire.core.impl.spam;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.search.RemoteFileDescAdapter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SpamServices;

@Singleton
public class SpamManagerImpl implements SpamManager {

    private final com.limegroup.gnutella.spam.SpamManager spamManager;
    private final SpamServices spamServices;
    
    @Inject
    public SpamManagerImpl(com.limegroup.gnutella.spam.SpamManager spamManager, SpamServices spamServices) {
        this.spamManager = spamManager;
        this.spamServices = spamServices;
    }

    @Override
    public void clearFilterData() {
        spamManager.clearFilterData();
    }

    @Override
    public void handleUserMarkedGood(List<? extends SearchResult> searchResults) {
        RemoteFileDesc[] remoteFileDescs = buildArray(searchResults);
        spamManager.handleUserMarkedGood(remoteFileDescs);
    }

    @Override
    public void handleUserMarkedSpam(List<? extends SearchResult> searchResults) {
        RemoteFileDesc[] remoteFileDescs = buildArray(searchResults);
        spamManager.handleUserMarkedSpam(remoteFileDescs);
    }

    private RemoteFileDesc[] buildArray(List<? extends SearchResult> searchResults) {
        List<RemoteFileDesc> remoteFileDescs = new ArrayList<RemoteFileDesc>(searchResults.size());
        // this can be an instance of a copy on write array list, so only iterating
        // over it is safe and assumptions about its size can be wrong
        for (SearchResult searchResult : searchResults) {
            RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter) searchResult;
            RemoteFileDesc remoteFileDesc = remoteFileDescAdapter.getRfd();
            remoteFileDescs.add(remoteFileDesc);
        }
        return remoteFileDescs.toArray(new RemoteFileDesc[remoteFileDescs.size()]);
    }

    @Override
    public void reloadIPFilter() {
        spamServices.reloadIPFilter();
    }

    @Override
    public void adjustSpamFilters() {
        spamServices.adjustSpamFilters();        
    }
    
    @Override
    public void addToBlackList(String ipAddress) {
        spamServices.blockHost(ipAddress);
    }
}
