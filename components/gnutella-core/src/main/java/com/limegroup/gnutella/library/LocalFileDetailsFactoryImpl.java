package com.limegroup.gnutella.library;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
class LocalFileDetailsFactoryImpl implements LocalFileDetailsFactory {
    
    private final CreationTimeCache creationTimeCache;
    
    @Inject
    public LocalFileDetailsFactoryImpl(CreationTimeCache creationTimeCache) {
        this.creationTimeCache = creationTimeCache;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LocalFileDetailsFactory#create(com.limegroup.gnutella.FileDesc)
     */
    public FileDetails create(final FileDesc fd) {
        return new FileDetails() {
            public String getFileName() {
                return fd.getFileName();
            }

            public long getSize() {
                return fd.getFileSize();
            }

            public URN getSHA1Urn() {
                return fd.getSHA1Urn();
            }

            public Set<URN> getUrns() {
                return fd.getUrns();
            }

            public LimeXMLDocument getXMLDocument() {
                return fd.getXMLDocument();
            }

            public long getIndex() {
                return fd.getIndex();
            }

            public long getCreationTime() {
                if(fd.getSHA1Urn() != null) {
                    return creationTimeCache.getCreationTimeAsLong(fd.getSHA1Urn());
                } else {
                    return -1;
                }
            }
        };
    }

}
