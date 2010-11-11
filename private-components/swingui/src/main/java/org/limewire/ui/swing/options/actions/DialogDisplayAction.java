package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;

/**
 * Creates a Dialog box for a given contentPane. The dialog is centered around
 * the parent component.
 */
public class DialogDisplayAction extends AbstractAction {

    private JDialog dialog;
    private JComponent parent;
    private JComponent contentPane;
    private String title;
        
    public DialogDisplayAction(JComponent parent, JComponent contentPane, String title, String actionText, String tooltip) {
        this.parent = parent;
        this.contentPane = contentPane;
        this.title = title;
        
        putValue(Action.NAME, actionText);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(dialog == null) {
            dialog = FocusJOptionPane.createDialog(title, parent, contentPane);
        }
        if(!dialog.isVisible()) {
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        }
    }
}
