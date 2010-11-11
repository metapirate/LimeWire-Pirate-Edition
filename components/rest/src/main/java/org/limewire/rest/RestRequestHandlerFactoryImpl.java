package org.limewire.rest;

import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Default implementation for RestRequestHandlerFactory.
 */
class RestRequestHandlerFactoryImpl implements RestRequestHandlerFactory {

    private final Provider<LibraryRequestHandler> libraryHandlerFactory;
    private final Provider<SearchRequestHandler> searchHandlerFactory;
    private final Provider<DownloadRequestHandler> downloadHandlerFactory;
    private final Provider<StreamRequestHandler> streamHandlerFactory;
    
    /**
     * Constructs a request handler factory using the specified services.
     */
    @Inject
    public RestRequestHandlerFactoryImpl(
            Provider<LibraryRequestHandler> libraryHandlerFactory,
            Provider<SearchRequestHandler> searchHandlerFactory,
            Provider<DownloadRequestHandler> downloadHandlerFactory,
            Provider<StreamRequestHandler> streamHandlerFactory) {
        this.libraryHandlerFactory = libraryHandlerFactory;
        this.searchHandlerFactory = searchHandlerFactory;
        this.downloadHandlerFactory = downloadHandlerFactory;
        this.streamHandlerFactory = streamHandlerFactory;
    }
    
    @Override
    public NHttpRequestHandler createRequestHandler(RestPrefix restTarget) {
        switch (restTarget) {
        case HELLO:
            return new HelloRequestHandler();
        case LIBRARY:
            return libraryHandlerFactory.get();
        case SEARCH:
            return searchHandlerFactory.get();
        case DOWNLOAD:
            return downloadHandlerFactory.get();
        case STREAM:
            return streamHandlerFactory.get();
        default:
            return new UnknownRequestHandler();
        }
    }

    /**
     * Request handler for "hello world" service.
     */
    private static class HelloRequestHandler extends AbstractRestRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            
            if (RestUtils.GET.equals(request.getRequestLine().getMethod())) {
                // Set entity and status in response.
                Date dateTime = new Date(System.currentTimeMillis());
                HttpEntity entity = RestUtils.createStringEntity(dateTime.toString() + ": Hello world!");
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                
            } else {
                response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
            }
        }
    }
    
    /**
     * Request handler for an unknown service.
     */
    private static class UnknownRequestHandler extends AbstractRestRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
}
