package org.limewire.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.collection.Buffer;
import org.limewire.nio.observer.WriteObserver;

/** A simple writer that maintains statistics about how much was written. */
public class StatisticGatheringWriter extends AbstractChannelInterestWriter {

    private final static long NANO_START = System.nanoTime();
    private static final int HISTORY = 50;

    private final Buffer<Long> handleWrites = new Buffer<Long>(HISTORY);
    private final Buffer<Long> interestWrites = new Buffer<Long>(HISTORY);
    private final Buffer<Boolean> interestWritesStatus = new Buffer<Boolean>(HISTORY);
    private final Buffer<Long> writeTimes = new Buffer<Long>(HISTORY);
    private final Buffer<Long> writeAmounts = new Buffer<Long>(HISTORY);
    
    private long amountWrote, totalHandleWrite, totalInterestWrite, positiveInterestWrite;
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        writeTimes.add(System.nanoTime() - NANO_START);
        int wrote = super.write(src);
        amountWrote += wrote;
        writeAmounts.add((long)wrote);
        return wrote;
    }
    
    @Override
    public boolean handleWrite() throws IOException {
        handleWrites.add(System.nanoTime() - NANO_START);
        totalHandleWrite++;
        return super.handleWrite();
    }

    @Override
    public void interestWrite(WriteObserver observer, boolean status) {
        interestWrites.add(System.nanoTime() - NANO_START);
        interestWritesStatus.add(status);
        totalInterestWrite++;
        if (status)
            positiveInterestWrite++;
        super.interestWrite(observer, status);
    }
}
