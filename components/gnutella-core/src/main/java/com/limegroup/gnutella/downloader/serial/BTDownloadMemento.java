package com.limegroup.gnutella.downloader.serial;


/**
 * Defines an interface from which BitTorrent downloads
 * can be described and recreated.
 */
public interface BTDownloadMemento extends DownloadMemento {

    void setBtMetaInfoMemento(BTMetaInfoMemento btMetaInfo);

    BTMetaInfoMemento getBtMetaInfoMemento();

}