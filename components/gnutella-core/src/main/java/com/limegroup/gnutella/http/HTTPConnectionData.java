package com.limegroup.gnutella.http;

/**
 * Stores connection flags.  
 */
public class HTTPConnectionData {

    private boolean local;
    
    private boolean firewalled;
    
    private boolean push;
    
    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isFirewalled() {
        return firewalled;
    }

    public void setFirewalled(boolean firewalled) {
        this.firewalled = firewalled;
    }

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }
    
}
