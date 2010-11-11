package org.limewire.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SimpleNetworkInstanceUtils implements NetworkInstanceUtils {

    private final boolean localIsPrivate;
    
    @Inject
    public SimpleNetworkInstanceUtils() {
        this(true);
    }
    
    public SimpleNetworkInstanceUtils(boolean localIsPrivate) {
        this.localIsPrivate = localIsPrivate;
    }

    public boolean isMe(byte[] address, int port) {
        return false;
    }

    public boolean isMe(IpPort me) {
        return false;
    }

    public boolean isMe(String host, int port) {
        return false;
    }

    public boolean isPrivate() {
        return false;
    }

    public boolean isPrivateAddress(byte[] address) {
        return localIsPrivate && NetworkUtils.isPrivateAddress(address);
    }

    public boolean isPrivateAddress(InetAddress address) {
        return localIsPrivate &&  NetworkUtils.isPrivateAddress(address);
    }

    public boolean isPrivateAddress(SocketAddress address) {
        return localIsPrivate &&  NetworkUtils.isPrivateAddress(((InetSocketAddress) address).getAddress());
    }

    public boolean isPrivateAddress(String address) {
        try {
            return localIsPrivate &&  NetworkUtils.isPrivateAddress(InetAddress.getByName(address));
        } catch (UnknownHostException e) {
            return true;
        }
    }

    public boolean isValidExternalIpPort(IpPort addr) {
        return NetworkUtils.isValidExternalIpPort(addr);
    }

    public boolean isVeryCloseIP(byte[] addr) {
        return false;
    }

    public boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        return NetworkUtils.isVeryCloseIP(addr0, addr1);
    }

    public boolean isVeryCloseIP(InetAddress addr) {
        return false;
    }
}
