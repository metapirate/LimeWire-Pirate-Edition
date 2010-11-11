package org.limewire.core.impl.download;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;

import com.google.inject.Singleton;

/**
 * Registry class for all concrete download item factories.
 * <p>
 * A concrete factory should register itself and is then invoked by this class
 * if a list of search results is being downloaded. 
 */
@Singleton
public class DownloadItemFactoryRegistry implements DownloadItemFactory {
     
    /**
     * Map factories by class to avoid duplicate factories from being registered. 
     */
    private final Map<Class<? extends DownloadItemFactory>, DownloadItemFactory> factories = new ConcurrentHashMap<Class<? extends DownloadItemFactory>, DownloadItemFactory>(); 

    @Override
    public DownloadItem create(Search search, List<? extends SearchResult> searchResults,
            File saveFile, boolean overwrite) throws DownloadException {
        for (DownloadItemFactory factory : factories.values()) {
            DownloadItem coreDownloadItem = factory.create(search, searchResults, saveFile, overwrite);
            if (coreDownloadItem != null) {
                return coreDownloadItem;
            }
        }
        return null;
    }
    
    /**
     * Registers factory with this composite factory registry.
     */
    public void register(DownloadItemFactory factory) {
        factories.put(factory.getClass(), factory);
    }

}
