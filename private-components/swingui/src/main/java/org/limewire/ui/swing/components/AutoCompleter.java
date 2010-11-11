package org.limewire.ui.swing.components;

import javax.swing.JComponent;

import org.limewire.concurrent.ListeningFuture;

/** A component that can offer autocomplete suggestions. */
public interface AutoCompleter {
    
    /** Sets the callback that will be notified when items are selected. */
    void setAutoCompleterCallback(AutoCompleterCallback callback);

    /** 
     * Sets the new input to the autocompleter.  This can change what is
     * currently visible as suggestions.
     * 
     * The returned Future returns true if the lookup for autocompletions
     * completed succesfully.  True does not indicate autocompletions are
     * available.  It merely indicates that the lookup completed.
     * 
     * Because AutoCompleters are allowed to be asynchronous, one should
     * use Future.get or listen to the future in order to see when
     * the future has completed.
     */
    ListeningFuture<Boolean> setInput(String input);

    /**
     * Returns true if any autocomplete suggestions are currently available, based the data
     * that was given to {@link #setInput(String)}.
     */
    boolean isAutoCompleteAvailable();

    /** Returns a component that renders the autocomplete items. */
    JComponent getRenderComponent();

    /** Returns the currently selected string. */
    String getSelectedAutoCompleteString();

    /** Increments the selection. */
    void incrementSelection();
    
    /** Decrements the selection. */
    void decrementSelection();
    
    /** A callback for users of autocompleter, so they know when items have been suggested. */
    public interface AutoCompleterCallback {
        /** Notification that an item is suggested. */
        void itemSuggested(String autoCompleteString, boolean keepPopupVisible, boolean triggerAction);
    }

}
