package com.limegroup.gnutella.messages.vendor;



import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.collection.IntervalSet;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.Decorator;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;

/**
 * a response to an HeadPing.  It is a trimmed down version of the standard HEAD response,
 * since we are trying to keep the sizes of the udp packets small.
 * 
 * This message can also be used for punching firewalls if the ping requests so. 
 * Feature like this can be used to allow firewalled nodes to participate more 
 * in download meshes.
 * 
 * Since headpings will be sent by clients who have started to download a file whose download
 * mesh contains  this host, it needs to contain information that will help those clients whether 
 * this host is a good bet to start an http download from.  Therefore, the following information should
 * be included in the response:
 * 
 *  - available ranges of the file 
 *  - queue status
 *  - some altlocs (if space permits)
 * 
 * the queue status can be an integer representing how many people are waiting in the queue.  If 
 * nobody is waiting in the queue and we have slots available, the integer can be negative.  So if
 * we have 3 people on the queue we'd send the integer 3.  If we have nobody on the queue and 
 * two upload slots available we would send -2.  A value of 0 means all upload slots are taken but 
 * the queue is empty.  This information can be used by the downloaders to better judge chances of
 * successful start of the download. 
 * 
 * NEW GGEP FORMAT:
 *   A GGEP block containing:
 *    F: features (optional)
 *     supported features:
 *      0x1 = TLS_CAPABLE
 *    C: response code (required)
 *    V: vendor id (required if not 404)
 *    Q: queue status (required if not 404)
 *    R: ranges (optional, shouldn't be if complete file)
 *    P: push locations (optional)
 *    A: direct locations (optional)
 *    T: indexes of which direct locations support TLS
 * 
 * OLD BINARY FORMAT:
 *   1 byte - features byte
 *   2 byte - response code
 *   4 bytes - vendor id
 *   1 byte - queue status
 *   n*8 bytes - n intervals (if requested && file partial && fits in packet)
 *   the rest - altlocs (if requested) 
 */
public class HeadPongImpl extends AbstractVendorMessage implements HeadPong {
    
    /** available ranges */
    private IntervalSet _ranges;
    /** the altlocs that were sent, if any */
    private Set<IpPort> _altLocs = Collections.emptySet();
    /** the firewalled altlocs that were sent, if any */
    private Set<PushEndpoint> _pushLocs = Collections.emptySet();
    /** the queue status, can be negative */
    private int _queueStatus;
    /** whether the other host has the file at all */
    private boolean _fileFound,_completeFile;
    /** the remote host */
    private byte [] _vendorId;
    /** whether the other host can receive tcp */
    private boolean _isFirewalled;
    /** whether the other host is currently downloading the file */
    private boolean _isDownloading;
    /** Whether the remote host supports TLS. */
    private boolean _tlsCapable;
    /** True if this came from a routed ping. */
    private boolean _routingBroken;
    
    private final PushEndpointFactory pushEndpointFactory;
    
    /**
     * Creates a message object with data from the network.
     * 
     * This will correctly set the fields of this HeadPong, as opposed
     * to the other constructor.
     */
    HeadPongImpl(byte[] guid, byte ttl, byte hops, int version, byte[] payload, Network network, PushEndpointFactory pushEndpointFactory) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload, network);
        
        // This isn't really used later on -- it's just used deep within the setFieldsFromXXX methods.
        // Maybe this constructor should go away and should be made with all fields passed in.
        this.pushEndpointFactory = pushEndpointFactory;
        
        //we should have some payload
        if (payload==null || payload.length<2)
            throw new BadPacketException("bad payload");
        
        if(version == BINARY_VERSION) {
            setFieldsFromBinary(payload);
        } else if(version >= GGEP_VERSION){
            setFieldsFromGGEP(payload);
        } else {
            throw new BadPacketException("invalid version!");
        }
    }
    
    /**
     * Constructs a message to send in response to the Ping.
     * If the Ping is version 1, this will construct a BINARY FORMAT pong.
     * Otherwise, this will construct a GGEP FORMAT pong.
     * 
     * NOTE: This will NOT set the fields of this class correctly.
     *       This constructor is intended ONLY for sending the reply
     *       through the network.  To access a HeadPong with the
     *       fields set correctly, you can write this to a ByteArrayOutputStream
     *       and reparse the resulting bytes through MessageFactory,
     *       which will construct a HeadPong with the network constructor,
     *       where the fields are correctly set.
     */
    protected HeadPongImpl(GUID guid, int version, byte[] payload) {
        super(F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
        setGUID(guid);
        pushEndpointFactory = null;
    }
    
    @Override
    public Class<HeadPong> getHandlerClass() {
        return HeadPong.class;
    }
    
    /**
     * Sets all local fields based off the original version of the HeadPong,
     * from which the format was not very extensible.
     * 
     * @param payload
     * @throws BadPacketException
     */
    private void setFieldsFromBinary(byte[] payload) throws BadPacketException {
        //the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE, 
        //COMPLETE_FILE, FIREWALLED or DOWNLOADING
        if (payload[1]>CODES_MASK) 
            throw new BadPacketException("invalid payload!");
        
        try {
            DataInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));          
            //read and mask the features
            byte features = (byte) (dais.readByte() & HeadPing.FEATURE_MASK);            
            // older clients echoed the feature mask a ping sent them,
            // which can sometimes include the GGEP_PING feature.
            // these older clients also didn't correctly route pings to
            // their leaves.  newer clients fixed this, and use this fact
            // to recognize when an older push proxy sends them a bogus
            // response.
            _routingBroken = (features & HeadPing.GGEP_PING) == HeadPing.GGEP_PING;
            
            //read the response code
            byte code = dais.readByte(); 
            if(!setFieldsFromCode(code))
                return;
            
            //read the vendor id
            _vendorId = new byte[4];
            dais.readFully(_vendorId);
            
            //read the queue status
            _queueStatus = dais.readByte();
            
            if(!_completeFile && (features & HeadPing.INTERVALS) == HeadPing.INTERVALS)
                _ranges = readRanges(dais);
            
            //parse any included firewalled altlocs
            if ((features & HeadPing.PUSH_ALTLOCS) == HeadPing.PUSH_ALTLOCS) 
                _pushLocs=readPushLocs(dais);           
                
            //parse any included altlocs
            if ((features & HeadPing.ALT_LOCS) == HeadPing.ALT_LOCS) 
                _altLocs= readLocs(dais);
        } catch(IOException oops) {
            throw new BadPacketException(oops);
        }
    }
    
    /**
     * Sets all fields in the pong based on the GGEP format.
     * 
     * @param payload
     * @throws BadPacketException
     */
    private void setFieldsFromGGEP(byte[] payload) throws BadPacketException {
        GGEP ggep;
        try {
            ggep = new GGEP(payload, 0);
        } catch (BadGGEPBlockException e) {
            throw new BadPacketException(e);
        }
        
        byte[] code = getRequiredGGEPField(ggep, CODE);
        if(!setFieldsFromCode(code[0]))
            return;
        
        // No pongs that support GGEP have routing broken.
        _routingBroken = false;
        
        // Otherwise, there's more required.
        _vendorId = getRequiredGGEPField(ggep, VENDOR);
        _queueStatus = getRequiredGGEPField(ggep, QUEUE)[0];
        byte[] features = getOptionalGGEPField(ggep, FEATURES);
        if(features.length > 0) {
            _tlsCapable = (features[0] & TLS_CAPABLE) == TLS_CAPABLE;
        }
        
        try {
            byte[] ranges = getOptionalGGEPField(ggep, RANGES);
            byte [] ranges5 = getOptionalGGEPField(ggep, RANGES5);
            if(ranges.length > 0 || ranges5.length > 0)
                _ranges = parseRanges(ranges, ranges5);
            
            byte[] pushLocs = getOptionalGGEPField(ggep, PUSH_LOCS);
            if(pushLocs.length > 0)
                _pushLocs = parsePushLocs(pushLocs);
            
            byte[] altTLS = getOptionalGGEPField(ggep, TLS_LOCS);
            BitNumbers tls = null;
            if(altTLS.length > 0)
                tls = new BitNumbers(altTLS);
            
            byte[] altLocs = getOptionalGGEPField(ggep, LOCS);
            if(altLocs.length > 0)
                _altLocs = parseAltLocs(altLocs, tls);            
        } catch(IOException iox) {
            throw new BadPacketException(iox);
        }
    }
    
    /**
     * Returns false if code is FILE_NOT_FOUND.
     * Otherwise, returns true and sets _fileFound, and optionally sets
     * _isFirewalled, _completeFile, and _isDownloading depending on 
     * what is set within coe.
     */
    private boolean setFieldsFromCode(byte code) {
        if (code == FILE_NOT_FOUND) 
            return false;
        
        _fileFound=true;
        
        //is the other host firewalled?
        if ((code & FIREWALLED) == FIREWALLED)
            _isFirewalled = true;
        
        //if we have a partial file and the pong carries ranges, parse their list
        if ((code & COMPLETE_FILE) == COMPLETE_FILE) 
            _completeFile=true;
        //also check if the host is downloading the file
        else if ((code & DOWNLOADING) == DOWNLOADING)
            _isDownloading=true;
        
        return true;
    }
    

    /** Returns a required field, throwing a BadPacketException if it doesn't exist. */
    private byte[] getRequiredGGEPField(GGEP ggep, String header) throws BadPacketException {
        try {
            byte[] bytes = ggep.getBytes(header);
            if(bytes.length == 0)
                throw new BadPacketException("no data for header: " + header + "!");
            return bytes;
        } catch(BadGGEPPropertyException bgpe) {
            throw new BadPacketException(bgpe);
        }
    }
    
    /** Returns the bytes of the field in the GGEP if it exists, otherwise an empty array. */
    private byte[] getOptionalGGEPField(GGEP ggep, String header) {
        if(ggep.hasValueFor(header)) {
            try {
                return ggep.getBytes(header);
            } catch(BadGGEPPropertyException ignored) {}
        }
        
        return DataUtils.EMPTY_BYTE_ARRAY;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#hasFile()
     */
    public boolean hasFile() {
        return _fileFound;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#hasCompleteFile()
     */
    public boolean hasCompleteFile() {
        return hasFile() && _completeFile;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getRanges()
     */
    public IntervalSet getRanges() {
        return _ranges;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getAltLocs()
     */
    public Set<IpPort> getAltLocs() {
        return _altLocs;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getPushLocs()
     */
    public Set<PushEndpoint> getPushLocs() {
        return _pushLocs;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#isTLSCapable()
     */
    public boolean isTLSCapable() {
        return _tlsCapable;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getAllLocsRFD(com.limegroup.gnutella.RemoteFileDesc)
     */
    public Set<RemoteFileDesc> getAllLocsRFD(RemoteFileDesc original, RemoteFileDescFactory remoteFileDescFactory){
        Set<RemoteFileDesc> ret = new HashSet<RemoteFileDesc>();
        
        for(IpPort current : _altLocs)
            ret.add(remoteFileDescFactory.createRemoteFileDesc(original, current));
        
        for(PushEndpoint current : _pushLocs)
            ret.add(remoteFileDescFactory.createRemoteFileDesc(original, current));
        
        return ret;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getVendor()
     */
    public String getVendor() {
        if(_vendorId != null)
            return StringUtils.getASCIIString(_vendorId);
        else
            return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#isFirewalled()
     */
    public boolean isFirewalled() {
        return _isFirewalled;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getQueueStatus()
     */
    public int getQueueStatus() {
        return _queueStatus;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#isBusy()
     */
    public boolean isBusy() {
        return _queueStatus >= BUSY;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#isDownloading()
     */
    public boolean isDownloading() {
        return _isDownloading;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#isRoutingBroken()
     */
    public boolean isRoutingBroken() {
        return _routingBroken;
    }
    
    @Override
    public String toString() {
        return "HeadPong: " +
            " isRoutingBroken: "+ isRoutingBroken()+
            ", hasFile: "+hasFile()+
            ", hasCompleteFile: "+hasCompleteFile()+
            ", isDownloading: "+isDownloading()+
            ", isFirewalled: "+isFirewalled()+
            ", queue rank: "+getQueueStatus()+
            ", \nranges: "+getRanges()+
            ", \nalts: "+getAltLocs()+
            ", \npushalts: "+getPushLocs();
    }
    
    //*************************************
    //utility methods
    //**************************************
    
    /**
     * reads available ranges from an inputstream
     */
    private final IntervalSet readRanges(DataInputStream dais) throws IOException {
        int rangeLength=dais.readUnsignedShort();
        byte [] ranges = new byte[rangeLength];
        dais.readFully(ranges);
        return parseRanges(ranges, DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    /** Parses available ranges. */
    private IntervalSet parseRanges(byte[] ranges, byte [] ranges5) throws IOException {
        return IntervalSet.parseBytes(ranges, ranges5);
    }
    
    /**
     * reads firewalled alternate locations from an input stream
     */
    private final Set<PushEndpoint> readPushLocs(DataInputStream dais) 
        throws IOException, BadPacketException {
        int size = dais.readUnsignedShort();
        byte [] altlocs = new byte[size];
        dais.readFully(altlocs);
        return parsePushLocs(altlocs);
    }
    
    /** Parses push alternate locations from a byte[]. */
    private Set<PushEndpoint> parsePushLocs(byte[] altlocs) throws IOException, BadPacketException {
        Set<PushEndpoint> ret = new HashSet<PushEndpoint>();
        ret.addAll(unpackPushEPs(new ByteArrayInputStream(altlocs)));
        return ret;
    }

    /** Unpacks a stream of Push Endpoints. */
    private List<PushEndpoint> unpackPushEPs(InputStream is)
      throws BadPacketException, IOException {
        List<PushEndpoint> ret = new LinkedList<PushEndpoint>();
        DataInputStream dais = new DataInputStream(is);
        while (dais.available() > 0) 
            ret.add(pushEndpointFactory.createFromBytes(dais));
        
        return Collections.unmodifiableList(ret);
    }    
    
    /**
     * reads non-firewalled alternate locations from an input stream
     */
    private final Set<IpPort> readLocs(DataInputStream dais) 
      throws IOException, BadPacketException {
        int size = dais.readUnsignedShort();
        byte [] altlocs = new byte[size];
        dais.readFully(altlocs);
        return parseAltLocs(altlocs, null);
    }
    
    /** Parses alternate locations from a byte[]. */
    private Set<IpPort> parseAltLocs(byte[] altlocs, final BitNumbers tlsIdx) throws IOException, BadPacketException {
        Set<IpPort> ret = new HashSet<IpPort>();
        try {
            if(tlsIdx == null) {
                ret.addAll(NetworkUtils.unpackIps(altlocs));
            } else {
                // Decorate the unpacking of the IPs in order to make
                // some of them TLS-capable.
                ret.addAll(NetworkUtils.unpackIps(altlocs, new Decorator<IpPort, IpPort>() {
                    int i = 0; 
                    
                    public IpPort decorate(IpPort input) {
                        if(tlsIdx.isSet(i))
                            input = new ConnectableImpl(input, true);
                        i++;
                        return input;
                    }
                }));
            }
        } catch(InvalidDataException ide) {
            throw new BadPacketException(ide);
        }
        return ret;
    }
    
    // TODO: hack to let PingRankerTest compile
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongI#getPayload()
     */
    @Override
    public byte[] getPayload() {
        return super.getPayload();
    }
    
}
    
