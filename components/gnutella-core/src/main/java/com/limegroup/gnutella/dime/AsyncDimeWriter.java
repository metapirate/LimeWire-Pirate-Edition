package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import org.limewire.io.ByteBufferOutputStream;
import org.limewire.nio.statemachine.WriteState;

import com.limegroup.gnutella.connection.MessageWriter;

/**
 * Uses NIO to write a list of {@link DIMERecord} objects to a channel. Based on
 * {@link MessageWriter}.
 */
public class AsyncDimeWriter extends WriteState {
    
    private final Iterator<DIMERecord> recordIt;

    /**
     * The OutputStream that messages are written to.  For efficieny, the stream
     * internally uses a ByteBuffer and we get the buffer directly to write to
     * our sink channel.  This prevents recreation of many byte[]s.
     */
    private final ByteBufferOutputStream out;

    public boolean isFirstRecord = true;

    private long amountProcessed;

    private boolean flipped;
    
    public AsyncDimeWriter(List<DIMERecord> records) {
        this.recordIt = records.iterator();
        this.out = new ByteBufferOutputStream();
    }
    
    @Override
    protected boolean processWrite(WritableByteChannel channel,
            ByteBuffer buffer) throws IOException {
        // first try to write any leftover data.
        if(writeRemaining(channel)) //still have data to send.
            return true;
            
        boolean isLastRecord = !recordIt.hasNext(); 
        while (!isLastRecord) {
            DIMERecord current = recordIt.next();
            if (isFirstRecord) {
                current.setFirstRecord(true);
                isFirstRecord = false;
            } else {
                current.setFirstRecord(false);
            }

            isLastRecord = !recordIt.hasNext();
            current.setLastRecord(isLastRecord);
            current.write(out);

            if (writeRemaining(channel)) // still have data to send.
                return true;
        }
        return false;
    }

    public long getAmountProcessed() {
        return amountProcessed;
    }
        
    /**
     * Writes any data that was left in the buffer.  As an optimization,
     * we do not recompact the buffer if more data can be written.  Instead,
     * we just wait till we can completely write the buffer & then clear it
     * entirely.  This prevents the need to compact the buffer.
     * @param channel 
     */
    private boolean writeRemaining(WritableByteChannel channel) throws IOException {
        // if there was data left in the stream, try writing it.
        ByteBuffer buffer = out.getBuffer();
        
        // write any data that was leftover in the buffer.
        if (flipped || buffer.position() > 0) {
            // prepare for writing...
            if(!flipped) {
                buffer.flip();
                flipped = true;
            }

            // write.
            int written = channel.write(buffer);
            amountProcessed += written;
            
            // if we couldn't write everything, exit.
            if(buffer.hasRemaining())
                return true; // still have data to write.
                
            flipped = false;
            buffer.clear();
        }
        return false; // wrote everything.
    }
    
}
