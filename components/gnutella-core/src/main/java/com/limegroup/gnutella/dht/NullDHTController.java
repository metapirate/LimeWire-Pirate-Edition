package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

/**
 * Uses the Null Object pattern to avoid repetitive null checks in 
 * <code>DHTManager</code>s or to be used as stub in tests.
 */
public class NullDHTController implements DHTController {

    public void addActiveDHTNode(SocketAddress hostAddress) {
    }

    public void addPassiveDHTNode(SocketAddress hostAddress) {
    }

    public void addContact(Contact node) {
    }

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        return Collections.emptyList();
    }

    public MojitoDHT getMojitoDHT() {
        return null;
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }

    public DHTMode getDHTMode() {
        return DHTMode.INACTIVE;
    }

    public boolean isRunning() {
        return false;
    }

    public boolean isWaitingForNodes() {
        return false;
    }

    public boolean isBootstrapped() {
        return false;
    }

    public void sendUpdatedCapabilities() {
    }

    public void start() {
    }

    public void stop() {
    }
}
