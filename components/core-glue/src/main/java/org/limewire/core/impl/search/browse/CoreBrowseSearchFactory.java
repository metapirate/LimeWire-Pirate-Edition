package org.limewire.core.impl.search.browse;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
class CoreBrowseSearchFactory implements BrowseSearchFactory {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;
    private final ExecutorService backgroundExecutor;

    @Inject
    public CoreBrowseSearchFactory(RemoteLibraryManager remoteLibraryManager,
            BrowseFactory browseFactory,
            @Named("backgroundExecutor") ExecutorService backgroundExecutor) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.browseFactory = browseFactory;
        this.backgroundExecutor = backgroundExecutor;
    }
    
    public BrowseSearch createFriendBrowseSearch(Friend friend){
        assert(friend != null && !friend.isAnonymous());
        return new FriendSingleBrowseSearch(remoteLibraryManager, friend, backgroundExecutor);
    }
    
    public BrowseSearch createBrowseSearch(FriendPresence presence){
        assert(presence != null);
        if(presence.getFriend().isAnonymous()){
            return new AnonymousSingleBrowseSearch(browseFactory, presence);
        } else {
            return createFriendBrowseSearch(presence.getFriend());
        }        
    }
    
    @Override
    public BrowseSearch createBrowseSearch(Collection<FriendPresence> presences) {
        return new MultipleBrowseSearch(this, presences);
    }
    
    public BrowseSearch createAllFriendsBrowseSearch(){
        return new AllFriendsBrowseSearch(remoteLibraryManager, backgroundExecutor);
    }
   
    
}
