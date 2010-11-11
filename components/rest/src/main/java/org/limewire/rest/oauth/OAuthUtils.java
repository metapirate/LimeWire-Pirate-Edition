package org.limewire.rest.oauth;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.limewire.rest.RestUtils;

/**
 * Utility methods to support OAuth.
 */
class OAuthUtils {
    private static final char AMPERSAND = '&';
    private static final char EQUAL = '=';

    /**
     * Creates the signature base string for the specified request.  This is 
     * composed of three elements: HTTP request method, request URL, and 
     * normalized request parameters.
     */
    public static String createSignatureBaseString(OAuthRequest request, String baseUrl) {
        StringBuilder buf = new StringBuilder();
        
        buf.append(request.getMethod().toUpperCase(Locale.US)).append(AMPERSAND);
        buf.append(RestUtils.percentEncode(baseUrl + request.getUri())).append(AMPERSAND);
        buf.append(RestUtils.percentEncode(createParameterString(request)));
        
        return buf.toString();
    }
    
    /**
     * Creates the request parameter string for the specified request.  This
     * includes all parameters except the realm and signature, sorted by
     * parameter name.
     */
    private static String createParameterString(OAuthRequest request) {
        StringBuilder buf = new StringBuilder();
        
        List<NameValuePair> parameters = request.getParameters();
        Collections.sort(parameters, new NameValueComparator());
        for (NameValuePair parameter : parameters) {
            // Skip realm and signature parameters.
            if (OAuthRequest.AUTH_REALM.equalsIgnoreCase(parameter.getName()) || 
                OAuthRequest.OAUTH_SIGNATURE.equalsIgnoreCase(parameter.getName())) {
                continue;
            }
            
            // Append parameter to string.
            if (buf.length() > 0) {
                buf.append(AMPERSAND);
            }
            buf.append(RestUtils.percentEncode(parameter.getName()));
            buf.append(EQUAL).append(RestUtils.percentEncode(parameter.getValue()));
        }
        
        // Return parameter string.
        return buf.toString();
    }
    
    /**
     * Comparator for sorting the request parameters by name.
     */
    private static class NameValueComparator implements Comparator<NameValuePair> {
        
        @Override
        public int compare(NameValuePair o1, NameValuePair o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
