package com.limegroup.gnutella.http;

import java.util.Collection;

public class HTTPHeaderValueCollection implements HTTPHeaderValue {
    
    private final Collection<? extends HTTPHeaderValue> _delegate;
    
    public HTTPHeaderValueCollection(Collection<? extends HTTPHeaderValue> d) {
        _delegate = d;
    }

    public String httpStringValue() {
        StringBuilder writeBuffer = new StringBuilder();
		boolean wrote = false;
        
        for(HTTPHeaderValue value : _delegate) {
            writeBuffer.append(value.httpStringValue()).append(",");
            wrote = true;
        }
        
		// Truncate the last comma from the buffer.
		// This is arguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-1);		    
		return writeBuffer.toString();
    }

}
