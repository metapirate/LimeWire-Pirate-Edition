package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.rudp.RUDPUtils;
import org.limewire.security.SecurityToken;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.util.DataUtils;

/**
 * A query reply. Contains information about the responding host in addition to
 * an array of responses. These responses are not parsed until the getResponses
 * method is called. For efficiency reasons, bad query reply packets may not be
 * discovered until the getResponses methods are called.
 * <p>
 * 
 * This class has partial support for BearShare-style query reply trailers. You
 * can extract the vendor code, push flag, and busy flag. These methods may
 * throw BadPacketException if the metadata cannot be extracted. Note that
 * BadPacketException does not mean that other data (namely responses) cannot be
 * read; MissingDataException might have been a better name.
 * <p>
 * This class also encapsulates xml metadata. See the description of the QHD
 * below for more details.
 */
public class QueryReplyImpl extends AbstractMessage implements QueryReply {
    // WARNING: see note in Message about IP addresses.

    /** The mask for extracting the push flag from the QHD common area. */
    private static final byte PUSH_MASK = (byte) 0x01;

    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte BUSY_MASK = (byte) 0x04;

    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte UPLOADED_MASK = (byte) 0x08;

    /** The mask for extracting the busy flag from the QHD common area. */
    private static final byte SPEED_MASK = (byte) 0x10;

    /** The mask for extracting the GGEP flag from the QHD common area. */
    static final byte GGEP_MASK = (byte) 0x20;

    /** The mask for extracting the chat flag from the QHD private area. */
    private static final byte CHAT_MASK = (byte) 0x01;

    /** Our static and final instance of the GGEPUtil helper class. */
    private static final GGEPUtil _ggepUtil = new GGEPUtil();

    /** the payload. */
    private byte[] _payload;

    /** The raw ip address of the host returning the hit. */
    private byte[] _address = new byte[4];

    /** Whether or not this message has been verified as secure. */
    private Status _secureStatus = Status.INSECURE;

    /** True if the responses and metadata have been extracted. */
    private boolean _parsed = false;

    /** The parsed query reply data. */
    private volatile QueryReplyData _data;

    // TODO move to QueryReply decorator?
    /** Whether or not this reply is allowed to have MCAST. */
    private volatile boolean _multicastAllowed = false;

    /** The cached clientGUID. */
    private byte[] clientGUID = null;

    private final NetworkManager networkManager;

    private final NetworkInstanceUtils networkInstanceUtils;

    private final ResponseFactory responseFactory;

    private final boolean local;

    private volatile boolean badPacket;

    QueryReplyImpl(byte[] guid, byte ttl, byte hops, byte[] payload, Network network,
            NetworkInstanceUtils networkInstanceUtils, NetworkManager networkManager,
            ResponseFactory responseFactory) throws BadPacketException {
        super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length, network);
        this.networkManager = networkManager;
        this.networkInstanceUtils = networkInstanceUtils;
        this.responseFactory = responseFactory;
        this._payload = payload;
        this.badPacket = true;

        if (!NetworkUtils.isValidPort(getPort())) {
            throw new BadPacketException("invalid port");
        }

        // 0xFFFFFFFF00000000L = Integer.MIN_VALUE * 2
        if ((getSpeedFromPayload() & 0xFFFFFFFF00000000L) != 0) {
            throw new BadPacketException("invalid speed: " + getSpeedFromPayload());
        }

        setAddress();

        if (!NetworkUtils.isValidAddress(getIPBytes())) {
            throw new BadPacketException("invalid address");
        }

        this.local = false;
        // repOk();
    }

    protected QueryReplyImpl(byte[] guid, byte ttl, int port, byte[] ip, long speed,
            Response[] responses, byte[] clientGUID, byte[] xmlBytes, boolean includeQHD,
            boolean needsPush, boolean isBusy, boolean finishedUpload, boolean measuredSpeed,
            boolean supportsChat, boolean supportsBH, boolean isMulticastReply,
            boolean supportsFWTransfer, Set<? extends IpPort> proxies, SecurityToken securityToken,
            NetworkInstanceUtils networkInstanceUtils, NetworkManager networkManager,
            ResponseFactory responseFactory) {
        super(guid, Message.F_QUERY_REPLY, ttl, (byte) 0, 0, Network.UNKNOWN);

        this.networkManager = networkManager;
        this.networkInstanceUtils = networkInstanceUtils;
        this.responseFactory = responseFactory;
        this.local = true;
        this.badPacket = true;

        if (xmlBytes.length > XML_MAX_SIZE)
            throw new IllegalArgumentException("xml too large: "
                    + StringUtils.getUTF8String(xmlBytes));

        final int n = responses.length;
        if (!NetworkUtils.isValidPort(port)) {
            throw new IllegalArgumentException("invalid port: " + port);
        } else if (ip.length != 4) {
            throw new IllegalArgumentException("invalid ip length: " + ip.length);
        } else if (!NetworkUtils.isValidAddress(ip)) {
            throw new IllegalArgumentException("invalid address: " + NetworkUtils.ip2string(ip));
        } else if ((speed & 0xFFFFFFFF00000000l) != 0) {
            throw new IllegalArgumentException("invalid speed: " + speed);
        } else if (n >= 256) {
            throw new IllegalArgumentException("invalid num responses: " + n);
        }

        _data = new QueryReplyData();
        _data.setXmlBytes(xmlBytes);
        _data.setProxies(proxies);
        _data.setSupportsFWTransfer(supportsFWTransfer);
        _data.setSecurityToken(securityToken != null ? securityToken.getBytes() : null);
        boolean supportsTLS = networkManager.isIncomingTLSEnabled();
        _data.setTLSCapable(supportsTLS);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // Write beginning of payload.
            // Downcasts are OK, even if they go negative
            baos.write(n);
            ByteUtils.short2leb((short) port, baos);
            baos.write(ip, 0, ip.length);
            ByteUtils.int2leb((int) speed, baos);

            // Write each response
            for (int left = n; left > 0; left--) {
                Response r = responses[n - left];
                r.writeToStream(baos);
            }
            // Write QHD if desired
            if (includeQHD || securityToken != null) {
                // a) vendor code. This is hard coded here for simplicity,
                // efficiency, and to prevent character decoding problems. If
                // you
                // change this, be sure to change CommonUtils.QHD_VENDOR_NAME as
                // well.
                baos.write(76); // 'L'
                baos.write(73); // 'I'
                baos.write(77); // 'M'
                baos.write(69); // 'E'

                // b) payload length
                baos.write(COMMON_PAYLOAD_LEN);

                // size of standard, no options, ggep block...
                int ggepLen = _ggepUtil.getQRGGEP(false, false, false, false, IpPort.EMPTY_SET,
                        null).length;

                // c) PART 1: common area flags and controls. See format in
                // parseResults2.
                boolean hasProxies = (proxies != null) && (proxies.size() > 0);
                byte flags = (byte) ((needsPush && !isMulticastReply ? PUSH_MASK : 0) | BUSY_MASK
                        | UPLOADED_MASK | SPEED_MASK | GGEP_MASK);
                byte controls = (byte) (PUSH_MASK | (isBusy && !isMulticastReply ? BUSY_MASK : 0)
                        | (finishedUpload ? UPLOADED_MASK : 0)
                        | (measuredSpeed || isMulticastReply ? SPEED_MASK : 0) | (supportsBH
                        || isMulticastReply || hasProxies || supportsFWTransfer
                        || securityToken != null || supportsTLS ? GGEP_MASK
                        : (ggepLen > 0 ? GGEP_MASK : 0)));
                baos.write(flags);
                baos.write(controls);

                // d) PART 2: size of xmlBytes + 1.
                int xmlSize = xmlBytes.length + 1;
                if (xmlSize > XML_MAX_SIZE)
                    xmlSize = XML_MAX_SIZE; // yes, truncate!
                ByteUtils.short2leb(((short) xmlSize), baos);

                // e) private area: one byte with flags
                // for chat support
                byte chatSupport = supportsChat ? CHAT_MASK : 0;
                baos.write(chatSupport);

                // f) the GGEP block
                byte[] ggepBytes = _ggepUtil.getQRGGEP(supportsBH, isMulticastReply,
                        supportsFWTransfer, supportsTLS, proxies, securityToken);
                baos.write(ggepBytes, 0, ggepBytes.length);

                writeSecureGGEP(baos, xmlBytes);

                // g) actual xml.
                baos.write(xmlBytes, 0, xmlBytes.length);

                // write null after xml, as specified
                baos.write(0);
            }

            // Write footer
            baos.write(clientGUID, 0, 16);

            // setup payload params
            _payload = baos.toByteArray();
            updateLength(_payload.length);
        } catch (IOException reallyBad) {
            ErrorService.error(reallyBad);
        }

        setAddress();
    }

    public void validate() throws BadPacketException {
        parseResults();
        if (badPacket) {
            throw new BadPacketException();
        }
    }

    /** Writes the 'secureGGEP' GGEP. */
    protected void writeSecureGGEP(ByteArrayOutputStream out, byte[] xml) {
        // writes the secure ggep portion.
        // don't forget to secure the null after the XML also.
    }

    /**
     * Sets the IP address bytes.
     */
    private void setAddress() {
        _address[0] = _payload[3];
        _address[1] = _payload[4];
        _address[2] = _payload[5];
        _address[3] = _payload[6];
    }

    public void setOOBAddress(InetAddress addr, int port) {
        _address = addr.getAddress();
        ByteUtils.short2leb((short) port, _payload, 1);

    }

    /**
     * Sets the guid for this message. Is needed, when we want to cache query
     * replies or for some other reason want to change the GUID as per the guid
     * of query request.
     * 
     * @param guid the guid to be set
     */
    @Override
    public void setGUID(GUID guid) {
        super.setGUID(guid);
    }

    // inherit doc comment
    @Override
    public void writePayload(OutputStream out) throws IOException {
        out.write(_payload);
    }

    /**
     * Sets this reply to be considered a 'browse host' reply.
     */
    public void setBrowseHostReply(boolean isBH) {
        parseResults();
        _data.setBrowseHostReply(isBH);
    }

    /**
     * Gets whether or not this reply is from a browse host request.
     */
    public boolean isBrowseHostReply() {
        parseResults();
        return _data.isBrowseHostReply();
    }

    /**
     * Return the associated xml metadata string if the query reply contained
     * one.
     */
    public byte[] getXMLBytes() {
        parseResults();
        return _data.getXmlBytes();
    }

    /** Return the number of results N in this query. */
    public short getResultCount() {
        // The result of ubyte2int always fits in a short, so downcast is OK.
        return (short) ByteUtils.ubyte2int(_payload[0]);
    }

    public short getPartialResultCount() {
        parseResults();
        return _data.getPartialResultCount();
    }

    /**
     * @return the number of unique results (per SHA1) carried in this message
     */
    public short getUniqueResultCount() {
        parseResults();
        return _data.getUniqueResultURNs();
    }

    public int getPort() {
        return ByteUtils.ushort2int(ByteUtils.leb2short(_payload, 1));
    }

    /**
     * Returns the IP address of the responding host in standard dotted-decimal
     * format, e.g., "192.168.0.1".
     */
    public String getIP() {
        return NetworkUtils.ip2string(_address); // takes care of signs
    }

    /**
     * Accessor the IP address in byte array form.
     * 
     * @return the IP address for this query hit as an array of bytes
     */
    public byte[] getIPBytes() {
        return _address;
    }

    private long getSpeedFromPayload() {
        return ByteUtils.uint2long(ByteUtils.leb2int(_payload, 7));
    }

    public int getSpeed() {
        // TODO move to QueryReply decorator?
        return isReplyToMulticastQuery() ? Integer.MAX_VALUE : ByteUtils
                .long2int(getSpeedFromPayload()); // safe cast;
    }

    /**
     * Returns the Response[]. Throws BadPacketException if this data couldn't
     * be extracted.
     */
    public Response[] getResultsArray() throws BadPacketException {
        parseResults();
        Response[] responses = _data.getResponses();
        if (responses == null)
            throw new BadPacketException();
        return responses;
    }

    /**
     * Returns an iterator that will yield the results, each as an instance of
     * the Response class. Throws BadPacketException if this data couldn't be
     * extracted.
     */
    public Iterator<Response> getResults() throws BadPacketException {
        return getResultsAsList().iterator();
    }

    /**
     * Returns a List that will yield the results, each as an instance of the
     * Response class. Throws BadPacketException if this data couldn't be
     * extracted.
     */
    public List<Response> getResultsAsList() throws BadPacketException {
        return Arrays.asList(getResultsArray());
    }

    /**
     * Returns the name of this' vendor, all capitalized. Throws
     * BadPacketException if the data couldn't be extracted, either because it
     * is missing or corrupted.
     */
    private String getVendorFromPayload() throws BadPacketException {
        parseResults();
        String vendor = _data.getVendor();
        if (vendor == null)
            throw new BadPacketException();
        return vendor;
    }

    public String getVendor() {
        // TODO move to QueryReply decorator?
        try {
            return getVendorFromPayload();
        } catch (BadPacketException e) {
            return "";
        }
    }

    /**
     * Returns true if this's push flag is set, i.e., a push download is needed.
     * Returns false if the flag is present but not set. Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.
     */
    public boolean getNeedsPush() throws BadPacketException {
        parseResults();

        switch (_data.getPushFlag()) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            throw new IllegalStateException("Bad value for push flag: " + _data.getPushFlag());
        }
    }

    /**
     * Returns true if this has no more download slots. Returns false if the
     * busy bit is present but not set. Throws BadPacketException if the flag
     * couldn't be extracted, either because it is missing or corrupted.
     */
    public boolean getIsBusy() throws BadPacketException {
        parseResults();

        switch (_data.getBusyFlag()) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            throw new IllegalStateException("Bad value for busy flag: " + _data.getBusyFlag());
        }
    }

    /**
     * Returns true if this has successfully uploaded a complete file (bit set).
     * Returns false if the bit is not set. Throws BadPacketException if the
     * flag couldn't be extracted, either because it is missing or corrupted.
     */
    public boolean getHadSuccessfulUpload() throws BadPacketException {
        parseResults();

        switch (_data.getUploadedFlag()) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            throw new IllegalStateException("Bad value for uploaded flag: "
                    + _data.getUploadedFlag());
        }
    }

    /**
     * Returns true if the speed in this QueryReply was measured (bit set).
     * Returns false if it was set by the user (bit unset). Throws
     * BadPacketException if the flag couldn't be extracted, either because it
     * is missing or corrupted.
     */
    public boolean getIsMeasuredSpeed() throws BadPacketException {
        parseResults();

        switch (_data.getMeasuredSpeedFlag()) {
        case UNDEFINED:
            throw new BadPacketException();
        case TRUE:
            return true;
        case FALSE:
            return false;
        default:
            throw new IllegalStateException("Bad value for measured speed flag: "
                    + _data.getMeasuredSpeedFlag());
        }
    }

    /** Returns the bytes of the signature from the secure GGEP block. */
    public byte[] getSecureSignature() {
        parseResults();
        SecureGGEPData sg = _data.getSecureGGEP();
        if (sg != null) {
            try {
                return sg.getGGEP().getBytes(GGEPKeys.GGEP_HEADER_SIGNATURE);
            } catch (BadGGEPPropertyException bgpe) {
                return null;
            }
        } else {
            return null;
        }
    }

    /** Passes in the appropriate bytes of the payload to the signature. */
    public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
        parseResults();
        SecureGGEPData sg = _data.getSecureGGEP();
        if (sg != null) {
            signature.update(_payload, 0, sg.getStartIndex());
            int end = sg.getEndIndex();
            int length = _payload.length - 16 - end;
            signature.update(_payload, end, length);
        }
    }

    /** Determines if the message was verified. */
    public synchronized Status getSecureStatus() {
        return _secureStatus;
    }

    /** Sets whether or not the message is verified. */
    public synchronized void setSecureStatus(Status secureStatus) {
        this._secureStatus = secureStatus;
    }

    /** Returns true iff this client supports TLS. */
    public boolean isTLSCapable() {
        parseResults();
        return _data.isTLSCapable();
    }

    private boolean getSupportsChatFromPayload() {
        parseResults();
        return _data.isSupportsChat();
    }

    /**
     * Returns true iff the client supports chat.
     */
    public boolean getSupportsChat() {
        // TODO move to QueryReply decorator?
        boolean firewalled = isFirewalledHack();
        return getSupportsChatFromPayload() && !firewalled;
    }

    private boolean isFirewalledHack() {
        // In theory, this method should be removed,
        // and callers should use isFirewalled().
        // However, that code also takes into account whether
        // the message was multicast, whereas the legacy piece of
        // code that did the getSupportsChat logic did *not*
        // use that information; need to verify with the team
        // that replacing this with isFirewalled() is ok.
        boolean firewalled;
        try {
            firewalled = getNeedsPush() || networkInstanceUtils.isPrivateAddress(getIP());
        } catch (BadPacketException e) {
            firewalled = true;
        }
        return firewalled;
    }

    /**
     * @return true if the remote host can firewalled transfers.
     */
    public boolean getSupportsFWTransfer() {
        parseResults();
        return _data.isSupportsFWTransfer();
    }

    /**
     * @return 1 or greater if FW Transfer is supported, else 0.
     */
    public byte getFWTransferVersion() {
        parseResults();
        return _data.getFwTransferVersion();
    }

    /**
     * Returns true iff the client supports browse host feature.
     */
    public boolean getSupportsBrowseHost() {
        parseResults();
        return _data.isSupportsBrowseHost();
    }

    /**
     * Returns true iff the reply was sent in response to a multicast query.
     * 
     * @return true, iff the reply was sent in response to a multicast query,
     *         false otherwise
     * @exception Throws BadPacketException if the flag couldn't be extracted,
     *            either because it is missing or corrupted. Typically this
     *            exception is treated the same way as returning false.
     */
    public boolean isReplyToMulticastQuery() {
        parseResults();
        return _multicastAllowed && _data.isReplyToMulticast();
    }

    /** Sets whether or not this reply is allowed to have an MCAST field. */
    public void setMulticastAllowed(boolean allowed) {
        _multicastAllowed = allowed;
    }

    /** Returns true if this reply tried to fake an MCAST field. */
    public boolean isFakeMulticast() {
        parseResults();
        return !_multicastAllowed && _data.isReplyToMulticast();
    }

    /**
     * @return null or a non-zero length array of PushProxy hosts.
     */
    public Set<? extends IpPort> getPushProxies() {
        parseResults();
        return _data.getProxies();
    }

    /**
     * Returns the message authentication bytes that were sent along with this
     * query reply or null the none have been sent.
     */
    public byte[] getSecurityToken() {
        parseResults();
        return _data.getSecurityToken();
    }

    /**
     * Determines if this result has secure data. This does NOT determine if the
     * result has been verified as secure.
     */
    public boolean hasSecureData() {
        parseResults();
        return _data.getSecureGGEP() != null;
    }

    /**
     * @modifies _data
     * @effects tries to extract responses from payload and store in responses.
     */
    private synchronized void parseResults() {
        if (_parsed)
            return;
        _parsed = true;
        parseResults2();
    }

    /**
     * Parses the individual results for the hit. If any one of the results is
     * invalid, none of them will be initialized, and the accessor methods for
     * this class will all throw <tt>BadPacketException</tt>. This is because a
     * single invalid response invalidates other invariants, such as the field
     * for the number of results matching the size of the result array.
     */
    private void parseResults2() {
        // index into payload to look for next response
        int i = 11;

        _data = new QueryReplyData();

        // 1. Extract responses. These are not copied to this.responses until
        // they are verified. Note, however that the metai nformation need not be
        // verified for these to be acceptable. Also note that exceptions are
        // silently caught.
        int left = getResultCount(); // number of records left to get

        // sanity check
        if (left > FilterSettings.MAX_RESPONSES_PER_REPLY.getValue())
            return;

        Response[] responses = new Response[left];
        Set<URN> urns = new HashSet<URN>(responses.length); // set for the urns
                                                            // carried in this
                                                            // reply
        short uniqueURNs = 0;
        short partialURNs = 0;
        try {
            InputStream bais = new ByteArrayInputStream(_payload, i, _payload.length - i);
            // For each record...
            for (; left > 0; left--) {
                Response r = responseFactory.createFromStream(bais);
                if (r.getRanges() != null && r.getRanges().getSize() > 0)
                    partialURNs++;
                responses[responses.length - left] = r;
                i += r.getIncomingLength();

                if (r.getUrns().isEmpty())
                    uniqueURNs++;
                else
                    urns.addAll(r.getUrns());
            }
            // All set. Accept parsed results.
            _data.setResponses(responses);
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        } catch (IOException e) {
            return;
        }

        // remember how many unique urns this reply carries
        uniqueURNs += (short) urns.size();
        _data.setUniqueResultURNs(uniqueURNs);
        _data.setPartialResultCount((short) Math.min(uniqueURNs, partialURNs));

        // 2. Extract BearShare-style meta information, if any. Any exceptions
        // are silently caught. The definitive reference for this format is at
        // http://www.clip2.com/GnutellaProtocol04.pdf. Briefly, the format is
        // vendor code (4 bytes, case insensitive)
        // common payload length (4 byte, unsigned, always>0)
        // common payload (length given above. See below.)
        // vendor payload (length until clientGUID)
        // The normal 16 byte clientGUID follows, of course.
        //
        // The first byte of the common payload has a one in its 0'th bit* if we
        // should try a push. However, if there is a second byte, and if the
        // 0'th bit of this byte is zero, the 0'th bit of the first byte should
        // actually be interpreted as MAYBE. Unfortunately LimeWire 1.4 failed
        // to set this bit in the second byte, so it should be ignored when
        // parsing, though set on writing.
        //
        // The remaining bits of the first byte of the common payload area tell
        // whether the corresponding bits in the optional second byte is
        // defined.
        // The idea behind having two bits per flag is to distinguish between
        // YES, NO, and MAYBE. These bits are as followed:
        // bit 1* undefined, for historical reasons
        // bit 2 1 iff server is busy
        // bit 3 1 iff server has successfully completed an upload
        // bit 4 1 iff server's reported speed was actually measured, not
        // simply set by the user.
        //
        // GGEP Stuff
        // Byte 5 and 6, if the 5th bit is set, signal that there is a GGEP
        // block. The GGEP block will be after the common payload and will be
        // headed by the GGEP magic prefix (see the GGEP class for more details.
        //
        // If there is a GGEP block, then we look to see what is supported.
        //
        // *Here, we use 0-(N-1) numbering. So "0'th bit" refers to the least
        // significant bit.
        /*
         * ---------------------------------------------------------------- QHD
         * UPDATE 8/17/01 Here is an updated QHD spec.
         * 
         * Byte 0-3 : Vendor Code Byte 4 : Public area size (COMMON_PAYLOAD_LEN)
         * Byte 5-6 : Public area (as described above) Byte 7-8 : Size of XML +
         * 1 (for a null), you need to count backward from the client GUID. Byte
         * 9 : private vendor flag Byte 10-X: GGEP area (may contain multiple
         * GGEP blocks) Byte X-beginning of xml : (new) private area Byte
         * (payload.length - 16 - xmlSize (above)) - (payload.length - 16 - 1) :
         * XML!! Byte (payload.length - 16 - 1) : NULL Last 16 Bytes: client
         * GUID.
         */
        try {
            if (i >= (_payload.length - 16)) { // see above
                throw new BadPacketException("No QHD");
            }

            // Attempt to verify. Results are not copied to this until verified.
            String vendorT = null;
            int pushFlagT = UNDEFINED;
            int busyFlagT = UNDEFINED;
            int uploadedFlagT = UNDEFINED;
            int measuredSpeedFlagT = UNDEFINED;
            boolean supportsChatT = false;
            boolean supportsBrowseHostT = false;
            boolean replyToMulticastT = false;
            Set<? extends IpPort> proxies = IpPort.EMPTY_SET;
            byte[] securityToken = null;
            boolean supportsTLST = false;

            // a) extract vendor code
            try {
                // Must use ISO encoding since characters are more than two
                // bytes on other platforms.
                vendorT = new String(_payload, i, 4, "ISO-8859-1");
                assert vendorT.length() == 4 : "Vendor length wrong.  Wrong character encoding?";
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("No support for ISO-8859-1 encoding");
            }
            i += 4;

            // b) extract payload length
            int length = ByteUtils.ubyte2int(_payload[i]);
            if (length <= 0)
                throw new BadPacketException("Common payload length zero.");

            i++;
            if ((i + length) > (_payload.length - 16)) // 16 is trailing GUID
                                                       // size
                throw new BadPacketException("Common payload length imprecise!");
            _data.setQHDOffset(i - 1);

            // c) extract push and busy bits from common payload
            // REMEMBER THAT THE PUSH BIT IS SET OPPOSITE THAN THE OTHERS.
            // (The 'I understand' is the second bit, the Yes/No is the first)
            if (length > 1) { // BearShare 2.2.0+
                byte control = _payload[i];
                byte flags = _payload[i + 1];
                if ((flags & PUSH_MASK) != 0)
                    pushFlagT = (control & PUSH_MASK) == 1 ? TRUE : FALSE;
                if ((control & BUSY_MASK) != 0)
                    busyFlagT = (flags & BUSY_MASK) != 0 ? TRUE : FALSE;
                if ((control & UPLOADED_MASK) != 0)
                    uploadedFlagT = (flags & UPLOADED_MASK) != 0 ? TRUE : FALSE;
                if ((control & SPEED_MASK) != 0)
                    measuredSpeedFlagT = (flags & SPEED_MASK) != 0 ? TRUE : FALSE;
                if ((control & GGEP_MASK) != 0 && (flags & GGEP_MASK) != 0) {
                    GGEPParser parser = new GGEPParser();
                    parser.scanForGGEPs(_payload, i + 2);
                    GGEP ggep = parser.getNormalGGEP();
                    if (ggep != null) {
                        _data.setGGEPStart(parser.getNormalStartIndex());
                        _data.setGGEPEnd(parser.getNormalEndIndex());
                        try {
                            supportsBrowseHostT = ggep.hasKey(GGEPKeys.GGEP_HEADER_BROWSE_HOST);
                            if (ggep.hasKey(GGEPKeys.GGEP_HEADER_FW_TRANS)) {
                                _data.setFwTransferVersion(ggep
                                        .getBytes(GGEPKeys.GGEP_HEADER_FW_TRANS)[0]);
                                _data.setSupportsFWTransfer(_data.getFwTransferVersion() > 0);
                            }
                            replyToMulticastT = ggep
                                    .hasKey(GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE);
                            proxies = _ggepUtil.getPushProxies(ggep);
                            if (ggep.hasKey(GGEPKeys.GGEP_HEADER_SECURE_OOB)) {
                                securityToken = ggep.getBytes(GGEPKeys.GGEP_HEADER_SECURE_OOB);
                                if (securityToken == null || securityToken.length == 0) {
                                    throw new BadPacketException(
                                            "Message had empty OOB security token");
                                }
                            }
                            supportsTLST = ggep.hasKey(GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
                        } catch (BadGGEPPropertyException bgpe) {
                        }
                    }
                    // store the data about the secure result, if it's there.
                    if (parser.getSecureGGEP() != null) {
                        _data.setSecureGGEP(new SecureGGEPData(parser));
                    }
                }
                i += 2; // increment used bytes appropriately...
            }

            if (length > 2) { // expecting XML.
                // d) we need to get the xml stuff.
                // first we should get its size, then we have to look
                // backwards and get the actual xml...
                int a, b, temp;
                temp = ByteUtils.ubyte2int(_payload[i++]);
                a = temp;
                temp = ByteUtils.ubyte2int(_payload[i++]);
                b = temp << 8;
                int xmlSize = a | b;
                if (xmlSize > 1) {
                    int xmlInPayloadIndex = _payload.length - 16 - xmlSize;
                    byte[] xmlBytes = new byte[xmlSize - 1];
                    System.arraycopy(_payload, xmlInPayloadIndex, xmlBytes, 0, (xmlSize - 1));
                    _data.setXmlBytes(xmlBytes);
                } else
                    _data.setXmlBytes(DataUtils.EMPTY_BYTE_ARRAY);
            }

            // Parse LimeWire's private area. Currently only a single byte
            // whose LSB is 0x1 if we support chat, or 0x0 if we do.
            // Shareaza also supports our chat, don't disclude them...
            int privateLength = _payload.length - i;
            if (privateLength > 0 && (vendorT.equals("LIME") || vendorT.equals("RAZA"))) {
                byte privateFlags = _payload[i];
                supportsChatT = (privateFlags & CHAT_MASK) != 0;
            }

            if (i > _payload.length - 16)
                throw new BadPacketException("Common payload length too large.");

            // All set. Accept parsed values.
            _data.setVendor(vendorT.toUpperCase(Locale.US));
            _data.setPushFlag(pushFlagT);
            _data.setBusyFlag(busyFlagT);
            _data.setUploadedFlag(uploadedFlagT);
            _data.setMeasuredSpeedFlag(measuredSpeedFlagT);
            _data.setSupportsChat(supportsChatT);
            _data.setSupportsBrowseHost(supportsBrowseHostT);
            _data.setReplyToMulticast(replyToMulticastT);
            _data.setProxies(proxies);
            _data.setSecurityToken(securityToken);
            _data.setTLSCapable(supportsTLST);

            badPacket = false;
        } catch (BadPacketException e) {
            return;
        } catch (IndexOutOfBoundsException e) {
            return;
        }
    }

    /**
     * Returns the 16 byte client ID (i.e., the "footer") of the responding
     * host.
     */
    public byte[] getClientGUID() {
        if (clientGUID == null) {
            byte[] result = new byte[16];
            // Copy the last 16 bytes of payload to result. Note that there may
            // be meta information before the client GUID. So it is not correct
            // to simply count after the last result record.
            int length = super.getLength();
            System.arraycopy(_payload, length - 16, result, 0, 16);
            clientGUID = result;
        }
        return clientGUID;
    }

    public byte[] getPayload() {
        return _payload;
    }

    @Override
    public String toString() {
        return ("QueryReply::\r\n" + getResultCount() + " hits\r\n" + super.toString() + "\r\n"
                + "ip: " + getIP() + "\r\n");
    }

    /**
     * This method calculates the quality of service for a given host. The
     * calculation is some function of whether or not the host is busy, whether
     * or not the host has ever received an incoming connection, etc. Search
     * results may be discarded if their quality is too low.
     * 
     * @return a value from -1 to 4 indicating the predicted quality of the
     * download connection, where -1 means there's no way to establish a
     * connection.
     */
    public int calculateQualityOfService() {
        if (Arrays.equals(_address, networkManager.getAddress()))
            return 3; // same address -- display it
        if (isReplyToMulticastQuery())
            return 4; // multicast, maybe busy (but doesn't matter)

        /* Is the local host firewalled? */
        boolean iFirewalled = !networkManager.acceptedIncomingConnection();

        /* Is the remote host firewalled? */
        int heFirewalled;
        if (networkInstanceUtils.isPrivateAddress(_address))
            heFirewalled = TRUE;
        else
            heFirewalled = _data.getPushFlag();

        /* Can both hosts do firewall transfers? */
        if (getSupportsFWTransfer() && networkManager.canDoFWT()) {
            iFirewalled = false;
            heFirewalled = FALSE;
        }

        if (iFirewalled && heFirewalled == TRUE)
            return -1; // both firewalled; transfer impossible

        /* Is the remote host busy? */
        int busy = _data.getBusyFlag();

        if (busy == UNDEFINED || heFirewalled == UNDEFINED) {
            return 0; // * older client; can't tell
        } else if (busy == TRUE) {
            assert heFirewalled == FALSE || !iFirewalled;
            if (heFirewalled == TRUE)
                return 0; // * busy, push
            else
                return 1; // ** busy, direct connect
        } else {
            assert busy == FALSE;
            assert heFirewalled == FALSE || !iFirewalled;
            boolean hasPushProxies = 
                getPushProxies() != null && getPushProxies().size() > 1;
            if (heFirewalled == TRUE && !hasPushProxies)
                return 2; // *** not busy, no/not many proxies, old push
            else
                return 3; // **** not busy, has proxies or direct connect
        }
    }

    public boolean isFirewalled() {
        // TODO move to QueryReply decorator?
        boolean firewalled;
        try {
            firewalled = getNeedsPush() || networkInstanceUtils.isPrivateAddress(getIP());
        } catch (BadPacketException e) {
            firewalled = true;
        }
        return firewalled && !isReplyToMulticastQuery();
    }

    /**
     * Handles all our GGEP stuff. Caches potential GGEP blocks for efficiency.
     */
    static class GGEPUtil {

        /**
         * The standard GGEP block for a LimeWire QueryReply. Currently has no
         * keys.
         */
        private final byte[] _standardGGEP;

        /** A GGEP block that has the 'Browse Host' extension. */
        private final byte[] _bhGGEP;

        /** A GGEP Block with BH & TLS */
        private final byte[] _bhTLSGGEP;

        /**
         * A GGEP block that has the 'Multicast Source' extension. Useful for
         * Query Replies for a Query from a multicast source.
         */
        private final byte[] _mcGGEP;

        /** A GGEP Block with MC + TLS. */
        private final byte[] _mcTLSGGEP;

        /** A GGEP Block with BH & MC. */
        private final byte[] _bhAndMC;

        /** A GGEP Block with BH, MC & TLS. */
        private final byte[] _bhMCAndTLS;

        /** A TLS GGEP Block only. */
        private final byte[] _tlsGGEP;

        public GGEPUtil() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            _standardGGEP = create(out);
            _bhGGEP = create(out, GGEPKeys.GGEP_HEADER_BROWSE_HOST);
            _mcGGEP = create(out, GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE);
            _mcTLSGGEP = create(out, GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE,
                    GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
            _bhAndMC = create(out, GGEPKeys.GGEP_HEADER_BROWSE_HOST,
                    GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE);
            _bhTLSGGEP = create(out, GGEPKeys.GGEP_HEADER_BROWSE_HOST,
                    GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
            _bhMCAndTLS = create(out, GGEPKeys.GGEP_HEADER_BROWSE_HOST,
                    GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE, GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
            _tlsGGEP = create(out, GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
        }

        private byte[] create(ByteArrayOutputStream out, String... headers) {
            out.reset();
            GGEP combo = new GGEP(true);
            for (String header : headers)
                combo.put(header);
            try {
                combo.write(out);
            } catch (IOException writeError) {
            }
            byte[] data = out.toByteArray();
            assert data != null;
            return data;
        }

        /**
         * @return the appropriate byte[] corresponding to the GGEP block you
         *         desire.
         */
        public byte[] getQRGGEP(boolean supportsBH, boolean isMulticastResponse,
                boolean supportsFWTransfer, boolean supportsTLS, Set<? extends IpPort> proxies,
                SecurityToken securityToken) {
            byte[] retGGEPBlock = _standardGGEP;

            // we have specific field values so we can't use precached ggeps
            if ((proxies != null && !proxies.isEmpty()) || securityToken != null) {
                if (proxies == null)
                    proxies = Collections.emptySet();

                final int MAX_PROXIES = 4;
                GGEP retGGEP = new GGEP(true);

                // write easy extensions if applicable
                if (supportsBH)
                    retGGEP.put(GGEPKeys.GGEP_HEADER_BROWSE_HOST);
                if (isMulticastResponse)
                    retGGEP.put(GGEPKeys.GGEP_HEADER_MULTICAST_RESPONSE);
                if (supportsTLS)
                    retGGEP.put(GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
                if (supportsFWTransfer)
                    retGGEP.put(GGEPKeys.GGEP_HEADER_FW_TRANS, new byte[] { RUDPUtils.VERSION });
                if (securityToken != null) {
                    retGGEP.put(GGEPKeys.GGEP_HEADER_SECURE_OOB, securityToken.getBytes());
                }

                // if a PushProxyInterface is valid, write up to MAX_PROXIES
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int numWritten = 0;
                BitNumbers bn = HTTPHeaderUtils.getTLSIndices(proxies, Math.min(MAX_PROXIES,
                        proxies.size()));
                if (!proxies.isEmpty()) {
                    Iterator<? extends IpPort> iter = proxies.iterator();
                    while (iter.hasNext() && (numWritten < MAX_PROXIES)) {
                        IpPort ppi = iter.next();
                        try {
                            baos
                                    .write(NetworkUtils.getBytes(ppi,
                                            java.nio.ByteOrder.LITTLE_ENDIAN));
                            numWritten++;
                        } catch (IOException ignored) {
                        } // cannot happen on ByteArrayOutputStream
                    }
                }

                try {
                    // add the PushProxies
                    if (numWritten > 0) {
                        retGGEP.put(GGEPKeys.GGEP_HEADER_PUSH_PROXY, baos.toByteArray());
                        // add the TLS push proxies info, if any.
                        if (!bn.isEmpty())
                            retGGEP.put(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS, bn.toByteArray());
                    }
                    // set up return value
                    baos.reset();
                    retGGEP.write(baos);
                    retGGEPBlock = baos.toByteArray();
                } catch (IOException ignored) {
                } // cannot happen on ByteArrayOutputStream
            }
            // else if (supportsBH && supportsFWTransfer &&
            // isMulticastResponse), since supportsFWTransfer is only helpful
            // if we have proxies
            else if (supportsBH && isMulticastResponse && supportsTLS)
                retGGEPBlock = _bhMCAndTLS;
            else if (supportsBH && isMulticastResponse)
                retGGEPBlock = _bhAndMC;
            else if (supportsBH && supportsTLS)
                retGGEPBlock = _bhTLSGGEP;
            else if (supportsBH)
                retGGEPBlock = _bhGGEP;
            else if (isMulticastResponse && supportsTLS)
                retGGEPBlock = _mcTLSGGEP;
            else if (isMulticastResponse)
                retGGEPBlock = _mcGGEP;
            else if (supportsTLS)
                retGGEPBlock = _tlsGGEP;
            else
                retGGEPBlock = _standardGGEP;
            return retGGEPBlock;
        }

        /**
         * @return a <tt>Set</tt> of <tt>IpPortCombo</tt> instances, which can be
         *         empty but is guaranteed not to be <tt>null</tt>, as described
         *         by the GGEP blocks
         * 
         * @param ggeps the array of GGEP extensions that may or may not contain
         *        push proxy data
         */
        public Set<? extends IpPort> getPushProxies(GGEP ggep) {
            Set<IpPort> proxies = null;
            BitNumbers bn = null;

            // First try and get the bits for which PPs support TLS.
            if (ggep.hasValueFor(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS)) {
                try {
                    bn = new BitNumbers(ggep.getBytes(GGEPKeys.GGEP_HEADER_PUSH_PROXY_TLS));
                } catch (BadGGEPPropertyException bad) {
                }
            }

            if (ggep.hasValueFor(GGEPKeys.GGEP_HEADER_PUSH_PROXY)) {
                try {
                    byte[] proxyBytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_PUSH_PROXY);
                    ByteArrayInputStream bais = new ByteArrayInputStream(proxyBytes);
                    int i = 0;
                    while (bais.available() > 0) {
                        byte[] combo = new byte[6];
                        if (bais.read(combo, 0, combo.length) == combo.length) {
                            try {
                                if (proxies == null)
                                    proxies = new IpPortSet();
                                IpPort ipp = NetworkUtils.getIpPort(combo,
                                        java.nio.ByteOrder.LITTLE_ENDIAN);
                                // make it a connectable if the TLS bit is set.
                                if (bn != null && bn.isSet(i))
                                    ipp = new ConnectableImpl(ipp, true);
                                proxies.add(ipp);
                            } catch (InvalidDataException malformedPair) {
                            }
                        }
                        i++;
                    }
                } catch (BadGGEPPropertyException bad) {
                }
            }
            return proxies != null ? proxies : IpPort.EMPTY_SET;
        }
    }

    @Override
    public Class<? extends Message> getHandlerClass() {
        return QueryReply.class;
    }

    public boolean isLocal() {
        return local;
    }

    int getGGEPStart() {
        parseResults();
        return _data.getGGEPStart();
    }

    int getGGEPEnd() {
        parseResults();
        return _data.getGGEPEnd();
    }
    
    @Override
    public GGEP getGGEP() {
        int start = getGGEPStart();
        if(start == -1)
            return null;
        try {
            return new GGEP(_payload, start, null);
        } catch(BadGGEPBlockException e) {
            return null;
        }
    }

    int getQHDOffset() {
        parseResults();
        return _data.getQHDOffset();
    }
}
