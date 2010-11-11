package com.limegroup.gnutella.dht.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.limewire.collection.MultiCollection;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;

/**
 * Publishes the localhost as an alternate 
 * locations for rare files. Rare files are files that haven't 
 * been uploaded for a certain amount of time. There are various
 * other ways to determinate the rareness of a file like using
 * the number of query hits instead of upload attempts or even
 * keeping track of the file activities over multiple sessions.
 */
@Singleton
public class AltLocModel implements StorableModel {
    
    private final Map<KUID, Storable> values 
        = Collections.synchronizedMap(new HashMap<KUID, Storable>());
    
    private final AltLocValueFactory altLocValueFactory;

    private final Provider<HashTreeCache> tigerTreeCache;

    private final FileView gnutellaFileView;
    
    @Inject
    public AltLocModel(AltLocValueFactory altLocValueFactory,
            @GnutellaFiles FileView gnutellaFileView, Provider<HashTreeCache> tigerTreeCache) {
        this.altLocValueFactory = altLocValueFactory;
        this.gnutellaFileView = gnutellaFileView;
        this.tigerTreeCache = tigerTreeCache;
    }
    
    public Collection<Storable> getStorables() {
        if (!DHTSettings.PUBLISH_ALT_LOCS.getValue()) {
            // Clear the mappings as they're no longer needed
            values.clear();
            return Collections.emptySet();
        }
        
        // List of Storables we're going to publish
        List<Storable> toRemove = new ArrayList<Storable>();
        List<Storable> toPublish = new ArrayList<Storable>();
        
        synchronized (values) {
            gnutellaFileView.getReadLock().lock();
            try {
                // Step One: Add every new FileDesc to the Map
                for(FileDesc fd : gnutellaFileView) {
                    URN urn = fd.getSHA1Urn();
                    KUID primaryKey = KUIDUtils.toKUID(urn);
                    if (!values.containsKey(primaryKey)) {
                        long fileSize = fd.getFileSize();
                        HashTree hashTree = tigerTreeCache.get().getHashTree(urn);
                        byte[] ttroot = null;
                        if (hashTree != null) {
                            ttroot = hashTree.getRootHashBytes();
                        }
                        
                        AltLocValue value = altLocValueFactory.createAltLocValueForSelf(fileSize, ttroot);
                        values.put(primaryKey, new Storable(primaryKey, value));
                    }
                }
            } finally {
                gnutellaFileView.getReadLock().unlock();
            }
            
            // Step Two: Remove every Storable that is no longer
            // associated with a FileDesc (i.e. the FileDesc was deleted)
            // and create a List of Storable that are rare and
            // must be republished
            for (Iterator<Storable> it = values.values().iterator(); it.hasNext(); ) {
                Storable storable = it.next();
                KUID primaryKey = storable.getPrimaryKey();
                URN urn = KUIDUtils.toURN(primaryKey);
                
                // For each URN check if the FileDesc still exists
                FileDesc fd = gnutellaFileView.getFileDesc(urn);
                
                // If it doesn't then remove it from the values map and
                // replace the entity value with the empty value
                // which will effectively remove the key-value mapping 
                // from the DHT.
                if (fd == null) {
                    storable = new Storable(primaryKey, DHTValue.EMPTY_VALUE);
                    it.remove();
                    
                    toRemove.add(storable);
                    
                // And if it does then check if it is rare and needs
                // publishing.
                } else if (fd.isRareFile() 
                        && DatabaseUtils.isPublishingRequired(storable)) {
                    toPublish.add(storable);
                }
            }
        }
        
        // Publish things always in a different order
        Collections.shuffle(toPublish);
        
        // Delete the things we want to remove from the DHT
        // first and continue with the things we want to publish
        return new MultiCollection<Storable>(toRemove, toPublish);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.StorableModel#handleStoreResult(org.limewire.mojito.db.Storable, org.limewire.mojito.result.StoreResult)
     */
    public void handleStoreResult(Storable storable, StoreResult result) {
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.StorableModel#handleContactChange()
     */
    public void handleContactChange() {
        
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("AltLocPublisher: ");
        synchronized (values) {
            buffer.append(CollectionUtils.toString(values.values()));
        }
        return buffer.toString();
    }
}
