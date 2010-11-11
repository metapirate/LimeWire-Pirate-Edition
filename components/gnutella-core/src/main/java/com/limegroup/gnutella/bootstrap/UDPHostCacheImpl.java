package com.limegroup.gnutella.bootstrap;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Cancellable;
import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.address.StrictIpPortSet;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * Manages a set of UDP host caches and retrieves hosts from them.
 */
@Singleton
class UDPHostCacheImpl implements UDPHostCache {

    private static final Log LOG = LogFactory.getLog(UDPHostCacheImpl.class);

    /**
     * The maximum number of failures to allow for a given UHC.
     */
    private static final int MAXIMUM_FAILURES = 5;

    /**
     * The maximum number of UHCs to remember between
     * launches, or at any given time.
     */
    public static final int PERMANENT_SIZE = 100;

    /**
     * The number of UHCs we try to fetch from at once.
     */
    public static final int FETCH_AMOUNT = 5;

    /**
     * How many milliseconds to wait before retrying a UHC.
     * FIXME: this is too long, we will have given up before retrying
     */
    public static final int EXPIRY_TIME = 10 * 60 * 1000;

    /**
     * A list of UHCs, to allow easy sorting & randomizing.
     * A set is also maintained, to easily look up duplicates.
     * INVARIANT: udpHosts contains no duplicates and contains exactly
     *  the same elements and udpHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final List<ExtendedEndpoint> udpHosts = new ArrayList<ExtendedEndpoint>(PERMANENT_SIZE);
    private final Set<ExtendedEndpoint> udpHostsSet = new HashSet<ExtendedEndpoint>();
    private final UniqueHostPinger pinger;

    /**
     * A set of UHCs we've recently contacted, so we don't contact them again.
     */
    private final Set<ExtendedEndpoint> attemptedHosts;

    /**
     * Whether or not we need to resort the UHCs by failures.
     */
    private boolean dirty = false;

    /**
     * Whether or not the set contains data different than when we last wrote.
     */
    private boolean writeDirty = false;

    private final Provider<MessageRouter> messageRouter;

    private final PingRequestFactory pingRequestFactory;

    private final ConnectionServices connectionServices;

    private final NetworkInstanceUtils networkInstanceUtils;

    /**
     * Constructs a new UDPHostCacheImpl that remembers attempting UHCs for the
     * default expiry time.
     */
    @Inject
    protected UDPHostCacheImpl(UniqueHostPinger pinger,
            Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory,
            ConnectionServices connectionServices,
            NetworkInstanceUtils networkInstanceUtils) {
        this(EXPIRY_TIME, pinger, messageRouter, pingRequestFactory,
                connectionServices, networkInstanceUtils);
    }

    /**
     * Constructs a new UDPHostCacheImpl that remembers attempting UHCs for the
     * given amount of time, in msecs.
     * 
     * @param connectionServices
     */
    UDPHostCacheImpl(int expiryTime, UniqueHostPinger pinger,
            Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory,
            ConnectionServices connectionServices,
            NetworkInstanceUtils networkInstanceUtils) {
        this.connectionServices = connectionServices;
        attemptedHosts = new FixedSizeExpiringSet<ExtendedEndpoint>(PERMANENT_SIZE, expiryTime);
        this.pinger = pinger;
        this.messageRouter = messageRouter;
        this.pingRequestFactory = pingRequestFactory;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /**
     * Writes the set of UHCs to the given stream.
     */
    @Override
    public synchronized void write(Writer out) throws IOException {
        for(ExtendedEndpoint e: udpHosts) {
            e.write(out);
        }
        writeDirty = false;
    }

    /**
     * Returns true if the set of UHCs needs to be saved.
     */
    @Override
    public synchronized boolean isWriteDirty() {
        return writeDirty;
    }

    /**
     * Returns the number of UHCs in the set.
     */
    @Override
    public synchronized int getSize() {
        return udpHostsSet.size();
    }

    /**
     * Attempts to contact some UHCs to retrieve hosts. This method blocks
     * while resolving hostnames.
     */
    @Override
    public synchronized boolean fetchHosts() {
        // If the hosts have been used, shuffle and sort them
        if(dirty) {
            // shuffle then sort, ensuring that we're still going to use
            // hosts in order of failure, but within each of those buckets
            // the order will be random.
            LOG.trace("Shuffling and sorting UHCs");
            Collections.shuffle(udpHosts);
            Collections.sort(udpHosts, FAILURE_COMPARATOR);
            dirty = false;
        }

        // Keep only the first FETCH_AMOUNT of the valid hosts.
        List<ExtendedEndpoint> validHosts = new ArrayList<ExtendedEndpoint>(Math.min(FETCH_AMOUNT, udpHosts.size()));
        List<ExtendedEndpoint> invalidHosts = new LinkedList<ExtendedEndpoint>();
        for(ExtendedEndpoint next : udpHosts) {
            if(validHosts.size() >= FETCH_AMOUNT)
                break;
            if(attemptedHosts.contains(next)) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Already attempted " + next);
                continue;
            }

            // Resolve addresses and remove UHCs with invalid addresses
            if(!networkInstanceUtils.isValidExternalIpPort(next)) {
                if(LOG.isInfoEnabled())
                    LOG.info("Invalid address for " + next);
                invalidHosts.add(next);
                continue;
            }

            validHosts.add(next);
        }

        // Remove all invalid hosts.
        for(ExtendedEndpoint next : invalidHosts) {
            remove(next);
        }

        attemptedHosts.addAll(validHosts);

        return fetch(validHosts);
    }

    /**
     * Attempts to contact the given set of UHCs to retrieve hosts.
     * Protected for testing.
     */
    protected boolean fetch(Collection<? extends ExtendedEndpoint> hosts) {
        if(hosts.isEmpty()) {
            LOG.info("No UHCs to try");
            return false;
        }

        if(LOG.isInfoEnabled())
            LOG.info("Pinging UHCs " + hosts);

        pinger.rank(
                hosts,
                new HostExpirer(hosts),
                // cancel when connected -- don't send out any more pings
                new Cancellable() {
                    public boolean isCancelled() {
                        return connectionServices.isConnected();
                    }
                },
                getPing()
        );
        return true;
    }

    /**
     * Constructs and returns a ping to be sent to UHCs.
     * Protected for testing.
     */
    protected PingRequest getPing() {
        return pingRequestFactory.createUHCPing();
    }

    /**
     * Removes a UHC from the set, returning true if it was removed.
     * Protected for testing.
     */
    protected synchronized boolean remove(ExtendedEndpoint e) {
        if(LOG.isInfoEnabled())
            LOG.info("Removing UHC " + e);
        boolean removed1=udpHosts.remove(e);
        boolean removed2=udpHostsSet.remove(e);
        assert removed1==removed2 : "Set "+removed1+" but queue "+removed2;
        if(removed1)
            writeDirty = true;
        return removed1;
    }

    /**
     * Adds a new UHC to the set, returning true if it was added.
     */
    @Override
    public synchronized boolean add(ExtendedEndpoint e) {
        assert e.isUDPHostCache();

        if(udpHostsSet.contains(e)) {
            if(LOG.isTraceEnabled())
                LOG.trace("Not adding known UHC " + e);
            return false;
        }        

        // note that we do not do any comparisons to ensure that
        // this host is "better" than existing hosts.
        // the rationale is that we'll only ever be adding hosts
        // who have a failure count of 0 (unless we're reading
        // from gnutella.net, in which case all will be added),
        // and we always want to try new people.

        if(LOG.isInfoEnabled())
            LOG.info("Adding UHC " + e);

        // if we've exceeded the maximum size, remove the worst element.
        if(udpHosts.size() >= PERMANENT_SIZE) {
            Object removed = udpHosts.remove(udpHosts.size() - 1);
            udpHostsSet.remove(removed);
            if(LOG.isTraceEnabled())
                LOG.trace("Ejected UHC " + removed);
        }

        // just insert.  we'll sort later.
        udpHosts.add(e);
        udpHostsSet.add(e);
        dirty = true;
        writeDirty = true;
        return true;
    }

    /**
     * Creates a UHC from the given host and port and adds it to the set.
     */
    @SuppressWarnings("unused")
    private void createAndAdd(String host, int port) {
        try {
        	// Resolve hostnames later
            add(new ExtendedEndpoint(host, port, false).setUDPHostCache(true));
        } catch(IllegalArgumentException ignored) {}
    }

    /**
     * Listener that listens for message from the specified UHCs, incrementing
     * the failure counts of any that do not respond and resetting the failure
     * counts of any that do. If a UHC exceeds the maximum failure count it is
     * removed.
     */
    private class HostExpirer implements MessageListener {

        private final Set<ExtendedEndpoint> hosts = new StrictIpPortSet<ExtendedEndpoint>();

        // allHosts contains all the hosts, so that we can
        // iterate over successful caches too.
        private final Set<ExtendedEndpoint> allHosts;
        private byte[] guid;

        /**
         * Constructs a new HostExpirer for the specified UHCs.
         */
        public HostExpirer(Collection<? extends ExtendedEndpoint> hostsToAdd) {
            hosts.addAll(hostsToAdd);
            allHosts = new HashSet<ExtendedEndpoint>(hostsToAdd);
            removeDuplicates(hostsToAdd, hosts);
        }

        /**
         * Removes any UHCs that exist in 'all' but not in 'some'.
         */
        private void removeDuplicates(Collection<? extends ExtendedEndpoint> all, Collection<? extends ExtendedEndpoint> some) {
            // Iterate through what's in our collection vs whats in our set.
            // If any entries exist in the collection but not in the set,
            // then that means they resolved to the same address.
            // Automatically eject entries that resolve to the same address.
            Set<ExtendedEndpoint> duplicates = new HashSet<ExtendedEndpoint>(all);
            duplicates.removeAll(some); // remove any hosts we're keeping.
            for(ExtendedEndpoint ep : duplicates) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Removing duplicate entry " + ep);
                remove(ep);
            }
        }

        /**
         * Notification that a message has been processed.
         */
        @Override
        public void processMessage(Message m, ReplyHandler handler) {
            // We expect only UDP replies
            if(handler instanceof UDPReplyHandler) {
                if(hosts.remove(handler)) {
                    if(LOG.isTraceEnabled())
                        LOG.trace("Recieved: " + m);
                }
                // OPTIMIZATION: if we've got successful responses from
                // all the UHCs, unregister ourselves early
                if(hosts.isEmpty()) {
                    LOG.trace("Unregistering message listener");
                    messageRouter.get().unregisterMessageListener(guid, this);
                }
            }
       }

        /**
         * Notification that this listener is now registered with the 
         * specified GUID.
         */
        @Override
        public void registered(byte[] g) {
            this.guid = g;
        }

        /**
         * Notification that this listener is now unregistered for the 
         * specified guid.
         */
        @Override
        public void unregistered(byte[] g) {
            synchronized(UDPHostCacheImpl.this) {
                // Record the failures...
                for(ExtendedEndpoint ep : hosts) {
                    if(LOG.isInfoEnabled())
                        LOG.info("No response from UHC " + ep);
                    ep.recordUDPHostCacheFailure();
                    dirty = true;
                    writeDirty = true;
                    if(ep.getUDPHostCacheFailures() > MAXIMUM_FAILURES)
                        remove(ep);
                }
                // Then record the successes...
                allHosts.removeAll(hosts);
                for(ExtendedEndpoint ep : allHosts) {
                    if(LOG.isInfoEnabled())
                        LOG.info("Valid response from UHC " + ep);
                    ep.recordUDPHostCacheSuccess();
                    dirty = true;
                    writeDirty = true;
                }
            }
        }
    }

    /**
     * The only FailureComparator we'll ever need.
     */
    private static final Comparator<ExtendedEndpoint> FAILURE_COMPARATOR = new FailureComparator();
    private static class FailureComparator implements Comparator<ExtendedEndpoint> {
        public int compare(ExtendedEndpoint e1, ExtendedEndpoint e2) {
            return e1.getUDPHostCacheFailures() - e2.getUDPHostCacheFailures();
        }
    }
}
