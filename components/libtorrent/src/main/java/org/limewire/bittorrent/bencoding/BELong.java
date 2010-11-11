package org.limewire.bittorrent.bencoding;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;

/**
 * A token used for the parsing of a Long value.
 * Values over Long.MAX_VALUE throw an <code>IOException</code>.
 */
class BELong extends NumberToken<Long> {
    
    private static final BigInteger MAX = BigInteger.valueOf(Long.MAX_VALUE);
    
    public BELong(ReadableByteChannel chan, byte terminator, byte firstByte) {
        super(chan, terminator, firstByte);
    }

    public BELong(ReadableByteChannel chan) {
        super(chan);
    }

    @Override
    public int getType() {
        return LONG;
    }
    
    @Override
    protected Long getResult(BigInteger rawValue) throws IOException {
        if (rawValue.compareTo(MAX) > 0)
            throw new IOException("too big");
        return rawValue.longValue() * multiplier;
    }
}
