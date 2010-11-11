package com.limegroup.gnutella.messages.vendor;

import java.util.Set;

import org.limewire.collection.IntervalSet;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

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

public interface HeadPong extends VendorMessage {

    public static final int BINARY_VERSION = 1;

    public static final int GGEP_VERSION = 2;

    public static final int VERSION = 2;
    
    /** GGEP fields in the ggep format of the pong. */ 
    static final String FEATURES  = "F";
    static final String CODE      = "C";
    static final String VENDOR    = "V";
    static final String QUEUE     = "Q";
    static final String RANGES    = "R";
    static final String RANGES5    = "R5";
    static final String PUSH_LOCS = "P";
    static final String LOCS      = "A";
    static final String TLS_LOCS  = "T";
    
    /** Features within the GGEP Features block. */
    static final byte TLS_CAPABLE = 0x1;
    
    /**
     * instead of using the HTTP codes, use bit values.  The first three 
     * possible values are mutually exclusive though.  DOWNLOADING is
     * possible only if PARTIAL_FILE is set as well.
     */
    static final byte FILE_NOT_FOUND= (byte)0x0;
    static final byte COMPLETE_FILE= (byte)0x1;
    static final byte PARTIAL_FILE = (byte)0x2;
    static final byte FIREWALLED = (byte)0x4;
    static final byte DOWNLOADING = (byte)0x8;
    
    static final byte CODES_MASK=(byte)0xF;
    /** all our slots are full. */
    static final byte BUSY=(byte)0x7F;

    /**
     * 
     * @return whether the alternate location still has the file
     */
    public boolean hasFile();

    /**
     * 
     * @return whether the alternate location has the complete file
     */
    public boolean hasCompleteFile();

    /**
     * 
     * @return the available ranges the alternate location has
     */
    public IntervalSet getRanges();

    /**
     * @return set of <tt>Endpoint</tt> 
     * containing any alternate locations this alternate location returned or
     * and empty set if not available
     */
    public Set<IpPort> getAltLocs();

    /**
     * @return set of <tt>PushEndpoint</tt>
     * containing any firewalled locations this alternate location returned or
     * an empty set if not available.
     */
    public Set<PushEndpoint> getPushLocs();

    /** Whether or not this pong supports TLS. */
    public boolean isTLSCapable();

    /**
     * @return all altlocs carried in the pong as 
     * set of <tt>RemoteFileDesc</tt>
     */
    public Set<RemoteFileDesc> getAllLocsRFD(RemoteFileDesc original, RemoteFileDescFactory remoteFileDescFactory);

    /**
     * 
     * @return the remote vendor as string
     */
    public String getVendor();

    /**
     * 
     * @return whether the remote is firewalled and will need a push
     */
    public boolean isFirewalled();

    public int getQueueStatus();

    public boolean isBusy();

    public boolean isDownloading();

    /**
     * @return true if this pong came from a host that doesn't support routing
     */
    public boolean isRoutingBroken();

    // TODO: hack to let PingRankerTest compile
    public byte[] getPayload();
}

