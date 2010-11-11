package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.net.SocketsManager;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.UploadServices;

/**
 * Specialized class that attempts to connect to a rotating list of well-known
 * Internet addresses to check whether or not this host has a live connection to
 * the Internet.
 */
public class ConnectionChecker {

    private static final Log LOG = LogFactory.getLog(ConnectionChecker.class);

    /**
     * Flag for whether or not we know for sure that we're connected from
     * successfully connecting to an external host.
     */
    private volatile boolean connected;

    /**
     * Variable for the number of unsuccessful connection attempts.
     */
    private int unsuccessfulAttempts;

    /**
     * Whether we have tried to work around SP2 cutting us off.
     */
    private boolean triedSP2Workaround;

    private final ConnectionServices connectionServices;

    private final UploadServices uploadServices;

    private final SocketsManager socketsManager;

    private final AtomicInteger numWorkarounds;

    private final UDPConnectionChecker udpConnectionChecker;

    private final DownloadServices downloadServices;

    private final List<String> hostList;

    public ConnectionChecker(AtomicInteger numWorkarounds, String[] hosts,
            ConnectionServices connectionServices, UploadServices uploadServices,
            DownloadServices downloadServices, SocketsManager socketsManager,
            UDPConnectionChecker udpConnectionChecker) {
        this.numWorkarounds = numWorkarounds;
        this.hostList = new ArrayList<String>(Arrays.asList(hosts));
        this.connectionServices = connectionServices;
        this.uploadServices = uploadServices;
        this.downloadServices = downloadServices;
        this.socketsManager = socketsManager;
        this.udpConnectionChecker = udpConnectionChecker;
    }

    /**
     * Checks for a live Internet connection.
     */
    public synchronized void run(ConnectionCheckerListener connectionCheckerListener) {
        // Add some randomization.
        Collections.shuffle(hostList);

        for (String curHost : hostList) {
            connectToHost(curHost);

            // Break out of the loop if we've already discovered that we're
            // connected -- we only need to successfully connect to one host
            // to know for sure that we're up.
            if (connected) {
                LOG.debug("Connection exists.");

                // if we did disconnect as an attempt to work around SP2,
                // connect now.
                if (triedSP2Workaround && !connectionServices.isConnected()
                        && !connectionServices.isConnecting()) {
                    LOG.debug("Reconnecting RouterService");
                    connectionServices.connect();
                }

                connectionCheckerListener.connected();
                return;
            }

            // Stop if we've failed to connect to more than 2 of the hosts
            // that should be up all of the time. We do this to make extra
            // sure the user's connection is down. If it is down, trying
            // multiple times adds no load to the test servers.
            if (unsuccessfulAttempts > 2) {
                LOG.debug("Failed connection check more than twice.");
                if (triedSP2Workaround || !shouldTrySP2Workaround()) {
                    break;
                } else if (hasNoTransfers() && udpConnectionChecker.udpIsDead()) {
                    // really disconnected
                    break;
                } else {
                    // otherwise shut off all attempts until sp2's limit times
                    // out
                    triedSP2Workaround = true;
                    trySP2Workaround();
                }
            }
        }

        connectionCheckerListener.noInternetConnection();
    }

    /**
     * For testing.
     */
    boolean shouldTrySP2Workaround() {
        return OSUtils.isSocketChallengedWindows();
    }

    /**
     * Terminates all attempts to open new sockets.
     */
    void trySP2Workaround() {
        connectionServices.disconnect();
        numWorkarounds.incrementAndGet();
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException ignored) {
        }
        unsuccessfulAttempts = 0;
    }

    /**
     * @return true if we don't have any transfers going at non-zero speed
     */
    private boolean hasNoTransfers() {
        return !downloadServices.hasActiveDownloads() && !uploadServices.hasActiveUploads();
    }

    /**
     * Determines whether or not we have connected to an external host,
     * verifying that we have an Internet connection.
     * 
     * @return <tt>true</tt> if we have created a successful connection,
     *         otherwise <tt>false</tt>
     */
    public boolean hasConnected() {
        return connected;
    }

    /**
     * Connects to an individual host.
     * 
     * @param host the host to connect to
     */
    private void connectToHost(String host) {
        if (LOG.isDebugEnabled())
            LOG.debug("Checking for connection with host: " + host);

        try {
            InetAddress.getByName(host); // die fast if unresolvable
            Observer observer = new Observer();
            synchronized (observer) {
                Socket s = socketsManager.connect(new InetSocketAddress(host, 80), 6000, observer);
                LOG.debug("Waiting for callback...");
                try {
                    observer.wait(12000);
                } catch (InterruptedException e) {
                }
                if (!observer.hasResponse()) {
                    LOG.debug("No response!");
                    // only consider unsuccessful if we were able to remove it
                    // 'cause if it couldn't be removed, a response is still
                    // pending...
                    if (socketsManager.removeConnectObserver(observer)) {
                        LOG.debug("Removed observer");
                        unsuccessfulAttempts++;
                        IOUtils.close(s);
                    }
                }
            }
        } catch (IOException bad) {
            LOG.debug("failed to resolve name", bad);
            unsuccessfulAttempts++;
        }
    }

    private class Observer implements ConnectObserver {
        boolean response = false;

        // unused.
        public void handleIOException(IOException iox) {
        }

        // Yay, we're connected.
        public synchronized void handleConnect(Socket socket) throws IOException {
            if (!response) {
                LOG.debug("Socket connected OK");

                response = true;
                connected = true;
                notify();

                IOUtils.close(socket);
            }
        }

        public synchronized void shutdown() {
            if (!response) {
                LOG.debug("Socket failed to connect");

                response = true;
                unsuccessfulAttempts++;
                notify();
            }
        }

        public boolean hasResponse() {
            return response;
        }

    }

}
