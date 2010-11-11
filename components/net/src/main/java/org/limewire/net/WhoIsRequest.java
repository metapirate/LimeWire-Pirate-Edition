package org.limewire.net;

import java.io.IOException;

import org.limewire.io.IP;

/**
 * Represents a WHOIS request. The request value will be
 * sent to the appropriate whois server, and the response
 * will be parsed for name:value pairs.
 * 
 * Some whois servers merely report the authoritative
 * server for a given domain, and we support ONE level of
 * forwarding here.
 * 
 */
public interface WhoIsRequest {
    
    public void doRequest() throws IOException;
    
    public IP getNetRange ();
    
}
