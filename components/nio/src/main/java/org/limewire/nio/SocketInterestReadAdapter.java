package org.limewire.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

import org.limewire.nio.channel.InterestScatteringByteChannel;

/**
 * Adapter that forwards InterestReadChannel.interest(..)
 * calls on to NIODispatcher.  All ReadableByteChannel
 * calls are delegated to the SocketChannel.
 */
class SocketInterestReadAdapter implements InterestScatteringByteChannel {
    
	/** Mask OOM as this exception */
	private static final IOException OOM = new IOException("Out Of Memory");
	
    /** the SocketChannel this is proxying. */
    private SocketChannel channel;

    SocketInterestReadAdapter(SocketChannel channel) {
        this.channel = channel;
    }

    public void interestRead(boolean status) {
        NIODispatcher.instance().interestRead(channel, status);
    }

    public int read(ByteBuffer dst) throws IOException {
    	try {
    		return channel.read(dst);
    	} catch (OutOfMemoryError oom) {
    		// gc-ing will stall the NIODispatcher thread
    		// but otherwise masking the oom is not very helpful
    		System.gc();  
    		throw OOM;
    	}
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public void close() throws IOException {
        channel.close();
    }
    
    ReadableByteChannel getChannel() {
        return channel;
    }

	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return channel.read(dsts, offset, length);
	}

	public long read(ByteBuffer[] dsts) throws IOException {
		return channel.read(dsts);
	}

    @Override
    public String toString() {
        return "SocketInterestReadAdapter: " + channel;
    }
}
