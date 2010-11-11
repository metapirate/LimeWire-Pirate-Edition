package org.limewire.ui.swing.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseActionListener extends MouseAdapter {

    private final ActionListener actionListener;
    
    public MouseActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(e.getComponent(),
                    ActionEvent.ACTION_PERFORMED, null));
        }
    }
}
