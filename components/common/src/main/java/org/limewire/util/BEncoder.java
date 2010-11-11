package org.limewire.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Convert Java objects into bencoded data.
 * <p>
 * Call BEncoder.encode(OutputStream, Object) to bencode a given Object and write the bencoded data to the given OutputStream.
 * <p>
 * Bencoded data is composed of strings, numbers, lists, and dictionaries.
 * Strings are prefixed by their length, like "5:hello".
 * Numbers are written as text numerals between the letters "i" and "e", like "i87e".
 * You can list any number of bencoded pieces of data between "l" for list and "e" for end.
 * A dictionary is a list of key and value pairs between "d" and "e".
 * The keys have to be strings, and they have to be in alphabetical order.
 * <p>
 * BitTorrent uses a simple and extensible data format called bencoding.
 * More information on bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding
 * http://www.bittorrent.org/protocol.html in the section titled "The connectivity is as follows".
 */
public class BEncoder {

    /** Identifies a bencoded real number. */
    public static final byte I;
    /** Identifies a bencoded rational number. */
    public static final byte R;
    /** Identifies a bencoded dictionary. */
    public static final byte D;
    /** Identifies a bencoded list. */
    public static final byte L;
    /** Marks the end of something in bencoding. */
    public static final byte E;
    
    /** Markers for true and false tokens */
    public static final byte TRUE;
    public static final byte FALSE;
    
    /** Separates the length from the string in the data of a bencoded string. */
    public final static byte COLON;
    
    private static final String ASCII = "ISO-8859-1";
    
    static {
        byte i = 0;
        byte d = 0;
        byte l = 0;
        byte e = 0;
        byte colon = 0;
        byte t = 0;
        byte f = 0;
        byte r = 0;

        try {

            i = "i".getBytes(ASCII)[0];
            d = "d".getBytes(ASCII)[0];
            l = "l".getBytes(ASCII)[0];
            e = "e".getBytes(ASCII)[0];
            colon = ":".getBytes(ASCII)[0];
            t = "t".getBytes(ASCII)[0];
            f = "f".getBytes(ASCII)[0];
            r = "r".getBytes(ASCII)[0];

        } catch (UnsupportedEncodingException impossible) {

            // TODO: connect to the error service
        }

        COLON = colon;
        I = i;
        D = d;
        L = l;
        E = e;
        TRUE = t;
        FALSE = f;
        R = r;
    }
    
    private final boolean fail, bool;
    private final String encoding;
    private final OutputStream output;
    private BEncoder(OutputStream output, boolean fail, boolean bool, String encoding) {
        this.fail = fail;
        this.encoding = encoding;
        this.output = output;
        this.bool = bool;
    }

    public static BEncoder getEncoder(OutputStream out) {
        return new BEncoder(out, false, true, ASCII);
    }
    
    public static BEncoder getEncoder(OutputStream out, boolean fail, boolean bool, String encoding) {
        return new BEncoder(out, fail, bool, encoding);
    }
    
    /**
     * Bencodes the given byte array to the given OutputStream.
     * <p>
     * Writes the length, a colon, and then the text.
     * For example, the byte array ['h', 'e', 'l', 'l', 'o'] becomes the bencoded bytes "5:hello".
     * 
     * @param b      the byte array to bencode and write
     */
    public void encodeByteArray(byte[] b) throws IOException {
        String length = String.valueOf(b.length);
        output.write(length.getBytes(ASCII));
        output.write(COLON);
        output.write(b);
    }

    /**
     * Bencodes the given Number to the given OutputStream.
     * <p>
     * Writes the base 10 digits of the number between the letters "i" and "e".
     * For example, the number 87 becomes the bencoded ASCII bytes "i87e".
     * 
     * @param n      the number to bencode and write
     */
    public void encodeInt(Number n) throws IOException {
        String numerals = String.valueOf(n.longValue());
        output.write(I);
        output.write(numerals.getBytes(ASCII));
        output.write(E);
    }
    

    /**
     * Bencodes the given Rational Number to the given OutputStream.
     * <p>
     * Writes the base 10 digits of the number's internal memory representation
     * between the letters "r" and "e".
     * 
     * @param n      the number to bencode and write
     */
    public void encodeRational(Number n) throws IOException {
        String numerals = String.valueOf(Double.doubleToLongBits(n.doubleValue()));
        output.write(R);
        output.write(numerals.getBytes(ASCII));
        output.write(E);
    }

    /**
     * Bencodes the given Iterable to the given OutputStream.
     * <p>
     * Writes "l" for list, the bencoded-form of each of the given objects, and then "e" for end.
     * 
     * @param iterable   a Java Iterable object to bencode and write
     */
    public void encodeList(Iterable<?> iterable) throws IOException {
        output.write(L);
        for(Object next : iterable) 
            encode(next);
        output.write(E);
    }

    /**
     * Bencodes the given Map to the given OutputStream.  Any String objects
     * are encoded using ASCII.
     * <p>
     * Writes a bencoded dictionary, which is a list of keys and values which looks like this:
     * <pre>
     * d
     * 5:color  5:green
     * 6:flavor 4:lime
     * 5:shape  5:round
     * e
     * </pre>
     * The bencoded data starts "d" for dictionary and ends "e" for end.
     * In the middle are pairs of bencoded values.
     * The keys have to be strings, while the values can be strings, numbers, lists, or more dictionaries.
     * The keys have to be in alphabetical order.
     * 
     * @param map the Java Map object to bencode and write
     */
    public void encodeDict(Map<?, ?> map) throws IOException {

        // The BitTorrent specification requires that dictionary keys are sorted in alphanumeric order
        SortedMap<String, Object> sorted = new TreeMap<String, Object>();
        for(Map.Entry<?, ?> entry : map.entrySet())
            sorted.put(entry.getKey().toString(), entry.getValue());

        output.write(D);
        for(Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (isValidType(entry.getKey()) && isValidType(entry.getValue())) {
                encodeByteArray(entry.getKey().getBytes(encoding));
                encode(entry.getValue());
            } else if (fail)
                throw new IllegalArgumentException();
        }
        output.write(E);
    }

    /**
     * Describes a given object using bencoding, and writes the bencoded data to the given stream.
     * <p>
     * To write a bencoded dictionary, pass a Map object.
     * To write a bencoded list, pass a List object.
     * To write a bencoded number, pass a Number object.
     * To write a bencoded string, pass a String or just a byte array.
     * 
     * @param  object                   the Java Object to bencode and write.
     * @throws IOException              if there was a problem reading from the OutputStream.
     *         IllegalArgumentException If you pass an object that isn't a Map, List, Number, String, or byte array.
     */
    private void encode(Object object) throws IOException {
    	if (object instanceof Map)
    		encodeDict((Map)object);
    	else if (object instanceof Iterable<?>)
    		encodeList((Iterable<?>)object);
    	else if (object instanceof Number) {
    	    if (object instanceof Double || object instanceof Float)
    	        encodeRational((Number)object);
    	    else
    	        encodeInt((Number)object);
    	}
    	else if (object instanceof String)
    		encodeByteArray(((String)object).getBytes(encoding));
    	else if (object instanceof byte[])
    		encodeByteArray((byte[])object);
        else if (object instanceof Boolean) 
            encodeBoolean((Boolean) object);
        else if (fail)
    		throw new IllegalArgumentException();
    }
    
    public void encodeBoolean(boolean value) throws IOException {
        if (bool)
            output.write(value ? TRUE : FALSE);
        else 
            encodeInt(value ? 1 : 0);
    }
    
    private static boolean isValidType(Object o ) {
        return (o instanceof Map) ||
        (o instanceof List )||
        (o instanceof Number) ||
        (o instanceof String) ||
        (o instanceof byte[]) ||
        (o instanceof Boolean);
    }
}
