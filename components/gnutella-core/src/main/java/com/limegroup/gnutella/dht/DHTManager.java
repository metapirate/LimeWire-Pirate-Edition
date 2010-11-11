package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * The DHT Manager interface defines methods to start, stop and perform
 * operations related to the maintenance of the DHT (bootstrapping, etc.).
 * It also takes care of switching between different DHT modes for a DHT node.
 */
public interface DHTManager extends ConnectionLifecycleListener, 
        EventDispatcher<DHTEvent, DHTEventListener>{
    
    /**
     * Defines the modes of a DHT Node (inactive, active, passive and passive leaf).
     */
    public static enum DHTMode {
        
        /**
         * A DHT Node is in INACTIVE mode if it supports the DHT
         * but is currently not capable of joining it.
         * 
         * @see NodeAssigner.java
         */
        INACTIVE(0x00, new byte[]{ 'I', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is ACTIVE mode if it's a full participant
         * of the DHT, e.g. a non-firewalled Gnutella leave node
         * with a sufficiently stable connection.
         */
        ACTIVE(0x01, new byte[]{ 'A', 'D', 'H', 'T' }),
        
        /**
         * A DHT Node is in PASSIVE mode if it's connected to
         * the DHT but is not part of the global DHT routing table. 
         * Thus, a passive node never receives requests from the DHT 
         * and does necessarily have an accurate knowledge of the DHT
         * structure. However, it can perform queries and requests stores.
         */
        PASSIVE(0x02, new byte[]{ 'P', 'D', 'H', 'T' }),
        
        /**
         * The PASSIVE_LEAF mode is very similar to PASSIVE mode with
         * two major differences:
         * <pre>
         * 1) A passive leaf has a fixed size LRU Map as its RouteTable.
         *    That means it has almost no knowledge of the global DHT
         *    RouteTable and depends entirely on an another peer (Ultrapeer)
         *    that feeds it continuously with fresh contacts.
         * 
         * 2) A passive leaf node does not perform any Kademlia maintenance
         *    operations!</pre>
         */
        PASSIVE_LEAF(0x03, new byte[]{ 'L', 'D', 'H', 'T' });
        
        public static final byte DHT_MODE_MASK = 0x0F;
        
        private final int mode;
        
        private final byte[] capabilityName;
        
        private DHTMode(int mode, byte[] capabilityName) {
            assert (capabilityName.length == 4);
            this.mode = mode;
            this.capabilityName = capabilityName;
        }
        
        /**
         * Returns the mode as byte.
         */
        public byte byteValue() {
            return (byte)(mode & 0xFF);
        }
        
        /**
         * Returns the VM capability name.
         */
        public byte[] getCapabilityName() {
            byte[] copy = new byte[capabilityName.length];
            System.arraycopy(capabilityName, 0, copy, 0, copy.length);
            return copy;
        }
        
        private static final DHTMode[] MODES;
        
        static {
            DHTMode[] modes = values();
            MODES = new DHTMode[modes.length];
            for (DHTMode m : modes) {
                int index = (m.mode & 0xFF) % MODES.length;
                assert (MODES[index] == null);
                MODES[index] = m;
            }
        }
        
        /**
         * Returns a DHTMode enum for the given mode and null
         * if no such DHTMode exists.
         */
        public static DHTMode valueOf(int mode) {
            int index = (mode & 0xFF) % MODES.length;
            DHTMode s = MODES[index];
            if (s.mode == mode) {
                return s;
            }
            return null;
        }
    }
    
    /**
     * Sets whether or not the DHT is enabled.
     */
    public void setEnabled(boolean enabled);
    
    /**
     * Returns whether or not the DHT is enabled.
     */
    public boolean isEnabled();
    
    /**
     * Starts the DHT Node either in active or passive mode.
     * <p>
     * Note: You can use this method to stop the DHT by passing in
     * DHTMode.INACTIVE. The difference between using this method
     * and the {@link #stop()} method is that stop() is synchronous 
     * (i.e. blocking) and start() with DHTMode.INACTIVE isn't.
     */
    public void start(DHTMode mode);

    /**
     * Stops the DHT Node.
     */
    public void stop();
    
    /**
     * Passes the given active DHT node to the DHT controller 
     * in order to bootstrap or perform other maintenance operations. 
     */
    public void addActiveDHTNode(SocketAddress hostAddress);
    
    /**
     * Passes the given passive DHT node to the DHT controller 
     * in order to bootstrap or perform other maintenance operations. 
     */
    public void addPassiveDHTNode(SocketAddress hostAddress);
    
    /**
     * Notifies the DHT controller that our external Address has changed.
     */
    public void addressChanged();
    
    /**
     * Returns maxNodes number of active Node's IP:Ports.
     */
    public List<IpPort> getActiveDHTNodes(int maxNodes);

    /**
     * Returns the mode of the DHT.
     */
    public DHTMode getDHTMode();
    
    /**
     * Returns whether this Node is running.
     */
    public boolean isRunning();
    
    /**
     * Returns whether this Node is bootstrapped.
     */
    public boolean isBootstrapped();
    
    /**
     * Returns whether this Node is part of the DHT.
     */
    public boolean isMemberOfDHT();
    
    /**
     * Returns whether this Node is waiting for Nodes or not.
     */
    public boolean isWaitingForNodes();
    
    /**
     * Returns the MojitoDHT instance (null if Node is running in inactive mode!).
     */
    public MojitoDHT getMojitoDHT();
    
    /**
     * Returns the Vendor code of this Node.
     */
    public Vendor getVendor();
    
    /**
     * Returns the Vendor code of this Node.
     */
    public Version getVersion();
    
    /**
     * Callback to notify the manager about new DHT Contacts that
     * were exchanged over regular Gnutella messages.
     * 
     * @see com.limegroup.gnutella.messages.vendor.DHTContactsMessage
     */
    public void handleDHTContactsMessage(DHTContactsMessage msg);
    
    /**
     * Calls the {@link MojitoDHT#get(EntityKey)} if a bootstrappable DHT is available.
     * Also handles the locking properly to ensure thread safety.
     * 
     * @param eKey the entity key used to perform lookup in the DHT.
     * 
     * @return an instance of <code>DHTFuture</code> containing the result of the lookup. 
     * <br> Returns null if DHT is unavailable or the DHT is not bootstrapped.
     */
    public DHTFuture<FindValueResult> get(EntityKey eKey);
    
    /**
     * Calls the {@link MojitoDHT#put(KUID, DHTValue)} if a bootstrappable DHT is available.
     * Also handles the locking properly to ensure thread safety.
     * 
     * @param eKey the entity key used to perform lookup in the DHT.
     * 
     * @return an instance of <code>DHTFuture</code> containing the result of the lookup. 
     * <br> Returns null if DHT is unavailable or the DHT is not bootstrapped.
     */
    public DHTFuture<StoreResult> put(KUID key, DHTValue value);
}
