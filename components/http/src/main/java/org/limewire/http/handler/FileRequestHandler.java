package org.limewire.http.handler;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.entity.BasicFileTransferMonitor;
import org.limewire.http.entity.FileNIOEntity;
import org.limewire.http.entity.NotFoundEntity;

/**
 * Generic request handler that serves files from the local file system. A root
 * directory must be specified and only files below that directory will be
 * served. Requests that contain relative path names are rejected. Browsing of
 * directories is not supported.
 */
public class FileRequestHandler extends SimpleNHttpRequestHandler {

    private String indexFilename = "index.html";
    
    private final File rootDirectory;

    private final MimeTypeProvider mimeTypeProvider;

    private int timeout = -1;
    
    /**
     * Constructs a request handler. The <code>rootDirectory</code> specifies
     * the root directory of the web server, i.e. a request for <code>/</code>
     * will be mapped to this directory.
     * 
     * @param rootDirectory only files below this directory are served
     * @param mimeTypeProvider used to determine the mime type of a file
     */
    public FileRequestHandler(File rootDirectory, MimeTypeProvider mimeTypeProvider) {
        this.rootDirectory = rootDirectory;
        this.mimeTypeProvider = mimeTypeProvider;
    }
    
    /**
     * If a directory is requested it is searched for a file named
     * <code>indexFilename</code>. The default is <code>index.html</code>.
     * 
     * @param indexFilename the name of the index file
     */
    public void setIndexFilename(String indexFilename) {
        this.indexFilename = indexFilename;
    }

    public String getIndexFilename() {
        return indexFilename;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        File file = getFile(request);         
        if (file.exists() && file.isFile()) {
            String mimeType = mimeTypeProvider.getMimeType(file);
            FileNIOEntity entity = new FileNIOEntity(file, mimeType , new BasicFileTransferMonitor(context));
            if (timeout != -1) {
                entity.setTimeout(timeout);
            }
            response.setEntity(entity);
        } else {
            response.setEntity(new NotFoundEntity(request));
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);                
        }
    }

    private File getFile(HttpRequest request) throws HttpException {
        if (!request.getRequestLine().getUri().startsWith("/")) {
            throw new ProtocolException("Invalid request");
        }
        
        File file = rootDirectory;
        StringTokenizer t = new StringTokenizer(request.getRequestLine().getUri(), "/");
        while (t.hasMoreTokens()) {
            String next = t.nextToken();
            if (next.indexOf(File.pathSeparator) != -1) {
                throw new ProtocolException("Invalid request");
            }
            if (next.indexOf("..") != -1) {
                throw new ProtocolException("Invalid request");
            }
            file = new File(file, next);
        }
        
        if (file.isDirectory()) {
            file = new File(file, indexFilename);
        }
        
        return file;
    }
}
