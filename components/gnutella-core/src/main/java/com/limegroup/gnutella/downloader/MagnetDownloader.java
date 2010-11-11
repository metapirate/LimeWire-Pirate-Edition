package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.browser.MagnetOptions;

public interface MagnetDownloader extends ManagedDownloader {

    public void setMagnet(MagnetOptions magnetOptions);

    public MagnetOptions getMagnet();
}