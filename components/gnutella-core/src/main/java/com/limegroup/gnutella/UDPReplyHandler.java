package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.util.DataUtils;

/**
 * This class is an implementation of <tt>ReplyHandler</tt> that is 
 * specialized for handling UDP messages.
 */
public final class UDPReplyHandler implements ReplyHandler {

	/** The InetSocketAddress this is for. */
    private final InetSocketAddress addr;
    private final UDPService udpService;
	
	/** Creates a new UDPReplyHandler for the given address. */
	UDPReplyHandler(InetSocketAddress addr, UDPService udpService) {
	    this.udpService = udpService;
        if(!NetworkUtils.isValidSocketAddress(addr))
	        throw new IllegalArgumentException("invalid addr: " + addr);
		this.addr = addr;
	}
    
    UDPReplyHandler(InetAddress addr, int port, UDPService udpService) {
        this(new InetSocketAddress(addr, port), udpService);
    }
    
	/**
	 * Sends the <tt>PingReply</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param hit the <tt>PingReply</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void handlePingReply(PingReply pong, ReplyHandler handler) {
        udpService.send(pong, addr);
	}

	/**
	 * Sends the <tt>QueryReply</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param hit the <tt>QueryReply</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void handleQueryReply(QueryReply hit, ReplyHandler handler) {
        udpService.send(hit, addr);
	}

	/**
	 * Sends the <tt>QueryRequest</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param request the <tt>QueryRequest</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void handlePushRequest(PushRequest request, ReplyHandler handler) {
        udpService.send(request, addr);
	}

	public void countDroppedMessage() {}

	public boolean isOpen() {
		return true;
	}

	public int getNumMessagesReceived() {
		return 0;
	}

	public boolean isOutgoing() {
		return false;
	}

	// inherit doc comment
	public boolean isKillable() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt>.  This always returns <tt>false</tt>
	 * for UDP reply handlers, as leaves are always connected via TCP.
	 *
	 * @return <tt>false</tt>, as all leaves are connected via TCP, so
	 *  directly connected leaves will not have <tt>UDPReplyHandler</tt>s
	 */
	public boolean isSupernodeClientConnection() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt> interface.  Always returns 
	 * <tt>false</tt> because leaves are connected via TCP, not UDP.
	 *
	 * @return <tt>false</tt>, since leaves never maintain their connections
	 *  via UDP, only TCP
	 */
	public boolean isLeafConnection() {
		return false;
	}

	/**
	 * Returns whether or not this connection is a high-degree connection,
	 * meaning that it maintains a high number of intra-Ultrapeer connections.
	 * In the case of UDP reply handlers, this always returns <tt>false<tt>.
	 *
	 * @return <tt>false</tt> because, by definition, a UDP 'connection' is not
	 *  a connection at all
	 */
	public boolean isHighDegreeConnection() {
		return false;
	}

    /**
     * Returns <tt>false</tt> since UDP reply handlers are not TCP 
     * connections in the first place.
     *
     * @return <tt>false</tt>, since UDP handlers are not connections in
     *  the first place, and therefore cannot use Ultrapeer query routing
     */
    public boolean isUltrapeerQueryRoutingConnection() {
        return false;
    }


    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    public boolean isGoodUltrapeer() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    public boolean isGoodLeaf() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, since we don't know whether a host 
     * communicating via UDP supports pong caching or not.
     *
     * @return <tt>false</tt> since we don't know if this node supports
     *  pong caching or not
     */
    public boolean supportsPongCaching() {
        return false;
    }

    /**
     * Returns whether or not to allow new pings from this <tt>ReplyHandler</tt>.
     * Since this ping is over UDP, we'll always allow it.
     *
     * @return <tt>true</tt> since this ping is received over UDP
     */
    public boolean allowNewPings() {
        return true;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return addr;
    }

    // inherit doc comment
    public InetAddress getInetAddress() {
        return addr.getAddress();
    }
    
    /**
     * Retrieves the host address.
     */
    public String getAddress() {
        return addr.getAddress().getHostAddress();
    }

    /**
     * Returns <tt>false</tt> to indicate that <tt>UDPReplyHandler</tt>s 
     * should never be considered stable, due to data loss over UDP and lack
     * of knowledge as to whether the host is still alive.
     *
     * @return <tt>false</tt> since UDP handler are never stable
     */
    public boolean isStable() {
        return false;
    }

    /**
     * implementation of interface. this is not used.
     */
    public String getLocalePref() {
        return ApplicationSettings.DEFAULT_LOCALE.get();
    }

	/**
	 * Overrides toString to print out more detailed information about
	 * this <tt>UDPReplyHandler</tt>
	 */
	@Override
    public String toString() {
		return addr.toString();
	}
	
	/**
	 * sends the response through udp back to the requesting party
	 */
	public void handleUDPCrawlerPong(UDPCrawlerPong m) {
		udpService.send(m, addr);
	}
	
	public void reply(Message m) {
	    udpService.send(m, addr);
	}
	
	public int getPort() {
		return addr.getPort();
	}
	
	public byte[] getClientGUID() {
	    return DataUtils.EMPTY_GUID;
	}
}
