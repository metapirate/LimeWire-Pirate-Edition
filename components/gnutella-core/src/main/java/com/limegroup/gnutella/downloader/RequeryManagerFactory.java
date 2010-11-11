package com.limegroup.gnutella.downloader;

public interface RequeryManagerFactory {

    public RequeryManager createRequeryManager(
            RequeryListener requeryListener);

}