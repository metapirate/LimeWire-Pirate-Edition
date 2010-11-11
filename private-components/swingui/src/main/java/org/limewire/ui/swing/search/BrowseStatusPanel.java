package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimePopupDialog;
import org.limewire.ui.swing.friends.refresh.AllFriendsRefreshManager;
import org.limewire.ui.swing.friends.refresh.BrowseRefreshStatus;
import org.limewire.ui.swing.friends.refresh.BrowseRefreshStatusListener;
import org.limewire.ui.swing.painter.ComponentBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
/**
 * Shows status updates for browses.  Must be disposed.
 */
public class BrowseStatusPanel extends JXPanel implements Disposable{

    private BrowseStatus status;

    private JButton warningButton;
    private JXPanel refreshPanel;
    private JButton refreshButton;
    private JLabel updatesLabel;
    
    @Resource private Icon warningIcon;    
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color innerBorder;
    @Resource private Color innerBackground;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    @Resource private Font font;
    @Resource private Color foreground;    
    @Resource private Color headerForeground;
    @Resource private Font headerFont;


    private final SearchResultsModel searchResultsModel;

    private final AllFriendsRefreshManager allFriendsRefreshManager;
    
    
    private BrowseRefreshStatusListener browseRefreshStatusListener;
    
    public BrowseStatusPanel(SearchResultsModel searchResultsModel, AllFriendsRefreshManager allFriendsRefreshManager){
        GuiUtils.assignResources(this);
        this.searchResultsModel = searchResultsModel;
        this.allFriendsRefreshManager = allFriendsRefreshManager;
        
        setOpaque(false);        
        initializeComponents();      
        if(isAllFriendsBrowse()){
            initializeAllFriendsListener();
        }
        layoutComponents();
        update();
    }
    
    private void initializeAllFriendsListener(){
        browseRefreshStatusListener = new BrowseRefreshStatusListener(){
            @Override
            public void statusChanged(final BrowseRefreshStatus status) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        refreshPanel.setVisible(status != BrowseRefreshStatus.REFRESHED);                              
                    }
                });
            }            
        };
        allFriendsRefreshManager.addBrowseRefreshStatusListener(browseRefreshStatusListener);        
    }

    private void initializeComponents() {        
        warningButton = new IconButton(warningIcon);
        warningButton.addActionListener(new WarningAction());
        warningButton.setVisible(false);
        
        refreshPanel = new JXPanel();
        refreshPanel.setOpaque(false);
        refreshPanel.setBackgroundPainter(createRefreshBackgroundPainter());
        refreshPanel.setVisible(false);
        
        updatesLabel = new JLabel(I18n.tr("There are updates!"));
        updatesLabel.setFont(font);
        updatesLabel.setForeground(foreground);
        
        refreshButton = new HyperlinkButton(new RefreshAction());
        refreshButton.setFont(font);
    }

    private void layoutComponents() {    
        refreshPanel.setLayout(new MigLayout("insets 6 8 6 8, gap 0, novisualpadding, aligny 50%, fill"));        
        
        refreshPanel.add(updatesLabel);
        refreshPanel.add(refreshButton, "gapleft 3");
        
        setLayout(new MigLayout("insets 0, gap 0, novisualpadding, filly"));
        add(warningButton);
        add(refreshPanel);
    }

    public void setBrowseStatus(BrowseStatus status) {
        this.status = status;
        update();
    }

    private void update() {
        if (!isAllFriendsBrowse()) {
            warningButton.setVisible(status != null && status.getState() == BrowseState.PARTIAL_FAIL);
            refreshPanel.setVisible(status != null && status.getState() == BrowseState.UPDATED);
        }
    }
    
    private void showFailedBrowses(){
        //TODO: present this properly
        List<String> friends = new ArrayList<String>();
        List<String> p2pUsers = new ArrayList<String>();
        
        for(Friend person : status.getFailedFriends()){
            if(person.isAnonymous()){
                p2pUsers.add(person.getRenderName());
            } else {
                friends.add(person.getRenderName());
            }            
        }
        
        JXPanel centerPanel = new JXPanel(new MigLayout("insets 0 5 5 5, gap 0, novisualpadding, fill"));
        if(friends.size() > 0){
            centerPanel.add(createHeaderLabel(I18n.tr("Friends")), "gaptop 5, wrap");
        }
        for (String name : friends){
            centerPanel.add(createItemLabel(name), "wrap");
        }
        
        if(p2pUsers.size() > 0){
            centerPanel.add(createHeaderLabel(I18n.tr("P2P Users")), "gaptop 5, wrap");
        }
        for (String name : p2pUsers){
            centerPanel.add(createItemLabel(name), "wrap");
        }
        
        LimePopupDialog popup = new LimePopupDialog(I18n.tr("Unable to Browse"), centerPanel);
        popup.showPopup(warningButton, 0, 0);
    }
    
    private JLabel createHeaderLabel(String text){
        JLabel header = new JLabel(text);
        header.setForeground(headerForeground);
        header.setFont(headerFont);
        return header;
    }
    
    private JLabel createItemLabel(String text){
        JLabel item = new JLabel(text);
        item.setForeground(foreground);
        item.setFont(font);
        return item;
    }
    
    private Painter<JXPanel> createRefreshBackgroundPainter() {
        return new ComponentBackgroundPainter<JXPanel>(innerBackground, innerBorder, bevelLeft, bevelTop1, 
                bevelTop2, bevelRight,bevelBottom, arcWidth, arcHeight,
                AccentType.NONE);
    }
    
    private boolean isAllFriendsBrowse(){
        return searchResultsModel.getSearchType() == SearchType.ALL_FRIENDS_BROWSE;
    }
        
    private class RefreshAction extends AbstractAction {
        public RefreshAction(){
            super(I18n.tr("Refresh"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if(isAllFriendsBrowse()){
                allFriendsRefreshManager.refresh();
            } else {            
                new DefaultSearchRepeater(status.getBrowseSearch(), searchResultsModel).refresh();
            }
        }        
    }
  
    
    private class WarningAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            showFailedBrowses();
        }        
    }


    @Override
    public void dispose() {
        if(browseRefreshStatusListener != null){        
            allFriendsRefreshManager.removeBrowseRefreshStatusListener(browseRefreshStatusListener);  
            browseRefreshStatusListener = null;
        }
    }
}
