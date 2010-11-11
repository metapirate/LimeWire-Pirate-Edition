package com.limegroup.gnutella.downloader;

import java.io.File;

public interface ResumeDownloader extends ManagedDownloader {

    public void initIncompleteFile(File incompleteFile, long size);

}