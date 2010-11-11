package org.limewire.ui.swing.options.actions;

import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

public class OKDialogAction extends AbstractAction {

    public static final String NAME = I18n.tr("OK");
    public static final String SHORT_DESCRIPTION = I18n.tr("Keep any changes made");
    
    public OKDialogAction() {
        this(NAME, SHORT_DESCRIPTION);
    }
    
    public OKDialogAction(String name) {
        this(name, null);
    }
    
    public OKDialogAction(String name, String shortDescription) {
        putValue(Action.NAME, name);
        putValue(Action.SHORT_DESCRIPTION, shortDescription);
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JComponent comp = (JComponent)e.getSource();
        Container dialog = comp.getRootPane().getParent();
        if(dialog != null && dialog instanceof JDialog) {
            ((JDialog)dialog).dispose();
        }
    }
}
