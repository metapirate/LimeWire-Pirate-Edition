package org.limewire.player.impl;

import static org.limewire.player.api.PlayerState.EOM;
import static org.limewire.player.api.PlayerState.GAIN;
import static org.limewire.player.api.PlayerState.NO_SOUND_DEVICE;
import static org.limewire.player.api.PlayerState.PAUSED;
import static org.limewire.player.api.PlayerState.PLAYING;
import static org.limewire.player.api.PlayerState.SEEKING;
import static org.limewire.player.api.PlayerState.SEEKING_PAUSED;
import static org.limewire.player.api.PlayerState.SEEKING_PLAY;
import static org.limewire.player.api.PlayerState.STOPPED;
import static org.limewire.player.api.PlayerState.UNKNOWN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.GainChangeEvent;
import javax.media.GainChangeListener;
import javax.media.GainControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import net.sf.fmj.media.AbstractGainControl;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;

/**
 *  An audio player to play compressed and uncompressed music.
 */
@LazySingleton
public class LimeWirePlayer implements Runnable, AudioPlayer {
   
    private static final Log LOG = LogFactory.getLog(LimeWirePlayer.class);
    
    /**
     * Sleep time is for when the song is loaded or paused but not playing.
     */
    private static final long SLEEP_NONPLAYING = 100;
    
    /**
     * Sleep time for when the song is playing but the sourceLine is full and 
     * no reading/writing occurs.
     */
    private static final long SLEEP_PLAYING = 100;

    /**
     *  Maximum size of the buffer to read/write from.
     */
    public static final int EXTERNAL_BUFFER_SIZE = 4096 * 8;

    /**
     * Our list of AudioPlayerListeners that are currently listening for events
     * from this player.
     */
    private List<AudioPlayerListener> listenerList = new CopyOnWriteArrayList<AudioPlayerListener>();
    
    /**
     * main thread that does the audio IO.
     */
    private Thread playerthread;

    /**
     * Synchronized holder for reading/writing the next song to be played.
     */
    private final LoadSongBuffer songBuffer;   
    
    /**
     * Used in playerThread to sleep when player is paused.
     */
    private final Object threadLock = new Object();
       
    /**
     * Current state of the player.
     */
    private volatile PlayerState playerState = UNKNOWN;

    private final Object seekLock = new Object();

    /**
     * byte location to skip to in file.
     */
    private long seekValue = -1;
    
    /**
     * true==the thread should close the current song and load the next song.
     */
    private volatile boolean loadSong = false;

    private final Object volumeLock = new Object();

    /**
     * true== the thread should update the volume on the sourceDataLine.
     */
    private boolean setVolume = false;
    
    /**
     * The current volume of the player.
     */
    private GainControl gainControl;

    /**
     * Contains the Input and Output streams for the IO
     * only <code>playerThread</code> should touch this.
     */
    private LimeAudioFormat currentAudioFormat;
    
    /**
     * The source that the thread is currently reading from.
     */
    private AudioSource currentSong;
    
    /**
     * Buffer for reading from input stream/ writing to the data line.
     */
    private final byte[] buffer = new byte[EXTERNAL_BUFFER_SIZE];
    
    /**
     * Bytes read from the input stream.
     */
    private int readBytes = 0;
    
    /**
     * Available bytes that can be written to the sourceDataLine.
     */
    private int avail;

    public LimeWirePlayer() {
        songBuffer = new LoadSongBuffer();
        gainControl = new JavaGainControl();
        gainControl.addGainChangeListener(new GainChangeListener(){
            @Override
            public void gainChange(GainChangeEvent event) {
                setVolume = true;
            }
        });
    }

    /**
     * Adds the specified AudioPlayer listener to the list.
     */
    public void addAudioPlayerListener(AudioPlayerListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified AudioPlayer listener from the list.
     */
    public void removeAudioPlayerListener(AudioPlayerListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Converts the playerstate from ints to PlayerState enums.
     */
    @Override
    public PlayerState getStatus() {
        return playerState;
    }
    
    /**
     * Loads a AudioSource into the player to play next.
     */
    @Override
    public void loadSong(AudioSource source) {
        if( source == null )
            throw new IllegalArgumentException();
        songBuffer.setSong(source);
    }

    @Override
    public void loadSong(File songFile) {
        if(LOG.isDebugEnabled())
            LOG.debug("loading " + songFile);
        loadSong(new AudioSourceImpl(songFile));        
    }

    @Override
    public void loadSong(InputStream songStream) {
        loadSong(new AudioSourceImpl(songStream));  
    }

    @Override
    public void loadSong(URL songUrl) {
        loadSong(new AudioSourceImpl(songUrl));  
    }

    /**
     * Begins playing a song.
     */
    public void playSong() {
        loadSong = true;
        playerState = PLAYING;
        if( (playerthread == null || !playerthread.isAlive()) ){ 
            playerthread = ThreadExecutor.newManagedThread(this, "LimewirePlayer");
            playerthread.setDaemon(true);
            playerthread.start();
        }
        notifyEvent(PLAYING, -1);
    }

    /**
     * Pausing the current song.
     */
    public void pause() {
        if( !(playerState == UNKNOWN || playerState == STOPPED)){
            playerState = PAUSED;
            notifyEvent(PAUSED, -1);
        }
    }

    /**
     * Unpauses the current song.
     */
    public void unpause() {
        if( !(playerState == UNKNOWN || playerState == STOPPED)){
            playerState = PLAYING;
            notifyEvent(PLAYING, -1);
        }
    }

    /**
     * Stops the current song.
     */
    public void stop() {
        if( !(playerState == UNKNOWN || playerState == STOPPED)) {
            playerState = STOPPED;
            notifyEvent(STOPPED, -1);
        }
    }

    /**
     * Seeks to a new location in the current song.
     */
    public long seekLocation(long value) {
        if( !(playerState == UNKNOWN || playerState == STOPPED) ) {
            if( playerState == PAUSED || playerState == SEEKING_PAUSED )
                playerState = SEEKING_PAUSED;
            else
                playerState = SEEKING_PLAY;
            synchronized (seekLock) {
                seekValue = value;
            }
            notifyEvent(SEEKING,value);
        }
        return value;
    }
    
    /**
     * Sets the gain(volume) for the outputline.
     * 
     * @param fGain [0.0 <-> 1.0]
     * @throws IOException thrown when the soundcard does not support this
     *         operation
     */
    public void setVolume(float fGain) {
        synchronized (volumeLock) {
            // volume = fGain;
            gainControl.setLevel(fGain);
            setVolume = true;
        }
    }
    
    public GainControl getGainControl() {
        return gainControl;
    }

    /**
     * Handles all the IO for reading and writing a song to the sound card.
     */
    public void run() {
        // while the thread is not stopped or unknown, keep alive
        while( playerState != UNKNOWN ) {

            // after stopping, wait a few milliseconds in case a new song is loaded to
            //  avoid recreating the thread
            if( playerState == STOPPED ){
                for(int i = 0; i < 3; i++) {
                    if( playerState == STOPPED )
                        try {
                            synchronized (threadLock) {
                                threadLock.wait(SLEEP_NONPLAYING);
                            }
                        } catch (InterruptedException e) { // dont catch
                        }
                }
                if( playerState == STOPPED )
                    playerState = UNKNOWN;
            }
            // update the volume if changed
            if (currentAudioFormat != null && setVolume) {
                try {
                    float vol = 0;
                    synchronized (volumeLock) {

                        vol = gainControl.getLevel();
                        setVolume = false;
                    }
                    currentAudioFormat.setGain(vol);
                    notifyEvent(GAIN, vol);

                } catch (IOException e) {
                }
            }
            // load a new song
            if( loadSong ){
                if( currentAudioFormat != null )
                    currentAudioFormat.closeStreams();    
                loadFromSongBuffer();
                if( currentSong != null ) 
                    loading();
                else
                    playerState = STOPPED;
            }
            // play the song
            if( playerState == PLAYING ){
                if(currentAudioFormat == null )
                    playerState = STOPPED;
                else 
                    playing();
            }
            // pause the song
            else if( playerState == PAUSED ){
                if( currentAudioFormat == null )
                    playerState = STOPPED;
                else
                    pausing();
            } 
            // seek to a new position in the song
            else if( playerState == SEEKING || playerState == SEEKING_PAUSED ||
                    playerState == SEEKING_PLAY){   
                seeking();
            }
        }
        if( currentAudioFormat != null)
            currentAudioFormat.closeStreams();
        currentAudioFormat = null;
        playerState = UNKNOWN;
    }
    
    /**
     * Attempts to remove the next song for playing from the songBuffer.
     * If there is no song waiting, the current song is placed back on
     * the buffer in case play() is pressed again prior to loading a new
     * song.
     */
    private void loadFromSongBuffer(){
        // if another song to play, load it and keep playing
        if( songBuffer.hasSong() ) { 
            currentSong = songBuffer.getSong();
            loadSong = false;
        }
        // if no other song to play
        else {
            loadSong = false;
            currentSong = null;
            playerState = STOPPED;
        }
    }
    
    /**
     *  Processes loading the current song.
     */
    private void loading(){          
        try {
            currentAudioFormat = new LimeAudioFormat(currentSong, 0);
            readBytes = 0;
            notifyOpened(currentAudioFormat.getProperties());
            playerState = PLAYING;
        } catch (IllegalArgumentException e ) {
            if(LOG.isDebugEnabled())
                LOG.debug("illegalArguement" + e);
            playerState = STOPPED;
            notifyEvent(NO_SOUND_DEVICE, -1);
        } catch (UnsupportedAudioFileException e) {
            if(LOG.isDebugEnabled())
                LOG.debug("unsupported audio file" + e);
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (IOException e) {
            if(LOG.isDebugEnabled())
                LOG.debug("ioexception " + e);
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (LineUnavailableException e) {
            if(LOG.isDebugEnabled())
                LOG.debug("line unavailable" + e);
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (NullPointerException e) {
            if(LOG.isDebugEnabled())
                LOG.debug("null pointer " + e);
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        }
    }
    
    /**
     * Processes playing the current song.
     */
    private void playing() {
        currentAudioFormat.startSourceDataLine();
        avail = currentAudioFormat.getSourceDataLine().available();

        // if we can write without blocking to the audio card, do so
        if (avail > 0) {
            try {
                readBytes = currentAudioFormat.getAudioInputStream().read(buffer, 0,
                        Math.min(avail, buffer.length));
            }
            // TODO: this is a hack for capturing problems in the JLayer decoder. The problems
            // should really fixed there but for the time being, this will hide the error from
            // the user and just proceed to the next song.
            catch (ArrayIndexOutOfBoundsException e) {
                playerState = STOPPED;
                notifyEvent(EOM, -1);
                loadSong = true;
            } catch (IOException e) {
                playerState = STOPPED;
            }
            // write whatever we were able to read
            if (readBytes > 0) {
                currentAudioFormat.getSourceDataLine().write(buffer, 0, readBytes);
                notifyProgress(currentAudioFormat.getEncodedStreamPosition());
            }
            // if the end of the song has been reached, see if there's a new
            // song
            // waiting to be played
            else if (readBytes == -1) {
                notifyEvent(EOM, -1);
                loadSong = true;
            }
        } else {
            try {
                synchronized (threadLock) {
                    threadLock.wait(SLEEP_PLAYING);
                }
            } catch (InterruptedException e) {
                // dont catch
            }
        }
    }
    
    /**
     * Processes pausing the current song.
     */
    private void pausing(){
        currentAudioFormat.stopSourceDataLine();
        try {
            synchronized (threadLock) {
                threadLock.wait(SLEEP_NONPLAYING);  
            }           
        } catch (InterruptedException e) {
            // dont catch
        }
    }
    
    /**
     * Processes a seek to a location in the song.
     */
    private void seeking(){
        try {
            
            long seekLocation = 0;
            synchronized (seekLock) {
                seekLocation = seekValue;
                seekValue = 0;
            }

            if (currentAudioFormat != null)
                currentAudioFormat.closeStreams();
            // open new file and seek to location
            currentAudioFormat = new LimeAudioFormat(currentSong, seekLocation);
            // TODO: the above should be replaced once the mp3 decoder is fixed

            // reload new volume
            synchronized (volumeLock) {
                setVolume = true;
            }
            if( playerState == SEEKING_PAUSED )
                playerState = PAUSED;
            else
                playerState = PLAYING;
        } catch (UnsupportedAudioFileException e) {
            playerState = STOPPED;
        } catch (IOException e) {
            playerState = STOPPED;
        } catch (LineUnavailableException e) {
            playerState = STOPPED;
        } catch (NullPointerException e) {
            playerState = STOPPED;
        } catch (IllegalArgumentException e) {
            playerState = STOPPED;
        }
    }
    
    /**
     * Notify listeners when a new audio source has been opened. 
     * 
     * @param properties any properties about the source that we extracted
     */
    protected void notifyOpened(final Map<String,Object> properties){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                fireOpened(properties);
            }
        });
    }

    /**
     * Notify listeners about an AudioPlayerEvent. This creates general state
     * modifications to the player such as the transition from opened to 
     * playing to paused to end of song.
     * 
     * @param value if the event was a modification such as a volume update,
     *        list the new value
     */
    protected void notifyEvent(final PlayerState state, final double value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireStateUpdated(new AudioPlayerEvent(state,value));
            }
        });
    }

    /**
     * fires a progress event off a new thread. This lets us safely fire events
     * off of the player thread while using a lock on the input stream.
     */
    protected void notifyProgress(final int bytesread) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireProgress(bytesread);
            }
        });
    }

    /**
     * This is fired every time a new song is loaded and ready to play. The
     * properties map contains information about the type of song such as bit
     * rate, sample rate, media type(MPEG, Streaming,etc..), etc..
     */
    protected void fireOpened(Map<String, Object> properties) {
        for (AudioPlayerListener listener : listenerList)
            listener.songOpened(properties);
    }

    /**
     * Fired every time a byte stream is written to the sound card. This lets 
     * listeners be aware of what point in the entire file is song is currently
     * playing. This also returns a copy of the written byte[] so it can get
     * passed along to objects such as a FFT for visual feedback of the song.
     */
    protected void fireProgress(int bytesread) {
        for (AudioPlayerListener listener : listenerList)
            listener.progressChange(bytesread);
    }

    /**
     * Fired every time the state of the player changes. This allows a listener
     * to be aware of state transitions such as from OPENED -> PLAYING ->
     * STOPPED -> EOF.
     */
    protected void fireStateUpdated(AudioPlayerEvent event) {
        for (AudioPlayerListener listener : listenerList)
            listener.stateChange(event);
    }

    /**
     * Holds a reference to the next song to be played.
     */
    private static class LoadSongBuffer {
        
        private AudioSource nextItem;
        
        public synchronized void setSong(AudioSource song){ 
            nextItem = song;
        }
        
        /**
         * @return the next song to be played, returns null if no new song is 
         * awaiting play
         */
        public synchronized AudioSource getSong() {
            AudioSource next = nextItem;
            nextItem = null;
            return next;
        }

        public synchronized boolean hasSong() {
            return nextItem != null;
        }
    }

    public boolean isPlaying(File file) {
        return playerState == PLAYING && currentSong != null && file.equals(currentSong.getFile());
    }

    @Override
    public boolean isPaused(File file) {
        return playerState == PAUSED && currentSong != null && file.equals(currentSong.getFile());
    }

    @Override
    public AudioSource getCurrentSong() {
        return currentSong;
    }

    private class JavaGainControl extends AbstractGainControl {
        private static final float MAX = 1.0f;
        private static final float MIN = 0.0f;
        private float level = 0.0f;

        public float getLevel() {
            return level;
        }

        public float setLevel(final float level) {
            if(level > MAX)
                this.level = MAX;
            else if(level < MIN)
                this.level = MIN;
            else
                this.level = level;
            
            notifyListenersGainChangeEvent();
            
            return this.level;
        }

    }
}