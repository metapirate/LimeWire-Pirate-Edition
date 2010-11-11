package org.limewire.friend.api.feature;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.friend.api.FileMetaData;

public class FileOfferFeature extends Feature<FileMetaData> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/file-offer/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public FileOfferFeature(FileMetaData feature) {
        super(feature, ID);
    }

    public FileOfferFeature() {
        super(ID);
    }
}
