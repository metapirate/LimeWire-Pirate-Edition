package com.limegroup.gnutella.http;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.limewire.util.StringUtils;

/**
 * This class supplies general facilities for handling HTTP, such as writing
 * headers, extracting header values, etc..
 */
public final class HTTPUtils {

    /**
     * Constant for the carriage-return linefeed sequence that marks the end of
     * an HTTP header
     */
    private static final String CRLF = "\r\n";

    /**
     * Cached colon followed by a space to avoid excessive allocations.
     */
    private static final String COLON_SPACE = ": ";

    /**
     * Cached colon to avoid excessive allocations.
     */
    public static final String COLON = ":";

    /**
     * Cached slash to avoid excessive allocations.
     */
    private static final String SLASH = "/";

    /**
     * Private constructor to ensure that this class cannot be constructed
     */
    private HTTPUtils() {
    }

    public static String createHeader(String name, String value) {
        StringBuilder sb = new StringBuilder(name.length() + value.length() + 4);
        return sb.append(name).append(COLON_SPACE).append(value).append(CRLF).toString();
    }

    /**
     * Utility method for extracting the version from a feature token.
     */
    public static float parseFeatureToken(String token) throws ProblemReadingHeaderException {
        int slashIndex = token.indexOf(SLASH);

        if (slashIndex == -1 || slashIndex >= token.length() - 1)
            throw new ProblemReadingHeaderException("invalid feature token");

        String versionS = token.substring(slashIndex + 1);

        try {
            return Float.parseFloat(versionS);
        } catch (NumberFormatException bad) {
            throw new ProblemReadingHeaderException(bad);
        }
    }

    /** Utility for extracting a value from a K=V string. */
    public static String parseValue(String keyValuePair) throws IOException {
        int equalIndex = keyValuePair.indexOf("=");
        if (equalIndex == -1 || equalIndex >= keyValuePair.length() - 1)
            throw new IOException("invalid keyValuePair: " + keyValuePair);

        return keyValuePair.substring(equalIndex + 1).trim();
    }

    /**
     * Utility method for getting the date value for the "Date" header in
     * standard format.
     * 
     * @return the current date as a standardized date string -- see RFC 2616
     *         section 14.18
     */
    public static String getDateValue() {
        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }

    /**
     * Encodes a name using URLEncoder, using %20 instead of + for spaces.
     */
    public static String encode(String name, String encoding) throws IOException {
        return StringUtils.replace(URLEncoder.encode(name, encoding), "+", "%20");
    }
    
    /**
     * Returns the offset at which content starts in the HttpResponse,
     * based on the 'Content-Range' header.
     */
    public static long getStartPoint(HttpResponse response) throws IOException {
//      LOG.debugf("Looking at response of {0}, {1}", response.getStatusLine(), Arrays.asList(response.getAllHeaders()));
      Header header = response.getFirstHeader(HTTPHeaderName.CONTENT_RANGE.httpStringValue());
      if(header == null) {
          return 0;
      } else {
//          Content-Range = "Content-Range" ":" content-range-spec
//
//          content-range-spec      = byte-content-range-spec
//          byte-content-range-spec = bytes-unit SP
//                                    byte-range-resp-spec "/"
//                                    ( instance-length | "*" )
//
//          byte-range-resp-spec = (first-byte-pos "-" last-byte-pos)
//                                         | "*"
//          instance-length           = 1*DIGIT
          String value = header.getValue();
          // Skip past the 'bytes ' prefix.
          if(!value.startsWith("bytes ")) {
              throw new IOException("invalid content range: " + value);
          }
          value = value.substring("bytes ".length()).trim();
          if(value.startsWith("*")) {
              // They're returning everything!
              return 0;
          } else {
              // There's got to be a dash in there, we want before the dash.
              int dashIdx = value.indexOf('-');
              if(dashIdx < 0) {
                  throw new IOException("invalid content range: " + value);
              } else {
                  value = value.substring(0, dashIdx).trim();
                  try {
                      long start = Long.parseLong(value);
                      if(start < 0) {
                          throw new IOException("invalid start: " + start);
                      }
                      return start;
                  } catch(NumberFormatException nfe) {
                      throw new IOException("invalid content range", nfe);
                    }
                }
            }
        }
    }
}
