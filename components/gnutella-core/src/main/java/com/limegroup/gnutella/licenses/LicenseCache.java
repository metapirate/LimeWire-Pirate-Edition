package com.limegroup.gnutella.licenses;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;

/**
 * A repository of licenses.
 */
@Singleton
public class LicenseCache {
    
    private static final Log LOG = LogFactory.getLog(LicenseCache.class);
    
    /**
     * The amount of time to keep a license in the cache.
     */
    private static final long EXPIRY_TIME = 7 * 24 * 60 * 60 * 1000; // one week.    
    
    /**
     * File where the licenses are serialized.
     */
    private final File CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "licenses.cache");        
    
    /**
     * A map of Licenses.  One License per URI.
     */
    private Map<URI, License> licenses;
    
    /**
     * An extra map of data that Licenses can use
     * to cache info.  This information lasts forever.
     */
    private Map<Object, Object> data;
    
    /**
     * Whether or not data is dirty since the last time we wrote to disk.
     */
    private boolean dirty = false;

    LicenseCache() {
        // TODO move this out of construction
        deserialize(); 
    }
    
    /**
     * Adds a verified license.
     */
    synchronized void addVerifiedLicense(License license) {
        licenses.put(license.getLicenseURI(), license);
        dirty = true;
    }
    
    /**
     * Adds data.
     */
    synchronized void addData(Object key, Object value) {
        data.put(key, value);
        dirty = true;
    }
    
    /**
     * Retrieves the cached license for the specified URI, substituting
     * the license string for a new one.
     */
    synchronized License getLicense(String licenseString, URI licenseURI) {
        License license = licenses.get(licenseURI);
        if(license != null)
            return license.copy(licenseString, licenseURI);
        else
             return null;
    }
    
    /**
     * Gets details.
     */
    synchronized Object getData(Object key) {
        return data.get(key);
    } 
    
    /**
     * Determines if the license is verified for the given URN and URI.
     */
    synchronized boolean isVerifiedAndValid(URN urn, URI uri) {
        License license = licenses.get(uri);
        return license != null && license.isValid(urn);
    }
    
   /**
     * Loads values from cache file, if available
     */
    private void deserialize() {
        ObjectInputStream ois = null;
        try {
            if (!CACHE_FILE.exists()) {
                // finally-block will initialize maps
                return;
            }
            ois = new ObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(CACHE_FILE)));
            Object o = ois.readObject();
            if(o != null) 
                licenses = GenericsUtils.scanForMap(o, URI.class, License.class, GenericsUtils.ScanMode.REMOVE);
            o = ois.readObject();
            if(o != null)
                data = GenericsUtils.scanForMap(o, Object.class, Object.class, GenericsUtils.ScanMode.REMOVE);
            removeOldEntries();
        } catch(Throwable t) {
            LOG.error("Can't read licenses", t);
        } finally {
            IOUtils.close(ois);
            
            if(licenses == null)
                licenses = new HashMap<URI, License>();
            if(data == null)
                data = new HashMap<Object, Object>();
        }
    }
    
   /**
     * Removes any stale entries from the map so that they will automatically
     * be replaced.
     */
    private void removeOldEntries() {
        long cutoff = System.currentTimeMillis() - EXPIRY_TIME;
        
        // discard outdated info
        for(Iterator<License> i = licenses.values().iterator(); i.hasNext(); ) {
            License license = i.next();
            if(license.getLastVerifiedTime() < cutoff) {
                dirty = true;
                i.remove();
            }
        }
    }

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        if(!dirty)
            return;
        
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(CACHE_FILE)));
            oos.writeObject(licenses);
            oos.writeObject(data);
            oos.flush();
        } catch (IOException e) {
            ErrorService.error(e);
        } finally {
            IOUtils.close(oos);
        }
        
        dirty = false;
    }
}