package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.listener.SourcedEventMulticaster;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.licenses.LicenseFactory;

/**
 * This class extends FileDesc and wraps an incomplete File, so it can be used
 * for partial file sharing.
 */
class IncompleteFileDescImpl extends FileDescImpl implements IncompleteFileDesc {
    /**
     * Ranges smaller than this will never be offered to other servents.
     */
    private final static int MIN_CHUNK_SIZE = 102400; // 100K

    /**
     * Needed to find out what ranges are available.
     */
    private VerifyingFile _verifyingFile;

    /**
     * The name of the file, as returned by IncompleteFileManager
     * .getCompletedName(FILE).
     */
    private final String _name;

    /**
     * The size of the file, casted to an <tt>int</tt>.
     */
    private final long _size;

    /**
     * Constructor for the IncompleteFileDesc object.
     */
    public IncompleteFileDescImpl(RareFileStrategy rareFileStrategy,
            LicenseFactory licenseFactory,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster, File file,
            Set<? extends URN> urns, int index, String completedName, long completedSize,
            VerifyingFile vf) {
        super(rareFileStrategy, licenseFactory, fileDescMulticaster, file, urns, index);
        _name = completedName;
        _size = completedSize;
        _verifyingFile = vf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.library.IncompleteFileDesc#getFileSize()
     */
    @Override
    public long getFileSize() {
        return _size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.library.IncompleteFileDesc#getFileName()
     */
    @Override
    public String getFileName() {
        return _name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.library.IncompleteFileDesc#getRangesAsByte()
     */
    public IntervalSet.ByteIntervals getRangesAsByte() {
        return _verifyingFile.toBytes();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.library.IncompleteFileDesc#getAvailableRanges()
     */
    public String getAvailableRanges() {
        StringBuilder ret = new StringBuilder("bytes");
        boolean added = false;
        // This must be synchronized so that downloaders writing
        // to the verifying file do not cause concurrent mod
        // exceptions.
        synchronized (_verifyingFile) {
            for (Range interval : _verifyingFile.getVerifiedBlocks()) {
                // don't offer ranges that are smaller than MIN_CHUNK_SIZE
                // ( we add one because HTTP values are exclusive )
                if (interval.getHigh() - interval.getLow() + 1 < MIN_CHUNK_SIZE)
                    continue;

                added = true;
                // ( we subtract one because HTTP values are exclusive )
                ret.append(" ").append(interval.getLow()).append("-")
                        .append(interval.getHigh() - 1).append(",");
            }
        }
        // truncate off the last ',' if atleast one was added.
        // it is necessary to do this (instead of checking hasNext when
        // adding the comma) because it's possible that the last range
        // is smaller than MIN_CHUNK_SIZE, leaving an extra comma at the end.
        if (added)
            ret.setLength(ret.length() - 1);

        return ret.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.library.IncompleteFileDesc#loadResponseRanges(
     * org.limewire.collection.IntervalSet)
     */
    public boolean loadResponseRanges(IntervalSet dest) {
        synchronized (_verifyingFile) {
            if (!hasUrnsAndPartialData()) {
                assert getUrns().size() > 1
                        && _verifyingFile.getBlockSize() + _verifyingFile.getAmountLost() >= MIN_CHUNK_SIZE : "urns : "
                        + getUrns().size()
                        + " size "
                        + _verifyingFile.getBlockSize()
                        + " lost "
                        + _verifyingFile.getAmountLost();
            }
            if (_verifyingFile.getVerifiedBlockSize() > 0) {
                dest.add(_verifyingFile.getVerifiedIntervalSet());
                return true;
            }
            dest.add(_verifyingFile.getPartialIntervalSet());
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.library.IncompleteFileDesc#hasUrnsAndPartialData()
     */
    public boolean hasUrnsAndPartialData() {
        return getUrns().size() > 1 && // must have both ttroot & sha1
                _verifyingFile.getBlockSize() >= MIN_CHUNK_SIZE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.library.IncompleteFileDesc#isRangeSatisfiable(
     * long, long)
     */
    public boolean isRangeSatisfiable(long low, long high) {
        // This must be synchronized so that downloaders writing
        // to the verifying file do not cause concurrent mod
        // exceptions.
        synchronized (_verifyingFile) {
            for (Range interval : _verifyingFile.getVerifiedBlocks()) {
                if (low >= interval.getLow() && high <= interval.getHigh())
                    return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.library.IncompleteFileDesc#getAvailableSubRange
     * (long, long)
     */
    public Range getAvailableSubRange(long low, long high) {
        synchronized (_verifyingFile) {
            for (Range interval : _verifyingFile.getVerifiedBlocks()) {
                if ((interval.getLow() <= high && low <= interval.getHigh()))
                    // overlap found
                    return Range.createRange(Math.max(interval.getLow(), low), Math.min(interval
                            .getHigh(), high));
                else if (interval.getLow() > high) // passed all viable
                                                   // intervals
                    break;
            }
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.limegroup.gnutella.library.IncompleteFileDesc#isRangeSatisfiable(
     * org.limewire.collection.Range)
     */
    public boolean isRangeSatisfiable(Range range) {
        return isRangeSatisfiable(range.getLow(), range.getHigh());
    }

    // implements HTTPHeaderValue
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.library.IncompleteFileDesc#httpStringValue()
     */
    public String httpStringValue() {
        return getAvailableRanges();
    }

    // overrides Object.toString to provide a more useful description
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.library.IncompleteFileDesc#toString()
     */
    @Override
    public String toString() {
        return ("IncompleteFileDesc:\r\n" + "name:     " + _name + "\r\n" + "index:    "
                + getIndex() + "\r\n");
    }
}
