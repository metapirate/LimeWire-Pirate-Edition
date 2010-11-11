package org.limewire.core.api.friend;

import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.InvalidDataException;

public interface FileMetaDataConverter {
    
    /** Converts FileMetaData into a SearchResult. */
    SearchResult create(FriendPresence presence, FileMetaData fileMetaData) throws InvalidDataException, DownloadException;
}
