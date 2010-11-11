package org.limewire.ui.swing.options.actions;

import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.options.OptionPanel;
import org.limewire.ui.swing.util.I18n;

public class CancelDialogAction extends AbstractAction {
    
    public static final String NAME = I18n.tr("Cancel");
    public static final String SHORT_DESCRIPTION = I18n.tr("Undo any changes made");
    
    private OptionPanel optionPanel;
    
    public CancelDialogAction() {
        this(null);
    }
    
    public CancelDialogAction(OptionPanel optionPanel) {
        this.optionPanel = optionPanel;
        
        putValue(Action.NAME, NAME);
        putValue(Action.SHORT_DESCRIPTION, SHORT_DESCRIPTION);
    }
    
    public void setOptionPanel(OptionPanel optionPanel) {
        this.optionPanel = optionPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JComponent comp = (JComponent)e.getSource();
        Container dialog = comp.getRootPane().getParent();
        if(dialog != null && dialog instanceof JDialog) {
            if(optionPanel != null)
                optionPanel.initOptions();
            ((JDialog)dialog).dispose();
        }
    }
}
