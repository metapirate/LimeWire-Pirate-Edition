package org.limewire.ui.swing.tray;

import javax.swing.Action;
import javax.swing.Icon;

import org.limewire.ui.swing.util.I18n;

/**
 * Represents a notification. A notification must have a message and can
 * optionally have an icon, title and associated actions.
 */
public class Notification {

    private String title;

    private String message;

    private Action[] actions;

    private Icon icon;

    public Notification(String title, String message, Icon icon, Action... actions) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.actions = actions;
    }

    public Notification(String message, Icon icon, Action... actions) {
        this(null, message, icon, actions);
    }

    public Notification(String title, String message, Action... actions) {
        this(title, message, null, actions);
    }

    public Notification(String message, Action... actions) {
        this(null, message, null, actions);
    }

    public String getMessage() {
        return message;
    }

    public Action[] getActions() {
        return actions;
    }

    /**
     * Returns the name for the first action found in the list of actions passed
     * to the constructor. If no name exists, the translated 'Launch' text is
     * returned.
     */
    public String getActionName() {
        String actionName = actions.length > 0 && actions[0].getValue(Action.NAME) != null ? actions[0]
                .getValue(Action.NAME).toString()
                : I18n.trc("Launch Action Text (Notification)", "Launch");
        return actionName;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }
}
