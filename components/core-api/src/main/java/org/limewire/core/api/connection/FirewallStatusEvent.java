package org.limewire.core.api.connection;

import org.limewire.listener.DefaultDataEvent;

/**
 * Event to broadcast changes to this nodes firewall status.  Specifically, 
 *  if this client is behind or being affected by a firewall.
 */
public class FirewallStatusEvent extends DefaultDataEvent<FirewallStatus> {

    public FirewallStatusEvent(FirewallStatus data) {
        super(data);
    }
}
