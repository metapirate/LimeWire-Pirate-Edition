package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;

import org.limewire.ui.swing.util.I18n;

public class CopyAllAction extends AbstractTextAction {
    
    public CopyAllAction() {
        super(I18n.tr("Copy All"), 
              DefaultEditorKit.selectAllAction, 
              DefaultEditorKit.copyAction);
    }
}
