package org.limewire.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;
import org.limewire.util.NameValue;
import org.limewire.util.StringUtils;



/** 
 * A mutable GGEP extension block.  A GGEP block can be thought of as a
 * collection of key/value pairs.  A key (extension header) cannot be greater
 * than 15 bytes.  The value (extension data) can be 0 to 2^24-1 bytes.  Values
 * can be formatted as a number, boolean, or generic blob of binary data.  If
 * necessary (e.g., for query replies), GGEP will COBS-encode values to remove
 * null bytes.  The order of the extensions is immaterial.  Extensions supported
 * by LimeWire have keys specified in this class (prefixed by GGEP_HEADER...)  
 */
public class GGEP {

    /** The maximum size of a extension header (key). */
    public static final int MAX_KEY_SIZE_IN_BYTES = 15;

    /** The maximum size of a extension data (value). */
    public static final int MAX_VALUE_SIZE_IN_BYTES = 262143;

    /** The GGEP prefix.  A GGEP block will start with this byte value.
     */
    public static final byte GGEP_PREFIX_MAGIC_NUMBER = (byte) 0xC3;

    /**
     * Default character set for GGEP values encoded as Strings
     */
    private static final String DEFAULT_ENCODING_CHARSET = "UTF-8";

    /** 
     * The collection of key/value pairs.  Rep. rationale: arrays of bytes are
     * convenient for values since they're easy to convert to numbers or
     * strings.  But strings are convenient for keys since they define hashCode
     * and equals.
     */
    private final Map<String, Object> _props = new TreeMap<String, Object>();

    /** True if COBS encoding is required. */
    private final boolean useCOBS;

    /**
     * Cached hash code value to avoid calculating the hash code from the
     * map each time.
     */
    private volatile int hashCode = 0;


    //////////////////// Encoding/Decoding (Map <==> byte[]) ///////////////////

    /** 
     * Creates a new empty GGEP block.  Typically this is used for outgoing
     * messages and mutated before encoding.  
     *
     * @param useCOBS false if nulls are allowed in extension values;true if
     *  this should activate COBS encoding if necessary to remove null bytes. 
     */
    public GGEP(boolean useCOBS) {
        this.useCOBS = useCOBS;
    }    

    /**  Creates a new empty GGEP block that does not needs COBS encoding. */
    public GGEP() {
        this(false);
    }
    
    /**
     * Constructs a new ggep message with the given data. 
     */
    public GGEP(byte[] data) throws BadGGEPBlockException {
        this(data, 0);
    }
    
    /**
     * Constructs a new GGEP message with the given bytes & offset.
     */
    public GGEP(byte[] data, int offset) throws BadGGEPBlockException {
        this(data, offset, null);
    }

    /**
     *  Constructs a GGEP instance based on the GGEP block beginning at
     *  messageBytes[beginOffset].
     *  @param messageBytes the bytes of the message.
     *  @param beginOffset  the begin index of the GGEP prefix.
     *  @param endOffset if you want to get the offset where the GGEP block
     *  ends (more precisely, one above the ending index), then send me a
     *  int[1].  I'll put the endOffset in endOffset[0].  If you don't care, 
     *  null will do....
     *  @exception BadGGEPBlockException Thrown if the block could not be parsed
     *  correctly.
     */
    public GGEP(byte[] messageBytes, final int beginOffset, int[] endOffset) 
      throws BadGGEPBlockException {

        if (messageBytes.length - beginOffset < 4)
            throw new BadGGEPBlockException();

        // all GGEP blocks start with this prefix....
        if (messageBytes[beginOffset] != GGEP_PREFIX_MAGIC_NUMBER)
            throw new BadGGEPBlockException();

        boolean tUseCOBS = false;
        boolean onLastExtension = false;
        int currIndex = beginOffset + 1;
        while (!onLastExtension) {

            // process extension header flags
            // bit order is interpreted as 76543210
            try {
                sanityCheck(messageBytes[currIndex]);
            } catch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlockException();
            }
            onLastExtension = isLastExtension(messageBytes[currIndex]);
            boolean encoded = isEncoded(messageBytes[currIndex]);
            boolean compressed = isCompressed(messageBytes[currIndex]);
            int headerLen = deriveHeaderLength(messageBytes[currIndex]);

            // get the extension header
            currIndex++;
            String extensionHeader = null;
            try {
                extensionHeader = StringUtils.getASCIIString(messageBytes, currIndex,
                                             headerLen);
            } catch (StringIndexOutOfBoundsException inputIsMalformed) {
                throw new BadGGEPBlockException();
            }

            // get the data length
            currIndex += headerLen;
            int[] toIncrement = new int[1];
            final int dataLength = deriveDataLength(messageBytes, currIndex,
                                                    toIncrement);

            byte[] extensionData = null;

            currIndex+=toIncrement[0];
            if (dataLength > 0) {
                // ok, data is present, get it....

                byte[] data = new byte[dataLength];
                try {
                    System.arraycopy(messageBytes, currIndex, data, 0, 
                                     dataLength);
                } catch (ArrayIndexOutOfBoundsException malformedInput) {
                    throw new BadGGEPBlockException();
                }

                if (encoded) {
                    tUseCOBS = true;
                    try {
                        data = GGEP.cobsDecode(data);
                    } catch (IOException badCobsEncoding) {
                        throw new BadGGEPBlockException("Bad COBS Encoding");
                    }
                }

                if (compressed) {
                    try {
                        data = IOUtils.inflate(data);
                    } catch(IOException badData) {
                        throw new BadGGEPBlockException("Bad compressed data");
                    }
                }

                extensionData = data;

                currIndex += dataLength;
            }

            // ok, everything checks out, just slap it in the hashmapper...
            if(compressed)
                _props.put(extensionHeader, new NeedsCompression(extensionData));
            else
                _props.put(extensionHeader, extensionData);

        }
        
        if ((endOffset != null) && (endOffset.length > 0))
            endOffset[0] = currIndex;
        
        useCOBS = tUseCOBS;
    }
    
    /**
     * Merges the other's GGEP with this' GGEP.
     */
    public void merge(GGEP other) {
        _props.putAll(other._props);
    }   

    private void sanityCheck(byte headerFlags) throws BadGGEPBlockException {
        // the 4th bit in the header's first byte must be 0.
        if ((headerFlags & 0x10) != 0)
            throw new BadGGEPBlockException();
    }
        
    private boolean isLastExtension(byte headerFlags) {
        boolean retBool = false;
        // the 8th bit in the header's first byte, when set, indicates that
        // this header is the last....
        if ((headerFlags & 0x80) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isEncoded(byte headerFlags) {
        boolean retBool = false;
        // the 7th bit in the header's first byte, when set, indicates that
        // this header is the encoded with COBS
        if ((headerFlags & 0x40) != 0)
            retBool = true;
        return retBool;        
    }


    private boolean isCompressed(byte headerFlags) {
        boolean retBool = false;
        // the 6th bit in the header's first byte, when set, indicates that
        // this header is the compressed with deflate
        if ((headerFlags & 0x20) != 0)
            retBool = true;
        return retBool;        
    }


    private int deriveHeaderLength(byte headerFlags) 
        throws BadGGEPBlockException {
        int retInt = 0;
        // bits 0-3 give the length of the extension header (1-15)
        retInt = headerFlags & 0x0F;
        if (retInt == 0)
            throw new BadGGEPBlockException();
        return retInt;
    }

    /** @param increment a int array of size >0.  i'll put the number of bytes
     *  devoted to data storage in increment[0].
     */
    private int deriveDataLength(byte[] buff, int beginOffset, int increment[]) 
        throws BadGGEPBlockException {
        int length = 0, iterations = 0;
        // the length is stored in at most 3 bytes....
        final int MAX_ITERATIONS = 3;
        byte currByte;
        do {
            try {
                currByte = buff[beginOffset++];
            }
            catch (ArrayIndexOutOfBoundsException malformedInput) {
                throw new BadGGEPBlockException();
            }
            length = (length << 6) | (currByte & 0x3f);
            if (++iterations > MAX_ITERATIONS)
                throw new BadGGEPBlockException();
        } while (0x40 != (currByte & 0x40));
        increment[0] = iterations;
        return length;
    }

    /** Writes this GGEP instance as a properly formatted GGEP Block.
     *  @param out this GGEP instance is written to out.
     *  @exception IOException thrown if had error writing to out.
     */
    public void write(OutputStream out) throws IOException {
        if (getHeaders().size() > 0) {
            // start with the magic prefix
            out.write(GGEP_PREFIX_MAGIC_NUMBER);

            Iterator<String> headers = getHeaders().iterator();
            // for each header, write the GGEP header and data
            while (headers.hasNext()) {
                String currHeader = headers.next();
                byte[] currData   = get(currHeader);
                int dataLen = 0;
                boolean shouldEncode = shouldCOBSEncode(currData);
                boolean shouldCompress = shouldCompress(currHeader);
                if (currData != null) {
                    if (shouldCompress) {
                        currData = IOUtils.deflate(currData);
                        if(currData.length > MAX_VALUE_SIZE_IN_BYTES)
                            throw new IllegalArgumentException("value for ["
                              + currHeader + "] too large after compression");
                    } if (shouldEncode)
                        currData = GGEP.cobsEncode(currData);
                    dataLen = currData.length;
                }
                writeHeader(currHeader, dataLen, 
                            !headers.hasNext(), out,
                            shouldEncode, shouldCompress);
                if (dataLen > 0) 
                    out.write(currData);
            }
        }
    }
    
    /**
     * Returns the GGEP as a byte array.
     * @return an empty array if GGEP is empty
     */
    public byte[] toByteArray() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(out);
        } catch (IOException e) {
            ErrorService.error(e);
        }
        return out.toByteArray();
    }

    private final boolean shouldCOBSEncode(byte[] data) {
        // if nulls are allowed from construction time and if nulls are present
        // in the data...
        return (useCOBS && containsNull(data));
    }
    
    private final boolean shouldCompress(String header) {
        return (_props.get(header) instanceof NeedsCompression);
    }
    
    private void writeHeader(String header, final int dataLen, 
                             boolean isLast, OutputStream out, 
                             boolean isEncoded, boolean isCompressed) 
        throws IOException {

        // 1. WRITE THE HEADER FLAGS
        byte[] headerBytes = StringUtils.toAsciiBytes(header);
        int flags = 0x00;
        if (isLast)
            flags |= 0x80;
        if (isEncoded)
            flags |= 0x40;
        if (isCompressed)
            flags |= 0x20;
        flags |= headerBytes.length;
        out.write(flags);

        // 2. WRITE THE HEADER
        out.write(headerBytes);

        // 3. WRITE THE DATA LEN
        // possibly 3 bytes
        int toWrite;
        int begin = dataLen & 0x3F000;
        if (dataLen > 0x00000fff) {
            begin = begin >> 12; // relevant bytes at the bottom now...
            toWrite = 0x80 | begin;
            out.write(toWrite);
        }
        int middle = dataLen & 0xFC0;
        if (dataLen > 0x0000003f) {
            middle = middle >> 6; // relevant bytes at the bottom now...
            toWrite = 0x80 | middle;
            out.write(toWrite);
        }
        int end = dataLen & 0x3F; // shut off everything except last 6 bits...
        toWrite = 0x40 | end;
        out.write(toWrite);
    }
    
    /**
     * Returns the amount of overhead that will be added 
     * when the following key/value pair is written.
     * <p>
     * This does *NOT* work for non-ASCII headers, or compressed data.
     */
    public int getHeaderOverhead(String key) {
        byte[] data = get(key);
        if(data == null)
            throw new IllegalArgumentException("no data for key: " + key);
        
        return 1 + // flags
               key.length() + // header
               data.length + // data
               1 + // required data length
               (data.length > 0x3F ? 1 : 0) + // optional data
               (data.length > 0xFFF ? 1 : 0); // more option data
    }
    
    ////////////////////////// Key/Value Mutators and Accessors ////////////////
    
    /**
     * Adds all the specified key/value pairs.
     */
     /* TODO: Allow a value to be compressed.
     */
    public void putAll(List<? extends NameValue<?>> fields) throws IllegalArgumentException {
        for(NameValue<?> next : fields) {
            String key = next.getName();
            Object value = next.getValue();
            if(value == null)
                put(key);
            else if(value instanceof byte[])
                put(key, (byte[])value);
            else if(value instanceof String)
                put(key, (String)value);
            else if(value instanceof Integer)
                put(key, ((Integer)value).intValue());
            else if(value instanceof Long)
                put(key, ((Long)value).longValue());
            else if(value instanceof Byte)
                put(key, ((Byte)value).byteValue());
            else
                throw new IllegalArgumentException("Unknown value: " + value);
        }
    }
    
    /**
     * Adds a key with data that should be compressed.
     */
    public void putCompressed(String key, byte[] value) throws IllegalArgumentException {
        validateKey(key);
        if(value == null)
            throw new IllegalArgumentException("null value for key: " + key);
        //validateValue(value); // done when writing.  TODO: do here?
        _props.put(key, new NeedsCompression(value));
    }
    
    /** 
     * Adds a key with byte value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data.
     */
    public void put(String key, byte value) throws IllegalArgumentException {
        put(key, new byte[] { value } );
    }    

    /** 
     * Adds a key with raw byte value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data
     * @exception IllegalArgumentException key is of an illegal length
     *  or if value is null.
     */
    public void put(String key, byte[] value) throws IllegalArgumentException {
        validateKey(key);
        validateValue(value, key);
        _props.put(key, value);
    }

    /** 
     * Adds a key with string value, using the default character encoding.
     * <p>
     * Enforcing a default encoding (UTF-8) because each machine can have its
     * own default encoding
     * 
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data
     * @exception IllegalArgumentException key is of an illegal length 
     *  or if value is null
     */
    public void put(String key, String value) throws IllegalArgumentException {
        put(key, value, DEFAULT_ENCODING_CHARSET);
    }
    
    /**
     * Keeping private access modifier until necessary to 
     * treat GGEP value Strings as encoded in different character sets.
     */
    private void put(String key, String value, String charSetName)
    throws IllegalArgumentException { 
        try {
            put(key, value==null ? null : value.getBytes(charSetName));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unsupported character set for " +
                    "String encoding", e);
        }
    }

    /** 
     * Adds a key with integer value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data, which should be an unsigned integer
     * @exception IllegalArgumentException key is of an illegal length
     *     or if value is negative
     */
    public void put(String key, int value) throws IllegalArgumentException {
        if (value < 0) // int2minLeb doesn't work on negative values
            throw new IllegalArgumentException("Negative value: " + value + " for key: " + key);
        put(key, ByteUtils.int2minLeb(value));
    }

    /** 
     * Adds a key with long value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @param value the GGEP extension data, which should be an unsigned long
     * @exception IllegalArgumentException key is of an illegal length
     *          of if value is negative
     */
    public void put(String key, long value) throws IllegalArgumentException {
        if (value < 0) // long2minLeb doesn't work on negative values
            throw new IllegalArgumentException("Negative value: " + value + " for key: " + key);
        put(key, ByteUtils.long2minLeb(value));
    }

    /** 
     * Adds a key without any value.
     * @param key the name of the GGEP extension, whose length should be between
     *  1 and 15, inclusive
     * @exception IllegalArgumentException key is of an illegal length.
     */
    public void put(String key) throws IllegalArgumentException {
        validateKey(key);
        _props.put(key, null);
    }

    /**
     * Returns the value for a key, as raw bytes.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.  Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public byte[] getBytes(String key) throws BadGGEPPropertyException {
        byte[] ret= get(key);
        if (ret==null)
            throw new BadGGEPPropertyException();
        return ret;
    }

    /**
     * Returns the value for a key, as a string, using the default encoding (UTF-8).
     * 
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public String getString(String key) throws BadGGEPPropertyException {
        return getString(key, DEFAULT_ENCODING_CHARSET);
    }
    
    /**
     * Keeping private access modifier until necessary to 
     * treat GGEP value Strings as encoded in different character sets.
     */
    private String getString(String key, String encoding) throws BadGGEPPropertyException, IllegalArgumentException {
        try {
            return new String(getBytes(key), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Cannot get GGEP key value as " +
                    "String due to unsupported encoding", e);
        }
    }

    /**
     * Returns the value for a key, as an integer.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public int getInt(String key) throws BadGGEPPropertyException {
        byte[] bytes=getBytes(key);
        if (bytes.length<1)
            throw new BadGGEPPropertyException("No bytes");
        if (bytes.length>4)
            throw new BadGGEPPropertyException("Integer too big");
        return ByteUtils.leb2int(bytes, 0, bytes.length);
    }
    
    /**
     * Returns the value for a key as a long.
     * @param key the name of the GGEP extension
     * @return the GGEP extension data associated with the key
     * @exception BadGGEPPropertyException extension not found, was corrupt,
     *  or has no associated data.   Note that BadGGEPPropertyException is
     *  is always thrown for extensions with no data; use hasKey instead.
     */
    public long getLong(String key) throws BadGGEPPropertyException {
        byte[] bytes=getBytes(key);
        if (bytes.length<1)
            throw new BadGGEPPropertyException("No bytes");
        if (bytes.length>8)
            throw new BadGGEPPropertyException("Integer too big");
        return ByteUtils.leb2long(bytes, 0, bytes.length);
    }

    /**
     * Returns whether this has the given key.
     * @param key the name of the GGEP extension
     * @return true if this has a key
     */
    public boolean hasKey(String key) {
        return _props.containsKey(key);
    }
    
    /** Returns true if the GGEP has a non-null value for the key. */
    public boolean hasValueFor(String key) {
        return get(key) != null;
    }

    /** 
     * Returns the set of keys.
     * @return a set of all the GGEP extension header name in this, each
     *  as a String.
     */
    public Set<String> getHeaders() {
        return _props.keySet();
    }
    
    /**
     * Returns whether this GGEP is empty or not.
     */
    public boolean isEmpty() {
        return _props.isEmpty();
    }
    
    /**
     * Gets the byte[] data from props.
     */
    public byte[] get(String key) {
        Object value = _props.get(key);
        if(value instanceof NeedsCompression)
            return ((NeedsCompression)value).data;
        else
            return (byte[])value;
    }

    private void validateKey(String key) throws IllegalArgumentException {
        if (!StringUtils.isAsciiOnly(key)) {
            throw new IllegalArgumentException("key is not ascii only: " + key);
        }
        byte[] bytes = StringUtils.toAsciiBytes(key);
        if ( key.equals("")
            || (bytes.length > MAX_KEY_SIZE_IN_BYTES)
            || containsNull(bytes))
            throw new IllegalArgumentException("invalid key: " + key);
    }

    private void validateValue(byte[] value, String key) throws IllegalArgumentException {
        if (value==null)
            throw new IllegalArgumentException("null value for key: " + key);
        if (value.length>MAX_VALUE_SIZE_IN_BYTES)
            throw new IllegalArgumentException("value (" + value + ") too large for key: " + key);
    }

    private boolean containsNull(byte[] bytes) {
        if (bytes != null) {
            for (int i = 0; i < bytes.length; i++)
                if (bytes[i] == 0x0)
                    return true;
        }
        return false;
    }
    
    //////////////////////////////// Miscellany ///////////////////////////////

    /** @return True if the two Maps that represent header/data pairs are
     *  equivalent.
     */
    @Override
    public boolean equals(Object o) {
		if(o == this) return true;
        if (! (o instanceof GGEP))
            return false; 
        //This is O(n lg n) time with n keys.  It would be great if we could
        //just check that the trees are isomorphic.  I don't think this code is
        //really used anywhere, however.
        return this.subset((GGEP)o) && ((GGEP)o).subset(this);
    }
    
    /** Returns true if this is a subset of other, e.g., all of this' keys 
     *  can be found in OTHER with the same value. */
    private boolean subset(GGEP other) {
        for(String key : _props.keySet()) {
            byte[] v1= this.get(key);
            byte[] v2= other.get(key);
            //Remember that v1 and v2 can be null.
            if ((v1==null) != (v2==null))
                return false;
            if (v1!=null && !Arrays.equals(v1, v2))
                return false;
        }
        return true;
    }
                
    // overrides Object.hashCode to be consistent with equals
    @Override
    public int hashCode() {
        if(hashCode == 0) {
            hashCode = 37 * _props.hashCode();
        }
        return hashCode;
    }
    
    /* COBS implementation....
     * For implementation details, please see:
     *  http://www.acm.org/sigcomm/sigcomm97/papers/p062.pdf 
     */

    /** Decode a COBS-encoded byte array.  The non-allowable byte value is 0.<p>
     *  PRE: src is not null.<p>
     *  POST: the return array will be a cobs decoded version of src.  namely,
     *  cobsDecode(cobsEncode(src)) ==  src.  
     *  @return the original COBS decoded string
     */
    static byte[] cobsDecode(byte[] src) throws IOException {
        final int srcLen = src.length;
        int currIndex = 0;
        int code = 0;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();        
    
        while (currIndex < srcLen) {
            code = ByteUtils.ubyte2int(src[currIndex++]);
            if ((currIndex+(code-2)) >= srcLen)
                throw new IOException();
            for (int i = 1; i < code; i++) {
                sink.write(src[currIndex++]);
            }
            if (currIndex < srcLen) // don't write this last one, it isn't used
                if (code < 0xFF) sink.write(0);
        }
    
        return sink.toByteArray();
    }

    static int cobsFinishBlock(int code, ByteArrayOutputStream sink, 
                                   byte[] src, int begin, int end) {
        sink.write(code);
        if (begin > -1)
            sink.write(src, begin, (end-begin)+1);
        return (byte) 0x01;
    }

    /** Encode a byte array with COBS.  The non-allowable byte value is 0.<p>
     *  PRE: src is not null.<p>
     *  POST: the return array will be a cobs encoded version of src.  namely,
     *  cobsDecode(cobsEncode(src)) ==  src.
     *  @return a COBS encoded version of src.
     */
    static byte[] cobsEncode(byte[] src) {
        final int srcLen = src.length;
        int code = 1;
        int currIndex = 0;
        // COBS encoding adds no more than one byte of overhead for every 254
        // bytes of packet data
        final int maxEncodingLen = src.length + ((src.length+1)/254) + 1;
        ByteArrayOutputStream sink = new ByteArrayOutputStream(maxEncodingLen);
        int writeStartIndex = -1;
    
        while (currIndex < srcLen) {
            if (src[currIndex] == 0) {
                // currIndex was incremented so take 1 less
                code = GGEP.cobsFinishBlock(code, sink, src, writeStartIndex,
                                   (currIndex-1));
                writeStartIndex = -1;
            }
            else {
                if (writeStartIndex < 0) writeStartIndex = currIndex;
                code++;
                if (code == 0xFF) {
                    code = GGEP.cobsFinishBlock(code, sink, src, writeStartIndex,
                                       currIndex);
                    writeStartIndex = -1;
                }
            }
            currIndex++;
        }
    
        // currIndex was incremented so take 1 less
        GGEP.cobsFinishBlock(code, sink, src, writeStartIndex, (currIndex-1));
        return sink.toByteArray();
    }

    /**
     * Marker class that wraps a byte[] value, if that value
     * is going to require compression upon write.
     */
    private static class NeedsCompression {
        final byte[] data;
        NeedsCompression(byte[] data) {
            this.data = data;
        }
    }
}





