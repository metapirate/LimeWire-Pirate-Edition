package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.BucketQueue;
import org.limewire.collection.Cancellable;
import org.limewire.collection.FixedSizeSortedList;
import org.limewire.collection.IntSet;
import org.limewire.collection.ListPartitioner;
import org.limewire.collection.RandomAccessMap;
import org.limewire.collection.RandomOrderHashMap;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Service;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.bootstrap.Bootstrapper;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;

/**
 * The host catcher collects the addresses of Gnutella and DHT hosts from ping
 * replies and bootstrap servers. Collected addresses are stored in a file
 * between sessions. The servent may attempt to connect to these addresses as
 * necessary to maintain full connectivity. Hosts that are known to be
 * ultrapeers are preferred when caching and returning addresses. 
 */
@EagerSingleton
public class HostCatcher implements Service, Bootstrapper.Listener {
    
    private static final Log LOG = LogFactory.getLog(HostCatcher.class);

    /**
     * The number of good addresses to store (definitely ultrapeers).
     */
    static final int GOOD_SIZE = 1000;

    /**
     * The number of normal addresses to store (maybe not ultrapeers). This
     * is also the number of addresses (good or normal) that will be saved
     * between sessions. Addresses read from disk are given normal priority.
     */
    static final int NORMAL_SIZE = 400;

    /**
     * Constant for identifying good priority hosts (definitely ultrapeers).
     */
    private static final int GOOD_PRIORITY = 1;

    /**
     * Constant for identifying normal priority hosts (maybe not ultrapeers).
     */
    private static final int NORMAL_PRIORITY = 0;

    /**
     * Netmask for filtering addresses by their class C networks.
     */
    private static final int PONG_MASK = 0xFFFFFF00;

    /**
     * Delete the host file if it's older than this in milliseconds.
     */
    static final long STALE_HOST_FILE = 4 * 7 * 24 * 60 * 60 * 1000L; // 4 weeks

    /**
     * Comparator for returning DHT hosts - return active hosts first, then
     * passive, then inactive.
     */
    private static final Comparator<ExtendedEndpoint> DHT_COMPARATOR = 
        new Comparator<ExtendedEndpoint>() {
        public int compare(ExtendedEndpoint e1, ExtendedEndpoint e2) {
            DHTMode mode1 = e1.getDHTMode();
            DHTMode mode2 = e2.getDHTMode();
            // FIXME: what about PASSIVE_LEAF?
            if((mode1.equals(DHTMode.ACTIVE) && !mode2.equals(DHTMode.ACTIVE)) ||
                    (mode1.equals(DHTMode.PASSIVE) && mode2.equals(DHTMode.INACTIVE))) {
                return -1;
            } else if((mode2.equals(DHTMode.ACTIVE) && !mode1.equals(DHTMode.ACTIVE)) ||
                    (mode2.equals(DHTMode.PASSIVE) && mode1.equals(DHTMode.INACTIVE))) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    /**
     * The list of hosts to try. Addresses that are known to be ultrapeers are
     * given priority. Within each priority level, recent hosts are prioritized
     * over older ones. Our representation consists of a set and a queue, both
     * bounded in size. The set lets us quickly check if there are duplicates,
     * while the queue provides ordering. The set is actually a map that points
     * to itself so we can retrieve an endpoint using its IP and port.
     *
     * INVARIANT: queue and set contain exactly the same elements
     * LOCKING: this
     */
    private final BucketQueue<ExtendedEndpoint> ENDPOINT_QUEUE = 
        new BucketQueue<ExtendedEndpoint>(new int[] {NORMAL_SIZE, GOOD_SIZE});
    private final Map<ExtendedEndpoint, ExtendedEndpoint> ENDPOINT_SET =
        new HashMap<ExtendedEndpoint, ExtendedEndpoint>();

    /**
     * Hosts advertising free ultrapeer connection slots.
     */
    private final RandomAccessMap<ExtendedEndpoint, ExtendedEndpoint>
    FREE_ULTRAPEER_SLOTS_SET = 
        new RandomOrderHashMap<ExtendedEndpoint, ExtendedEndpoint>(200);

    /**
     * Hosts advertising free leaf connection slots.
     */
    private final RandomAccessMap<ExtendedEndpoint, ExtendedEndpoint>
    FREE_LEAF_SLOTS_SET = 
        new RandomOrderHashMap<ExtendedEndpoint, ExtendedEndpoint>(200);

    /**
     * Map of locales to sets of hosts with those locales.
     */
    private final Map<String, Set<ExtendedEndpoint>> LOCALE_SET_MAP =
        new HashMap<String, Set<ExtendedEndpoint>>();

    /**
     * Number of hosts to keep in each locale set.
     */
    private static final int LOCALE_SET_SIZE = 100;

    /**
     * Hosts that should be saved in the host file for future sessions. The
     * hosts are ordered by priority using three criteria:
     * 1. Whether the most recent connection attempt succeeded or failed,
     * 2. Whether the host's locale matches our own (if locale preferencing
     * is enabled),
     * 3. The host's average daily uptime (high-uptime hosts are more likely
     * to be reachable in future sessions, and even if they don't have any
     * free connection slots we can learn other addresses from them).
     *
     * INVARIANT: queue and set contain exactly the same elements
     * LOCKING: this
     */
    private final FixedSizeSortedList<ExtendedEndpoint> permanentHosts =
        new FixedSizeSortedList<ExtendedEndpoint>(
                ExtendedEndpoint.priorityComparator(), NORMAL_SIZE);
    private final Set<ExtendedEndpoint> permanentHostsSet =
        new HashSet<ExtendedEndpoint>();

    /**
     * Whether the set of permanent hosts has changed since we last saved it.
     */
    private boolean dirty = false;

    /**
     * Hosts that were loaded from the host file, ordered using the same
     * criteria as permanentHosts.
     * 
     * LOCKING: this
     */
    private final List<ExtendedEndpoint> restoredHosts =
        new FixedSizeSortedList<ExtendedEndpoint>(
                ExtendedEndpoint.priorityComparator(), NORMAL_SIZE);

    /** 
     * Partition view of the list of restored hosts.
     * FIXME: the list is sorted, why not just pop the last element?
     */
    private final ListPartitioner<ExtendedEndpoint> uptimePartitions = 
        new ListPartitioner<ExtendedEndpoint>(restoredHosts, 3);

    /**
     * Hosts to which we could not create a TCP connection, and which should
     * therefore not be tried again. Fixed size, which is package accessible
     * for testing.
     * 
     * LOCKING: this
     */
    private final Set<Endpoint> EXPIRED_HOSTS = new HashSet<Endpoint>();
    protected static final int EXPIRED_HOSTS_SIZE = 500;

    /**
     * Hosts that accepted a TCP connection but not a Gnutella connection, and
     * which are therefore "on probation". Fixed size, which is package
     * accessible for testing.
     * 
     * LOCKING: this
     */    
    private final Set<Endpoint> PROBATION_HOSTS = new HashSet<Endpoint>();
    protected static final int PROBATION_HOSTS_SIZE = 500;

    /**
     * How long (in milliseconds) to wait before first recovering hosts on
     * probation. Non-final for testing.
     */
    private static long PROBATION_RECOVERY_WAIT_TIME = 60 * 1000;

    /**
     * How long (in milliseconds) to wait between periodically recovering
     * hosts on probation. Non-final for testing.
     */
    private static long PROBATION_RECOVERY_TIME = 60 * 1000;

    /**
     * All EndpointObservers waiting on getting an Endpoint.
     */
    private final List<EndpointObserver> _catchersWaiting =
        new LinkedList<EndpointObserver>();

    /**
     * How long (in milliseconds) to send pings after the host catcher
     * is initialized.
     */
    private static final long PONG_RANKING_EXPIRE_TIME = 20 * 1000;

    /**
     * The time at which we should stop sending pings.
     */
    private volatile long lastAllowedPongRankTime = 0;

    /**
     * Stop sending pings if we have this many connections.
     */
    private static final int MAX_CONNECTIONS = 5;

    /**
     * Random number generator for choosing a host at random when no host with
     * a given locale is available.
     */
    private final Random RND = new Random();

    /**
     * Keep references to background threads so we can shut them down later.
     */
    private ScheduledFuture probationFuture;
    private ScheduledFuture bootstrapperFuture;
    private ScheduledFuture clearPingedHostsFuture;

    private final ScheduledExecutorService backgroundExecutor;
    private final ConnectionServices connectionServices;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<UDPService> udpService;
    private final Provider<DHTManager> dhtManager;
    private final Provider<QueryUnicaster> queryUnicaster;
    private final Provider<IPFilter> ipFilter;
    private final UniqueHostPinger uniqueHostPinger;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final Bootstrapper bootstrapper;
    
    @Inject
    protected HostCatcher(
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            ConnectionServices connectionServices,
            Provider<ConnectionManager> connectionManager,
            Provider<UDPService> udpService,
            Provider<DHTManager> dhtManager,
            Provider<QueryUnicaster> queryUnicaster,
            Provider<IPFilter> ipFilter,
            UniqueHostPinger uniqueHostPinger,
            NetworkInstanceUtils networkInstanceUtils,
            Bootstrapper bootstrapper) {
        this.backgroundExecutor = backgroundExecutor;
        this.connectionServices = connectionServices;
        this.connectionManager = connectionManager;
        this.udpService = udpService;
        this.dhtManager = dhtManager;
        this.queryUnicaster = queryUnicaster;
        this.ipFilter = ipFilter;
        this.uniqueHostPinger = uniqueHostPinger;
        this.networkInstanceUtils = networkInstanceUtils;
        this.bootstrapper = bootstrapper;
    }
    
    /**
     * Schedules background threads.
     */
    @Override
    public void start() {
        Runnable probationRestorer = new Runnable() {
            public void run() {
                // Restore probated hosts
                List<Endpoint> toAdd;
                synchronized(HostCatcher.this) {
                    if(LOG.isTraceEnabled()) {
                        LOG.trace("Restoring " + PROBATION_HOSTS.size() +
                        " probated hosts");
                    }
                    toAdd = new ArrayList<Endpoint>(PROBATION_HOSTS);
                    PROBATION_HOSTS.clear();
                }
                for(Endpoint e : toAdd)
                    add(e, false);
                // Take this opportunity to do some logging
                if(LOG.isTraceEnabled()) {
                    LOG.trace(ENDPOINT_SET.size() + " ordinary, " +
                            FREE_ULTRAPEER_SLOTS_SET.size() + " UP slots, " +
                            FREE_LEAF_SLOTS_SET.size() + " leaf slots, " +
                            LOCALE_SET_MAP.size() + " locales, " +
                            permanentHostsSet.size() + " permanent, " +
                            restoredHosts.size() + " restored, " +
                            EXPIRED_HOSTS.size() + " expired, " +
                            PROBATION_HOSTS.size() + " on probation, " +
                            _catchersWaiting.size() + " waiting");
                }
            } 
        };
        // Recover hosts on probation every minute.
        probationFuture =
            backgroundExecutor.scheduleWithFixedDelay(probationRestorer, 
                    PROBATION_RECOVERY_WAIT_TIME, PROBATION_RECOVERY_TIME,
                    TimeUnit.MILLISECONDS);
        // Try to fetch hosts whenever we need them.
        // Start it immediately, so that if we have no hosts
        // (because of a fresh installation) we will connect.
        bootstrapperFuture = 
            backgroundExecutor.scheduleWithFixedDelay(bootstrapper, 0, 2000,
                    TimeUnit.MILLISECONDS);
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Peer Locator");
    }
    
    @Override
    public void initialize() {
    }
    
    /**
     * Shuts down background threads and saves the host file.
     */
    @Override
    public void stop() {
        // Shut down the background threads
        if(probationFuture != null)
            probationFuture.cancel(true);
        if(bootstrapperFuture != null)
            bootstrapperFuture.cancel(true);
        if(clearPingedHostsFuture != null)
            clearPingedHostsFuture.cancel(true);
        write();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    /**
     * Informs the host catcher that we have (re)connected to the network;
     * reads the host file and starts pinging.
     */
    public void connect() {
        // Allow pings to be sent for the next PONG_RANKING_EXPIRE_TIME msecs
        lastAllowedPongRankTime =
            System.currentTimeMillis() + PONG_RANKING_EXPIRE_TIME;
        // Schedule a runnable after we stop pinging to clear the set of
        // hosts that were pinged while trying to connect
        Runnable clearPingedHosts = new Runnable() {
            public void run() {
                uniqueHostPinger.resetData();
            }
        };
        clearPingedHostsFuture = backgroundExecutor.schedule(clearPingedHosts,
                PONG_RANKING_EXPIRE_TIME, TimeUnit.MILLISECONDS);
        // Load the default bootstrap servers
        bootstrapper.reset();
        // Read the gnutella.net file
        read();
        // Ping some of the hosts we just loaded
        ArrayList<Endpoint> hosts;
        synchronized(this) {
            hosts = new ArrayList<Endpoint>(ENDPOINT_SET.size() +
                    restoredHosts.size());
            hosts.addAll(ENDPOINT_SET.keySet());
            hosts.addAll(restoredHosts);
        }
        Collections.shuffle(hosts);
        rank(hosts);
    }

    /**
     * Pings some or all of the specified hosts, if necessary.
     */
    private void rank(Collection<? extends IpPort> hosts) {
        if(needsPongRanking()) {
            if(LOG.isTraceEnabled())
                LOG.trace("Sending " + hosts.size() + " hosts to pinger");
            uniqueHostPinger.rank(
                hosts,
                // cancel when connected -- don't send out any more pings
                new Cancellable() {
                    public boolean isCancelled() {
                        return !needsPongRanking();
                    }
                }
            );
        }
    }
    
    /**
     * Sends a ping to every host the catcher knows about (used for
     * discovering DHT-capable hosts).
     */
    public void sendMessageToAllHosts(Message m, MessageListener listener, Cancellable c) {
        uniqueHostPinger.rank(getAllHosts(), listener, c, m);
    }
    
    private synchronized Collection<ExtendedEndpoint> getAllHosts() {
        //keep them ordered -- TODO: Why?
        Collection<ExtendedEndpoint> hosts = new LinkedHashSet<ExtendedEndpoint>(getNumHosts());
        hosts.addAll(FREE_ULTRAPEER_SLOTS_SET.keySet());
        hosts.addAll(FREE_LEAF_SLOTS_SET.keySet());
        hosts.addAll(ENDPOINT_SET.keySet());
        hosts.addAll(restoredHosts);
        return hosts;
    }
    
    /**
     * Gets a (possibly empty) list of hosts that support the DHT. Active nodes
     * are returned first, then passive nodes, then inactive nodes. 
     * Note: this method is slow and is not meant to be used often.
     * 
     * @param minVersion the minimum DHT version, 0 to return all versions.
     */
    public synchronized List<ExtendedEndpoint> getDHTSupportEndpoint(int minVersion) {
        List<ExtendedEndpoint> hosts = new ArrayList<ExtendedEndpoint>();
        IntSet masked = new IntSet();
        boolean filter = ConnectionSettings.FILTER_CLASS_C.getValue();
        for(ExtendedEndpoint host : getAllHosts()) {
            if(host.supportsDHT() && host.getDHTVersion() >= minVersion) {
                int ip = NetworkUtils.getMaskedIP(host.getInetAddress(), PONG_MASK);
                if(!filter || masked.add(ip))
                    hosts.add(host);
            }
        }
        Collections.sort(hosts, DHT_COMPARATOR);
        return hosts;
    }
    
    /**
     * Determines whether UDP pings should be sent out.
     */
    private synchronized boolean needsPongRanking() {
        if(connectionServices.isFullyConnected()) {
            if(LOG.isTraceEnabled())
                LOG.trace("Pong ranking not needed - fully connected");
            return false;
        }
        int have = connectionManager.get().getInitializedConnections().size();
        if(have >= MAX_CONNECTIONS) {
            if(LOG.isTraceEnabled())
                LOG.trace("Pong ranking not needed - have max connections");
            return false;
        }
        long now = System.currentTimeMillis();
        if(now > lastAllowedPongRankTime) {
            if(LOG.isTraceEnabled())
                LOG.trace("Pong ranking not allowed - last time has passed");
            return false;
        }
        int size;
        if(connectionServices.isSupernode()) {
            synchronized(this) {
                size = FREE_ULTRAPEER_SLOTS_SET.size();
            }
        } else {
            synchronized(this) {
                size = FREE_LEAF_SLOTS_SET.size();
            }
        }
        int preferred = connectionManager.get().getPreferredConnectionCount();        
        boolean needsPongRanking =  size < preferred - have;
        if(!needsPongRanking)
            LOG.trace("Pong ranking not needed - have enough hosts to try");
        return needsPongRanking;
    }
    
    /**
     * Reads the host file from the default location.
     */
    private void read() {
        try {
            read(getHostsFile());
        } catch (IOException e) {
            if(LOG.isInfoEnabled())
                LOG.info("Exception reading host file " + getHostsFile(), e);
        }
    }

    /**
     * Reads hosts from the specified file. Package access for testing.
     */
    void read(File hostFile) throws FileNotFoundException, IOException {
        LOG.trace("Reading host file");
        long now = System.currentTimeMillis();
        long lastModified = hostFile.lastModified(); // 0 if file does not exist
        if(now - lastModified > STALE_HOST_FILE) {
            if(lastModified > 0) {
                LOG.info("Deleting stale host file");
                hostFile.delete();
            }
            return; // Hit the bootstrap hosts instead
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(hostFile));
            while(true) {
                String line = in.readLine();
                if(line == null)
                    break;
                try {
                    // Resolve hostnames later
                    ExtendedEndpoint e = ExtendedEndpoint.read(line); 
                    if(e.isUDPHostCache()) {
                        bootstrapper.addUDPHostCache(e);
                    } else if(isValidHost(e)) {
                        synchronized(this) {
                            addPermanent(e);
                            restoredHosts.add(e);
                        }
                        endpointAdded();
                    } else {
                        if(LOG.isTraceEnabled())
                            LOG.trace("File contains invalid host: " + line);
                    }
                } catch (ParseException pe) {
                    LOG.info("Exception parsing host file", pe);
                    continue;
                }
            }
        } finally {
            try {
                if(in != null)
                    in.close();
            } catch(IOException e) {
                LOG.info("Exception closing host file", e);
            }
        }
    }

    /**
     * Writes the host file to the default location. Package access for testing.
     */
    protected void write() {
        try {
            write(getHostsFile());
        } catch(IOException e) {
            if(LOG.isInfoEnabled())
                LOG.info("Exception writing host file " + getHostsFile(), e);   
        }
    }

    /**
     * Writes hosts to the specified file. Package access for testing.
     */
    protected synchronized void write(File hostFile) throws IOException {
        checkInvariants();
        LOG.trace("Writing host file");
        if(dirty || bootstrapper.isWriteDirty()) {
            FileWriter out = new FileWriter(hostFile);
                
            //Write udp hostcache endpoints.
            bootstrapper.write(out);
    
            //Write elements of permanent from worst to best.  Order matters, as it
            //allows read() to put them into queue in the right order without any
            //difficulty.
            for(ExtendedEndpoint e : permanentHosts)
                e.write(out);
            
            out.close();
        }
    }

    /**
     * Returns the default host file.
     */
    private File getHostsFile() {
        return new File(CommonUtils.getUserSettingsDir(), "gnutella.net");
    }
    
    /**
     * Adds hosts from a ping reply, possibly ejecting others from the cache.
     * Also adds any UHCs in the ping reply to the UDPHostCache, active and
     * passive DHT nodes to the DHT manager, and unicast endpoints to the
     * unicast manager.
     * 
     * @return true if any hosts were added to the catcher
     */
    public boolean add(PingReply pr) {
        if(LOG.isTraceEnabled())
            LOG.trace("Pong from " + pr.getAddress() + ":" + pr.getPort());

        // Discard UDP pongs with unknown GUIDs, unless they're from local
        // sources, in which case they might be replies to multicast pings 
        byte[] source = pr.getInetAddress().getAddress();
        boolean isLocalOrPrivate = networkInstanceUtils.isVeryCloseIP(source)
        || networkInstanceUtils.isPrivateAddress(source);
        if(pr.isUDP() && !isLocalOrPrivate) {
            GUID g = new GUID(pr.getGUID());
            if(!g.equals(PingRequest.UDP_GUID) &&
                    !g.equals(udpService.get().getSolicitedGUID())) {
                LOG.info("Discarding UDP pong with unknown GUID");
                return false;
            }
        } 

        // Convert to endpoint
        ExtendedEndpoint endpoint;
        if(pr.getDailyUptime() != -1) {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort(), 
                    pr.getDailyUptime());
        } else {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort());
        }
        if(!pr.getClientLocale().equals(""))
            endpoint.setClientLocale(pr.getClientLocale());
        if(pr.isUDPHostCache()) {
            endpoint.setHostname(pr.getUDPCacheAddress());            
            endpoint.setUDPHostCache(true);
        }
        if(pr.isTLSCapable())
            endpoint.setTLSCapable(true);

        // Make a temporary exception for local addresses so we can extract
        // the packed hosts and UHCs - we'll check validity again later 
        if(!isValidHost(endpoint) && !isLocalOrPrivate) {
            if(LOG.isInfoEnabled())
                LOG.info("Discarding pong from invalid host " + endpoint);
            return false;
        }

        // If the pong has packed hosts, add them
        Collection<IpPort> packed = pr.getPackedIPPorts();
        if(ConnectionSettings.FILTER_CLASS_C.getValue())
            packed = NetworkUtils.filterOnePerClassC(packed);
        if(LOG.isTraceEnabled() && !packed.isEmpty())
            LOG.trace("Pong contains " + packed.size() + " packed endpoints");
        Collection<ExtendedEndpoint> valid = new HashSet<ExtendedEndpoint>();
        boolean addedPacked = false;
        for(IpPort ipp : packed) {
            ExtendedEndpoint ee;
            if(ipp instanceof ExtendedEndpoint) {
                ee = (ExtendedEndpoint)ipp;
            } else {
                ee = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
                if(ipp instanceof Connectable) {
                    // When more items other than TLS are added to HostInfo,
                    // it would make more sense to make this something like:
                    // ep.addHostInfo(ipp);
                    ee.setTLSCapable(((Connectable)ipp).isTLSCapable());
                }
            }
            if(isValidHost(ee)) {
                addedPacked |= add(ee, GOOD_PRIORITY);
                valid.add(ee);
            } else if(LOG.isInfoEnabled()) {
                LOG.info("Not adding invalid host " + ee);
            }
        }
        // Ping the valid packed hosts (if any)
        if(!valid.isEmpty())
            rank(valid);

        // If the pong has packed UHCs, add them
        packed = pr.getPackedUDPHostCaches();
        if(LOG.isTraceEnabled() && !packed.isEmpty())
            LOG.trace("Pong contains " + packed.size() + " packed UHCs");
        for(IpPort ipp : packed) {
            ExtendedEndpoint ee =
                new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
            ee.setUDPHostCache(true);
            if(isValidHost(ee))
                bootstrapper.addUDPHostCache(ee);
        }

        // If the pong came from a local address but we let it through
        // to extract the packed hosts and UHCs, throw it away now
        if(!isValidHost(endpoint)) {
            if(LOG.isInfoEnabled())
                LOG.info("Not adding invalid host " + endpoint);
            return addedPacked;
        }

        // If the pong came from a UHC, just add it as that
        if(endpoint.isUDPHostCache()) {
            LOG.trace("Adding host as UHC");
            return bootstrapper.addUDPHostCache(endpoint) || addedPacked;
        }

        // If the pong came from a DHT node, pass it to the DHT manager
        int dhtVersion = pr.getDHTVersion();
        if(dhtVersion > -1) {
            DHTMode mode = pr.getDHTMode();
            endpoint.setDHTVersion(dhtVersion);
            endpoint.setDHTMode(mode);
            if(dhtManager.get().isRunning()) {
                // Send active and passive DHT endpoints to the DHT manager
                if(mode.equals(DHTMode.ACTIVE)) {
                    SocketAddress address = new InetSocketAddress(
                            endpoint.getAddress(), endpoint.getPort());
                    dhtManager.get().addActiveDHTNode(address);
                } else if(mode.equals(DHTMode.PASSIVE)) {
                    SocketAddress address = new InetSocketAddress(
                            endpoint.getAddress(), endpoint.getPort());
                    dhtManager.get().addPassiveDHTNode(address);
                }
            }
        }

        // If the pong came from a unicast endpoint, pass it to the unicaster
        if(pr.supportsUnicast()) {
            queryUnicaster.get().addUnicastEndpoint(
                    pr.getInetAddress(), pr.getPort());
        }

        // Add the endpoint with good priority if it's an ultrapeer
        boolean addedSource = false;
        if(pr.isUltrapeer()) {
            // Add it to our free leaf slots list if it has free leaf slots
            if(pr.hasFreeLeafSlots()) {
                LOG.trace("Adding host to free leaf slot set");
                addToFreeSlotSet(endpoint, FREE_LEAF_SLOTS_SET);
                addedSource = true;
            }
            // Add it to our free UP slots list if it has free UP slots, or if
            // the locales match and it has free locale preferencing slots
            String myLocale = ApplicationSettings.LANGUAGE.get();
            if(pr.hasFreeUltrapeerSlots() || 
                    (myLocale.equals(pr.getClientLocale()) &&
                            pr.getNumFreeLocaleSlots() > 0)) {
                LOG.trace("Adding host to free UP slot set");
                addToFreeSlotSet(endpoint, FREE_ULTRAPEER_SLOTS_SET);
                addedSource = true;
            }
            if(!addedSource) {
                LOG.trace("Adding host with good priority");
                addedSource = add(endpoint, GOOD_PRIORITY);
            }
        } else {
            LOG.trace("Adding host with normal priority");
            addedSource = add(endpoint, NORMAL_PRIORITY);
        }
        return addedPacked || addedSource;
    }
    
    /**
     * Adds a host to the specified set and the permanent set.
     */
    private void addToFreeSlotSet(ExtendedEndpoint host,
            Map<? super ExtendedEndpoint, ? super ExtendedEndpoint> hosts) {
        synchronized(this) {
            hosts.put(host, host);
            addPermanent(host);
        }        
        endpointAdded();
    }

    /**
     * Adds an endpoint to the map which matches locales to sets of endpoints.
     * 
     * LOCKING: this
     */
    private void addToLocaleMap(ExtendedEndpoint ee) {
        String locale = ee.getClientLocale();
        Set<ExtendedEndpoint> s = LOCALE_SET_MAP.get(locale);
        if(s == null) {
            s = new HashSet<ExtendedEndpoint>();
            LOCALE_SET_MAP.put(locale, s);
        }
        s.add(ee);
        if(s.size() > LOCALE_SET_SIZE)
            s.remove(s.iterator().next());
    }
    
    /**
     * Adds a collection of hosts to the catcher.
     * 
     * @return the number of hosts added
     */
    public int add(Collection<? extends Endpoint> endpoints) {
        rank(endpoints);
        int added = 0;
        for(Endpoint e: endpoints) {
            if(add(e, true)) {
                added++;
            }
        }
        return added;
    }

    /**
     * Adds a host, possibly ejecting others from the cache.
     *
     * @param e the host to add
     * @param forceHighPriority true if the host should have high priority
     * @return true if the host was added
     */
    public boolean add(Endpoint e, boolean forceHighPriority) {
        if(!isValidHost(e)) {
            if(LOG.isInfoEnabled())
                LOG.info("Not adding invalid host " + e);
            return false;
        }
        if (forceHighPriority)
            return add(e, GOOD_PRIORITY);
        else
            return add(e, NORMAL_PRIORITY);
    }

    /**
     * Adds a host with known locale. This is used when a handshake is
     * rejected because of the locale.
     * 
     * @return true if the host was added
     */
    public boolean add(Endpoint e, boolean forceHighPriority, String locale) {
        if(!isValidHost(e)) {
            if(LOG.isInfoEnabled())
                LOG.info("Not adding invalid host " + e);
            return false;
        }
        //need ExtendedEndpoint for the locale
        if(forceHighPriority)
            return add(new ExtendedEndpoint(e.getAddress(), 
                    e.getPort(), locale), GOOD_PRIORITY);
        else
            return add(new ExtendedEndpoint(e.getAddress(),
                    e.getPort(), locale), NORMAL_PRIORITY);
    }

    /**
     * Adds a host with the specified priority.
     * 
     * @param e the host to add
     * @param priority the priority of the host
     * @return true if the host was added
     */
    private boolean add(Endpoint host, int priority) {
        if(host instanceof ExtendedEndpoint)
            return add((ExtendedEndpoint)host, priority);
        return add(new ExtendedEndpoint(host.getAddress(), 
                host.getPort()), priority);
    }

    /**
     * Adds a host with the specified priority.
     * 
     * @param ee the host to add
     * @param priority the priority of the host
     * @return true if the host was added
     */
    private boolean add(ExtendedEndpoint e, int priority) {
        checkInvariants();
        
        if(e.isUDPHostCache()) {
            LOG.trace("Adding host as UHC");
            return bootstrapper.addUDPHostCache(e);
        }
        
        boolean added = false;
        synchronized(this) {
            addPermanent(e);
            if(ENDPOINT_SET.containsKey(e)) {
                //TODO: we could adjust the key
                LOG.trace("Not adding duplicate host");
                return false;
            }
            ExtendedEndpoint removed = ENDPOINT_QUEUE.insert(e, priority);
            if(removed != e) {
                // The host was actually added...
                if(LOG.isInfoEnabled())
                    LOG.info("Adding host " + e);
                ENDPOINT_SET.put(e, e);
                if(removed != null) {
                    // ...and something else was removed
                    if(LOG.isTraceEnabled())
                        LOG.trace("Ejected host " + removed);
                    ENDPOINT_SET.remove(removed);
                }
                added = true;
            }
        }
        if(added)
            endpointAdded();
        checkInvariants();
        return added;
    }

    /**
     * Adds a host to the set that will be saved for future sessions.
     * 
     * LOCKING: this
     *
     * @param ee the host to add
     * @return true if the host was added
     */
    private boolean addPermanent(ExtendedEndpoint e) {
        if(networkInstanceUtils.isPrivateAddress(e.getInetAddress())) {
            LOG.trace("Not permanently adding host with private address");
            return false;
        }
        if(permanentHostsSet.contains(e)) {
            //TODO: we could adjust the key
            LOG.trace("Not permanently adding duplicate host");
            return false;
        }
        addToLocaleMap(e);
        ExtendedEndpoint removed = permanentHosts.insert(e);
        if(removed != e) {
            // The host was actually added...
            if(LOG.isInfoEnabled())
                LOG.info("Permanently adding host " + e);
            permanentHostsSet.add(e);
            if(removed != null) {
                // ...and something else was removed
                if(LOG.isTraceEnabled())
                    LOG.trace("Ejected permanent host " + removed);
                permanentHostsSet.remove(removed);
            }
            dirty = true;
            return true;
        } else {
            // Uptime was not good enough to add
            LOG.trace("Not permanently adding host with low uptime");
            return false;
        }
    }
    
    /**
     * Removes a host from the set that will be saved for future sessions.
     * 
     * LOCKING: this
     * 
     * @return true if the host was removed
     */
    private boolean removePermanent(ExtendedEndpoint e) {
        boolean removed1 = permanentHosts.remove(e);
        boolean removed2 = permanentHostsSet.remove(e);
        assert removed1 == removed2 : "Queue "+removed1+" but set "+removed2;
        if(removed1) {
            dirty = true;
            if(LOG.isTraceEnabled())
                LOG.trace("Removed permanent host " + e);
        }
        return removed1;
    }

    /**
     * Determines whether a host is valid for adding to the catcher.
     */
    public boolean isValidHost(Endpoint host) {
        if(LOG.isTraceEnabled())
            LOG.trace("Validating host " + host);
        
        // Don't add the host if its address is unknown, private or blacklisted
        byte[] addr;
        try {
            addr = host.getHostBytes();
        } catch(UnknownHostException uhe) {
            LOG.trace("Host is invalid: unknown host");
            return false;
        }
        if(networkInstanceUtils.isPrivateAddress(addr)) {
            LOG.trace("Host is invalid: private address");
            return false;
        }
        if(networkInstanceUtils.isMe(addr, host.getPort())) {
            LOG.trace("Host is invalid: own address");
            return false;
        }
        if(!ipFilter.get().allow(addr)) {
            LOG.trace("Host is invalid: blacklisted");
            return false;
        }

        // Don't add the host if it has previously failed
        synchronized(this) {
            if(EXPIRED_HOSTS.contains(host)) {
                LOG.trace("Host is invalid: expired");
                return false;
            }
            if(PROBATION_HOSTS.contains(host)) {
                LOG.trace("Host is invalid: on probation");
                return false;
            }
        }

        LOG.trace("Host is valid");
        return true;
    }
    
    /**
     * Returns true if the given host is known to be TLS-capable.
     */
    public boolean isHostTLSCapable(IpPort ipp) {
        if(ipp instanceof Connectable) {
            boolean capable = ((Connectable)ipp).isTLSCapable();
            if(LOG.isTraceEnabled())
                LOG.trace(ipp + (capable ? " is" : " is not") + " TLS capable");
            return capable;
        }        
        // Retrieve an ExtendedEndpoint using its IP and port
        Endpoint p = new Endpoint(ipp.getAddress(), ipp.getPort());
        ExtendedEndpoint ee;
        // Look everywhere
        synchronized(this) {
            ee = ENDPOINT_SET.get(p);
            if(ee == null)
                ee = FREE_ULTRAPEER_SLOTS_SET.get(p);
            if(ee == null)
                ee = FREE_LEAF_SLOTS_SET.get(p);
        }
        
        if(ee == null) {
            if(LOG.isTraceEnabled())
                LOG.trace(ipp + " is not known to be TLS capable");
            return false;
        } else {
            boolean capable = ee.isTLSCapable();
            if(LOG.isTraceEnabled())
                LOG.trace(ipp + (capable ? " is" : " is not") + " TLS capable");
            return capable;
        }
    }
    
    /**
     * Notifies one waiting observer that an endpoint is now available.
     */
    private void endpointAdded() {
        Endpoint p;
        EndpointObserver observer;
        synchronized (this) {
            if(_catchersWaiting.isEmpty()) {
                LOG.trace("No observers waiting");
                return; // no one waiting.
            }
            
            p = getAnEndpointInternal();
            if(p == null) {
                LOG.trace("No hosts available");
                return; // no more endpoints to give.
            }
            
            observer = _catchersWaiting.remove(0);
        }
        // It's important that this is outside the lock, otherwise
        // HostCatcher's lock is exposed to the outside world
        LOG.trace("Returning a host to a waiting observer");
        observer.handleEndpoint(p);
    }

    /**
     * Passes the next available host to the given EndpointObserver. If
     * no host is immediately available, the observer is added to a list
     * of waiting observers.
     */
    public void getAnEndpoint(EndpointObserver observer) {
        Endpoint p;
        
        // We can only lock around endpoint retrieval & _catchersWaiting,
        // we don't want to expose our lock to the observer.
        synchronized(this) {
            p = getAnEndpointInternal();
            if(p == null) {
                LOG.trace("Couldn't get a host immediately; waiting");
                _catchersWaiting.add(observer);
            }
        }
        
        if(p != null)
            observer.handleEndpoint(p);
    }
    
    /**
     * Returns a host immediately if one is available. Otherwise the observer
     * is added to a list of waiting observers and null is returned.
     * 
     * If the observer is null and no host is available, this method returns
     * null and schedules no future callback.
     */
    public Endpoint getAnEndpointImmediate(EndpointObserver observer) {
        Endpoint p;
        
        synchronized(this) {
            p = getAnEndpointInternal();
            if(p == null && observer != null) {
                LOG.trace("Couldn't get a host immediately; waiting");
                _catchersWaiting.add(observer);
            }
        }
        
        return p;
    }
    
    /**
     * Removes an observer that is waiting for an endpoint.
     */
    public synchronized void removeEndpointObserver(EndpointObserver observer) {
        LOG.trace("Removing waiting observer");
        _catchersWaiting.remove(observer);
    }

    /**
     * Removes and returns a host, blocking if one is not immediately available.
     * Package access for testing.
     * 
     * @throws InterruptedException if the calling thread is interrupted
     */
    protected Endpoint getAnEndpoint() throws InterruptedException {
        BlockingObserver observer = new BlockingObserver();

        getAnEndpoint(observer);
        try {
            synchronized (observer) {
                if(observer.getEndpoint() == null) {
                    // only stops waiting when handleEndpoint is called.
                    observer.wait();
                }
                return observer.getEndpoint();
            }
        } catch (InterruptedException ie) {
            // If we got interrupted, we must remove the waiting observer.
            synchronized (this) {
                LOG.trace("Removing waiting observer");
                _catchersWaiting.remove(observer);
                throw ie;
            }
        }
    }
  
    /**
     * Notifies the catcher that a connection fetcher has finished attempting
     * a connection to the given host. This allows the catcher to update the
     * host's connection history.
     */
    public synchronized void doneWithConnect(Endpoint e, boolean success) {
        //Normal host: update key.  TODO: adjustKey() operation may be more
        //efficient.
        if(!(e instanceof ExtendedEndpoint)) {
            //Should never happen, but I don't want to update public
            //interface of this to operate on ExtendedEndpoint.
            LOG.warn("Endpoint in doneWithConnect() is not ExtendedEndpoint");
            return;
        }
        
        ExtendedEndpoint ee = (ExtendedEndpoint)e;

        removePermanent(ee);
        if(success) {
            ee.recordConnectionSuccess();
        } else {
            ee.recordConnectionFailure();
        }
        addPermanent(ee);
    }

    /**
     * Removes and returns the best available host, or null if no hosts are
     * available. Protected so tests can override it.
     * 
     * LOCKING: this
     */
    protected ExtendedEndpoint getAnEndpointInternal() {
        if(connectionServices.isSupernode()) {
            // Ultrapeer - prefer hosts with free ultrapeer slots
            if(!FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
                LOG.trace("UP: returning host with free UP slots");
                return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);
            }
            if(!FREE_LEAF_SLOTS_SET.isEmpty()) {
                LOG.trace("UP: returning host with free leaf slots");
                return preferenceWithLocale(FREE_LEAF_SLOTS_SET);
            }
        } else {
            // Leaf or undecided - prefer hosts with free leaf slots
            if(!FREE_LEAF_SLOTS_SET.isEmpty()) {
                LOG.trace("Returning host with free leaf slots");
                return preferenceWithLocale(FREE_LEAF_SLOTS_SET);
            }
            if(!FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
                LOG.trace("Returning host with free UP slots");
                return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);
            }
        }
        // No free slots
        if(!ENDPOINT_QUEUE.isEmpty()) {
            LOG.trace("Returning ordinary host");
            ExtendedEndpoint removed1 = ENDPOINT_QUEUE.extractMax();
            ExtendedEndpoint removed2 = ENDPOINT_SET.remove(removed1);
            assert removed1 == removed2 : "Invariant for HostCatcher broken";
            return removed1;
        }
        // Getting pretty desperate now
        if(!restoredHosts.isEmpty()) {
            LOG.trace("Returning restored host with high uptime");
            // Last partition has highest uptimes
            List<ExtendedEndpoint> best = uptimePartitions.getLastPartition();
            ExtendedEndpoint ee =
                best.remove((int)(Math.random() * best.size()));
            return ee;
        }
        LOG.trace("No hosts to return");
        return null;
    }
    
    /**
     * Returns a host from the specified set that matches our locale, or a
     * randomly chosen host from the set if there are no matches.
     * 
     * LOCKING: this
     */
    private ExtendedEndpoint preferenceWithLocale(
            RandomAccessMap<ExtendedEndpoint, ExtendedEndpoint> base) {

        String loc = ApplicationSettings.LANGUAGE.get();
        ExtendedEndpoint ret = null;
        // preference a locale host if we haven't matched any locales yet
        if(!connectionManager.get().isLocaleMatched()) {
            if(LOCALE_SET_MAP.containsKey(loc)) {
                Set<ExtendedEndpoint> locales = LOCALE_SET_MAP.get(loc);
                // Iterate in a random order
                for(ExtendedEndpoint e : base.keySet()) {
                    if(locales.contains(e)) {
                        LOG.trace("Found a host with matching locale");
                        locales.remove(e);
                        ret = e;
                        break;
                    }
                }
            }
        }
        if(ret == null) {
            LOG.trace("Did not find a host with matching locale");
            ret = base.getKeyAt(RND.nextInt(base.size()));
        }
        
        Object removed = base.remove(ret);
        assert ret == removed : "Key: " + ret + ", value: " + removed;
        return ret;
    }

    /**
     * Returns the number of hosts the catcher knows about.
     */
    public synchronized int getNumHosts() {
        int hosts = ENDPOINT_QUEUE.size()+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size()+restoredHosts.size();
        if(LOG.isTraceEnabled())
            LOG.trace(hosts + " hosts");
        return hosts;
    }

    /**
     * Returns the number of hosts that are known to be ultrapeers.
     * Package access for testing.
     */
    protected synchronized int getNumUltrapeerHosts() {
        return ENDPOINT_QUEUE.size(GOOD_PRIORITY)+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
    }

    /**
     * Returns an iterator of the set of permanent hosts. Package access for
     * testing. THIS MUST NOT BE MODIFIED WHILE THE ITERATOR IS IN USE.
     */
    protected Iterator<ExtendedEndpoint> getPermanentHosts() {
        return permanentHosts.iterator();
    }

    /**
     * Returns a collection containing up to the specified number of hosts that
     * have advertised free ultrapeer slots, matching the specified locale if
     * possible. The returned collection can be modified.
     */
    public synchronized Collection<IpPort>
    getUltrapeersWithFreeUltrapeerSlots(String locale, int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET, locale, num);
    }
    
    /**
     * Returns a collection containing up to the specified number of hosts that
     * have advertised free leaf slots, matching the specified locale if
     * possible. The returned collection can be modified.
     */
    public synchronized Collection<IpPort>
    getUltrapeersWithFreeLeafSlots(String locale, int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET, locale, num);
    }

    /**
     * Selects up to the specified number of hosts from a collection,
     * matching the specified locale if possible. The returned collection
     * can be modified.
     */
    private Collection<IpPort> getPreferencedCollection(
            Map<? extends ExtendedEndpoint, ? extends ExtendedEndpoint> base,
            String loc, int num) {
        if(loc == null || loc.equals(""))
            loc = ApplicationSettings.DEFAULT_LOCALE.get();

        Set<IpPort> hosts = new HashSet<IpPort>(num);
        IntSet masked = new IntSet();
        
        Set<ExtendedEndpoint> locales = LOCALE_SET_MAP.get(loc);
        boolean filter = ConnectionSettings.FILTER_CLASS_C.getValue();
        if(locales != null) {
            for(ExtendedEndpoint e : locales) {
                if(hosts.size() >= num)
                    break;
                if(base.containsKey(e)) { 
                    int ip = NetworkUtils.getMaskedIP(e.getInetAddress(), PONG_MASK);
                    if(!filter || masked.add(ip))
                        hosts.add(e);
                }
            }
            if(LOG.isTraceEnabled())
                LOG.trace("Found " + hosts.size() + " locale-matched hosts");
        }
        
        for(IpPort ipp : base.keySet()) {
            if(hosts.size() >= num)
                break;
            int ip = NetworkUtils.getMaskedIP(ipp.getInetAddress(), PONG_MASK);
            if(!filter || masked.add(ip))
                hosts.add(ipp);
        }
        if(LOG.isTraceEnabled())
            LOG.trace("Found " + hosts.size() + " hosts");
        
        return hosts;
    }

    /**
     * Resets all recorded failures on the assumption that they were
     * caused by having no internet connection
     */
    public void noInternetConnection() {
        LOG.trace("Resetting failures caused by no internet connection");

        synchronized(this) {
            PROBATION_HOSTS.clear();
            EXPIRED_HOSTS.clear();
            bootstrapper.reset();
            restoredHosts.clear();
            uniqueHostPinger.resetData();
        }

        // Read the hosts file again.  This will also notify any waiting 
        // connection fetchers from previous connection attempts.
        read();
    }

    /**
     * Resets the state of the host catcher (only for testing).
     */
    public synchronized void reset() {
        LOG.trace("Clearing hosts");
        FREE_LEAF_SLOTS_SET.clear();
        FREE_ULTRAPEER_SLOTS_SET.clear();
        LOCALE_SET_MAP.clear();
        ENDPOINT_QUEUE.clear();
        ENDPOINT_SET.clear();
        restoredHosts.clear();
        PROBATION_HOSTS.clear();
        EXPIRED_HOSTS.clear();
        permanentHosts.clear();
        permanentHostsSet.clear();
        bootstrapper.reset();
        uniqueHostPinger.resetData();
    }
    
    /** Whether to check invariants. Package access for testing. */
    static boolean DEBUG = false;

    /**
     * Checks invariants. Not as slow as it used to be.
     */
    private void checkInvariants() {
        if(DEBUG) {
            synchronized(this) {
                // Check ENDPOINT_SET == ENDPOINT_QUEUE
                for(ExtendedEndpoint ee : ENDPOINT_QUEUE) {
                    assert ENDPOINT_SET.containsKey(ee);
                }
                assert ENDPOINT_QUEUE.size() == ENDPOINT_SET.size();
                // Check permanentHostsSet === permanentHosts
                for(ExtendedEndpoint ee : permanentHosts) {
                    assert permanentHostsSet.contains(ee);
                }
                assert permanentHosts.size() == permanentHostsSet.size();
            }
        }
    }
    
    /**
     * Adds the specified host to the group of hosts currently on "probation."
     * These are hosts that are on the network but that have rejected a 
     * connection attempt.  They will periodically be re-activated as needed.
     */
    public synchronized void putHostOnProbation(Endpoint host) {
        LOG.trace("Putting a host on probation");
        PROBATION_HOSTS.add(host);
        if(PROBATION_HOSTS.size() > PROBATION_HOSTS_SIZE) {
            PROBATION_HOSTS.remove(PROBATION_HOSTS.iterator().next());
        }
    }
    
    /**
     * Adds the specified host to the group of expired hosts.  These are hosts
     * that we have been unable to create a TCP connection to, let alone a 
     * Gnutella connection.
     */
    public synchronized void expireHost(Endpoint host) {
        LOG.trace("Expiring a host");
        EXPIRED_HOSTS.add(host);
        if(EXPIRED_HOSTS.size() > EXPIRED_HOSTS_SIZE) {
            EXPIRED_HOSTS.remove(EXPIRED_HOSTS.iterator().next());
        }
    }
    
    /** Returns true if the host catcher needs hosts from the bootstrapper. */
    @Override
    public boolean needsHosts() {
        return getNumHosts() == 0;
    }
    
    /** Receives hosts from the bootstrapper, returning the number used. */
    @Override
    public int handleHosts(Collection<? extends Endpoint> hosts) {
        return add(hosts);
    }
    
    /** Simple callback for having an endpoint added. */
    public static interface EndpointObserver {
        public void handleEndpoint(Endpoint p);
    }
    
    /** A blocking implementation of EndpointObserver. */
    private static class BlockingObserver implements EndpointObserver {
        private Endpoint endpoint;
        
        public synchronized void handleEndpoint(Endpoint p) {
            endpoint = p;
            notify();
        }
        
        public Endpoint getEndpoint() {
            return endpoint;
        }
    }
}
