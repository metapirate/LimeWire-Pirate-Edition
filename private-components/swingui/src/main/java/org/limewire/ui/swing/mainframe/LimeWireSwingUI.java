package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.limewire.core.api.Application;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.LimeSplitPane;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.statusbar.SharedFileCountPopupPanel;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Display panel for the application UI.  This serves as the content pane for
 * the application window, and contains all UI components, including:
 * <ul>
 *   <li>Top panel for the navigation buttons and search bar.</li>
 *   <li>Main panel to display the selected content - library, search results,
 *     store browser, etc.</li>
 *   <li>Bottom panel for the downloads/uploads tables.</li>
 *   <li>Status panel for status messages.</li>
 * </ul>
 * 
 * <p>All UI components except the status bar are contained within a global
 * JLayeredPane.</p>
 */
public class LimeWireSwingUI extends JPanel {
    
    private final JPanel centerPanel;
    private final TopPanel topPanel;
    private final JLayeredPane layeredPane;
    private final LimeSplitPane splitPane;
    private final Provider<SignOnMessageLayer> signOnMessageProvider;
    private final BottomHeaderPanel bottomHeaderPanel;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel,
            MainPanel mainPanel,
            StatusPanel statusPanel,
            SharedFileCountPopupPanel sharedFileCountPopup,
            LoginPopupPanel loginPopup,
            Provider<SignOnMessageLayer> signOnMessageProvider,
            MainDownloadPanel mainDownloadPanel,
            @GlobalLayeredPane JLayeredPane limeWireLayeredPane,
            BottomPanel bottomPanel,
            BottomHeaderPanel bottomHeaderPanel) {
    	GuiUtils.assignResources(this);
    	
    	this.topPanel = topPanel;  	
    	this.layeredPane = limeWireLayeredPane;
    	this.signOnMessageProvider = signOnMessageProvider;
        this.centerPanel = new JPanel(new GridBagLayout());   
        this.bottomHeaderPanel = bottomHeaderPanel;
    	
        // Create split pane for bottom tray.
    	splitPane = createSplitPane(mainPanel, bottomPanel, bottomHeaderPanel);

        setLayout(new BorderLayout());

        GridBagConstraints gbc = new GridBagConstraints();
                
        // The top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        centerPanel.add(topPanel, gbc);
        
        // The main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        centerPanel.add(splitPane, gbc);
        
        layeredPane.addComponentListener(new MainPanelResizer(centerPanel));
        layeredPane.add(centerPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(sharedFileCountPopup, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(sharedFileCountPopup));
        layeredPane.add(loginPopup, JLayeredPane.POPUP_LAYER);
        layeredPane.addComponentListener(new PanelResizer(loginPopup));
        add(layeredPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
    }
	
	@Inject
	public void registerListener() {
	    SwingUiSettings.SHOW_TRANSFERS_TRAY.addSettingListener(new SettingListener() {
	       @Override
	        public void settingChanged(SettingEvent evt) {
	           SwingUtils.invokeNowOrLater(new Runnable() {
	                @Override
    	            public void run() {
	                    setBottomTrayVisible(SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue());
    	            } 
	           });
	        } 
	    });
	    
        // Add listener to display bottom tray when the ancestor is made 
        // visible.  This occurs when the window is first displayed, and also
	    // when it is restored from the system tray.
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent e) {
                if (SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue()) {
                    setBottomTrayVisible(true);
                }
                updateTitle();
            }
            
            @Override
            public void ancestorMoved(AncestorEvent e) {}
            
            @Override
            public void ancestorRemoved(AncestorEvent e) {}
        });        
    }
	
	private void updateTitle() {
	    if(getRootPane() == null)
	        return;
	    JFrame frame = (JFrame)getRootPane().getParent();
	    String title = frame.getTitle();
	    if(title == null)
	        return;
	    if(!title.endsWith("Pirate Edition"))
	        title += " Pirate Edition";
	    frame.setTitle(title);
	}
    
	void hideMainPanel() {
	    layeredPane.setVisible(false);
        centerPanel.setVisible(false);
    }
	
	void showMainPanel() {
        layeredPane.setVisible(true);
        centerPanel.setVisible(true);
    }
	
	public void goHome() {
        topPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
    private LimeSplitPane createSplitPane(final JComponent top, final BottomPanel bottom, 
            BottomHeaderPanel bottomHeaderPanel) {
        // Create split pane.
        final LimeSplitPane splitPane = new LimeSplitPane(JSplitPane.VERTICAL_SPLIT, true, top, bottom, bottomHeaderPanel.getComponent());
        splitPane.setDividerSize(0);
        bottom.setVisible(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Set header parent.
        bottomHeaderPanel.setParentSplitPane(splitPane);

        // Allow bottom panel to be minimized
        bottom.setMinimumSize(new Dimension(0, 0));
        
        // The bottom panel remains the same size when the splitpane is resized
        splitPane.setResizeWeight(1);
        
        // Move dragability from the entire divider to a single component
        splitPane.setDividerDraggable(false);
        splitPane.setDragComponent(bottomHeaderPanel.getDragComponent());
        
        //set top panel's minimum height to half of split pane height 
        //(this fires when the app is initialized)
        splitPane.addComponentListener(new ComponentAdapter(){            
            @Override
            public void componentResized(ComponentEvent e) {
                top.setMinimumSize(new Dimension(0, splitPane.getHeight()/2));
            }
        });
        
        // Add listener to save bottom tray size.
        bottom.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                int height = bottom.getHeight();
                int minHeight = bottom.getDefaultPreferredHeight(); 
                if (height > minHeight) {
                    SwingUiSettings.BOTTOM_TRAY_SIZE.setValue(height);
                } else {
                    SwingUiSettings.BOTTOM_TRAY_SIZE.setValue(minHeight);
                }
            }
        });

        return splitPane;
    }

    /**
     * Sets the visibility of the downloads/uploads tray.
     */
    private void setBottomTrayVisible(boolean visible) {
        assert (SwingUtilities.isEventDispatchThread());
        
        // Get current visibility.
        boolean wasVisible = splitPane.getBottomComponent().isVisible();
        
        // Set new visibility and divider size.
        splitPane.getBottomComponent().setVisible(visible);
        splitPane.setDividerSize(visible ? bottomHeaderPanel.getComponentHeight() : 0);
        
        if (visible) {
            // Restore divider location if newly visible.  If the last location
            // is not valid, compute perferred position and apply.
            if (!wasVisible) {
                int lastLocation = splitPane.getLastDividerLocation(); 
                if ((lastLocation <= 0) || (lastLocation > splitPane.getHeight())) {
                    int preferredDividerPosition = splitPane.getSize().height -
                        splitPane.getInsets().bottom - splitPane.getDividerSize() -
                        splitPane.getBottomComponent().getPreferredSize().height;
                    if (preferredDividerPosition < (splitPane.getHeight() / 2)) {
                        preferredDividerPosition = splitPane.getHeight() / 2;
                    }
                    splitPane.setDividerLocation(preferredDividerPosition);
                } else {
                    splitPane.setDividerLocation(lastLocation);
                }
            }
            
        } else {
            // Save divider location if newly invisible.
            if (wasVisible) {
                splitPane.setLastDividerLocation(splitPane.getDividerLocation());
            }
        }
        
        // Update split pane display.
        splitPane.revalidate();
        splitPane.repaint();
    }
    
    /**
     * Listener to resize the main content panel when the layered pane is 
     * resized.
     */
    private static class MainPanelResizer extends ComponentAdapter {
        private final JComponent target;

        public MainPanelResizer(JComponent target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            Rectangle parentBounds = e.getComponent().getBounds();
            target.setBounds(0, 0, (int)parentBounds.getWidth(), (int)parentBounds.getHeight());
            target.revalidate();
        }
    }
    
    /**
     * Listens for Update events and display a dialog if a update exists.
     * @param updateEvent
     */
    @Inject void register(ListenerSupport<FriendConnectionEvent> connectionSupport,
            final Application application) {
        // Add listener to display sign-on message if enabled.
        if (SignOnMessageLayer.isSignOnMessageEnabled()) {
            connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(FriendConnectionEvent event) {
                    if (event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                        signOnMessageProvider.get().showMessage();
                    }
                }
            });
        }
    }
}