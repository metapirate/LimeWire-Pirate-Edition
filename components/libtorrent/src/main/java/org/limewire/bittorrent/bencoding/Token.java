package org.limewire.bittorrent.bencoding;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.limewire.util.BEncoder;


/**
 * Provides common functionality for objects that represent pieces of bencoded data.
 * <p>
 * Reads bencoded data and parses it into objects that extend <code>Token</code>, 
 * like <code>BEString</code> and <code>BEList</code>.
 * Use the factory method <code>Token.getNextToken(ReadableByteChannel)</code> 
 * to get a parsed <code>Token</code> object.
 * <p>
 * BitTorrent uses a simple and extensible data format called bencoding.
 * More information about bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding and
 * http://www.bittorrent.org/beps/bep_0003.html#the-connectivity-is-as-follows.
 */
//TODO: Write the steps to parse bencoded data.
public abstract class Token<T> {

    /** An undefined Token. */
    protected static final int INTERNAL = -1;
    /** A number Token. */
    public static final int LONG = 0;
    /** A string Token. */
    public static final int STRING = 1;
    /** A list Token. */
    public static final int LIST = 2;
    /** A dictionary Token. */
    public static final int DICTIONARY = 3;
    /** A boolean Token */
    public static final int BOOLEAN = 4;
    /** A rational Token */
    public static final int RATIONAL = 5;

    /** The normal ACSII text encoding to use in bencoding for BitTorrent. */
    protected static final String ASCII = "ISO-8859-1";



    // When parsing ASCII characters, ZERO and NINE are used to see if a character 
    //like '5' is between them, and thus a number
    protected static final byte ZERO, NINE;

    static {
        byte zero = 0;
        byte nine = 0;

        try {

            zero = "0".getBytes(ASCII)[0];
            nine = "9".getBytes(ASCII)[0];

        } catch (UnsupportedEncodingException impossible) {
            throw new RuntimeException(impossible);
        }

        ZERO = zero;
        NINE = nine;
    }

    /** The channel this Token reads bencoded data from. */
    protected final ReadableByteChannel chan;

    /** The parsed Java object this Token made from the bencoded data it read. */
    protected T result;

    /**
     * Makes a new object to represent a bencoded token to be read and parsed.
     * 
     * @param chan the <code>ReadableByteChannel</code> this can read bencoded data from
     */
    public Token(ReadableByteChannel chan) {
        this.chan = chan;
    }

    /**
     * Notification that this can read bencoded data from its channel.
     */
    public abstract void handleRead() throws IOException;

    /**
     * Determines if this has read a complete bencoded sentence.
     * 
     * @return true if we've read enough bencoded data to parse it into a complete object.
     *         false if we're still waiting to read more bencoded data to finish our object.
     */
    protected abstract boolean isDone();

    /**
     * Finds out what kind of bencoded element this is.
     */
    abstract public int getType();

    /**
     * Gets the object we made from the bencoded data we read and parsed.
     * 
     * @return the Object we parsed.
     *         null if we haven't read enough bencoded data from our channel to make it yet.
     */
    public T getResult() {
        if (!isDone())
            return null;
        return result;
    }

    /** A Token that marks the end of a list of Token objects. */
    static final EndElement TERMINATOR = new EndElement();
    private static class EndElement extends Token<EndElement> {
    	EndElement() {
            super(null); // No channel to read from
            result = this; // The object we parsed is this one
        }
        @Override
        public void handleRead() throws IOException {}
        @Override
        protected boolean isDone() {
            return true; // There is no data to parse
        }
        
        @Override
        public int getType() {
            return INTERNAL;
        }
    }

    /**
     * Reads the next bencoded object from the channel, returning a Token object that matches its type.
     * The Token this returns may be incomplete.
     * 
     * Call handleRead() to finish parsing an incomplete Token object.
     * Use isDone() to determine if it's complete, and getResult() to get the parsed Token object.
     * 
     * @param  chan        the ReadableByteChannel to read bencoded data from
     * @param charsetName  the charset used for decoding dictionary keys
     * @return             a possibly incomplete Token object, or null
     * @throws IOException if a read from the channel throws
     */
    public static Token<?> getNextToken(ReadableByteChannel chan, String charsetName) throws IOException {
        // There's some bencoded data in the given chanel for us to read and parse.
        // It might be a string like "5:hello", or a list that starts "l", has other elements, and ends "e".
        // 
        // First, it reads a single byte from the channel.
        // This is going to be a number like "5", or a letter that identifies a type like "l".
        //
        // Based on what letter it reads, it hands off control to a type specific constructor.
        // If it's a "d" for dictionary for instance, it gives the channel to the BEDictionary constructor.

    	byte []b = new byte[1];
    	ByteBuffer one_byte = ByteBuffer.wrap(b);
    	int read = chan.read(one_byte);
    	if (read == 0)
    	    return null; // The channel gave us no data, so we have no parsed object to return
    	if (read == -1)
    	    throw new EOFException("Could not read next Token");
        
        if (b[0] == BEncoder.I)
            return new BELong(chan);
        if (b[0] == BEncoder.R)
            return new BERational(chan);
        else if (b[0] == BEncoder.D)
            return new BEDictionary(chan, charsetName);
        else if (b[0] == BEncoder.L)
            return new BEList(chan);
        else if (b[0] == BEncoder.E)
            return Token.TERMINATOR;
        else if (b[0] >= ZERO && b[0] <= NINE)
            return new BEString(b[0], chan);
        else if (b[0] == BEncoder.TRUE || b[0] == BEncoder.FALSE)
            return b[0] == BEncoder.TRUE ? BEBoolean.TRUE : BEBoolean.FALSE;
        else
            throw new IOException("unrecognized token type " + (char)b[0]);
    }
    
    /**
     * Reads the next bencoded object from the channel, returning a Token object that matches its type.
     * The Token this returns may be incomplete.
     *
     * The charset that is used for decoding dictionary keys is {@link #ASCII}.
     * 
     * Call handleRead() to finish parsing an incomplete Token object.
     * Use isDone() to determine if it's complete, and getResult() to get the parsed Token object.
     * 
     * @param  chan        the ReadableByteChannel to read bencoded data from
     * @return             a possibly incomplete Token object, or null
     * @throws IOException if a read from the channel throws
     */
    public static Token<?> getNextToken(ReadableByteChannel chan) throws IOException {
        return getNextToken(chan, ASCII);
    }


    /**
     * Parses bencoded data in a channel into an object that extends Token.
     * 
     * @param data a channel with a complete bencoded object.
     * @param charsetName name of the charset used for decoding dictionary keys
     * @return     an object that extends Token like BEList.
     *             null if the byte array didn't contain a complete bencoded object.
     */
    public static Object parse(ReadableByteChannel byteChannel, String charsetName) throws IOException {
        Token<?> t = getNextToken(byteChannel, charsetName); // Reads the first letter like "l" to see what's next
        if (t == null)
            return null; // The channel couldn't even give 1 byte
        t.handleRead(); // Tell t to read from its channel and parse the data it reads
        return t.getResult();
    }
    
    /**
     * Parses bencoded data in a channel into an object that extends Token.
     * 
     * @param data a channel with a complete bencoded object.
     * @return     an object that extends Token like BEList.
     *             null if the byte array didn't contain a complete bencoded object.
     */
    public static Object parse(ReadableByteChannel byteChannel) throws IOException {
        return parse(byteChannel, ASCII);
    }
}
