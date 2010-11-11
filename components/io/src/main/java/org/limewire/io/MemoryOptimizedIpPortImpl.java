package org.limewire.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.limewire.util.ByteUtils;

class MemoryOptimizedIpPortImpl implements IpPort {

    private int addr;
    private short shortport;
    
    public MemoryOptimizedIpPortImpl(IP ip, short shortport) {
        this.addr = ip.addr;
        this.shortport = shortport;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    public InetAddress getInetAddress() {
        byte[] baddr = new byte[4];
        ByteUtils.int2beb(addr, baddr, 0);
        try {
            return InetAddress.getByAddress(baddr);
        } catch(UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }
    
    public String getAddress() {
        return ((addr >> 24) & 0xFF) + "." +
               ((addr >> 16) & 0xFF) + "." + 
               ((addr >>  8) & 0xFF) + "." + 
               ( addr        & 0xFF);
    }
    
    public int getPort() {
        return ByteUtils.ushort2int(shortport);
    }
    
    @Override
    public String toString() {
        return "host: " + getAddress() + ", port: " + getPort();
    }
}
