package org.limewire.core.impl.friend;

import org.limewire.core.impl.xmpp.FriendFileViewProvider;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;

@EagerSingleton
public class CoreGlueFriendService implements Service {

    private final Provider<HTTPAcceptor> httpAcceptor;
    private final HttpRequestHandlerFactory httpRequestHandlerFactory;
    private final Provider<FriendFileViewProvider> authenticatingBrowseFriendListProvider;
    
    final static String FRIEND_BROWSE_PREFIX = "/friend/browse/";
    final static String FRIEND_DOWNLOAD_PREFIX = "/friend/download/";
    
    final static String FRIEND_BROWSE_PATTERN = FRIEND_BROWSE_PREFIX + "*";
    final static String FRIEND_DOWNLOAD_PATTERN = FRIEND_DOWNLOAD_PREFIX + "*";

    @Inject
    public CoreGlueFriendService(Provider<HTTPAcceptor> httpAcceptor, HttpRequestHandlerFactory httpRequestHandlerFactory,
           Provider<FriendFileViewProvider> authenticatingBrowseFriendListProvider) {
        this.httpAcceptor = httpAcceptor;
        this.httpRequestHandlerFactory = httpRequestHandlerFactory;
        this.authenticatingBrowseFriendListProvider = authenticatingBrowseFriendListProvider;
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return I18nMarker.marktr("XMPP Service");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        httpAcceptor.get().registerHandler(FRIEND_BROWSE_PATTERN, httpRequestHandlerFactory.createBrowseRequestHandler(authenticatingBrowseFriendListProvider.get(), true)); 
        httpAcceptor.get().registerHandler(FRIEND_DOWNLOAD_PATTERN, httpRequestHandlerFactory.createFileRequestHandler(authenticatingBrowseFriendListProvider.get(), true));
    }

    @Override
    public void stop() {
        httpAcceptor.get().unregisterHandler(FRIEND_BROWSE_PATTERN);
        httpAcceptor.get().unregisterHandler(FRIEND_DOWNLOAD_PATTERN);
    }

}
