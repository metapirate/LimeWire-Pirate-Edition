package org.limewire.rest.oauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.rest.RestUtils;

/**
 * A representation of an HTTP request using the OAuth protocol.
 */
public class OAuthRequest {

    public static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    public static final String OAUTH_TOKEN = "oauth_token";
    public static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    public static final String OAUTH_SIGNATURE = "oauth_signature";
    public static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    public static final String OAUTH_NONCE = "oauth_nonce";
    public static final String OAUTH_VERSION = "oauth_version";
    
    public static final String AUTH_HEADER = "Authorization";
    public static final String AUTH_SCHEME = "OAuth";
    public static final String AUTH_REALM = "realm";
    
    private static final Pattern AUTH_PATTERN = Pattern.compile("\\s*(\\w*)\\s+(.*)");
    private static final Log LOG = LogFactory.getLog(OAuthRequest.class);
    
    private final String method;
    private final String uri;
    private final Map<String, String> parameterMap;
    
    /**
     * Constructs an OAuthRequest for the specified HTTP request.
     */
    public OAuthRequest(HttpRequest request) {
        RequestLine requestLine = request.getRequestLine();

        method = requestLine.getMethod();
        uri = RestUtils.getBaseUri(requestLine.getUri());
        parameterMap = new HashMap<String, String>();
        
        parseAuthHeaderParameters(request);
        parseQueryParameters(request);
    }
    
    /**
     * Parses the parameters from the Authorization header.  The realm is
     * included as a parameter.  If the header doesn't start with "OAuth", 
     * then the parameters are ignored.
     */
    private void parseAuthHeaderParameters(HttpRequest request) {
        Header[] headers = request.getHeaders(AUTH_HEADER);
        for (Header header : headers) {
            String authorization = header.getValue();
            Matcher m = AUTH_PATTERN.matcher(authorization);
            if (m.matches()) {
                // First element must be OAuth.
                if (AUTH_SCHEME.equalsIgnoreCase(m.group(1))) {
                    // Extract name/value pairs.
                    Header temp = new BasicHeader(AUTH_HEADER, m.group(2));
                    HeaderElement[] elements = temp.getElements();
                    for (HeaderElement element : elements) {
                        String name = RestUtils.percentDecode(element.getName());
                        String value = RestUtils.percentDecode(element.getValue());
                        parameterMap.put(name, value);
                    }
                }
            }
        }
    }
    
    /**
     * Parses the query parameters in the specified HTTP request, and adds
     * them to the parameter map.
     */
    private void parseQueryParameters(HttpRequest request) {
        try {
            String uriStr = request.getRequestLine().getUri();
            Map<String, String> queryMap = RestUtils.getQueryParams(uriStr);
            for (String key : queryMap.keySet()) {
                String value = queryMap.get(key);
                value = (value == null) ? "" : value;
                parameterMap.put(RestUtils.percentDecode(key), RestUtils.percentDecode(value));
            }
        
        } catch (IOException ex) {
            LOG.debugf(ex, "Unable to parse query parms {0}", ex.getMessage());
        }
    }
    
    /**
     * Returns the HTTP method.
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * Returns the URI of the request.
     */
    public String getUri() {
        return uri;
    }
    
    /**
     * Returns the value for the specified request parameter name.  If the
     * parameter is not found, then null is returned.
     */
    public String getParameter(String name) {
        return parameterMap.get(name);
    }
    
    /**
     * Returns the long value for the specified request parameter name.  If
     * the parameter is not found, or cannot be parsed into a long, then the
     * specified default value is returned.
     */
    public long getParameter(String name, long defaultValue) {
        String value = parameterMap.get(name);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }
    
    /**
     * Returns a List of name/value pairs representing the request parameters.
     */
    public List<NameValuePair> getParameters() {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return parameters;
    }
}
