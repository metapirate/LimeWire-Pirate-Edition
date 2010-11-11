package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.OutputStream;

public interface HashTreeWriteHandler {

    /**
     * Method for writing a HashTree to an OutputStream.
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to os.
     */
    public void write(OutputStream os) throws IOException;
    
    /**
     * Determines the length of the written data.
     */
    public int getOutputLength() ;

    /**
     * Determines the mime type of the output.
     */
    public String getOutputType();

    public ThexWriter createAsyncWriter();
    
}
