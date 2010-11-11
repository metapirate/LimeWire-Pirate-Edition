package org.limewire.ui.swing.action;

import javax.swing.text.JTextComponent;

public abstract class AbstractSelectionRequiredTextAction extends AbstractTextAction {
    private final JTextComponent component;
    
    public AbstractSelectionRequiredTextAction(String name, JTextComponent component, String... actions) {
        super(name, actions);
        this.component = component;
    }
    
    @Override
    public boolean isEnabled() {
        return component.getSelectedText() != null;
    }
}
