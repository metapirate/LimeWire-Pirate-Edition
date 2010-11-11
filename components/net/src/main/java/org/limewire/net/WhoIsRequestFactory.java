package org.limewire.net;

import java.util.Map;

/**
 * The only parameter for a whois request is the request
 * string itself, which should be a DNS name or an IP
 * address.
 * 
 */
public interface WhoIsRequestFactory {

    public WhoIsRequest createWhoIsRequest (String name);
    
    public WhoIsRequest createWhoIsRequest (String name, Map<String,String> servers);
    
}
