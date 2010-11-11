package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced audio search. */
class AdvancedAudioPanel extends AdvancedPanel {

    public AdvancedAudioPanel(PropertyDictionary propertyDictionary, FriendAutoCompleterFactory friendAutoCompleterFactory, Action enterKeyAction) {
        super(SearchCategory.AUDIO, friendAutoCompleterFactory, enterKeyAction);
        addField(FilePropertyKey.TITLE);
        addField(FilePropertyKey.AUTHOR);
        addField(FilePropertyKey.ALBUM);
        addField(FilePropertyKey.GENRE, propertyDictionary.getAudioGenres());
        addField(FilePropertyKey.TRACK_NUMBER);
        addField(FilePropertyKey.YEAR);
        addField(FilePropertyKey.BITRATE);
    }

}
