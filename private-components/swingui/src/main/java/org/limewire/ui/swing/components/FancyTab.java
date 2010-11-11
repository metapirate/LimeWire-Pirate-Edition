package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.JToggleButton.ToggleButtonModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingHacks;

/**
 * A fancy 'tab' for use in a {@link FancyTabList}.
 */
public class FancyTab extends JXPanel {

    private final TabActionMap tabActions;
    private final AbstractButton mainButton;
    private final AbstractButton removeButton;
    private final JLabel busyLabel;
    private final JLabel additionalText;
    private final FancyTabProperties props;
    
    private static enum TabState {
        BACKGROUND, ROLLOVER, SELECTED;
    }
    
    private TabState currentState;
    
    private boolean mouseInside;
    private Icon removeEmptyIcon;
    @Resource private Icon removeActiveIcon;
    @Resource private Icon removeActiveRolloverIcon;
    @Resource private Icon removeActivePressedIcon;
    @Resource private Icon removeInactiveIcon;
    @Resource private Icon removeInactiveRolloverIcon;
    @Resource private Icon removeInactivePressedIcon;
    
    public FancyTab(TabActionMap actionMap,
            ButtonGroup group,
            FancyTabProperties fancyTabProperties) {
        GuiUtils.assignResources(this);
        
        removeEmptyIcon = new EmptyIcon(removeActiveIcon.getIconWidth(),
                removeActiveIcon.getIconHeight());
        
        this.tabActions = actionMap;
        this.props = fancyTabProperties;
        this.mainButton = createMainButton();
        this.additionalText = createAdditionalText();
        this.removeButton = createRemoveButton();
        this.busyLabel = createBusyLabel();

        if (group != null) {
            group.add(mainButton);
        }

        mainButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                changeState(e.getStateChange() == ItemEvent.SELECTED ?
                    TabState.SELECTED : TabState.BACKGROUND);
            }
        });
            
        setOpaque(false);
        setToolTipText(getTooltip());
        
        HighlightListener highlightListener = new HighlightListener();
        if (props.isRemovable()) {
            removeButton.addMouseListener(highlightListener);
        }
        
        addMouseListener(highlightListener);
        mainButton.addMouseListener(highlightListener);

        updateButtons(false);
        changeState(isSelected() ? TabState.SELECTED : TabState.BACKGROUND);
        
        setLayout(new MigLayout("insets 0 0 0 0, fill, gap 0"));        
        add(mainButton,     "gapafter 4, gapbefore 6, growy, aligny 50%, width min(pref,50):pref:max, cell 1 0");
        add(additionalText, "gapafter 4, aligny 50%, cell 2 0, hidemode 3");
        add(busyLabel,      "gapbefore 4, gapafter 6, gapbottom 1, aligny 50%, alignx right, cell 3 0, hidemode 3");
        add(removeButton,   "gapbefore 4, gapafter 6, gapbottom 1, aligny 50%, alignx right, cell 3 0, hidemode 3");
    }
    
    @Override
    public String toString() {
        return "FancyTab for: " + getTitle() + ", " + super.toString();
    }
    
    @Override 
    public Insets getInsets() {
        if (props == null || props.getInsets() == null)
            return super.getInsets();
        
        return props.getInsets();
    }
    
    private boolean isBusy() {
        return Boolean.TRUE.equals(tabActions.getMainAction().getValue(TabActionMap.BUSY_KEY));
    }
    
    JLabel createBusyLabel() {
        final JXBusyLabel busy = new ColoredBusyLabel(new Dimension(12, 12));
        
        busy.setVisible(false);
        
        if (isBusy()) {
            busy.setBusy(true);
            busy.setVisible(true);
        } 
        
        tabActions.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(TabActionMap.BUSY_KEY)) {
                    boolean on = Boolean.TRUE.equals(evt.getNewValue());
                    busy.setBusy(on);
                    busy.setVisible(on);  
                    updateButtons(mouseInside);
                }
            }
        });
        return busy;
    }
    
    JLabel createAdditionalText() {
        final JLabel label = new JLabel();
        label.setVisible(false);
        
        if (tabActions.getMoreTextAction() != null) {
            label.setOpaque(false);
            label.setFont(mainButton.getFont());
            
            String name =
                (String) tabActions.getMoreTextAction().getValue(Action.NAME);
            if (name != null && name.length() > 0) {
                label.setText("(" + name + ")");
                label.setVisible(true);
            }
            
            tabActions.getMoreTextAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(Action.NAME)) {
                        if (evt.getNewValue() != null) {
                            String newValue = (String) evt.getNewValue();
                            label.setText("(" + newValue + ")");
                            label.setVisible(true);
                        } else {
                            label.setVisible(false);
                        }
                    }
                }
            });
        }
        
        return label;
    }
    
    JButton createRemoveButton() {
        JButton button = new JButton();

        button.setIcon(removeEmptyIcon);
        button.setRolloverIcon(removeActiveRolloverIcon);
        button.setPressedIcon(removeActivePressedIcon);
        
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setAction(tabActions.getRemoveAction());
        button.setActionCommand(TabActionMap.REMOVE_COMMAND);
        button.setHideActionText(true);
        button.setVisible(false);
        if (removeButton != null) {
            for (ActionListener listener : removeButton.getActionListeners()) {
                if (listener == tabActions.getRemoveAction()) {
                    // Ignore the remove action -- it's added implicitly.
                    continue;
                }
                button.addActionListener(listener);
            }
        }
        return button;
    }
    
    AbstractButton createMainButton() {
        final AbstractButton button = new JToggleButton();
        button.setModel(new NoToggleModel());
        button.setAction(tabActions.getMainAction());
        button.setActionCommand(TabActionMap.SELECT_COMMAND);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setToolTipText(getTooltip());
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setOpaque(false);

        if (props.getTextFont() != null) {
            button.setFont(props.getTextFont());
        }
        
        if(Boolean.TRUE.equals(tabActions.getMainAction().getValue(TabActionMap.NEW_HINT))) {
            FontUtils.bold(button);
        } else {
            FontUtils.plain(button);
        }
        
        tabActions.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(TabActionMap.NEW_HINT)) {
                    if(Boolean.TRUE.equals(evt.getNewValue())) {
                        FontUtils.bold(button);
                    } else {
                        FontUtils.plain(button);
                    }
                }
            }
        });
        
        return button;
    }
    
    public FancyTabProperties getProperties() {
        return props;
    }
    /** Gets the action underlying this tab. */
    public TabActionMap getTabActionMap() {
        return tabActions;
    }
    
    public void remove() {
        removeButton.doClick(0);
    }
    
    public void select() {
        mainButton.doClick(0);
    }
    
    void addRemoveActionListener(ActionListener listener) {
        removeButton.addActionListener(listener);
    }
    
    void removeRemoveActionListener(ActionListener listener) {
        removeButton.removeActionListener(listener);
    }

    /** Selects this tab. */
    void setSelected(boolean selected) {
        mainButton.setSelected(selected);
    }

    /** Returns true if this tab is selected. */
    boolean isSelected() {
        return mainButton.isSelected();
    }

    /** Sets the foreground color of the tab. */
    void setButtonForeground(Color color) {
        mainButton.setForeground(color);
        additionalText.setForeground(color);
    }
    
    void setUnderlineEnabled(boolean enabled) {
        if (enabled) {
            FontUtils.underline(mainButton); 
        }
        else {
            FontUtils.removeUnderline(mainButton);
        }
    }

    /** Returns true if the tab is currently highlighted (in a rollover). */
    boolean isHighlighted() {
        return currentState == TabState.ROLLOVER;
    }

    /** Removes this tab from the button group. */
    void removeFromGroup(ButtonGroup group) {
        group.remove(mainButton);
    }
    
    public void setTextFont(Font font) {
        if (mainButton != null) {
            mainButton.setFont(font);
        }
        
        if (additionalText != null) {
            additionalText.setFont(font);
        }
    }
    
    private void updateButtons(boolean mouseInside) {
        this.mouseInside = mouseInside;
        
        if(mouseInside || !isBusy()) {
            if(props.isRemovable()) {
                removeButton.setVisible(true);
            } else {
                removeButton.setVisible(false);
            }
            busyLabel.setVisible(false);
        } else { // isBusy == true
            busyLabel.setVisible(true);
            removeButton.setVisible(false);
        }
    }
    
    private void changeState(TabState tabState) {
        if (currentState != tabState) {
            this.currentState = tabState;
            switch(tabState) {
            case SELECTED:
                setUnderlineEnabled(false);
                mainButton.setForeground(props.getSelectionColor());
                additionalText.setForeground(props.getSelectionColor());
                this.setBackgroundPainter(props.getSelectedPainter());
                removeButton.setIcon(removeActiveIcon);
                removeButton.setRolloverIcon(removeActiveRolloverIcon);
                removeButton.setPressedIcon(removeActivePressedIcon);
                break;
            case BACKGROUND:
                setUnderlineEnabled(props.isUnderlineEnabled());
                mainButton.setForeground(props.getNormalColor());
                additionalText.setForeground(props.getNormalColor());
                this.setBackgroundPainter(props.getNormalPainter());
                removeButton.setIcon(removeEmptyIcon);
                break;
            case ROLLOVER:
                setUnderlineEnabled(props.isUnderlineEnabled());
                setBackgroundPainter(props.getHighlightPainter());
                removeButton.setIcon(removeInactiveIcon);
                removeButton.setRolloverIcon(removeInactiveRolloverIcon);
                removeButton.setPressedIcon(removeInactivePressedIcon);
                break;
            }
        }
    }

    public String getTitle() {
        return (String)tabActions.getMainAction().getValue(Action.NAME);
    }
    
    private String getTooltip() {
        return (String)tabActions.getMainAction().getValue(Action.LONG_DESCRIPTION);
    }
    
    private void showPopup(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        SwingHacks.fixPopupMenuForWindows(menu);
        
        for (Action action : getTabActionMap().getRightClickActions()) {
            if(action == TabActionMap.SEPARATOR){
                menu.addSeparator();
            } else {
                menu.add(action);
            }
        }
        
        if (getComponentCount() != 0 && props.isRemovable()) {
            menu.addSeparator();
        }
        
        if (props.isRemovable()) {
            menu.add(getTabActionMap().getRemoveOthers());
            menu.add(getTabActionMap().getRemoveAll());
            menu.addSeparator();
            menu.add(new AbstractAction(props.getCloseOneText()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    remove();
                }
            });
        }
        menu.show((Component)e.getSource(), e.getX() + 3, e.getY() + 3);
    }
    
    private class HighlightListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            updateButtons(true);
            if (!isSelected() && mainButton.isEnabled()) {
                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                changeState(TabState.ROLLOVER);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updateButtons(false);
            e.getComponent().setCursor(Cursor.getDefaultCursor());
            if (!isSelected()) {
                changeState(TabState.BACKGROUND);
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (props.isRemovable() && SwingUtilities.isMiddleMouseButton(e)) {
                remove();
            } else if (!(e.getSource() instanceof AbstractButton) && SwingUtilities.isLeftMouseButton(e)) {
                select();
            } else if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopup(e);
            }
        }
    }    
    
    private static class NoToggleModel extends ToggleButtonModel {
        @Override
        public void setPressed(boolean b) {
            if ((isPressed() == b) || !isEnabled()) {
                return;
            }

            // This is different than the super in that
            // we only go from false -> true, not true -> false.
            if (!b && isArmed() && !isSelected()) {
                setSelected(true);
            } 

            if (b) {
                stateMask |= PRESSED;
            } else {
                stateMask &= ~PRESSED;
            }

            fireStateChanged();

            if (!isPressed() && isArmed()) {
                int modifiers = 0;
                AWTEvent currentEvent = EventQueue.getCurrentEvent();
                if (currentEvent instanceof InputEvent) {
                    modifiers = ((InputEvent)currentEvent).getModifiers();
                } else if (currentEvent instanceof ActionEvent) {
                    modifiers = ((ActionEvent)currentEvent).getModifiers();
                }
                fireActionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    getActionCommand(),
                                    EventQueue.getMostRecentEventTime(),
                                    modifiers));
            }
        }
    }
}
