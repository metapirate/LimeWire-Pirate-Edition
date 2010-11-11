package com.limegroup.gnutella.uploader.authentication;

import java.util.Collections;

import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;

/**
 * Returns the file list for a public Gnutella browse of the client's shared
 * files.
 */
@Singleton
public class GnutellaBrowseFileViewProvider implements HttpRequestFileViewProvider {

    private final FileView gnutellaFileView;

    @Inject
    public GnutellaBrowseFileViewProvider(@GnutellaFiles FileView gnutellaFileView) {
        this.gnutellaFileView = gnutellaFileView;
    }
    
    /**
     * @return A {@link FileView} of files visible to gnutella.
     */
    @Override
    public Iterable<FileView> getFileViews(String userID, HttpContext httpContext) {
        return Collections.singletonList(gnutellaFileView);
    }

}