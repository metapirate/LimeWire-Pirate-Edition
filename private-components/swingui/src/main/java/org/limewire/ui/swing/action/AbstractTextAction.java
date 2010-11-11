package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

public abstract class AbstractTextAction extends TextAction {
    private final String[] actions;

    public AbstractTextAction(String name, String... actions) {
        super(name);
        this.actions = actions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for(String action : actions) {
            doAction(e, action);
        }
    }

    private void doAction(ActionEvent e, String actionName) {
        JTextComponent target = getTextComponent(e);
        Action action = target.getActionMap().get(actionName);
        action.actionPerformed(e);
    }
}
