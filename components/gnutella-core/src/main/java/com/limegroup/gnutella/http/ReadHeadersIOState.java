package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.statemachine.ReadState;
import org.limewire.util.BufferUtils;


public abstract class ReadHeadersIOState extends ReadState {
    
    private static final Log LOG = LogFactory.getLog(ReadHeadersIOState.class);

    /** Header support. */
    protected final HeaderSupport support;
    /** The maximum size of a header we'll read. */
    private final int maxHeaderSize;
    /** The maximum number of headers we'll read. */
    private final int maxHeaders;
    
    /** Whether or not we've finished reading the initial connect line. */
    protected boolean doneConnect;
    /** The current header we're in the process of reading. */
    protected StringBuilder currentHeader = new StringBuilder(1024);
    /** The connect line. */
    protected String connectLine;
    /** The amount of data read. */
    private long amountRead;
        
    /** Constructs a new ReadHandshakeState using the given support & stat. */
    public ReadHeadersIOState(HeaderSupport support,
                              int maxHeaders, int maxHeaderSize) {
        this.support = support;
        this.maxHeaders = maxHeaders;
        this.maxHeaderSize = maxHeaderSize;
    }

    /**
     * Reads as much data as it can from the buffer, farming the processing of the
     * connect line (same as response line) and headers out to the methods:
     * <pre>
     *   processConnectLine(String line)
     *   processHeaders()
     * </pre>
     * This will return true if it needs to be called again for more processing,
     * otherwise it will return false indicating it's time to move on to the next
     * state.
     */
    @Override
    protected boolean processRead(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
        boolean allDone = false;
        while(!allDone) {
            int read = 0;
            
            while(buffer.hasRemaining() && (read = rc.read(buffer)) > 0) {
                amountRead += read;
            }
            
            if(buffer.position() == 0) {
                if(read == -1)
                    throw new IOException("EOF");
                break;
            }
            
            buffer.flip();
            if(!doneConnect) {
                if(BufferUtils.readLine(buffer, currentHeader)) {
                    connectLine = currentHeader.toString();
                    if(LOG.isDebugEnabled())
                        LOG.debug("Read connect line: " + connectLine);
                    currentHeader.delete(0, currentHeader.length());
                    processConnectLine();
                    doneConnect = true;
                }
            }
            
            if(doneConnect) {
                while(true) {
                    if(!BufferUtils.readLine(buffer, currentHeader))
                        break;
                    
                    if(LOG.isDebugEnabled())
                        LOG.debug("Read header: " + currentHeader);
                    if(!support.processReadHeader(currentHeader.toString())) {
                        allDone = true;
                        break; // we finished reading this set of headers!
                    }
                    
                    currentHeader.delete(0, currentHeader.length()); // reset for the next header.

                    // Make sure we don't try and read forever.
                    if(support.getHeadersReadSize() > maxHeaders)
                        throw new IOException("too many headers");
                }
            }
            
            buffer.compact();
            
            // Don't allow someone to send us a header so big that we blow up.
            // Note that we don't check this after immediately after creating the
            // header, because it's not really so important there.  We know the
            // data cannot be bigger than the buffer's size, and the buffer's size isn't
            // too extraordinarily large, so this works out okay.
            if(currentHeader.length() > maxHeaderSize)
                throw new IOException("header too big");
        }
        
        if(allDone) {
            processHeaders();
            return false;
        } else {
            return true;
        }
    }
    
    public long getAmountProcessed() {
        return amountRead;
    }
    
    /**
     * Reacts to the connect line, either throwing an IOException if it was invalid
     * or doing nothing if it was valid.
     */
    abstract protected void processConnectLine() throws IOException;
    
    /**
     * Reacts to the event of headers being finished processing.  Throws an IOException
     * if the connection wasn't allowed.
     * 
     * @throws IOException
     */
    abstract protected void processHeaders() throws IOException;

}
