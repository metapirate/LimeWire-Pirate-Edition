package org.limewire.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HTTP;
import org.limewire.util.URIUtils;

/**
 * Provides utility methods for HttpCore.
 */
public class HttpCoreUtils {

    /**
     * Returns true, if message contains a header with <code>name</code> and the
     * value is a comma separated list that contains <code>token</code>.
     */
    public static boolean hasHeaderListValue(HttpMessage message, String name, String token) {   
        Header[] headers = message.getHeaders(name);
        for (Header header : headers) {
            StringTokenizer t = new StringTokenizer(header.getValue(), ",");
            while (t.hasMoreTokens()) {
                if (token.equals(t.nextToken().trim())) {
                    return true;
                }
            }
        }
        return false;
    }
 
    /**
     * Parses {@link URI#getQuery() query part} of uri string into a map.
     * <p>
     * Note: it does not parse the query part of magnet uris correctly.
     * 
     * @param encoding can be null, if null {@link HTTP#DEFAULT_CONTENT_CHARSET} will
     * be used
     * 
     * @return empty map if there was no query or query could not be parsed or
     * uri string does not denote a valid uri
     */
    public static Map<String, String> parseQuery(String uriString, String encoding) {
        try {
            URI uri = URIUtils.toURI(uriString);
            return parseQuery(uri, encoding);
        } catch (URISyntaxException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Parses {@link URI#getQuery() query part} of uri into a map.
     * <p>
     * Note: it does not parse the query part of magnet uris correctly.
     * 
     * @param encoding can be null, if null {@link HTTP#DEFAULT_CONTENT_CHARSET} will
     * be used
     * 
     * @return empty map if there was no query or query could not be parsed
     */
    public static Map<String, String> parseQuery(URI uri, String encoding) {
        List<NameValuePair> list = URLEncodedUtils.parse(uri, encoding);
        if (list.isEmpty()) {
            return Collections.emptyMap();
        } else {
            Map<String, String> map = new HashMap<String, String>(list.size());
            for (NameValuePair pair : list) {
                map.put(pair.getName(), pair.getValue());
            }
            return map;
        }
    }
}
