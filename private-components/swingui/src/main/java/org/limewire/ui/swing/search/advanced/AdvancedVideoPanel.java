package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;

/** The panel for an advanced video search. */
class AdvancedVideoPanel extends AdvancedPanel {

    public AdvancedVideoPanel(PropertyDictionary dictionary, FriendAutoCompleterFactory friendAutoCompleterFactory, Action enterKeyAction) {
        super(SearchCategory.VIDEO, friendAutoCompleterFactory, enterKeyAction);
        addField(FilePropertyKey.TITLE);
        addField(FilePropertyKey.GENRE, dictionary.getVideoGenres());
        addField(FilePropertyKey.YEAR);
        addField(FilePropertyKey.RATING, dictionary.getVideoRatings());
    }
}
