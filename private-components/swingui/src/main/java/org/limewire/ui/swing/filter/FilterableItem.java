package org.limewire.ui.swing.filter;

import java.util.Collection;

import org.limewire.core.api.library.PropertiableFile;
import org.limewire.friend.api.Friend;

/**
 * Defines an item that can be filtered.  Known implementations include
 * {@link org.limewire.ui.swing.search.model.VisualSearchResult VisualSearchResult}.
 */
public interface FilterableItem extends PropertiableFile {

    /**
     * Returns an indicator that determines if the item is from an anonymous
     * source.
     */
    boolean isAnonymous();
    
    /**
     * Returns the file extension for the item.
     */
    String getFileExtension();
    
    /**
     * Returns a Collection of friends that are sources for the item.
     */
    Collection<Friend> getFriends();
    
    /**
     * Returns the size of the item in bytes.
     */
    long getSize();
    
    /**
     * Returns an indicator that determines if the result is spam.
     */
    boolean isSpam();
    
}
