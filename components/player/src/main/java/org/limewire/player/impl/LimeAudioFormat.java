package org.limewire.player.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.limewire.player.api.AudioSource;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;
import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * <p>
 *  This handles creation/destruction of an audio source. This includes
 *  extracting audio properties about the source and any decoding that may
 *  need to take place. Once created, the owner can safely read/write from 
 *  the input stream and to the dataline.
 *  </p>
 *  <p>
 *  When opening an input stream to read from, encoded formats such as
 *  .mp3, .flac, .ogg, .mp4, etc.., must be wrapped in their own unique 
 *  audioInputStream which will decode all input streams into a PCM format.
 *  PCM is a format that the sound card will understand.
 *  </p>
 *  The process of initializing a new song to read from is as follows:
 *  <pre>
 *      - create an AudioInputStream (this creates AudioFormat information about the
 *              encoding of the input stream, such as # of channels, sameple rate, 
 *              encoding format, etc.)
 *      - create a decoded AudioInputStream (now that we have the AudioFormat information
 *              about the encoded audio source, we can construct a proper AudioInputStream
 *              that will decode the audio source into PCM format)
 *      - Optional: extract audio properties about stream
 *      - create a SourceDataLine (depending on a the AudioFormat of the input stream and the
 *              sound card, a proper data line must be created to write to. An input Stream 
 *              in mono will be handled differently than one in stereo, etc.) The sourceDataLine
 *              will write the information to the sound card for playback.
 *      - reading/writing - this is handled by the Object that created this instance
 *      - Finally: upon completion of writing the song, the input streams and 
 *              sourceDataLine must be closed and discarded.
 *  </pre>
 */
public class LimeAudioFormat {

    /**
     * Property values that are loaded into the properties map if we can't 
     * parse the data using TAudioFileFormat.
     */
    public static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";

    public static final String AUDIO_LENGTH_FRAMES = "audio.length.frames";

    public static final String AUDIO_TYPE = "audio.type";

    public static final String AUDIO_FRAMERATE_FPS = "audio.framerate.fps";

    public static final String AUDIO_FRAMESIZE_BYTES = "audio.framesize.bytes";

    public static final String AUDIO_SAMPLERATE_HZ = "audio.samplerate.hz";

    public static final String AUDIO_SAMPLESIZE_BITS = "audio.samplesize.bits";

    public static final String AUDIO_CHANNELS = "audio.channels";

    /**
     * Stream for reading from the audioSource.
     */
    private  AudioInputStream audioInputStream;

    /**
     * Stream for writing audio data to sound card from.
     */
    private SourceDataLine sourceDataLine;

    /**
     * Stream for reading in encodedFormat. After decoding the inputStream, its
     * no longer possible to use stream.available() to give us the correct
     * current location so we save a reference of the encodedStream.
     */
    private AudioInputStream encodedAudioInputStream;

    /**
     * Audio source currently reading from (url, file, input stream).
     */
    private AudioSource audioSource;

    /**
     * Properties of the current audio source.
     */
    private Map<String, Object> properties;

    /**
     * Total length of the current song in bytes.
     */
    private long totalLength;

    /**
     * Control for the gain on the sourceDataLine.
     */
    private FloatControl gainControl;

    
    /**
     * Loads a file into the player and initializes all the input and output streams.
     * After loading the audio source, it is safe to begin reading/writing the audio
     * source to the data line.
     */
    public LimeAudioFormat(File file, long position) throws UnsupportedAudioFileException, 
                    IOException, LineUnavailableException, NullPointerException {
        
//        this( new AudioSource(file), position );
    }

    /**
     * Loads a stream into the player and initializes all the input and output streams
     * After loading the audio source, it is safe to begin reading/writing the audio
     * source to the data line.
     */
    public LimeAudioFormat(InputStream stream, long position) throws UnsupportedAudioFileException, 
                    IOException, LineUnavailableException, NullPointerException {
//        this( new AudioSource(stream), position );
    }

    /**
     * Loads an audioSource into the player and initializes all the input and output streams
     * After loading the audio source, it is safe to begin reading/writing the audio
     * source to the data line.
     */ 
    public LimeAudioFormat(AudioSource audioSource, long position) throws UnsupportedAudioFileException, 
                    IOException, LineUnavailableException, NullPointerException {
        if (audioSource == null)
            throw new NullPointerException("Couldn't load song");
            
        this.audioSource = audioSource;
        encodedAudioInputStream = createAudioInputStream(audioSource, position);
        properties = createProperties(audioSource);
        if( audioSource.getFile() != null )
            totalLength = audioSource.getFile().length();
        else
            totalLength = encodedAudioInputStream.available();
        audioInputStream = createDecodedAudioInputStream(encodedAudioInputStream);
        sourceDataLine = createSourceDataLine(audioInputStream);
    }

    /**
     * Creates an audioInputStream for reading from. An audioInputStream is 
     * an inputStream with a specified audio format and length. Unlike 
     * InputStreams, the length is expressed in frames not bytes and the 
     * AudioFormat contains specifications for how the input stream is encoded 
     * such as number of bytes per frame, sample rate, # of channels, etc.. 
     * <p>
     *  NOTE: The audioInputStream returned here is not guaranteed to
     *  write to the sound card. Most audio sources, even .wav  files already 
     *  in PCM format, need to be decoded to a proper format that the 
     *  sourceDataLine can understand prior to reading from.
     * 
     * @param source audio source to read from, either a file, url or 
     *      inputStream
     * @param skip number of frames from the beginning of the file to skip
     * @return AudioInputStream - based on <code>source</code> creates an input 
     *      stream containing audioFormat properties about the encoding of the
     *      stream
     */
    public static AudioInputStream createAudioInputStream(AudioSource source,
            long skip) throws UnsupportedAudioFileException, IOException, NullPointerException {
        AudioInputStream stream; 
        if (source.getFile() != null) {
            // skip doesn't guarantee to return to skip the exact number of frames
            // requested, don't try and skip to close to the EOF to avoid overflow
            if( skip < 0 || skip > source.getFile().length() - 10000 )
                skip = 0;
            // use RandomAccessStreams to speed up mp3 searches since its encoded
            if (source.getFile().getName().toLowerCase(Locale.US).endsWith(".mp3")) {
                RandomAudioInputStream i = new RandomAudioInputStream(
                        new RandomAccessFile(source.getFile(), "rw"));
                i.skip(skip);
                stream = AudioSystem.getAudioInputStream(i);
            } else {
                stream = AudioSystem.getAudioInputStream(source.getFile());
                stream.skip(skip);
            }

        } else if (source.getStream() != null) {
            stream = AudioSystem.getAudioInputStream(source.getStream());
        } else if ( source.getURL() != null ) {
            stream = AudioSystem.getAudioInputStream(source.getURL().openStream());
        }
        else
            throw new IllegalArgumentException("Attempting to open invalid audio source");
        return stream;
    }

    /**
     * Creates a decoded audioInputStream. All audio input streams must be in a
     * PCM format compatible with the OS and sound card in order to written 
     * correctly by the sound card. To write to the soundcard we open a source 
     * data line to read data from the input stream. The sourceDataLine expects 
     * data to be in a specific audio format regardless of how the data is encoded. 
     * <p>
     * To ensure that all supported formats are decoded properly, the original 
     * audioInputStream is decoded into a new audioInputStream. The java AudioSystem
     * uses a bit of reflection to create a new AudioInputStream which can decode 
     * a given audioInputStream into a PCM formatted stream. 
     * 
     * 
     * @param audioInputStream encoded inputStream to read from which contains 
     *          specific audioFormat properties such as a number of channels, 
     *          encoding method, sample rate, etc..
     * @return AudioInputStream a decoded audioInputStream in PCM format
     */
    public static AudioInputStream createDecodedAudioInputStream(
            AudioInputStream audioInputStream) {
        AudioFormat sourceFormat = audioInputStream.getFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, sourceFormat, LimeWirePlayer.EXTERNAL_BUFFER_SIZE);
        
        // if audioInputStream already in PCM format of audio card, do nothing
        if( AudioSystem.isLineSupported(info)) { 
            return audioInputStream;
        } else {        
            int nSampleSizeInBits = sourceFormat.getSampleSizeInBits();
            if (nSampleSizeInBits <= 0)
                nSampleSizeInBits = 16;
            if ((sourceFormat.getEncoding() == AudioFormat.Encoding.ULAW)
                    || (sourceFormat.getEncoding() == AudioFormat.Encoding.ALAW))
                nSampleSizeInBits = 16;
            if (nSampleSizeInBits != 8)
                nSampleSizeInBits = 16;
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(),
                    nSampleSizeInBits, sourceFormat.getChannels(), sourceFormat
                            .getChannels()
                            * (nSampleSizeInBits / 8),
                    sourceFormat.getSampleRate(), false);
            
            info = new DataLine.Info(SourceDataLine.class, targetFormat, LimeWirePlayer.EXTERNAL_BUFFER_SIZE);
            // Use reflection to try and load the proper decoded to create a decoded stream.
            return AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
        }
    }

    /**
     * Opens a sourceDataLine for writing to an audio card from a given inputstream. 
     * SourceDataLines are the link between the source of an audiostream and the java Mixer. 
     * From the Mixer, all the input streams are combined and written to the sound card. 
     * SourceDataLines wrap a given audioInputStream and ensures that
     * all inputs to the mixer are in the same format.
     * <p>
     * Each audioInputStream contains an audioFormat( ie. # of channels, frame size, sample
     * rate, etc.). A SourceDataLine is created based on the audioFormat's properties. 
     * 
     * @param audioInputStreamthe decoded audio input stream that is being read from
     * @return SourceDataLine a properly formated data line to write to based on the 
     *              audio format of the audioInputStream
     */
    private SourceDataLine createSourceDataLine(AudioInputStream audioInputStream)
            throws LineUnavailableException {
        return createSourceDataLine(audioInputStream, -1);
    }

    /**
     * Opens a sourceDataLine for writing to an audio card from a given inputstream. 
     * SourceDataLines are the link between the source of an audiostream and the java Mixer. 
     * From the Mixer, all the input streams are combined and written to the sound card. 
     * SourceDataLines wrap a given audioInputStream and ensures that
     * all inputs to the mixer are in the same format.
     * <p>
     * Each audioInputStream contains an audioFormat( ie. # of channels, frame size, sample
     * rate, etc.). A SourceDataLine is created based on the audioFormat's properties. 
     * 
     * @param audioInputStream the decoded audio input stream that is being read from
     * @return SourceDataLine a properly formated data line to write to based on the 
     *              audio format of the audioInputStream
     */
    private SourceDataLine createSourceDataLine(
            AudioInputStream audioInputStream, int bufferSize)
            throws LineUnavailableException {
        if( audioInputStream == null )
            throw new NullPointerException("input stream is null");

        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                audioFormat, AudioSystem.NOT_SPECIFIED);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

        if (bufferSize <= 0)
            bufferSize = line.getBufferSize();
        line.open(audioFormat, bufferSize);

        /*-- Is Gain Control supported ? --*/
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            gainControl = (FloatControl) line
                    .getControl(FloatControl.Type.MASTER_GAIN);
        }
        return line;
    }

    /**
     * Creates a map of properties about the current inputstream. Unlike many inputStreams,
     * audioInputStreams have a variety of extra properties associated with them such as
     * <pre>
     *  - frame size
     *  - sample rate
     *  - frames per second
     *  - audio type
     *  - length in # of frames
     *  - # of audio channels
     *  - etc.
     * </pre>
     * This information is often useful to the application that initiated the song. This information
     * is extracted in case another class wishes to use it. 
     * 
     * @param source the audio source that the audioInputStream is created from for reading
     * @return a Map<String,Object> containing properties about the audio source
     */
    private static Map<String, Object> createProperties(AudioSource source)
            throws UnsupportedAudioFileException, IOException {

        AudioFileFormat audioFileFormat;
        Map<String, Object> properties = new HashMap<String, Object>();

        if (source.getFile() != null) {
            audioFileFormat = AudioSystem.getAudioFileFormat(source.getFile());
        } else if (source.getStream() != null) {
            audioFileFormat = AudioSystem
                    .getAudioFileFormat(source.getStream());
        } else
            return properties;

        if (audioFileFormat instanceof TAudioFileFormat) {
            // Tritonus SPI compliant audio file format.
            properties = GenericsUtils.scanForMap(
                    ((TAudioFileFormat) audioFileFormat).properties(),
                    String.class, Object.class, ScanMode.REMOVE);
            // Clone the Map because it is not mutable.
            Map<String, Object> newMap = new HashMap<String, Object>(properties);
            properties = newMap;
        } 
        // Add JavaSound properties.
        if (audioFileFormat.getByteLength() > 0)
            properties.put(AUDIO_LENGTH_BYTES, audioFileFormat.getByteLength());
        if (audioFileFormat.getFrameLength() > 0)
            properties.put(AUDIO_LENGTH_FRAMES, audioFileFormat.getFrameLength());
        if (audioFileFormat.getType() != null)
            properties.put(AUDIO_TYPE, (audioFileFormat.getType().toString()));

        AudioFormat audioFormat = audioFileFormat.getFormat();

        if (audioFormat.getFrameRate() > 0)
            properties.put(AUDIO_FRAMERATE_FPS, audioFormat.getFrameRate());
        if (audioFormat.getFrameSize() > 0)
            properties.put(AUDIO_FRAMESIZE_BYTES, audioFormat.getFrameSize());
        if (audioFormat.getSampleRate() > 0)
            properties.put(AUDIO_SAMPLERATE_HZ, audioFormat.getSampleRate());
        if (audioFormat.getSampleSizeInBits() > 0)
            properties.put(AUDIO_SAMPLESIZE_BITS, audioFormat
                    .getSampleSizeInBits());
        if (audioFormat.getChannels() > 0)
            properties.put(AUDIO_CHANNELS, audioFormat.getChannels());
        if (audioFormat instanceof TAudioFormat) {
            // Tritonus SPI compliant audio format.
            Map<String, Object> addproperties = GenericsUtils.scanForMap(
                    ((TAudioFormat) audioFormat).properties(), String.class,
                    Object.class, ScanMode.REMOVE);
            properties.putAll(addproperties);
        }
        return properties;
    }
    

    /**
     * @return the audio source of the inputStream
     */
    public AudioSource getSource() {
        return audioSource;
    }

    /**
     * @return the audioInputStream for reading from
     */
    public AudioInputStream getAudioInputStream() {
        return audioInputStream;
    }

    /**
     * @return the SourceDataLine for writing to
     */
    public SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }

    /**
     * @return the properties associated with this audio source
     *      such as sampleRate, framesize, number of frames, etc..
     */
    public Map<String, Object> getProperties() {
        if(properties != null )
            return properties;
        else
           return new HashMap<String,Object>();
    }

    /**
     * @return the total number of frames in the input stream 
     */
    public long totalLength() {
        return totalLength;
    }

    /**
     * @return the number of frames left to read.
     */
    public int available() {
        int avail = -1;
        if ( encodedAudioInputStream != null ) {
            try {
                avail = encodedAudioInputStream.available();
            } catch (IOException e) {
                //don't catch, can't read from stream
            }
        }
        return avail;
    }
       
    /**
     * Prior to writing to a new or stopped sourceDataLine, the dataLine needs to
     * be opened.
     */
    public void startSourceDataLine(){
        if( sourceDataLine != null && !sourceDataLine.isRunning())
            sourceDataLine.start();
    }
    
    /**
     * Stops the current sourceDataLine from writing. This should be called when the 
     * stream has been paused with intent to reopen it.
     */
    public void stopSourceDataLine(){
        if( sourceDataLine != null && sourceDataLine.isRunning()){
            sourceDataLine.flush();
            sourceDataLine.stop();
        }
    }
    
    /**
     * @return frame position in the current song being played
     */
    public int getEncodedStreamPosition() {
        return (int)(totalLength - available());
    }

    /**
     * Seeks to a current position in the song.
     * 
     * @param position position from the beginning of the file to seek to
     * @return the number of bytes actually skipped
     */
    public long seek(long position) {
        //TODO: modify javazoom mp3 decoder to support RandomAccessFiles at the bit level
        //      and add a new interface for a seek method. 
       return -1; 
    }
    
    /**
     *  Closes all the open streams. This is a convenience method for when the 
     *  the song is done being read from. 
     */
    public void closeStreams() {
        //close our IO streams
       FileUtils.close(encodedAudioInputStream);
       FileUtils.close(audioInputStream);

        if (sourceDataLine != null) {
            sourceDataLine.stop();
            sourceDataLine.close();
        }
    }
    
    /**
     * Returns Gain value.
     */
    public float getGainValue() {
        if (hasGainControl()) {
            return gainControl.getValue();
        } else {
            return 0.0F;
        }
    }

    /**
     * Gets max Gain value.
     */
    public float getMaximumGain() {
        if (hasGainControl()) {
            return gainControl.getMaximum();
        } else {
            return 0.0F;
        }
    }

    /**
     * Gets min Gain value.
     */
    public float getMinimumGain() {
        if (hasGainControl()) {
            return gainControl.getMinimum();
        } else {
            return 0.0F;
        }
    }

    /**
     * Returns true if Gain control is supported.
     */
    public boolean hasGainControl() {
        if (gainControl == null) {
            // Try to get Gain control again (to support J2SE 1.5)
            if ((sourceDataLine != null)
                    && (sourceDataLine
                            .isControlSupported(FloatControl.Type.MASTER_GAIN)))
                gainControl = (FloatControl) sourceDataLine
                        .getControl(FloatControl.Type.MASTER_GAIN);
        }
        return gainControl != null;
    }

    /**
     * Sets the gain(volume) for the outputline
     * 
     * @param gain [0.0 <-> 1.0]
     * @throws IOException thrown when the soundcard does not support this
     *         operation
     */
    public void setGain(double fGain) throws IOException {
        if (hasGainControl()) {
            double ampGainDB = ((10.0f / 20.0f) * getMaximumGain())
                    - getMinimumGain();
            double cste = Math.log(10.0) / 20;
            double valueDB = getMinimumGain() + (1 / cste)
                    * Math.log(1 + (Math.exp(cste * ampGainDB) - 1) * fGain);

            gainControl.setValue((float) valueDB);
        } else
            throw new IOException("Volume error");
    }


}