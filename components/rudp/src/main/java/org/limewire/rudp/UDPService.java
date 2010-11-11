package org.limewire.rudp;

import java.net.InetAddress;
import java.net.SocketAddress;

import org.limewire.rudp.messages.RUDPMessage;

/** Defines an interface to allow {@link RUDPMessage RUDPMessages} to be sent 
 * and received over UDP. 
 */
public interface UDPService {

    /** Determines if the service is listening for incoming messages. */
    public boolean isListening();
    
    /** Determines if the service is capable of traversing NATs. */
    public boolean isNATTraversalCapable();
    
    /** Sends an RUDP message to the given address. */
    public void send(RUDPMessage message, SocketAddress address);
    
    /** Retrieves the port this appears to be listening on from the outside world. */
    public int getStableListeningPort();
    
    /** Retrieves the address this appears to be listening on from the outside world. */
    public InetAddress getStableListeningAddress();
    
}
