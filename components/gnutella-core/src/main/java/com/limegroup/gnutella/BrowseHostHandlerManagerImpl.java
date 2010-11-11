package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.GUID;
import org.limewire.lifecycle.Service;
import org.limewire.net.SocketsManager;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;
import com.limegroup.gnutella.messages.MessageFactory;

@EagerSingleton
class BrowseHostHandlerManagerImpl implements BrowseHostHandlerManager, Service {

    private static final Log LOG = LogFactory.getLog(BrowseHostHandlerManagerImpl.class);

    /** Map from serventID to BrowseHostHandler instance. */
    private final Map<GUID, BrowseHostHandler.PushRequestDetails> _pushedHosts = new HashMap<GUID, BrowseHostHandler.PushRequestDetails>();

    private final SocketsManager socketsManager;
    private final Provider<ForMeReplyHandler> forMeReplyHandler;
    private final ScheduledExecutorService backgroundExecutor;

    private final MessageFactory messageFactory;

    private final NetworkManager networkManager;

    private final PushEndpointFactory pushEndpointFactory;
    private final Provider<HttpParams> httpParams;

    @Inject
    public BrowseHostHandlerManagerImpl(@Named("backgroundExecutor")
    ScheduledExecutorService backgroundExecutor,
                                        SocketsManager socketsManager,
                                        Provider<ForMeReplyHandler> forMeReplyHandler,
                                        MessageFactory messageFactory,
                                        NetworkManager networkManager,
                                        PushEndpointFactory pushEndpointFactory, 
                                        @Named("defaults")Provider<HttpParams> httpParams) {
        this.socketsManager = socketsManager;
        this.forMeReplyHandler = forMeReplyHandler;
        this.messageFactory = messageFactory;
        this.backgroundExecutor = backgroundExecutor;
        this.networkManager = networkManager;
        this.pushEndpointFactory = pushEndpointFactory;
        this.httpParams = httpParams;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Browse Host Handler");
    }
    
    public void initialize() {
    }
    
    public void stop() {
    }
    
    public void start() {
        backgroundExecutor.scheduleWithFixedDelay(new Expirer(), 0, 5000, TimeUnit.MILLISECONDS);    
    }

    @Inject
    public void register(PushedSocketHandlerRegistry registry) {
        registry.register(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.BrowseHostHandlerManager#createBrowseHostHandler(com.limegroup.gnutella.ActivityCallback,
     *      com.limegroup.gnutella.GUID, com.limegroup.gnutella.GUID)
     */
    public BrowseHostHandler createBrowseHostHandler(GUID guid, GUID serventID) {
        return new BrowseHostHandler(guid, socketsManager,
                forMeReplyHandler, messageFactory, httpParams, 
                networkManager, pushEndpointFactory);
    }

    /** @return true if the Push was handled by me. */
    public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, final Socket socket) {
        GUID serventID = new GUID(clientGUID);
        boolean retVal = false;
        LOG.trace("BHH.handlePush(): entered.");
        // if (index == SPECIAL_INDEX)
        // ; // you'd hope, but not necessary...

        BrowseHostHandler.PushRequestDetails prd = null;
        synchronized (_pushedHosts) {
            prd = _pushedHosts.remove(serventID);
        }
        if (prd != null) {
            final BrowseHostHandler browseHostHandler = prd.getBrowseHostHandler();
            final FriendPresence friendPresence = prd.getFriendPresence();
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        browseHostHandler.browseHost(socket, friendPresence);
                    } catch (IOException e) {
                        LOG.debug("error while push transfer", e);
                        browseHostHandler.failed();
                    } catch (HttpException e) {
                        LOG.debug("error while push transfer", e);
                        browseHostHandler.failed();
                    } catch (URISyntaxException e) {
                        LOG.debug("error while push transfer", e);
                        browseHostHandler.failed();
                    } catch (InterruptedException e) {
                        LOG.debug("error while push transfer", e);
                        browseHostHandler.failed();
                    }
                }
            }, "BrowseHost");
            retVal = true;
        } else
            LOG.debug("BHH.handlePush(): no matching BHH.");

        LOG.trace("BHH.handlePush(): returning.");
        return retVal;
    }

    /** Can be run to invalidate pushes that we are waiting for.... */
    private class Expirer implements Runnable {
        public void run() {
            try {
                Set<GUID> toRemove = new HashSet<GUID>();
                synchronized (_pushedHosts) {
                    for (GUID key : _pushedHosts.keySet()) {
                        BrowseHostHandler.PushRequestDetails currPRD = _pushedHosts
                                .get(key);
                        if ((currPRD != null) && (currPRD.isExpired())) {
                            LOG.debug("Expirer.run(): expiring a badboy.");
                            toRemove.add(key);
                            currPRD.getBrowseHostHandler().failed();
                        }
                    }
                    for (GUID key : toRemove)
                        _pushedHosts.remove(key);
                }
            } catch (Throwable t) {
                ErrorService.error(t);
            }
        }
    }

    @Override
    public BrowseHostHandler createBrowseHostHandler(GUID browseGuid) {
        return new BrowseHostHandler(browseGuid, socketsManager,
                forMeReplyHandler, messageFactory, httpParams,
                networkManager, pushEndpointFactory);
    }

}
