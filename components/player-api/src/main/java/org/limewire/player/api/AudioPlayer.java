package org.limewire.player.api;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.media.GainControl;


/**
 * This interface defines the functionality of an AudioPlayer
 * component.
 */
public interface AudioPlayer {

    /**
     * Loads a song wrapped in a AudioSource object.
     */
    public void loadSong(AudioSource source);
    
    /**
     * Loads a song from a File.
     */
    public void loadSong(File source);
    
    /**
     * Loads a song from an InputStream.
     */
    public void loadSong(InputStream source);
    
    /**
     * Loads a song from a URL.
     */
    public void loadSong(URL source);

    /**
     * Begins playing the loaded song.
     */
    public void playSong();

    /**
     * Pauses the current song.
     */
    public void pause();
    
    /**
     * Unpauses the current song.
     */
    public void unpause();

    /**
     * Stops the current song from playing (essentially returns the song to the
     * loaded state).
     */
    public void stop();
    
    /**
     * If playing a file, searches to a specified location in the song If
     * playing a stream, has no effect.
     *
     * @param value non-negative frame to skip to
     */
    public long seekLocation(long value);
    
    /**
     * Returns the current state of the player.
     * 
     * @return the state of the player -- one of STATUS_PLAYING, STATUS_PAUSED,
     *         STATUS_STOPPED, STATUS_STOPPPED, STATUS_OPENED, STATUS_SEEKING,
     *         STATUS_UNKNOWN
     */
    public PlayerState getStatus();
    
    /**
     * Sets Volume(Gain) value Linear scale 0.0 <--> 1.0.
     */
    public void setVolume(float value);
    
    /**
     * Returns the GainController used by this Player.
     */
    public GainControl getGainControl();
    
    /**
     * Adds a listener to the list of player listeners.
     */
    public void addAudioPlayerListener(AudioPlayerListener listener);
    
    /**
     * Removes a listener from the list of player listeners.
     */
    public void removeAudioPlayerListener(AudioPlayerListener listener);
    
    /**
     * @return true if the file is currently playing
     */
    public boolean isPlaying(File file);
    
    /**
     * @return true if the file is currently paused
     */
    public boolean isPaused(File file);

    /**
     * @return The current song playing in the player. This may return null.
     */
    AudioSource getCurrentSong();
}
