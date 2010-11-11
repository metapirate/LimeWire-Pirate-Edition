package com.limegroup.gnutella.metadata.bittorrent;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTData.BTFileData;
import org.limewire.util.Base32;
import org.limewire.util.NameValue;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Allows accessing metadata about the torrent through the MetaData interface. 
 */
public class TorrentMetaData implements MetaData {

    private List<NameValue<String>> nameValues;

    public TorrentMetaData(BTData data) throws IOException {
        nameValues = Collections.unmodifiableList(buildNameValueList(data));
    }

    @Override
    public String getSchemaURI() {
        return LimeXMLNames.TORRENT_SCHEMA;
    }

    @Override
    public void populate(LimeXMLDocument doc) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private List<NameValue<String>> buildNameValueList(BTData data) throws IOException {
        NameValueListBuilder builder = new NameValueListBuilder();
        builder.add(LimeXMLNames.TORRENT_INFO_HASH, Base32.encode(data.getInfoHash()));

        List<URI> trackers = data.getTrackerUris();
        if (!trackers.isEmpty()) {
            // only add max of 3 trackers
            int maxSize = Math.min(trackers.size(), 3);
            String trackerUris = StringUtils.explode(trackers.subList(0, maxSize), " ");
            builder.add(LimeXMLNames.TORRENT_TRACKERS, trackerUris);
        }
        
        Long length = data.getLength();
        if (length != null) {
            builder.add(LimeXMLNames.TORRENT_LENGTH, length);
        }
        builder.add(LimeXMLNames.TORRENT_NAME, data.getName());

        boolean isPrivate = data.isPrivate();
        if (isPrivate) {
            builder.add(LimeXMLNames.TORRENT_PRIVATE, Boolean.TRUE.toString());
        }
        
        String uris = StringUtils.explode(data.getWebSeeds(), " ");
        if (uris.length() > 0) {
            builder.add(LimeXMLNames.TORRENT_WEBSEEDS, uris);
        }
        
        List<BTFileData> files = data.getFiles();
        if (files != null) {
            List<String> filePaths = new ArrayList<String>(files.size());
            List<Long> fileLengths = new ArrayList<Long>(files.size());
            for (BTFileData file : files) {
                filePaths.add(file.getPath());
                fileLengths.add(file.getLength());
            }
            builder.add(LimeXMLNames.TORRENT_FILE_PATHS, StringUtils.explode(filePaths, "//"));
            builder.add(LimeXMLNames.TORRENT_FILE_SIZES, StringUtils.explode(fileLengths, " "));
        }
        return builder.toList();
    }
    
    @Override
    public List<NameValue<String>> toNameValueList() {
        return nameValues;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    private static class NameValueListBuilder {

        private List<NameValue<String>> values = new ArrayList<NameValue<String>>();

        public void add(String name, String value) {
            values.add(new NameValue<String>(name, value));
        }

        public void add(String name, long value) {
            values.add(new NameValue<String>(name, Long.toString(value)));
        }

        List<NameValue<String>> toList() {
            return values;
        }
    }

}
