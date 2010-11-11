package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.limewire.http.entity.AbstractProducingNHttpEntity;
import org.limewire.http.entity.FilePieceReader;
import org.limewire.http.entity.Piece;
import org.limewire.http.entity.PieceListener;
import org.limewire.http.reactor.HttpIOSession;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Provider;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.Constants;

/**
 * An event based {@link HttpEntity} that uploads a {@link File}. A
 * corresponding {@link HTTPUploader} is updated with progress.
 */
public class FileResponseEntity extends AbstractProducingNHttpEntity {

    private static final Log LOG = LogFactory.getLog(FileResponseEntity.class);
    
    private final HTTPUploader uploader;

    private final File file;

    /** Buffer that is currently transferred. */
    private ByteBuffer buffer;

    /** Total number of bytes to transfer. */
    private long length;

    /** Offset of the first byte. */
    private long begin;

    /** Number of bytes remaining to be read from disk. */
    private long remaining;

    private FilePieceReader reader;

    /** Piece that is currently transferred. */
    private Piece piece;

    private final Provider<BandwidthManager> bandwidthManager;

    FileResponseEntity(HTTPUploader uploader, File file, Provider<BandwidthManager> bandwidthManager) {
        this.uploader = uploader;
        this.file = file;
        this.bandwidthManager = bandwidthManager;

        setContentType(Constants.FILE_MIME_TYPE);

        begin = uploader.getUploadBegin();
        long end = uploader.getUploadEnd();
        length = end - begin;
        remaining = length;
        
        if (length < 0) {
            throw new IllegalStateException("upload end must be >= upload begin");
        }
    }
    
    @Override
    public long getContentLength() {
        return length;
    }

    @Override
    public void initialize(ContentEncoder contentEncoder, IOControl ioctrl) {
        if (LOG.isDebugEnabled())
            LOG.debug("Initializing upload of " + file.getName() + " [begin=" + begin + ",length=" + length + "]");

        if (length == 0) {
            // handle special case of empty file upload
            return;
        }
        
        HttpIOSession ioSession = uploader.getSession().getIOSession();
        ioSession.setThrottle(bandwidthManager.get().getWriteThrottle(ioSession.getSocket()));

        reader = new FilePieceReader(NIODispatcher.instance().getBufferCache(), file, begin, length, new PieceHandler(ioctrl));
        reader.start();
    }
    
    public void finish() {
        if (LOG.isDebugEnabled())
            LOG.debug("Finished upload of " + file.getName() + " [begin=" + begin + ",length=" + length + ",remaining=" + remaining + "]");

        deactivateTimeout();
        if (reader != null) {
            reader.shutdown();
        }
    }
    
    @Override
    public boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
//        Throwable t = new Throwable();
//        LOG.debug(t, t);
        // flush current buffer
        if (buffer != null && buffer.hasRemaining()) {
            int written = contentEncoder.write(buffer);
            uploader.addAmountUploaded(written);
            if (buffer.hasRemaining()) {
                activateTimeout();
                return true;
            } else if (remaining == 0) {
                if (LOG.isTraceEnabled())
                    LOG.trace("... buffer drained and upload complete");
                reader.release(piece);
                return false;
            }
        } else if (remaining == 0) {
            if (LOG.isTraceEnabled())
                LOG.trace("upload complete");
            // handle special case of empty file upload
            return false;            
        }
        
        int written;
        do {
            if (buffer == null || !buffer.hasRemaining()) {
                if (piece != null) {
                    reader.release(piece);
                }

                // get next piece from file
                synchronized (this) {
                    piece = reader.next();
                    if (piece == null) {
                        // need to wait for the disk, PieceHandler will turn
                        // interest back on when the next piece is available
                        buffer = null;
                        ioctrl.suspendOutput();
                        activateTimeout();
                        if (LOG.isTraceEnabled())
                            LOG.trace("Waiting for file contents to be read");
                        return true;
                    }
                    buffer = piece.getBuffer();
                    remaining -= buffer.remaining();
                }
            }
            
            if (LOG.isTraceEnabled())
                LOG.trace("Uploading " + file.getName() + " [remaining=" + remaining + "+" + buffer.remaining() + "]");

            written = contentEncoder.write(buffer);
//            if (LOG.isTraceEnabled())
//                LOG.trace("wrote " + written + " bytes");
            uploader.addAmountUploaded(written);
        } while (written > 0 && remaining > 0);

        activateTimeout();
//        if (LOG.isTraceEnabled())
//                LOG.trace("returning " + (remaining > 0 || buffer.hasRemaining()) + " [remaining: " + remaining + ", buffer.hasRemaining(): " + buffer.hasRemaining() + "]");
        return remaining > 0 || buffer.hasRemaining();
    }

    @Override
    public void timeout() {
        if (LOG.isWarnEnabled())
            LOG.warn("File transfer timed out: " + uploader);
        uploader.stop();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [file=" + file.getName() + "]"; 
    }
    
    private class PieceHandler implements PieceListener {
        private final IOControl ioControl;
        
        public PieceHandler(IOControl ioControl) {
            this.ioControl = ioControl;
        }

        public void readFailed(IOException e) {
            if (LOG.isWarnEnabled())
                LOG.warn("Error reading file from disk: " + uploader, e);
            uploader.stop();
        }

        public void readSuccessful() {
            synchronized (FileResponseEntity.this) {
                LOG.debug("read successful");
                ioControl.requestOutput();
            }
        }

    }
    
}