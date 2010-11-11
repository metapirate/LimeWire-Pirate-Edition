package com.limegroup.gnutella.uploader.authentication;

import java.util.Arrays;

import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFiles;

/**
 * Returns the file lists for public Gnutella uploads.
 */
@Singleton
public class GnutellaUploadFileViewProvider implements HttpRequestFileViewProvider {

    private final FileView gnutellaFileView;
    private final FileView incompleteFileView;
    
    @Inject
    public GnutellaUploadFileViewProvider(@GnutellaFiles FileView gnutellaFileView,
            @IncompleteFiles FileView incompleteFileView) {
        this.gnutellaFileView = gnutellaFileView;
        this.incompleteFileView = incompleteFileView;
    }

    /**
     * @return a combination of FileViews of files visible to gnutella & incomplete files.
     */
    @Override
    public Iterable<FileView> getFileViews(String userID, HttpContext httpContext) {
        return Arrays.asList(gnutellaFileView, incompleteFileView);
    }
}