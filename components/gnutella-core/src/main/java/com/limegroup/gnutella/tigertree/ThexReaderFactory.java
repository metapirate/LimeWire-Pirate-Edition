package com.limegroup.gnutella.tigertree;

public interface ThexReaderFactory {
    
    public ThexReader createHashTreeReader(String sha1, String root32, long fileSize);

}
