package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.BitNumbers;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.MultiRRIterator;
import org.limewire.core.settings.UploadSettings;
import org.limewire.io.Connectable;
import org.limewire.io.CountingOutputStream;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.IncompleteFiles;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;

@Singleton
public class HeadPongFactoryImpl implements HeadPongFactory {
    
    private static final Log LOG = LogFactory.getLog(HeadPongFactoryImpl.class);
    
    private final NetworkManager networkManager;
    private final Provider<UploadManager> uploadManager;
    private final FileView gnutellaFileView;
    private final FileView incompleteFileView;
    private final Provider<AltLocManager> altLocManager;
    private final PushEndpointFactory pushEndpointFactory; 

    /** The real packet size. */
    public static final int DEFAULT_PACKET_SIZE = 1380;

    /** The packet size used by this class -- non-final for testing. */
    // TODO: Should either be a parameter in the constructor, or changed by a setter
    private static /*final*/ int PACKET_SIZE = DEFAULT_PACKET_SIZE;

    private final Provider<DownloadManager> downloadManager;
    
    @Inject
    public HeadPongFactoryImpl(NetworkManager networkManager,
            Provider<UploadManager> uploadManager,
            Provider<AltLocManager> altLocManager,
            PushEndpointFactory pushEndpointFactory,
            Provider<DownloadManager> downloadManager,
            @GnutellaFiles FileView gnutellaFileView,
            @IncompleteFiles FileView incompleteFileView) {
        this.networkManager = networkManager;
        this.uploadManager = uploadManager;
        this.altLocManager = altLocManager;
        this.pushEndpointFactory = pushEndpointFactory;
        this.downloadManager = downloadManager;
        this.gnutellaFileView = gnutellaFileView;
        this.incompleteFileView = incompleteFileView;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongFactory#createFromNetwork(byte[], byte, byte, int, byte[])
     */
    public HeadPong createFromNetwork(byte[] guid, byte ttl, byte hops,
            int version, byte[] payload, Network network) throws BadPacketException {
        return new HeadPongImpl(guid, ttl, hops, version, payload, network, pushEndpointFactory);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.messages.vendor.HeadPongFactory#create(com.limegroup.gnutella.messages.vendor.HeadPongRequestor)
     */
    public HeadPong create(HeadPongRequestor ping) {
        return new HeadPongImpl(new GUID(ping.getGUID()), versionFor(ping), derivePayload(ping));
    }

    /** Returns the byte[] of the written GGEP. */
    private byte[] writeGGEP(GGEP ggep) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox);
        }
        return out.toByteArray();
    }

    /** Adds direct locations, if possible. */
    private boolean addLocations(HeadPongRequestor ping, URN urn, OutputStream out,
                                        AtomicReference<BitNumbers> tlsIndexes,
                                        int written, boolean includeSize) {
        //now add any non-firewalled altlocs in case they were requested. 
        if (ping.requestsAltlocs()) {
            AlternateLocationCollection<DirectAltLoc> col = altLocManager.get().getDirect(urn);
            synchronized(col) {
                try {
                    return !writeLocs(out, col.iterator(), tlsIndexes, written, includeSize);
                } catch(IOException impossible) {
                    ErrorService.error(impossible);
                }
            }
        }
        
        return false;
    }

    /** Adds push locations, if possible. */
    private boolean addPushLocations(HeadPongRequestor ping, URN urn, OutputStream out, boolean includeTLS,
                                            int written, boolean includeSize) {
        if(!ping.requestsPushLocs())
            return true;
        
        try {
            boolean FWTOnly = ping.requestsFWTOnlyPushLocs();           
            if (FWTOnly) {
                AlternateLocationCollection<PushAltLoc> push = altLocManager.get().getPushFWT(urn);
                synchronized(push) {
                    return !writePushLocs(out,
                                          push.iterator(),
                                          includeTLS,
                                          written,
                                          includeSize);
                }
            } else {
                AlternateLocationCollection<PushAltLoc> push = altLocManager.get().getPushNoFWT(urn);
                AlternateLocationCollection<PushAltLoc> fwt = altLocManager.get().getPushFWT(urn);
                synchronized(push) {
                    synchronized(fwt) {
                        return !writePushLocs(out,
                                              new MultiRRIterator<PushAltLoc>(push.iterator(),
                                                                              fwt.iterator()),
                                              includeTLS,
                                              written,
                                              includeSize);
                    }
                }
            }
        } catch(IOException impossible) {
            ErrorService.error(impossible);            
            return false;
        }
    }

    /** Calculates the queue status. */
    private byte calculateQueueStatus() {
        int queueSize = uploadManager.get().getNumQueuedUploads();

        if(queueSize >= UploadSettings.UPLOAD_QUEUE_SIZE.getValue()) {
            return HeadPong.BUSY;
        } else if(queueSize > 0) {
            return (byte) Math.min(queueSize, 127); // 127 == HeadPong.BUSY
        } else {
            // Negative queue status means free slots
            queueSize = uploadManager.get().uploadsInProgress() - 
                        UploadSettings.HARD_MAX_UPLOADS.getValue();
            return (byte) Math.max(Math.min(queueSize, 127), -128);
        }
    }

    /** Calculates the code that should be returned, based on the FileDesc. */
    private byte calculateCode(FileDesc fd) {
        byte code = 0;
        if(!networkManager.acceptedIncomingConnection()) {
            code = HeadPong.FIREWALLED;
        }
        
        if(fd instanceof IncompleteFileDesc) {
            code |= HeadPong.PARTIAL_FILE;
            
            if (downloadManager.get().isActivelyDownloading(fd.getSHA1Urn())) {
                code |= HeadPong.DOWNLOADING;
            }
            
        } else {
            code |= HeadPong.COMPLETE_FILE;
        }
        
        return code;
    }

    /** Constructs the payload in GGEP format. */
    private byte[] constructGGEPPayload(HeadPongRequestor ping) {
        GGEP ggep = new GGEP();
        
        URN urn = ping.getUrn();
        FileDesc desc = gnutellaFileView.getFileDesc(urn);
        if(desc == null) {
            desc = incompleteFileView.getFileDesc(urn);
        }
        // Easy case: no file, add code & exit
        if(desc == null) {
            ggep.put(HeadPong.CODE, HeadPong.FILE_NOT_FOUND);
            return writeGGEP(ggep);
        }
        
        // OK, we have the file, now what!
        int size = 1;  // begin with 1 because of GGEP magic
        
        // If we're not firewalled and support TLS,
        // spread word about our TLS status.
        if(networkManager.acceptedIncomingConnection() && 
                networkManager.isIncomingTLSEnabled() ) {
            ggep.put(HeadPong.FEATURES, HeadPong.TLS_CAPABLE);
            size += 4;
        }
        
        byte code = calculateCode(desc);
        ggep.put(HeadPong.CODE, code); size += ggep.getHeaderOverhead(HeadPong.CODE);
        ggep.put(HeadPong.VENDOR, VendorMessage.F_LIME_VENDOR_ID); size += ggep.getHeaderOverhead(HeadPong.VENDOR);
        ggep.put(HeadPong.QUEUE, calculateQueueStatus()); size += ggep.getHeaderOverhead(HeadPong.QUEUE);
        
        // NOTE: All insertion checks assume that the header is going to take up
        //       the maximum amount of bytes possible for a GGEP header + overhead.
        
        if((code & HeadPong.PARTIAL_FILE) == HeadPong.PARTIAL_FILE && ping.requestsRanges()) {
            IntervalSet.ByteIntervals ranges = deriveRanges(desc);
            if(ranges.length() == 0) {
                // If we have no ranges available, change queue status to busy,
                // so that they come back and ask us later, when we may have
                // more ranges available. (but don't increment size, since that
                // was already done above.)
                ggep.put(HeadPong.QUEUE, HeadPong.BUSY);
            } else if(size + ranges.length() + 11 <= PACKET_SIZE) { //5 for "R" and 6 for "R5"
                if (ranges.ints.length > 0) {
                    ggep.put(HeadPong.RANGES, ranges.ints);
                    size += ggep.getHeaderOverhead(HeadPong.RANGES);
                }
                if (ranges.longs.length > 0) {
                    ggep.put(HeadPong.RANGES5, ranges.longs);
                    size += ggep.getHeaderOverhead(HeadPong.RANGES5);
                }
            }
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        addPushLocations(ping, urn, out, true, size+5, false);
        if(out.size() > 0) {
            byte[] pushLocs = out.toByteArray();
            ggep.put(HeadPong.PUSH_LOCS, pushLocs);
            size += ggep.getHeaderOverhead(HeadPong.PUSH_LOCS);
        }
        
        out.reset();
        AtomicReference<BitNumbers> bnRef = new AtomicReference<BitNumbers>();
        addLocations(ping, urn, out, bnRef, size+5, false);
        if(out.size() > 0) {
            byte[] altLocs = out.toByteArray();
            ggep.put(HeadPong.LOCS, altLocs);
            size += ggep.getHeaderOverhead(HeadPong.LOCS);
        }
        
        // If it went over, we screwed up somewhere.
        assert size <= PACKET_SIZE : "size is too big "+size+" vs "+PACKET_SIZE;
        
        // Here we fudge a bit -- possibly going over PACKET_SIZE.
        BitNumbers bn = bnRef.get();
        if(bn != null) {
            byte[] bnBytes = bn.toByteArray();
            if(bnBytes.length > 0) {
                ggep.put(HeadPong.TLS_LOCS, bnBytes);
                size += ggep.getHeaderOverhead(HeadPong.TLS_LOCS);
            }
        }
        
        byte[] output = writeGGEP(ggep);
        assert output.length == size : "expected: " + size + ", was: " + output.length;
        return output;
    }

    /** Constructs the payload in binary format. */
    private byte[] constructBinaryPayload(HeadPongRequestor ping) {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	CountingOutputStream caos = new CountingOutputStream(baos);
    	DataOutputStream daos = new DataOutputStream(caos);
    	byte retCode=0;
    	URN urn = ping.getUrn();
    	FileDesc desc = gnutellaFileView.getFileDesc(urn);
    	if(desc == null) {
    	    desc = incompleteFileView.getFileDesc(urn);
    	}
    	boolean didNotSendAltLocs=false;
    	boolean didNotSendPushAltLocs = false;
    	boolean didNotSendRanges = false;
    	
    	try {
    		byte features = ping.getFeatures();
    		features &= ~HeadPing.GGEP_PING;
    		daos.write(features);
    		if (LOG.isDebugEnabled())
    			LOG.debug("writing features "+features);
    		
    		//if we don't have the file or its too large...
    		if (desc == null || desc.getFileSize() > Integer.MAX_VALUE) {
    			LOG.debug("we do not have the file");
    			daos.write(HeadPong.FILE_NOT_FOUND);
    			return baos.toByteArray();
    		}
    
            retCode = calculateCode(desc);
    		daos.write(retCode);
    		
    		if(LOG.isDebugEnabled())
    			LOG.debug("our return code is "+retCode);
    		
    		//write the vendor id
    		daos.write(VendorMessage.F_LIME_VENDOR_ID);
    
    		//write out the return code and the queue status
    		daos.writeByte(calculateQueueStatus());
    		
    		//if we sent partial file and the remote asked for ranges, send them 
    		if ((retCode & HeadPong.PARTIAL_FILE) == HeadPong.PARTIAL_FILE && ping.requestsRanges()) 
    			didNotSendRanges=!writeRanges(caos,desc);
            
            didNotSendPushAltLocs = addPushLocations(ping, urn, caos, false, caos.getAmountWritten(), true);
            didNotSendAltLocs = addLocations(ping, urn, caos, null, caos.getAmountWritten(), true);
    		
    	} catch(IOException impossible) {
    		ErrorService.error(impossible);
    	}
    	
    	//done!
    	byte []ret = baos.toByteArray();
    	
    	//if we did not add ranges or altlocs due to constraints, 
    	//update the flags now.
    	
    	if (didNotSendRanges){
    		LOG.debug("not sending ranges");
    		ret[0] = (byte) (ret[0] & ~HeadPing.INTERVALS);
    	}
    	if (didNotSendAltLocs){
    		LOG.debug("not sending altlocs");
    		ret[0] = (byte) (ret[0] & ~HeadPing.ALT_LOCS);
    	}
    	if (didNotSendPushAltLocs){
    		LOG.debug("not sending push altlocs");
    		ret[0] = (byte) (ret[0] & ~HeadPing.PUSH_ALTLOCS);
    	}
    	return ret;
    }

    /**
     * Constructs a byte[] that contains the payload of the HeadPong.
     * 
     * @param ping the original UDP head ping to respond to
     */
    private byte [] derivePayload(HeadPongRequestor ping)  {
        if(!ping.isPongGGEPCapable()) {
            return constructBinaryPayload(ping);
        } else {
            return constructGGEPPayload(ping);
        }
    }

    /** Determines the version that will be used based on the requestor. */
    private int versionFor(HeadPongRequestor ping) {
        if(!ping.isPongGGEPCapable())
            return HeadPong.BINARY_VERSION;
        else
            return HeadPong.VERSION;
    }

    /** Returns the byte[] of the ranges. */
    private final IntervalSet.ByteIntervals deriveRanges(FileDesc desc) {
        return ((IncompleteFileDesc)desc).getRangesAsByte();
    }

    /**
     * Writes out alternate locations in binary form to the output stream.
     * This will only write as many locations as possible that can
     * fit in the PACKET_SIZE.  If tlsIndexes is non-null, the reference
     * will be set to a BitNumbers whose size is the number of locations
     * that are attempted to write, with the corresponding bits set
     * if the location at the index is tls capable.
     * If includeSize is true, the written data will be prepended by the length
     * of the amount written.
     */
    private final boolean writeLocs(OutputStream out,
                                           Iterator<DirectAltLoc> altlocs,
                                           AtomicReference<BitNumbers> tlsIndexes,
                                           int written,
                                           boolean includeSize) throws IOException {
    	
    	//do we have any altlocs?
    	if (!altlocs.hasNext())
    		return false;
        
        //how many can we fit in the packet?
        int toSend = (PACKET_SIZE - (written + (includeSize ? 2 : 0)) ) / 6;
        if (toSend == 0)
            return false;
        
    	if (LOG.isDebugEnabled())
    		LOG.debug("trying to add up to "+ toSend +" locs to pong");
        
        BitNumbers bn = null;
        if(tlsIndexes != null) {
            bn = new BitNumbers(toSend);
            tlsIndexes.set(bn);
        }
        
        // optimization: do not duplicate byte[] if not needed
    	ByteArrayOutputStream baos = includeSize ? new ByteArrayOutputStream() : (ByteArrayOutputStream)out;
        int sent = 0;
        long now = System.currentTimeMillis();
    	while(altlocs.hasNext() && sent < toSend) {
            DirectAltLoc loc = altlocs.next();
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                loc.send(now,AlternateLocation.MESH_PING);
                baos.write(loc.getHost().getInetAddress().getAddress());
                ByteUtils.short2leb((short)loc.getHost().getPort(),baos);
                IpPort host = loc.getHost();
                if(bn != null && host instanceof Connectable && ((Connectable)host).isTLSCapable())
                    bn.set(sent);
                sent++;
            } else if (!loc.canBeSentAny())
                altlocs.remove();
        }
    	
    	LOG.debug("adding altlocs");
        if(includeSize) {
    		ByteUtils.short2beb((short)baos.size(),out);
    		baos.writeTo(out);
        }
    	return true;
    		
    }

    /**
     * Writes out PushEndpoints in binary form to the output stream.
     * This will only write as many push locations as possible that can
     * fit in the PACKET_SIZE.  If includeTLS is true, this will include
     * a byte that describes which push proxies of each PushEndpoint
     * are capable of receiving TLS connections.
     * If includeSize is true, the written data will be prepended by the length
     * of the amount written.
     */
    private final boolean writePushLocs(OutputStream out,
                                               Iterator<PushAltLoc> pushlocs,
                                               boolean includeTLS,
                                               int written, 
                                               boolean includeSize) throws IOException {
    
        if (!pushlocs.hasNext())
            return false;
        
    
        //push altlocs are bigger than normal altlocs, however we 
        //don't know by how much.  The size can be between
        //23 and 48 bytes.  We assume its 47 if includeTLS is false, 48 otherwise.
        int available = (PACKET_SIZE - (written + (includeSize ? 2 : 0))) / (includeTLS ? 48 : 47);
        
        // if we don't have any space left, we can't send any pushlocs
        if (available == 0)
            return false;
        
    	if (LOG.isDebugEnabled())
    		LOG.debug("trying to add up to "+available+ " push locs to pong");
    	
        long now = System.currentTimeMillis();
        // Optimization: don't duplicate the written byte[] if not needed
    	ByteArrayOutputStream baos = includeSize ? new ByteArrayOutputStream() : (ByteArrayOutputStream)out;
        while (pushlocs.hasNext() && available > 0) {
            PushAltLoc loc = pushlocs.next();
    
            if (loc.getPushAddress().getProxies().isEmpty()) {
                pushlocs.remove();
                continue;
            }
            
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                baos.write(loc.getPushAddress().toBytes(includeTLS));
                available--;
                loc.send(now,AlternateLocation.MESH_PING);
            } else if (!loc.canBeSentAny())
                pushlocs.remove();
        }
    	
    	if (baos.size() == 0) {
    		//altlocs will not fit or none available - say we didn't send them
    		LOG.debug("did not send any push locs");
    		return false;
    	} else { 
    		LOG.debug("adding push altlocs");
            if(includeSize) {
                ByteUtils.short2beb((short)baos.size(),out);
    			baos.writeTo(out);
            } // else it's already written to out
    		return true;
    	}
    }

    /**
     * @param daos the output stream to write the ranges to
     * @return if they were written or not.
     */
    private final boolean writeRanges(CountingOutputStream caos, FileDesc desc) throws IOException{
    	DataOutputStream daos = new DataOutputStream(caos);
    	LOG.debug("adding ranges to pong");
    	IntervalSet.ByteIntervals ranges = deriveRanges(desc);
    
        // this is a non-ggep pong so we should not be serving long files.
        assert  ranges.longs.length == 0 : "long ranges in legacy pong";
        
    	//write the ranges only if they will fit in the packet
    	if (caos.getAmountWritten()+2 + ranges.ints.length <= PACKET_SIZE) {
    		LOG.debug("added ranges");
    		daos.writeShort((short)ranges.ints.length);
    		caos.write(ranges.ints);
    		return true;
    	} 
    	else { //the ranges will not fit - say we didn't send them.
    		LOG.debug("ranges will not fit :(");
    		return false;
    	}
    }

}
