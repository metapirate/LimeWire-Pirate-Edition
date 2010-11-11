/**
 * 
 */
package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.ContentEncoderChannel;
import org.apache.http.nio.IOControl;
import org.limewire.core.settings.UploadSettings;
import org.limewire.http.entity.AbstractProducingNHttpEntity;
import org.limewire.nio.NBThrottle;

import com.limegroup.gnutella.tigertree.HashTreeWriteHandler;
import com.limegroup.gnutella.tigertree.ThexWriter;

/**
 * Sends a THEX tree as an HTTP message.
 *
 * The tree is in compliance with the THEX protocol at
 * http://open-content.net/specs/draft-jchapweske-thex-02.html
 */
public class THEXResponseEntity extends AbstractProducingNHttpEntity {

    /**
     * Throttle for the speed of THEX uploads, allow up to 0.5K/s
     */
    private static final NBThrottle THROTTLE =
        new NBThrottle(true, UploadSettings.THEX_UPLOAD_SPEED.getValue());

    private HTTPUploader uploader;
    
    private final HashTreeWriteHandler tigerWriteHandler;

    private ThexWriter writer;

    private long size;

    public THEXResponseEntity(HTTPUploader uploader, HashTreeWriteHandler tigerWriteHandler, long size) {
        this.uploader = uploader;
        this.tigerWriteHandler = tigerWriteHandler;
        this.size = size;

        setContentType(tigerWriteHandler.getOutputType());
    }

    @Override
    public long getContentLength() {
        return size;
    }

    @Override
    public void initialize(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
        this.writer = tigerWriteHandler.createAsyncWriter();
        
        THROTTLE.setRate(UploadSettings.THEX_UPLOAD_SPEED.getValue());
        uploader.getSession().getIOSession().setThrottle(THROTTLE);
    }

    @Override
    public boolean writeContent(ContentEncoder contentEncoder, IOControl ioctrl) throws IOException {
        boolean more = writer.process(new ContentEncoderChannel(contentEncoder), null);
        uploader.setAmountUploaded(writer.getAmountProcessed());
        activateTimeout();
        return more;
    }

    public void finish() {
        deactivateTimeout();
        this.writer = null;
    }

    @Override
    public void timeout() {
        uploader.stop();
    }

}