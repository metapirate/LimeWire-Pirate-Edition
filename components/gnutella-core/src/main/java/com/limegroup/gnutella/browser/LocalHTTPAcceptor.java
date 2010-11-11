package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.BasicHttpAcceptor;
import org.limewire.http.auth.AuthenticationInterceptor;
import org.limewire.inject.EagerSingleton;

import com.google.inject.Inject;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.util.LimeWireUtils;

@EagerSingleton
public class LocalHTTPAcceptor extends BasicHttpAcceptor {

    private static final Log LOG = LogFactory.getLog(LocalHTTPAcceptor.class);

    private static final String[] SUPPORTED_METHODS = new String[] { "GET",
        "HEAD", "POST", "DELETE"};
    
    private final Executor magnetExecutor = ExecutorsHelper.newProcessingQueue("magnet-handler");

    /** Magnet request for a default action on parameters */
//    private static final String MAGNET_DEFAULT = "/magnet10/default.js?";

    /** Magnet request for a paused response */
//    private static final String MAGNET_PAUSE = "/magnet10/pause";

    /** Start of Magnet URI */
    private static final String MAGNET = "magnet:?";

    /** Magnet detail command */
    private static final String MAGNET_DETAIL = "magcmd/detail?";

    private String lastCommand;

    private long lastCommandTime;

    private long MIN_REQUEST_INTERVAL = 1500;

    private final ExternalControl externalControl;

    @Inject
    public LocalHTTPAcceptor(ExternalControl externalControl,
                        AuthenticationInterceptor requestAuthenticator) {
        super(createDefaultParams(LimeWireUtils.getHttpServer(), Constants.TIMEOUT),
                requestAuthenticator, SUPPORTED_METHODS);
        this.externalControl = externalControl;
        
        registerHandler("magnet:", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/default.js", new MagnetCommandRequestHandler());
        registerHandler("/magnet10/pause", new MagnetPauseRequestHandler());
        registerHandler("/magcmd/detail", new MagnetDetailRequestHandler());
        // TODO figure out which files we want to serve from the local file system
        //registerHandler("*", new FileRequestHandler(new File("root"), new BasicMimeTypeProvider()));
    }
   
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Magnet Processor");
    }
    
    private class MagnetCommandRequestHandler extends SimpleNHttpRequestHandler  {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            final String uri = request.getRequestLine().getUri();
            magnetExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        triggerMagnetHandling(uri);
                    } catch(IOException ignored) {}
                }
            });
        }
    }

    private class MagnetPauseRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            response.setStatusCode(HttpStatus.SC_NO_CONTENT);
            magnetExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                    }
                }
            });
        }
    }

    private static class MagnetDetailRequestHandler extends SimpleNHttpRequestHandler {
        public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                HttpContext context) throws HttpException, IOException {
            return null;
        }
        
        @Override
        public void handle(HttpRequest request, HttpResponse response,
                HttpContext context) throws HttpException, IOException {
            String uri = request.getRequestLine().getUri();
            int i = uri.indexOf(MAGNET_DETAIL);
            String command = uri.substring(i + MAGNET_DETAIL.length());
            String page = MagnetHTML.buildMagnetDetailPage(command);
            NStringEntity entity = new NStringEntity(page);
            entity.setContentType("text/html");
            response.setEntity(entity);
        }
    }

    private synchronized void triggerMagnetHandling(String uri)
            throws IOException {
        int i = uri.indexOf("?");
        if (i == -1) {
            throw new IOException("Invalid command");
        }
        String command = uri.substring(i + 1);

        // suppress duplicate requests from some browsers
        long currentTime = System.currentTimeMillis();
        if (!command.equals(lastCommand) || (currentTime - lastCommandTime) >= MIN_REQUEST_INTERVAL) {
            // trigger an operation
            externalControl.handleMagnetRequest(MAGNET + command);
            lastCommand = command;
            lastCommandTime = currentTime;

        } else {
            LOG.warn("Ignoring duplicate request: " + command);
        }
    }
    
}
