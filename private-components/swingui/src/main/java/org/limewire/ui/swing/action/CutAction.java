package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import org.limewire.ui.swing.util.I18n;

public class CutAction extends AbstractSelectionRequiredTextAction {

    public CutAction(JTextComponent component) {
        super(I18n.tr("Cut"), component, DefaultEditorKit.cutAction);
    }
}
