package org.limewire.ui.swing.player;

import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.ClockStoppedException;
import javax.media.Control;
import javax.media.Controller;
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

import net.sf.fmj.utility.URLUtils;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;

import com.google.inject.Provider;

/**
 * Wraps the LimeWirePlayer in a javax.media.Player. This allows us to reuse
 * the LWPlayer using the FMJ Player. 
 */
public class JavaSoundPlayer implements Player {
    
    private static final Log LOG = LogFactory.getLog(JavaSoundPlayer.class);
    
    private static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";
    private static final String AUDIO_TYPE = "audio.type";

    private static final String MP3 = "mp3";
    private static final String WAVE = "wave";
    
    private final Provider<AudioPlayer> audioPlayerProvider;
    private List<ControllerListener> listenerList = new CopyOnWriteArrayList<ControllerListener>();
    private PlayerListener playerListener;
    private volatile int state = Unrealized;
    /** Map containing properties for the last opened song. */
    private volatile Map audioProperties = null;
    private volatile long bytesRead = 0;
    private volatile Time duration = null;
    
    /** Audio player component. */
    private AudioPlayer audioPlayer;
    
    public JavaSoundPlayer(Provider<AudioPlayer> audioPlayerProvider) {
        this.audioPlayerProvider = audioPlayerProvider;
    }
    
    /**
     * Returns the audio player component.  When first called, this method
     * creates the component and registers this mediator as a listener.
     */
    private AudioPlayer getPlayer() {
        if (audioPlayer == null) {
            audioPlayer = audioPlayerProvider.get();
            playerListener = new PlayerListener();
            audioPlayer.addAudioPlayerListener(playerListener);
        }
        return audioPlayer;
    }

    @Override
    public void setSource(DataSource source) throws IncompatibleSourceException {        
        if (!source.getLocator().getProtocol().equals("file")) {
            LOG.debug("attempted to load non-file");
            throw new IncompatibleSourceException("Only file URLs supported: " + source);
        }

        String path = URLUtils.extractValidPathFromFileUrl(source.getLocator().toExternalForm());

        if(path == null) {
            LOG.debug("could not locate path of file");
            throw new IncompatibleSourceException("Cannot find file path");
        }
        
        File file = new File(path);
        if(!isPlayable(file)) {
            LOG.debug("unplayable file");
            throw new IncompatibleSourceException("Unplayable file type");
        }
        try {
            getPlayer().loadSong(file);
        } catch(Exception e) {
            if(LOG.isDebugEnabled())
                LOG.debug("failed to load file " + e);
            throw new IncompatibleSourceException("failed to load javasoundplayer");
        }
        
        bytesRead = 0;
    }


    @Override
    public Component getControlPanelComponent() {
        return null;
    }

    @Override
    public GainControl getGainControl() {
        if(audioPlayer != null)
            return audioPlayer.getGainControl();
        return null;
    }

    @Override
    public Component getVisualComponent() {
        return null;
    }
    
    private boolean isPlayable(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
    }

    @Override
    public void start() {
        state = Started;
        PlayerState status = getPlayer().getStatus();
        if (status != PlayerState.PAUSED) {
            audioPlayer.stop();
            audioPlayer.playSong();
        } else {
            audioPlayer.unpause();
        }
    }

    @Override
    public void close() {
        stop();
    }

    @Override
    public void deallocate() {
        audioProperties = null;
        if(playerListener != null && audioPlayer != null)
            audioPlayer.removeAudioPlayerListener(playerListener);
        audioPlayer = null;
    }

    @Override
    public Control getControl(String forName) {
        return null;
    }

    @Override
    public Control[] getControls() {
        if(audioPlayer != null)
            return new Control[]{audioPlayer.getGainControl()};
        else
            return new Control[]{};
    }

    @Override
    public Time getStartLatency() {
        return new Time(0);
    }

    @Override
    public int getState() {
        return state;
    }
    @Override
    public Time getMediaTime() {
        if ((audioProperties != null) && audioProperties.containsKey(AUDIO_LENGTH_BYTES) && audioProperties.containsKey("duration")) {
            return new Time(bytesRead);
        }
        return new Time(0);
    }
    
    @Override
    public void setMediaTime(Time now) {
        if(audioPlayer != null && audioProperties != null && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
            // currently, only mp3 and wav files can be seeked upon
            if (isSeekable((String) audioProperties.get(AUDIO_TYPE))) {
                float percent = now.getNanoseconds()/((float)getDuration().getNanoseconds());
                final long skipBytes = Math.round((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)* percent);
                getPlayer().seekLocation(skipBytes);
            }
        }
    }
    
    private static boolean isSeekable(String songType) {
        if (songType == null) {
            return false;
        }
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }
    
    public void pause() {
        if(audioPlayer != null)
            audioPlayer.pause();
    }

    @Override
    public void stop() {
        if(audioPlayer != null)
            audioPlayer.stop();
    }

    @Override
    public Time getDuration() {
        if(duration == null)
            return Time.TIME_UNKNOWN;
        else
            return duration;
    }
    
    @Override
    public void addControllerListener(ControllerListener listener) {
        listenerList.add(listener);
    }
    
    @Override
    public void removeControllerListener(ControllerListener listener) {
        listenerList.remove(listener);
    }
    
    @Override
    public void removeController(Controller oldController) {}
    @Override
    public void addController(Controller newController) throws IncompatibleTimeBaseException {}
    @Override
    public int getTargetState() { return 0; }
    @Override
    public void prefetch() {}
    @Override
    public void realize() {}
    @Override
    public long getMediaNanoseconds() { return 0; }
    @Override
    public float getRate() { return 0; }
    @Override
    public Time getStopTime() { return null; }
    @Override
    public Time getSyncTime() { return null; }
    @Override
    public TimeBase getTimeBase() { return null; }
    @Override
    public Time mapToTimeBase(Time t) throws ClockStoppedException { return null; }
    @Override
    public float setRate(float factor) { return 0; }
    @Override
    public void setStopTime(Time stopTime) {}
    @Override
    public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException {}
    @Override
    public void syncStart(Time at) {}
    
    private class PlayerListener implements AudioPlayerListener {
        @Override
        public void progressChange(int read) {
            bytesRead = read;
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            audioProperties = properties;
            if(audioProperties != null && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                duration = new Time((Integer)audioProperties.get(AUDIO_LENGTH_BYTES));
            } else {
                duration = Time.TIME_UNKNOWN;
            }
            
            for(ControllerListener listener : listenerList) {
                listener.controllerUpdate(new StartEvent(JavaSoundPlayer.this, 0, 0, 0, null, null));
            }
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            // Go to next song when finished.
            if (event.getState() == PlayerState.EOM) {
                state = Unrealized;
                for(ControllerListener listener : listenerList) {
                    listener.controllerUpdate(new EndOfMediaEvent(JavaSoundPlayer.this, 0, 0, 0, null));
                }
            } else if(event.getState() == PlayerState.STOPPED || event.getState() == PlayerState.PAUSED) {
                state = Realized;
                for(ControllerListener listener : listenerList) {
                    listener.controllerUpdate(new StopEvent(JavaSoundPlayer.this, 0, 0, 0, null));
                }
            } else if(event.getState() == PlayerState.PLAYING) {
                state = Started;
                for(ControllerListener listener : listenerList) {
                    listener.controllerUpdate(new StartEvent(JavaSoundPlayer.this, 0, 0, 0, null, null));
                }
            } else {
                state = Realized;
            }
        }
    }
}
