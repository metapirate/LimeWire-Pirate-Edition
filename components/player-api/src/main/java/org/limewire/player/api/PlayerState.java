package org.limewire.player.api;


/**
 * Different states reachable by the LimeWirePlayer.
 */
public enum PlayerState {
    
    /** The player state is unknown.
     */
    UNKNOWN, 
    
    /** The player is attempting to open a file for reading.
     */
    OPENING, 
    
    /** The player has opened the song and sourcedataline and 
     *  is ready to be read.
     */
    OPENED, 
    
    /** The player is playing the current song.
     */
    PLAYING, 
    
    /** The player has stopped the current song.
     */
    STOPPED, 
    
    /** The player has paused the current song.
     */
    PAUSED, 
    
    /** The player has resumed playing the current song.
     */
    RESUMED, 
    
    /** The player is seeking to a new location in the song.
     */
    SEEKING, 
    
    /** Seeking with intent to pause after seek.
     */
    SEEKING_PAUSED,
    
    /** Seeking with intent to begin playing after seek.
     */
    SEEKING_PLAY,
    
    /** The end of the song has been reached.
     */
    EOM,
    
    /** The volume on the outputstream has changed.
     */
    GAIN, 
    
    /** The player has reached the new song location.
     */
    SEEKED,
    
    /** The player cannot find a sound device to play this audio source with.
    */
    NO_SOUND_DEVICE;
}
