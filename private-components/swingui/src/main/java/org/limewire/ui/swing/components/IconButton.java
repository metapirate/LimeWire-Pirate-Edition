package org.limewire.ui.swing.components;

import java.awt.Insets;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.listener.ActionHandListener;

public class IconButton extends JButton {
    
    private MouseListener actionHandListener;

    public IconButton() {
        init();
    }
    
    public IconButton(Action a) {
        super(a);
        init();
    }

    public IconButton(Icon icon) {
        super(icon);
        init();
    }

    public IconButton(String text, Icon icon) {
        super(text, icon);
        init();
    }
    
    public IconButton(Icon icon, Icon rolloverIcon) {
        super(icon);
        init();
        setRolloverIcon(rolloverIcon);
    }
    
    public IconButton(Icon icon, Icon rolloverIcon, Icon pressedIcon) {
        super(icon);
        init();
        setRolloverIcon(rolloverIcon);
        setPressedIcon(pressedIcon);
    }
    
    public IconButton(Icon icon, Icon rolloverIcon, Icon pressedIcon, Icon selectedIcon) {
        super(icon);
        init();
        setRolloverIcon(rolloverIcon);
        setPressedIcon(pressedIcon);
        setSelectedIcon(selectedIcon);
    }
    
    @Override
    public void setIcon(Icon defaultIcon) {
        super.setIcon(defaultIcon);
        if(getSelectedIcon() == null || getSelectedIcon() instanceof CustomShiftedIcon) {
            if(defaultIcon == null) {
                setSelectedIcon(null);
            } else {                
                setSelectedIcon(new CustomShiftedIcon(1, 1, defaultIcon));
            }
        }
    }
    
    /** Sets all properties to make the button look like an icon button. */
    public static void setIconButtonProperties(JButton button) {
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);
        button.setHideActionText(true);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setOpaque(false);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setIconTextGap(2);
    }
    
    private void init() {
        setIconButtonProperties(this);
        actionHandListener = new ActionHandListener();
        addMouseListener(actionHandListener);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        ActionHandListener.setActionHandDrawingDisabled(this, !enabled);
    }
    
    public void removeActionHandListener() {
        removeMouseListener(actionHandListener);
    }
    
    public void resetDefaultCursor() {
        ActionHandListener.resetDefaultCursor(this);
    }
    
    @Override
    protected void configurePropertiesFromAction(Action a) {
        super.configurePropertiesFromAction(a);
        if (hasSelectedKey(a)) {
            setSelectedFromAction(a);
        }
        
        Icon icon = (Icon)a.getValue(AbstractAction.PRESSED_ICON);
        if(icon != null) {
            setPressedIcon(icon);
        }
        
        icon = (Icon)a.getValue(AbstractAction.ROLLOVER_ICON);
        if(icon != null) {
            setRolloverIcon(icon);
        }
    }
    
    @Override
    protected void actionPropertyChanged(Action action, String propertyName) {
        super.actionPropertyChanged(action, propertyName);
        if (propertyName == Action.SELECTED_KEY && hasSelectedKey(action)) {
            setSelectedFromAction(action);
        } else if (propertyName == AbstractAction.PRESSED_ICON) {
            setPressedIcon((Icon)action.getValue(AbstractAction.PRESSED_ICON));
        } else if (propertyName == AbstractAction.ROLLOVER_ICON) {
            setRolloverIcon((Icon)action.getValue(AbstractAction.ROLLOVER_ICON));
        }
    }
    
    /** Sets the selected state of the button from the action. */
    private void setSelectedFromAction(Action a) {
        boolean selected = false;
        if (a != null) {
            selected =  Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
        }
        
        if (selected != isSelected()) {
            setSelected(selected);
        }
    }    

    private static boolean hasSelectedKey(Action a) {
        return (a != null && a.getValue(Action.SELECTED_KEY) != null);
    }
    
    /** An extension so we can check instanceof for our custom selected icons. */
    private static class CustomShiftedIcon extends ShiftedIcon {
        public CustomShiftedIcon(int shiftWidth, int shiftHeight, Icon icon) {
            super(shiftWidth, shiftHeight, icon);
        }        
    }
}
