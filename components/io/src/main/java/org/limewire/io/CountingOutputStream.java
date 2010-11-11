package org.limewire.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes data and tracks the number of bytes you attempt to write. 
 * Additionally, you can turn off byte counting.
 */
public final class CountingOutputStream extends FilterOutputStream {
    
    private int _count = 0;
    private boolean _isCounting = true;
    
    public CountingOutputStream (final OutputStream out) {
        super(out);
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b);
        if(_isCounting)
            _count++;
        return;
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // do NOT call super.write(b, off, len) as that will call
        // write(b) and double-count each byte.
        out.write(b, off, len);
        if(_isCounting)
            _count += len;
    }
    
    @Override
    public void close() throws IOException {
        out.close();
    }    
    
    public int getAmountWritten() {
        return _count;
    }
    
    public void setIsCounting(boolean count) {
        _isCounting = count;
    }
    
}
