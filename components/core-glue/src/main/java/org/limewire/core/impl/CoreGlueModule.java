package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.impl.browse.CoreGlueBrowseModule;
import org.limewire.core.impl.connection.CoreGlueConnectionModule;
import org.limewire.core.impl.daap.CoreGlueDaapModule;
import org.limewire.core.impl.download.CoreGlueDownloadModule;
import org.limewire.core.impl.download.DownloadListenerList;
import org.limewire.core.impl.friend.CoreGlueFriendModule;
import org.limewire.core.impl.itunes.ItunesMediator;
import org.limewire.core.impl.itunes.ItunesMediatorImpl;
import org.limewire.core.impl.library.CoreGlueLibraryModule;
import org.limewire.core.impl.lifecycle.LifeCycleManagerImpl;
import org.limewire.core.impl.magnet.MagnetFactoryImpl;
import org.limewire.core.impl.mojito.CoreGlueMojitoModule;
import org.limewire.core.impl.monitor.CoreGlueMonitorModule;
import org.limewire.core.impl.monitor.IncomingSearchListenerList;
import org.limewire.core.impl.mozilla.CoreGlueMozillaModule;
import org.limewire.core.impl.network.CoreGlueNetworkModule;
import org.limewire.core.impl.player.CoreGluePlayerModule;
import org.limewire.core.impl.properties.CoreGluePropertiesModule;
import org.limewire.core.impl.rest.CoreGlueRestModule;
import org.limewire.core.impl.search.CoreGlueSearchModule;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.search.browse.CoreGlueBrowseSearchModule;
import org.limewire.core.impl.spam.CoreGlueSpamModule;
import org.limewire.core.impl.support.CoreGlueSupportModule;
import org.limewire.core.impl.upload.CoreGlueUploadModule;
import org.limewire.core.impl.upload.UploadListenerList;
import org.limewire.core.impl.xmpp.CoreGlueXMPPModule;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.ActivityCallback;

public class CoreGlueModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ActivityCallback.class).to(GlueActivityCallback.class);
        bind(GuiCallbackService.class).to(GlueActivityCallback.class);
        bind(QueryReplyListenerList.class).to(GlueActivityCallback.class);
        bind(DownloadListenerList.class).to(GlueActivityCallback.class);
        bind(UploadListenerList.class).to(GlueActivityCallback.class);
        bind(IncomingSearchListenerList.class).to(GlueActivityCallback.class);
        bind(TorrentFactory.class).to(TorrentFactoryImpl.class);
        bind(Application.class).to(ApplicationImpl.class);

        bind(LifeCycleManager.class).to(LifeCycleManagerImpl.class);
        bind(MagnetFactory.class).to(MagnetFactoryImpl.class);
        bind(ItunesMediator.class).to(ItunesMediatorImpl.class);
        
        install(new CoreGlueSpamModule());
        install(new CoreGlueConnectionModule());
        install(new CoreGlueDaapModule());
        install(new CoreGlueSearchModule());
        install(new CoreGlueBrowseSearchModule());
        install(new CoreGlueNetworkModule());
        install(new CoreGlueFriendModule());
        install(new CoreGlueDownloadModule());
        install(new CoreGlueLibraryModule());
        install(new CoreGlueMojitoModule());
        install(new CoreGlueMonitorModule());
        install(new CoreGlueBrowseModule());
        install(new CoreGlueXMPPModule());
        install(new CoreGluePlayerModule());
        install(new CoreGluePropertiesModule());
        install(new CoreGlueMozillaModule());
        install(new CoreGlueRestModule());
        install(new CoreGlueSupportModule());
        install(new CoreGlueUploadModule());
    }
}
