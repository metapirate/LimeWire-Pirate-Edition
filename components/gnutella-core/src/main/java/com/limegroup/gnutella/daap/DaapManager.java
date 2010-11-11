package com.limegroup.gnutella.daap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DaapSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Join;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.listener.EventListener;
import org.limewire.service.MessageService;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.LibraryStatusEvent;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

import de.kapsi.net.daap.AutoCommitTransaction;
import de.kapsi.net.daap.DaapAuthenticator;
import de.kapsi.net.daap.DaapConfig;
import de.kapsi.net.daap.DaapFilter;
import de.kapsi.net.daap.DaapServer;
import de.kapsi.net.daap.DaapStreamSource;
import de.kapsi.net.daap.DaapUtil;
import de.kapsi.net.daap.Database;
import de.kapsi.net.daap.Library;
import de.kapsi.net.daap.Playlist;
import de.kapsi.net.daap.Song;
import de.kapsi.net.daap.Transaction;

/**
 * This class handles the mDNS registration and acts as an
 * interface between LimeWire and DAAP.
 */
@EagerSingleton
public class DaapManager {
    
    private static final Log LOG = LogFactory.getLog(DaapManager.class);
    private final ScheduledExecutorService backgroundExecutor;
    private final com.limegroup.gnutella.library.Library coreLibrary;
    private final FileView gnutellaFileView;
    private final Provider<IPFilter> ipFilter;
    private final Provider<NetworkInstanceUtils> networkInstanceUtils;
    private final Provider<ActivityCallback> activityCallback;
    
    /**
     * A shared processing queue for disk-related tasks.
     */
    private static final ExecutorService DAAP_EVENT_QUEUE = 
                         ExecutorsHelper.newProcessingQueue("DAAPQUEUE");

    private Library library;
    private Database database;
    private Playlist masterPlaylist;
    private Playlist whatsNew;
    private Playlist creativecommons;
    private Playlist videos;
        
    private DaapServer server;
    
    private BonjourService bonjour;
    private AutoCommitTransaction autoCommitTxn;
    
    private boolean enabled = false;
    private int maxPlaylistSize;
    
    private Map<URN, Song> urnToSong;  
    
    @Inject
    public DaapManager( @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                        Provider<IPFilter> ipFilter,
                        Provider<NetworkInstanceUtils> networkInstanceUtils,
                        Provider<ActivityCallback> activityCallback,
                        @GnutellaFiles FileView gnutellaFileView, 
                        com.limegroup.gnutella.library.Library coreLibrary) {
        this.backgroundExecutor = backgroundExecutor;
        this.coreLibrary = coreLibrary;
        this.ipFilter = ipFilter;
        this.networkInstanceUtils = networkInstanceUtils;
        this.activityCallback = activityCallback;
        this.gnutellaFileView = gnutellaFileView;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            @Asynchronous(join = Join.NONE)
            public void start() {
                if (DaapSettings.DAAP_ENABLED.getValue()) {
                    try {
                        DaapManager.this.start();
                    } catch (IOException err) {
                        LOG.debug("error starting DAAP", err);
                        MessageService.showError(I18nMarker.marktr("LimeWire was unable to start the Digital Audio Access Protocol Service (for sharing files in iTunes). This feature will be turned off. You can turn it back on in options, under Advanced -> Files -> iTunes."));
                        DaapSettings.DAAP_ENABLED.setValue(false);
                    }
                }
            }

            @Asynchronous (daemon = false)
            public void stop() {
                DaapManager.this.stop();
            }

            public void initialize() {
                coreLibrary.addManagedListStatusListener(new EventListener<LibraryStatusEvent>() {
                    @Override
                    public void handleEvent(LibraryStatusEvent event) {
                        handleManagedListStatusEvent(event);
                    }
                });
                gnutellaFileView.addListener(new EventListener<FileViewChangeEvent>() {
                    @Override
                    public void handleEvent(FileViewChangeEvent event) {
                        handleFileListEvent(event);
                    }
                });
            }

            public String getServiceName() {
                return "DAAP";
            }
        }).in(ServiceStage.VERY_LATE);
    }
    
    /**
     * Starts the DAAP Server.
     */
    public synchronized void start() throws IOException {
        
        if (!isServerRunning()) {
            
            try {
                
                InetAddress addr = NetworkUtils.getLocalAddress();
                
                bonjour = new BonjourService(addr);
                urnToSong = new HashMap<URN, Song>();
                
                maxPlaylistSize = DaapSettings.DAAP_MAX_LIBRARY_SIZE.getValue();
                
                String name = DaapSettings.DAAP_LIBRARY_NAME.get();
                
                library = new Library(name);
                autoCommitTxn = new AutoCommitTransaction(library);
                
                database = new Database(name);
                whatsNew = new Playlist(activityCallback.get().translate(I18nMarker.marktr("What's New")));
                creativecommons = new Playlist(activityCallback.get().translate(I18nMarker.marktr("Creative Commons")));
                videos = new Playlist(activityCallback.get().translate(I18nMarker.marktr("Video")));
                
                library.addDatabase(null, database);
                database.addPlaylist(null, creativecommons);
                database.addPlaylist(null, whatsNew);
                creativecommons.setSmartPlaylist(null, true);
                whatsNew.setSmartPlaylist(null, true);
                masterPlaylist = database.getMasterPlaylist();

                LimeConfig config = new LimeConfig(addr);
                
                if (DaapSettings.DAAP_REQUIRES_PASSWORD.getValue()) {
                    if (DaapSettings.DAAP_REQUIRES_USERNAME.getValue()) {
                        config.setAuthenticationMethod(DaapConfig.USERNAME_AND_PASSWORD);
                        config.setAuthenticationScheme(DaapConfig.DIGEST_SCHEME);
                    } else {
                        config.setAuthenticationMethod(DaapConfig.PASSWORD);
                        config.setAuthenticationScheme(DaapConfig.BASIC_SCHEME);
                    }
                } else {
                    config.setAuthenticationMethod(DaapConfig.NO_PASSWORD);
                    config.setAuthenticationScheme(DaapConfig.BASIC_SCHEME);
                }
                
                server = new LimeDaapServerNIO(library, config, backgroundExecutor);

                server.setAuthenticator(new LimeAuthenticator());
                server.setStreamSource(new LimeStreamSource());
                server.setFilter(new LimeFilter());
                
                final int maxAttempts = 10;
                
                for(int i = 0; i < maxAttempts; i++) {
                    try {
                        server.bind();
                        break;
                    } catch (BindException bindErr) {
                        if (i < (maxAttempts-1)) {
                            // try next port...
                            config.nextPort();
                        } else {
                            throw bindErr;
                        }
                    }
                }

                server.run();

                bonjour.registerService();

            } catch (IOException err) {
                stop();
                throw err;
            }
        }
        
        if (isServerRunning()) {
            setEnabled(enabled);
        }
    }

    /**
     * Stops the DAAP Server and releases all resources.
     */
    public synchronized void stop() {

        if (bonjour != null)
            bonjour.close();

        if (server != null)
            server.stop();

        if (urnToSong != null)
            urnToSong.clear();

        bonjour = null;
        server = null;
        urnToSong = null;
        library = null;
        whatsNew = null;
        creativecommons = null;
        database = null;
        autoCommitTxn = null;
    }

    /**
     * Restarts the DAAP server and re-registers it via mDNS. This is equivalent
     * to:
     * <p>
     * 
     * <pre>
     * stop();
     * start();
     * init();
     * </pre>
     */
    public synchronized void restart() throws IOException {
        if (isServerRunning())
            stop();

        start();
    }

    /**
     * Shutdown the DAAP service properly. In this case is the main focus on
     * mDNS as in some rare cases iTunes doesn't recognize that LimeWire/DAAP 
     * is no longer online.
     */
    public void doFinalize() {
        stop();
    }

    /**
     * Updates the mDNS service info.
     */
    public synchronized void updateService() throws IOException {

        if (isServerRunning()) {
            bonjour.updateService();

            Transaction txn = library.beginTransaction();
            String name = DaapSettings.DAAP_LIBRARY_NAME.get();
            library.setName(txn, name);
            masterPlaylist.setName(txn, name);
            database.setName(txn, name);
            
            DaapConfig config = server.getConfig();
            if (DaapSettings.DAAP_REQUIRES_PASSWORD.getValue()) {
                if (DaapSettings.DAAP_REQUIRES_USERNAME.getValue()) {
                    config.setAuthenticationMethod(DaapConfig.USERNAME_AND_PASSWORD);
                    config.setAuthenticationScheme(DaapConfig.DIGEST_SCHEME);
                } else {
                    config.setAuthenticationMethod(DaapConfig.PASSWORD);
                    config.setAuthenticationScheme(DaapConfig.BASIC_SCHEME);
                }
            } else {
                config.setAuthenticationMethod(DaapConfig.NO_PASSWORD);
                config.setAuthenticationScheme(DaapConfig.BASIC_SCHEME);
            }
            
            txn.commit();
        }
    }

    /**
     * Disconnects all clients.
     */
    public synchronized void disconnectAll() {
        if (isServerRunning()) {
            server.disconnectAll();
        }
    }

    /**
     * Returns <tt>true</tt> if server is running.
     */
    public synchronized boolean isServerRunning() {
        if (server != null) {
            return server.isRunning();
        }
        return false;
    }

    /**
     * Returns true if the extension of name is a supported file type.
     */
    private static boolean isSupportedAudioFormat(String name) {
        return isSupportedFormat(DaapSettings.DAAP_SUPPORTED_AUDIO_FILE_TYPES.get(), name);
    }
    
    private static boolean isSupportedVideoFormat(String name) {
        return isSupportedFormat(DaapSettings.DAAP_SUPPORTED_VIDEO_FILE_TYPES.get(), name);
    }
    
    private static boolean isSupportedFormat(String[] types, String name) {
        for (String type : types) {
            if (name.endsWith(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles a change event.
     */
    private synchronized void handleChangeEvent(FileViewChangeEvent evt) {
        Song song = urnToSong.remove(evt.getOldValue().getSHA1Urn());
        if (song != null) {
            urnToSong.put(evt.getFileDesc().getSHA1Urn(), song);
            
            // See if the URNs changed -- if so, we can just rename.
            boolean rename = evt.getOldValue().getSHA1Urn().equals(evt.getFileDesc().getSHA1Urn());
            if(rename) {
                song.setAttachment(evt.getFileDesc());
            } else {                
                String name = evt.getFileDesc().getFileName().toLowerCase(Locale.US);
                
                if (isSupportedAudioFormat(name)) {
                    updateSongAudioMeta(autoCommitTxn, song, evt.getFileDesc());
                } else if (isSupportedVideoFormat(name)) {
                    updateSongVideoMeta(autoCommitTxn, song, evt.getFileDesc());
                } else {
                    database.removeSong(autoCommitTxn, song);
                }
                // auto commit
            }
            
        }
    }
    
    /** Handles a change in metadata event. */
    private synchronized void handleMetaChangeEvent(FileViewChangeEvent evt) {
        URN urn = evt.getFileDesc().getSHA1Urn();
        Song song = urnToSong.get(urn);
        if (song != null) {
            String name = evt.getFileDesc().getFileName().toLowerCase(Locale.US);                
            if (isSupportedAudioFormat(name)) {
                updateSongAudioMeta(autoCommitTxn, song, evt.getFileDesc());
            } else if (isSupportedVideoFormat(name)) {
                updateSongVideoMeta(autoCommitTxn, song, evt.getFileDesc());
            } else {
                database.removeSong(autoCommitTxn, song);
            }
        } else {
            handleAddEvent(evt);
        }
    }

    /**
     * Handles an add event.
     */
    private synchronized void handleAddEvent(FileViewChangeEvent evt) {
        // Transactions synchronize on the Library. So if there's
        // an ongoing commit we may get a ConcurrentModificationException
        // because Database has to iterate through all Playlists and
        // count the Songs.
        synchronized (library) {
            if (database.getSongCount() >= maxPlaylistSize) {
                return;
            }
        }
        
        FileDesc fileDesc = evt.getFileDesc();
        if (!(fileDesc instanceof IncompleteFileDesc)) {

            String name = fileDesc.getFileName().toLowerCase(Locale.US);

            Song song = null;
            
            if (isSupportedAudioFormat(name)) {
                song = createSong(fileDesc, true);
            } else if (isSupportedVideoFormat(name)) {
                song = createSong(fileDesc, false);
            }
            
            if (song != null) {
                urnToSong.put(fileDesc.getSHA1Urn(), song);
                
                database.getMasterPlaylist().addSong(autoCommitTxn, song);
                whatsNew.addSong(autoCommitTxn, song);
                
                if (fileDesc.isLicensed()) {
                    creativecommons.addSong(autoCommitTxn, song);
                }

                if (isSupportedVideoFormat(name)) {
                    videos.addSong(autoCommitTxn, song);
                }
                
                // auto commit
            }
        }
    }
    
    /**
     * Handles a remove event.
     */
    private synchronized void handleRemoveEvent(FileViewChangeEvent evt) {
        Song song = urnToSong.remove(evt.getFileDesc().getSHA1Urn());

        if (song != null) {
            database.removeSong(autoCommitTxn, song);
            song.setAttachment(null);
            
            // auto commit
        }
    }
    
    private synchronized void handleClearEvent() {
        for(Song song : urnToSong.values()) {
            if(song != null) {
                database.removeSong(autoCommitTxn, song);
                song.setAttachment(null);
            }
        }
        urnToSong.clear();
    }
    
    public synchronized boolean isEnabled() {
        return enabled;
    }
    
    private synchronized void setEnabled(boolean enabled) {
        
        this.enabled = enabled;
        
        if (!enabled || !isServerRunning())
            return;
        
        Map<URN, Song> tmpUrnToSong = new HashMap<URN, Song>();
        
        int size = masterPlaylist.getSongCount();        
        Transaction txn = library.beginTransaction();    
   
        gnutellaFileView.getReadLock().lock();
        try {
            for(FileDesc fd : gnutellaFileView) {
                String name = fd.getFileName().toLowerCase(Locale.US);
                boolean audio = isSupportedAudioFormat(name);
                
                if(!audio && !isSupportedVideoFormat(name)) {
                    continue;
                }
                
                URN urn = fd.getSHA1Urn();
                
                // 1)
                // _Remove_ URN from the current 'map'...
                Song song = urnToSong.remove(urn);
                    
                // Check if URN is already in the tmpMap.
                // If so do nothing as we don't want add 
                // the same file multiple times...
                if(tmpUrnToSong.containsKey(urn)) {
                    continue;
                }
                
                // This URN was already mapped with a Song.
                // Save the Song (again) and update the meta
                // data if necessary
                if (song != null) {
                    tmpUrnToSong.put(urn, song);
                    
                    if (audio) {
                        updateSongAudioMeta(txn, song, fd);
                    } else {
                        updateSongVideoMeta(txn, song, fd);
                    }
                    
                } else if (size < maxPlaylistSize) {
    
                    song = createSong(fd, audio);
                    tmpUrnToSong.put(urn, song);
                    database.getMasterPlaylist().addSong(txn, song);
                    
                    if (fd.isLicensed()) {
                        creativecommons.addSong(txn, song);
                    }
                    
                    if (isSupportedVideoFormat(name)) {
                        videos.addSong(txn, song);
                    }
                    
                    size++;
                }
            }
        } finally {
            gnutellaFileView.getReadLock().unlock();
        }
        
        // See 1)
        // As all known URNs were removed from 'map' only
        // deleted FileDesc URNs can be leftover! We must 
        // remove the associated Songs from the Library now
        for(Song song : urnToSong.values()) {
            database.removeSong(txn, song);
            song.setAttachment(null);
        }
        
        urnToSong.clear();
        urnToSong = tmpUrnToSong; // tempMap is the new 'map'

        txn.commit();
    }
    
    /**
     * Create a Song and sets its meta data with
     * the data which is retrieved from the FileDesc.
     */
    private Song createSong(FileDesc desc, boolean audio) {
        
        Song song = new Song(desc.getFileName());
        
        song.setSize(null, desc.getFileSize() & 0xFFFFFFFFL);
        song.setDateAdded(null, System.currentTimeMillis()/1000L);
        
        File file = desc.getFile();
        String ext = FileUtils.getFileExtension(file);
        
        if (!audio) {
            song.setHasVideo(null, true);
        }
        
        if (!ext.isEmpty()) {
            // Note: This is required for formats other than MP3
            // For example AAC (.m4a) files won't play if no
            // format is set. As far as I can tell from the iTunes
            // 'Get Info' dialog are Songs assumed as MP3 until
            // a format is set explicit.
            ext = ext.toLowerCase(Locale.US);
            if (!ext.endsWith("mp3"))
                song.setFormat(null, ext);

            if (audio) {
                updateSongAudioMeta(null, song, desc);
            } else {
                updateSongVideoMeta(null, song, desc);
            }
            
        } else {
            song.setAttachment(desc);
        }

        return song;
    }
    
    private boolean updateSongVideoMeta(Transaction txn, Song song, FileDesc desc) {
        
        song.setAttachment(desc);
        LimeXMLDocument doc = desc.getXMLDocument(LimeXMLNames.VIDEO_SCHEMA);
        
        if (doc == null) {
            return false;
        }
        
        boolean update = false;
        
        String title = doc.getValue(LimeXMLNames.VIDEO_TITLE);
        //String type = doc.getValue(LimeXMLNames.VIDEO_TYPE);
        String year = doc.getValue(LimeXMLNames.VIDEO_YEAR);
        String rating = doc.getValue(LimeXMLNames.VIDEO_RATING);
        String length = doc.getValue(LimeXMLNames.VIDEO_LENGTH);
        //String comments = doc.getValue(LimeXMLNames.VIDEO_COMMENTS);
        //String licensetype = doc.getValue(LimeXMLNames.VIDEO_LICENSETYPE);
        String license = doc.getValue(LimeXMLNames.VIDEO_LICENSE);
        //String height = doc.getValue(LimeXMLNames.VIDEO_HEIGHT);
        //String width = doc.getValue(LimeXMLNames.VIDEO_WIDTH);
        String bitrate = doc.getValue(LimeXMLNames.VIDEO_BITRATE);
        //String action = doc.getValue(LimeXMLNames.VIDEO_ACTION);
        String director = doc.getValue(LimeXMLNames.VIDEO_DIRECTOR);
        //String studio = doc.getValue(LimeXMLNames.VIDEO_STUDIO);
        //String language = doc.getValue(LimeXMLNames.VIDEO_LANGUAGE);
        //String stars = doc.getValue(LimeXMLNames.VIDEO_STARS);
        //String producer = doc.getValue(LimeXMLNames.VIDEO_PRODUCE);
        //String subtitles = doc.getValue(LimeXMLNames.VIDEO_SUBTITLES);
        
        if (title != null) {
            String currentTitle = song.getName();
            if (currentTitle == null || !title.equals(currentTitle)) {
                update = true;
                song.setName(txn, title);
            }
        }
        
        int currentBitrate = song.getBitrate();
        if (bitrate != null) {
            try {
                int num = Integer.parseInt(bitrate);
                if (num > 0 && num != currentBitrate) {
                    update = true;
                    song.setBitrate(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentBitrate != 0) {
            update = true;
            song.setBitrate(txn, 0);
        }
        
        long currentLength = song.getTime();
        if (length != null) {
            try {
                // iTunes expects the song length in milliseconds
                int num = (int)(Integer.parseInt(length)*1000L);
                if (num > 0 && num != currentLength) {
                    update = true;
                    song.setTime(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentLength != 0) {
            update = true;
            song.setTime(txn, 0);
        }
        
        int currentYear = song.getYear();
        if (year != null) {
            try {
                int num = Integer.parseInt(year);
                if (num > 0 && num != currentYear) {
                    update = true;
                    song.setYear(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentYear != 0) {
            update = true;
            song.setYear(txn, 0);
        }
        
        // Genre = License
        String currentGenre = song.getGenre();
        if (license != null) {
            if (currentGenre == null || !license.equals(currentGenre)) {
                update = true;
                song.setGenre(txn, license);
            }
        } else if (currentGenre != null) {
            update = true;
            song.setGenre(txn, null);
        }
        
        // Artist = Director
        String currentArtist = song.getArtist();
        if (director != null) {
            if (currentArtist == null || !director.equals(currentArtist)) {
                update = true;
                song.setArtist(txn, director);
            }
        } else if (currentArtist != null) {
            update = true;
            song.setArtist(txn, null);
        }
        
        // Rating = Album
        String currentAlbum = song.getAlbum();
        if (rating != null) {
            if (currentAlbum == null || !rating.equals(currentAlbum)) {
                update = true;
                song.setAlbum(txn, rating);
            }
        } else if (currentAlbum != null) {
            update = true;
            song.setAlbum(txn, null);
        }
        
        return update;
    }
    
    /**
     * Sets the audio meta data.
     */
    private boolean updateSongAudioMeta(Transaction txn, Song song, FileDesc desc) {
        
        song.setAttachment(desc);
        
        LimeXMLDocument doc = desc.getXMLDocument(LimeXMLNames.AUDIO_SCHEMA);
        
        if (doc == null)
            return false;
        
        boolean update = false;
        
        String title = doc.getValue(LimeXMLNames.AUDIO_TITLE);
        String track = doc.getValue(LimeXMLNames.AUDIO_TRACK);
        String artist = doc.getValue(LimeXMLNames.AUDIO_ARTIST);
        String album = doc.getValue(LimeXMLNames.AUDIO_ALBUM);
        String genre = doc.getValue(LimeXMLNames.AUDIO_GENRE);
        String bitrate = doc.getValue(LimeXMLNames.AUDIO_BITRATE);
        //String comments = doc.getValue(LimeXMLNames.AUDIO_COMMENTS);
        String time = doc.getValue(LimeXMLNames.AUDIO_SECONDS);
        String year = doc.getValue(LimeXMLNames.AUDIO_YEAR);
        
        if (title != null) {
            String currentTitle = song.getName();
            if (currentTitle == null || !title.equals(currentTitle)) {
                update = true;
                song.setName(txn, title);
            }
        }
        
        int currentTrack = song.getTrackNumber();
        if (track != null) {
            try {
                int num = Integer.parseInt(track);
                if (num > 0 && num != currentTrack) {
                    update = true;
                    song.setTrackNumber(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentTrack != 0) {
            update = true;
            song.setTrackNumber(txn, 0);
        }
        
        String currentArtist = song.getArtist();
        if (artist != null) {
            if (currentArtist == null || !artist.equals(currentArtist)) {
                update = true;
                song.setArtist(txn, artist);
            }
        } else if (currentArtist != null) {
            update = true;
            song.setArtist(txn, null);
        }
        
        String currentAlbum = song.getAlbum();
        if (album != null) {
            if (currentAlbum == null || !album.equals(currentAlbum)) {
                update = true;
                song.setAlbum(txn, album);
            }
        } else if (currentAlbum != null) {
            update = true;
            song.setAlbum(txn, null);
        }
        
        String currentGenre = song.getGenre();
        if (genre != null) {
            if (currentGenre == null || !genre.equals(currentGenre)) {
                update = true;
                song.setGenre(txn, genre);
            }
        } else if (currentGenre != null) {
            update = true;
            song.setGenre(txn, null);
        }
        
        /*String currentComments = song.getComment();
        if (comments != null) {
            if (currentComments == null || !comments.equals(currentComments)) {
                update = true;
                song.setComment(txn, comments);
            }
        } else if (currentComments != null) {
            update = true;
            song.setComment(txn, null);
        }*/
        
        int currentBitrate = song.getBitrate();
        if (bitrate != null) {
            try {
                int num = Integer.parseInt(bitrate);
                if (num > 0 && num != currentBitrate) {
                    update = true;
                    song.setBitrate(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentBitrate != 0) {
            update = true;
            song.setBitrate(txn, 0);
        }
        
        long currentTime = song.getTime();
        if (time != null) {
            try {
                // iTunes expects the song length in milliseconds
                long num = Integer.parseInt(time)*1000l;
                if (num > 0 && num != currentTime) {
                    update = true;
                    song.setTime(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentTime != 0) {
            update = true;
            song.setTime(txn, 0);
        }
        
        int currentYear = song.getYear();
        if (year != null) {
            try {
                int num = Integer.parseInt(year);
                if (num > 0 && num != currentYear) {
                    update = true;
                    song.setYear(txn, num);
                }
            } catch (NumberFormatException err) {}
        } else if (currentYear != 0) {
            update = true;
            song.setYear(txn, 0);
        }
        
        // iTunes expects the date/time in seconds
        int mod = (int)(desc.lastModified()/1000);
        if (song.getDateModified() != mod) {
            update = true;
            song.setDateModified(txn, mod);
        }

        return update;
    }
    
    /**
     * Handles the audio stream.
     */
    private final static class LimeStreamSource implements DaapStreamSource {
        
        public Object getSource(Song song) throws IOException {
            FileDesc fileDesc = (FileDesc)song.getAttachment();

            if(fileDesc != null)
                return new FileInputStream(fileDesc.getFile());
            
            return null;
        }
    }
    
    /**
     * Implements the DaapAuthenticator.
     */
    private final static class LimeAuthenticator implements DaapAuthenticator {
        
        public boolean authenticate(String username, String password, String uri, String nonce) {
            
            if (uri == null && nonce == null) {
                // BASIC
                return DaapSettings.DAAP_PASSWORD.equals(password);
            } else if (uri != null && nonce != null) {
                // DIGEST
                String ha1 = DaapSettings.DAAP_PASSWORD.get();
                if (ha1.startsWith("MD5/")) {
                    ha1 = ha1.substring(4);
                }
                String ha2 = DaapUtil.calculateHA2(uri);
                String digest = DaapUtil.digest(ha1, ha2, nonce);
                return digest.equalsIgnoreCase(password);
            } else {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Unknown scheme!");
                }
            }
            
            return false;
        }
    }
    
    /**
     * The DAAP Library should be only accessible from the LAN
     * as we can not guarantee for the required bandwidth and it
     * could be used to bypass Gnutella etc. Note: iTunes can't
     * connect to DAAP Libraries outside of the LAN but certain
     * iTunes download tools can.
     */
    private final class LimeFilter implements DaapFilter {

        public boolean accept(InetAddress address) {
            
            try {
                return (networkInstanceUtils.get().isVeryCloseIP(address)
                        || networkInstanceUtils.get().isPrivateAddress(address))
                        && ipFilter.get().allow(address.getAddress());
            } catch (IllegalArgumentException err) {
                LOG.error(err);
                return false;
            }
        }
    }

    /**
     * A LimeWire specific implementation of DaapConfig.
     */
    private final static class LimeConfig extends DaapConfig {

        private InetAddress addr;

        public LimeConfig(InetAddress addr) {
            this.addr = addr;

            // Reset PORT to default value to prevent increasing
            // it to infinity
            DaapSettings.DAAP_PORT.revertToDefault();
        }

        @Override
        public String getServerName() {
            return LimeWireUtils.getHttpServer();
        }

        public void nextPort() {
            int port = DaapSettings.DAAP_PORT.getValue();
            DaapSettings.DAAP_PORT.setValue(port + 1);
        }

        @Override
        public int getBacklog() {
            return 0;
        }

        @Override
        public InetSocketAddress getInetSocketAddress() {
            int port = DaapSettings.DAAP_PORT.getValue();
            return new InetSocketAddress(addr, port);
        }

        @Override
        public int getMaxConnections() {
            return DaapSettings.DAAP_MAX_CONNECTIONS.getValue();
        }
    }

    /**
     * Helps us to publicize and update the DAAP Service via mDNS.
     */
    private final static class BonjourService {

        private static final String VERSION = "Version";

        private static final String MACHINE_NAME = "Machine Name";

        private static final String PASSWORD = "Password";

        private final JmDNS zeroConf;

        private ServiceInfo serviceInfo;

        public BonjourService(InetAddress addr) throws IOException {
            zeroConf = new JmDNS(addr);
        }

        public boolean isRegistered() {
            return (serviceInfo != null);
        }

        private ServiceInfo createServiceInfo() {

            String type = DaapSettings.DAAP_TYPE_NAME.get();
            String name = DaapSettings.DAAP_SERVICE_NAME.get();

            int port = DaapSettings.DAAP_PORT.getValue();
            int weight = DaapSettings.DAAP_WEIGHT.getValue();
            int priority = DaapSettings.DAAP_PRIORITY.getValue();

            boolean password = DaapSettings.DAAP_REQUIRES_PASSWORD.getValue();

            Hashtable<String, String> props = new Hashtable<String, String>();

            // Greys the share and the playlist names when iTunes's
            // protocol version is different from this version. It's
            // only a nice visual effect and has no impact to the
            // ability to connect this server! Disabled because
            // iTunes 4.2 is still widespread...
            props.put(VERSION, Integer.toString(DaapUtil.DAAP_VERSION_3));

            // This is the initial share name
            props.put(MACHINE_NAME, name);

            // shows the small lock if Service is protected
            // by a password!
            props.put(PASSWORD, Boolean.toString(password));

            String qualifiedName = null;

            // This isn't really required but as iTunes
            // does it in this way I'm doing it too...
            if (password) {
                qualifiedName = name + "_PW." + type;
            } else {
                qualifiedName = name + "." + type;
            }

            return new ServiceInfo(type, qualifiedName, port,
                    weight, priority, props);
        }

        public void registerService() throws IOException {

            if (isRegistered())
                throw new IOException();

            ServiceInfo serviceInfo = createServiceInfo();
            zeroConf.registerService(serviceInfo);
            this.serviceInfo = serviceInfo;
        }

        public void unregisterService() {
            if (!isRegistered())
                return;

            zeroConf.unregisterService(serviceInfo);
            serviceInfo = null;
        }

        public void updateService() throws IOException {
            if (!isRegistered())
                throw new IOException();

            if (serviceInfo.getPort() != DaapSettings.DAAP_PORT.getValue())
                unregisterService();

            ServiceInfo serviceInfo = createServiceInfo();
            zeroConf.registerService(serviceInfo);

            this.serviceInfo = serviceInfo;
        }

        public void close() {
            unregisterService();
            zeroConf.close();
        }   
    }

    /**
     * Listens for events from FileManager.
     */
    private void handleManagedListStatusEvent(final LibraryStatusEvent evt) {
        
        DAAP_EVENT_QUEUE.execute(new Runnable(){
            public void run(){
                switch(evt.getType()) {
                    case LOAD_COMPLETE:
                        setEnabled(true);
                }
            }
        });
    }
    
    private void handleFileListEvent(final FileViewChangeEvent evt) {
        // if Daap isn't enabled ignore events
        if (!DaapSettings.DAAP_ENABLED.getValue())
            return;

        DAAP_EVENT_QUEUE.execute(new Runnable() {
            public void run() {

                if (!isEnabled() || !isServerRunning())
                    return;

                switch (evt.getType()) {
                case FILE_CHANGED:
                    handleChangeEvent(evt);
                    break;
                case FILE_ADDED:
                    handleAddEvent(evt);
                    break;
                case FILE_REMOVED:
                    handleRemoveEvent(evt);
                    break;
                case FILES_CLEARED:
                    handleClearEvent();
                    break;
                case FILE_META_CHANGED:
                    handleMetaChangeEvent(evt);
                    break;
                }
            }
        });
    }
}
