package org.limewire.bittorrent.bencoding;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.limewire.util.BEncoder;

/**
 * A bencoding Token that represents a string element of bencoded data.
 * <p>
 * In BitTorrent's bencoding, a string data on the wire looks like "5:hello".
 * The length comes first, then a colon, then that number of characters.
 * Bencoded strings hold ASCII text, or data of any format.
 * <p>
 * If bencoded data starts "0" through "9", it's a string.
 * A BEString object can read the rest of it. Calling 
 * <code>beString.getToken()</code> returns a Java String with the payload text.
 */
class BEString extends Token<byte[]> {

	/**
	 * The largest bencoded string we'll read.
	 * 
	 * .torrent files don't have a maximum size, so for now this limit is 
	 * set to 1 MB.
	 * 
	 */
//TODO: Find a proper way to deal with this limit.
    
	private static final int MAX_STRING_SIZE = 1024 * 1024;

    /** 
     * The first byte of the length of the string that was read 
     * from the channel, if any 
     */
    private final byte firstSizeByte;

    /** Token that will be used to parse the string length. */
    private BELong sizeToken;

    /** The parsed length of the string. */
    private int size = -1; // -1 because we haven't read the length prefix yet

    /** Buffer used for internal storage. */
    private ByteBuffer buf;

    /** Empty Buffer to point a reference at. */
    private static final ByteBuffer EMPTY_STRING = ByteBuffer.allocate(0);

    

    /**
     * Makes a new BEString Token ready to parse bencoded string data 
     * from a given ReadableByteChannel.
     * 
     * @param firstChar the first byte we already read from the channel, 
     * it was "0" through "9" indicating this is a string
     * @param chan the ReadableByteChannel the caller read the first 
     * character from, and we can read the remaining characters from
     */
    BEString(byte firstChar, ReadableByteChannel chan) {
        super(chan);
        this.firstSizeByte = firstChar; 
    }

    @Override
    public void handleRead() throws IOException {

    	// If we haven't read the whole length prefix yet, 
    	// try to read more 
        if (size == -1 && !readSize())
            return; // Don't do more until the next read notification

        if (size == 0)
        	return; 
        if (!buf.hasRemaining()) 
            throw new IllegalStateException("Token is done - don't read to it");

        int read = 0;
        while (buf.hasRemaining() && (read = chan.read(buf)) > 0);
        if (read == -1 && buf.hasRemaining())
            throw new EOFException("Could not read String token");
    }

    /**
     * Reads the length prefix at the start of a bencoded string.
     * 
     * @return true if it got it all and set size, false to call 
     * it again to keep reading more
     */
    private boolean readSize() throws IOException {

    	/*
    	 * A bencoded string is like "17:this is the text".
    	 * The length of "this is the text", 17, is written at the start 
    	 * before the colon.
    	 * 
    	 * The first step is to read the length.
    	 * To do this, readSize() makes a new BELong object.
    	 * We give it our channel to read from and tell it to stop when it 
    	 * gets to a ":".
    	 * 
    	 * We call handleRead() on it to get it to read bencoded data from 
    	 * our channel. When it's read the ":", it returns the number it 
    	 * read and parsed as a Long object, and control enters the if 
    	 * statement.
    	 */

    	if (sizeToken == null)
            sizeToken = new BELong(chan, BEncoder.COLON, firstSizeByte);
    	
        sizeToken.handleRead();
        
        Long l = sizeToken.getResult();
        if (l != null) { // Same as size == -1
            sizeToken = null; // We don't need the object anymore
            long l2 = l.longValue();

            // Valid length
            if (l2 > 0 && l2 < MAX_STRING_SIZE) {

            	size = (int)l2;
                result = new byte[size];
                buf = ByteBuffer.wrap(result);
                return true;

            // The bencoded string data is "0:", this is valid
            } else if (l2 == 0) {

            	size = 0;
            	buf = EMPTY_STRING;
            	result = new byte[0];
            	return true;

            } else
                throw new IOException("invalid string length"); // Too big
        } else
            return false; // We're still reading the size, try again next time 
    }

    /**
     * Determines if this is finished reading bencoded data from its channel 
     * and parsing it into a String object.
     * 
     * @return true if it is done, false if it needs more read notifications to 
     * finish
     */
    @Override
    protected boolean isDone() {

    	// We're only done if we parsed the size to make a buffer that big, 
    	// and then filled it
        return buf != null && !buf.hasRemaining();
    }

    /**
     * Hints that this BEString Token object is parsing a bencoded string 
     * into a Java byte [].
     * 
     * @return Token.STRING
     */
    @Override
    public int getType() {
        return STRING;
    }
}
