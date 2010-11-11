package com.limegroup.gnutella.library;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.gnutella.http.HTTPHeaderValue;

public interface IncompleteFileDesc extends FileDesc, HTTPHeaderValue {

    public IntervalSet.ByteIntervals getRangesAsByte();

    /**
     * Returns the available ranges as an HTTP string value.
     */
    public String getAvailableRanges();

    /**
     * @param dest where to load the ranges
     * @return true if the loaded ranges were verified
     */
    public boolean loadResponseRanges(IntervalSet dest);

    /**
     * @return true if responses should be returned for this IFD.
     */
    public boolean hasUrnsAndPartialData();

    /**
     * Determines whether or not the given range is satisfied by this
     * incomplete file.
     */
    public boolean isRangeSatisfiable(long low, long high);

    /**
     * Adjusts the requested range to the available range.
     * @return Interval that has been clipped to match the available range, null
     * if the interval does not overlap any available ranges
     */
    public Range getAvailableSubRange(long low, long high);

    /**
     * Determines whether or not the given interval is within the range
     * of our incomplete file.
     */
    public boolean isRangeSatisfiable(Range range);

}