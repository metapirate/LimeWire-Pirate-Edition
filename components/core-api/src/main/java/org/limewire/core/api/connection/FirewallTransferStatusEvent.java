package org.limewire.core.api.connection;

import org.limewire.listener.DefaultDataTypeEvent;

/**
 * An event type that is used to broadcast changes to {@link FirewallTransferStatus} for this node.  Namely
 *  if this client can or can not do FWT and the reason why. 
 */
public class FirewallTransferStatusEvent extends DefaultDataTypeEvent<FirewallTransferStatus, FWTStatusReason> {

    public FirewallTransferStatusEvent(FirewallTransferStatus status, FWTStatusReason reason) {
        super(status, reason);
    }
}
