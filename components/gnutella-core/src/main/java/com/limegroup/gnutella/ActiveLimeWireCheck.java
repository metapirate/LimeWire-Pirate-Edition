package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.FileLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.ByteReader;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.browser.ExternalControl;

/**
 * Allows one to check if a LimeWire is already running under this user's name.
 */
public class ActiveLimeWireCheck {

    private static final Log LOG = LogFactory.getLog(ActiveLimeWireCheck.class);

    // Old-fashioned singleton because it's used before the injector is created
    private static final ActiveLimeWireCheck instance = new ActiveLimeWireCheck();

    public static ActiveLimeWireCheck instance() {
        return instance;
    }

    /**
     * Checks if the client is already running, and if so, tries to pass it the
     * arguments, which may be null. Arguments are not passed on OSX.
     * 
     * @return true if another instance is running.
     * @throws ActiveLimeWireException if another instance appears to be
     * running but it can't be contacted.
     */
    public boolean checkForActiveLimeWire(String[] args)
    throws ActiveLimeWireException {
        // Only pass through args on windows/linux
        if(!OSUtils.isWindows() && !OSUtils.isLinux())
            return testForLimeWire(null);
        if(args == null || args.length == 0)
            return testForLimeWire(null);
        return testForLimeWire(ExternalControl.preprocessArgs(args));
    }

    /**
     * Checks if the client is already running, and if so, tries to pass it the
     * argument, which may be null.
     * 
     * @param handoffLink a string representing the torrent/magnet link to hand
     * off to an existing LW instance if necessary.
     * @return true if another instance is running, in which case this instance
     * must quit.
     * @throws ActiveLimeWireException if another instance appears to be
     * running but it can't be contacted.
     */
    private boolean testForLimeWire(String handoffLink) throws ActiveLimeWireException {
        File file = new File(CommonUtils.getUserSettingsDir(), "lock");
        FileLock lock;
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 5 * 60 * 1000) {
            // Try to lock a file in the settings dir; if the lock can't be
            // acquired, another instance of LW is running.
            try {
                file.getParentFile().mkdirs(); // Ensure the settings dir exists
                RandomAccessFile r = new RandomAccessFile(file, "rw");
                lock = r.getChannel().tryLock(); // Null if we can't get the lock
            } catch(IOException e) {
                // Couldn't access the file - something's badly wrong, so quit.
                LOG.error("Failed to access lock file", e);
                return true;
            }
            if(lock != null) {
                LOG.trace("Acquired lock");
                // Don't do this without acquiring the lock first or we could
                // delete a file locked by another instance, effectively
                // removing its lock.
                file.deleteOnExit();
                return false;
            }
            // Another instance is running, but it may not be listening on the
            // port yet; try for FIVE WHOLE MINUTES to contact it (on a
            // heavily loaded machine it can take that long for an instance to
            // start accepting connections after acquiring the lock). 
            LOG.trace("Could not acquire lock");
            if(tryToContactRunningLimeWire(handoffLink)) {
                LOG.trace("Contacted existing instance");
                return true;
            }
            try {
                Thread.sleep(2000);
            } catch(InterruptedException ignored) {}
        }
        LOG.trace("Could not contact existing instance");
        throw new ActiveLimeWireException();
    }

    /**
     * Tries to contact an existing instance of LimeWire on the configured port
     * and pass it the argument (to hand off a torrent or magnet link), which may be null.
     * 
     * @param handoffLink the argument to pass to the running instance, if contacted.
     * @return true if another instance was successfully contacted on the port.
     */
    private boolean tryToContactRunningLimeWire(String handoffLink) {
        Socket socket = null;
        boolean contacted = false;
        int port = NetworkSettings.PORT.getValue();
        if(!NetworkUtils.isValidPort(port)) {
            LOG.trace("Invalid port");
            return false;
        }
        String type = ExternalControl.isTorrentRequest(handoffLink) ? "TORRENT" : "MAGNET";
        try {
            if(LOG.isTraceEnabled())
                LOG.trace("Connecting to port " + port);
            socket = new Socket();
            // Give LW a while to respond -- it might be busy.
            // In the case where no one is even listening on the port,
            // this will fail with a ConnectException really fast.
            socket.connect(new InetSocketAddress("127.0.0.1", port), 10000);
            LOG.trace("Connected");
            InputStream istream = socket.getInputStream(); 
            socket.setSoTimeout(10000); 
            ByteReader byteReader = new ByteReader(istream);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter out = new BufferedWriter(osw);
            out.write(type + " " + handoffLink + " ");
            out.write("\r\n");
            out.flush();
            String str = byteReader.readLine();
            if(str == null) {
                LOG.trace("Null response");
                contacted = false;
            }
            else if(!str.startsWith(CommonUtils.getUserName())) {
                LOG.trace("Invalid response");
                contacted = false;
            } else {
                LOG.trace("Valid response");
                contacted = true;
            }
        } catch (IOException e) {
            LOG.debug("Failed to contact existing instance", e);
        } finally {
            LOG.trace("Closing socket");
            IOUtils.close(socket);
        }
        return contacted;
    }

    public static class ActiveLimeWireException extends Exception {}
}
