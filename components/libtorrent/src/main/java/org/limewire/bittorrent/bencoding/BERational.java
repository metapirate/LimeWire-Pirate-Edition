package org.limewire.bittorrent.bencoding;

import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;


public class BERational extends NumberToken<Double> {
    BERational(ReadableByteChannel chan) {
        super(chan);
    }
    
    @Override
    public int getType() {
        return RATIONAL;
    }
    
    @Override
    protected Double getResult(BigInteger rawValue) {
        double ret = Double.longBitsToDouble(rawValue.longValue());
        return ret * multiplier;
    }
}
