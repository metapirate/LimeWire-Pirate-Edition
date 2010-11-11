package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.jdesktop.animation.transitions.EffectsManager;
import org.jdesktop.animation.transitions.ScreenTransition;
import org.jdesktop.animation.transitions.TransitionTarget;
import org.jdesktop.animation.transitions.EffectsManager.TransitionType;
import org.jdesktop.application.Resource;
import org.limewire.ui.swing.animate.EffectsUtils;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/** 
 * A horizontal container for {@link FancyTab FancyTab} objects.  
 * FlexibleTabList adjusts the number of visible tabs depending on the container
 * size, and displays a "more" button when the actual tab count exceeds the 
 * visible count.  New tabs may be added to the container by calling the 
 * {@link #addTabActionMapAt(TabActionMap, int) addTabActionMapAt()} method.  
 * 
 * <p>FlexibleTabList is used to display the search tabs at the top of the main
 * window.</p>
 */
public class FlexibleTabList extends AbstractTabList {
    private static final int MAX_TAB_WIDTH = 205;
    private static final int MIN_TAB_WIDTH = 115;
    private static final int RIGHT_INSET = 3;
    
    @Resource private Icon moreDefaultIcon;
    @Resource private Icon morePressedIcon;
    @Resource private Icon moreRolloverIcon;
    
    private final ComboBoxDecorator comboBoxDecorator;
    private final Action closeOtherAction;
    private final Action closeAllAction;

    private final JComponent parent;
    private final LayoutAnimator animator;
    
    private int maxVisibleTabs;
    private int vizStartIdx = -1;
    private boolean delayedLayout;
    private ChangeType pendingChangeType;
    
    /**
     * Constructs a FlexibleTabList with the specified combobox decorator.
     */
    @Inject
    FlexibleTabList(ComboBoxDecorator comboBoxDecorator) {
        this.comboBoxDecorator = comboBoxDecorator;
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0 0 0 3, gap 0, filly, hidemode 2"));  
        setMinimumSize(new Dimension(0, getMinimumSize().height));
        
        closeOtherAction = new CloseOther();
        closeAllAction = new CloseAll();
        
        maxVisibleTabs = Integer.MAX_VALUE;
        
        // Wrap the tab list in a parent component. (JXLayer also works here.)
        // This is needed to work around a bug in the Animated Transitions
        // library.  To work correctly, the library requires the bounds of the
        // animated container relative to its parent to start at location (0, 0).
        parent = new JPanel(new BorderLayout());
        parent.setOpaque(false);
        parent.add(this, BorderLayout.CENTER);
        
        // Create layout animation manager.
        animator = new LayoutAnimator(this);
        
        // Add listener to adjust tab layout when container is resized. 
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Redo tab layout when number of tabs changes.
                if (calculateVisibleTabCount() != maxVisibleTabs) {
                    layoutTabs(ChangeType.NONE);
                }
            }
        });
    }

    /**
     * Returns the display component.
     */
    public JComponent getComponent() {
        return parent;
    }
    
    /** 
     * Adds a new tab using the specified action map at the specified index. 
     */
    public void addTabActionMapAt(TabActionMap actionMap, int i) {
        FancyTab tab = createAndPrepareTab(actionMap);
        addTab(tab, i);
    }

    /**
     * Creates a new tab with the specified action map.  This calls the 
     * superclass method to create the tab, sets the minimum and maximum widths,
     * and adds close actions. 
     */
    @Override
    protected FancyTab createAndPrepareTab(TabActionMap actionMap) {
        // Create tab.
        FancyTab tab = super.createAndPrepareTab(actionMap);
        
        // Set minimum and maximum widths.
        tab.setMinimumSize(new Dimension(MIN_TAB_WIDTH, tab.getMinimumSize().height));
        tab.setMaximumSize(new Dimension(MAX_TAB_WIDTH, tab.getMaximumSize().height));
        
        // Add Close actions.
        actionMap.setRemoveAll(closeAllAction);
        actionMap.setRemoveOthers(closeOtherAction);
        
        return tab;
    }
    
    @Override
    protected void layoutTabs() {
        // Must be called on UI thread.
        assert SwingUtilities.isEventDispatchThread();
        
        // Do layout only if not in progress or delayed.
        if (!animator.isRunning() && !delayedLayout) {
            List<FancyTab> visibleTabs = getPendingVisibleTabs(false);
            doTabLayout(visibleTabs);
            revalidate();
            repaint();
        }
    }
    
    @Override
    protected void layoutTabs(ChangeType changeType) {
        // Must be called on UI thread.
        assert SwingUtilities.isEventDispatchThread();
        
        // Skip if layout is delayed.
        if (delayedLayout) return;
        
        if (animator.isRunning()) {
            // Save change type for later use.
            pendingChangeType = changeType;
            return;
            
        } else {
            // Clear pending change type.
            pendingChangeType = null;

            // Get old index of first visible tab.
            int oldStartIdx = vizStartIdx;

            // Get new list of visible tabs.
            List<FancyTab> visibleTabs = getPendingVisibleTabs(changeType == ChangeType.ADDED);

            // Animate layout when tab added, removed or selected.
            if (changeType == ChangeType.ADDED || 
                    changeType == ChangeType.REMOVED ||
                    changeType == ChangeType.SELECTED) {
                // Set up tab effects and start layout animation.
                setupTabEffects(changeType, oldStartIdx, visibleTabs);
                animator.start(visibleTabs);

            } else {
                // Layout tabs without animation.
                EffectsManager.clearAllEffects();
                doTabLayout(visibleTabs);
                revalidate();
                repaint();
            }
        }
    }
    
    /** 
     * Adds the specified list of visible tabs to the layout.  This method 
     * removes all previously visible tabs, and adds the specified tabs to 
     * the layout. 
     */
    private void doTabLayout(List<FancyTab> visibleTabs) {
        // Remove all tabs.
        removeAll();
        
        // Add visible tabs to container.
        for (FancyTab tab : visibleTabs) {
            add(tab, "growy");
        }
        
        // Add "more" button if some tabs not visible.
        List<FancyTab> allTabs = getTabs();
        if (visibleTabs.size() < allTabs.size()) {
            FancyTabMoreButton more = new FancyTabMoreButton(allTabs);
            comboBoxDecorator.decorateIconComboBox(more);
            more.setIcon(moreDefaultIcon);
            more.setPressedIcon(morePressedIcon);
            more.setRolloverIcon(moreRolloverIcon);
            more.setSelectedIcon(morePressedIcon);
            add(more, "gapleft 0:" + String.valueOf(MIN_TAB_WIDTH));
        }
    }
    
    /**
     * Sets up the animation effects when the tab layout changes.
     */
    private void setupTabEffects(ChangeType mode, int oldStartIdx, List<FancyTab> visibleTabs) {
        // Remove existing effects.
        EffectsManager.clearAllEffects();
        
        // Set move-in effects on visible tabs.
        for (FancyTab tab : visibleTabs) {
            if (vizStartIdx == oldStartIdx) {
                // When tab added, tabs slide in from the left.
                // When tab removed or selected, tabs slide in from the right.
                if (mode == ChangeType.ADDED) {
                    EffectsManager.setEffect(tab, EffectsUtils.createMoveInEffect(-MIN_TAB_WIDTH, 0, false), TransitionType.APPEARING);
                } else {
                    EffectsManager.setEffect(tab, EffectsUtils.createMoveInEffect(getWidth() - RIGHT_INSET, getHeight() / 2, true), TransitionType.APPEARING);
                }
            } else if (vizStartIdx < oldStartIdx || oldStartIdx < 0) {
                // New tabs slide in from the left.
                EffectsManager.setEffect(tab, EffectsUtils.createMoveInEffect(-MIN_TAB_WIDTH, 0, false), TransitionType.APPEARING);
            } else {
                // New tabs slide in from the right.
                EffectsManager.setEffect(tab, EffectsUtils.createMoveInEffect(getWidth() - RIGHT_INSET, getHeight() / 2, true), TransitionType.APPEARING);
            }
        }
        
        // Set move-out effects on all tabs.
        List<FancyTab> allTabs = getTabs();
        for (FancyTab tab : allTabs) {
            if (vizStartIdx <= oldStartIdx) {
                // Old tabs slide out to the right.
                EffectsManager.setEffect(tab, EffectsUtils.createMoveOutEffect(getWidth() - RIGHT_INSET, getHeight() / 2, true), TransitionType.DISAPPEARING);
            } else {
                // Old tabs slide out to the left.
                EffectsManager.setEffect(tab, EffectsUtils.createMoveOutEffect(-MIN_TAB_WIDTH, 0, false), TransitionType.DISAPPEARING);
            }
        }
    }
    
    /**
     * Returns the tabs that *should* be visible, based on the currently visible
     * tabs, and the currently selected tab.  This keeps state and assumes the
     * tabs it returns will become visible.
     * <p>
     * The goal is to shift the minimum amount of distance possible, while
     * still keeping the selected tab in view.  If there's no selected tab,
     * this bumps everything to the left one.
     */
    private List<FancyTab> getPendingVisibleTabs(boolean tabAdded) {
        // Calculate maximum visible tabs.
        maxVisibleTabs = calculateVisibleTabCount();
        
        // Determine tabs to display.
        List<FancyTab> tabs = getTabs();
        List<FancyTab> vizTabs;
        if (maxVisibleTabs >= tabs.size()) {
            vizStartIdx = 0;
            vizTabs = tabs;
        } else {        
            // Bump the start down from where it previously was
            // if there's now more room to display more tabs,
            // so that we display as many tabs as possible.
            if (tabs.size() - vizStartIdx < maxVisibleTabs) {
                vizStartIdx = tabs.size() - maxVisibleTabs;
            }
            vizTabs = tabs.subList(vizStartIdx, vizStartIdx + maxVisibleTabs);
            
            // If we had a selection, make sure that we shift in the
            // appropriate distance to keep that selection in view.
            // We always select the first tab when a new tab is added.
            FancyTab selectedTab = tabAdded ? tabs.get(0) : getSelectedTab();
            if (selectedTab != null && !vizTabs.contains(selectedTab)) {
                int selIdx = tabs.indexOf(selectedTab);
                if (vizStartIdx > selIdx) { // We have to shift left
                    vizStartIdx = selIdx;
                } else { // We have to shift right
                    vizStartIdx = selIdx - maxVisibleTabs + 1;
                }
                vizTabs = tabs.subList(vizStartIdx, vizStartIdx+maxVisibleTabs);
            }
        }
        
        return vizTabs;
    }
    
    /**
     * Calculates the number of visible tabs that will fit in the container.
     * This is based on the current container width and the minimum tab width.
     */
    private int calculateVisibleTabCount() {
        int visibleTabCount;
        
        // Calculate available width and maximum visible tabs.
        int totalWidth = getSize().width;
        int availWidth = Math.max(totalWidth, MIN_TAB_WIDTH);
        visibleTabCount = availWidth / MIN_TAB_WIDTH;
        
        // Adjust maximum tabs including "more" button if necessary.
        if (visibleTabCount < getTabs().size()) {
            int moreWidth = moreDefaultIcon.getIconWidth();
            availWidth = Math.max(totalWidth - moreWidth - RIGHT_INSET, MIN_TAB_WIDTH);
            visibleTabCount = availWidth / MIN_TAB_WIDTH;
        }
        
        return visibleTabCount;
    }

    /**
     * Recreates the tabs in the container using their action maps.
     */
    private void recreateTabs() {
        List<FancyTab> tabs = getTabs();
        if (tabs.size() > 0) {
            List<TabActionMap> actionMaps = new ArrayList<TabActionMap>(tabs.size());
            for (FancyTab tab : tabs) {
                actionMaps.add(tab.getTabActionMap());
            }
            setTabActionMaps(actionMaps);
        }
    }
    
    /**
     * Handles event when animated layout is completed.
     */
    private void layoutDone() {
        // Redo layout if layout change is pending.
        if (pendingChangeType != null) {
            layoutTabs(pendingChangeType);
        }
    }
    
    /**
     * Freezes the current tab layout.  This method may be called when we want
     * to aggregate multiple tab additions/deletions into a single layout
     * update.  The <code>updateTabLayout()</code> method should be called to
     * unfreeze and update the layout.
     * 
     * @see #updateTabLayout(ChangeType)
     */
    public void freezeTabLayout() {
        assert SwingUtilities.isEventDispatchThread();
        delayedLayout = true;
    }
    
    /**
     * Updates the tab layout.
     * 
     * @see #freezeTabLayout()
     */
    public void updateTabLayout(ChangeType changeType) {
        delayedLayout = false;
        layoutTabs(changeType);
    }
    
    /**
     * Sets the text for the Close All tabs action.
     */
    public void setCloseAllText(String closeAllText) {
        getTabProperties().setCloseAllText(closeAllText);
        closeAllAction.putValue(Action.NAME, closeAllText);
    }

    /**
     * Sets the text for the Close tab action.
     */
    public void setCloseOneText(String closeOneText) {
        getTabProperties().setCloseOneText(closeOneText);
    }

    /**
     * Sets the text for the Close All Other tabs action.
     */
    public void setCloseOtherText(String closeOtherText) {
        getTabProperties().setCloseOtherText(closeOtherText);
        closeOtherAction.putValue(Action.NAME, closeOtherText);
    }
    
    /** 
     * Sets whether or not the tabs should render a 'remove' icon. 
     */
    public void setRemovable(boolean removable) {
        getTabProperties().setRemovable(removable);
        recreateTabs();
    }
    
    /**
     * Sets the insets for all tabs.
     */
    public void setTabInsets(Insets insets) {
        getTabProperties().setInsets(insets);
        
        revalidate();
    }
    
    /**
     * Action to remove all tabs from the container.
     */
    private class CloseAll extends AbstractAction {
        public CloseAll() {
            super(getTabProperties().getCloseAllText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            freezeTabLayout();
            
            // Remove all tabs.
            while (!getTabs().isEmpty()) {
                getTabs().get(0).remove();
            }
            
            // Update tab layout.
            updateTabLayout(ChangeType.REMOVED);
        }
    }
    
    /**
     * Action to remove all tabs except the current one from the container.
     */
    private class CloseOther extends AbstractAction {
        public CloseOther() {
            super(getTabProperties().getCloseOtherText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            freezeTabLayout();
            
            // Remove all other tabs.
            while (getTabs().size() > 1) {
                FancyTab tab = getTabs().get(0);
                if (isFrom(tab, (Component) e.getSource())) {
                    tab = getTabs().get(1);
                }
                tab.remove();
            }
            
            // Update tab layout. 
            updateTabLayout(ChangeType.REMOVED);
        }
        
        private boolean isFrom(JComponent parent, Component child) {
            while (child.getParent() != null) {
                child = child.getParent();
                if (child instanceof JPopupMenu) {
                    child = ((JPopupMenu) child).getInvoker();
                }
                
                if (child == parent) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Animation manager for the tab layout. 
     */
    private class LayoutAnimator extends TimingTargetAdapter implements TransitionTarget {
        private final Animator animator;
        private final ScreenTransition transition;
        
        private List<FancyTab> newVisibleTabs;
        private boolean running;

        /**
         * Constructs a LayoutAnimator for the specified tab container.
         */
        public LayoutAnimator(JComponent tabContainer) {
            // Set up animation to run for 0.3 seconds at 33 frames per second.
            animator = new Animator(300, this);
            animator.setResolution(30);
            transition = new ScreenTransition(tabContainer, this, animator);
        }
        
        /**
         * Starts the animated transition.
         */
        public void start(List<FancyTab> visibleTabs) {
            running = true;
            newVisibleTabs = new ArrayList<FancyTab>(visibleTabs);
            transition.start();
        }
        
        /**
         * Returns true if the animated transition is running.
         */
        public boolean isRunning() {
            return running;
        }
        
        @Override
        public void setupNextScreen() {
            // Add new visible tabs to container.
            doTabLayout(newVisibleTabs);
        }
        
        @Override
        public void end() {
            // Reset indicator and notify container.
            running = false;
            layoutDone();
        }
    }
}
