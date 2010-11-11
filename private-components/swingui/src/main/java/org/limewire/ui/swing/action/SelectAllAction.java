package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;

import org.limewire.ui.swing.util.I18n;

public class SelectAllAction extends AbstractTextAction {

    public SelectAllAction() {
        super(I18n.tr("Select All"), DefaultEditorKit.selectAllAction);
    }
}
