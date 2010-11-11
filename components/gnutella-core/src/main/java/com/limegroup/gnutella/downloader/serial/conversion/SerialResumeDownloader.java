package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;

import com.limegroup.gnutella.URN;

interface SerialResumeDownloader extends SerialManagedDownloader {

    public File getIncompleteFile();

    public String getName();

    public long getSize();

    public URN getUrn();

}