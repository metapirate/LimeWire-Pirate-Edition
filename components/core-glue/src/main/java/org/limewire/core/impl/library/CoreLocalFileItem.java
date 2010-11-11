package org.limewire.core.impl.library;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.impl.InvalidURN;
import org.limewire.core.impl.TorrentFactory;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.LocalFileDetailsFactory;

class CoreLocalFileItem implements LocalFileItem , Comparable {

    private final Category category;
    private final FileDesc fileDesc;
    private final LocalFileDetailsFactory detailsFactory;
    private final CreationTimeCache creationTimeCache;
    private final TorrentFactory torrentFactory;

    @Inject
    public CoreLocalFileItem(@Assisted FileDesc fileDesc, LocalFileDetailsFactory detailsFactory,
            CreationTimeCache creationTimeCache, CategoryManager categoryManager,
            TorrentFactory torrentFactory) {
        this.fileDesc = fileDesc;
        this.detailsFactory = detailsFactory;
        this.creationTimeCache = creationTimeCache;
        this.torrentFactory = torrentFactory;
        this.category = categoryManager.getCategoryForFile(fileDesc.getFile());
    }

    @Override
    public long getCreationTime() {
        URN sha1 = fileDesc.getSHA1Urn();
        if(sha1 != null) {
            return creationTimeCache.getCreationTimeAsLong(sha1);
        } else {
            return -1;
        }
    }

    @Override
    public File getFile() {
        return fileDesc.getFile();
    }

    @Override
    public long getLastModifiedTime() {
        return fileDesc.lastModified();
    }

    @Override
    public String getName() {
        return FileUtils.getFilenameNoExtension(fileDesc.getFileName());
    }

    @Override
    public long getSize() {
        return fileDesc.getFileSize();
    }

    @Override
    public int getNumHits() {
        return fileDesc.getHitCount();
    }

    @Override
    public int getNumUploads() {
        return fileDesc.getCompletedUploads();
    }
    
    @Override
    public int getNumUploadAttempts() {
        return getFileDesc().getAttemptedUploads();
    }  

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public Object getProperty(FilePropertyKey property) {
        switch(property) {
        case LOCATION:
            return getFile().getParent();
        case NAME:
            return getName();
        case DATE_CREATED:
            long ct = getCreationTime();
            return ct == -1 ? null : ct;
        case FILE_SIZE:
            return getSize();       
        case TORRENT:
            return torrentFactory.createTorrentFromXML(fileDesc.getXMLDocument());
        default:
            return FilePropertyKeyPopulator.get(category, property, fileDesc.getXMLDocument());
        }
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }

    @Override
    public FileMetaData toMetadata() {
        FileMetaDataImpl fileMetaData = new FileMetaDataImpl();
        fileMetaData.setCreateTime(new Date(getCreationTime()));
        fileMetaData.setDescription(""); // TODO
        fileMetaData.setId(fileDesc.getSHA1Urn().toString());
        fileMetaData.setIndex(fileDesc.getIndex());
        fileMetaData.setName(fileDesc.getFileName());
        fileMetaData.setSize(fileDesc.getFileSize());
        Set<String> urns = new HashSet<String>();
        for(URN urn : fileDesc.getUrns()) {
            urns.add(urn.toString());
        }
        fileMetaData.setURNs(urns);
        return fileMetaData;
    }

    public FileDetails getFileDetails() {
        return detailsFactory.create(fileDesc);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFile() == null) ? 0 : getFile().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return getFile().equals(((CoreLocalFileItem) obj).getFile());
    }

    @Override
    public String toString() {
        return "CoreLocalFileItem for: " + fileDesc;
    }

    @Override
    public String getFileName() {
        return fileDesc.getFileName();
    }

    @Override
    public boolean isShareable() {
        return !InvalidURN.instance.equals(getUrn()) && fileDesc.isShareable() && !isIncomplete();
    }
    
    @Override
    public org.limewire.core.api.URN getUrn() {
        URN urn = fileDesc.getSHA1Urn();
        if(urn != null) {
            return urn;
        } else {
            return InvalidURN.instance;
        }
    }

    @Override
    public boolean isIncomplete() {
        return fileDesc instanceof IncompleteFileDesc;
    }

    public FileDesc getFileDesc() {
        return fileDesc;
    }

    @Override
    public int compareTo(Object obj) {
        if (getClass() != obj.getClass()) {
            return -1;
        }
        return Objects.compareToNullIgnoreCase(getFileName(), ((CoreLocalFileItem) obj).getFileName(), true);
    }

    @Override
    public boolean isLoaded() {
        return !InvalidURN.instance.equals(getUrn());
    }
}
