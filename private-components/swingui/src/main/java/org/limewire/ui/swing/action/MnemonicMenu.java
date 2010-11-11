package org.limewire.ui.swing.action;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.limewire.ui.swing.components.PlainCheckBoxMenuItemUI;
import org.limewire.ui.swing.components.PlainMenuItemUI;
import org.limewire.ui.swing.components.PlainMenuUI;
import org.limewire.ui.swing.components.PlainWindowsCheckBoxMenuItemUI;
import org.limewire.ui.swing.components.PlainWindowsMenuItemUI;
import org.limewire.ui.swing.components.PlainWindowsMenuUI;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingHacks;

/**
 * Allows the text of the menu to have an ampersand to 
 * mark the mnemonic of its name.
 */
public class MnemonicMenu extends JMenu {
    
    public MnemonicMenu(String text) {
        super(text);

        SwingHacks.fixPopupMenuForWindows(getPopupMenu());
    }
    
    @Override
    public void updateUI() {
        super.updateUI();
        overrideMenu(this);
    }
    
    @Override
    public void setText(String text) {
        int mnemonicKeyCode = GuiUtils.getMnemonicKeyCode(text);
        text = GuiUtils.stripAmpersand(text);
        if (mnemonicKeyCode != -1) { 
            setMnemonic(mnemonicKeyCode);
        }
        super.setText(text);
    }
    
    
    @Override
    public JMenuItem add(Action action) {
        JMenuItem item = super.add(action);
        overrideMenu(item);
        return item;
    }
    
    @Override
    public JMenuItem add(JMenuItem item) {
        JMenuItem itemReturned = super.add(item);
        overrideMenu(item);
        return itemReturned;
    }
    
    private void overrideMenu(JMenuItem item) {
        Class originalUIClass = item.getUI().getClass();
        
        if (originalUIClass == PlainCheckBoxMenuItemUI.class.getSuperclass()) {
            item.setUI(new PlainCheckBoxMenuItemUI());
        } else if (originalUIClass == PlainMenuUI.class.getSuperclass()) {
            item.setUI(new PlainMenuUI());
        } else if (originalUIClass == PlainMenuItemUI.class.getSuperclass()) {
            item.setUI(new PlainMenuItemUI());
        } if (originalUIClass == PlainWindowsCheckBoxMenuItemUI.class.getSuperclass()) {
            item.setUI(new PlainWindowsCheckBoxMenuItemUI());
        } else if (originalUIClass == PlainWindowsMenuUI.class.getSuperclass()) {
            item.setUI(new PlainWindowsMenuUI());
        } else if (originalUIClass == PlainWindowsMenuItemUI.class.getSuperclass()) {
            item.setUI(new PlainWindowsMenuItemUI());
        }
        
    }
}
