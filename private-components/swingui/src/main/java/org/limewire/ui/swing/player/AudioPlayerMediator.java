package org.limewire.ui.swing.player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.FileUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Mediator that controls the interaction between the player view, the current
 * playlist, and the audio player.
 */
@Singleton
class AudioPlayerMediator implements PlayerMediator {

    public static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";
    public static final String AUDIO_TYPE = "audio.type";
    
    private static final String MP3 = "mp3";
    private static final String WAVE = "wave";
    
    private final Provider<AudioPlayer> audioPlayerProvider;
    private final LibraryMediator libraryMediator;
    private final List<PlayerMediatorListener> listenerList;
    private final CategoryManager categoryManager;
    private final Playlist playlist;
    
    /** Audio player component. */
    private AudioPlayer audioPlayer;
    
    /** File item for the last opened song. */
    private LocalFileItem fileItem = null;
    
    /** Map containing properties for the last opened song. */
    private Map audioProperties = null;
    
    /** Progress of current song from 0.0 to 1.0. */
    private float progress;
    
    private long playingWindowStartTime = -1;
    private int playingSwitches = -1;
    
    /**
     * Constructs a PlayerMediator using the specified services.
     */
    @Inject
    public AudioPlayerMediator(Provider<AudioPlayer> audioPlayerProvider,
            LibraryMediator libraryMediator, CategoryManager categoryManager) {
        this.audioPlayerProvider = audioPlayerProvider;
        this.libraryMediator = libraryMediator;
        this.categoryManager = categoryManager;
        
        this.listenerList = new ArrayList<PlayerMediatorListener>();
        this.playlist = new Playlist();
    }
    
    /**
     * Returns the audio player component.  When first called, this method
     * creates the component and registers this mediator as a listener.
     */
    private AudioPlayer getPlayer() {
        if (audioPlayer == null) {
            audioPlayer = audioPlayerProvider.get();
            audioPlayer.addAudioPlayerListener(new PlayerListener());
        }
        return audioPlayer;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#addMediatorListener(org.limewire.ui.swing.player.PlayerMediatorListener)
     */
    public void addMediatorListener(PlayerMediatorListener listener) {
        listenerList.add(listener);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#removeMediatorListener(org.limewire.ui.swing.player.PlayerMediatorListener)
     */
    public void removeMediatorListener(PlayerMediatorListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies registered listeners that the progress is updated to the 
     * specified value.
     */
    private void fireProgressUpdated(float progress) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).progressUpdated(progress);
        }
    }
    
    /**
     * Notifies registered listeners that the song is changed to the specified
     * song name.
     */
    private void fireSongChanged(String name) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).mediaChanged(name);
        }
    }
    
    /**
     * Notifies registered listeners that the player state is changed to the
     * specified state.
     */
    private void fireStateChanged(PlayerState state) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).stateChanged(state);
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#getStatus()
     */
    @Override
    public PlayerState getStatus() {
        return getPlayer().getStatus();
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#isShuffle()
     */
    @Override
    public boolean isShuffle() {
        return playlist.isShuffle();
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#setShuffle(boolean)
     */
    @Override
    public void setShuffle(boolean shuffle) {
        playlist.setShuffle(shuffle);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#setVolume(double)
     */
    @Override
    public void setVolume(double value) {
        getPlayer().setVolume((float)value);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#pause()
     */
    @Override
    public void pause() {
        getPlayer().pause();
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#resume()
     */
    @Override
    public void resume() {
        PlayerState status = getPlayer().getStatus();
        if ((status == PlayerState.STOPPED  || status == PlayerState.UNKNOWN) && fileItem != null) {
            //resume the file that was stopped
            play(fileItem);
        } else if ((status == PlayerState.STOPPED) || (status == PlayerState.UNKNOWN)) {
            // Get first selected item.
            List<LocalFileItem> selectedItems = libraryMediator.getSelectedItems();
            if (selectedItems.size() > 0) {
                LocalFileItem selectedItem = selectedItems.get(0);
                if (isPlayable(selectedItem.getFile())) {
                    // Set active playlist and play file item.
                    setActivePlaylist(libraryMediator.getPlayableList());
                    play(selectedItem);
                }
            }
            
        } else {
            getPlayer().unpause();
        }
    }

    @Override
    public void setActivePlaylist(EventList<LocalFileItem> fileList) {
        playlist.setActivePlaylist(fileList);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#play(java.io.File)
     */
    public void play(File file) {
        // Stop current song.
        stop();
        
        // Play new song.
        this.fileItem = null;
        playlist.setActivePlaylist(null);
        loadAndPlay(file);
    }
    

    @Override
    public void playOrLaunchNatively(File file) {
        if(isPlayable(file)) {
            play(file);
        } else {
            NativeLaunchUtils.safeLaunchFile(file, categoryManager);
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#play(org.limewire.core.api.library.LocalFileItem)
     */
    @Override
    public void play(LocalFileItem localFileItem) {
        // Stop current song.
        stop();
        
        // Play new song.
        this.fileItem = localFileItem;
        playlist.setCurrentItem(localFileItem);
        loadAndPlay(localFileItem.getFile());
    }
    
    private void loadAndPlay(File fileToPlay) {
        AudioPlayer player = getPlayer();
        player.loadSong(fileToPlay);
        player.playSong();
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#skip(double)
     */
    @Override
    public void seek(double percent) {
        // need to know something about the audio type to be able to skip
        if (audioProperties != null && audioProperties.containsKey(AUDIO_TYPE)) {
            String songType = (String) audioProperties.get(AUDIO_TYPE);
            
            // currently, only mp3 and wav files can be seeked upon
            if (isSeekable(songType) && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                final long skipBytes = Math.round((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)* percent);
                getPlayer().seekLocation(skipBytes);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#stop()
     */
    @Override
    public void stop() {
        getPlayer().stop();
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#nextSong()
     */
    @Override
    public void nextSong() {
        // Stop current song.
        stop();

        // Get next file item.
        fileItem = playlist.getNextFileItem();

        // Play song.
        if (fileItem != null) {
            loadAndPlay(fileItem.getFile());
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#prevSong()
     */
    @Override
    public void prevSong() {
        // Stop current song.
        stop();

        fileItem = playlist.getPrevFileItem();

        // Play song.
        if (fileItem != null) {
            loadAndPlay(fileItem.getFile());
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#getCurrentSongFile()
     */
    @Override
    public File getCurrentMediaFile() {
        AudioSource source = getPlayer().getCurrentSong();
        return (source != null) ? source.getFile() : null;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#isPlaying(java.io.File)
     */
    @Override
    public boolean isPlaying(File file) {
        return getPlayer().isPlaying(file);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#isPaused(java.io.File)
     */
    @Override
    public boolean isPaused(File file) {
        return getPlayer().isPaused(file);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.ui.swing.player.IPlayerMediator#isSeekable()
     */
    @Override
    public boolean isSeekable() {
        if (audioProperties != null) {
            return isSeekable((String) audioProperties.get(AUDIO_TYPE));
        }
        return false;
    }   
    
    @Override
    public boolean isPlayable(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
    }

    @Override
    public boolean hasVolumeControl() {
        return true;
    }
    
    /**
     * Returns true if the specified song type is seekable, which means that 
     * the progress position can be set.  At present, only MP3 and Wave files 
     * are seekable.
     */
    private boolean isSeekable(String songType) {
        if (songType == null) {
            return false;
        }
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }
    
    /**
     * Returns the name of the current song.
     */
    private String getSongName() {
        // Use audio properties if available.
        if (audioProperties != null) {
            Object author = audioProperties.get("author");
            Object title = audioProperties.get("title");
            if ((author != null) && (title != null)) {
                return author + " - " + title;
            }
        }
        
        // Use file item if available.
        if (fileItem != null) {
            return fileItem.getFile().getName();
        } else if(getCurrentMediaFile() != null) {
            return FileUtils.getFilenameNoExtension(getCurrentMediaFile().getName());
        } else {
            return I18n.tr("Unknown");
        }
    }
    
    /**
     * Listener to handle audio player events.
     */
    private class PlayerListener implements AudioPlayerListener {
        @Override
        public void progressChange(int bytesread) {
            // If we know the length of the song, update progress value.
            if ((audioProperties != null) && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                float byteslength = ((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)).floatValue();
                progress = bytesread / byteslength;
                
                // Notify UI about progress.
                fireProgressUpdated(progress);
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            // Save properties.
            audioProperties = properties;
            
            // Notify UI about new song.
            fireSongChanged(getSongName());
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            // Go to next song when finished.
            if (event.getState() == PlayerState.EOM) {
                
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
            // Notify UI about state change.
            fireStateChanged(event.getState());
        }
    }
}
