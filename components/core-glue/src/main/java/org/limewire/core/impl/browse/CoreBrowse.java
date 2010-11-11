package org.limewire.core.impl.browse;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

class CoreBrowse implements Browse {    
    
    private final SearchServices searchServices;
    private final FriendPresence friendPresence;
    private final QueryReplyListenerList listenerList;
    private final RemoteFileDescAdapter.Factory remoteFileDescAdapterFactory;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile byte[] browseGuid;
    private volatile BrowseResultAdapter listener;

    @Inject
    public CoreBrowse(@Assisted FriendPresence friendPresence, SearchServices searchServices,
            QueryReplyListenerList listenerList, RemoteFileDescAdapter.Factory remoteFileDescAdapterFactory) {
        this.friendPresence = Objects.nonNull(friendPresence, "friendPresence");
        this.searchServices = searchServices;
        this.listenerList = listenerList;
        this.remoteFileDescAdapterFactory = remoteFileDescAdapterFactory;
    }

    @Override
    public void start(final BrowseListener browseListener) {
        if (started.getAndSet(true)) {
            throw new IllegalStateException("already started!");
        }

        browseGuid = searchServices.newQueryGUID();
        listener = new BrowseResultAdapter(browseListener);
        listenerList.addQueryReplyListener(browseGuid, listener);
        
        searchServices.doAsynchronousBrowseHost(friendPresence, new GUID(browseGuid), new ListenerProxy(browseListener));
    }

    @Override
    public void stop() {
        // if the listener hadn't already stopped,
        // this is a 'cancel' which we'll consider a failed browse.
        if(!stopped.getAndSet(true)) {
            if(listener != null) {
                listener.browseListener.browseFinished(false);
            }
        }
        // TODO: This should cancel the browse if it was active.
        listenerList.removeQueryReplyListener(browseGuid, listener);
        searchServices.stopQuery(new GUID(browseGuid));
    }
    
    private class ListenerProxy implements BrowseListener {
        private final BrowseListener delegate;
        
        public ListenerProxy(BrowseListener delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void browseFinished(boolean success) {
            // only push to delegate if this hasn't already been stoppeed.
            if(!stopped.getAndSet(true)) {
                delegate.browseFinished(success);
            }
            stop();
        }
        
        @Override
        public void handleBrowseResult(SearchResult searchResult) {
            if(!stopped.get()) {
                delegate.handleBrowseResult(searchResult);
            }
        }
    }

    private class BrowseResultAdapter implements QueryReplyListener {
        private final BrowseListener browseListener;

        public BrowseResultAdapter(BrowseListener browseListener) {
            this.browseListener = browseListener;
        }

        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply, Set<? extends IpPort> locs) {
            browseListener.handleBrowseResult(remoteFileDescAdapterFactory.create(rfd, locs));
        }
    }
}
