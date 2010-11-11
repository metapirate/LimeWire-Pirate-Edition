package org.limewire.collection;

import java.io.Serializable;

/**
 * An ordered tuple of long values, (low, high).
 */
public abstract class Range implements Serializable {

    private static final long serialVersionUID = -2562093104400487223L;

    /** Maximum value a Range can hold. */
    public static final long MAX_VALUE = 0xFFFFFFFFFFL;

    /**
     * @return true if this Interval is a "subrange" of the other interval
     */
    public final boolean isSubrange(Range other) {
        return (getLow() >= other.getLow() && getHigh() <= other.getHigh());
    }

    /**
     * @return a byte [] representation of this range. The representation will
     *         be 8 bytes if isLong() is false, 10 bytes otherwise.
     */
    public abstract byte[] toBytes();

    /**
     * Places a byte[] representation of this range in the specified array at
     * the specified offset.
     */
    public abstract void toBytes(byte[] dest, int offset);

    public abstract long getLow();

    public abstract long getHigh();

    /**
     * @return a Range with the provided values that will use the least amount
     *         of memory.
     */
    public static Range createRange(long start, long end) {
        if (start <= Integer.MAX_VALUE && end <= Integer.MAX_VALUE)
            return new Interval(start, end);
        else
            return new LongInterval(start, end);
    }

    /**
     * @return a range (singleton,singleton)
     */
    public static Range createRange(long singleton) {
        return createRange(singleton, singleton);
    }

    @Override
    public String toString() {
        if (getLow() == getHigh())
            return String.valueOf(getLow());
        else
            return String.valueOf(getLow()) + "-" + String.valueOf(getHigh());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Range))
            return false;
        Range other = (Range) o;
        return getLow() == other.getLow() && getHigh() == other.getHigh();
    }

    @Override
    public int hashCode() {
        return (int) ((getLow() * getHigh()) % Integer.MAX_VALUE);
    }

    /**
     * @return true if this range has values > Integer.MAX_VALUE.
     */
    public abstract boolean isLong();

    /**
     * Returns the length of this range.
     */
    public long getLength() {
        return getHigh() - getLow() + 1;
    }
}