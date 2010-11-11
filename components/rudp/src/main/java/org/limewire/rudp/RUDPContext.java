package org.limewire.rudp;

import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.messages.RUDPMessageFactory;

/** Defines the interface of a mediator to retrieve necessary RUDP components, 
 * including RUDP messages and listener of events. 
 * */
public interface RUDPContext {

    /** The message factory from which RUDP messages should be created. */
    public RUDPMessageFactory getMessageFactory();

    /** The <code>TransportListener</code> which should be notified when events 
     * are pending. */
    public TransportListener getTransportListener();
    
    /** The <code>UDPService</code> used to send messages and know about UDP listening ports. */
    public UDPService getUDPService();
    
    /** The <code>RUDPSettings</code> to use controlling the algorithm. */
    public RUDPSettings getRUDPSettings();

}