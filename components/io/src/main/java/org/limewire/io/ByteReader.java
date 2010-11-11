package org.limewire.io;

import java.io.IOException;
import java.io.InputStream;

/** 
 * Provides the readLine method of a BufferedReader with no no automatic
 * buffering.  All methods are like those in InputStream except they return
 * -1 instead of throwing IOException.
 * <p>
 * This also catches ArrayIndexOutOfBoundsExceptions while reading, as this
 * exception can be thrown from native socket code on windows occasionally.
 * The exception is treated exactly like an IOException.
 * <p>
 * Is only guaranteed to handle single byte string decodings correctly.
 */
public class ByteReader {

    private static final byte R = '\r';
    private static final byte N = '\n';

    private InputStream _istream;
    
    public ByteReader(InputStream stream) {
        _istream = stream;
    }

    public void close() {
        try {
            _istream.close();
        } catch (IOException ignored) {
        }
    }

    public int read() {

        int c = -1;
    
        if (_istream == null)
            return c;
    
        try {
            c =  _istream.read();
        } catch(IOException ignored) {
            // return -1
        } catch(ArrayIndexOutOfBoundsException ignored) {
            // return -1
        }
        return c;
    }

    public int read(byte[] buf) {
        int c = -1;

        if (_istream == null) {
            return c;
        }

        try {
            c = _istream.read(buf);
        } catch(IOException ignored) {
            // return -1
        } catch(ArrayIndexOutOfBoundsException ignored) {
            // return -1
        }
        return c;
    }

    public int read(byte[] buf, int offset, int length) {
        int c = -1;

        if (_istream == null) {
            return c;
        }

        try {
            c = _istream.read(buf, offset, length);
        } catch(IOException ignored) {
            // return -1
        } catch(ArrayIndexOutOfBoundsException ignored) {
            // happens on windows machines occasionally.
            // return -1
        }
        return c;
    }

    /** 
     * Reads a new line WITHOUT end of line characters.  A line is 
     * defined as a minimal sequence of character ending with "\n", with
     * all "\r"'s thrown away.  Hence calling readLine on a stream
     * containing "abc\r\n" or "a\rbc\n" will return "abc".
     * <p>
     * Throws IOException if there is an IO error.  Returns null if
     * there are no more lines to read, i.e., EOF has been reached.
     * Note that calling readLine on "ab<EOF>" returns null.
     */
    public String readLine() throws IOException {
        if (_istream == null)
            return "";

        StringBuilder sBuffer = new StringBuilder();
        int c = -1; //the character just read
        boolean keepReading = true;
        
        do {
            try {
                c = _istream.read();
            } catch(ArrayIndexOutOfBoundsException aiooe) {
                // this is apparently thrown under strange circumstances.
                // interpret as an IOException.
                throw new IOException("aiooe.");
            }
            switch(c) {
                // if this was a \n character, break out of the reading loop
                case  N: keepReading = false;
                         break;
                // if this was a \r character, ignore it.
                case  R: continue;
                // if we reached an EOF ...
                case -1: return null;			             
                // if it was any other character, append it to the buffer.
                default: sBuffer.append((char)c);
            }
        } while(keepReading);

        // return the string we have read.
        return sBuffer.toString();
    }
}
