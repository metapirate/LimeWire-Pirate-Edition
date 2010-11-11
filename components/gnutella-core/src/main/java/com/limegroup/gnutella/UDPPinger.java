package com.limegroup.gnutella;

import java.util.Collection;

import org.limewire.collection.Cancellable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.messages.Message;

public interface UDPPinger {

    /**
     * Ranks the specified Collection of hosts with the given
     * Canceller.
     */
    public void rank(Collection<? extends IpPort> hosts, Cancellable canceller);

    /**
     * Ranks the specified <tt>Collection</tt> of hosts with the given
     * MessageListener, Cancellable and Message.
     * 
     */
    public void rank(final Collection<? extends IpPort> hosts,
            final MessageListener listener, Cancellable canceller,
            final Message message);

    /**
     * Ranks the specified <tt>Collection</tt> of hosts.
     * 
     * If expireTime is < 0, the default expiry time for the message 
     * is DEFAULT_LISTEN_EXPIRE_TIME
     * 
     * @param hosts the <tt>Collection</tt> of hosts to rank
     * @param listener a MessageListener if you want to spy on the message.  can
     * be null.
     * @param canceller a Cancellable that can short-circuit the sending
     * @param message the message to send, can be null. 
     * @param expireTime The expiry time of the message. If this is < 0, takes the 
     * DEFAULT_LISTEN_EXPIRE_TIME value.
     * @return a new <tt>UDPHostRanker</tt> instance
     * @throws <tt>NullPointerException</tt> if the hosts argument is 
     *  <tt>null</tt>
     */
    public void rank(final Collection<? extends IpPort> hosts,
            final MessageListener listener, Cancellable canceller,
            final Message message, int expireTime);

}