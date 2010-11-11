package com.limegroup.gnutella.messages;

import java.util.Set;

import org.limewire.io.IpPort;
import org.limewire.security.SecurityToken;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.Message.Network;

public interface QueryReplyFactory {

    /**
     * Creates a new query reply. The number of responses is responses.length
     * The Browse Host GGEP extension is ON by default.
     * 
     * @requires
     *           0 < port < 2^16 (i.e., can fit in 2 unsigned bytes),
     *           ip.length==4 and ip is in <i>BIG-endian</i> byte order, 0 <
     *           speed < 2^32 (i.e., can fit in 4 unsigned bytes),
     *           responses.length < 2^8 (i.e., can fit in 1 unsigned byte),
     *           clientGUID.length==16
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            boolean isMulticastReply);

    /**
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD. The QHD with
     * the LIME vendor code and the given busy and push flags. Note that this
     * constructor has no support for undefined push or busy bits. The Browse
     * Host GGEP extension is ON by default.
     * 
     * @param needsPush true iff this is firewalled and the downloader should
     *        attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload
     *        slots.
     * @param finishedUpload true iff this server has successfully finished an
     *        upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *        user
     * @param supportsChat true iff the host currently allows chatting.
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            boolean needsPush, boolean isBusy, boolean finishedUpload,
            boolean measuredSpeed, boolean supportsChat,
            boolean isMulticastReply);

    /**
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD. The QHD with
     * the LIME vendor code and the given busy and push flags. Note that this
     * constructor has no support for undefined push or busy bits. The Browse
     * Host GGEP extension is ON by default.
     * 
     * @param xmlBytes the (non-null) byte[] containing aggregated and indexed
     *        information regarding file metadata. In terms of byte-size, this
     *        should not be bigger than 65535 bytes. Anything larger will result
     *        in an Exception being throw. This String is assumed to consist of
     *        compressed data.
     * @param needsPush true iff this is firewalled and the downloader should
     *        attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload
     *        slots
     * @param finishedUpload true iff this server has successfully finished an
     *        upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *        user
     * @param supportsChat true iff the host currently allows chatting.
     * @exception IllegalArgumentException Thrown if xmlBytes.length >
     *            XML_MAX_SIZE
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            byte[] xmlBytes, boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,
            boolean supportsChat, boolean isMulticastReply);

    /**
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD. The QHD with
     * the LIME vendor code and the given busy and push flags. Note that this
     * constructor has no support for undefined push or busy bits. The Browse
     * Host GGEP extension is ON by default.
     * 
     * @param xmlBytes the (non-null) byte[] containing aggregated and indexed
     *        information regarding file metadata. In terms of byte-size, this
     *        should not be bigger than 65535 bytes. Anything larger will result
     *        in an Exception being throw. This String is assumed to consist of
     *        compressed data.
     * @param needsPush true iff this is firewalled and the downloader should
     *        attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload
     *        slots
     * @param finishedUpload true iff this server has successfully finished an
     *        upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *        user
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces. will be included in the
     *        replies GGEP extension.
     * @exception IllegalArgumentException Thrown if xmlBytes.length >
     *            XML_MAX_SIZE
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            byte[] xmlBytes, boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,
            boolean supportsChat, boolean isMulticastReply,
            Set<? extends IpPort> proxies);

    /**
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD. The QHD with
     * the LIME vendor code and the given busy and push flags. Note that this
     * constructor has no support for undefined push or busy bits. The Browse
     * Host GGEP extension is ON by default.
     * 
     * @param xmlBytes the (non-null) byte[] containing aggregated and indexed
     *        information regarding file metadata. In terms of byte-size, this
     *        should not be bigger than 65535 bytes. Anything larger will result
     *        in an Exception being throw. This String is assumed to consist of
     *        compressed data.
     * @param needsPush true iff this is firewalled and the downloader should
     *        attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload
     *        slots
     * @param finishedUpload true iff this server has successfully finished an
     *        upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *        user
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces. will be included in the
     *        replies GGEP extension.
     * @param the security token to echo along with the query reply
     * @exception IllegalArgumentException Thrown if xmlBytes.length >
     *            XML_MAX_SIZE
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            byte[] xmlBytes, boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,
            boolean supportsChat, boolean isMulticastReply,
            Set<? extends IpPort> proxies, SecurityToken securityToken);

    /**
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD. The QHD with
     * the LIME vendor code and the given busy and push flags. Note that this
     * constructor has no support for undefined push or busy bits. The Browse
     * Host GGEP extension is ON by default.
     * 
     * @param needsPush true iff this is firewalled and the downloader should
     *        attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload
     *        slots
     * @param finishedUpload true iff this server has successfully finished an
     *        upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *        user
     * @param xmlBytes The (non-null) byte[] containing aggregated and indexed
     *        information regarding file metadata. In terms of byte-size, this
     *        should not be bigger than 65535 bytes. Anything larger will result
     *        in an Exception being throw. This String is assumed to consist of
     *        compressed data.
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces. will be included in the
     *        replies GGEP extension.
     * @exception IllegalArgumentException Thrown if xmlBytes.length >
     *            XML_MAX_SIZE
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            byte[] xmlBytes, boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,
            boolean supportsChat, boolean isMulticastReply,
            boolean supportsFWTransfer, Set<? extends IpPort> proxies);

    /**
     * Creates a new QueryReply with a BearShare 2.2.0-style QHD. The QHD with
     * the LIME vendor code and the given busy and push flags. Note that this
     * constructor has no support for undefined push or busy bits. The Browse
     * Host GGEP extension is ON by default.
     * 
     * @param xmlBytes the (non-null) byte[] containing aggregated and indexed
     *        information regarding file metadata. In terms of byte-size, this
     *        should not be bigger than 65535 bytes. Anything larger will result
     *        in an Exception being throw. This String is assumed to consist of
     *        compressed data.
     * @param needsPush true iff this is firewalled and the downloader should
     *        attempt a push without trying a normal download.
     * @param isBusy true iff this server is busy, i.e., has no more upload
     *        slots
     * @param finishedUpload true iff this server has successfully finished an
     *        upload
     * @param measuredSpeed true iff speed is measured, not as reported by the
     *        user
     * @param supportsChat true iff the host currently allows chatting.
     * @param proxies an array of PushProxy interfaces. will be included in the
     *        replies GGEP extension.
     * @param securityToken might be null
     * @exception IllegalArgumentException Thrown if xmlBytes.length >
     *            XML_MAX_SIZE
     */
    public QueryReply createQueryReply(byte[] guid, byte ttl, int port,
            byte[] ip, long speed, Response[] responses, byte[] clientGUID,
            byte[] xmlBytes, boolean needsPush, boolean isBusy,
            boolean finishedUpload, boolean measuredSpeed,
            boolean supportsChat, boolean isMulticastReply,
            boolean supportsFWTransfer, Set<? extends IpPort> proxies,
            SecurityToken securityToken);

    /** Creates a new query reply with data read from the network. */
    public QueryReply createFromNetwork(byte[] guid, byte ttl, byte hops,
            byte[] payload) throws BadPacketException;

    /**
     * Copy constructor. Creates a new query reply from the passed query Reply.
     * The new one is same as the passed one, but with different specified GUID.
     * <p>
     * 
     * Note: The payload is not really copied, but the reference in the newly
     * constructed query reply, points to the one in the passed reply. But since
     * the payload cannot be mutated, it shouldn't make difference if different
     * query replies maintain reference to same payload
     * 
     * @param guid the new GUID for the reply
     * @param reply the query reply from where to copy the fields into the new
     *        constructed query reply
     */
    public QueryReply createQueryReply(byte[] guid, QueryReply reply);
    
    /**
     * Copy constructor, appending information about the return path. 
     * @param original the original query
     * @param me this host.
     * @param source the source that the reply came from
     * @return original reply with return path info.
     */
    public QueryReply createWithReturnPathInfo(QueryReply original, IpPort me, IpPort source);

    /**
     * Copy constructor.
     * @param address 4 byte IP address.
     * @return original reply but with the new address.
     */
    public QueryReply createWithNewAddress(byte [] address, QueryReply original);
    
    public QueryReply createFromNetwork(byte[] guid, byte ttl, byte hops,
            byte[] payload, Network network) throws BadPacketException;

}
