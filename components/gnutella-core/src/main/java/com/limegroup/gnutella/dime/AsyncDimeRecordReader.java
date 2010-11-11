package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.statemachine.ReadState;
import org.limewire.util.BufferUtils;
import org.limewire.util.ByteUtils;


public class AsyncDimeRecordReader extends ReadState {
    
    private static final Log LOG = LogFactory.getLog(AsyncDimeRecordReader.class);
    
    private ByteBuffer header;
    private static final int OPTIONS = 0;
    private static final int OPTIONS_P = 1;
    private static final int ID = 2;
    private static final int ID_P = 3;
    private static final int TYPE = 4;
    private static final int TYPE_P = 5;
    private static final int DATA = 6;
    private static final int DATA_P = 7;
    private static final int TOTAL = 8;
    private ByteBuffer[] parts;
    
    public AsyncDimeRecordReader() {
        header = ByteBuffer.allocate(12);
    }
    
    /**
     * Returns the next record if it can be constructed or null if it isn't
     * processed yet.
     * 
     * @throws IOException
     */
    public DIMERecord getRecord() throws DIMEException {
        if(parts == null || parts[DATA].hasRemaining() || parts[DATA_P].hasRemaining()) {
            return null;
        } else {
            try {
            return new DIMERecord(header.get(0), header.get(1),
                                  parts[OPTIONS].array(), parts[ID].array(),
                                  parts[TYPE].array(), parts[DATA].array());
            } catch(IllegalArgumentException iae) {
                throw new DIMEException(iae);
            }
        }
    }

    @Override
    protected boolean processRead(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
        // Header must be completely read before continuing...
        if(fill(header, rc, buffer)) {
            LOG.debug("Header not full, leaving.");
            return true;
        }
        
        // If we haven't created things yet, create them.
        if(parts == null)
            createOtherStructures();

        for(int i = 0; i < TOTAL; i++) {
            if(i == 0 || !parts[i-1].hasRemaining()) {
                if(fill(parts[i], rc, buffer))
                    return true;
            }
        }
        
        return false;
    }
    
    /**
     * Attempts to read as much data as possible into 'current'.
     * Data will be transferred from 'buffer' into 'current' and then
     * read from 'channel' into 'current'.
     * 
     * @throws IOException if more data needs to be read but the last read returned -1
     * @return true if current still has space to read
     */
    private boolean fill(ByteBuffer current, ReadableByteChannel rc, ByteBuffer buffer) throws IOException {        
        int read = BufferUtils.readAll(buffer, rc, current);
        LOG.debug("Filling current.  Left: " + current.remaining());
        if(current.hasRemaining()) {
            if(read == -1)
                throw new IOException("EOF");
            else
                return true;
        } else {
            return false;
        }
    }
    
    /**
     * Validates the header bytes & constructs options, id, type and data.
     * @throws IOException
     */
    private void createOtherStructures() throws DIMEException {
        try {
            DIMERecord.validateFirstBytes(header.get(0), header.get(1));
        } catch (IllegalArgumentException iae) {
            throw new DIMEException(iae);
        }

        byte[] headerArr = header.array();
        int optionsLength = ByteUtils.beb2int(headerArr, 2, 2);
        int idLength = ByteUtils.beb2int(headerArr, 4, 2);
        int typeLength = ByteUtils.beb2int(headerArr, 6, 2);
        int dataLength = ByteUtils.beb2int(headerArr, 8, 4);

        if(LOG.isDebugEnabled()) {
            LOG.debug("creating dime record." + 
                      "  optionsLength: " + optionsLength +
                      ", idLength: " + idLength +
                      ", typeLength: " + typeLength + 
                      ", dataLength: " + dataLength);
        }

        // The DIME specification allows this to be a 32-bit unsigned field,
        // which in Java would be a long -- but in order to hold the array
        // of the data, we can only read up to 16 unsigned bits (an int), in order
        // to size the array correctly.
        if (dataLength < 0)
            throw new DIMEException("data too big.");

        parts = new ByteBuffer[TOTAL];
        parts[OPTIONS]   = createBuffer(optionsLength);
        parts[OPTIONS_P] = createBuffer(DIMERecord.calculatePaddingLength(optionsLength)); 
        parts[ID]        = createBuffer(idLength);
        parts[ID_P]      = createBuffer(DIMERecord.calculatePaddingLength(idLength));
        parts[TYPE]      = createBuffer(typeLength);
        parts[TYPE_P]    = createBuffer(DIMERecord.calculatePaddingLength(typeLength));
        parts[DATA]      = createBuffer(dataLength);
        parts[DATA_P]    = createBuffer(DIMERecord.calculatePaddingLength(dataLength));
    }
    
    private ByteBuffer createBuffer(int length) {
        if(length == 0)
            return BufferUtils.getEmptyBuffer();
        else
            return ByteBuffer.allocate(length);
    }

    public long getAmountProcessed() {
        long read = header.position();
        if(parts != null) {
            for(int i = 0; i < TOTAL; i++)
                read += parts[i].position();
        }
        return read;
    }

}
