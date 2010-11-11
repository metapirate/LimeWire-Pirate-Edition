package org.limewire.core.impl.search;


/**
 * Class for creating and modifying URLs for search functionality.
 */
class SearchUrlUtils {

    /**
     * Strips "http://" and anything after ".com" (or .whatever) from the url
     * 
     * @return the stripped URL.
     */
    public static String stripUrl(String url){
        int dotIndex = url.indexOf('.');
        int endIndex = url.indexOf('/', dotIndex);
        endIndex = endIndex == -1 ? url.length() :  endIndex;
        int startIndex = url.indexOf("//");
        // this will either be 0 or the first character after "//"
        startIndex = startIndex == -1 ? 0 :  startIndex + 2;
        return url.substring(startIndex, endIndex);
    }
}
