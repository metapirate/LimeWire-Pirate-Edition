package org.limewire.ui.swing.search.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.PropertiableFileUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;
import org.limewire.util.Objects;

import com.google.inject.Provider;

/**
 * An implementation of VisualSearchResult for displaying actual search 
 * results. 
 */
class SearchResultAdapter implements VisualSearchResult, Comparable {

    private static final Log LOG = LogFactory.getLog(SearchResultAdapter.class);
    
    private static final Matcher FIND_HTML_MARKUP; 
    static {
        Pattern p = Pattern.compile("[<][/]?[\\w =\"\\./:#\\-\\!\\&\\?]*[>]");
        FIND_HTML_MARKUP = p.matcher("");
    }
    
    private final GroupedSearchResult groupedSearchResult;
    
    private final Provider<PropertiableHeadings> propertiableHeadings;
    private BasicDownloadState downloadState = BasicDownloadState.NOT_STARTED;
    private CopyOnWriteArrayList<VisualSearchResult> similarResults = null;
    private VisualSearchResult similarityParent;
    private boolean visible;
    private boolean childrenVisible;    
    private Boolean spamCache;    
    private boolean preExistingDownload = false;    
    private String cachedHeading;    
    private String cachedSubHeading;
    
    private final VisualSearchResultStatusListener changeListener;

    /**
     * Constructs a SearchResultAdapter with the specified grouped core results.
     */
    public SearchResultAdapter(GroupedSearchResult groupedSearchResult,
            Provider<PropertiableHeadings> propertiableHeadings,
            VisualSearchResultStatusListener changeListener) {
        this.groupedSearchResult = groupedSearchResult;
        this.propertiableHeadings = propertiableHeadings;
        this.changeListener = changeListener;
        
        this.visible = true;
        this.childrenVisible = false;
    }

    @Override
    public boolean isAnonymous() {
        return groupedSearchResult.isAnonymous();
    }
    
    @Override
    public Category getCategory() {
        return getCoreSearchResults().get(0).getCategory();
    }

    @Override
    public List<SearchResult> getCoreSearchResults() {
        return groupedSearchResult.getSearchResults();
    }

    @Override
    public String getFileExtension() {
        return getCoreSearchResults().get(0).getFileExtension();
    }
    
    @Override
    public String getFileName() {
        return groupedSearchResult.getFileName();
    }
    
    @Override
    public Collection<Friend> getFriends() {
        return groupedSearchResult.getFriends();
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        // find the first non-null value in any of the search results.
        for (SearchResult result : getCoreSearchResults()) {
            Object value = result.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getNameProperty(boolean useAudioArtist) {
        return PropertiableFileUtils.getNameProperty(this, useAudioArtist);
    }
    
    @Override
    public void addSimilarSearchResult(VisualSearchResult similarResult) {
        assert similarResult != this;
        if(similarResults == null) {
            similarResults = new CopyOnWriteArrayList<VisualSearchResult>();
        }
        similarResults.addIfAbsent(similarResult);
    }

    @Override
    public void removeSimilarSearchResult(VisualSearchResult result) {
        if(similarResults != null) {
            similarResults.remove(result);
        }
    }

    @Override
    public List<VisualSearchResult> getSimilarResults() {
        return similarResults == null ? Collections.<VisualSearchResult>emptyList() : similarResults;
    }

    @Override
    public void setSimilarityParent(VisualSearchResult parent) {
        VisualSearchResult oldParent = this.similarityParent;
        this.similarityParent = parent;
        firePropertyChange(SIMILARITY_PARENT, oldParent, parent);
    }

    @Override
    public VisualSearchResult getSimilarityParent() {
        return similarityParent;
    }

    @Override
    public long getSize() {
        return getCoreSearchResults().get(0).getSize();
    }

    @Override
    public Collection<RemoteHost> getSources() {
        return groupedSearchResult.getSources();
    }

    @Override
    public BasicDownloadState getDownloadState() {
        return downloadState;
    }

    @Override
    public void setDownloadState(BasicDownloadState downloadState) {
        // If the download was aborted, recalculate the spam score
        if(downloadState == BasicDownloadState.NOT_STARTED) {
            boolean oldSpam = isSpam();
            spamCache = null;
            boolean newSpam = isSpam();
            firePropertyChange("spam-core", oldSpam, newSpam);
        }
        BasicDownloadState oldDownloadState = this.downloadState;
        this.downloadState = downloadState;
        firePropertyChange("downloadState", oldDownloadState, downloadState);
    }

    @Override
    public String toString() {
        return getCoreSearchResults().toString();
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        boolean oldValue = this.visible;
        this.visible = visible;
        firePropertyChange("visible", oldValue, visible);
        if(LOG.isDebugEnabled()) {
            LOG.debugf("Updating visible to {0} for urn: {1}", visible, getUrn());
        }
    }

    @Override
    public boolean isChildrenVisible() {
        return childrenVisible;
    }

    @Override
    public boolean isSpam() {
        if (spamCache == null) {
            boolean spam = false;
            for (SearchResult result : getCoreSearchResults())
                spam |= result.isSpam();
            spamCache = spam;
        }
        return spamCache.booleanValue();
    }

    @Override
    public void setSpam(boolean spam) {
        boolean oldSpam = isSpam();
        spamCache = spam;
        firePropertyChange("spam-ui", oldSpam, spam);
    }
    
    @Override
    public void setChildrenVisible(boolean childrenVisible) {
        boolean oldChildrenVisible = childrenVisible;
        this.childrenVisible = childrenVisible;
        for (VisualSearchResult similarResult : getSimilarResults()) {
            similarResult.setVisible(childrenVisible);
            similarResult.setChildrenVisible(false);
        }
        firePropertyChange("childrenVisible", oldChildrenVisible, childrenVisible);
    }

    @Override
    public void toggleChildrenVisibility() {
        setChildrenVisible(!isShowingSimilarResults());
    }

    private boolean isShowingSimilarResults() {
        return getSimilarResults().size() > 0 && isChildrenVisible();
    }

    @Override
    public URN getUrn() {
        return groupedSearchResult.getUrn();
    }

    @Override
    public URN getNavSelectionId() {
        return getUrn();
    }

    @Override
    public String getMagnetLink() {
        //TODO: add other search results as alternative results to the magnet.
        // A bit too intensive for the release.
        if(getCoreSearchResults().size() > 0)
            return getCoreSearchResults().get(0).getMagnetURL();
        else
            return null;
    }

    @Override
    public String getHeading() {
        if (cachedHeading == null) {
            cachedHeading =  sanitize(propertiableHeadings.get().getHeading(this));
        }
        return cachedHeading;
    }
    
    /**
     * This method checks for HTML encoding in the content of the supplied string.
     * If found, the encoding is stripped from the string and this result is marked as
     * spam.  
     * @param input
     * @return The stripped string is returned (or the same string if no HTML encoding
     * is found).
     */
    private String sanitize(String input) {
        Matcher matcher = FIND_HTML_MARKUP.reset(input);
        if (matcher.find()) {
            setSpam(true);
            return matcher.replaceAll("");
        }
        return input;
    }

    @Override
    public String getSubHeading() {
        if (cachedSubHeading == null) {
            cachedSubHeading = sanitize(propertiableHeadings.get().getSubHeading(this));
        }
        return cachedSubHeading;
    }

    @Override
    public float getRelevance() {
        return groupedSearchResult.getRelevance();
    }

    /**
     * If any of the search results' lime xml docs contains a license string
     * the entire VisualSearchResult is considered "licensed".
     *
     */
    @Override
    public boolean isLicensed() {
        for (SearchResult searchResult : getCoreSearchResults()) {
             if (searchResult.isLicensed()) {
                return true;
             }
        }
        return false;
    }

    @Override
    public boolean isPreExistingDownload() {
        return preExistingDownload;
    }

    @Override
    public void setPreExistingDownload(boolean preExistingDownload) {
        this.preExistingDownload = preExistingDownload;
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof SearchResultAdapter)) 
            return -1;
        
        SearchResultAdapter sra = (SearchResultAdapter) o;
        return getHeading().compareTo(sra.getHeading());
    }

    private void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            changeListener.resultChanged(this, propertyName, oldValue, newValue);
        }
    }

    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (!Objects.equalOrNull(oldValue, newValue)) {
            changeListener.resultChanged(this, propertyName, oldValue, newValue);
        }
    }
}
