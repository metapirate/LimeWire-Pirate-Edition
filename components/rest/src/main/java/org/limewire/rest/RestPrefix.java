package org.limewire.rest;

/**
 * REST API request path prefixes.
 */
public enum RestPrefix {
    HELLO("hello"), LIBRARY("library"), SEARCH("search"), DOWNLOAD("download"), STREAM("stream");
    
    private final String pattern;
    
    RestPrefix(String pattern) {
        this.pattern = pattern;
    }
    
    public String pattern() {
        return pattern;
    }
}
