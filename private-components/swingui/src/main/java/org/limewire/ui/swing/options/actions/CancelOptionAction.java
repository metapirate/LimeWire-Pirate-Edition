package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;

import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;

public class CancelOptionAction extends AbstractAction {

    private JDialog optionDialog;
    
    public CancelOptionAction(JDialog optionDialog) {
        this.optionDialog = optionDialog;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        optionDialog.dispose();
    }
}
