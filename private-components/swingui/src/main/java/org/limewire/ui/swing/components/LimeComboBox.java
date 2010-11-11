package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.ui.swing.util.SwingHacks;
import org.limewire.util.Objects;

/** A combobox rendered in the LimeWire 5.0 style. */
public class LimeComboBox extends JXButton {
    
    /** The list of actions that the combobox is going to render as items. */
    private final List<Action> actions;

    /** The currently selected item. */
    private Action selectedAction;
    
    /** The currently selected component. */
    private JComponent selectedComponent;
    
    /** The currently selected label */
    private ActionLabel selectedLabel;

    /** Listeners that will be notified when a new item is selected. */
    private final List<SelectionListener> selectionListeners 
        = new ArrayList<SelectionListener>();
    
    /** Listeners that will be notified when the menu is recreated. */
    private final List<MenuCreationListener> menuCreationListeners
        = new ArrayList<MenuCreationListener>();
    
    /** True if you've supplied a custom menu via {@link #overrideMenu(JPopupMenu)}. */
    private boolean customMenu = false;
    
    /** True if the menu has been updated since the last addition of an action. */
    private boolean menuDirty = false;
    
    /** The menu, lazily created. */
    private JPopupMenu menu = null;
    
    /** True if the menu is visible. */
    private boolean menuVisible = false;
    
    /** The time that the menu became invisible, to allow toggling of on/off. */
    private long menuInvizTime = -1;
    
    /** True if clicking will always force visibility. */
    private boolean clickForcesVisible = false;
    
    /** Position to place the popup from the bottom left corner **/
    private Point popupPosition = new Point(1,-1);

    private final MouseListener mouseListener;

    private final ActionListener actionListener;
    
    /** Constructs an empty unskinned combo box. */
    public LimeComboBox() {
        this(null);
    }
    
    /** Constructs an empty unskinned combo box. */
    public LimeComboBox(List<Action> newActions) {        
        setText(null);
        actions = new ArrayList<Action>();
        addActions(newActions);
        
        if (!actions.isEmpty()) {
            selectedAction = actions.get(0);
        } else {
            selectedAction = null;
        }        
        
        initModel();
        
        actionListener =  new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActionLabel label = (ActionLabel)e.getSource();
                Action action = label.getAction();
                selectedAction = action;
                selectedComponent = (JComponent)label.getParent();
                selectedLabel = label;
                fireChangeEvent(action);
                repaint();
                menu.setVisible(false);
            }
        };
        
        mouseListener = new MouseAdapter() {
            
            private final Color foreground = UIManager.getColor("MenuItem.foreground");
            private final Color selectedForeground = UIManager.getColor("MenuItem.selectionForeground");
            
            @Override
            public void mouseEntered(MouseEvent e) {
                paintNormal(e.getSource(), true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                paintNormal(e.getSource(), false);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                paintNormal(e.getSource(), true);
            }
            
            private void paintNormal(Object source, boolean selected) {
                 JComponent label = (JComponent)source;
                 label.setForeground(selected ? selectedForeground : foreground );
                 
                 JComponent parent = (JComponent) label.getParent();
                 parent.setOpaque(selected);
                 parent.repaint();
                
                 // Remove highlight on the last selected component.
                 if (selectedComponent != null && selectedComponent != parent) {
                     selectedLabel.setForeground(foreground);
                     selectedComponent.setOpaque(false);
                     selectedComponent.repaint();
                     selectedLabel = null;
                     selectedComponent = null;
                 }
            }
        };
    }

    /** Sets the combobox to always display the given popupmenu. */
    public void overrideMenu(JPopupMenu menu) {
        this.menu = menu;
        customMenu = true;
        initMenu(true);
    }
    
    /** Sets the combobox to always display the given popupmenu. */
    public void overrideMenuNoRestyle(JPopupMenu menu) {
        this.menu = menu;
        customMenu = true;
        initMenu(false);
    }

    
    /**
     * A helper method for painting elements of overridden menus in the default style.
     */
    public JComponent decorateMenuComponent(JComponent item) {
        item.setFont(getFont());
        item.setBackground(Color.WHITE);
        item.setForeground(Color.BLACK);
        item.setBorder(BorderFactory.createEmptyBorder(0,1,0,0));
        return item;
    }

    protected JComponent attachListeners(JComponent comp) {
        comp.addMouseListener(mouseListener);
        if (comp instanceof ActionLabel) {
            ((ActionLabel)comp).addActionListener(actionListener);
        }
        return comp;
    }
    
    protected JComponent wrapItemForSelection(JComponent comp) {
        JPanel panel = new JPanel(new VerticalLayout());
        panel.setBackground(UIManager.getColor("MenuItem.selectionBackground"));
        panel.add(comp);
        panel.setOpaque(false);
        
        return panel;
    }
    
    /**
     * A helper method for creating menu items painted in the default style of an 
     *  overridden menu.
     */
    public JMenuItem createMenuItem(Action a) {
        JMenuItem item = new JMenuItem(a);
        decorateMenuComponent(item);
        return item;
    }
    
    /**
     * Adds the given actions to the combobox.  The actions will
     * be rendered as items that can be chosen.
     * <p>
     * This method has no effect if the popupmenu is overridden. 
     */
    public void addActions(List<Action> newActions) {
        if (newActions == null) {
            return;
        }
        menuDirty = true;
        actions.addAll(newActions);
        if (selectedAction == null) {
            selectedAction = actions.get(0);
        }
        ResizeUtils.updateSize(this, actions);
        if (menu == null) {
            createPopupMenu();
        }
    }
    
    /**
     * Returns the current popup menu. The menu is only the currently active
     * menu. If any methods are called that require the combobox to rebuild the
     * menu, any modifications made to this menu will be lost.
     */
    public JPopupMenu getPopupMenu() {
        if(menu == null)
            createPopupMenu();
        return menu;
    }
    
    public void setPopupPosition(Point p) {
        popupPosition = p;
    }
    
    /** 
     * Adds a single action to the combobox.
     *<p>
     * This method has no effect if the popupmenu is overridden.
     */
    public void addAction(Action action) {
        actions.add(Objects.nonNull(action, "action"));
        menuDirty = true;
        if (selectedAction == null) {
            selectedAction = actions.get(0);
        }
        ResizeUtils.updateSize(this, actions);
        if (menu == null) {
            createPopupMenu();
        }
    }

    /** 
     * Removes all actions & any selected action.
     * <p>
     * This method has no effect if the popupmenu is overridden.
     */
    public void removeAllActions() {
        menuDirty = true;
        actions.clear();
        selectedAction = null;
    }
    
    /**
     * Removes the specific action.  If it was the selected one, selection is lost. 
     *  <p>
     * This method has no effect if the popupmenu is overridden.
     */
    public void removeAction(Action action) {
        menuDirty = true;
        actions.remove(action);
        if (action == selectedAction) {
            if (actions.isEmpty()) {
                selectedAction = null;
            } else {
                // Selected the first element if there are any left
                selectedAction = actions.get(0);
            }
            selectedComponent = null;
            selectedLabel = null;                
        }
    }

    /** 
     * Selects the specified action and fires a change event.
     */
    public void selectAction(Action action) {
        boolean found = false;
        if (menu != null && menu.getComponentCount() > 0) {
            // Find menu component associated with action.
            for (Component component : menu.getComponents()) {
                if (component instanceof JPanel) {
                    Component child = ((JPanel) component).getComponent(0);
                    if (child instanceof ActionLabel) {
                        ActionLabel label = (ActionLabel) child;
                        if (label.getAction().equals(action)) {
                            // Update previously selected label.
                            if (selectedComponent != null && selectedComponent != component) {
                                selectedComponent.setOpaque(false);
                                selectedLabel.setForeground(UIManager.getColor("MenuItem.foreground"));
                            }
                            // Update newly selected label.
                            ((JComponent) component).setOpaque(true);
                            label.setForeground(UIManager.getColor("MenuItem.selectionForeground"));
                            selectedComponent = (JComponent) component;
                            selectedLabel = label;
                            found = true;
                            break;
                        }
                    }
                }
            }
            
        } else {
            // Determine if action is in the list.
            found = actions.contains(action);
        }
        
        if (found) {
            // Perform action and fire change event.
            selectedAction = action;
            action.actionPerformed(null);
            fireChangeEvent(action);
            repaint();
        }
    }
    
    /** 
     * Selects the specific action.
     * <p>
     * This method has no effect if the popupmenu is overridden, unless
     * the menu has been pre-seeded with actions that correspond to the
     * popup menu.
     */
    public void setSelectedAction(Action action) {        
        // Make sure the selected action is in the list
        if (actions.contains(action)) {
            selectedAction = action;
            repaint();
        }        
    }
    
    /**
     * Returns the selected action.
     * <p>
     * This method has no effect if the popupmenu is overridden.
     */
    public Action getSelectedAction() {
        return selectedAction;
    }
    
    
    /** Get all actions. */
    public List<Action> getActions() {
        return actions;
    }
    
    
    /** Sets the text this will display as the prompt. */
    @Override
    public void setText(String promptText) {
        super.setText(promptText);        
        if (promptText != null) {
            ResizeUtils.updateSize(this, actions);
        }
    }

    /** Manually triggers a resize of the component. 
      * <p>
      *  Should be avoided but can be used after drastic changes to font size/border after
      *  the component is layed out.
      */
    public void forceResize() {
        ResizeUtils.updateSize(this, actions);
    }
    
    // TODO: Resize model must be redone so this is not necessary
    @Override
    public void setFont(Font f) {
        super.setFont(f);
        menuDirty = true;
        ResizeUtils.updateSize(this, actions);
    }

    /**
     * Adds a listener to be notified when the selection changes.
     * <p>
     * This method has no effect if the popupmenu is overridden.
     */
    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    /**
     * Adds a listener to be notified when the popup menu is rebuilt.
     * <p>
     * This method has no effect if the popupmenu is overridden. 
     */
    public void addMenuCreationListener(MenuCreationListener listener) {
        menuCreationListeners.add(listener);
    }    
    
    @Override 
    public void setModel(final ButtonModel delegate) {
        super.setModel(new ButtonModel() {
            public boolean isArmed() { return delegate.isArmed(); }
            public boolean isSelected() { return delegate.isSelected(); }
            public boolean isEnabled() { return delegate.isEnabled(); }
            public boolean isPressed() {
                return delegate.isPressed() || (menu != null && menu.isVisible()); 
            }
            public boolean isRollover() { return delegate.isRollover(); }
            public void setArmed(boolean b) { delegate.setArmed(b); }
            public void setSelected(boolean b) { delegate.setSelected(b); }
            public void setEnabled(boolean b) { delegate.setEnabled(b); }
            public void setPressed(boolean b) { delegate.setPressed(b); }
            public void setRollover(boolean b) { delegate.setRollover(b); }
            public void setMnemonic(int i) { delegate.setMnemonic(i); }
            public int getMnemonic() { return delegate.getMnemonic(); }
            public void setActionCommand(String string) { delegate.setActionCommand(string); }
            public String getActionCommand() { return delegate.getActionCommand(); }
            public void setGroup(ButtonGroup buttonGroup) { delegate.setGroup(buttonGroup); }
            public void addActionListener(ActionListener actionListener) { 
                delegate.addActionListener(actionListener);
            }
            public void removeActionListener(ActionListener actionListener) {
                delegate.removeActionListener(actionListener);
            }
            public Object[] getSelectedObjects() { return delegate.getSelectedObjects(); }
            public void addItemListener(ItemListener itemListener) {
                delegate.addItemListener(itemListener);
            }
            public void removeItemListener(ItemListener itemListener) { 
                delegate.removeItemListener(itemListener);
            }
            public void addChangeListener(ChangeListener changeListener) { 
                delegate.addChangeListener(changeListener);
            }
            public void removeChangeListener(ChangeListener changeListener) {
                delegate.removeChangeListener(changeListener);
            }
        });
    }

    @Override
    public void setIcon(Icon icon) {
        super.setIcon(icon);
        ResizeUtils.updateSize(this, actions);
    }
    
    @Override
    public Icon getRolloverIcon() {
        Icon icon = super.getRolloverIcon();
        if (icon == null) {
            return getIcon();
        } else {
            return icon;
        }
    }
    
    @Override
    public Icon getPressedIcon() {
        Icon icon = super.getPressedIcon();
        if (icon == null) {
            return getIcon();
        } else {
            return icon;
        }
    }
        
    @Override
    public boolean isOpaque() {
        return false;
    }
       
    /**
     * Sets whether or not clicking the combobox forces the menu to display.
     * Normally clicking it would cause a visible menu to disappear.
     * If this is true, clicking will always force the menu to appear.
     * This is useful for renderers such as in tables.
     */
    public void setClickForcesVisible(boolean clickForcesVisible) {
        this.clickForcesVisible = clickForcesVisible;
    }

    private void initModel() {
        setModel(getModel());        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (menu != null && isEnabled()) {
                    // If the menu is visible or this is the click that
                    // caused it to become invisible, go with inviz.
                    if(!clickForcesVisible && (menuVisible || System.currentTimeMillis() - menuInvizTime <= 10f)) {
                        menu.setVisible(false);
                    } else {
                        menu.revalidate();
                        menu.show((Component) e.getSource(), popupPosition.x, getHeight()+popupPosition.y);
                    }
                }
            }
        });
    }

    private void createPopupMenu() {
        menu = new JPopupMenu();
        initMenu(true);
    }
       
    private void updateMenu() {
        // If custom or not dirty, do nothing.
        if(customMenu || !menuDirty) {
            return;
        }
        
        // otherwise, reset up the menu.
        menuDirty = false;
        menu.removeAll();
        Icon emptyIcon = null; 
        for(Action action : actions) {
            if(action.getValue(Action.SMALL_ICON) != null) {
                emptyIcon = new EmptyIcon(16, 16);
                break;
            }
        }
        
        selectedComponent = null;
        selectedLabel = null;
        
        for (Action action : actions) {
            
            // We create the label ourselves (instead of using JMenuItem),
            // because JMenuItem adds lots of bulky insets.

            ActionLabel menuItem = new ActionLabel(action);
            JComponent panel = wrapItemForSelection(menuItem);
                        
            if (action != selectedAction) {
                panel.setOpaque(false);
                menuItem.setForeground(UIManager.getColor("MenuItem.foreground"));
            } else {
                selectedComponent = panel;
                selectedLabel = menuItem;
                selectedComponent.setOpaque(true);
                selectedLabel.setForeground(UIManager.getColor("MenuItem.selectionForeground"));
            }
            
            if(menuItem.getIcon() == null) {
                menuItem.setIcon(emptyIcon);
            }
            attachListeners(menuItem);
            decorateMenuComponent(menuItem);
            menuItem.setBorder(BorderFactory.createEmptyBorder(0, 6, 2, 6));

            // Add separator if specified.
            if (action.getValue(AbstractAction.SEPARATOR) != null) {
                menu.addSeparator();
            }
            
            menu.add(panel);
        }
        
        if (getText() == null) {
            menu.add(Box.createHorizontalStrut(getWidth()-4));
        }   
        
        for(MenuCreationListener listener : menuCreationListeners) {
            listener.menuCreated(this, menu);
        }
    }
    
    protected void fireChangeEvent(Action action) {
        // Fire the parent listeners
        for (SelectionListener listener : selectionListeners) {
            listener.selectionChanged(action);
        }
    }
    
    private void initMenu(boolean style) {    
        if (style) {
            decorateMenuComponent(menu);
            menu.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
        }
        
        SwingHacks.fixPopupMenuForWindows(menu);
        
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                menuVisible = false;
                menuInvizTime = System.currentTimeMillis();
                
                LimeComboBox.this.repaint();
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                menuVisible = false;
                menuInvizTime = System.currentTimeMillis();
                
                LimeComboBox.this.repaint();
            }
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menuVisible = true;
                updateMenu();
            }
        });
    }

    /** A listener that's notified when the combobox rebuilds its JPopupMenu. */
    public static interface MenuCreationListener {
        public void menuCreated(LimeComboBox comboBox, JPopupMenu menu);
    }

    /** A listener that's notified when the selection in the combobox changes. */
    public static interface SelectionListener {
        /** Notification that the given action is now selected. */
        public void selectionChanged(Action item);
    }
}
