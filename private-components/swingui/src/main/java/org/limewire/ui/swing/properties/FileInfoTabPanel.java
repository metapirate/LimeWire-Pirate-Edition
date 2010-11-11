package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * A list of tabs to select different panels containing information
 * about a given PropertiableFile.
 */
class FileInfoTabPanel {

    enum Tabs {
        GENERAL(I18n.tr("General")), 
        SHARING(I18n.tr("Sharing")),
        TRACKERS(I18n.tr("Trackers")),
        TRANSFERS(I18n.tr("Transfers")),
        PIECES(I18n.tr("Pieces")), 
        BITTORENT(I18n.tr("Files")),
        LIMITS(I18n.tr("Limits"));
        
        private final String name;
        
        Tabs(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    };

    @Resource Color backgroundColor;
    @Resource Color borderColor;
    @Resource Font buttonFont;
    @Resource Color fontColor;
    @Resource Color fontSelectedColor;
    
    @Resource Color selectionTopGradientColor;
    @Resource Color selectionBottomGradientColor;
    @Resource Color selectionBorderTopColor;
    @Resource Color selectionBorderBottomColor;
    @Resource Color highlightBackgroundColor;
    @Resource Color highlightBorderColor;
    
    private final JPanel component;
    private final FancyTabList tabList;
    private final List<TabActionMap> tabActionMaps;
    private final List<FileInfoTabListener> listeners;
    
    @Inject
    public FileInfoTabPanel() {
        tabActionMaps = new ArrayList<TabActionMap>();
        component = new JPanel(new MigLayout("insets 0 14 0 5, gap 0, fill", "", "[28!]"));
        tabList = new FancyTabList(tabActionMaps);
        listeners = new CopyOnWriteArrayList<FileInfoTabListener>();
        
        GuiUtils.assignResources(this);
        
        init();
    }
    
    private void init() {
        component.setBackground(backgroundColor);
        component.setBorder(BorderFactory.createMatteBorder(0,0,1,0, borderColor));
        
        tabList.setSelectionPainter(new CategoryTabPainter(selectionTopGradientColor, selectionBottomGradientColor, selectionBorderTopColor, selectionBorderBottomColor));
        tabList.setHighlightPainter(new CategoryTabPainter(highlightBackgroundColor, highlightBackgroundColor, highlightBorderColor, highlightBorderColor));
        
        tabList.setTabTextColor(fontColor);
        tabList.setTextFont(buttonFont);
        tabList.setTabTextSelectedColor(fontSelectedColor);
        tabList.setUnderlineEnabled(false);
        
        component.add(tabList, "growy");
    }
    
    /**
     * Returns the JComponent that displays the tabs. 
     */
    public JComponent getComponent() {
        return component;
    }
    
    /**
     * Adds a listener to the tabs that is notified when a new tab is selected.
     */
    public void addSearchTabListener(FileInfoTabListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Replaces the current list of tabs with this list. The tabs
     * are added in order of index of the list.
     */
    public void setTabs(List<Tabs> tabs) {
        tabActionMaps.clear();
        for(Tabs tab: tabs) {
            addTab(tab);
        }
        tabList.setTabActionMaps(tabActionMaps);
    }
    
    private void addTab(Tabs tab) {
        FileInfoTabAction action = new FileInfoTabAction(tab);
        if(tab == Tabs.GENERAL) {
            action.putValue(Action.SELECTED_KEY, true);
        }
        TabActionMap map = newTabActionMap(action);
        tabActionMaps.add(map);
    }
    
    private TabActionMap newTabActionMap(FileInfoTabAction action) {
        Action moreText = new NoOpAction();
        moreText.putValue(Action.NAME, "");
        return new TabActionMap(action, null, moreText, null);
    }
    
    static interface FileInfoTabListener {
        void tabSelected(Tabs tab);
    }
    
    private class FileInfoTabAction extends AbstractAction {
        private final Tabs tab;
        
        public FileInfoTabAction(Tabs tab) {
            super(tab.getName());
            
            this.tab = tab;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            for(FileInfoTabListener listener : listeners) {
                listener.tabSelected(tab);
            }
        }
        
    }
    
    /**
     * Creates a Painter used to render the selected category tab.
     */  
    //TODO: duplicate code of LibraryFilterPanel
    private static class CategoryTabPainter extends RectanglePainter<FancyTab> {
        
        public CategoryTabPainter(Color topGradient, Color bottomGradient, Color topBorder,
                Color bottomBorder) {
            setFillPaint(new GradientPaint(0, 0, topGradient, 0, 1, bottomGradient));
            setBorderPaint(new GradientPaint(0, 0, topBorder, 0, 1, bottomBorder));
            
            setRoundHeight(10);
            setRoundWidth(10);
            setRounded(true);
            setPaintStretched(true);
            setInsets(new Insets(2,0,1,0));
                    
            setAntialiasing(true);
            setCacheable(true);
        }
    }
}
