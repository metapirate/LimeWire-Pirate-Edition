package com.limegroup.gnutella.uploader;

import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.http.auth.RequiresAuthentication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileViewProvider;

@Singleton
public class BrowseRequestHandlerFactory {

    private final HTTPUploadSessionManager sessionManager;
    private final Provider<ResponseFactory> responseFactory;
    private final OutgoingQueryReplyFactory outgoingQueryReplyFactory;
    private final BrowseTracker tracker;

    @Inject
    public BrowseRequestHandlerFactory(HTTPUploadSessionManager sessionManager,
            Provider<ResponseFactory> responseFactory,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            BrowseTracker tracker) {
        this.sessionManager = sessionManager;
        this.responseFactory = responseFactory;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
        this.tracker = tracker;
    }
    
    public BrowseRequestHandler createBrowseRequestHandler(HttpRequestFileViewProvider browseRequestFileListProvider,
                                                           boolean requiresAuthentication) {
        if(!requiresAuthentication) {
            return new BrowseRequestHandler(sessionManager, responseFactory, outgoingQueryReplyFactory,
                    browseRequestFileListProvider, tracker);
        } else {
            return new ProtectedBrowseRequestHandler(sessionManager, responseFactory, outgoingQueryReplyFactory,
                    browseRequestFileListProvider);
        }
    }
    
    @RequiresAuthentication 
    class ProtectedBrowseRequestHandler extends BrowseRequestHandler {
        ProtectedBrowseRequestHandler(HTTPUploadSessionManager sessionManager, Provider<ResponseFactory> responseFactory, OutgoingQueryReplyFactory outgoingQueryReplyFactory, HttpRequestFileViewProvider browseRequestFileListProvider) {
            super(sessionManager, responseFactory, outgoingQueryReplyFactory, browseRequestFileListProvider, tracker);
        }
    }

}
