package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.metadata.audio.reader.AudioDataReader;
import com.limegroup.gnutella.metadata.audio.reader.MP3Reader;
import com.limegroup.gnutella.metadata.audio.reader.WMAReader;
import com.limegroup.gnutella.metadata.audio.writer.AudioDataEditor;
import com.limegroup.gnutella.metadata.audio.writer.MP3DataEditor;
import com.limegroup.gnutella.metadata.bittorrent.TorrentMetaReader;
import com.limegroup.gnutella.metadata.video.reader.MOVMetaData;
import com.limegroup.gnutella.metadata.video.reader.MPEGMetaData;
import com.limegroup.gnutella.metadata.video.reader.OGMMetaData;
import com.limegroup.gnutella.metadata.video.reader.RIFFMetaData;
import com.limegroup.gnutella.metadata.video.reader.WMMetaReader;
import com.limegroup.gnutella.metadata.video.reader.WMVMetaData;

/**
 * Implementation of MetaDataFactory. Returns the appropriate reader/writer for
 * the file type if one exists, null if one does not exist.
 */
@Singleton
public class MetaDataFactoryImpl implements MetaDataFactory {

    private static final Log LOG = LogFactory.getLog(MetaDataFactory.class);
    
    private final ConcurrentMap<String, MetaReader> readerByExtension = new ConcurrentHashMap<String, MetaReader>();
    
    private final Set<String> audioExtensions = new HashSet<String>();
    private final Set<String> videoExtensions = new HashSet<String>();
    
    private final ConcurrentMap<String, MetaWriter> writerByExtension = new ConcurrentHashMap<String, MetaWriter>();
    
    public MetaDataFactoryImpl() {

        registerAudioReader(new MP3Reader());
        registerAudioReader(new AudioDataReader());
        registerAudioReader(new WMAReader());
        
        registerVideoReader(new RIFFMetaData());
        registerVideoReader(new OGMMetaData());
        registerVideoReader(new WMVMetaData());
        registerVideoReader(new MPEGMetaData());
        registerVideoReader(new MOVMetaData());
        
        registerMultiFormat(new WMMetaReader());
        
        registerEditor(new MP3DataEditor());
        registerEditor(new AudioDataEditor());
        
        registerReader(new TorrentMetaReader());
    }
    
    /**
     * Factory method which returns an instance of MetaDataEditor which
     * should be used with the specific file.
     * @param name the name of the file to be annotated
     * @return the MetaDataEditor that will do the annotation.  null if the
     * lime xml repository should be used.
     */
    public MetaWriter getEditorForFile(String name) {
        String extension = FileUtils.getFileExtension(name);
        if (!extension.isEmpty()) {
            MetaWriter writer = writerByExtension.get(extension.toLowerCase(Locale.US));
            return writer;
        }
        return null;
    }
    
    /**
     * Registers this reader as both an audio and video format reader. Some extensions such
     * as .asf, .wm, .mp4 can contain both audio or video data.
     */
    private void registerMultiFormat(MetaReader reader) {
        for (String extension : reader.getSupportedExtensions()) {
            MetaReader existingReader = readerByExtension.put(extension, reader);
            audioExtensions.add(extension);
            videoExtensions.add(extension);
            if (existingReader != null) {
                throw new IllegalArgumentException("factory: " + existingReader.getClass() + " already resistered for: " + extension);
            }
        }
    }

	/**
     * Registers a reader of audio files. The reader is registered with all the
     * associated file extensions it can read. If a reader already exists for
     * this file type, an exception is thrown.
     */
    private void registerAudioReader(MetaReader reader) {
        for (String extension : reader.getSupportedExtensions()) {
            MetaReader existingReader = readerByExtension.put(extension, reader);
            audioExtensions.add(extension);
            if (existingReader != null) {
                throw new IllegalArgumentException("factory: " + existingReader.getClass()
                        + " already resistered for: " + extension);
            }
        }
    }

    /**
     * Registers a reader of video files. The reader is registered with all the
     * associated file extensions it can read. If a reader already exists for
     * this file type, an exception is thrown.
     */
    private void registerVideoReader(MetaReader reader) {
        for (String extension : reader.getSupportedExtensions()) {
            MetaReader existingReader = readerByExtension.put(extension, reader);
            videoExtensions.add(extension);
            if (existingReader != null) {
                throw new IllegalArgumentException("factory: " + existingReader.getClass() + " already resistered for: " + extension);
            }
        }
    }
    
    /**
     * Registers a writer of meta data. The writer is registered
     * with all the associated file extensions it can write. If a writer
     * already exists for this file type, an exception is thrown.
     */
    private void registerEditor(MetaWriter writer) {
        for (String extension : writer.getSupportedExtensions()) {
            MetaWriter existingWriter = writerByExtension.put(extension, writer);
            if (existingWriter != null) {
                throw new IllegalArgumentException("factory: " + existingWriter.getClass() + " already resistered for: " + extension);
            }
        }
    }
    
    public MetaData parse(File file) throws IOException {
        try {
            MetaReader reader = getMetaReader(file);
            if (reader != null) {
                return reader.parse(file);
            }
        } catch (OutOfMemoryError e) {
            LOG.warn("Ran out of memory while parsing.",e);
            throw (IOException)new IOException().initCause(e);
        } catch (Exception e) {
            LOG.warn("Exception parsing file.",e);
            throw (IOException)new IOException().initCause(e);
        }
        return null;
    }
    
    /** Creates MetaData for the file, if possible. */  
    public MetaReader getMetaReader(File file) {
        String extension = FileUtils.getFileExtension(file);
        if (!extension.isEmpty()) {
            MetaReader reader = readerByExtension.get(extension.toLowerCase(Locale.US));
            return reader;
        }
        return null;
    }


    @Override
    public boolean containsEditor(String name) {
        String extension = FileUtils.getFileExtension(name);
        if (!extension.isEmpty() && writerByExtension.get(extension.toLowerCase(Locale.US)) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean containsReader(File file) {
        return getMetaReader(file) != null;
    }
    
    @Override
    public boolean containsAudioReader(File file) {
        String extension = FileUtils.getFileExtension(file);
        if (!extension.isEmpty() && audioExtensions.contains(extension.toLowerCase(Locale.US))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean containsVideoReader(File file) {
        String extension = FileUtils.getFileExtension(file);
        if (!extension.isEmpty() && videoExtensions.contains(extension.toLowerCase(Locale.US))) {
            return true;
        }
        return false;
    }

    @Override
    public void registerReader(MetaReader reader) {
        for (String extension : reader.getSupportedExtensions()) {
            MetaReader existingReader = readerByExtension.put(extension, reader);
            if (existingReader != null) {
                throw new IllegalArgumentException("factory: " + existingReader.getClass() + " already resistered for: " + extension);
            }
        }
    }
}
