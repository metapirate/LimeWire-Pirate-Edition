package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.Map;

public class SimpleReadHeaderState extends ReadHeadersIOState {
    
    public SimpleReadHeaderState(int maxHeaders, int maxHeaderSize) {
        super(new HeaderSupport(), maxHeaders, maxHeaderSize);
    }

    @Override
    protected void processConnectLine() throws IOException {
        // Does nothing.
    }

    @Override
    protected void processHeaders() throws IOException {
        // Does nothing.
    }
    
    public Map<String, String> getHeaders() {
        return support.getHeaders();
    }
    
    public String getConnectLine() {
        return connectLine;
    }

}
