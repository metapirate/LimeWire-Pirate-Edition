package com.limegroup.gnutella;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class ResponseImpl implements Response {

    //private static final Log LOG = LogFactory.getLog(Response.class);
    
    /** Both index and size must fit into 4 unsigned bytes; see
     *  constructor for details. */
    private final long index;
    private final long size;

    /**
     * The bytes for the name string, guaranteed to be non-null.
     */
    private final byte[] nameBytes;

    /** The name of the file matching the search.  This does NOT
     *  include the double null terminator.
     */
    private final String name;
    
    /** 
     *  The size of the byte array for the response object
     */
    private final int incomingNameByteArraySize;

    /** The document representing the XML in this response. */
    private LimeXMLDocument document;

    /** 
     * The <tt>Set</tt> of <tt>URN</tt> instances for this <tt>Response</tt>,
     * as specified in HUGE v0.94.  This is guaranteed to be non-null, 
     * although it is often empty.
     */
    private final Set<URN> urns;

    /**
     * The bytes between the nulls for the <tt>Response</tt>, as specified
     * in HUGE v0.94.  This is guaranteed to be non-null, although it can be
     * an empty array.
     */
    private final byte[] extBytes;
    
    /**
     * The cached RemoteFileDesc created from this Response.
     */
    private volatile RemoteFileDesc cachedRFD;
        
    /**
     * If this is a response for a metafile, i.e. a file
     * that itself triggers another download.
     */
    private final boolean isMetaFile;
    
    /** The alternate locations for this Response. */
    private final Set<? extends IpPort> alternateLocations;
    
    /** The creation time for this Response. */
    private final long creationTime;
    
    /** Ranges carried in this response, null if none */
    private final IntervalSet ranges;
    
    /** If the ranges carried in this response are verified */
    private final boolean verified;

    /**
     * Can be null if not set, empty array or array of compressed xml bytes.
     */
    private byte[] compressedXmlBytes = null;

    /**
     * Overloaded constructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instances.  This 
     * is the primary constructor that establishes all of the class's 
     * invariants, does any necessary parameter validation, etc.
     *
     * If extensions is non-null, it is used as the extBytes instead
     * of creating them from the urns and locations.
     *
     * @param index the index of the file referenced in the response
     * @param size the size of the file (in bytes)
     * @param name the name of the file
     * @param incomingNameByteArraySize the number of bytes that were 
     *  used to encode the file name in the message, used to compute 
     *  the correct message length when reading from the network
     * @param urns the <tt>Set</tt> of <tt>URN</tt> instances associated
     *  with the file
     * @param doc the <tt>LimeXMLDocument</tt> instance associated with
     *  the file
     * @param alternateLocations Other hosts with this file 
     * @param extensions The raw unparsed extension bytes.
     * @param ranges Ranges of data to be represented by this response
     */
    public ResponseImpl(long index, long size, String name,
                     int incomingNameByteArraySize, Set<? extends URN> urns, 
                     LimeXMLDocument doc,
                     Set<? extends IpPort> alternateLocations,
                     long creationTime, byte[] extensions, IntervalSet ranges, 
                     boolean verified) {
        
                
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IllegalArgumentException("invalid index: " + index);
        // see note in createFromStream about Integer.MAX_VALUE
        if (size < 0 || size > MAX_FILE_SIZE)
            throw new IllegalArgumentException("invalid size: " + size);
            
        this.index=index;
        this.size=size;
        
        if (name == null)
            this.name = "";
        else 
            this.name = name;
        
        isMetaFile = this.name.toLowerCase(Locale.US).endsWith(".torrent");

        byte[] temp = null;
        try {
            temp = this.name.getBytes("UTF-8");
        } catch(UnsupportedEncodingException namex) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            ErrorService.error(namex);
        }
        this.nameBytes = temp;

        if (urns == null)
            this.urns = Collections.emptySet();
        else
            this.urns = Collections.unmodifiableSet(urns);
        
        this.alternateLocations = alternateLocations;
        this.creationTime = creationTime;
        this.extBytes = extensions;
        
        this.incomingNameByteArraySize = incomingNameByteArraySize;

        this.document = doc;
        this.ranges = ranges;
        this.verified = verified;
    }
  
    public void writeToStream(OutputStream os) throws IOException {
        ByteUtils.int2leb((int)index, os);
        if (size > Integer.MAX_VALUE) 
            ByteUtils.int2leb(0xFFFFFFFF, os);
        else
            ByteUtils.int2leb((int)size, os);
        for (int i = 0; i < nameBytes.length; i++)
            os.write(nameBytes[i]);
        //Write first null terminator.
        os.write(0);
        // write HUGE v0.93 General Extension Mechanism extensions
        // (currently just URNs)
        for (int i = 0; i < extBytes.length; i++)
            os.write(extBytes[i]);
        //add the second null terminator
        os.write(0);
    }

    public void setDocument(LimeXMLDocument doc) {
        document = doc;
    }
       
    public int getIncomingLength() {
        // must match same number of bytes of Response when initially read from the network
        if(incomingNameByteArraySize != -1){
            return 8 +                   // index and size
            incomingNameByteArraySize +
            1 +                   // null
            extBytes.length +
            1;                    // final null
        }
        return 8 + nameBytes.length + 1 + extBytes.length + 1;
    }
   
    public long getIndex() {
        return index;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public LimeXMLDocument getDocument() {
        return document;
    }

    public Set<URN> getUrns() {
        return urns;
    }
    
    public Set<? extends IpPort> getLocations() {
        return alternateLocations;
    }
    
    public long getCreateTime() {
        return creationTime;
    }    
    
    public boolean isMetaFile() {
        return isMetaFile;
    }
    
    public byte[] getExtBytes() {
        return extBytes;
    }
    
    public IntervalSet getRanges() {
        return ranges;
    }
    
    public boolean isVerified() {
        return verified;
    }

    public RemoteFileDesc toRemoteFileDesc(QueryReply queryReply, Address address, RemoteFileDescFactory remoteFileDescFactory, PushEndpointFactory pushEndpointFactory) throws UnknownHostException {
        // TODO fberger move this to query reply, since all responses can share a common address
        if (address == null) {
            if (queryReply.isFirewalled()) {
                address = pushEndpointFactory.createPushEndpoint(queryReply.getClientGUID(), queryReply.getPushProxies(), PushEndpoint.PLAIN, queryReply.getFWTransferVersion(), new ConnectableImpl(queryReply.getIP(), queryReply.getPort(), queryReply.isTLSCapable()));
            } else {
                address = new ConnectableImpl(queryReply.getIP(), queryReply.getPort(), queryReply.isTLSCapable());
            }
        }
        if (cachedRFD != null && cachedRFD.getAddress().equals(address)) {
            return cachedRFD;
        } else {
            Set<URN> urns = getUrns();
            RemoteFileDesc rfd = remoteFileDescFactory.createRemoteFileDesc(
                    address, getIndex(), getName(), getSize(),
                    queryReply.getClientGUID(), queryReply.getSpeed(),
                    queryReply.calculateQualityOfService(),
                    queryReply.getSupportsBrowseHost(), getDocument(), urns,
                    queryReply.isReplyToMulticastQuery(),
                    queryReply.getVendor(), getCreateTime(), !urns.isEmpty(),
                    queryReply.getGUID());
            cachedRFD = rfd;
            return rfd;
        }
    }

    /**
     * Overrides equals to check that these two responses are equal.
     * Raw extension bytes are not checked, because they may be
     * extensions that do not change equality, such as
     * otherLocations.
     */
    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if (! (o instanceof Response))
            return false;
        Response r=(Response)o;
        return getIndex() == r.getIndex() &&
               getSize() == r.getSize() &&
               getName().equals(r.getName()) &&
               ((getDocument() == null) ? (r.getDocument() == null) :
               getDocument().equals(r.getDocument())) &&
               getUrns().equals(r.getUrns());
    }


    @Override
    public int hashCode() {
        return  (int)((31 * 31 * getName().hashCode() + 31 * getSize()+getIndex()));
    }

    /**
     * Overrides Object.toString to print out a more informative message.
     */
    @Override
    public String toString() {
        return ("index:        "+index+"\r\n"+
                "size:         "+size+"\r\n"+
                "name:         "+name+"\r\n"+
                "xml document: "+document+"\r\n"+
                "urns:         "+urns);
    }

    @Override
    public int getWireSize() {
        int size = 10; // 10: see writeToStream()
        size += nameBytes.length;
        size += extBytes.length;
        return size;
    }

    @Override
    public byte[] getCompressedXmlBytes() {
        return compressedXmlBytes;
    }

    @Override
    public void setCompressedXmlBytes(byte[] compressedXmlBytes) {
        this.compressedXmlBytes = compressedXmlBytes;
    }
}
