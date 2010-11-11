package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import com.limegroup.gnutella.http.HeaderSupport;

class HandshakeSupport extends HeaderSupport {

    /** Connection string. */
    private String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";
    
    /** Valid connect line string. */
    private static final String GNUTELLA_06 = "GNUTELLA/0.6";
    
    /** Gnutella 0.6 accept connection string. */
    private static final String CONNECT = "CONNECT/";
    
    /** End of line for Gnutella 0.6 */
    private static final String CRLF = "\r\n";
    
    /** All headers we've read from the remote side. */
    private final Properties readHeaders;
    
    /** All headers we wrote to the remote side. */
    private final Properties writtenHeaders;
    
    /** The remote address to use when processing a sent header. */
    private final String remoteAddress;
    
    /** The connectLine used in the remote response. */
    private String remoteResponse;
    
    HandshakeSupport(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        this.readHeaders = new Properties();
        this.writtenHeaders = new Properties();
    }

    /** Creates their response, based on the given connectLine. */
    HandshakeResponse createRemoteResponse(String connectLine) throws IOException {
        this.remoteResponse = connectLine.substring(GNUTELLA_06.length()).trim();
        return HandshakeResponse.createRemoteResponse(remoteResponse, readHeaders);
    }
    
    /** Appends the connect line to the given StringBuffer. */
    void appendConnectLine(StringBuilder sb) {
        sb.append(GNUTELLA_CONNECT_06).append(CRLF);
    }
    
    /** Appends a response (using the given response) to the given StringBuffer */
    void appendResponse(HandshakeResponse response, StringBuilder sb) {        
        sb.append(GNUTELLA_06).append(" ").append(response.getStatusLine()).append(CRLF);
        appendHeaders(response.props(), sb);
    }
    
    /**
     * Appends the given properties to the given StringBuffer.
     * 
     * @param props The headers to be sent. Note: null argument is acceptable,
     *  if no headers need to be sent the trailer will be sent.
     */
    void appendHeaders(Properties props, StringBuilder sb)  {
        if (props != null) {
            Enumeration names = props.propertyNames();
            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();
                String value = props.getProperty(key);
                String toWrite = processKeyValueForWriting(key, value);
                sb.append(toWrite);
            }
        }
        sb.append(CRLF);
    }

    /** Determines if the given connect line is valid. */
    boolean isConnectLineValid(String s) {
        return s.startsWith(GNUTELLA_06);
    }

    /**
     * Returns true iff line ends with "CONNECT/N", where N is a number greater than or equal "0.6".
     */
    boolean notLessThan06(String line) {
        int i = line.indexOf(CONNECT);
        if (i < 0)
            return false;
        try {
            float f = Float.parseFloat(line.substring(i + CONNECT.length()));
            return f >= 0.6f;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /** 
     * Process a single read header.
     * Returns true if this wasn't a blank line and more headers are expected,
     * returns false if this was a blank line and no more headers are expected.
     */
    @Override
    public boolean processReadHeader(String line) {
        if(line.equals(""))
            return false;
        
        int i = line.indexOf(':');
        if (i > 0) {
            String key = line.substring(0, i);
            String value = line.substring(i + 1).trim();
            readHeaders.put(key, value);
        }
        return true;
    }
    
    /**
     * Process a key/value pair for writing.
     * May change the value.
     * Returns what should be sent over the wire.
     */
    String processKeyValueForWriting(String key, String value) {
        // Ensure we put their remote-ip correctly.
        if (HeaderNames.REMOTE_IP.equals(key))
            value = remoteAddress;
        if (value == null)
            value = "";
        writtenHeaders.put(key, value);
        return key + ": " + value + CRLF;
    }

    
    /** Returns the number of headers we've read so far. */
    @Override
    public int getHeadersReadSize() {
        return readHeaders.size();
    }
    
    /** Constructs a HandshakeResponse object using the remote response data. */
    HandshakeResponse getReadHandshakeRemoteResponse() throws IOException {
        return HandshakeResponse.createRemoteResponse(remoteResponse, readHeaders);
    }

    /** Constructs a HandshakeResponse object wrapping the headers we've read. */
    HandshakeResponse getReadHandshakeResponse() {
        return HandshakeResponse.createResponse(readHeaders);
    }

    /** Constructs a HandshakeResponse object wrapping the headers we've written. */
    HandshakeResponse getWrittenHandshakeResponse() {
        return HandshakeResponse.createResponse(writtenHeaders);
    }

}
