package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicSliderUI;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeSliderBar;
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.components.VolumeSlider;
import org.limewire.ui.swing.components.decorators.SliderBarDecorator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.painter.ComponentBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Provider;

/**
 * Main UI container for the media player.
 */
class PlayerControlPanel extends JXPanel implements PlayerMediatorListener, Disposable {
    
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color innerBorder;
    @Resource private Color innerBackground;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    @Resource private Icon backIcon;
    @Resource private Icon backIconPressed;
    @Resource private Icon backIconRollover; 
    @Resource private Icon backIconDisabled;
    @Resource private Icon forwardIcon;
    @Resource private Icon forwardIconPressed;
    @Resource private Icon forwardIconRollover;
    @Resource private Icon forwardIconDisabled;
    @Resource private Icon playIcon;
    @Resource private Icon playIconPressed;
    @Resource private Icon playIconRollover;
    @Resource private Icon playIconDisabled;
    @Resource private Icon pauseIcon;
    @Resource private Icon pauseIconPressed;
    @Resource private Icon pauseIconRollover;
    @Resource private Icon volumeIcon;
    @Resource private Icon volumeIconPressed;
    @Resource private Icon volumeIconRollover;
    @Resource private Icon volumeIconDisabled;
    @Resource private Icon shuffleIcon;
    @Resource private Icon shuffleIconPressed;
    @Resource private Icon shuffleIconRollover;
    @Resource private Icon shuffleIconActive;
          
    @Resource private Font font;
    
    private final JXPanel innerPanel;
    private final JButton backButton;
    private final JButton playButton;
    private final JButton pauseButton;
    private final JButton forwardButton;
    private final LimeSliderBar progressSlider;
    private final JPanel statusPanel;
    private final JButton volumeButton;
    private JButton shuffleButton;
    private int numberOfPlayerTimeValuesToIgnore = 0;
    
    private final JPopupMenu volumeControlPopup;
    private final VolumeSlider volumeSlider; 
    
    private final MarqueeButton titleLabel;
    
    private static final String BACK = "BACK";
    private static final String PLAY = "PLAY";
    private static final String PAUSE = "PAUSE";
    private static final String FORWARD = "FORWARD";
    private static final String VOLUME = "VOLUME";
    private static final String SHUFFLE = "SHUFFLE";

    private final Provider<PlayerMediator> playerProvider;
    private final LibraryMediator libraryMediator;
    private final boolean isAudio;
    
    private SettingListener settingListener;
    
    /**
     * Constructs a PlayerPanel with the specified component providers and
     * decorators.
     */
    public PlayerControlPanel(Provider<PlayerMediator> playerProvider,
                              LibraryMediator libraryMediator,
                              SliderBarDecorator sliderBarDecorator,
                              boolean isAudio) {        
        this.playerProvider = playerProvider;
        this.libraryMediator = libraryMediator;
        this.isAudio = isAudio;
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, filly, alignx center"));
        setOpaque(false);
        
        final ButtonListener playerListener = new ButtonListener();

        backButton = new IconButton(backIcon, backIconRollover, backIconPressed);
        backButton.addActionListener(playerListener);
        backButton.setActionCommand(BACK);
        backButton.setDisabledIcon(backIconDisabled);
        backButton.setToolTipText(I18n.tr("Skip Back"));
        backButton.setFocusable(false);
        
        playButton = new IconButton(playIcon, playIconRollover, playIconPressed);
        playButton.addActionListener(playerListener);
        playButton.setActionCommand(PLAY);
        playButton.setDisabledIcon(playIconDisabled);
        playButton.setToolTipText(I18n.tr("Play"));
        playButton.setFocusable(false);

        pauseButton = new IconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
        pauseButton.addActionListener(playerListener);
        pauseButton.setActionCommand(PAUSE);
        pauseButton.setVisible(false);
        pauseButton.setToolTipText(I18n.tr("Pause"));
        pauseButton.setFocusable(false);
        
        pauseButton.setMinimumSize(playButton.getMinimumSize());
        pauseButton.setPreferredSize(playButton.getPreferredSize());

        forwardButton = new IconButton(forwardIcon, forwardIconRollover, forwardIconPressed);
        forwardButton.addActionListener(playerListener);
        forwardButton.setActionCommand(FORWARD);
        forwardButton.setDisabledIcon(forwardIconDisabled);
        forwardButton.setToolTipText(I18n.tr("Skip Forward"));
        forwardButton.setFocusable(false);
        
        volumeButton = new IconButton(volumeIcon, volumeIconRollover, volumeIconPressed);
        volumeButton.addActionListener(playerListener);
        volumeButton.setActionCommand(VOLUME);
        volumeButton.setDisabledIcon(volumeIconDisabled);
        volumeButton.setToolTipText(I18n.tr("Volume"));
        volumeButton.setFocusable(false);
        
        volumeSlider = new VolumeSlider(0, 100);
        volumeSlider.setValue((int)(SwingUiSettings.PLAYER_VOLUME.getValue() * volumeSlider.getMaximum()));        
        volumeControlPopup = volumeSlider.createPopup();
        
        if (isAudio) {
            shuffleButton = new IconButton(shuffleIcon, shuffleIconRollover, shuffleIconPressed, shuffleIconActive);
            shuffleButton.addActionListener(playerListener);
            shuffleButton.setActionCommand(SHUFFLE);
            shuffleButton.setRolloverSelectedIcon(shuffleIconActive);
            shuffleButton.setToolTipText(I18n.tr("Shuffle"));
            shuffleButton.setFocusable(false);
        }
        
        progressSlider = new LimeSliderBar();
        sliderBarDecorator.decoratePlain(progressSlider);
        initProgressControl();
        
        statusPanel = new JPanel(new MigLayout());
        
        titleLabel = new MarqueeButton("", 150);
        titleLabel.setFont(font);

        ResizeUtils.forceSize(titleLabel, new Dimension(206, (int)
                font.getMaxCharBounds(new FontRenderContext(null, false, false)).getHeight()));
        
        statusPanel.add(titleLabel);
        statusPanel.add(progressSlider, "dock south");
        statusPanel.setOpaque(false);
        
        int buttonWidth = backButton.getPreferredSize().width + 
        playButton.getPreferredSize().width + forwardButton.getPreferredSize().width; 
                
        Dimension statusSize = new Dimension(buttonWidth, statusPanel.getPreferredSize().height);
        statusPanel.setPreferredSize(statusSize);

        innerPanel = new JXPanel(new MigLayout("insets 4 10 4 10, filly, gapy 5, alignx center"));
        innerPanel.setOpaque(false);
        innerPanel.setBackgroundPainter(createStatusBackgroundPainter());
        
        if (isAudio) {
            innerPanel.add(backButton, "gapright 1");
        }
        innerPanel.add(pauseButton, "hidemode 3");
        innerPanel.add(playButton, "hidemode 3");
        if (isAudio) {
            innerPanel.add(forwardButton, "gapright 3");
        }
        innerPanel.add(statusPanel, "gapbottom 2, hidemode 2");
        innerPanel.add(volumeButton, "gapleft 2");
        if (isAudio) {
            innerPanel.add(shuffleButton, "gapleft 2");
        }
        
        innerPanel.setVisible(SwingUiSettings.PLAYER_ENABLED.getValue());
        add(innerPanel, "gaptop 2, gapbottom 2");

        VolumeController volumeController = new VolumeController();
        volumeSlider.addChangeListener(volumeController);
        
        volumeControlPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                playerListener.clearMenu();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                playerListener.clearMenu();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        
        register();
    }
    
    /**
     * Registers listeners for player events.
     */
    private void register() {
        getPlayerMediator().addMediatorListener(this);
        
        // Stop player if disabled, and show/hide player.
        settingListener = new SettingListener(){
            @Override
            public void settingChanged(final SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        boolean enabled = SwingUiSettings.PLAYER_ENABLED.getValue();
                        if (!enabled) {
                            getPlayerMediator().stop();
                            resetPlayer();
                        }
                        PlayerControlPanel.this.innerPanel.setVisible(enabled);
                    }
                });
            }
        };
        
        SwingUiSettings.PLAYER_ENABLED.addSettingListener(settingListener);
    }

	/**
	 * Clears all of the progress/song fields.
	 */
    private void resetPlayer() {
        if (isAudio) {
            titleLabel.stop();
        }
        progressSlider.setValue(0);
        progressSlider.setEnabled(false);
        titleLabel.setText("");
        titleLabel.setToolTipText("");     
    }
    
    public void dispose(){
        getPlayerMediator().removeMediatorListener(this);
        SwingUiSettings.PLAYER_ENABLED.removeSettingListener(settingListener);
        if (isAudio) {
            titleLabel.stop();
        }
    }
    
    /**
     * Initializes the progress component.
     */
    private void initProgressControl() {
        // The progress slider UI has been offsetting the value of the slider to account for
        // the size of the position knob, but since we're overriding the foreground painter to not
        // paint a knob, this is causing the slider to be positioned incorrectly.
        // So, let's override the BasicSliderUI to make the position knob small enough to not
        // distort the mapping but bit enough to still be dragable.
        progressSlider.setUI(new BasicSliderUI(progressSlider) {
            @Override
            protected Dimension getThumbSize() {
                return new Dimension(5,8);
            }
        });
        progressSlider.addChangeListener(new ProgressListener());
        progressSlider.setMaximum(Integer.MAX_VALUE);
        progressSlider.setMaximumSize(new Dimension(206, 8));
        progressSlider.setMinimumSize(new Dimension(206, 8));
        progressSlider.setPreferredSize(new Dimension(206, 8));
        progressSlider.setSize(new Dimension(206, 6));
        progressSlider.setEnabled(false);
        progressSlider.setMinorTickSpacing(1);
        progressSlider.addMouseListener(new MouseAdapter() {
            /**
             * Reposition the thumb on the jslider to the location of the mouse
             * click
             */
            @Override
            public void mousePressed(MouseEvent e) {
                if (!progressSlider.isEnabled())
                    return;

                mouseSkip(e.getX());
            }
            
            /**
             * Overrides the mouse press increment when a mouse click occurs in the 
             * jslider. Repositions the jslider directly to the location the mouse
             * click avoiding the standard step increment with each click
             * @param x - location of mouse click
             */
            protected void mouseSkip(int x) {
                if (progressSlider.getUI() instanceof BasicSliderUI) {
                    progressSlider.setValue(((BasicSliderUI)progressSlider.getUI()).valueForXPosition(x));
                    numberOfPlayerTimeValuesToIgnore = 2;
                }
            }
        });
    }
    
    /**
     * Creates a background painter for the container.
     */
    private Painter<JXPanel> createStatusBackgroundPainter() {
        return new ComponentBackgroundPainter<JXPanel>(innerBackground, innerBorder, 
                bevelLeft, bevelTop1, bevelTop2, bevelRight, bevelBottom, 
                arcWidth, arcHeight, AccentType.SHADOW);
    }
    
    /**
     * Returns the mediator component that controls the player.
     */
    private PlayerMediator getPlayerMediator() {
        return playerProvider.get();
    }
    
    /**
     * Handles update to the specified progress value to adjust the visual
     * position of the slider.
     */
    @Override
    public void progressUpdated(float progress) {
        if (!(progressSlider.getValueIsAdjusting() || getPlayerMediator().getStatus() == PlayerState.SEEKING)) {
            // When the slider value is updated by the mouse, there can be some slider updates with the old values still 
            // queued and waiting to run. So, after the user moves the slider with his mouse to the new play location,
            // an old update can cause it to return to the old play location before a fresh update causes it to jump
            // forward to the new location again. To prevent this odd behavior, after the user changes the play position
            // with his mouse, we ignore a few of the subsequent updates.
            if (numberOfPlayerTimeValuesToIgnore > 0) {
                numberOfPlayerTimeValuesToIgnore--;
            } else {
                progressSlider.setValue((int) (progressSlider.getMaximum() * progress));
            }
        }
    }
    
    /**
     * Handles song change to the specified song name.
     */
    @Override
    public void mediaChanged(String name) {

        initializeVolumeSettings();
        
        // Enable progress slider.
        progressSlider.setEnabled(getPlayerMediator().isSeekable());
        
        // Set song text.
        titleLabel.setText(name);
        titleLabel.setToolTipText(name);
        if (isAudio) {
            titleLabel.start();
        }
        
        if (!innerPanel.isVisible()) {
            innerPanel.setVisible(true);
        }
    }
    
    private void initializeVolumeSettings() {
        // Update volume.
        updateVolumeFromSetting();
        
        //enable volume control
        volumeButton.setEnabled(getPlayerMediator().hasVolumeControl());
    }

    /**
     * Handles state change in the player to the specified state.
     */
    @Override
    public void stateChanged(PlayerState playerState) {
        progressSlider.setEnabled(getPlayerMediator().isSeekable());
        if ((playerState == PlayerState.OPENED) || (playerState == PlayerState.SEEKED)) {
            initializeVolumeSettings();
        } else if (playerState == PlayerState.GAIN) {
            // Exit on volume change.
            return;
        }
        
        // Update buttons based on player status.
        if ((playerState == PlayerState.PLAYING) || (playerState == PlayerState.SEEKING_PLAY)) {
            playButton.setVisible(false);
            pauseButton.setVisible(true);
            if (isAudio) {
                titleLabel.start();
            }
        } else if (playerState == PlayerState.PAUSED || playerState == PlayerState.SEEKING_PAUSED ||
                playerState == PlayerState.EOM || playerState == PlayerState.STOPPED ||
                playerState == PlayerState.UNKNOWN || playerState == PlayerState.NO_SOUND_DEVICE) {
            playButton.setVisible(true);
            pauseButton.setVisible(false);
            if (isAudio) {
                titleLabel.stop();
            }
        }        
    }
    
    private void setVolume(float volume){
        SwingUiSettings.PLAYER_VOLUME.setValue(volume);
        updateVolumeFromSetting();
    }
    
    /**
     * Updates the volume in the player.
     */
    private void updateVolumeFromSetting() {
        if (getPlayerMediator().hasVolumeControl()) {
            getPlayerMediator().setVolume(SwingUiSettings.PLAYER_VOLUME.getValue());
        }
    }
    
    /**
     * Listener to handle user actions on various player buttons.
     */
    private class ButtonListener implements ActionListener {
        
        private long menuInvizTime = -1;
        
        public void clearMenu() {
            menuInvizTime = System.currentTimeMillis();
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            
            if (e.getActionCommand() == PLAY){
                // If the player already has a file, then let's resume play on that file. 
                // Otherwise, let's start playing whatever file is selected in the library. 
                if (getPlayerMediator().getCurrentMediaFile() != null) {
                    getPlayerMediator().resume();
                } else {
                    List<LocalFileItem> selectedItems = libraryMediator.getSelectedItems();
                    if (selectedItems.size() > 0) {
                        playerProvider.get().play(selectedItems.get(0));
                    }
                }
                
            } else if (e.getActionCommand() == PAUSE){
                getPlayerMediator().pause();
                
            } else if (e.getActionCommand() == FORWARD) {
                getPlayerMediator().nextSong();
                
            } else if (e.getActionCommand() == BACK) {
                getPlayerMediator().prevSong();
                    
            } else if (e.getActionCommand() == VOLUME) {
                if (System.currentTimeMillis() - menuInvizTime > 250f) {
                    volumeControlPopup.show(volumeButton, 0, 14);
                    volumeControlPopup.requestFocus();
                }
                
            } else if (e.getActionCommand() == SHUFFLE) {
                // Toggle shuffle mode.
                getPlayerMediator().setShuffle(!getPlayerMediator().isShuffle());
                // Update button state.
                shuffleButton.setSelected(getPlayerMediator().isShuffle());
                shuffleButton.setPressedIcon(getPlayerMediator().isShuffle() ?
                        shuffleIconActive : shuffleIconPressed);
            }
        }
    }
  
    /**
     * Listener to handle change to progress bar to skip to a new position in 
     * the song.
     */
    private class ProgressListener implements ChangeListener {
        
        private boolean waiting = false; 
       
        @Override
        public void stateChanged(ChangeEvent e) {
            if (progressSlider.getMaximum() != 0 && getPlayerMediator().isSeekable() && progressSlider.getValueIsAdjusting()) {
                if (!waiting) {
                    waiting = true;
                }
                
            } else if (waiting) {
                waiting = false;
                double percent = (double)progressSlider.getValue() / (double)progressSlider.getMaximum();
                getPlayerMediator().seek(percent);
                progressSlider.setValue((int)(percent * progressSlider.getMaximum()));
            } 
        }
    }

    /**
     * Listener to update volume when volume slider is adjusted.
     */
    private class VolumeController implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            setVolume(((float) volumeSlider.getValue()) / volumeSlider.getMaximum());
        }
    }
}
