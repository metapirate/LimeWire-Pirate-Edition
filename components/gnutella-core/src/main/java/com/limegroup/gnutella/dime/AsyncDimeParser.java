package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.statemachine.ReadState;


/**
 * Parser for creating DIMERecords from non-blocking input.
 *
 * See: http://www.gotdotnet.com/team/xml_wsspecs/dime/dime.htm
 * (or http://www.perfectxml.com/DIME.asp )
 * for information about DIME.
 */
public class AsyncDimeParser extends ReadState {
    
    private static final Log LOG = LogFactory.getLog(AsyncDimeParser.class);
    
    /** Whether or not we've read the last record. */
    private boolean lastRead = false;
    
    /** A list of DIMERecords this has read. */
    private List<DIMERecord> records = new LinkedList<DIMERecord>();
    
    private long amountRead = 0;
    
    /** The AsyncDimeRecordReader we're using to read the current record. */
    private AsyncDimeRecordReader reader;

    @Override
    protected boolean processRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        while(true) {
            if(lastRead) 
                throw new IOException("already read last message.");
            
            if(reader == null)
                reader = new AsyncDimeRecordReader();
            
            // Reader still needs more info.
            try {
                if(reader.process(channel, buffer))
                    return true;
            } catch(DIMEException de) {
                LOG.warn("Error processing DIME", de);
                amountRead += reader.getAmountProcessed();
                return false;
            }

            amountRead += reader.getAmountProcessed();
            
            // Reader's read a full record.
            DIMERecord next;
            try {
                next = reader.getRecord();
            } catch(DIMEException de) {
                LOG.warn("Error constructing DIME record", de);
                return false;
            }
            
            if(next.isLastRecord())
                lastRead = true;
                
            if(records.isEmpty() && !next.isFirstRecord())
                throw new IOException("middle of stream.");
            else if(!records.isEmpty() && next.isFirstRecord())
                throw new IOException("two first records.");
            
            records.add(next);
            
            reader = null;
            
            if(lastRead)
                return false;
        }
    }
    
    public List<DIMERecord> getRecords() {
        return records;
    }

    public long getAmountProcessed() {
        return amountRead;
    }
}
