package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.InputStream;

import org.limewire.collection.IntervalSet;
import org.limewire.util.ConverterObjectInputStream;

import com.limegroup.gnutella.xml.SerialXml;

class DownloadConverterObjectInputStream extends ConverterObjectInputStream {
    
    static enum Version {
        Four16, Four14, Four11, Three0;
    }
    
    DownloadConverterObjectInputStream(InputStream in) throws IOException {
        super(in);
        deserializeVersion(Version.Four16);
    }
    
    void deserializeVersion(Version version) {
        revertToDefault();
        addCommon();
        switch(version) {
        case Four16:
            addLookup("com.limegroup.gnutella.downloader.ResumeDownloader", SerialResumeDownloader4x16.class.getName());
            addLookup("com.limegroup.gnutella.RemoteFileDesc", SerialRemoteFileDesc4x16.class.getName());
            break;
        case Four14:
            addLookup("com.limegroup.gnutella.downloader.ResumeDownloader", SerialResumeDownloader4x16.class.getName());
            addLookup("com.limegroup.gnutella.RemoteFileDesc", SerialRemoteFileDesc4x14.class.getName());
            break;
        case Four11:
            addLookup("com.limegroup.gnutella.RemoteFileDesc", SerialRemoteFileDesc4x14.class.getName());
            addLookup("com.limegroup.gnutella.downloader.ResumeDownloader", SerialResumeDownloader4x11.class.getName());
            break;
        case Three0:
            addLookup("com.limegroup.gnutella.RemoteFileDesc", SerialRemoteFileDesc3x0.class.getName());
            addLookup("com.limegroup.gnutella.downloader.ResumeDownloader", SerialResumeDownloader4x11.class.getName());            
            break;
        default:
            throw new IllegalArgumentException("invalid version: " + version);
        }
    }
    
    private void addCommon() {
        addLookup("com.limegroup.gnutella.downloader.AbstractDownloader", SerialRoot.class.getName());
        addLookup("com.limegroup.gnutella.downloader.ManagedDownloader", SerialManagedDownloaderImpl.class.getName());
        addLookup("com.limegroup.gnutella.downloader.StoreDownloader", SerialStoreDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.RequeryDownloader", SerialRequeryDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.MagnetDownloader", SerialMagnetDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.InNetworkDownloader", SerialInNetworkDownloader.class.getName());
        addLookup("com.limegroup.bittorrent.BTDownloader", SerialBTDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.IncompleteFileManager", SerialIncompleteFileManager.class.getName());
        addLookup("com.limegroup.gnutella.downloader.URLRemoteFileDesc", SerialUrlRemoteFileDesc.class.getName());
        addLookup("com.limegroup.gnutella.xml.LimeXMLDocument", SerialXml.class.getName());
        addLookup("com.limegroup.gnutella.downloader.Interval", "org.limewire.collection.Interval");
        addLookup("com.limegroup.gnutella.util.IntervalSet", IntervalSet.class.getName());
        addLookup("com.limegroup.gnutella.BandwidthTrackerImpl", SerialBandwidthTrackerImpl.class.getName());
        addLookup("org.apache.commons.httpclient.URI", SerialOldURI.class.getName());
        addLookup("com.limegroup.bittorrent.BTMetaInfo", SerialBTMetaInfo.class.getName());
        addLookup("com.limegroup.bittorrent.BTMetaInfo$SerialKeys", SerialBTMetaInfo.SerialKeys.class.getName());
        addLookup("com.limegroup.bittorrent.TorrentFileSystem", SerialTorrentFileSystem.class.getName());
        addLookup("com.limegroup.bittorrent.disk.VerifyingFolder$SerialData", SerialDiskManagerData.class.getName());
        addLookup("com.limegroup.bittorrent.disk.VerifyingFolder$BlockRangeMap", SerialBlockRangeMap.class.getName());
    }
}
