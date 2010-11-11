package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;

import org.limewire.ui.swing.util.I18n;

public class PasteAction extends AbstractTextAction {

    public PasteAction() {
        super(I18n.tr("Paste"), DefaultEditorKit.pasteAction);
    }
}
