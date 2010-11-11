package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.ByteReader;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Constants;

@Singleton
public class ExternalControl {

    private static final Log LOG = LogFactory.getLog(ExternalControl.class);

    private boolean initialized = false;

    private volatile String enqueuedRequest = null;

    private final Provider<ActivityCallback> activityCallback;

    @Inject
    public ExternalControl(Provider<ActivityCallback> activityCallback) {
        this.activityCallback = activityCallback;
    }

    public static String preprocessArgs(String args[]) {
        StringBuilder arg = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            arg.append(args[i]);
        }
        return arg.toString();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void enqueueControlRequest(String arg) {
        enqueuedRequest = arg;
    }

    public void runQueuedControlRequest() {
        initialized = true;
        if (enqueuedRequest != null) {
            String request = enqueuedRequest;
            enqueuedRequest = null;
            if (isTorrentRequest(request))
                handleTorrentRequest(request);
            else
                handleMagnetRequest(request);
        }
    }

    /**
     * @return true if this is a torrent request.
     */
    public static boolean isTorrentRequest(String arg) {
        if (arg == null)
            return false;
        arg = arg.trim().toLowerCase(Locale.US);
        // magnets pointing to .torrent files are just magnets for now
        return arg.endsWith(".torrent") && !arg.startsWith("magnet:");
    }

    // refactored the download logic into a separate method
    public void handleMagnetRequest(String arg) {
        LOG.trace("enter handleMagnetRequest");

        ActivityCallback callback = restoreApplication();
        MagnetOptions options[] = MagnetOptions.parseMagnet(arg);

        if (options.length == 0) {
            if (LOG.isWarnEnabled())
                LOG.warn("Invalid magnet, ignoring: " + arg);
            return;
        }

        callback.handleMagnets(options);
    }

    private ActivityCallback restoreApplication() {
        activityCallback.get().restoreApplication();
        return activityCallback.get();
    }

    private void handleTorrentRequest(String arg) {
        LOG.trace("enter handleTorrentRequest");
        ActivityCallback callback = restoreApplication();
        File torrentFile = new File(arg.trim());
        callback.handleTorrent(torrentFile);
    }

    /**
     * Handle a Magnet request via a socket (for TCP handling). Deiconify the
     * application, fire MAGNET request and return true as a sign that LimeWire
     * is running.
     */
    public void fireControlThread(Socket socket, boolean magnet) {
        LOG.trace("enter fireControl");

        Thread.currentThread().setName("IncomingControlThread");
        try {
            // Only allow control from localhost
            if (!NetworkUtils.isLocalHost(socket)) {
                if (LOG.isWarnEnabled())
                    LOG.warn("Invalid control request from: "
                            + socket.getInetAddress().getHostAddress());
                return;
            }
            if(LOG.isInfoEnabled())
                LOG.info("Request on port " + socket.getLocalPort());

            // First read extra parameter
            socket.setSoTimeout(Constants.TIMEOUT);
            ByteReader br = new ByteReader(socket.getInputStream());
            // read the first line. if null, throw an exception
            String line = br.readLine();
            socket.setSoTimeout(0);

            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            String s = CommonUtils.getUserName() + "\r\n";
            // system internal, so use system encoding
            byte[] bytes = s.getBytes();
            out.write(bytes);
            out.flush();
            if (magnet)
                handleMagnetRequest(line);
            else
                handleTorrentRequest(line);
        } catch (IOException e) {
            LOG.warn("Exception while responding to control request", e);
        } finally {
            IOUtils.close(socket);
        }
    }
}
