package org.limewire.ui.swing.action;

import javax.swing.Icon;

import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

/**
 * Abstract class that allows the name of the action to have an ampersand to 
 * mark the mnemonic of the action in its name.
 * <p>
 * A call to {@link #putValue(String, Object) putValue(Action.Name, "Hello &World")}
 * will set the name of the action to "Hello World" and its mnemonic to 'W'.
 */
public abstract class AbstractAction extends javax.swing.AbstractAction {
    
    public static final String PRESSED_ICON = "limewire.pressedIcon";
    public static final String ROLLOVER_ICON = "limewire.rolloverIcon";
    /** Key to indicate action should be preceded by separator in menu */
    public static final String SEPARATOR = "limewire.separator";

    public AbstractAction(String name, Icon icon) {
        super(name, icon);
    }

    public AbstractAction(String name) {
        super(name);
    }
    
    public AbstractAction() {
    }
    
    @Override
    public void putValue(String key, Object newValue) {
        // parse out mnemonic key for action name
        if (key.equals(NAME)) {
            String name = (String)newValue;
            newValue = GuiUtils.stripAmpersand(name);
            int mnemonicKeyCode = GuiUtils.getMnemonicKeyCode(name);
            if (mnemonicKeyCode != -1) { 
            	super.putValue(MNEMONIC_KEY, mnemonicKeyCode);
            }
        }
        super.putValue(key, newValue);
    }

    /**
     * Swing thread-safe way to enable/disable the action from any thread. 
     */
    public void setEnabledLater(final boolean enabled) {
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                setEnabled(enabled);
            }
        });
    }
}
