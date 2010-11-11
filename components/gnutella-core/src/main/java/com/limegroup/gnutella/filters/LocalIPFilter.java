package com.limegroup.gnutella.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IOUtils;
import org.limewire.io.IP;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Blocks messages and hosts based on IP address.  
 */
@EagerSingleton
public final class LocalIPFilter extends AbstractIPFilter {
    
    private static final Log LOG = LogFactory.getLog(LocalIPFilter.class);
    
    private volatile IPList badHosts;
    private volatile IPList goodHosts;
    /** List contained in hostiles.txt if any.  Loaded on startup only */ 
    private final IPList hostilesTXTHosts = new IPList();
    
    private final IPFilter hostileNetworkFilter;
    private final ScheduledExecutorService ipLoader;
    /** Marker for whether or not hostiles need to be loaded. */
    private volatile boolean shouldLoadHostiles;
    
    private volatile long whitelistings; // # of times we whitelisted an ip 
    private volatile long blacklistings; // # of times we blacklisted an ip 
    private volatile long netblockings;  // # of times net blacklisted an ip 
    private volatile long implicitings;  // # of times an ip was implicitly allowed
    
    /** Constructs an IPFilter that automatically loads the content. */
    @Inject
    public LocalIPFilter(@Named("hostileFilter") IPFilter hostileNetworkFilter, 
            @Named("backgroundExecutor") ScheduledExecutorService ipLoader) {
        this.hostileNetworkFilter = hostileNetworkFilter;
        this.ipLoader = ipLoader;
        
        File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
        shouldLoadHostiles = hostiles.exists();
        
        refreshHosts();
    }
    
    @Override
    public void refreshHosts() {
        refreshHosts(null);
    }
    
    @Override
    public void refreshHosts(final LoadCallback callback) {
        Runnable load = new Runnable() {
            public void run() {
                hostileNetworkFilter.refreshHosts();
                refreshHostsImpl();
                if (callback != null)
                    callback.spamFilterLoaded();
            }
        };
        if (!shouldLoadHostiles) 
            load.run();
        else 
            ipLoader.execute(load);
    }
    
    /** Does the work of setting new good  & bad hosts. */
    private void refreshHostsImpl() {
        LOG.debug("Refreshing local IP filter");
        
        // Load the local blacklist, stripping out invalid entries
        IPList newBad = new IPList();
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.get();
        ArrayList<String> valid = new ArrayList<String>(allHosts.length);
        for (int i=0; i<allHosts.length; i++) {
            if(newBad.add(allHosts[i]))
                valid.add(allHosts[i]);
        }
        if(valid.size() != allHosts.length) {
            allHosts = valid.toArray(new String[0]);
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(allHosts);
        }
        
        // Load the local whitelist, stripping out invalid entries
        IPList newGood = new IPList();
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.get();
        valid = new ArrayList<String>(allHosts.length);
        for (int i=0; i<allHosts.length; i++) {
            if(newGood.add(allHosts[i]))
                valid.add(allHosts[i]);
        }
        if(valid.size() != allHosts.length) {
            allHosts = valid.toArray(new String[0]);
            FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(allHosts);
        }

        // Load data from hostiles.txt (if it wasn't already loaded!)...
        if(shouldLoadHostiles) {
            shouldLoadHostiles = false;
            
            LOG.debug("Loading hostiles.txt");
            File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(hostiles));
                String read = null;
                while( (read = reader.readLine()) != null) {
                    hostilesTXTHosts.add(read);
                }
            } catch(IOException e) {
                LOG.debug("Error loading hostiles.txt", e);
            } finally {
                IOUtils.close(reader);
            }
        }
        
        badHosts = new MultiIPList(newBad, hostilesTXTHosts);
        goodHosts = newGood;
    }
    
    /** Determines if any blacklisted hosts exist. */
    @Override
    public boolean hasBlacklistedHosts() {
        return 
          (FilterSettings.USE_NETWORK_FILTER.getValue() && hostileNetworkFilter.hasBlacklistedHosts())
          || !badHosts.isEmpty();
    }
    
    @Override
    protected boolean allowImpl(IP ip) {
        if (goodHosts.contains(ip)) {
            whitelistings++;
            return true;
        }

        if (badHosts.contains(ip)) {
            blacklistings++;
            return false;
        }

        if (FilterSettings.USE_NETWORK_FILTER.getValue() && !hostileNetworkFilter.allow(ip)) {
            netblockings++;
            return false;
        }

        implicitings++;
        return true;
    }
}