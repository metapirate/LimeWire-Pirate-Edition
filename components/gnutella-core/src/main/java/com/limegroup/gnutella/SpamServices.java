package com.limegroup.gnutella;

import java.net.InetAddress;

import com.limegroup.gnutella.messages.Message;

public interface SpamServices {

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public void adjustSpamFilters();

    /**
     * Reloads the IP filter and calls <code>adjustSpamFilters()</code> when
     * finished.
     */
    public void reloadIPFilter();
    
    /**
     * Reloads the IP and URN filters and calls <code>adjustSpamFilters()</code>
     * when finished.
     */
    public void reloadSpamFilters();

    public void blockHost(String host);
    
    public boolean isAllowed(InetAddress host);

    public void unblockHost(String host);
    
    /** Returns true if the message is spam according to the personal filter. */
    public boolean isPersonalSpam(Message m);
}