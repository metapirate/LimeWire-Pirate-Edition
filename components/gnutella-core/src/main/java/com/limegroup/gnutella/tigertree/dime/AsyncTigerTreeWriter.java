package com.limegroup.gnutella.tigertree.dime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.limewire.nio.statemachine.WriteState;

import com.limegroup.gnutella.dime.AsyncDimeWriter;
import com.limegroup.gnutella.tigertree.ThexWriter;

public class AsyncTigerTreeWriter extends WriteState implements ThexWriter {

    private AsyncDimeWriter writer;

    public AsyncTigerTreeWriter(AsyncDimeWriter asyncDimeWriter) {
        this.writer = asyncDimeWriter;
    }

    @Override
    protected boolean processWrite(WritableByteChannel channel,
            ByteBuffer buffer) throws IOException {
        return writer.process(channel, buffer);
    }

    public long getAmountProcessed() {
        return writer.getAmountProcessed();
    }
    
}
