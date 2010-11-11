package org.limewire.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.nio.entity.NStringEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.http.HttpCoreUtils;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

/**
 * Static constants and utility methods to support REST functions.
 */
public abstract class RestUtils {
    
    public static final String GET = "GET";
    public static final String PUT = "PUT";
    public static final String POST = "POST";
    public static final String DELETE = "DELETE";
    
    private static final String ENCODING = "UTF-8";
    private static final String ACCESS_FILE = "restaccess.txt";

    /** Symbols used for random string generator. */
    private static final char[] SYMBOLS = new char[62];
    static {
        for (int i = 0; i < 10; i++) {
            SYMBOLS[i] = (char) ('0' + i);
        }
        for (int i = 0; i < 26; i++) {
            SYMBOLS[i + 10] = (char) ('A' + i);
        }
        for (int i = 0; i < 26; i++) {
            SYMBOLS[i + 36] = (char) ('a' + i);
        }
    }
    
    /**
     * Generates a random alphanumeric string with the specified length.
     */
    public static String createRandomString(int length) {
        char[] buf = new char[length];
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
        
        return new String(buf);
    }
    
    /**
     * Returns a new HttpEntity containing the specified content string.
     */
    public static HttpEntity createStringEntity(String content) throws IOException {
        return new NStringEntity(content, ENCODING);
    }

    /**
     * Performs percent decoding on the specified string according to the 
     * OAuth 1.0a specification.
     */
    public static String percentDecode(String s) {
        try {
            // Decode string.  Plus sign should remain unchanged instead of 
            // decoded into space char.
            return URLDecoder.decode(s.replace("+", "%2B"), ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Performs percent encoding on the specified string according to the 
     * OAuth 1.0a specification.
     */
    public static String percentEncode(String s) {
        try {
            // Encode string.  For OAuth, unreserved characters like tilde
            // must not be encoded.  Also, space char should be percent encoded
            // instead of changed to plus sign.
            return URLEncoder.encode(s, ENCODING).replace("+", "%20").replace("%7E", "~");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns the base URI by stripping off the query parameters from the
     * specified URI string.
     */
    public static String getBaseUri(String uriStr) {
        // Strip off query parameters.
        int pos = uriStr.indexOf("?");
        return (pos < 0) ? uriStr : uriStr.substring(0, pos);
    }
    
    /**
     * Parses the URI in the specified request, and returns URI target.  The
     * target is the piece between the prefix and the query parameters.  For
     * example, if the URI is "http://localhost/remote/library/files?type=audio"
     * and the prefix is "/library", then the target is "/files". 
     */
    public static String getUriTarget(HttpRequest request, String uriPrefix) throws IOException {
        // Get uri string.
        String uriStr = request.getRequestLine().getUri();
        
        // Strip off uri prefix.
        int pos = uriStr.indexOf(uriPrefix);
        if (pos < 0) throw new IOException("Invalid URI");
        String uriTarget = uriStr.substring(pos + uriPrefix.length());
        
        // Strip off query parameters.
        pos = uriTarget.indexOf("?");
        return (pos < 0) ? uriTarget : uriTarget.substring(0, pos);
    }
    
    /**
     * Returns a map of name/value pairs corresponding to the query parameters
     * in the specified request.
     */
    public static Map<String, String> getQueryParams(HttpRequest request) throws IOException {
        // Get uri string.
        String uriStr = request.getRequestLine().getUri();
        return getQueryParams(uriStr);
    }
    
    /**
     * Returns a map of name/value pairs corresponding to the query parameters
     * in the specified URI string.
     */
    public static Map<String, String> getQueryParams(String uriStr) throws IOException {
        try {
            // Get query parameters.
            URI uri = new URI(uriStr);
            return HttpCoreUtils.parseQuery(uri, null);
            
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Returns the REST access secret.
     */
    public static String getAccessSecret() throws IOException {
        // Read access secret from file.
        File accessFile = new File(CommonUtils.getUserSettingsDir(), ACCESS_FILE);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(accessFile));
            return reader.readLine();
        } finally {
            IOUtils.close(reader);
        }
    }
    
    /**
     * Updates the REST access secret if necessary.
     */
    public static void updateAccessSecret() {
        // Update access secret only if not already saved.
        File accessFile = new File(CommonUtils.getUserSettingsDir(), ACCESS_FILE);
        if (!accessFile.exists()) {
            String secret = RestUtils.createRandomString(32);
            byte[] bytes = StringUtils.toUTF8Bytes(secret);
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), ACCESS_FILE, bytes);
        }
    }
    
    /**
     * Creates the JSON description object for the specified local file item.
     */
    public static JSONObject createFileItemJson(LocalFileItem fileItem) throws JSONException {
        String filename = fileItem.getFileName();
        Category category = fileItem.getCategory();
        String artist = (String) fileItem.getProperty(FilePropertyKey.AUTHOR);
        String album = (String) fileItem.getProperty(FilePropertyKey.ALBUM);
        String genre = (String) fileItem.getProperty(FilePropertyKey.GENRE);
        String title = (String) fileItem.getProperty(FilePropertyKey.TITLE);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("filename", filename);
        jsonObj.put("category", category.getSchemaName());
        jsonObj.put("size", fileItem.getSize());
        jsonObj.put("id", fileItem.getUrn());
        jsonObj.put("artist", (category == Category.AUDIO && artist != null) ? artist : "");
        jsonObj.put("album", (category == Category.AUDIO && album != null) ? album : "");
        jsonObj.put("genre", (category == Category.AUDIO && genre != null) ? genre : "");
        jsonObj.put("title", (category == Category.AUDIO && title != null) ? title : filename);
        return jsonObj;
    }
    
    /**
     * Creates the JSON description object for the specified library file list.
     */
    public static JSONObject createLibraryJson(LibraryFileList fileList) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "Library");
        jsonObj.put("size", fileList.size());
        jsonObj.put("id", "library");
        return jsonObj;
    }
    
    /**
     * Creates the JSON description object for the specified search list.
     */
    public static JSONObject createSearchJson(SearchResultList searchList) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", searchList.getSearchQuery());
        jsonObj.put("size", searchList.getGroupedResults().size());
        jsonObj.put("id", searchList.getGuid());
        return jsonObj;
    }
    
    /**
     * Creates the JSON description object for the specified grouped search
     * result.
     */
    public static JSONObject createSearchResultJson(GroupedSearchResult result) throws JSONException {
        String filename = result.getFileName();
        SearchResult searchResult = result.getSearchResults().get(0);
        Category category = searchResult.getCategory();
        String artist = (String) searchResult.getProperty(FilePropertyKey.AUTHOR);
        String album = (String) searchResult.getProperty(FilePropertyKey.ALBUM);
        String genre = (String) searchResult.getProperty(FilePropertyKey.GENRE);
        String title = (String) searchResult.getProperty(FilePropertyKey.TITLE);
        
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("filename", filename);
        jsonObj.put("category", category.getSchemaName());
        jsonObj.put("size", searchResult.getSize());
        jsonObj.put("id", result.getUrn());
        jsonObj.put("magnetUrl", searchResult.getMagnetURL());
        jsonObj.put("sources", result.getSources().size());
        jsonObj.put("spam", searchResult.isSpam());
        jsonObj.put("artist", (category == Category.AUDIO && artist != null) ? artist : "");
        jsonObj.put("album", (category == Category.AUDIO && album != null) ? album : "");
        jsonObj.put("genre", (category == Category.AUDIO && genre != null) ? genre : "");
        jsonObj.put("title", (category == Category.AUDIO && title != null) ? title : filename);
        return jsonObj;
    }
}
