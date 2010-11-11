package org.limewire.http.entity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ProducingNHttpEntity;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.timeout.StalledUploadWatchdog;

/**
 * An event based {@link HttpEntity} that uploads a {@link File}. A
 * corresponding {@link FileTransferMonitor} is updated with progress.
 */
public class FileNIOEntity extends FileEntity implements ProducingNHttpEntity {

    private static final Log LOG = LogFactory.getLog(FileNIOEntity.class);

    private final FileTransferMonitor transfer;

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

    private IOControl ioctrl;

    /** Cancels the transfer if inactivity for too long. */
    private StalledUploadWatchdog watchdog;

    private long timeout = StalledUploadWatchdog.DELAY_TIME;
    
    /** shutdownable to shut off in case of a timeout */
    private final Shutdownable timeoutable = new Shutdownable() {
        public void shutdown() {
            timeout();
        }
    };
    
    public FileNIOEntity(final File file, final String contentType,
           final FileTransferMonitor transfer, final long beginIndex, final long length) {
        super(file, contentType);

        this.transfer = transfer;
        this.file = file;
        this.begin = beginIndex;
        this.length = length;
        this.remaining = length;
        
        if (length < 0) {
            throw new IllegalStateException("upload end must be >= upload begin");
        }
    }

    public FileNIOEntity(File file, String contentType,
            FileTransferMonitor transfer) {
        this(file, contentType, transfer, 0, file.length());
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    @Override
    public long getContentLength() {
        return length;
    }
    
    @Override
    public InputStream getContent() throws IOException {
        final FileInputStream in = new FileInputStream(this.file);
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if (remaining == 0) {
                    return -1;
                }
                remaining--;
                return in.read();
            }
        };
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        if (outstream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        InputStream instream = new BufferedInputStream(new FileInputStream(
                this.file));
        try {
            byte[] tmp = new byte[4096];
            int l;
            instream.skip(this.begin);
            while (remaining > 0 && (l = instream.read(tmp, 0, //
                    (int) Math.min(remaining, tmp.length))) != -1) {
                outstream.write(tmp, 0, l);
                remaining -= l;
            }
            outstream.flush();
        } finally {
            instream.close();
        }
    }

    public File getFile() {
        return file;
    }

    public void initializeReader() throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Initializing upload of " + file.getName() + " [begin="
                    + begin + ",length=" + length + "]");

        if (length == 0) {
            // handle special case of empty file
            return;
        }

        transfer.start();

        reader = new FilePieceReader(NIODispatcher.instance().getBufferCache(),
                file, begin, length, new PieceHandler());
        reader.start();
    }

    public void initializeWriter() throws IOException {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public void finish() {
        deactivateTimeout();
        
        if (reader != null) {
            reader.shutdown();
            reader = null;
        }

        ioctrl = null;
    }

    public void produceContent(ContentEncoder encoder, IOControl ioctrl)
            throws IOException {
        if (this.ioctrl == null) {
            this.ioctrl = ioctrl;
            initializeReader();
        }
        
        // flush current buffer
        if (buffer != null && buffer.hasRemaining()) {
            int written = encoder.write(buffer);
            transfer.addAmountUploaded(written);
            if (buffer.hasRemaining()) {
                activateTimeout();
                return;
            } else if (remaining == 0) {
                reader.release(piece);
                encoder.complete();
                return;
            }
        } else if (remaining == 0) {
            // handle special case of empty file
            encoder.complete();
            return;            
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
                        return;
                    }
                    buffer = piece.getBuffer();
                    remaining -= buffer.remaining();
                }
            }

            if (LOG.isTraceEnabled())
                LOG.trace("Uploading " + file.getName() + " [read="
                        + buffer.remaining() + ",remaining=" + remaining + "]");

            written = encoder.write(buffer);
            transfer.addAmountUploaded(written);
        } while (written > 0 && remaining > 0);

        if (remaining == 0 && !buffer.hasRemaining()) {
            encoder.complete();
        } else {
            activateTimeout();
        }
    }

    protected void activateTimeout() {
        if (this.watchdog == null) {
            this.watchdog = new StalledUploadWatchdog(timeout, NIODispatcher.instance().getScheduledExecutorService());
        }
        this.watchdog.activate(timeoutable);
    }

    protected void deactivateTimeout() {
        if (this.watchdog != null) {
            this.watchdog.deactivate();
        }
    }

    public int consumeContent(ContentDecoder decoder, IOControl ioctrl)
            throws IOException {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [file=" + file.getName() + "]";
    }

    public void timeout() {
        if (LOG.isWarnEnabled())
            LOG.warn("File transfer timed out: " + transfer);
        transfer.timeout();
    }

    private class PieceHandler implements PieceListener {

        public void readFailed(IOException e) {
            if (LOG.isWarnEnabled())
                LOG.warn("Error reading file from disk: " + transfer, e);
            transfer.failed(e);
        }

        public void readSuccessful() {
            synchronized (FileNIOEntity.this) {
                ioctrl.requestOutput();
            }
        }

    }

}
