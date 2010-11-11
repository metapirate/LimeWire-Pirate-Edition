package com.limegroup.gnutella.messages.vendor;

/** 
 * The message that lets other know what capabilities you support. Every time 
 * you add a capability you should modify this class.
 *
 */
public interface CapabilitiesVM extends VendorMessage.ControlMessage {

    /**
     * Bytes for advertising that we support a 'feature' search.
     * The value is 'WHAT' for legacy reasons, because 'what is new' 
     * was the first feature search supported.
     */
    static final byte[] FEATURE_SEARCH_BYTES = {(byte)87, (byte)72,
                                                      (byte)65, (byte)84};
    
    /** Bytes for supporting incoming TLS. */
    static final byte[] TLS_SUPPORT_BYTES = { 'T', 'L', 'S', '!' };
    
    /** Bytes for supporting FWT. */
    static final byte[] FWT_SUPPORT_BYTES = { 'F', '2', 'F', 'T' };
    
    /** Bytes for supporting incoming TCP connections. */
    static final byte[] INCOMING_TCP_BYTES = { 'T', 'C', 'P', 'I' };
    
    /**
     * The current version of this message.
     */
    public static final int VERSION = 1;

    
    /**
     * @return -1 if the ability isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsCapability(byte[] capabilityName);
    
    /**
     * Return 1 or higher if TLS is supported by the connection.
     * This does not necessarily mean the connection is over
     * TLS though.
     */
    public int supportsTLS();

    /** @return 1 or higher if capability queries are supported.  the version
     *  number gives some indication about what exactly is a supported.  if no
     *  support, returns -1.
     */
    public int supportsFeatureQueries();
    

    /** @return true if 'what is new' capability query feature is supported.
     */
    public boolean supportsWhatIsNew();
    
    /**
     * Returns the current DHT version if this node is an ACTIVE DHT node.
     */
    public int isActiveDHTNode();
    
    /**
     * Returns the current DHT version if this node is an PASSIVE DHT node.
     */
    public int isPassiveDHTNode();

    /**
     * Returns the current DHT version if this node is an PASSIVE_LEAF DHT node.
     */
    public int isPassiveLeafNode();
    
    /**
     * @return true unless the remote host indicated they can't accept 
     * incoming tcp. If they didn't say anything we assume they can
     */
    public boolean canAcceptIncomingTCP();
    
    /**
     * @return true unless the remote host indicated they can't do 
     * firewall-to-firewall transfers. If they didn't say anything we assume they can
     */
    public boolean canDoFWT();
}



