package com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Cancellable;
import org.limewire.collection.DualIterator;
import org.limewire.collection.MultiIterable;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.util.Visitor;

import com.google.inject.Inject;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;

public class PingRanker extends AbstractSourceRanker implements MessageListener, Cancellable {

    private static final Log LOG = LogFactory.getLog(PingRanker.class);
    private static final Comparator<RemoteFileDescContext> ALT_DEPRIORITIZER =
        new RFDAltDeprioritizer();
    
    /**
     * New hosts (as RFDs) that we've learned about.
     */
    private Set<RemoteFileDescContext> newHosts;
    
    /**
     * Mapping IpPort -> RFD to which we have sent pings.
     * Whenever we send pings to push proxies, each proxy points to the same
     * RFD.  Used to check whether we receive a pong from someone we have sent
     * a ping to.
     */
    private TreeMap<IpPort, RemoteFileDescContext> pingedHosts;
    
    /**
     * A set containing the unique remote file locations that we have pinged.  It
     * differs from pingedHosts because it contains only RemoteFileDesc objects 
     */
    private Set<RemoteFileDescContext> testedLocations;
    
    /**
     * RFDs that have responded to our pings.
     */
    private TreeSet<RemoteFileDescContext> verifiedHosts;
    
    /**
     * The urn to use to create pings.
     */
    private URN sha1;
    
    /**
     * The guid to use for my headPings.
     */
    private GUID myGUID;
    
    /**
     * Whether the ranker has been stopped.
     */
    private boolean running;
    
    /**
     * The last time we sent a bunch of hosts for pinging.
     */
    private long lastPingTime;
    
    private final NetworkManager networkManager;
    private final UDPPinger udpPinger;

    private final MessageRouter messageRouter;
    private final RemoteFileDescFactory remoteFileDescFactory;
    
    @Inject
    PingRanker(NetworkManager networkManager, UDPPinger udpPinger,
            MessageRouter messageRouter,
            RemoteFileDescFactory remoteFileDescFactory,
            Comparator<RemoteFileDescContext> rfdComparator) {
        this.networkManager = networkManager; 
        this.udpPinger = udpPinger;
        this.messageRouter = messageRouter;
        this.remoteFileDescFactory = remoteFileDescFactory;
        pingedHosts = new TreeMap<IpPort, RemoteFileDescContext>(IpPort.COMPARATOR);
        testedLocations = new HashSet<RemoteFileDescContext>();
        newHosts = new HashSet<RemoteFileDescContext>();
        verifiedHosts = new TreeSet<RemoteFileDescContext>(rfdComparator);
    }
    
    @Override
    public synchronized boolean addToPool(Collection<? extends RemoteFileDescContext> c)  {
        List<? extends RemoteFileDescContext> l;
        if (c instanceof List)
            l = (List<? extends RemoteFileDescContext>)c;
        else
            l = new ArrayList<RemoteFileDescContext>(c);
        Collections.sort(l, ALT_DEPRIORITIZER);
        return addInternal(l);
    }
    
    /**
     * Adds the collection of hosts to to the internal structures.
     */
    private boolean addInternal(Collection<? extends RemoteFileDescContext> c) {
        boolean ret = false;
        for(RemoteFileDescContext rfd : c) { 
            if (addInternal(rfd))
                ret = true;
        }
        
        pingNewHosts();
        return ret;
    }
    
    @Override
    public synchronized boolean addToPool(RemoteFileDescContext host){
        boolean ret = addInternal(host);
        pingNewHosts();
        return ret;
    }
    
    private boolean addInternal(RemoteFileDescContext host) {
        // initialize the sha1 if we don't have one
        if (sha1 == null) {
            if( host.getSHA1Urn() != null)
                sha1 = host.getSHA1Urn();
            else    //  BUGFIX:  We can't discard sources w/out a SHA1 when we dont' have  
                    //  a SHA1 for the download, or else it won't be possible to download a
                    //  file from a query hit without a SHA1, if we can received UDP pings
                return testedLocations.add(host); // we can't do anything yet
        }
         
        // do not allow duplicate hosts 
        if (running && knowsAboutHost(host))
                return false;
        
        if(LOG.isDebugEnabled())
            LOG.debug("adding new host "+host+" "+host.getAddress());
        
        boolean ret = false;
        
        // don't bother ranking multicasts
        if (host.isReplyToMulticast())
            ret = verifiedHosts.add(host);
        else 
            ret = newHosts.add(host); // rank
        
        // make sure that if we were stopped, we return true
        ret = ret | !running;
        
        // initialize the guid if we don't have one
        if (myGUID == null && getMeshHandler() != null) {
            myGUID = new GUID();
            messageRouter.registerMessageListener(myGUID.bytes(),this);
        }
        
        return ret;
    }
    
    private boolean knowsAboutHost(RemoteFileDescContext host) {
        return newHosts.contains(host) || 
            verifiedHosts.contains(host) || 
            testedLocations.contains(host);
    }
    
    @Override
    public synchronized RemoteFileDescContext getBest() throws NoSuchElementException {
        if (!hasMore())
            return null;
        RemoteFileDescContext ret;
        
        // try a verified host
        if (!verifiedHosts.isEmpty()) {
            LOG.debug("getting a verified host");
            ret = verifiedHosts.first();
            verifiedHosts.remove(ret);
        } else {
            LOG.debug("getting a non-verified host");
            // use the legacy ranking logic to select a non-verified host
            Iterator<RemoteFileDescContext> dual =
                new DualIterator<RemoteFileDescContext>(testedLocations.iterator(),newHosts.iterator());
            ret = LegacyRanker.getBest(dual, getRfdVisitor());
            if(ret == null) {
                return null; // no valid host.
            }
            newHosts.remove(ret);
            testedLocations.remove(ret);
            Address address = ret.getAddress();
            if (address instanceof PushEndpoint) {
                for(IpPort ipp : ((PushEndpoint)address).getProxies())
                    pingedHosts.remove(ipp);
            } else {
                pingedHosts.remove(ret.getAddress());
            }
        }
        
        pingNewHosts();
        
        if (LOG.isDebugEnabled()) {
            int hosts = verifiedHosts.size() + newHosts.size() +
                testedLocations.size() + 1;
            LOG.debug(hosts + " hosts, the best is " + ret.getAddress() +
                    ", multicast " + ret.isReplyToMulticast() +
                    ", queue length " + ret.getQueueStatus() +
                    ", round-trip time " + ret.getRoundTripTime() +
                    ", firewalled " + isFirewalled(ret) +
                    ", partial source " + ret.isPartialSource());
        }
        return ret;
    }
    
    /**
     * Pings a bunch of hosts if necessary.
     */
    private void pingNewHosts() {
        // if we have reached our desired # of altlocs, don't ping
        if (isCancelled())
            return;
        
        // if we don't have anybody to ping, don't ping
        if (!hasUsableHosts())
            return;
        
        // if we haven't found a single RFD with URN, don't ping anybody
        if (sha1 == null)
            return;
        
        // if its not time to ping yet, don't ping 
        // use the same interval as workers for now
        long now = System.currentTimeMillis();
        if (now - lastPingTime < DownloadSettings.WORKER_INTERVAL.getValue())
            return;
        
        // create a ping for the non-firewalled hosts
        HeadPing ping = new HeadPing(myGUID,sha1,getPingFlags());
        
        // prepare a batch of hosts to ping
        int batch = DownloadSettings.PING_BATCH.getValue();
        List<IpPort> toSend = new ArrayList<IpPort>(batch);
        int sent = 0;
        for (Iterator<RemoteFileDescContext> iter = newHosts.iterator(); iter.hasNext() && sent < batch;) {
            RemoteFileDescContext rfdContext = iter.next();
            RemoteFileDesc rfd = rfdContext.getRemoteFileDesc();
            if (rfdContext.isBusy(now))
                continue;
            iter.remove();
            
            Address address = rfd.getAddress();
            if (address instanceof PushEndpoint) {
                PushEndpoint pushEndpoint = (PushEndpoint)address;
                if (!pushEndpoint.getProxies().isEmpty() && rfdContext.getSHA1Urn() != null)
                    pingProxies(rfdContext, pushEndpoint);
            } else {
                IpPort ipPort = (IpPort)address;
                pingedHosts.put(ipPort,rfdContext);
                toSend.add(ipPort);
            }
            testedLocations.add(rfdContext);
            rfdContext.recordPingTime(now);
            sent++;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("\nverified hosts " +verifiedHosts.size()+
                    "\npingedHosts "+pingedHosts.values().size()+
                    "\nnewHosts "+newHosts.size()+
                    "\npinging hosts: "+sent);
        }
        
        udpPinger.rank(toSend,null,this,ping);
        lastPingTime = now;
    }
    
    /**
     * Schedules a push ping to each proxy of the given host.
     */
    private void pingProxies(RemoteFileDescContext rfdContext, PushEndpoint pushEndpoint) {
        if (networkManager.acceptedIncomingConnection() || 
                (networkManager.canDoFWT() && pushEndpoint.getFWTVersion() > 0)) {
            HeadPing pushPing = 
                new HeadPing(myGUID,rfdContext.getSHA1Urn(),
                        new GUID(pushEndpoint.getClientGUID()),getPingFlags());
            
            for(IpPort ipp : pushEndpoint.getProxies()) 
                pingedHosts.put(ipp, rfdContext);
            
            if (LOG.isDebugEnabled())
                LOG.debug("pinging push location "+ pushEndpoint);
            
            udpPinger.rank(pushEndpoint.getProxies(),null,this,pushPing);
        }
        
    }
    
    /**
     * @return the appropriate ping flags based on current conditions
     */
    private int getPingFlags() {
        int flags = HeadPing.INTERVALS | HeadPing.ALT_LOCS;
        if (networkManager.acceptedIncomingConnection() ||
                networkManager.canDoFWT())
            flags |= HeadPing.PUSH_ALTLOCS;
        return flags;
    }
    
    @Override
    public synchronized boolean hasMore() {
        return !(verifiedHosts.isEmpty() && newHosts.isEmpty() && testedLocations.isEmpty());
    }
    
    /**
     * Informs the Ranker that a host has replied with a HeadPong.
     */
    public void processMessage(Message m, ReplyHandler handler) {
        
        MeshHandler mesh;
        RemoteFileDescContext rfd;
        Collection<RemoteFileDesc> alts = null;
        // this -> meshHandler NOT ok
        synchronized(this) {
            if (!running)
                return;
            
            if (! (m instanceof HeadPong))
                return;
            
            HeadPong pong = (HeadPong)m;
            
            // update cache with push proxies of headpong since they are
            // brand new
            for (PushEndpoint pushEndpoint : pong.getPushLocs()) {
                pushEndpoint.updateProxies(true);
            }
            
            if (!pingedHosts.containsKey(handler)) 
                return;
            
            rfd = pingedHosts.remove(handler);
            testedLocations.remove(rfd);
            rfd.recordPongTime(System.currentTimeMillis());
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("received a pong " + pong + " from " + handler +
                        " for rfd " + rfd + " with address " +
                        rfd.getAddress() + " and round-trip time " +
                        rfd.getRoundTripTime());
            }
            
            // older push proxies do not route but respond directly, we want to get responses
            // from other push proxies
            if (!pong.hasFile() && pong.isRoutingBroken() && isFirewalled(rfd)) {
                return;
            }
            
            // if the pong is firewalled, remove the other proxies from the 
            // pinged set
            if (pong.isFirewalled() && rfd.getAddress() instanceof PushEndpoint) {
                for(IpPort ipp : ((PushEndpoint)rfd.getAddress()).getProxies())
                    pingedHosts.remove(ipp);
            }
            
            mesh = getMeshHandler();
            if (pong.hasFile()) {
                //update the rfd with information from the pong
                updateContext(rfd, pong);
                
                // if the remote host is busy, re-add him for later ranking
                if (rfd.isBusy())
                    newHosts.add(rfd);
                else
                    verifiedHosts.add(rfd);
                
                alts = pong.getAllLocsRFD(rfd.getRemoteFileDesc(), remoteFileDescFactory);
            }
        }
        
        // if the pong didn't have the file, drop it
        // otherwise add any altlocs the pong had to our known hosts
        if (alts == null)  {
            mesh.informMesh(rfd.getRemoteFileDesc(), false);
        } else {
            mesh.addPossibleSources(alts);
        }
    }

    
    public static void updateContext(RemoteFileDescContext rfdContext, HeadPong headPong) {
        // if the rfd claims its busy, ping it again in a minute
        // (we're obviously using HeadPings, so its cheap to ping it sooner 
        // rather than later)
        if (headPong.isBusy()) {
            rfdContext.setRetryAfter(DownloadWorker.RETRY_AFTER_NONE_ACTIVE);
        }
        rfdContext.setQueueStatus(headPong.getQueueStatus());
        rfdContext.setAvailableRanges(headPong.getRanges());
    }

    public synchronized void registered(byte[] guid) {
        if (LOG.isDebugEnabled())
            LOG.debug("ranker registered with guid "+(new GUID(guid)).toHexString());
        running = true;
    }

    public synchronized void unregistered(byte[] guid) {
        if (LOG.isDebugEnabled())
            LOG.debug("ranker unregistered with guid "+(new GUID(guid)).toHexString());
    
        running = false;
        newHosts.addAll(verifiedHosts);
        newHosts.addAll(testedLocations);
        verifiedHosts.clear();
        pingedHosts.clear();
        testedLocations.clear();
        lastPingTime = 0;
    }
    
    public synchronized boolean isCancelled(){
        return !running || verifiedHosts.size() >= DownloadSettings.MAX_VERIFIED_HOSTS.getValue();
    }
    
    @Override
    protected synchronized void clearState(){
        if (myGUID != null) {
            messageRouter.unregisterMessageListener(myGUID.bytes(),this);
            myGUID = null;
        }
    }
    
    @Override
    protected synchronized boolean visitSources(Visitor<RemoteFileDescContext> contextVisitor) {
        for(RemoteFileDescContext context : new MultiIterable<RemoteFileDescContext>(verifiedHosts, newHosts, testedLocations)) {
            if(!contextVisitor.visit(context)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public synchronized Collection<RemoteFileDescContext> getShareableHosts(){
        List<RemoteFileDescContext>  ret = new ArrayList<RemoteFileDescContext> (verifiedHosts.size()+newHosts.size()+testedLocations.size());
        ret.addAll(verifiedHosts);
        ret.addAll(newHosts);
        ret.addAll(testedLocations);
        return ret;
    }
    
    private static boolean isFirewalled(RemoteFileDescContext rfdContext) {
        return rfdContext.getAddress() instanceof PushEndpoint;
    }
        
    /**
     * A ranker that deprioritizes RFDs from altlocs, used to make sure
     * we ping the hosts that actually returned results first.
     */
    private static final class RFDAltDeprioritizer implements Comparator<RemoteFileDescContext>{
        public int compare(RemoteFileDescContext rfd1, RemoteFileDescContext rfd2) {
            if (rfd1.isFromAlternateLocation() != rfd2.isFromAlternateLocation()) {
                if (rfd1.isFromAlternateLocation())
                    return 1;
                else
                    return -1;
            }
            return 0;
        }
    }
}
