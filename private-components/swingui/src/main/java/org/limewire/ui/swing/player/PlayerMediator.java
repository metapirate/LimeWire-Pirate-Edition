package org.limewire.ui.swing.player;

import java.io.File;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;

import ca.odell.glazedlists.EventList;

public interface PlayerMediator {

    /**
     * Adds the specified listener to the list that is notified about 
     * mediator events.
     */
    public void addMediatorListener(PlayerMediatorListener listener);

    /**
     * Removes the specified listener from the list that is notified about
     * mediator events.
     */
    public void removeMediatorListener(PlayerMediatorListener listener);

    /**
     * Returns the current status of the media player.
     */
    public PlayerState getStatus();

    /**
     * Sets the active playlist to the current EventList. If the EventList
     * is modified or filtered, this will be reflected in the playlist. If  
     * no playlist exists, this can be set to null.
     */
    public void setActivePlaylist(EventList<LocalFileItem> fileList);

    /**
     * Returns true if shuffle mode is enabled.
     */
    public boolean isShuffle();

    /**
     * Sets an indicator to enable shuffle mode.
     */
    public void setShuffle(boolean shuffle);

    /**
     * Sets the volume (gain) value on a linear scale from 0.0 to 1.0.
     */
    public void setVolume(double value);

    /**
     * @return true if a volume Control exists on this player, false otherwise.
     */
    boolean hasVolumeControl();

    /**
     * Pauses the media player. 
     */
    public void pause();

    /**
     * Resumes the media player.
     */
    public void resume();

    /**
     * Starts playing the specified file item in the media player.
     */
    public void play(LocalFileItem localFileItem);
    
    /**
     * Starts playing the specified file within LW if it can, otherwise it
     * launches natively on failure.
     */
    public void playOrLaunchNatively(File file);

    /**
     * Seek to given point within the current file. If the media's
     * length is unknown (ie. streaming audio), the seek is ignored.
     * 
     * @param percent of the song frames to skip from begining of file
     */
    public void seek(double percent);

    /**
     * Stops playing the current file.
     */
    public void stop();

    /**
     * Plays the next song in the playlist.
     */
    public void nextSong();

    /**
     * Plays the previous song in the playlist.
     */
    public void prevSong();

    /**
     * Returns the current playing file.
     */
    public File getCurrentMediaFile();

    /**
     * Returns true if this file is currently playing, false otherwise
     */
    public boolean isPlaying(File file);

    /**
     * Returns true if this file is currently loaded and paused, false otherwise.
     */
    public boolean isPaused(File file);

    /**
     * Returns true if the currently playing song is seekable.
     */
    public boolean isSeekable();
    
    /**
     * Returns true if this is an audio or video file.
     */
    public boolean isPlayable(File file);
}