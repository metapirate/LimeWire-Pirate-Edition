package org.limewire.ui.swing.friends.chat;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Minimizes the chat window
 */
public class MinimizeAction extends AbstractAction {
    private final Provider<ChatMediator> chatMediator;
    
    @Inject
    public MinimizeAction(Provider<ChatMediator> chatMediator) {
        this.chatMediator = chatMediator;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        chatMediator.get().setVisible(false);
    }
}
