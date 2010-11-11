package org.limewire.ui.swing.menu;

import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.action.MnemonicMenu;

/** An extension of MnemonicMenu that implements DelayedMenuItemCreator. */
public abstract class DelayedMnemonicMenu extends MnemonicMenu implements DelayedMenuItemCreator {
    
    public DelayedMnemonicMenu(String text) {
        super(text);
    }
}
