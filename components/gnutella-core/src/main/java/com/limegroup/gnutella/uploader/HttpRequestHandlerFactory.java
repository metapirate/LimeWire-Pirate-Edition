package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

public interface HttpRequestHandlerFactory {

    public FileRequestHandler createFileRequestHandler(HttpRequestFileViewProvider fileRequestFileListProvider, boolean requiresAuthentication);
    
    public BrowseRequestHandler createBrowseRequestHandler(HttpRequestFileViewProvider browseRequestFileListProvider, boolean requiresAuthentication);

    public FreeLoaderRequestHandler createFreeLoaderRequestHandler();

    // TODO move LimitReachedRequestHandler into FileRequestHandler
    public LimitReachedRequestHandler createLimitReachedRequestHandler(HTTPUploader uploader);
    
    public HttpPushRequestHandler createPushProxyRequestHandler();

}