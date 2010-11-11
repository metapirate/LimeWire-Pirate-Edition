/**
 * 
 */
package org.limewire.xmpp.client.impl;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.Network;

/**
 * Different implementation from {@link org.jivesoftware.smack.ReconnectionManager}
 * to ensure thread safe behavior.
 */
class ReconnectionManager implements EventListener<FriendConnectionEvent> {

    private static final int MAX_RECONNECTION_ATTEMPTS = 10;
    
    private static final Log LOG = LogFactory.getLog(ReconnectionManager.class);
    
    private final XMPPConnectionFactoryImpl serviceImpl;

    /**
     * @param serviceImpl
     */
    ReconnectionManager(XMPPConnectionFactoryImpl serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    private volatile boolean connected;
    
    @Override
    public void handleEvent(FriendConnectionEvent event) {
        // todo: should not be necessary to check for xmpp. Address this in LWC-3436, ReconnectionManager for facebook
        if (event.getSource().getConfiguration().getType() == Network.Type.XMPP) {
            if(event.getType() == FriendConnectionEvent.Type.CONNECTED) {
            connected = true;   
            } else if(event.getType() == FriendConnectionEvent.Type.DISCONNECTED) {
            if(event.getException() != null && connected) {
                    FriendConnection connection = event.getSource();
                    final FriendConnectionConfiguration configuration = connection.getConfiguration();
                Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
                    @Override
                    public void run() {
                        long sleepTime = 10000;
                            FriendConnection newConnection = null;
                        for(int i = 0; i < MAX_RECONNECTION_ATTEMPTS &&
                                newConnection == null; i++) {
                            try {
                                LOG.debugf("attempting to reconnect to {0} ..." + configuration.getServiceName());
                                newConnection = serviceImpl.loginImpl(configuration, true);
                            } catch (FriendException e) {
                                // Ignored
                            }
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                // Ignored
                            }
                        }
                        LOG.debugf("giving up trying to connect to {0}" + configuration.getServiceName());
                    }
                }), "xmpp-reconnection-manager");
                t.start();
            }
            connected = false;
        }
    }
}
}