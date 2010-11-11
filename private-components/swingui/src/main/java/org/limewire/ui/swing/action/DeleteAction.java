package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.text.JTextComponent;

import org.limewire.ui.swing.util.I18n;

public class DeleteAction extends AbstractSelectionRequiredTextAction {

    public DeleteAction(JTextComponent component) {
        super(I18n.tr("Delete"), component);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent textComponent = getTextComponent(e);
        StringBuilder bldr = new StringBuilder(textComponent.getText());
        bldr.delete(textComponent.getSelectionStart(), textComponent.getSelectionEnd());
        textComponent.setText(bldr.toString());
    }
}
