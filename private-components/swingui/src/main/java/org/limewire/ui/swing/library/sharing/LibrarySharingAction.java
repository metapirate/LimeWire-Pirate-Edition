package org.limewire.ui.swing.library.sharing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.limewire.listener.EventListenerList;

import com.google.inject.Inject;

/**
 * An action listener that broadcasts a LibrarySharingEvent to all registered
 * listeners.
 */
public class LibrarySharingAction implements ActionListener {

    private final EventListenerList<LibrarySharingEvent> listenerList;
    
    @Inject
    public LibrarySharingAction(EventListenerList<LibrarySharingEvent> listenerList) {
        this.listenerList = listenerList;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        listenerList.broadcast(new LibrarySharingEvent());
    }
}
