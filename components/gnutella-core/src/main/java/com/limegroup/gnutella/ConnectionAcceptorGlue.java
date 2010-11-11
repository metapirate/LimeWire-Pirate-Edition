package com.limegroup.gnutella;

import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.browser.ControlRequestAcceptor;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;
import com.limegroup.gnutella.downloader.PushDownloadManager;

@EagerSingleton
class ConnectionAcceptorGlue {

    private final ConnectionDispatcher externalDispatcher;
    private final ConnectionDispatcher localDispatcher;
    
    private final LocalHTTPAcceptor localHttpAcceptor;
    private final HTTPAcceptor externalHttpAcceptor;
    private final PushDownloadManager pushDownloadManager;
    private final ControlRequestAcceptor controlRequestAcceptor;

    @Inject
    public ConnectionAcceptorGlue(
            @Named("global") ConnectionDispatcher externalDispatcher,
            @Named("local") ConnectionDispatcher localDispatcher,
            HTTPAcceptor externalHttpAcceptor,
            LocalHTTPAcceptor localHttpAcceptor,
            PushDownloadManager pushDownloadManager,
            ControlRequestAcceptor controlRequestAcceptor) {
        this.externalDispatcher = externalDispatcher;
        this.localDispatcher = localDispatcher;
        this.externalHttpAcceptor = externalHttpAcceptor;
        this.pushDownloadManager = pushDownloadManager;
        this.localHttpAcceptor = localHttpAcceptor;
        this.controlRequestAcceptor = controlRequestAcceptor;
    }

    @Inject
    @SuppressWarnings({"unused", "UnusedDeclaration"})
    private void register(org.limewire.lifecycle.ServiceRegistry registry) {
        // TODO: This really should be a bunch of services that depend on the
        //       dispatchers being started.  We workaround that by starting
        //       them in the LATE stage, which assumes the dispatchers
        //       are started in EARLY or NORMAL.
        registry.register(new Service() {
            public String getServiceName() {
                return org.limewire.i18n.I18nMarker.marktr("Connection Dispatching");
            }

            public void initialize() {
            };

            public void start() {
                externalDispatcher.addConnectionAcceptor(externalHttpAcceptor, false,
                        externalHttpAcceptor.getHttpMethods());
                externalDispatcher.addConnectionAcceptor(pushDownloadManager, false, "GIV");
                localDispatcher.addConnectionAcceptor(localHttpAcceptor, true, localHttpAcceptor
                        .getHttpMethods());
                localDispatcher.addConnectionAcceptor(controlRequestAcceptor,
                            true, "MAGNET", "TORRENT");
                externalDispatcher.addConnectionAcceptor(controlRequestAcceptor,
                            true, "MAGNET","TORRENT");
            }

            public void stop() {
            };
        }).in(ServiceStage.LATE);
    }

}
