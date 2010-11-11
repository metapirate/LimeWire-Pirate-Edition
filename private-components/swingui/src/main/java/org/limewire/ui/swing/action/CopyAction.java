package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import org.limewire.ui.swing.util.I18n;

public class CopyAction extends AbstractSelectionRequiredTextAction {
    
    public CopyAction(JTextComponent component) {
        super(I18n.tr("Copy"), component, DefaultEditorKit.copyAction);
    }
}
