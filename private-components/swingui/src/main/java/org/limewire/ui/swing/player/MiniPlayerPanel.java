package org.limewire.ui.swing.player;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.TextComponent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class MiniPlayerPanel extends JPanel {
  
    @Resource
    private Icon pauseIcon;
    @Resource
    private Icon pauseIconPressed;
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon playIcon;
    @Resource
    private Icon playIconPressed;
    @Resource
    private Icon playIconRollover;
    @Resource 
    private Color foregroundColor;
    @Resource
    private Font font;

    private JButton playPauseButton;

    private MarqueeButton statusButton;
    
    private final Provider<PlayerMediator> playerMediator;
    private final LibraryMediator libraryMediator;
    private boolean isInitialized = false;

    @Inject
    public MiniPlayerPanel(Provider<PlayerMediator> playerMediator, LibraryMediator libraryMediator) {
        super(new MigLayout("insets 0", "4[][]", "0[]0"));

        this.playerMediator = playerMediator;
        this.libraryMediator = libraryMediator;
    }
    
    private void initialize() {
        if(!isInitialized) {
            isInitialized = true;
            
            GuiUtils.assignResources(this);
            
            setOpaque(false);

            playPauseButton = new JButton();
            playPauseButton.setMargin(new Insets(0, 0, 0, 0));
            playPauseButton.setBorderPainted(false);
            playPauseButton.setContentAreaFilled(false);
            playPauseButton.setFocusPainted(false);
            playPauseButton.setRolloverEnabled(true);
            playPauseButton.setIcon(playIcon);
            playPauseButton.setFocusable(false);
            Dimension playPauseDimensions = new Dimension(playIcon.getIconWidth(), playIcon.getIconHeight());
            playPauseButton.setMaximumSize(playPauseDimensions);
            playPauseButton.setPreferredSize(playPauseDimensions);
            playPauseButton.setRolloverIcon(playIconRollover);
            playPauseButton.setPressedIcon(playIconPressed);
            playPauseButton.setHideActionText(true);
            playPauseButton.addActionListener(new PlayListener());

            statusButton = new MarqueeButton(I18n.tr("Nothing selected"), 16);
            Dimension statusButtonDimensions = new Dimension(Integer.MAX_VALUE, playIcon.getIconHeight());
            statusButton.setMaximumSize(statusButtonDimensions);
            statusButton.setFont(font);
            statusButton.setForeground(foregroundColor);    
            statusButton.addActionListener(new ShowPlayerListener());

            add(playPauseButton, "gapbottom 0, gaptop 0");
            add(statusButton, "gapbottom 0, gaptop 0");
         
            setMaximumSize(getPreferredSize());

            //we are using AWTEventListener rather than key binding so that other focused components won't consume the event
            Toolkit.getDefaultToolkit().addAWTEventListener(new SpacebarPlaybackToggler(), AWTEvent.KEY_EVENT_MASK);
        }
    }
    
    @Inject
    void register() {
        playerMediator.get().addMediatorListener(new PlayerListener());
        
        //hide the player if setting is disabled
        SwingUiSettings.PLAYER_ENABLED.addSettingListener(new SettingListener(){
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        MiniPlayerPanel.this.setVisible(false);
                    }
                });
            }
        });
    }

    private class ShowPlayerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            File currentFile = playerMediator.get().getCurrentMediaFile();
            
            if (currentFile != null) { 
                libraryMediator.selectInLibrary(currentFile);
            }
        }
    }
    
    private class PlayListener implements ActionListener {  
        @Override
        public void actionPerformed(ActionEvent e) {       
            setPlaying(!isPlaying());                        
        }

    }
    
    private boolean isPlaying(){
        return playerMediator.get().getStatus() == PlayerState.PLAYING || 
                playerMediator.get().getStatus() == PlayerState.SEEKING_PLAY ;
    }
    
    private void setPlaying(boolean playing){
        if (playing){
            playerMediator.get().resume();
        } else {
            playerMediator.get().pause();
        }
    }
    
    private class PlayerListener implements PlayerMediatorListener {
         @Override
        public void progressUpdated(float progress) {
        }

        @Override
        public void mediaChanged(String name) {
            initialize();
            //Show MiniPlayer when song is opened
            statusButton.setText(name);
            statusButton.setToolTipText(name);
            if(!isVisible())
                setVisible(true);
            statusButton.start();
        }

        @Override
        public void stateChanged(PlayerState state) {
            initialize();
            if (state == PlayerState.PLAYING || state == PlayerState.RESUMED){
                playPauseButton.setIcon(pauseIcon);
                playPauseButton.setRolloverIcon(pauseIconRollover);
                playPauseButton.setPressedIcon(pauseIconPressed);
                statusButton.start(); 
            } else if (state == PlayerState.EOM || state == PlayerState.UNKNOWN || state == PlayerState.NO_SOUND_DEVICE) {
                setVisible(false);
                statusButton.stop();
            } else if(state == PlayerState.PAUSED || state == PlayerState.STOPPED){
                playPauseButton.setIcon(playIcon);
                playPauseButton.setRolloverIcon(playIconRollover);
                playPauseButton.setPressedIcon(playIconPressed);
                statusButton.stop();
            }
        }
        
    }
    
    private class SpacebarPlaybackToggler implements AWTEventListener {
        
        @Override
        public void eventDispatched(AWTEvent event) {
            if(shouldTogglePlayback(event)){
                setPlaying(!isPlaying());                        
            }
        }
        
        private boolean shouldTogglePlayback(AWTEvent event){
            if (isSpacePressed(event)) {
                return MiniPlayerPanel.this.isVisible() && !isTextComponentFocused();
            }            
            return false;
        }
        
        private boolean isSpacePressed(AWTEvent event) {
            if (event instanceof KeyEvent) {
                KeyEvent keyEvent = (KeyEvent) event;
                
                return keyEvent.getKeyCode() == KeyEvent.VK_SPACE
                        && keyEvent.getID() == KeyEvent.KEY_PRESSED;
            }
            
            return false;
        }
        
        private boolean isTextComponentFocused(){
            Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            return focusOwner instanceof TextComponent || focusOwner instanceof JTextComponent;
        }
    }
}
