package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.settings.UploadSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FancyTab;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dock.DockIconFactory;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.mainframe.BottomPanel.TabId;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.transfer.TransferTrayNavigator;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Control panel that is displayed above the downloads/uploads tables.
 */
@Singleton
public class BottomHeaderPanel implements TransferTrayNavigator {

    private static volatile int uploadsViewed = 0;
    
    @Resource private Icon moreButtonArrow;
    @Resource private Icon scrollPaneNubIcon;
    @Resource private Font hyperlinkFont;
    @Resource(key="HyperlinkButtonResources.foreground") private Color hyperlinkForeground;
    @Resource private Color highlightBackground;
    @Resource private Color highlightBorderColor;
    @Resource private Color selectionTopGradientColor;
    @Resource private Color selectionBottomGradientColor;
    @Resource private Color selectionTopBorderColor;
    @Resource private Color selectionBottomBorderColor;
    @Resource private Font textFont;
    @Resource private Color textForeground;
    @Resource private Color textSelectedForeground;

    private final DownloadMediator downloadMediator;
    private final UploadMediator uploadMediator;
    private final ComboBoxDecorator comboBoxDecorator;
    private final BandwidthCollector bandwidthCollector;
    private final BottomPanel bottomPanel;
    
    private final JXPanel component;
    private JComponent dragComponent;

    private Map<TabId, Action> actionMap = new EnumMap<TabId, Action>(TabId.class);
    private List<TabActionMap> tabActionList;
    
    private FancyTabList tabList;
    private JLabel titleTextLabel;
    private JPanel downloadButtonPanel;
    private JPanel uploadButtonPanel;
    private LimeComboBox downloadOptionsButton;
    private LimeComboBox uploadOptionsButton;
    
    private int componentHeight;
    private JSplitPane parentSplitPane;
    
    @Inject
    public BottomHeaderPanel(DownloadMediator downloadMediator,
            UploadMediator uploadMediator,
            ComboBoxDecorator comboBoxDecorator, 
            BarPainterFactory barPainterFactory, 
            DockIconFactory iconFactory,
            BandwidthCollector bandwidthCollector,
            BottomPanel bottomPanel) {
        
        this.downloadMediator = downloadMediator;
        this.uploadMediator = uploadMediator;
        this.comboBoxDecorator = comboBoxDecorator;
        this.bandwidthCollector = bandwidthCollector;
        this.bottomPanel = bottomPanel;
        
        GuiUtils.assignResources(this);
        hyperlinkFont = FontUtils.deriveUnderline(hyperlinkFont, true);
        
        component = new JXPanel(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding, fill"));
        component.setBackgroundPainter(barPainterFactory.createDownloadSummaryBarPainter());
        setComponentHeight(20);
        
        // initialize the dock icon since it registers as a Service
        iconFactory.createDockIcon();
        
        initialize();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    public JComponent getDragComponent() {
        return dragComponent;
    }
    
    private void initialize(){
        initializeComponents();
        initializeTabList();
        layoutComponents();
        updateSelection();
    }

    private void initializeComponents(){        
        titleTextLabel = new JLabel(I18n.tr("Downloads"));
        titleTextLabel.setFont(textFont);
        titleTextLabel.setForeground(textForeground);
        
        dragComponent = new JLabel(scrollPaneNubIcon);
        dragComponent.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));

        downloadButtonPanel = new JPanel(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding"));
        downloadButtonPanel.setOpaque(false);

        uploadButtonPanel = new JPanel(new MigLayout("insets 0 0 0 0, gap 0, novisualpadding"));
        uploadButtonPanel.setOpaque(false);
        
        initializeOptionsButton();

        // Install listener to show appropriate popup menu.
        component.addMouseListener(new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                // Determine popup menu.
                JPopupMenu popupMenu = null;
                if (downloadButtonPanel.isVisible()) {
                    popupMenu = downloadMediator.getHeaderPopupMenu();
                } else if (uploadButtonPanel.isVisible()) {
                    popupMenu = uploadMediator.getHeaderPopupMenu();
                }
                
                // Display popup menu.
                if (popupMenu != null) {
                    popupMenu.show(component, e.getX(), e.getY());
                }
            }
        });
    }
    
    private void layoutComponents(){
        List<JButton> downloadButtons = downloadMediator.getHeaderButtons();
        for (JButton button : downloadButtons) {
            button.setFont(hyperlinkFont);
            downloadButtonPanel.add(button, "gapafter 5");
        }
        downloadButtonPanel.add(downloadOptionsButton, "gapafter 5");
        
        List<JButton> uploadButtons = uploadMediator.getHeaderButtons();
        for (JButton button : uploadButtons) {
            button.setFont(hyperlinkFont);
            uploadButtonPanel.add(button, "gapafter 5");
        }
        uploadButtonPanel.add(uploadOptionsButton, "gapafter 5");
        
        component.add(tabList, "growy, push, hidemode 3");
        component.add(titleTextLabel, "gapbefore 5, push, hidemode 3");
        component.add(downloadButtonPanel, "hidemode 3");
        component.add(uploadButtonPanel, "hidemode 3");
        
        component.add(dragComponent, "pos 0.5al 0");
    }
        
    @Inject
    public void register(){
        // Add listeners to update header title.
        downloadMediator.getActiveList().addListEventListener(new LabelUpdateListListener());

        uploadMediator.getActiveList().addListEventListener(new ListEventListener<UploadItem>() {
            @Override
            public void listChanged(ListEvent<UploadItem> listChanges) {
                updateUploadTitle();
            }
        });
        
        SwingUiSettings.SHOW_TRANSFERS_TRAY.addSettingListener(new SettingListener() {
           @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtils.invokeNowOrLater(new Runnable() {
                   @Override
                    public void run() {
                       updateSelection();
                    }
               });
            } 
        });
        
        UploadSettings.SHOW_UPLOADS_IN_TRAY.addSettingListener(new SettingListener() {
           @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtils.invokeNowOrLater(new Runnable() {
                   @Override
                    public void run() {
                       updateSelection();
                    }
               });
            } 
        });
    }

    private void initializeOptionsButton(){
        // Create options button for downloads.
        downloadOptionsButton = new LimeComboBox();
        downloadOptionsButton.setText(I18n.tr("Options"));
        comboBoxDecorator.decorateMiniComboBox(downloadOptionsButton);
        downloadOptionsButton.setFocusable(false);
        
        downloadOptionsButton.setFont(hyperlinkFont);
        downloadOptionsButton.setIcon(moreButtonArrow);
        downloadOptionsButton.setForeground(hyperlinkForeground);
        ResizeUtils.forceHeight(downloadOptionsButton, 16);
        
        downloadOptionsButton.overrideMenu(downloadMediator.getHeaderPopupMenu());
        
        // Create options button for uploads.
        uploadOptionsButton = new LimeComboBox();
        uploadOptionsButton.setText(I18n.tr("Options"));
        comboBoxDecorator.decorateMiniComboBox(uploadOptionsButton);
        uploadOptionsButton.setFocusable(false);
        
        uploadOptionsButton.setFont(hyperlinkFont);
        uploadOptionsButton.setIcon(moreButtonArrow);
        uploadOptionsButton.setForeground(hyperlinkForeground);
        ResizeUtils.forceHeight(uploadOptionsButton, 16);
        
        uploadOptionsButton.overrideMenu(uploadMediator.getHeaderPopupMenu());
    }
    
    /**
     * Initializes the tab list to select content.
     */
    private void initializeTabList() {
        // Create actions for tab list.
        Action downloadAction = new ShowDownloadsAction();
        Action uploadAction = new ShowUploadsAction();
        actionMap.put(TabId.DOWNLOADS, downloadAction);
        actionMap.put(TabId.UPLOADS, uploadAction);
        tabActionList = TabActionMap.createMapForMainActions(downloadAction, uploadAction);
        
        // Create tab list.
        tabList = new FancyTabList(tabActionList);
        
        // Set tab list attributes.
        tabList.setTabTextColor(textForeground);
        tabList.setTextFont(textFont);
        tabList.setTabTextSelectedColor(textSelectedForeground);
        tabList.setUnderlineEnabled(false);
        tabList.setSelectionPainter(new TabPainter(selectionTopGradientColor, selectionBottomGradientColor, 
                selectionTopBorderColor, selectionBottomBorderColor));
        tabList.setHighlightPainter(new TabPainter(highlightBackground, highlightBackground, 
                highlightBorderColor, highlightBorderColor));
        
        //preselecting tabs so the cardlayout in BottomPanel is setup appropriately. 
        selectTab(TabId.UPLOADS);
        selectTab(TabId.DOWNLOADS);
    }
    
    /**
     * Returns the height of the header component.
     */
    public int getComponentHeight() {
        return componentHeight;
    }
    
    /**
     * Sets the height of the header component.
     */
    private void setComponentHeight(int height) {
        componentHeight = height;
        ResizeUtils.forceHeight(component, height);
        
        // Set the divider size of the split pane.
        if (parentSplitPane != null) {
            parentSplitPane.setDividerSize(height);
        }
    }
    
    /**
     * Sets the split pane for which the header serves as the divider.
     */
    public void setParentSplitPane(JSplitPane splitPane) {
        this.parentSplitPane = splitPane;
    }
    
    /**
     * Selects the tab for the specified tab id.
     */
    public void selectTab(TabId tabId) {
        Action mainAction = actionMap.get(tabId);
        List<FancyTab> tabs = tabList.getTabs();
        for (FancyTab tab : tabs) {
            if (mainAction == tab.getTabActionMap().getMainAction()) {
                tab.select();
                break;
            }
        }
    }
    
    /**
     * Selects the content for the specified tab id.
     */
    private void select(TabId tabId) {
        bottomPanel.show(tabId);
        updateHeader(tabId);
    }
    
    /**
     * Updates the header title and controls.
     */
    private void updateHeader(TabId tabId) {
        switch (tabId) {
        case DOWNLOADS:
            updateDownloadTitle();
            downloadButtonPanel.setVisible(true);
            uploadButtonPanel.setVisible(false);
            break;
            
        case UPLOADS:
            updateUploadTitle();
            downloadButtonPanel.setVisible(false);
            uploadButtonPanel.setVisible(true);
            break;
        }
    }
    
    /**
     * Updates the component layout based on the visible tables.
     */
    private void updateLayout() {
        boolean downloadVisible = SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue();
        boolean uploadVisible  = UploadSettings.SHOW_UPLOADS_IN_TRAY.getValue();
        
        if (downloadVisible && uploadVisible) {
            tabList.setVisible(true);
            titleTextLabel.setVisible(false);
            setComponentHeight(26);
        } else {
            tabList.setVisible(false);
            titleTextLabel.setVisible(true);
            setComponentHeight(20);
        }
    }
    
    private void updateSelection() {
        boolean downloadVisible = SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue();
        boolean uploadVisible  = UploadSettings.SHOW_UPLOADS_IN_TRAY.getValue();
        if(tabList.getSelectedTab() == null || (downloadVisible && !uploadVisible)) {
            selectTab(TabId.DOWNLOADS);
        }
        updateLayout();
    }
    
    /**
     * Updates title for Downloads tray.
     */
    private void updateDownloadTitle() {
        String title;
        int size = downloadMediator.getActiveList().size();
        
        // Create title with size and bandwidth.
        if (SwingUiSettings.SHOW_TOTAL_BANDWIDTH.getValue()) {
            int bandwidth = bandwidthCollector.getCurrentDownloaderBandwidth();
            title = (size > 0) ? I18n.tr("Downloads ({0} at {1} KB/s)", size, bandwidth) : I18n.tr("Downloads");
        } else {
            title = (size > 0) ? I18n.tr("Downloads ({0})", size) : I18n.tr("Downloads");
        }

        // Apply title to tab action and label.
        actionMap.get(TabId.DOWNLOADS).putValue(Action.NAME, title);
        titleTextLabel.setText(title);
    }
    
    /**
     * Updates title for Uploads tray.
     */
    private void updateUploadTitle() {
        String title;
        int size = uploadMediator.getActiveList().size();
        
        // Create title with size and bandwidth.
        if (SwingUiSettings.SHOW_TOTAL_BANDWIDTH.getValue()) {
            int bandwidth = bandwidthCollector.getCurrentUploaderBandwidth();
            title = (size > 0) ? I18n.tr("Uploads ({0} at {1} KB/s)", size, bandwidth) : I18n.tr("Uploads");
        } else {
            title = (size > 0) ? I18n.tr("Uploads ({0})", size) : I18n.tr("Uploads");
        }
        
        // Apply title to tab action and label.
        actionMap.get(TabId.UPLOADS).putValue(Action.NAME, title);
    }
    
    /**
     * Listener to update tab and header title when download list changes. 
     */
    private class LabelUpdateListListener implements ListEventListener<DownloadItem> {       
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            updateDownloadTitle();
        }
    }
    
    /**
     * Action to display downloads table.
     */
    private class ShowDownloadsAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            select(TabId.DOWNLOADS);
        }
    }
    
    /**
     * Action to display uploads table.
     */
    private class ShowUploadsAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            select(TabId.UPLOADS);
            uploadsViewed++;
        }
    }
    
    /**
     * A Painter used to render the selected or highlighted tab.
     */  
    private static class TabPainter extends RectanglePainter<FancyTab> {
        
        public TabPainter(Color topGradient, Color bottomGradient, 
                Color topBorder, Color bottomBorder) {
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
    
    private boolean isSelectedTab(TabId tabId) {
        Action mainAction = actionMap.get(tabId);
        FancyTab tab = tabList.getSelectedTab();
        if (tab != null && mainAction == tab.getTabActionMap().getMainAction()) {
           return true;
        }
        return false;
    }

    @Override
    public void selectDownloads() {
        showTray();
        selectTab(TabId.DOWNLOADS);        
    }

    @Override
    public void selectUploads() {
        showTray();
        UploadSettings.SHOW_UPLOADS_IN_TRAY.setValue(true);
        selectTab(TabId.UPLOADS);
    }

    @Override
    public void hideTray() {
        SwingUiSettings.SHOW_TRANSFERS_TRAY.setValue(false);
    }

    @Override
    public boolean isDownloadsSelected() {
        return isSelectedTab(TabId.DOWNLOADS);
    }

    @Override
    public boolean isTrayShowing() {
        return SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue();
    }

    @Override
    public boolean isUploadsSelected() {
        return isSelectedTab(TabId.UPLOADS);
    }

    @Override
    public void showTray() {
        SwingUiSettings.SHOW_TRANSFERS_TRAY.setValue(true);
    }
}
