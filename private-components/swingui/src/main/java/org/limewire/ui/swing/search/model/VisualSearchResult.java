package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.List;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.filter.FilterableItem;
import org.limewire.ui.swing.nav.NavSelectable;

/**
 * Defines an interface for a displayed search result.  The displayed result
 * may be supported by multiple sources, and may also contain an aggregation 
 * of similar results.   
 */
public interface VisualSearchResult extends NavSelectable, PropertiableFile, FilterableItem {

    /** Name for new sources property change event. */
    public static final String NEW_SOURCES = "newSources";
    
    /** Name for similarity parent property change event. */
    public static final String SIMILARITY_PARENT = "similarityParent";
    
    /**
     * Returns a list of core SearchResult values associated with this visual 
     * result.
     */
    List<SearchResult> getCoreSearchResults();

    /**
     * Returns the download state for the search result. 
     */
    BasicDownloadState getDownloadState();
    
    /**
     * Sets the download state for the search result. 
     */
    void setDownloadState(BasicDownloadState downloadState);

    /**
     * Returns the file extension for the search result.
     */
    String getFileExtension();

    /**
     * Returns the value associated with the specified FilePropertyKey as a
     * text string.
     */
    String getPropertyString(FilePropertyKey key);

    /**
     * Returns the name property as a text string.  For audio files with a 
     * non-blank title, the title property is returned, prefixed by the artist
     * if <code>useAudioArtist</code> is true.
     */
    String getNameProperty(boolean useAudioArtist);
    
    /**
     * Returns a Collection of sources that support the search result.  Each
     * source is represented by a RemoteHost object. 
     */
    Collection<RemoteHost> getSources();
    
    /**
     * Returns a list of similar search results.
     */
    List<VisualSearchResult> getSimilarResults();

    /**
     * Returns the size of the search result in bytes.
     */
    long getSize();
    
    /**
     * Returns an indicator that determines if the result is visible.
     */
    boolean isVisible();

    /**
     * Sets an indicator that determines if the result is visible.
     */
    void setVisible(boolean visible);

    /**
     * Returns an indicator that determines if similar results are visible.
     */
    boolean isChildrenVisible();
    
    /**
     * Sets an indicator that determines if similar results are visible.
     */
    void setChildrenVisible(boolean childrenVisible);
    
    /**
     * Toggles the indicator that determines if similar results are visible.
     */
    void toggleChildrenVisibility();
    
    /**
     * Returns an indicator that determines if the result is spam.
     */
    boolean isSpam();
    
    /**
     * Sets an indicator that determines if the result is spam.
     */
    void setSpam(boolean spam);

    /**
     * Adds the specified search result to the list of similar results.
     */
    public void addSimilarSearchResult(VisualSearchResult similarResult);

    /**
     * Removes the specified search result from the list of similar results.
     */
    public void removeSimilarSearchResult(VisualSearchResult result);

    /**
     * Returns the parent result that this result is similar to.  May be null.
     */
    VisualSearchResult getSimilarityParent();

    /**
     * Sets the specified result as the parent that this result is similar to.
     */
    public void setSimilarityParent(VisualSearchResult parent);

    /**
     * Returns the magnet link associated with the search result.
     */
    public String getMagnetLink();

    /**
     * Returns the main heading for the search result.
     */
    String getHeading();

    /**
     * Returns the sub-heading for the search result.
     */
    String getSubHeading();

    /**
     * Returns the relevance value of the search result.  
     */
    float getRelevance();
    
    /**
     * Returns an indicator that determines if the result is an existing 
     * download.
     */
    boolean isPreExistingDownload();
    
    /**
     * Sets an indicator that determines if the result is an existing 
     * download.
     */
    void setPreExistingDownload(boolean preExistingDownload);

    /**
     * Returns true if the associated file contains a license.
     */
    boolean isLicensed();

}
