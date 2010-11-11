package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Class that handles the displaying of video.
 */
class VideoDisplayDirector {
    
    private final JLayeredPane limeWireLayeredPane;   
    
    private final Integer videoLayer = new Integer(JLayeredPane.DEFAULT_LAYER + 1);
    
    private VideoPanel videoPanel;
    
    private JFrame fullScreenFrame;
    private NavigationListener closeVideoOnNavigation;
    
    @Resource(key="WireframeTop.preferredSize") private Dimension topPanelPreferredSize;

    private final VideoPanelFactory videoPanelFactory;

    private final Navigator navigator;
    
    private final Provider<PlayerMediatorImpl> playerMediator;

    private VideoPanelResizer resizerListener;

    @Inject
    public VideoDisplayDirector(@GlobalLayeredPane JLayeredPane limeWireLayeredPane, Provider<PlayerMediatorImpl> videoPlayerMediator,
            VideoPanelFactory videoPanelFactory, Navigator navigator){
        this.limeWireLayeredPane = limeWireLayeredPane;
        this.playerMediator = videoPlayerMediator;
        this.videoPanelFactory = videoPanelFactory;
        this.navigator = navigator;
        
        GuiUtils.assignResources(this);
        assert(topPanelPreferredSize != null);
        
        resizerListener = new VideoPanelResizer();
    }
    
    /**
     * Prepares a new videoRenderer to be displayed.
     * 
     * @param videoRenderer the Container that will be displayed in a PlayerPanel.
     * @param isFullScreen true for fullscreen, false to embed in the client frame
     */
    public void initialize(Container videoRenderer, boolean isFullScreen){
        boolean isReinitializing = false;
        
        if(this.videoPanel != null){
            isReinitializing = true;
            close();
        }
        //Recycling the video panel causes problems with native painting.  We need a new one each time.
        videoPanel = videoPanelFactory.createVideoPanel(videoRenderer);
        
        // If the start screen is open, then open up the library view before showing the video.
        // The start screen has a heavy weight component that would cut off the video.
        NavItem selectedNavItem = navigator.getSelectedNavItem();
        if (selectedNavItem == null || selectedNavItem.getId().equals(HomeMediator.NAME)) {
            NavItem item = navigator.getNavItem(NavCategory.LIBRARY, LibraryMediator.NAME);
            item.select();
        }
        
        if(isFullScreen){
            initializeFullScreen();
        } else {
            initializeInClient(isReinitializing);
        }            
    }
    
    private void initializeInClient(boolean isReinitializing){
        limeWireLayeredPane.add(videoPanel.getComponent(), videoLayer);
        registerNavigationListener();
        limeWireLayeredPane.addComponentListener(resizerListener);
        //If we are reinitializing (for example returning from full screen) we want to show the panel immediately
        if (isReinitializing) {
            showInClient();
        } else {
            videoPanel.getComponent().setVisible(false);
        }
    }   
    
    private void registerNavigationListener() {
        if (closeVideoOnNavigation == null) {
            closeVideoOnNavigation = new CloseVideoOnNavigationListener();
        }

        navigator.addNavigationListener(closeVideoOnNavigation);
    }
    
    
    private void initializeFullScreen(){
        //Hide main frame first so that fullScreenFrame gets proper focus.
        GuiUtils.getMainFrame().setVisible(false);
        
        fullScreenFrame = new LimeJFrame();

        fullScreenFrame.setTitle(GuiUtils.getMainFrame().getTitle());
        // fullScreenFrame.setAlwaysOnTop(true) and
        // SystemUtils.setWindowTopMost(fullScreenFrame) don't play nicely with
        // dialog boxes so we aren't using them here.
       
        fullScreenFrame.setUndecorated(true);
        fullScreenFrame.add(videoPanel.getComponent(), BorderLayout.CENTER);            

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        fullScreenFrame.setBounds(0,0,screenSize.width, screenSize.height);        

        //addNotify is necessary here so that a null pointer doesn't happen in sun's awt code 
        //when a canvas is added to videoRenderer
        fullScreenFrame.addNotify();
    }
    
    /**
     * Displays the video as {@link #initialize(Container, boolean) initialized}.
     */
    public void show(){
        if(isFullScreen()){
            showFullScreen();
        } else {
            showInClient();
        }
        videoPanel.playerLoaded();
    }


    private void showInClient() {
        videoPanel.getComponent().setVisible(true);
        resizeVideoContainer();     
        //Make sure the flash of native video window doesn't steal focus
        GuiUtils.getMainFrame().toFront();   
        videoPanel.requestFocus();
    }

    private void showFullScreen() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        
        if (OSUtils.isMacOSX() && device.isFullScreenSupported()) {
            device.setFullScreenWindow(fullScreenFrame);
        } else {
            fullScreenFrame.setVisible(true);
            fullScreenFrame.toFront();
        }        
    }

    public boolean isFullScreen() {
        return fullScreenFrame != null;
    }
    
    public void close() {
        if (isFullScreen()) {
            closeFullScreen();
            videoPanel.dispose();
        } else if(videoPanel != null) {
            closeInClient();
            videoPanel.dispose();
        }
        videoPanel = null;
        //Force a repaint on close - gets rid of artifacts (especially noticable on Mac)
        GuiUtils.getMainFrame().repaint();
    }    
    
    private void closeFullScreen() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        if (OSUtils.isMacOSX() && device.isFullScreenSupported()) {
            device.setFullScreenWindow(null);
        }
        fullScreenFrame.setVisible(false);
        //manually dispose of the frame so that everything gets garbage collected.
        fullScreenFrame.dispose();
        fullScreenFrame = null;
        GuiUtils.getMainFrame().setVisible(true);
    }

    private void closeInClient(){   
        limeWireLayeredPane.removeComponentListener(resizerListener);
        limeWireLayeredPane.remove(videoPanel.getComponent()); 
        removeNavigationListener();
    }
    
    private void removeNavigationListener(){
        navigator.removeNavigationListener(closeVideoOnNavigation);
    }

    
    private class VideoPanelResizer extends ComponentAdapter {        
        @Override
        public void componentResized(ComponentEvent e) {
            resizeVideoContainer();
        }
    }  
    

    private void resizeVideoContainer() {
        if (videoPanel != null) {
            Rectangle parentBounds = videoPanel.getComponent().getParent().getBounds();
            // TODO: this knows too much about the layered pane's layout
            videoPanel.getComponent().setBounds(0, (int)topPanelPreferredSize.getHeight(), (int)parentBounds.getWidth(), 
                    (int)parentBounds.getHeight() - (int)topPanelPreferredSize.getHeight());
            videoPanel.getComponent().revalidate();
        }
    }
    
    private class CloseVideoOnNavigationListener implements NavigationListener {

        @Override
        public void itemSelected(NavCategory category, NavItem navItem,
                NavSelectable selectable, NavMediator navMediator) {
            playerMediator.get().stop();                    
        }

        @Override public void categoryAdded(NavCategory category) {}
        @Override public void categoryRemoved(NavCategory category, boolean wasSelected) {}
        @Override public void itemAdded(NavCategory category, NavItem navItem) {}
        @Override public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected) {}
    }
}
