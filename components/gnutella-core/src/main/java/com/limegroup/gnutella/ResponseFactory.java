package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface ResponseFactory {

    /**
     * Creates a fresh new response.
     * 
     * @requires index and size can fit in 4 unsigned bytes, i.e., 0 <= index,
     *           size < 2^32
     */
    public Response createResponse(long index, long size, String name, URN urn);

    /**
     * Creates a new response with parsed metadata. Typically this is used to
     * respond to query requests.
     * @param doc the metadata to include
     */
    public Response createResponse(long index, long size, String name,
            LimeXMLDocument doc, URN urn);
    
    /**
     * Constructs a new <tt>Response</tt> instance from the data in the
     * specified <tt>FileDesc</tt>. LimeXmlDocument is set by default if 
     * the data is available. It can be unset by calling response.setDocument(null)
     * to save bandwidth over the wire.
     * 
     * @param fd the <tt>FileDesc</tt> containing the data to construct this
     *        <tt>Response</tt> -- must not be <tt>null</tt>
     */
    public Response createResponse(FileDesc fd);
    
    /**
     * Constructs a new <tt>Response</tt> instance from the data in the
     * specified <tt>FileDesc</tt>. LimeXmlDocument is set by default if 
     * the data is available. It can be unset by calling response.setDocument(null)
     * to save bandwidth over the wire.
     * 
     * @param fd the <tt>FileDesc</tt> containing the data to construct this
     *        <tt>Response</tt> -- must not be <tt>null</tt>
     * @param includeNMS1Urn whether the response should contain the 
     * non-metadata sha1 or not
     */
    public Response createResponse(FileDesc fd, boolean includeNMS1Urn);

    /**
     * Factory method for instantiating individual responses from an
     * <tt>InputStream</tt> instance.
     * 
     * @param is the <tt>InputStream</tt> to read from
     * @throws <tt>IOException</tt> if there are any problems reading from or
     *         writing to the stream
     */
    public Response createFromStream(InputStream is) throws IOException;

}