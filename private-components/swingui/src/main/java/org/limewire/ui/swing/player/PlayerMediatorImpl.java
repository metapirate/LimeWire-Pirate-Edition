package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.media.ClockStoppedException;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.GainControl;
import javax.media.IncompatibleSourceException;
import javax.media.IncompatibleTimeBaseException;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.media.TimeBase;
import javax.media.protocol.DataSource;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import net.sf.fmj.concurrent.ExecutorServiceManager;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.lti.utils.OSUtils;

@Singleton
class PlayerMediatorImpl implements PlayerMediator {

    private static final Log LOG = LogFactory.getLog(PlayerMediatorImpl.class);
    
    private final VideoDisplayDirector displayDirector;
    private final List<PlayerMediatorListener> listenerList;
    private final CategoryManager categoryManager;
    private final PlayerInitializer playerInitializer;
    private final ControllerListener controllerListener;
    private final Provider<MediaPlayerFactoryImpl> mediaPlayerFactory;
    private final Playlist playlist;
    
    /** A static Player to avoid null checks when no player currently is in use. */
    private static final Player EMPTY_PLAYER = new EmptyPlayer();
    
    /** The current instance of the Player that is being used. This will change on a file per file basis.
     *  This Player should never be null, if no Player exists currently it should be an instanceOf EMPTY_PLAYER. 
     */
    private Player player;
    
    /** The File that is currently playing. If no File has been set, this is null. */
    private File currentMediaFile;
    
    /** If the Player is active, this will return a valid Timer, otherwise return null. */
    private volatile Timer updateTimer;
    
    /** Returns true if this Player is currently seeking, false otherwise.*/
    private boolean isSeeking;
    
    private long playingWindowStartTime = -1;
    private int playingSwitches = -1;
    
    @SuppressWarnings("unused")
    private volatile boolean videoPlayedInSessionByLW = false;
    
    @SuppressWarnings("unused")
    private volatile boolean audioPlayedInSession = false;
    
    @Inject
    PlayerMediatorImpl(VideoDisplayDirector displayDirector, CategoryManager categoryManager,
            Provider<MediaPlayerFactoryImpl> mediaPlayerFactory) {
        this.displayDirector = displayDirector;
        this.categoryManager = categoryManager;
        this.mediaPlayerFactory = mediaPlayerFactory;
        this.listenerList = new ArrayList<PlayerMediatorListener>();
        this.playerInitializer = new PlayerInitializer();
        this.controllerListener = new VideoControllerListener();
        this.playlist = new Playlist();
        this.player = EMPTY_PLAYER;
        
        //NOTE: used in Handler to catch runtime exceptions and report them, don't delete this
        ThreadPoolListeningExecutor tpe = new ThreadPoolListeningExecutor(1, 1,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                ExecutorsHelper.daemonThreadFactory("Video ThreadPool"));
    
        ExecutorServiceManager.setExecutorService(ExecutorsHelper.unconfigurableExecutorService(tpe));
    }

    @Inject
    void register(){
        // if playing audio/video files within LW is disabled, stop any file
        // that may be playing currently.
        SwingUiSettings.PLAYER_ENABLED.addSettingListener(new SettingListener(){
            @Override
            public void settingChanged(final SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        if (!SwingUiSettings.PLAYER_ENABLED.getValue()) {
                            stop();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void addMediatorListener(PlayerMediatorListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void removeMediatorListener(PlayerMediatorListener listener) {
        listenerList.remove(listener);
    }

    @Override
    public File getCurrentMediaFile() {
        return currentMediaFile;
    }

    @Override
    public PlayerState getStatus() {
        return convertControllerState(player.getState());
    }

    private PlayerState convertControllerState(int controllerState) {
        if(isSeeking){
            return PlayerState.SEEKING;
        }
        
        switch (controllerState) {
        case Controller.Started:
            return PlayerState.PLAYING;
        case Controller.Realized:
            return PlayerState.PAUSED;
        default:
            return PlayerState.UNKNOWN;
        }
    }
    
    @Override
    public void setActivePlaylist(EventList<LocalFileItem> fileList) {
        playlist.setActivePlaylist(fileList);
    }

    @Override
    public boolean isPaused(File file) {
        return player.getState() != Controller.Started && file.equals(currentMediaFile);
    }

    @Override
    public boolean isPlaying(File file) {
        return player.getState() == Controller.Started && file.equals(currentMediaFile);
    }

    @Override
    public boolean isSeekable() {
        return isDurationMeasurable();
    }

    @Override
    public boolean isShuffle() {
        return playlist.isShuffle();
    }

    @Override
    public void pause() {
        if(player instanceof JavaSoundPlayer)
            ((JavaSoundPlayer)player).pause();
        else
            player.stop();
    }

    @Override
    public void play(LocalFileItem localFileItem) {
        playlist.setCurrentItem(localFileItem);
        play(localFileItem.getFile(), true);
    }
    
    @Override
    public void playOrLaunchNatively(File file) {
        playlist.setCurrentItem(null);
        playlist.setActivePlaylist(null);
        play(file, true);
    }
    
    /**
     * Handles the actual launching of the media file. 
     * @param isLaunchOnFailure if true will launch the file natively if it fails to
     * open within LW. If false and an audio file, will attempt to play the next
     * file within a playlist if one exists.
     */
    private void play(File file, boolean isLaunchOnFailure) {
        Category category = categoryManager.getCategoryForFile(file);
        if (SwingUiSettings.PLAYER_ENABLED.getValue() && (OSUtils.isWindows() || OSUtils.isMacOSX()) && 
                (category == Category.AUDIO || category == Category.VIDEO)) {
            currentMediaFile = file;
            initializePlayerOrNativeLaunch(file, null, false, true, isLaunchOnFailure);
        } else {
            currentMediaFile = null;
            NativeLaunchUtils.safeLaunchFile(file, categoryManager);
        }
    }

    /**
     * Initializes an FMJ player for the video if possible, launches natively if
     * not.
     * 
     * @param file the video file to be played
     * @param time the starting time of the video. null to start at the
     *        beginning.
     * @param autoPlay whether or not to start playback
     * @return true if the player is successfully initialized, false if it is
     *         not initialized and the file is natively launched
     */
    private void initializePlayerOrNativeLaunch(final File file, Time time, boolean isFullScreen, boolean autoStart, boolean isLaunchOnFailure) {
        GuiUtils.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     
        if(playerInitializer.isInitializing()) {
            playerInitializer.cancel();
        }
        killPlayer();
        playerInitializer.initialize(file, time, isFullScreen, autoStart, isLaunchOnFailure);
    }
    
    @Override
    public void nextSong() {
        stop();

        LocalFileItem fileItem = playlist.getNextFileItem();

        if (fileItem != null) {
            play(fileItem.getFile(), false);
        }
    }

    @Override
    public void prevSong() {
        stop();

        LocalFileItem fileItem = playlist.getPrevFileItem();

        if (fileItem != null) {
            play(fileItem.getFile(), false);
        }
    }

    @Override
    public void resume() {
        if (player.getState() != Controller.Started) {
            player.start();
        }
    }

    @Override
    public void setShuffle(boolean shuffle) {
        playlist.setShuffle(shuffle);
    }

    @Override
    public void setVolume(double value) {
        if ( player.getState() != Controller.Unrealized && player.getState() != Controller.Realizing && 
                player.getGainControl() != null) {
            player.getGainControl().setLevel((float) value);
        }
    }
    
    private boolean isAudioFile(File file) {
        return categoryManager.getCategoryForFile(file) == Category.AUDIO;
    }

    @Override
    public boolean hasVolumeControl() {
        // On Mac OS X we're using true full screen mode, and you can't use popups in true full screen mode.
        // So, since the volume popup won't work, let's disable it.
        if (OSUtils.isMacOSX() && isFullScreen())
            return false;

        int state = player.getState();
        if(state == Controller.Unrealized || state == Controller.Realizing)
            return false;
        return player.getGainControl() != null;
    }

    @Override
    public void seek(double percent) {
        isSeeking = true;
        player.setMediaTime(new Time(percent * player.getDuration().getSeconds()));
        isSeeking = false;
    }

    @Override
    public void stop() {
        if(playerInitializer.isInitializing()){
            playerInitializer.cancel();
        }
      
        displayDirector.close();
        
        killTimer();
        killPlayer();
    
        currentMediaFile = null;  
    }

    /**
     * Notifies the listeners of the change in state of the player.
     */
    private void firePlayerStateChanged(PlayerState state) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.stateChanged(state);
        }
    }
    
    /**
     * Notifies the listeners of the current progress on the currently playing
     * media file.
     */
    private void fireProgressUpdated() {
        fireProgressUpdated((float) (player.getMediaTime().getSeconds() / player.getDuration().getSeconds()));
    }

    /**
     * Notifies the listeners of the current progress on the currently playing
     * media file.
     * 
     * @param progress - the current progress [0.0f,100.0f]
     */
    private void fireProgressUpdated(float progress) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.progressUpdated(progress);
        }
    }

    /**
     * Notifies the listeners when the media file that is currently playing changes.
     */
    private void fireMediaChanged(String name) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.mediaChanged(name);
        }
    }

    private void killTimer() {
        if (updateTimer != null) {
            if (updateTimer.isRunning()) {
                updateTimer.stop();
            }
            updateTimer = null;
        }
    }

    /**
     * Stops the Player and cleans up any resources used by the Player.
     * Player is reset to EMPTY_PLAYER.
     */
    private void killPlayer() {
        player.stop();
        player.removeControllerListener(controllerListener);
        player.close();
        player.deallocate();
        player = EMPTY_PLAYER;
    }

    public void setFullScreen(boolean isFullScreen) {
        if(player == EMPTY_PLAYER) {
            return;
        }
        
        if(isAudioFile(currentMediaFile))
            return;
        
        if (displayDirector.isFullScreen() == isFullScreen) {
            return;
        }
        
        //task is already running.  user probably hit crtl-f twice quickly.
        if (playerInitializer.isInitializing()){
            return;
        }
        
        boolean isPlaying = player.getState() == Controller.Started;

        Time time = isDurationMeasurable() ? player.getMediaTime() : null;

        killTimer();
        killPlayer();

        initializePlayerOrNativeLaunch(currentMediaFile, time, isFullScreen, isPlaying, false);
    }
    
    
    @Override
    public boolean isPlayable(File file) {
        if(!SwingUiSettings.PLAYER_ENABLED.getValue())
            return false;
        Category category = categoryManager.getCategoryForFile(file);
        return category == Category.AUDIO || category == Category.VIDEO;
    }

    public boolean isFullScreen() {
        return displayDirector.isFullScreen();
    }

    private boolean isDurationMeasurable() {
        if(player == EMPTY_PLAYER) {
            return false;
        }
        long time = player.getDuration().getNanoseconds();        
        return Player.DURATION_UNBOUNDED.getNanoseconds() != time && Player.DURATION_UNKNOWN.getNanoseconds() != time && Time.TIME_UNKNOWN.getNanoseconds() != time;
    }

    private class VideoControllerListener implements ControllerListener {
        @Override
        public void controllerUpdate(final ControllerEvent controllerEvent) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    if (controllerEvent instanceof EndOfMediaEvent) {
                        setEndOfMedia();
                    } else if (controllerEvent instanceof StartEvent || controllerEvent.getSourceController().getState() == Controller.Started) {
                        firePlayerStateChanged(PlayerState.OPENED);
                        firePlayerStateChanged(PlayerState.PLAYING);
                        if (updateTimer == null) {
                            updateTimer = new Timer(100, new TimerAction());
                        }

                        if (!updateTimer.isRunning()) {
                            updateTimer.start();
                        }

                    } else if (controllerEvent instanceof StopEvent) {
                        firePlayerStateChanged(PlayerState.STOPPED);
                        if (updateTimer != null) {
                            updateTimer.stop();
                        }
                    }
                }
            });
        }
    }

    private class TimerAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isDurationMeasurable()) {
                if(updateTimer != null)
                    updateTimer.stop();
                return;
            }

            if (player.getMediaTime().getSeconds() >= player.getDuration().getSeconds()) {
                // some FMJ players don't seem to fire EndOfMediaEvents so we need to do
                // this manually
                setEndOfMedia();
            } else {
                fireProgressUpdated();
            }
        }
    }    
    
    private void setEndOfMedia() {
        player.stop();
        player.setMediaTime(new Time(0));
        if(updateTimer != null)
            updateTimer.stop();
        firePlayerStateChanged(PlayerState.EOM);
        fireProgressUpdated(100f);

        // Sanity check before switching to the next song,
        //  the last 10 songs that switched must have taken over
        //  5 seconds to play.
        playingSwitches = (playingSwitches+1);
        if (playingSwitches % 10 == 0) {
            if(playingSwitches == 0) {
                playingWindowStartTime = System.currentTimeMillis();
                nextSong();
            } else {
                long currentTime = System.currentTimeMillis();
                if(currentTime - playingWindowStartTime < 5000) {
                    playingSwitches = -1;
                    playingWindowStartTime = -1;   
                } else {
                    playingSwitches = -1;
                    playingWindowStartTime = currentTime;
                    nextSong();
                }
            }
        } else {
            nextSong();
        }
    }
    
    interface PlayerCompleteCallback {
        public void complete(Player newPlayer, Time time, boolean autoStart);
    }
    
    /**
     * Asynchronously initializes new media players.
     */
    private class PlayerInitializer {

        private PlayerInitalizationWorker initializationWorker;
        
        public PlayerInitializer(){}
        
        public boolean isInitializing() {
            return initializationWorker != null;
        }

        public void initialize(final File file, Time time, boolean isFullScreen, boolean autoStart, boolean isLaunchOnFailure) {
            if (isInitializing()) {
                cancel();
            }
            if(LOG.isDebugEnabled())
                LOG.debug("initializing player for: " + file);

            initializationWorker = new PlayerInitalizationWorker(file, time, isFullScreen, autoStart, isLaunchOnFailure, new PlayerCompleteCallback() {
                @Override
                public void complete(Player newPlayer, Time time, boolean autoStart) {
                    assert(SwingUtilities.isEventDispatchThread());

                    if(newPlayer == null) {
                        //New player creation failed.  The video was launched natively.
                        displayDirector.close();
                        return;
                    }

                    if (time != null) {
                        newPlayer.setMediaTime(time);
                    }

                    newPlayer.addControllerListener(controllerListener);           

                    player = newPlayer;
               
                    if(!isAudioFile(file))
                        displayDirector.show();   
                    
                    fireMediaChanged(file.getName()); 
                      
                    startMediaPlayer(autoStart); 
                    initializationWorker = null;
                    if(LOG.isDebugEnabled())
                        LOG.debug("player initialized");
                }
            });
            initializationWorker.execute();
        }

        public void cancel() {
            initializationWorker.cancelInitialization();
            initializationWorker = null;
        }
        
        private void startMediaPlayer(final boolean autoStart) {
            ActionListener listener = new ActionListener() {                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(player != EMPTY_PLAYER) {
                        // start the player regardless of startVideo so that
                        // control panel is correctly updated

                        player.start();
                        // force fire these immediately to make sure
                        // everything updates
                        firePlayerStateChanged(PlayerState.PLAYING);
                        if (isDurationMeasurable()) {
                            fireProgressUpdated();
                        }

                        if (!autoStart) {
                            player.stop();
                        }
                    }
                }
            };
            
            // Prevents the flash of native video window by delaying 
            // video playback so that the video can be fully embedded first.
            Timer timer = new Timer(100, listener);
            timer.setRepeats(false);
            timer.start();
        }

        /**
         * SwingWorker that initializes the new player off of the EDT.
         */
        private class PlayerInitalizationWorker extends SwingWorker<Player, Void> {
            private final File mediaFile;
            private final Time time;
            private final boolean autoStart;
            private final boolean isLaunchOnFailure;
            private final PlayerCompleteCallback callback;
            private final Container renderPanel;
            
            private volatile boolean canceled = false;   

            public PlayerInitalizationWorker(File mediaFile, Time time, boolean isFullScreen,
                    boolean autoStart, boolean isLaunchOnFailure, PlayerCompleteCallback callback) {
                this.mediaFile = mediaFile;
                this.time = time;
                this.autoStart = autoStart;
                this.isLaunchOnFailure = isLaunchOnFailure;
                this.callback = callback;
                this.renderPanel = new JPanel(new BorderLayout());
                
                if(!isAudioFile(mediaFile))
                    displayDirector.initialize(renderPanel, isFullScreen);                
            }

            /**
             * Cancels the player initialization. We don't want to interrupt the
             * thread by using cancel(boolean) because we need to properly
             * dispose of the player.
             */
            public void cancelInitialization() {
                canceled = true;
            }
            
            @Override
            protected Player doInBackground() throws Exception {
                try {
                    Player player = mediaPlayerFactory.get().createMediaPlayer(mediaFile, renderPanel);
                    if(categoryManager.getCategoryForFile(mediaFile) == Category.AUDIO)
                        audioPlayedInSession = true;
                    else
                        videoPlayedInSessionByLW = true;
                    return player;
                } catch (IncompatibleSourceException e) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("failed to obtain a player " + e);
                    if(isLaunchOnFailure && !canceled) {
                        NativeLaunchUtils.safeLaunchFile(mediaFile, categoryManager);
                    } else if(categoryManager.getCategoryForFile(mediaFile) == Category.AUDIO) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                if(!canceled)
                                    nextSong();                                
                            }
                        });
                    } else {
                        if(!canceled)
                            NativeLaunchUtils.safeLaunchFile(mediaFile, categoryManager);
                    }
                    return null;
                }
            }            

            @Override
            protected void done() {
                GuiUtils.getMainFrame().setCursor(Cursor.getDefaultCursor());

                Player player = null;
                try {
                    player = get();
                } catch (InterruptedException e) {
                    // we're already finished so this can't happen
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                if (canceled || player == null) {
                    if (player != null) {
                        player.close();
                        player.deallocate();
                    }
                    return;
                }

                // if no renderer has been added yet or its OSX and a video file, add the renderer
                // these are weird work arounds for the DS player on Windows 7 and audio playback on OSX
                if(renderPanel.getComponentCount() == 0 &&
                    (!OSUtils.isMacOSX() || OSUtils.isMacOSX() && categoryManager.getCategoryForFile(mediaFile) == Category.VIDEO))
                        renderPanel.add(player.getVisualComponent());
                callback.complete(player, time, autoStart);
            }
        }
    }
    
    /**
     * Wrapper for Player to avoid null checks when the Player has not been initialized.
     */
    static class EmptyPlayer implements Player {

        @Override public void addController(Controller newController) throws IncompatibleTimeBaseException {}
        @Override public Component getControlPanelComponent() {return null;}
        @Override public GainControl getGainControl() {return null;}
        @Override public Component getVisualComponent() {return null;}
        @Override public void removeController(Controller oldController) {}
        @Override public void start() {}
        @Override public void setSource(DataSource source) throws IOException, IncompatibleSourceException {}
        @Override public void addControllerListener(ControllerListener listener) {}
        @Override public void close() {}
        @Override public void deallocate() {}
        @Override public Control getControl(String forName) {return null;}
        @Override public Control[] getControls() {return new Control[0];}
        @Override public Time getStartLatency() {return new Time(0);}
        @Override public int getState() {return Controller.Unrealized;}
        @Override public int getTargetState() {return 0;}
        @Override public void prefetch() {}
        @Override public void realize() {}
        @Override public void removeControllerListener(ControllerListener listener) {}
        @Override public long getMediaNanoseconds() {return 0;}
        @Override public Time getMediaTime() {return new Time(0);}
        @Override public float getRate() {return 0;}
        @Override public Time getStopTime() {return null;}
        @Override public Time getSyncTime() {return null;}
        @Override public TimeBase getTimeBase() {return null;}
        @Override public Time mapToTimeBase(Time t) throws ClockStoppedException {return null;}
        @Override public void setMediaTime(Time now) {}
        @Override public float setRate(float factor) {return 0;}
        @Override public void setStopTime(Time stopTime) {}
        @Override public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException {}
        @Override public void stop() {}
        @Override public void syncStart(Time at) {}
        @Override public Time getDuration() {return new Time(1);}
    }    
}
