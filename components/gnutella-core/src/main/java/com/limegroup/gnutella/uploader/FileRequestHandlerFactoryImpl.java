package com.limegroup.gnutella.uploader;

import org.limewire.http.auth.RequiresAuthentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandlerFactory;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

@Singleton
class FileRequestHandlerFactoryImpl implements FileRequestHandlerFactory {
    
    private final HTTPUploadSessionManager sessionManager;

    private final Library library;

    private final HTTPHeaderUtils httpHeaderUtils;

    private final HttpRequestHandlerFactory httpRequestHandlerFactory;

    private final Provider<CreationTimeCache> creationTimeCache;

    private final FileResponseEntityFactory fileResponseEntityFactory;

    private final AltLocManager altLocManager;

    private final AlternateLocationFactory alternateLocationFactory;

    private final Provider<DownloadManager> downloadManager;

    private final Provider<HashTreeCache> tigerTreeCache;

    private final PushEndpointFactory pushEndpointFactory;

    private final HashTreeWriteHandlerFactory tigerWriteHandlerFactory;
    
    @Inject
    public FileRequestHandlerFactoryImpl(HTTPUploadSessionManager sessionManager, Library library,
            HTTPHeaderUtils httpHeaderUtils, HttpRequestHandlerFactory httpRequestHandlerFactory,
            Provider<CreationTimeCache> creationTimeCache,
            FileResponseEntityFactory fileResponseEntityFactory, AltLocManager altLocManager,
            AlternateLocationFactory alternateLocationFactory,
            Provider<DownloadManager> downloadManager, Provider<HashTreeCache> tigerTreeCache,
            PushEndpointFactory pushEndpointFactory,
            HashTreeWriteHandlerFactory tigerWriteHandlerFactory) {
        this.sessionManager = sessionManager;
        this.library = library;
        this.httpHeaderUtils = httpHeaderUtils;
        this.httpRequestHandlerFactory = httpRequestHandlerFactory;
        this.creationTimeCache = creationTimeCache;
        this.fileResponseEntityFactory = fileResponseEntityFactory;
        this.altLocManager = altLocManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.downloadManager = downloadManager;
        this.tigerTreeCache = tigerTreeCache;
        this.pushEndpointFactory = pushEndpointFactory;
        this.tigerWriteHandlerFactory = tigerWriteHandlerFactory;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.uploader.FileRequestHandlerFactory#createFileRequestHandler(com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider, boolean)
     */
    public FileRequestHandler createFileRequestHandler(HttpRequestFileViewProvider fileListProvider, boolean requiresAuthentication) {
        if(!requiresAuthentication) {
            return new FileRequestHandler(sessionManager, library, httpHeaderUtils,
                    httpRequestHandlerFactory, creationTimeCache, fileResponseEntityFactory,
                    altLocManager, alternateLocationFactory, downloadManager, tigerTreeCache,
                    pushEndpointFactory, tigerWriteHandlerFactory, fileListProvider);
        } else {
            return new ProtectedFileRequestHandler(sessionManager, library, httpHeaderUtils,
                    httpRequestHandlerFactory, creationTimeCache, fileResponseEntityFactory,
                    altLocManager, alternateLocationFactory, downloadManager, tigerTreeCache,
                    pushEndpointFactory, tigerWriteHandlerFactory, fileListProvider);
        }
    }
    
    @RequiresAuthentication
    static class ProtectedFileRequestHandler extends FileRequestHandler {
        ProtectedFileRequestHandler(HTTPUploadSessionManager sessionManager, Library library, HTTPHeaderUtils httpHeaderUtils, HttpRequestHandlerFactory httpRequestHandlerFactory, Provider<CreationTimeCache> creationTimeCache, FileResponseEntityFactory fileResponseEntityFactory, AltLocManager altLocManager, AlternateLocationFactory alternateLocationFactory, Provider<DownloadManager> downloadManager, Provider<HashTreeCache> tigerTreeCache, PushEndpointFactory pushEndpointFactory, HashTreeWriteHandlerFactory tigerWriteHandlerFactory, HttpRequestFileViewProvider fileListProvider) {
            super(sessionManager, library, httpHeaderUtils, httpRequestHandlerFactory, creationTimeCache, fileResponseEntityFactory, altLocManager, alternateLocationFactory, downloadManager, tigerTreeCache, pushEndpointFactory, tigerWriteHandlerFactory, fileListProvider);
        }
    }

}
