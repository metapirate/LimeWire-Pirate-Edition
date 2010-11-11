package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

interface FileRequestHandlerFactory {

    FileRequestHandler createFileRequestHandler(
            HttpRequestFileViewProvider fileListProvider, boolean requiresAuthentication);

}