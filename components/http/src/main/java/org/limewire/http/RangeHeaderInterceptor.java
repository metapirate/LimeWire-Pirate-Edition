package org.limewire.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpContext;

/**
 * Parses ranges found in Range headers formatted as specified in RFC 2616,
 * 14.35.1.
 */
public class RangeHeaderInterceptor implements HeaderInterceptor {

    public static String RANGE_HEADER = "Range";
    
    private List<Range> ranges;

    /**
     * Looks for range header of form, "Range: bytes=", "Range: bytes=", "Range:
     * bytes ", etc. Note that the "=" is required by HTTP, but old versions of
     * BearShare do not send it. The value following the bytes unit will be in
     * the form '-n', 'm-n', or 'm-'.
     * 
     * @see #getRequestedRanges()
     */
    public void process(Header header, HttpContext context)
            throws HttpException, IOException {
        if (!RANGE_HEADER.equals(header.getName())) {
            return;
        }

        String value = header.getValue().trim();
        if (!value.startsWith("bytes")) {
            throw new MalformedHeaderException(
                    "bytes not present in range header");
        }
        if (value.length() <= 6) {
            throw new MalformedHeaderException(
                    "range not present in range header");
        }

        // remove the "bytes" or "bytes="
        value = value.substring(6);

        StringTokenizer t = new StringTokenizer(value, ",");
        while (t.hasMoreElements()) {
            Range range = parseRange(t.nextToken().trim());
            if (this.ranges == null) {
                this.ranges = new ArrayList<Range>(1);
            }
            this.ranges.add(range);
        }
    }

    private Range parseRange(String value) throws MalformedHeaderException {
        if (value.length() < 2) {
            throw new MalformedHeaderException("invalid range: " + value);
        }

        final Range range = new Range();

        int i = value.indexOf("-");
        if (i == -1 || value.indexOf("-", i + 1) != -1) {
            // there must be exactly one dash
            throw new MalformedHeaderException("invalid range: " + value);
        } else if (i == 0) {
            // - n
            try {
                range.endOffset = Long.parseLong(value.substring(1).trim());
            } catch (NumberFormatException e) {
                throw new MalformedHeaderException();
            }
        } else if (i == value.length() - 1) {
            // n -
            try {
                range.startOffset = Long.parseLong(value.substring(0,
                        value.length() - 1).trim());
            } catch (NumberFormatException e) {
                throw new MalformedHeaderException();
            }
        } else {
            // n-m
            try {
                range.startOffset = Long
                .parseLong(value.substring(0, i).trim());
            } catch (NumberFormatException e) {
                throw new MalformedHeaderException();
            }
            try {
                range.endOffset = Long.parseLong(value.substring(i + 1).trim());
            } catch (NumberFormatException e) {
                throw new MalformedHeaderException();
            }
        }
        
        if (range.endOffset != -1 && range.startOffset > range.endOffset) {
            throw new MalformedHeaderException(
                    "start offset is greater than end offset ("
                            + range.startOffset + ">" + range.endOffset + ")");
        }

        assert range.startOffset >= 0 || range.endOffset >= 0;
        return range;
    }
    
    public boolean hasRequestedRanges() {
        return ranges != null;
    }

    /**
     * List of ranges found in all Range headers.
     * 
     * @return null, if no Range headers were found
     */
    public Range[] getRequestedRanges() {
        return (ranges != null) ? ranges.toArray(new Range[0]) : null;
    }

    /**
     * A single byte range.
     */
    public static class Range {

        private long startOffset = -1;

        private long endOffset = -1;

        /* For testing. */
        protected Range(long startOffset, long endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        private Range() {
        }

        /**
         * Returns the inclusive start offset.
         * 
         * @param totalSize the total size of the entity
         */
        public long getStartOffset(long totalSize) {
            if (totalSize < 0) {
                throw new IllegalArgumentException("totalSize must be >= 0");
            }

            if (startOffset > totalSize - 1) {
                return -1;
            }
            if (startOffset >= 0) {
                return startOffset;
            }
            // format is -n, meaning return last n bytes
            if (totalSize >= endOffset) {
                return totalSize - endOffset;
            } else {
                // requested bytes exceed size of entity, return the whole
                // entity
                return 0;
            }
        }

        /**
         * Returns the inclusive end offset.
         * 
         * @param totalSize the total size of the entity
         */
        public long getEndOffset(long totalSize) {
            if (totalSize < 0) {
                throw new IllegalArgumentException("totalSize must be >= 0");
            }

            if (startOffset >= 0) {
                if (endOffset >= 0 && endOffset < totalSize) {
                    return endOffset;
                } else {
                    return totalSize - 1;
                }
            }
            // format is -n, meaning return last n bytes
            return totalSize - 1;
        }

    }

}
