package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;

import com.google.inject.Inject;

/**
 * Factory for creating advanced search panels.
 */
class AdvancedPanelFactory {

    private final PropertyDictionary propertyDictionary;
    private final FriendAutoCompleterFactory friendAutoCompleterFactory;
    
    @Inject
    public AdvancedPanelFactory(PropertyDictionary propertyDictionary,
            FriendAutoCompleterFactory friendAutoCompleterFactory) {
        this.propertyDictionary = propertyDictionary;
        this.friendAutoCompleterFactory = friendAutoCompleterFactory;
    }
    
    /**
     * Creates a new AdvancedPanel for the specified search category.
     */
    public AdvancedPanel create(SearchCategory searchCategory, Action enterKeyAction) {
        switch (searchCategory) {
        case AUDIO:
            return new AdvancedAudioPanel(propertyDictionary, friendAutoCompleterFactory, enterKeyAction);
            
        case VIDEO:
            return new AdvancedVideoPanel(propertyDictionary, friendAutoCompleterFactory, enterKeyAction);
            
        default:
            throw new IllegalArgumentException("Search category not supported");
        }
    }
}
