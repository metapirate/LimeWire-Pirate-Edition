package com.limegroup.gnutella.dht;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.connection.Connection;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * Controls a passive DHT node (an {@link ActiveDHTNodeController} but is a 
 * Gnutella Ultrapeer). 
 * <p>
 * The passive node controller also maintains the list of this ultrapeer's 
 * leafs in a separate list. These leaves are this node's most accurate knowledge 
 * of the DHT, as leaf connections are state-full TCP, and they are therefore 
 * propagated in priority in the Gnutella network.
 * <p>
 * Persistence is implemented in order to be able to bootstrap the next session 
 * by saving a few Most Recently Seen (MRS) nodes. It is not necessary
 * to persist the entire DHT, because we don't want to keep the same node ID at 
 * the next session, and accuracy of the contacts in the route table is not guaranteed 
 * when a node is passive (as it does not get contacted by the DHT).   
 */
public class PassiveDHTNodeController extends AbstractDHTController {
    
    /**
     * The file to persist the list of host
     */
    private static final File FILE = new File(CommonUtils.getUserSettingsDir(), "passive.mojito");
    
    /**
     * A RouteTable for passive Nodes.
     */
    private PassiveDHTNodeRouteTable routeTable;
    
    PassiveDHTNodeController(Vendor vendor, Version version,
            EventDispatcher<DHTEvent, DHTEventListener> dispatcher,
            DHTControllerFacade dhtControllerFacade) {
        super(vendor, version, dispatcher, DHTMode.PASSIVE, dhtControllerFacade);
    }
    
    @Override
    protected MojitoDHT createMojitoDHT(Vendor vendor, Version version) {
        MojitoDHT dht = MojitoFactory.createFirewalledDHT("PassiveUltrapeerDHT", vendor, version);
        
        routeTable = new PassiveDHTNodeRouteTable(dht);
        dht.setRouteTable(routeTable);
        
        // Load the small list of MRS Nodes for bootstrap
        if (DHTSettings.PERSIST_PASSIVE_DHT_ROUTETABLE.getValue()
                && FILE.exists() && FILE.isFile()) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(
                            new BufferedInputStream(
                                new SecureInputStream(
                                    new FileInputStream(FILE))));
                
                int routeTableVersion = ois.readInt();
                if (routeTableVersion >= getRouteTableVersion()) {
                    Contact node = null;
                    while((node = (Contact)ois.readObject()) != null) {
                        routeTable.add(node);
                    }
                }
            } catch (Throwable ignored) {
                LOG.error("Throwable", ignored);
            } finally {
                IOUtils.close(ois);
            }
        }
        
        return dht;
    }
    
    /**
     * This method first adds the given host to the list of bootstrap nodes and 
     * then adds it to this passive node's routing table.
     * <p>
     * Note: This method makes sure the DHT is running already, as adding a node
     * as a leaf involves sending it a DHT ping (in order to get its KUID).
     */
    protected void addLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return;
        }
        
        SocketAddress addr = new InetSocketAddress(host, port);
        //add to bootstrap nodes if we need to.
        addActiveDHTNode(addr, false);
        //add to our DHT leaves
        if(LOG.isDebugEnabled()) {
            LOG.debug("Adding host: "+addr+" to leaf dht nodes");
        }
        routeTable.addLeafDHTNode(host, port);
    }
    
    protected SocketAddress removeLeafDHTNode(String host, int port) {
        if(!isRunning()) {
            return null;
        }
        
        SocketAddress removed = routeTable.removeLeafDHTNode(host, port);

        if(LOG.isDebugEnabled() && removed != null) {
            LOG.debug("Removed host: "+removed+" from leaf dht nodes");
        }
        return removed;
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        
        super.stop();
        
        if (DHTSettings.PERSIST_PASSIVE_DHT_ROUTETABLE.getValue()) {
            Collection<Contact> contacts = routeTable.getActiveContacts(); 
            if (contacts.size() >= 2) {
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(
                                new BufferedOutputStream(
                                    new SecureOutputStream(
                                        new FileOutputStream(FILE))));
                    
                    oos.writeInt(getRouteTableVersion());
                    
                    // Sort by MRS and save only some Nodes
                    contacts = ContactUtils.sort(contacts, 
                            DHTSettings.MAX_PERSISTED_NODES.getValue());
                    
                    KUID localNodeID = getMojitoDHT().getLocalNodeID();
                    for(Contact node : contacts) {
                        if(!node.getNodeID().equals(localNodeID)) {
                            oos.writeObject(node);
                        }
                    }
                    
                    // EOF Terminator
                    oos.writeObject(null);
                    oos.flush();
                    
                } catch (IOException ignored) {
                } finally {
                    IOUtils.close(oos);
                }
            }
        }
    }

    /**
     * This method return this passive node's leafs first (they have the highest timestamp)
     * <p>
     * Note: Although a passive node does not have accurate info in its route table 
     * (except for direct leafs), we still return nodes. 
     */
    @Override
    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        if(!isRunning() || !getMojitoDHT().isBootstrapped()) {
            return Collections.emptyList();
        }
        
        return getMRSNodes(maxNodes, true);
    }
    
    /**
     * Handle connection-specific life cycle events only. 
     */
    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        //handle connection specific events
        Connection c = evt.getConnection();
        if( c == null) {
            return;
        }
        
        String host = c.getAddress();
        int port = c.getPort();
        
        if(evt.isConnectionClosedEvent()) {
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Got a connection closed event for connection: "+ c);
            }
            
            removeLeafDHTNode( host , port );
            
        } else if(evt.isConnectionCapabilitiesEvent()){
            
            if(c.getConnectionCapabilities().remostHostIsActiveDHTNode() > -1) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is active dht node: "+ c);
                }
                addLeafDHTNode( host , port );
            } else if(c.getConnectionCapabilities().remostHostIsPassiveDHTNode() > -1) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is passive dht node: "+ c);
                }
                addPassiveDHTNode(new InetSocketAddress(host, port));
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Connection is node not connected to the DHT network: "+ c);
                }
                removeLeafDHTNode( host , port );
            }
        } 
    }
}
