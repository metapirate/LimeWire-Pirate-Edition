package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.bittorrent.LimeWireTorrentProperties;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.inject.LazySingleton;
import org.limewire.libtorrent.LibTorrentParams;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.URIUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ActivityCallback;

/**
 * This class is responsible for saving a torrent as an upload memento and
 * loading them at startup.
 */
@LazySingleton
public class TorrentUploadManager implements BTUploaderFactory {

    private static final Log LOG = LogFactory.getLog(TorrentUploadManager.class);
    private final Provider<ActivityCallback> activityCallback;
    private final Provider<LimeWireTorrentManager> torrentManager;

    @Inject
    public TorrentUploadManager(Provider<LimeWireTorrentManager> torrentManager, Provider<ActivityCallback> activityCallback) {
        this.torrentManager = torrentManager;
        this.activityCallback = activityCallback;
    }

    /**
     * Iterates through the uploads folder finding saved torrent mementos and
     * starting off the uploads.
     */
    @SuppressWarnings("unchecked")
    public void loadSavedUploads() {
        File uploadsDirectory = BittorrentSettings.TORRENT_UPLOADS_FOLDER.get();
        if (uploadsDirectory.exists()) {
            File[] uploadMementos = uploadsDirectory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return "memento".equals(FileUtils.getFileExtension(file));
                }
            });

            if (uploadMementos != null) {
                for (File mementoFile : uploadMementos) {
                    Map<String, Object> memento = null;
                    try {
                        memento = readMemento(mementoFile);
                    } catch (IllegalArgumentException e) {
                        LOG.error("Error reading memento for: " + mementoFile, e);
                    } catch (IOException e) {
                        LOG.error("Error reading memento for: " + mementoFile, e);
                    } catch (ClassNotFoundException e) {
                        LOG.error("Error reading memento for: " + mementoFile, e);
                    }
                    if (memento != null) {

                        long mementoModifiedTime = mementoFile.lastModified();

                        File torrentFile = (File) memento.get("torrentFile");
                        File fastResumeFile = (File) memento.get("fastResumeFile");
                        File torrentDataFile = (File) memento.get("torrentDataFile");
                        String sha1 = (String) memento.get("sha1");
                        String name = (String) memento.get("name");
                        Float seedRatioLimit = (Float) memento.get("seedRatioLimit");
                        Integer timeRatioLimit = (Integer) memento.get("timeRatioLimit");
                        
                        List<URI> trackers = (List<URI>) memento.get("trackers"); 
                        if (trackers == null) { 
                            String firstTracker = (String) memento.get("trackerURL");
                            try {
                                trackers = Arrays.asList(URIUtils.toURI(firstTracker));
                            } catch (URISyntaxException e) {
                                // No valid trackers found in memento
                            }
                        }

                        boolean torrentAdded = false;
                        boolean torrentLoaded = false;
                        Torrent torrent = null;

                        if (torrentDataFile.exists() && fastResumeFile != null
                                && fastResumeFile.exists()) {
                            if (torrentManager.get().isValid()
                                    && !torrentManager.get().isDownloadingTorrent(mementoFile)) {
                                try {
                                    TorrentParams params = new LibTorrentParams(torrentDataFile
                                            .getParentFile(), name, sha1);
                                    params.setTrackers(trackers);
                                    params.setFastResumeFile(fastResumeFile);
                                    params.setTorrentFile(torrentFile);
                                    params.setTorrentDataFile(torrentDataFile);
                                    if(seedRatioLimit != null)
                                        params.setSeedRatioLimit(seedRatioLimit);
                                    if(timeRatioLimit != null)
                                        params.setTimeRatioLimit(timeRatioLimit);
                                    torrent = torrentManager.get().seedTorrent(params); 
                                    if (torrent != null) {
                                        torrentAdded = true;
                                        if (torrent.hasMetaData()) {
                                            TorrentInfo torrentInfo = torrent.getTorrentInfo();
                                            boolean filesOk = true;
                                            for (TorrentFileEntry entry : torrentInfo
                                                    .getTorrentFileEntries()) {
                                                int priority = entry.getPriority();
                                                if (priority > 0) {
                                                    File torrentFileEntry = torrent
                                                            .getTorrentDataFile(entry);
                                                    boolean exists = torrentFileEntry.exists();
                                                    long fileModifiedTime = torrentFileEntry
                                                            .lastModified();
                                                    if (!exists
                                                            || fileModifiedTime > mementoModifiedTime) {
                                                        filesOk = false;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (filesOk) {
                                                createBTUploader(torrent);
                                                torrent.setAutoManaged(true);
                                                torrent.start();
                                                torrentLoaded = true;
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    LOG.error("Error initializing memento from: " + mementoFile, e);
                                }
                            }
                        }

                        if (!torrentLoaded) {
                            cleanup(mementoFile, torrentFile, fastResumeFile);
                            if (torrent != null && torrentAdded) {
                                torrentManager.get().removeTorrent(torrent);
                            }
                        }
                    }
                }
            }
        }
    }

    private void cleanup(File mementoFile, File torrentFile, File fastResumeFile) {
        if (torrentFile != null) {
            FileUtils.delete(torrentFile, false);
        }
        if (fastResumeFile != null) {
            FileUtils.delete(fastResumeFile, false);
        }
        if (mementoFile != null) {
            FileUtils.delete(mementoFile, false);
        }
    }

    private Map<String, Object> readMemento(File mementoFile) throws IOException,
            ClassNotFoundException, IllegalArgumentException {
        Object mementoObject = FileUtils.readObject(mementoFile);
        Map<String, Object> memento = GenericsUtils.scanForMap(mementoObject, String.class,
                Object.class, ScanMode.EXCEPTION);
        return memento;
    }

    /**
     * Creates an upload memento from the Torrent and writes it to disk.
     */
    public void writeMemento(Torrent torrent) throws IOException {
        File torrentMomento = getMementoFile(torrent);
        torrentMomento.getParentFile().mkdirs();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("torrentDataFile", torrent.getTorrentDataFile().getAbsoluteFile());
        File torrentFile = torrent.getTorrentFile();
        if(torrentFile != null)
            map.put("torrentFile", torrentFile.getAbsoluteFile());
        map.put("fastResumeFile", torrent.getFastResumeFile().getAbsoluteFile());
        map.put("sha1", torrent.getSha1());
        map.put("trackers", torrent.getTrackerURIS());
        map.put("name", torrent.getName());
        
        float seedRatioLimit = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_RATIO_LIMIT, -1f);
        if(seedRatioLimit >= 0)
            map.put("seedRatioLimit", seedRatioLimit);
        
        int seedTimeRatioLimit = torrent.getProperty(LimeWireTorrentProperties.MAX_SEED_TIME_RATIO_LIMIT, -1);
        if(seedTimeRatioLimit >= 0)
            map.put("timeRatioLimit", seedTimeRatioLimit);

        FileUtils.writeObject(torrentMomento, map);
    }

    private File getMementoFile(Torrent torrent) {
        File torrentMomento = new File(BittorrentSettings.TORRENT_UPLOADS_FOLDER.get(), torrent
                .getName()
                + ".memento");
        return torrentMomento;
    }

    /**
     * Removes any found upload mementos/artifacts for the given torrent from
     * disk.
     */
    public void removeMemento(Torrent torrent) {
        File torrentMomento = getMementoFile(torrent);
        FileUtils.forceDelete(torrentMomento);
        if(torrent.getTorrentFile() != null && torrent.getTorrentFile().getParentFile().equals(BittorrentSettings.TORRENT_UPLOADS_FOLDER.get())) {
            FileUtils.forceDelete(torrent.getTorrentFile());
        }
        if(torrent.getFastResumeFile().getParentFile().equals(BittorrentSettings.TORRENT_UPLOADS_FOLDER.get())) {
            FileUtils.forceDelete(torrent.getFastResumeFile());
        }
    }

    @Override
    public BTUploader createBTUploader(Torrent torrent) {
        BTUploader btUploader = new BTUploader(torrent, activityCallback.get(), this,
                torrentManager.get());
        btUploader.registerTorrentListener();
        activityCallback.get().addUpload(btUploader);
        return btUploader;
    }
}
