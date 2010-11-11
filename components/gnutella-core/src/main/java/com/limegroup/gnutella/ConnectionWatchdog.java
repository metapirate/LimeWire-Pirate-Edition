package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

/*
 * A "watchdog" that periodically examines connections and
 * replaces dud connections with better ones.  There are a number of
 * possible heuristics to use when examining connections.
 */
@EagerSingleton
public final class ConnectionWatchdog implements Service {
    
    private static final Log LOG = LogFactory.getLog(ConnectionWatchdog.class);

    /** How long (in msec) a connection can be a dud (see below) before being booted. */
    private static final int EVALUATE_TIME=30000;
    /** Additional time (in msec) to wait before rechecking connections. */
    private static final int REEVALUATE_TIME=15000;
    
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<ConnectionManager> connectionManager;
    private final ConnectionServices connectionServices;

    private final PingRequestFactory pingRequestFactory;
    private volatile int oldKillCount;
    
    @Inject
    public ConnectionWatchdog(
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<MessageRouter> messageRouter,
            Provider<ConnectionManager> connectionManager,
            ConnectionServices connectionServices,
            PingRequestFactory pingRequestFactory) {
        this.backgroundExecutor = backgroundExecutor;
        this.messageRouter = messageRouter;
        this.connectionManager = connectionManager;
        this.connectionServices = connectionServices;
        this.pingRequestFactory = pingRequestFactory;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Stale Connection Management");
    }
    
    public void initialize() {
    }
    
    public void stop() {
    }
    
    /**
     * Starts the <tt>ConnectionWatchdog</tt>.
     */
    public void start() {
        findDuds();
    }

    /** A snapshot of a connection. */
    private class ConnectionState {
        final long sentDropped;
        final long sent;
        final long received;
        private long bytesReceived;
        private long bytesSent;

        /** Takes a snapshot of the given connection. */
        ConnectionState(RoutedConnection c) {
            this.sentDropped=c.getConnectionMessageStatistics().getNumSentMessagesDropped();
            this.sent=c.getConnectionMessageStatistics().getNumMessagesSent();
            this.received=c.getConnectionMessageStatistics().getNumMessagesReceived();
            this.bytesReceived = c.getConnectionBandwidthStatistics().getBytesReceived();
            this.bytesSent = c.getConnectionBandwidthStatistics().getBytesSent();
        }

        /**
         * Returns true if the state of this connection has not
         * made sufficient progress since the old snapshot was taken.
         */
        boolean notProgressedSince(ConnectionState old) {
            //Current policy: returns true if (a) all packets sent since
            //snapshot were dropped or (b) we have received no data.
            long numSent=this.sent-old.sent;
            long numSentDropped=this.sentDropped-old.sentDropped;
            long numReceived=this.received-old.received;
            long numBytesReceived = this.bytesReceived - old.bytesReceived;
            long numBytesSent = this.bytesSent - old.bytesSent;
            
            if ((numSent==numSentDropped) && numSent!=0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(numBytesSent + " all sent messages dropped");
                }
                if (numBytesSent < ConnectionSettings.MIN_BYTES_SENT.getValue()) {
                    return true;
                } else {
                    ++oldKillCount;
                    return false;
                }
            } else if (numReceived==0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(numBytesReceived + " no messages received");
                }
                if (numBytesReceived < ConnectionSettings.MIN_BYTES_RECEIVED.getValue()) { 
                    return true;
                } else {
                    ++oldKillCount;
                    return false;
                }
            } else
                return false;
        }

        @Override
        public String toString() {
            return "{sent: "+sent+", sdropped: "+sentDropped+"}";
        }
    }

    /**
     * Schedules a snapshot of connection progress to be evaluated for duds.
     */
    private void findDuds() {
        //Take a snapshot of all connections, including leaves.
        Map<RoutedConnection, ConnectionState> snapshot = new HashMap<RoutedConnection, ConnectionState>();
        for(RoutedConnection c : allConnections()) {
            if (!c.isKillable())
				continue;
            snapshot.put(c, new ConnectionState(c));
        }
        
        backgroundExecutor.schedule(new DudChecker(snapshot, false), EVALUATE_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * Looks at a list of connections & pings them, waiting a certain amount of
     * time for a response.  If no messages are exchanged on the connection in
     * that time, the connection is killed.
     *
     * This is done by scheduling an event and checking the progress against
     * a snapshot.
     
     * @requires connections is a list of RoutedConnection
     * @modifies manager, router
     * @effects removes from manager any RoutedConnection's in "connections"
     *  that still aren't progressing after being pinged.
     */
    private void killIfStillDud(List<? extends RoutedConnection> connections) {
        //Take a snapshot of each connection, then send a ping.
        //The horizon statistics for the connection are temporarily disabled
        //during this process.  In the rare chance that legitimate pongs 
        //(other than in response to my ping), they will be ignored.
        Map<RoutedConnection, ConnectionState> snapshot = new HashMap<RoutedConnection, ConnectionState>();
        for(RoutedConnection c : connections) {
            if (!c.isKillable())
				continue;
            snapshot.put(c, new ConnectionState(c));
            PingRequest ping = pingRequestFactory.createPingRequest((byte)1);
            messageRouter.get().sendPingRequest(ping, c);
        }
        
        backgroundExecutor.schedule(new DudChecker(snapshot, true), REEVALUATE_TIME, TimeUnit.MILLISECONDS);
    }

    /** Returns an iterable of all initialized connections in this, including
     *  leaf connecions. */
    private Iterable<RoutedConnection> allConnections() {
        List<RoutedConnection> normal = connectionManager.get().getInitializedConnections();
        List<RoutedConnection> leaves = connectionManager.get().getInitializedClientConnections();

        List<RoutedConnection> buf = new ArrayList<RoutedConnection>(normal.size() + leaves.size());
        buf.addAll(normal);
        buf.addAll(leaves);
        return buf;
    }
    

    
    /**
     * Determines if snapshots of connections are duds.
     * If 'kill' is true, if they're a dud they're immediately clue.
     * Otherwise, duds are queued up for additional checking.
     * If no duds exist (or they were killed), findDuds() is started again.
     */
    private class DudChecker implements Runnable {
        private Map<RoutedConnection, ConnectionState> snapshots;
        private boolean kill;
        
        /**
         * Constructs a new DudChecker with the snapshots of ConnectionStates.
         * The checker may be used to kill the connections (if they haven't progressed)
         * or to re-evaluate them later.
         */
        DudChecker(Map<RoutedConnection, ConnectionState> snapshots, boolean kill) {
            this.snapshots = snapshots;
            this.kill = kill;
        }
        
        public void run() {
            //Loop through all connections, trying to find ones that
            //have not made sufficient progress. 
            List<RoutedConnection> potentials;
            if(kill)
                potentials = Collections.emptyList();
            else
                potentials = new ArrayList<RoutedConnection>();
            for(RoutedConnection c : allConnections()) {
                if (!c.isKillable())
    				continue;
                ConnectionState oldState = snapshots.get(c);
                if (oldState == null)
                    continue;  //this is a new connection
    
                ConnectionState currentState=new ConnectionState(c);
                if (currentState.notProgressedSince(oldState)) {
                    if(kill) {
                        if(ConnectionSettings.WATCHDOG_ACTIVE.getValue()) {
                            if(LOG.isWarnEnabled())
                                LOG.warn("Killing connection: " + c);
                            connectionServices.removeConnection(c);
                        }
                    } else {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Potential dud: " + c);
                        potentials.add(c);
                    }
                }
            }
            
            if(potentials.isEmpty())
                findDuds();
            else
                killIfStillDud(potentials);
        }
    }
}
