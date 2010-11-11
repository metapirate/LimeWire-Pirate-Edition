package com.limegroup.gnutella.downloader.serial;

import com.limegroup.gnutella.browser.MagnetOptions;

public interface MagnetDownloadMemento extends GnutellaDownloadMemento {

    MagnetOptions getMagnet();

    void setMagnet(MagnetOptions magnet);

}
