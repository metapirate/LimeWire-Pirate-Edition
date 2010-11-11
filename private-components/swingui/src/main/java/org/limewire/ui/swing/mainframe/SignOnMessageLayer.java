package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventUtils;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.components.decorators.MessageDecorator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.LibraryTableRect;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.sharing.LibrarySharingEvent;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Container for overlay messages that are displayed when the user signs on 
 * for private sharing.
 */
class SignOnMessageLayer {
    /** Message type definitions. */
    private enum Type {
        NONE, LIBRARY, SHARING;
    }

    @Resource private Color messageForeground;
    @Resource private Font libraryFont;
    @Resource private Font sharingFont;
    @Resource private Icon addFileIcon;
    @Resource private Icon checkedIcon;
    @Resource private Icon closeIcon;
    @Resource private Icon closeHoverIcon;
    @Resource private Icon upArrowIcon;
    @Resource private Icon leftArrowIcon;
    
    private final JLayeredPane layeredPane;
    private final TopPanel topPanel;
    private final Navigator navigator;
    private final Provider<LibraryNavigatorPanel> libraryNavProvider;
    private final LibraryMediator libraryMediator;
    private final EventListenerList<LibrarySharingEvent> libraryListenerList;
    private final Provider<Rectangle> libraryTableRect;
    private final EventBean<FriendConnectionEvent> connectionEventBean;
    private final Provider<MessageDecorator> messageDecoratorProvider;
    
    /** Current message component. */
    private Component messageComponent;
    
    /** Type of current message. */
    private Type messageType = Type.NONE;
    
    /** Listener to handle layered pane resizing. */
    private PanelResizer panelResizer;
    
    /** Listener to handle library sharing events. */
    private EventListener<LibrarySharingEvent> libraryListener;
    
    /**
     * Constructs a SignOnMessagePanel with the specified services.
     */
    @Inject
    public SignOnMessageLayer(
            @GlobalLayeredPane JLayeredPane limeWireLayeredPane,
            TopPanel topPanel,
            Navigator navigator,
            Provider<LibraryNavigatorPanel> libraryNavProvider,
            LibraryMediator libraryMediator,
            EventListenerList<LibrarySharingEvent> libraryListenerList,
            @LibraryTableRect Provider<Rectangle> libraryTableRect,
            EventBean<FriendConnectionEvent> connectionEventBean,
            Provider<MessageDecorator> messageDecoratorProvider) {
        
        this.layeredPane = limeWireLayeredPane;
        this.topPanel = topPanel;
        this.navigator = navigator;
        this.libraryMediator = libraryMediator;
        this.libraryNavProvider = libraryNavProvider;
        this.libraryListenerList = libraryListenerList;
        this.libraryTableRect = libraryTableRect;
        this.connectionEventBean = connectionEventBean;
        this.messageDecoratorProvider = messageDecoratorProvider;
        
        GuiUtils.assignResources(this);
    }
    
    /**
     * Registers listeners for service events.  This method should be called
     * post-construction.
     */
    @Inject
    void register() {
        // Add listener to handle Navigator selection events.
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void categoryAdded(NavCategory category) {
            }

            @Override
            public void categoryRemoved(NavCategory category, boolean wasSelected) {
            }

            @Override
            public void itemAdded(NavCategory category, NavItem navItem) {
            }

            @Override
            public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected) {
            }

            @Override
            public void itemSelected(NavCategory category, NavItem navItem,
                    NavSelectable selectable, NavMediator navMediator) {
                //TODO: all this logic is extremely brittle
                switch (messageType) {
                case LIBRARY:
                    // If library message showing and Library selected, close message.
                    if (category == NavCategory.LIBRARY) {
                        hideMessage();
                    }
                    break;
                case SHARING:
                    // If sharing message showing and Library de-selected, close message.
                    if (category != NavCategory.LIBRARY) {
                        hideMessage();
                    }
                    break;
                }
                // If Private Shared selected and signed on, show message.
                NavType selectedType = getSelectedNavType();
                if ((category == NavCategory.LIBRARY) && 
                        (selectedType == NavType.LIST) && isSignedOn()) {
                    showMessage();
                }
            }
        });
        
        // Add listener to handle LibraryNavigator selection events.
        libraryNavProvider.get().addTableSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                NavType navType = getSelectedNavType();
                //TODO: all this logic is extremely brittle
                switch (messageType) {
                case LIBRARY:
                    // If library message showing and Private Shared selected, close message.
                    if (navType != NavType.LIBRARY) {
                        hideMessage();
                    }
                    // If Private Shared selected and signed on, show message.
                    if ((navType == NavType.LIST) && isSignedOn()) {
                        showMessage();
                    }
                    break;
                    
                case SHARING:
                    // If sharing message showing and Private Shared not selected, close message.
                    if (navType != NavType.LIST) {
                        hideMessage();
                    }
                    break;
                    
                case NONE:
                    // If Private Shared selected and signed on, show message.
                    if ((navType == NavType.LIST) && isSignedOn()) {
                        showMessage();
                    }
                    break;
                }
            }
        });
    }
    
    /**
     * Returns true if there are any pending sign-on messages.
     */
    public static boolean isSignOnMessageEnabled() {
        return SwingUiSettings.SHOW_LIBRARY_OVERLAY_MESSAGE.getValue() ||
                SwingUiSettings.SHOW_SHARING_OVERLAY_MESSAGE.getValue();
    }
    
    /**
     * Returns true if the user is signed on to any service.
     */
    private boolean isSignedOn() {
        FriendConnection connection = EventUtils.getSource(connectionEventBean);
        return ((connection != null) && connection.isLoggedIn());
    }
    
    /**
     * Displays a sign-on message using the specified layered pane.
     */
    public void showMessage() {
        // Retrieve message settings.
        boolean showLibraryMsg = SwingUiSettings.SHOW_LIBRARY_OVERLAY_MESSAGE.getValue();
        boolean showSharingMsg = SwingUiSettings.SHOW_SHARING_OVERLAY_MESSAGE.getValue();
        
        // Get Library item from navigator.
        NavItem libraryItem = navigator.getNavItem(NavCategory.LIBRARY, LibraryMediator.NAME);
        
        // Get selected collection in library.
        NavType selectedType = getSelectedNavType();
        
        // Remove old message components.
        if (panelResizer != null) {
            layeredPane.removeComponentListener(panelResizer);
            panelResizer = null;
        }
        if (messageComponent != null) {
            layeredPane.remove(messageComponent);
            messageComponent = null;
        }
        
        if ((libraryItem == null) || !libraryItem.isSelected()) {
            // Library not selected so show message pointing to Library.
            if (showLibraryMsg) {
                Point location = new Point(16, topPanel.getSize().height - 4);
                messageComponent = new LibraryMessagePanel(location, false);
                messageType = Type.LIBRARY;
            }
            
        } else if ((selectedType == NavType.LIBRARY) || (selectedType == NavType.PUBLIC_SHARED)) {
            // Library selected so show message pointing to Private Shared.
            if (showLibraryMsg) {
                LibraryNavigatorPanel libraryNavPanel = libraryNavProvider.get();
                Point location = new Point(libraryNavPanel.getSize().width - 6, 75);
                messageComponent = new LibraryMessagePanel(location, true);
                messageType = Type.LIBRARY;
            }
            
        } else {
            // Disable library overlay message.
            SwingUiSettings.SHOW_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            
            // Private Shared selected so show message about sharing files.
            if (showSharingMsg) {
                messageComponent = new SharingMessagePanel();
                messageType = Type.SHARING;
                // Add listener for Library events to dismiss message.
                libraryListener = new EventListener<LibrarySharingEvent>() {
                    @Override
                    @SwingEDTEvent
                    public void handleEvent(LibrarySharingEvent event) {
                        hideMessage();
                    }
                };
                libraryListenerList.addListener(libraryListener);
            }
        }
        
        // Exit if no message to display.
        if (messageComponent == null) {
            messageType = Type.NONE;
            return;
        }
        
        // Add new message to layered pane.
        panelResizer = new PanelResizer((Resizable) messageComponent);
        layeredPane.add(messageComponent, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(panelResizer);
        layeredPane.validate();
        
        // Show message.
        messageComponent.setVisible(true);
        ((Resizable) messageComponent).resize();
    }
    
    private NavType getSelectedNavType() {
        // Get selected collection in library.
        LibraryNavItem selectedItem = libraryMediator.getSelectedNavItem();
        
        NavType selectedType;
        if(selectedItem == null)
            selectedType = NavType.LIBRARY;
        else
            selectedType = selectedItem.getType();
        return selectedType;
    }
    
    /**
     * Hides the current sign-on message, and disables the message so it will
     * not be displayed again.
     */
    public void hideMessage() {
        // Hide message and validate layered pane.
        if(panelResizer != null) {
            layeredPane.removeComponentListener(panelResizer);
            panelResizer = null;
        }
        if(messageComponent != null) {
            messageComponent.setVisible(false);
            layeredPane.remove(messageComponent);
            messageComponent = null;
        }
        layeredPane.validate();

        // Remove library listener.
        if (libraryListener != null) {
            libraryListenerList.removeListener(libraryListener);
            libraryListener = null;
        }
        
        // Disable overlay messages.
        switch (messageType) {
        case LIBRARY:
            SwingUiSettings.SHOW_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            break;
        case SHARING:
            SwingUiSettings.SHOW_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            SwingUiSettings.SHOW_SHARING_OVERLAY_MESSAGE.setValue(false);
            break;
        }
        messageType = Type.NONE;
    }
    
    /**
     * Creates a close button.
     */
    private JButton createCloseButton() {
        JButton closeButton = new JButton();
        
        closeButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setIcon(closeIcon);
        closeButton.setRolloverIcon(closeHoverIcon);
        closeButton.setRolloverEnabled(true);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideMessage();
            }
        });
        
        // Install listener to display rollover cursor.
        new RolloverCursorListener().install(closeButton);
        
        return closeButton;
    }
    
    /**
     * Container for Library overlay message that may be displayed when the 
     * user signs on for private sharing.  LibraryMessagePanel extends 
     * java.awt.Panel because the message may be displayed on top of the 
     * browser component.
     */
    private class LibraryMessagePanel extends Panel implements Resizable {
        private final Point messageLocation;
        private JXPanel messagePanel;
        private JButton closeButton;
        
        public LibraryMessagePanel(Point messageLocation, boolean librarySelected) {
            super(new BorderLayout());
            
            this.messageLocation = messageLocation;
           
            messagePanel = new JXPanel(new MigLayout("insets 3 3 3 3, gap 0!"));
            messagePanel.setOpaque(false);
            messageDecoratorProvider.get().decorateGreenRectangleMessage(messagePanel);
            
            // Create message components.
            closeButton = createCloseButton();
            
            JLabel arrowLabel = new JLabel();
            arrowLabel.setIcon(librarySelected ? leftArrowIcon : upArrowIcon);
            
            JLabel messageLabel = new JLabel();
            messageLabel.setFont(libraryFont);
            messageLabel.setForeground(messageForeground);
            messageLabel.setText(I18n.tr("Share with friends using your Private Shared list"));
            
            // Add components to container.
            if (librarySelected) {
                messagePanel.add(arrowLabel, "gap 6 0 0 0, aligny center");
            } else {
                messagePanel.add(arrowLabel, "gap 6 0 6 0, aligny top");
            }
            messagePanel.add(messageLabel, "gap 6 6 10 10, aligny center");
            messagePanel.add(closeButton , "aligny top");
            
            add(messagePanel, BorderLayout.CENTER);
        }

        @Override
        public void resize() {
            // Get preferred size.
            Dimension preferredSize = getPreferredSize();

            // Set bounds to position message in layered pane.
            setBounds(messageLocation.x, messageLocation.y, preferredSize.width, preferredSize.height);
        }
    }
    
    /**
     * Container for Sharing overlay message that may be displayed when the 
     * user signs on for private sharing.  SharingMessagePanel extends 
     * javax.swing.JPanel because it is displayed over the library table with 
     * a transparent background. 
     */
    private class SharingMessagePanel extends JPanel implements Resizable {
        private MessageComponent messageComponent;
        private JButton closeButton;
        private JLabel leftMessageLabel;
        private JLabel rightMessageLabel;

        public SharingMessagePanel() {
            setLayout(new MigLayout("insets 0 0 0 0"));
            setOpaque(false);
            
            // Create message component.
            messageComponent = new MessageComponent(5, 5, 18, 8);
            messageDecoratorProvider.get().decorateGreenMessage(messageComponent);
            
            // Create message elements.
            closeButton = createCloseButton();
            
            leftMessageLabel = new JLabel();
            leftMessageLabel.setFont(sharingFont);
            leftMessageLabel.setForeground(messageForeground);
            leftMessageLabel.setIcon(checkedIcon);
            leftMessageLabel.setText(I18n.tr("Share this list with friends"));
            
            rightMessageLabel = new JLabel();
            rightMessageLabel.setFont(sharingFont);
            rightMessageLabel.setForeground(messageForeground);
            rightMessageLabel.setIcon(addFileIcon);
            rightMessageLabel.setText(I18n.tr("Add files to it"));
            
            // Add message elements to component.
            messageComponent.addComponent(closeButton, "span 2, align right, push, wrap");
            //messageComponent.addComponent(titleLabel, "gap 15 15, span 2, align left, wrap");
            messageComponent.addComponent(leftMessageLabel , "gap 18 6, align left, wrap");
            messageComponent.addComponent(rightMessageLabel, "gap 15 15, align left");
            //messageComponent.addComponent(moreMessageLabel , "gap 15 15, span 2, align left");
            
            // Add message component to container.
            add(messageComponent, "pos 0.5al 0.5al");
        }
        
        @Override
        public void resize() {
            // Post event to perform delayed resizing.  This corrects an issue
            // with component sizes when the window is maximized or unmaximized.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Determine message location and size.
                    int y0 = topPanel.getSize().height;
                    Rectangle tableRect = libraryTableRect.get(); 
                    
                    // Set bounds to position message in layered pane.
                    setBounds(tableRect.x, y0 + tableRect.y, tableRect.width, tableRect.height);
                    
                    // Revalidate layered pane.
                    layeredPane.revalidate();
                }
            });
        }
    }
}
