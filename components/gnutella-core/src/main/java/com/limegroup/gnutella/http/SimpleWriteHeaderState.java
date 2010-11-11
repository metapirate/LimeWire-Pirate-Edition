package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.http.Header;
import org.limewire.util.StringUtils;


public class SimpleWriteHeaderState extends WriteHeadersIOState {
    
    private final String connectLine;
    private final List<? extends Header> headers;

    public SimpleWriteHeaderState(String connectLine,
                                  List<? extends Header> headers) {
        super();
        this.connectLine = connectLine;
        this.headers = headers;
    }

    @Override
    protected ByteBuffer createOutgoingData() throws IOException {
        StringBuilder sb = new StringBuilder(connectLine.length() + headers.size() * 25);
        sb.append(connectLine).append("\r\n");
        for(Header header : headers)
            sb.append(HTTPUtils.createHeader(header.getName(), header.getValue()));
        sb.append("\r\n");
        return ByteBuffer.wrap(StringUtils.toAsciiBytes(sb.toString()));
    }

    @Override
    protected void processWrittenHeaders() throws IOException {
        // does nothing.
    }

}
