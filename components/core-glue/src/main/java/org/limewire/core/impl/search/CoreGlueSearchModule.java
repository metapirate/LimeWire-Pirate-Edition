package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.impl.search.torrentweb.TorrentRobotsTxt;
import org.limewire.core.impl.search.torrentweb.TorrentRobotsTxtImpl;
import org.limewire.core.impl.search.torrentweb.TorrentRobotsTxtStore;
import org.limewire.core.impl.search.torrentweb.TorrentUriDatabaseStore;
import org.limewire.core.impl.search.torrentweb.TorrentUriPrioritizerFactory;
import org.limewire.core.impl.search.torrentweb.TorrentUriPrioritizerImpl;
import org.limewire.core.impl.search.torrentweb.TorrentUriStore;
import org.limewire.core.impl.search.torrentweb.TorrentWebSearch;
import org.limewire.core.impl.search.torrentweb.TorrentWebSearchFactory;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchManager.class).to(CoreSearchManager.class);
        bind(SearchFactory.class).toProvider(FactoryProvider.newFactory(SearchFactory.class, CoreSearch.class));
        EventMulticaster<SearchEvent> searchMulticaster = new EventMulticasterImpl<SearchEvent>();
        bind(new TypeLiteral<EventBroadcaster<SearchEvent>>(){}).toInstance(searchMulticaster);
        bind(new TypeLiteral<ListenerSupport<SearchEvent>>(){}).toInstance(searchMulticaster);
        
        bind(RemoteFileDescAdapter.Factory.class).toProvider(FactoryProvider.newFactory(RemoteFileDescAdapter.Factory.class, RemoteFileDescAdapter.class));
        bind(TorrentWebSearchFactory.class).toProvider(FactoryProvider.newFactory(TorrentWebSearchFactory.class, TorrentWebSearch.class));
        bind(TorrentUriPrioritizerFactory.class).toProvider(FactoryProvider.newFactory(TorrentUriPrioritizerFactory.class, TorrentUriPrioritizerImpl.class));
        
        bind(TorrentUriStore.class).to(TorrentUriDatabaseStore.class);
        bind(TorrentRobotsTxtStore.class).to(TorrentUriDatabaseStore.class);
        bind(TorrentRobotsTxt.class).to(TorrentRobotsTxtImpl.class);
    }

}
