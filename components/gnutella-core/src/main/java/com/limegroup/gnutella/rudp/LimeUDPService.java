package com.limegroup.gnutella.rudp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.limewire.io.NetworkUtils;
import org.limewire.rudp.messages.RUDPMessage;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;

@Singleton
class LimeUDPService implements org.limewire.rudp.UDPService {
    
    private final NetworkManager networkManager;
    private final Provider<UDPService> udpService;
    
    @Inject
    public LimeUDPService(NetworkManager networkManager, Provider<UDPService> udpService) {
        this.networkManager = networkManager;
        this.udpService = udpService;
    }

    public InetAddress getStableListeningAddress() {
        InetAddress lip = null;
        try {
            lip = InetAddress.getByName(
              NetworkUtils.ip2string(networkManager.getNonForcedAddress()));
        } catch (UnknownHostException uhe) {
            try {
                lip = InetAddress.getLocalHost();
            } catch (UnknownHostException uhe2) {
            }
        }
        return lip;

    }

    public int getStableListeningPort() {
        return udpService.get().getStableUDPPort();
    }

    public boolean isListening() {
        return udpService.get().isListening();
    }

    public boolean isNATTraversalCapable() {
        return networkManager.canDoFWT();
    }

    public void send(RUDPMessage message, SocketAddress address) {
        udpService.get().send((Message)message, (InetSocketAddress)address);   
    }

}
