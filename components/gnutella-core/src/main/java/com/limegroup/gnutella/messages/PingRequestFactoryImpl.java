package com.limegroup.gnutella.messages;

import java.util.LinkedList;
import java.util.List;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.GUID;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.Message.Network;

@Singleton
public class PingRequestFactoryImpl implements PingRequestFactory {

    public final NetworkManager networkManager;
    private final ConnectionServices connectionServices;

    @Inject
    public PingRequestFactoryImpl(NetworkManager networkManager, ConnectionServices connectionServices) {
        this.networkManager = networkManager;
        this.connectionServices = connectionServices;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createPingRequest(byte[], byte, byte)
     */
    public PingRequest createPingRequest(byte[] guid, byte ttl, byte hops) {
        return new PingRequestImpl(guid, ttl, hops);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createPingRequest(byte[], byte, byte, byte[])
     */
    public PingRequest createFromNetwork(byte[] guid, byte ttl,
            byte hops, byte[] payload, Network network) {
        return new PingRequestImpl(guid, ttl, hops, payload);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createPingRequest(byte)
     */
    public PingRequest createPingRequest(byte ttl) {
        return new PingRequestImpl(ttl);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createPingRequest(byte[], byte)
     */
    public PingRequest createPingRequest(byte[] guid, byte ttl) {
        return new PingRequestImpl(guid, ttl);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createQueryKeyRequest()
     */
    public PingRequest createQueryKeyRequest() {
        List<NameValue<?>> l = new LinkedList<NameValue<?>>();
        l.add(new NameValue(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT));
        return new PingRequestImpl(GUID.makeGuid(), (byte)1, l);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createUDPPing()
     */
    public PingRequest createUDPPing() {
        List<NameValue<?>> l = new LinkedList<NameValue<?>>();
        return new PingRequestImpl(populateUDPGGEPList(l).bytes(), (byte)1, l);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createUDPingWithDHTIPPRequest()
     */
    public PingRequest createUDPingWithDHTIPPRequest() {
        List<NameValue<?>> l = new LinkedList<NameValue<?>>();
        GUID guid = new GUID();
        l.add(new NameValue(GGEPKeys.GGEP_HEADER_DHT_IPPORTS));
        return new PingRequestImpl(guid.bytes(), (byte)1, l);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.PingRequestFactory#createUHCPing()
     */    
    public PingRequest createUHCPing() {
        List<NameValue<?>> ggeps = new LinkedList<NameValue<?>>();
        GUID guid = populateUDPGGEPList(ggeps);
        ggeps.add(new NameValue(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE));
        return new PingRequestImpl(guid.bytes(),(byte)1,ggeps);
    }
    
    /**
     * Creates a TTL 1 Ping for faster bootstrapping, intended
     * for sending to the multicast network.
     */
    public PingRequest createMulticastPing() {
        GUID guid = new GUID();        
        List<NameValue<?>> l = new LinkedList<NameValue<?>>();
        l.add(new NameValue<byte[]>(GGEPKeys.GGEP_HEADER_SUPPORT_CACHE_PONGS, getSCPData()));
        return new PingRequestImpl(guid.bytes(), (byte)1, l);
    }    
    
    /**
     * @param l list to put the standard extentions we add to UDP pings
     * @return the guid to use for the ping
     */
    private GUID populateUDPGGEPList(List<NameValue<?>> l) {
        GUID guid;
        if(ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue()) {
            guid = PingRequest.UDP_GUID;
        } else {
            l.add(new NameValue(GGEPKeys.GGEP_HEADER_IPPORT));
            guid = networkManager.getSolicitedGUID();
        }        
        l.add(new NameValue<byte[]>(GGEPKeys.GGEP_HEADER_SUPPORT_CACHE_PONGS, getSCPData()));
        return guid;
    }
    
    byte[] getSCPData() {
        byte[] data = new byte[1];
        if(connectionServices.isSupernode())
            data[0] = PingRequest.SCP_ULTRAPEER;
        else
            data[0] = PingRequest.SCP_LEAF;
        
        if(networkManager.isIncomingTLSEnabled())
            data[0] |= PingRequest.SCP_TLS; // add our support for TLS.
        
        return data;
    }
    
}
