package org.limewire.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides methods for resolving endianness issues. Methods operate on byte
 * arrays as well as on streams.
 * <p>
 * See <a href = "http://en.wikipedia.org/wiki/Endianness">Endianness</a> for
 * more information.
 */
public class ByteUtils {
    /**
     * Returns the reverse of x.
     */
    public static byte[] reverse(final byte[] x) {
        int i, j;
        final int n;
        if ((n = x.length) > 0) {
            final byte[] ret = new byte[n];
            for (i = 0, j = n - 1; j >= 0;)
                ret[i++] = x[j--];
            return ret;
        }
        return x;
    }

    /**
     * Little-endian bytes to short.
     * 
     * @requires x.length - offset &gt;= 2
     * @effects returns the value of x[offset .. offset + 2] as a short,
     *          assuming x is interpreted as a signed little-endian number
     *          (i.e., x[offset] is LSB). If you want to interpret it as an
     *          unsigned number, call ubytes2int() on the result.
     */
    public static short leb2short(final byte[] x, final int offset) {
        return (short) ((x[offset] & 0xFF) | (x[offset + 1] << 8));
    }

    /**
     * Big-endian bytes to short.
     * 
     * @requires x.length - offset &gt;= 2
     * @effects returns the value of x[offset .. offset + 2] as a short,
     *          assuming x is interpreted as a signed big-endian number (i.e.,
     *          x[offset] is MSB). If you want to interpret it as an unsigned
     *          number, call ubytes2int() on the result.
     */
    public static short beb2short(final byte[] x, final int offset) {
        return (short) ((x[offset] << 8) | (x[offset + 1] & 0xFF));
    }

    /**
     * Little-endian bytes to short - stream version.
     */
    public static short leb2short(final InputStream is) throws IOException {
        return (short) ((readByte(is) & 0xFF) | (readByte(is) << 8));
    }

    /**
     * Big-endian bytes to short - stream version.
     */
    public static short beb2short(final InputStream is) throws IOException {
        return (short) ((readByte(is) << 8) | (readByte(is) & 0xFF));
    }

    /**
     * Little-endian bytes to int.
     * 
     * @requires x.length - offset &gt;= 4
     * @effects returns the value of x[offset .. offset + 4] as an int, assuming
     *          x is interpreted as a signed little-endian number (i.e.,
     *          x[offset] is LSB) If you want to interpret it as an unsigned
     *          number, call ubytes2long() on the result.
     */
    public static int leb2int(final byte[] x, final int offset) {
        return (x[offset] & 0xFF) | ((x[offset + 1] & 0xFF) << 8) | ((x[offset + 2] & 0xFF) << 16)
                | (x[offset + 3] << 24);
    }

    /**
     * Big-endian bytes to int.
     * 
     * @requires x.length - offset &gt;= 4
     * @effects returns the value of x[offset .. offset + 4] as an int, assuming
     *          x is interpreted as a signed big-endian number (i.e., x[offset]
     *          is MSB) If you want to interpret it as an unsigned number, call
     *          ubytes2long() on the result.
     */
    public static int beb2int(final byte[] x, final int offset) {
        return (x[offset] << 24) | ((x[offset + 1] & 0xFF) << 16) | ((x[offset + 2] & 0xFF) << 8)
                | (x[offset + 3] & 0xFF);
    }

    /**
     * Little-endian bytes to int - stream version.
     */
    public static int leb2int(final InputStream is) throws IOException {
        return (readByte(is) & 0xFF) | ((readByte(is) & 0xFF) << 8) | ((readByte(is) & 0xFF) << 16)
                | (readByte(is) << 24);
    }

    /**
     * Big-endian bytes to int - stream version.
     */
    public static int beb2int(final InputStream is) throws IOException {
        return (readByte(is) << 24) | ((readByte(is) & 0xFF) << 16) | ((readByte(is) & 0xFF) << 8)
                | (readByte(is) & 0xFF);
    }

    /**
     * Little-endian bytes to int. Unlike leb2int(x, offset), this version can
     * read fewer than 4 bytes. If n &lt; 4, the returned value is never
     * negative.
     * 
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 4,
     *        inclusive
     * @return the value of x[offset .. offset + N] as an int, assuming x is
     *         interpreted as an unsigned little-endian number (i.e., x[offset]
     *         is LSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException if offset &lt; 0 or offset + n &gt;
     *            x.length
     */
    public static int leb2int(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return x[offset] & 0xFF;
        case 2:
            return (x[offset] & 0xFF) | ((x[offset + 1] & 0xFF) << 8);
        case 3:
            return (x[offset] & 0xFF) | ((x[offset + 1] & 0xFF) << 8)
                    | ((x[offset + 2] & 0xFF) << 16);
        case 4:
            return (x[offset] & 0xFF) | ((x[offset + 1] & 0xFF) << 8)
                    | ((x[offset + 2] & 0xFF) << 16) | (x[offset + 3] << 24);
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }

    /**
     * Little-endian bytes to long. This version can read fewer than 8 bytes. If
     * n &lt; 8, the returned value is never negative.
     * 
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 8,
     *        inclusive
     * @return the value of x[offset .. offset + N] as an int, assuming x is
     *         interpreted as an unsigned little-endian number (i.e., x[offset]
     *         is LSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 8
     * @exception IndexOutOfBoundsException if offset &lt; 0 or offset + n &gt;
     *            x.length
     */
    public static long leb2long(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return x[offset] & 0xFFL;
        case 2:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8);
        case 3:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16);
        case 4:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16) | ((x[offset + 3] & 0xFFL) << 24);
        case 5:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16) | ((x[offset + 3] & 0xFFL) << 24)
                    | ((x[offset + 4] & 0xFFL) << 32);
        case 6:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16) | ((x[offset + 3] & 0xFFL) << 24)
                    | ((x[offset + 4] & 0xFFL) << 32) | ((x[offset + 5] & 0xFFL) << 40);
        case 7:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16) | ((x[offset + 3] & 0xFFL) << 24)
                    | ((x[offset + 4] & 0xFFL) << 32) | ((x[offset + 5] & 0xFFL) << 40)
                    | ((x[offset + 6] & 0xFFL) << 48);
        case 8:
            return (x[offset] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16) | ((x[offset + 3] & 0xFFL) << 24)
                    | ((x[offset + 4] & 0xFFL) << 32) | ((x[offset + 5] & 0xFFL) << 40)
                    | ((x[offset + 6] & 0xFFL) << 48) | ((long) x[offset + 7] << 56);
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }

    /**
     * Little-endian bytes to long. Stream version.
     */
    public static long leb2long(InputStream is) throws IOException {
        return (readByte(is) & 0xFFL) | ((readByte(is) & 0xFFL) << 8)
                | ((readByte(is) & 0xFFL) << 16) | ((readByte(is) & 0xFFL) << 24)
                | ((readByte(is) & 0xFFL) << 32) | ((readByte(is) & 0xFFL) << 40)
                | ((readByte(is) & 0xFFL) << 48) | (readByte(is) << 56);
    }

    /**
     * Big-endian bytes to long. Unlike beb2long(x, offset), this version can
     * read fewer than 4 bytes. If n &lt; 4, the returned value is never
     * negative.
     * 
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 4,
     *        inclusive
     * @return the value of x[offset .. offset + N] as an int, assuming x is
     *         interpreted as an unsigned big-endian number (i.e., x[offset] is
     *         MSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 4
     * @exception IndexOutOfBoundsException if offset &lt; 0 or offset + n &gt;
     *            x.length
     */
    public static int beb2int(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return x[offset] & 0xFF;
        case 2:
            return ((x[offset] & 0xFF) << 8) | (x[offset + 1] & 0xFF);
        case 3:
            return ((x[offset] & 0xFF) << 16) | ((x[offset + 1] & 0xFF) << 8)
                    | (x[offset + 2] & 0xFF);
        case 4:
            return (x[offset] << 24) | ((x[offset + 1] & 0xFF) << 16)
                    | ((x[offset + 2] & 0xFF) << 8) | (x[offset + 3] & 0xFF);
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }

    /**
     * Short to little-endian bytes: writes x to buf[offset .. ].
     */
    public static void short2leb(final short x, final byte[] buf, final int offset) {
        buf[offset] = (byte) x;
        buf[offset + 1] = (byte) (x >> 8);
    }

    /**
     * Short to big-endian bytes: writes x to buf[offset .. ].
     */
    public static void short2beb(final short x, final byte[] buf, final int offset) {
        buf[offset] = (byte) (x >> 8);
        buf[offset + 1] = (byte) x;
    }

    /**
     * Short to little-endian bytes: writes x to given stream.
     */
    public static void short2leb(final short x, final OutputStream os) throws IOException {
        os.write((byte) x);
        os.write((byte) (x >> 8));
    }

    /**
     * Short to big-endian bytes: writes x to given stream.
     */
    public static void short2beb(final short x, final OutputStream os) throws IOException {
        os.write((byte) (x >> 8));
        os.write((byte) x);
    }

    /**
     * Int to little-endian bytes: writes x to buf[offset ..].
     */
    public static void int2leb(final int x, final byte[] buf, final int offset) {
        buf[offset] = (byte) x;
        buf[offset + 1] = (byte) (x >> 8);
        buf[offset + 2] = (byte) (x >> 16);
        buf[offset + 3] = (byte) (x >> 24);
    }

    /**
     * Long to big-endian bytes: writes x to buf[offset ..].
     */
    public static void long2beb(final long x, final byte[] buf, final int offset) {
        buf[offset] = (byte) (x >> 56);
        buf[offset + 1] = (byte) (x >> 48);
        buf[offset + 2] = (byte) (x >> 40);
        buf[offset + 3] = (byte) (x >> 32);
        buf[offset + 4] = (byte) (x >> 24);
        buf[offset + 5] = (byte) (x >> 16);
        buf[offset + 6] = (byte) (x >> 8);
        buf[offset + 7] = (byte) x;
    }

    /**
     * Long to little-endian bytes: writes x to buf[offset ..].
     */
    public static void long2leb(final long x, final byte[] buf, final int offset) {
        buf[offset] = (byte) x;
        buf[offset + 1] = (byte) (x >> 8);
        buf[offset + 2] = (byte) (x >> 16);
        buf[offset + 3] = (byte) (x >> 24);
        buf[offset + 4] = (byte) (x >> 32);
        buf[offset + 5] = (byte) (x >> 40);
        buf[offset + 6] = (byte) (x >> 48);
        buf[offset + 7] = (byte) (x >> 56);
    }

    /**
     * @return a big-endian array of byteCount bytes matching the passed-in
     *         number: ie: 1L,4 becomes -> [0,0,0,1]
     * @param byteCount a number between 0 and 8, the size of the resulting
     *        array
     * @throws NegativeArraySizeException if byteCount < 0
     * @throws ArrayIndexOutOfBoundsException if byteCount > 8
     */
    public static byte[] long2bytes(long i, int byteCount) {
        byte[] b = new byte[8];
        b[7] = (byte) (i);
        i >>>= 8;
        b[6] = (byte) (i);
        i >>>= 8;
        b[5] = (byte) (i);
        i >>>= 8;
        b[4] = (byte) (i);
        i >>>= 8;
        b[3] = (byte) (i);
        i >>>= 8;
        b[2] = (byte) (i);
        i >>>= 8;
        b[1] = (byte) (i);
        i >>>= 8;
        b[0] = (byte) (i);

        // We have an 8 byte array. Copy the interesting bytes into our new
        // array of size 'byteCount'
        byte[] bytes = new byte[byteCount];
        System.arraycopy(b, 8 - byteCount, bytes, 0, byteCount);
        return bytes;
    }

    /**
     * Int to big-endian bytes: writes x to buf[offset ..].
     */
    public static void int2beb(final int x, final byte[] buf, final int offset) {
        buf[offset] = (byte) (x >> 24);
        buf[offset + 1] = (byte) (x >> 16);
        buf[offset + 2] = (byte) (x >> 8);
        buf[offset + 3] = (byte) x;
    }

    /**
     * Int to big-endian bytes: writing only the up to n bytes.
     * 
     * @requires x fits in n bytes, else the stored value will be incorrect. n
     *           may be larger than the value required to store x, in which case
     *           this will pad with 0.
     * 
     * @param x the little-endian int to convert
     * @param out the outputstream to write to.
     * @param n the number of bytes to write, which must be between 1 and 4,
     *        inclusive
     * @exception IllegalArgumentException if n is less than 1 or greater than 4
     */
    public static void int2beb(final int x, OutputStream out, final int n) throws IOException {
        switch (n) {
        case 1:
            out.write((byte) x);
            break;
        case 2:
            out.write((byte) (x >> 8));
            out.write((byte) x);
            break;
        case 3:
            out.write((byte) (x >> 16));
            out.write((byte) (x >> 8));
            out.write((byte) x);
            break;
        case 4:
            out.write((byte) (x >> 24));
            out.write((byte) (x >> 16));
            out.write((byte) (x >> 8));
            out.write((byte) x);
            break;
        default:
            throw new IllegalArgumentException("invalid n: " + n);
        }
    }

    /**
     * Int to little-endian bytes: writes x to given stream.
     */
    public static void int2leb(final int x, final OutputStream os) throws IOException {
        os.write((byte) x);
        os.write((byte) (x >> 8));
        os.write((byte) (x >> 16));
        os.write((byte) (x >> 24));
    }

    /**
     * Int to big-endian bytes: writes x to given stream.
     */
    public static void int2beb(final int x, final OutputStream os) throws IOException {
        os.write((byte) (x >> 24));
        os.write((byte) (x >> 16));
        os.write((byte) (x >> 8));
        os.write((byte) x);
    }

    /**
     * Returns the minimum number of bytes needed to encode x in little-endian
     * format, assuming x is non-negative. Note that leb2int(int2leb(x)) == x.
     * 
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    public static byte[] int2minLeb(final int x) throws IllegalArgumentException {
        if (x <= 0xFFFF) {
            if (x <= 0xFF) {
                if (x < 0)
                    throw new IllegalArgumentException();
                return new byte[] { (byte) x };
            }
            return new byte[] { (byte) x, (byte) (x >> 8) };
        }
        if (x <= 0xFFFFFF)
            return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16) };
        return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16), (byte) (x >> 24) };
    }

    /**
     * Returns the minimum number of bytes needed to encode x in little-endian
     * format, assuming x is non-negative.
     * 
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    public static byte[] long2minLeb(final long x) throws IllegalArgumentException {
        if (x <= 0xFFFFFFFFFFFFFFL) {
            if (x <= 0xFFFFFFFFFFFFL) {
                if (x <= 0xFFFFFFFFFFL) {
                    if (x <= 0xFFFFFFFFL) {
                        if (x <= 0xFFFFFFL) {
                            if (x <= 0xFFFFL) {
                                if (x <= 0xFFL) {
                                    if (x < 0)
                                        throw new IllegalArgumentException();
                                    return new byte[] { (byte) x };
                                }
                                return new byte[] { (byte) x, (byte) (x >> 8) };
                            }
                            return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16) };
                        }
                        return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16),
                                (byte) (x >> 24) };
                    }
                    return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16),
                            (byte) (x >> 24), (byte) (x >> 32) };
                }
                return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16), (byte) (x >> 24),
                        (byte) (x >> 32), (byte) (x >> 40) };
            }
            return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16), (byte) (x >> 24),
                    (byte) (x >> 32), (byte) (x >> 40), (byte) (x >> 48) };
        }

        return new byte[] { (byte) x, (byte) (x >> 8), (byte) (x >> 16), (byte) (x >> 24),
                (byte) (x >> 32), (byte) (x >> 40), (byte) (x >> 48), (byte) (x >> 56) };
    }

    /**
     * Returns the minimum number of bytes needed to encode x in big-endian
     * format, assuming x is non-negative. Note that beb2int(int2beb(x)) == x.
     * 
     * @param x a non-negative integer
     * @exception IllegalArgumentException x is negative
     */
    public static byte[] int2minBeb(final int x) throws IllegalArgumentException {
        if (x <= 0xFFFF) {
            if (x <= 0xFF) {
                if (x < 0)
                    throw new IllegalArgumentException();
                return new byte[] { (byte) x };
            }
            return new byte[] { (byte) (x >> 8), (byte) x };
        }
        if (x <= 0xFFFFFF)
            return new byte[] { (byte) (x >> 16), (byte) (x >> 8), (byte) x };
        return new byte[] { (byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) x };
    }

    /**
     * Interprets the value of x as an unsigned byte, and returns it as integer.
     * For example, ubyte2int(0xFF) == 255, not -1.
     */
    public static int ubyte2int(final byte x) {
        return x & 0xFF;
    }

    /**
     * Interprets the value of x as an unsigned two-byte number.
     */
    public static int ushort2int(final short x) {
        return x & 0xFFFF;
    }

    /**
     * Interprets the value of x as an unsigned four-byte number.
     */
    public static long uint2long(final int x) {
        return x & 0xFFFFFFFFL;
    }

    /**
     * Returns the int value that is closest to l. That is, if l can fit into a
     * 32-bit unsigned number, returns (int)l. Otherwise, returns either
     * Integer.MAX_VALUE or Integer.MIN_VALUE as appropriate.
     */
    public static int long2int(final long l) {
        int m;
        if (l < (m = Integer.MAX_VALUE) && l > (m = Integer.MIN_VALUE))
            return (int) l;
        return m;
    }

    /**
     * Big-endian bytes to long. Unlike beb2long(x, offset), this version can
     * read fewer than 8 bytes. If n &lt; 8, the returned value is never
     * negative.
     * 
     * @param x the source of the bytes
     * @param offset the index to start reading bytes
     * @param n the number of bytes to read, which must be between 1 and 8,
     *        inclusive
     * @return the value of x[offset .. offset + N] as a long, assuming x is
     *         interpreted as an unsigned big-endian number (i.e., x[offset] is
     *         MSB).
     * @exception IllegalArgumentException if n is less than 1 or greater than 8
     * @exception IndexOutOfBoundsException if offset &lt; 0 or offset + n &gt;
     *            x.length
     */
    public static long beb2long(final byte[] x, final int offset, final int n)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        switch (n) {
        case 1:
            return x[offset] & 0xFFL;
        case 2:
            return (x[offset + 1] & 0xFFL) | ((x[offset] & 0xFFL) << 8);
        case 3:
            return (x[offset + 2] & 0xFFL) | ((x[offset + 1] & 0xFFL) << 8)
                    | ((x[offset] & 0xFFL) << 16);
        case 4:
            return (x[offset + 3] & 0xFFL) | ((x[offset + 2] & 0xFFL) << 8)
                    | ((x[offset + 1] & 0xFFL) << 16) | ((x[offset] & 0xFFL) << 24);
        case 5:
            return (x[offset + 4] & 0xFFL) | ((x[offset + 3] & 0xFFL) << 8)
                    | ((x[offset + 2] & 0xFFL) << 16) | ((x[offset + 1] & 0xFFL) << 24)
                    | ((x[offset] & 0xFFL) << 32);
        case 6:
            return (x[offset + 5] & 0xFFL) | ((x[offset + 4] & 0xFFL) << 8)
                    | ((x[offset + 3] & 0xFFL) << 16) | ((x[offset + 2] & 0xFFL) << 24)
                    | ((x[offset + 1] & 0xFFL) << 32) | ((x[offset] & 0xFFL) << 40);
        case 7:
            return (x[offset + 6] & 0xFFL) | ((x[offset + 5] & 0xFFL) << 8)
                    | ((x[offset + 4] & 0xFFL) << 16) | ((x[offset + 3] & 0xFFL) << 24)
                    | ((x[offset + 2] & 0xFFL) << 32) | ((x[offset + 1] & 0xFFL) << 40)
                    | ((x[offset] & 0xFFL) << 48);
        case 8:
            return (x[offset + 7] & 0xFFL) | ((x[offset + 6] & 0xFFL) << 8)
                    | ((x[offset + 5] & 0xFFL) << 16) | ((x[offset + 4] & 0xFFL) << 24)
                    | ((x[offset + 3] & 0xFFL) << 32) | ((x[offset + 2] & 0xFFL) << 40)
                    | ((x[offset + 1] & 0xFFL) << 48) | ((x[offset] & 0xFFL) << 56);
        default:
            throw new IllegalArgumentException("No bytes specified");
        }
    }

    /**
     * Reads a byte from input stream and throws {@link EOFException} if the end
     * of the stream was reached.
     * <p>
     * Do not make public, the same method can be found in {@link IOUtils}, but
     * can't be used here to not introduce dependencies.
     */
    private static int readByte(InputStream is) throws IOException {
        int ret = is.read();
        if (ret == -1)
            throw new EOFException();
        return ret;
    }
}
