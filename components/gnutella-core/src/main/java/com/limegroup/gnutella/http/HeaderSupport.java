package com.limegroup.gnutella.http;

import java.util.HashMap;
import java.util.Map;

public class HeaderSupport {

    private Map<String, String> headers = new HashMap<String, String>();
    
    /** 
     * Process a single read header.
     * Returns true if this wasn't a blank line and more headers are expected,
     * returns false if this was a blank line and no more headers are expected.
     */
    public boolean processReadHeader(String line) {
        if(line.equals(""))
            return false;
        
        int i = line.indexOf(':');
        if (i > 0) {
            String key = line.substring(0, i);
            String value = line.substring(i + 1).trim();
            headers.put(key, value);
        }
        return true;
    }
    
    /** Returns the number of headers we've read so far. */
    public int getHeadersReadSize() {
        return headers.size();
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }

}
