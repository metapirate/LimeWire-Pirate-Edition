package com.limegroup.gnutella.tigertree.dime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import org.limewire.nio.statemachine.ReadState;

import com.limegroup.gnutella.dime.AsyncDimeParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.tigertree.ThexReader;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeFactory;

class AsyncTigerTreeReader extends ReadState implements ThexReader {
    
    private final String sha1;

    private final long fileSize;

    private final String root32;

    private final AsyncDimeParser parser;

    private final HashTreeFactory tigerTreeFactory;

    public AsyncTigerTreeReader(String sha1, long fileSize, String root32,
            HashTreeFactory tigerTreeFactory) {
        this.sha1 = sha1;
        this.fileSize = fileSize;
        this.root32 = root32;
        this.parser = new AsyncDimeParser();
        this.tigerTreeFactory = tigerTreeFactory;
    }

    @Override
    protected boolean processRead(ReadableByteChannel channel, ByteBuffer buffer)
            throws IOException {
        return parser.process(channel, buffer);
    }

    public HashTree getHashTree() throws IOException {
        List<DIMERecord> records = parser.getRecords();
        return tigerTreeFactory.createHashTree(TigerDimeReadUtils.nodesFromRecords(records.iterator(),
                fileSize, root32), sha1, fileSize);
    }

    public long getAmountProcessed() {
        return parser.getAmountProcessed();
    }
}
