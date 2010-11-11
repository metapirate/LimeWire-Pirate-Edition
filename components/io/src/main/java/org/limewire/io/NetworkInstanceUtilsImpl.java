package org.limewire.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class NetworkInstanceUtilsImpl implements NetworkInstanceUtils {
    
    private final LocalSocketAddressProvider localSocketAddressProvider;
    private final IpPortForSelf ipPortForSelf;
    
    @Inject
    NetworkInstanceUtilsImpl(LocalSocketAddressProvider localSocketAddressProvider,
            IpPortForSelf ipPortForSelf) {
        this.localSocketAddressProvider = localSocketAddressProvider;
        this.ipPortForSelf = ipPortForSelf;
    }

    public boolean isMe(String host, int port) {
        try {
            return isMe(InetAddress.getByName(host).getAddress(), port);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public boolean isMe(byte[] address, int port) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "127.*.*.*" since
        //they are aliases this machine.
    
        if (NetworkUtils.isLoopbackAddress(address)) {
            return port == localSocketAddressProvider.getLocalPort();
        } else {
            byte[] local = localSocketAddressProvider.getLocalAddress();
            return port == localSocketAddressProvider.getLocalPort() 
                    && Arrays.equals(address, local);
        }
    }

    public boolean isMe(IpPort me) {
        if (me == ipPortForSelf)
            return true;
        return isMe(me.getInetAddress().getAddress(), me.getPort());
    }
    

    public boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        // if 0 is not a private address but 1 is, then the next
        // check will fail anyway, so this is okay.
        if (!isPrivateAddress(addr0) ) {
            return false;
        } else {
            return NetworkUtils.isVeryCloseIP(addr0, addr1);
        }
    }
    
    public boolean isVeryCloseIP(InetAddress addr) {
        return isVeryCloseIP(addr.getAddress());
    }
    
    public boolean isVeryCloseIP(byte[] addr) {
        return isVeryCloseIP(localSocketAddressProvider.getLocalAddress(), addr);
    }
    
    public boolean isPrivate() {
        return isPrivateAddress(localSocketAddressProvider.getLocalAddress());
    }
    
    public boolean isPrivateAddress(InetAddress address) {
        if(!localSocketAddressProvider.isLocalAddressPrivate()) {
            return false;
        } else {
            return NetworkUtils.isPrivateAddress(address);
        }
    }
    
    public boolean isPrivateAddress(byte[] address) {
        if(!localSocketAddressProvider.isLocalAddressPrivate()) {
            return false;
        } else {
            return NetworkUtils.isPrivateAddress(address);
        }
    }
    
    public boolean isPrivateAddress(String address) {
        try {
            return isPrivateAddress(InetAddress.getByName(address));
        } catch(UnknownHostException uhe) {
            return true;
        }
    }

    public boolean isPrivateAddress(SocketAddress address) {
        return isPrivateAddress(((InetSocketAddress)address).getAddress());
    }
    
    public boolean isValidExternalIpPort(IpPort addr) {
        return NetworkUtils.isValidExternalIpPort(addr) && !isPrivateAddress(addr.getInetAddress());
    }

    
}
