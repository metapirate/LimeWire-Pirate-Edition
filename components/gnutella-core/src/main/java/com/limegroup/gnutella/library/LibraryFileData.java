package com.limegroup.gnutella.library;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.AbstractSettingsGroup;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

// Provided like a singleton by LimeWireLibraryModule.lfd()
class LibraryFileData extends AbstractSettingsGroup {
    
    private static final Log LOG = LogFactory.getLog(LibraryFileData.class);
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final CategoryManager categoryManager;
    
    private static enum Version {
        // for prior versions [before 5.0], see OldLibraryData & LibraryConverter
        ONE, // the first ever version [active 5.0 -> 5.1]
        TWO, // [active 5.2]
        THREE; // the current version [active 5.3]
    }
    
    private static final String CURRENT_VERSION_KEY = "CURRENT_VERSION";
//    private static final String USER_EXTENSIONS_KEY = "USER_EXTENSIONS";
//    private static final String USER_REMOVED_KEY = "USER_REMOVED";
//    private static final String MANAGED_DIRECTORIES_KEY = "MANAGED_DIRECTORIES";
//    private static final String DO_NOT_MANAGE_KEY = "DO_NOT_MANAGE";
//    private static final String EXCLUDE_FILES_KEY = "EXCLUDE_FILES";
    private static final String SHARE_DATA_KEY = "SHARE_DATA";
    private static final String FILE_DATA_KEY = "FILE_DATA";
    private static final String COLLECTION_NAME_KEY = "COLLECTION_NAMES";
    private static final String COLLECTION_SHARE_DATA_KEY = "COLLECTION_SHARE_DATA";
    private static final String SAFE_URNS = "SAFE_URNS";
    
    static final Integer DEFAULT_SHARED_COLLECTION_ID = 0;
    private static final Integer MIN_COLLECTION_ID = 1;
    
    private final Version CURRENT_VERSION = Version.THREE;
    
    private final Map<String, List<Integer>> fileData = new HashMap<String, List<Integer>>();
    private final SortedMap<Integer, String> collectionNames = new TreeMap<Integer, String>();
    private final Map<Integer, List<String>> collectionShareData = new HashMap<Integer, List<String>>();
    private final Set<String> safeUrns = new HashSet<String>();
    private volatile boolean dirty = false;
    
    private final File saveFile = new File(CommonUtils.getUserSettingsDir(), "library5.dat"); 
    private final File backupFile = new File(CommonUtils.getUserSettingsDir(), "library5.bak");
    
    private volatile boolean loaded = false;   

    LibraryFileData(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
        SettingsGroupManager.instance().addSettingsGroup(this);
    }
    
    private int originalNumPublicSharedFiles = -1;
    
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void reload() {
        load();
    }
    
    @Override
    public boolean revertToDefault() {
        clear();
        return true;
    }
    
    private void clear() {
        lock.writeLock().lock();
        try {
            dirty = true;
            fileData.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean save() {
        if(!loaded || !dirty) {
            return false;
        }
        
        Map<String, Object> save = new HashMap<String, Object>();
        lock.readLock().lock();
        try {
            save.put(CURRENT_VERSION_KEY, CURRENT_VERSION);
            save.put(FILE_DATA_KEY, fileData);
            save.put(COLLECTION_NAME_KEY, collectionNames);
            save.put(COLLECTION_SHARE_DATA_KEY, collectionShareData);
            save.put(SAFE_URNS, safeUrns);
            if(FileUtils.writeWithBackupFile(save, backupFile, saveFile, LOG)) {
                dirty = false;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return true;
    }
    
    void load() {
        boolean failed = false;
        if(!loadFromFile(saveFile)) {
            failed = !loadFromFile(backupFile);
        }
        dirty = failed;
        
        // Save initial public share list size for inspection stats
        if (fileData.size() > 0) {
            originalNumPublicSharedFiles = peekPublicSharedListCount();
        } else {
            originalNumPublicSharedFiles = 0;
        }
        
        loaded = true;
    }
    
    private boolean loadFromFile(File file) {
        Map<String, Object> readMap = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object read = in.readObject();
            readMap = GenericsUtils.scanForMap(read, String.class, Object.class, ScanMode.REMOVE);
            if (readMap != null) {
                Object currentVersion = readMap.get("CURRENT_VERSION");
                if(currentVersion == null) {
                    currentVersion = Version.ONE;
                }
                
                if(currentVersion instanceof Version) {
                    initializeFromVersion(((Version)currentVersion), readMap);
                    return true;
                } else {
                    return false;
                }
            }
        } catch(Throwable throwable) {
            LOG.error("Error loading library", throwable);
        }
        
        return false;
    }

    /**
     * Initializes the read map assuming it's a particular version.
     */
    private void initializeFromVersion(Version version, Map<String, Object> readMap) {
        Map<String, List<Integer>> fileData;
        Map<Integer, String> collectionNames;
        Map<Integer, List<String>> collectionShareData;
        Set<String> safeUrns;
        
        switch(version) {
        case ONE:
            Map<File, FileProperties> oldShareData = GenericsUtils.scanForMap(readMap.get(SHARE_DATA_KEY), File.class, FileProperties.class, ScanMode.REMOVE);
            fileData = new HashMap<String, List<Integer>>();
            collectionNames = new HashMap<Integer, String>();
            collectionShareData = new HashMap<Integer, List<String>>();
            
            convertShareData(oldShareData, fileData, collectionNames, collectionShareData);
            
            final Map<String, List<Integer>> fileDataFinal = fileData;
            LibraryConverterHelper helper = new LibraryConverterHelper(new LibraryConverterHelper.FileAdder() {
               @Override
                public void addFile(File file) {
                   if(!fileDataFinal.containsKey(createKey(file))) {
                       fileDataFinal.put(createKey(file), Collections.<Integer>emptyList());
                   }
                }
            }, categoryManager);
            
            //add save directories to library
            Set<File> convertedDirectories = new HashSet<File>();
            List<File> emptyList = Collections.emptyList();
            helper.convertSaveDirectories(emptyList, emptyList, convertedDirectories);
            
            safeUrns = new HashSet<String>();
            break;
        case TWO:
            fileData = new HashMap<String, List<Integer>>();
            Map<File, List<Integer>> oldFileData = GenericsUtils.scanForMapOfList(readMap.get(FILE_DATA_KEY), File.class, List.class, Integer.class, ScanMode.REMOVE);
            convertShareData(oldFileData, fileData);
            
            collectionNames = GenericsUtils.scanForMap(readMap.get(COLLECTION_NAME_KEY), Integer.class, String.class, ScanMode.REMOVE);
            collectionShareData = GenericsUtils.scanForMapOfList(readMap.get(COLLECTION_SHARE_DATA_KEY), Integer.class, List.class, String.class, ScanMode.REMOVE);
            safeUrns = GenericsUtils.scanForSet(readMap.get(SAFE_URNS), String.class, ScanMode.REMOVE);
            break;
        case THREE:
            fileData = GenericsUtils.scanForMapOfList(readMap.get(FILE_DATA_KEY), String.class, List.class, Integer.class, ScanMode.REMOVE);
            
            collectionNames = GenericsUtils.scanForMap(readMap.get(COLLECTION_NAME_KEY), Integer.class, String.class, ScanMode.REMOVE);
            collectionShareData = GenericsUtils.scanForMapOfList(readMap.get(COLLECTION_SHARE_DATA_KEY), Integer.class, List.class, String.class, ScanMode.REMOVE);
            safeUrns = GenericsUtils.scanForSet(readMap.get(SAFE_URNS), String.class, ScanMode.REMOVE);
            
            break;
            
        default:
            throw new IllegalStateException("Invalid version: " + version);
        }
        
        fileData = internKeys(fileData);
        safeUrns = internSafeUrns(safeUrns);
    
        validateCollectionData(fileData, collectionNames, collectionShareData);
                
        
        lock.writeLock().lock();
        try {
            clear();
            this.fileData.putAll(fileData);
            this.collectionNames.putAll(collectionNames);
            this.collectionShareData.putAll(collectionShareData);
            this.safeUrns.addAll(safeUrns);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static Map<String,List<Integer>> internKeys(Map<String, List<Integer>> oldFileData) {
        Map<String,List<Integer>> newFileData = new HashMap<String, List<Integer>>();
        for ( Map.Entry<String, List<Integer>> entry : oldFileData.entrySet() ) {
            newFileData.put(entry.getKey().intern(), entry.getValue());
        }
        return newFileData;
    }
    
    private static Set<String> internSafeUrns(Set<String> oldSafeUrns) {
        Set<String> newSafeUrns = new HashSet<String>();
        for ( String entry : oldSafeUrns ) {
            newSafeUrns.add(entry.intern());
        }
        return newSafeUrns;
    }
    
    private void validateCollectionData(Map<String, List<Integer>> fileData, Map<Integer, String> collectionNames, Map<Integer, List<String>> collectionShareData) {
        // TODO: Do some validation
    }

    /** Converts 5.0 & 5.1 style share data into 5.2-style collections. */
    private void convertShareData(Map<File, FileProperties> oldShareData, Map<String, List<Integer>> fileData, Map<Integer, String> collectionNames, Map<Integer, List<String>> collectionShareData) {
        int currentId = MIN_COLLECTION_ID;
        Map<String, Integer> friendToCollectionMap = new HashMap<String, Integer>();
        for(Map.Entry<File, FileProperties> data : oldShareData.entrySet()) {
            File file = data.getKey();
            FileProperties shareData = data.getValue();
            if (shareData == null
                    || ((shareData.friends == null || shareData.friends.isEmpty()) && !shareData.gnutella)) {
                fileData.put(createKey(file), Collections.<Integer> emptyList());
            } else {
                if (shareData.friends != null) {
                    for (String friend : shareData.friends) {
                        Integer collectionId = friendToCollectionMap.get(friend);
                        if (collectionId == null) {
                            collectionId = currentId;
                            friendToCollectionMap.put(friend, collectionId);
                            collectionNames.put(collectionId, friend);
                            List<String> shareList = new ArrayList<String>(1);
                            shareList.add(friend);
                            collectionShareData.put(collectionId, shareList);

                            currentId++;
                        }

                        List<Integer> collections = fileData.get(createKey(file));
                        if (collections == null || collections == Collections.<Integer> emptyList()) {
                            collections = new ArrayList<Integer>(1);
                            fileData.put(createKey(file), collections);
                        }
                        collections.add(collectionId);
                    }
                }

                if (shareData.gnutella) {
                    List<Integer> collections = fileData.get(createKey(file));
                    if (collections == null || collections == Collections.<Integer> emptyList()) {
                        collections = new ArrayList<Integer>(1);
                        fileData.put(createKey(file), collections);
                    }
                    collections.add(DEFAULT_SHARED_COLLECTION_ID);
                }
            }
        }
    }

    /** Converts 5.0 & 5.1 style share data into 5.2-style collections. */
    private void convertShareData(Map<File, List<Integer>> oldFileData, Map<String, List<Integer>> fileData) {
        for(Map.Entry<File, List<Integer>> data : oldFileData.entrySet()) {
           fileData.put(createKey(data.getKey()), data.getValue());            
        }
    }

    private static String createKey(File file) {
        return file.getPath().intern();
    }    
    
    /** Returns true if this URN was marked as safe. */
    boolean isFileSafe(String urn) {
        lock.readLock().lock();
        try {
            return safeUrns.contains(urn);
        } finally {
            lock.readLock().unlock();
        }
    }

	/** Caches the URN as being safe or not. */
    void setFileSafe(String urn, boolean safe) {
        lock.writeLock().lock();
        try {
            if(!safe) {
                if(safeUrns.remove(urn)) {
                    dirty = true;
                }
            } else {
                if(safeUrns.add(urn)) {
                    dirty = true;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Clears all file data. */
    void clearFileData() {
        lock.writeLock().lock();
        try {
            if(!fileData.isEmpty()) {
                fileData.clear();
                dirty = true;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Adds a managed file.
     */
    void addManagedFile(File file) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            
            String key = createKey(file);
            if(!fileData.containsKey(key)) {
                fileData.put(key, Collections.<Integer>emptyList());
                changed = true;
            } 
            dirty |= changed;            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Adds a managed file.
     */
    void addOrRenameManagedFile(File file, File originalFile) {
        if (originalFile == null) {
            addManagedFile(file);
        } else {
            lock.writeLock().lock();
            try {
                boolean changed = false;
                String key = createKey(file);
                if(!fileData.containsKey(key)) {
                    String originalKey = createKey(originalFile);
                    if (fileData.containsKey(originalKey)) {
                        fileData.put(key, fileData.get(originalKey));
                        fileData.remove(originalKey);
                    } else {
                        fileData.put(key, Collections.<Integer>emptyList());
                    }
                    changed = true;
                }
                dirty |= changed;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Removes a file from being managed.
     */
    void removeManagedFile(File file) {
        lock.writeLock().lock();
        try {
            boolean changed = fileData.remove(createKey(file)) != null;
            dirty |= changed;            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns a list of all files that should be managed. */
    Iterable<File> getManagedFiles() {
        List<File> indivFiles = new ArrayList<File>();
        lock.readLock().lock();
        try {
            for ( String key : fileData.keySet() ) {
                indivFiles.add(new File(key));
            }
        } finally {
            lock.readLock().unlock();
        }
        return indivFiles;
    }

    /** Retuns true if the given folder is the incomplete folder. */
    boolean isIncompleteDirectory(File folder) {
        return FileUtils.canonicalize(SharingSettings.INCOMPLETE_DIRECTORY.get()).equals(folder);
    }
    
    /** Returns the IDs of all collections. */
    Collection<Integer> getStoredCollectionIds() {
        lock.readLock().lock();
        try {
            return new ArrayList<Integer>(collectionNames.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** Marks the given file as either in the collection or not in the collection. */
    void setFileInCollection(File file, int collectionId, boolean contained) {
        lock.writeLock().lock();
        try {
            if(contained) {
                dirty |= addFileToCollection(file, collectionId);
            } else {
                dirty |= removeFileFromCollection(file, collectionId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Sets whether or not all the given files should be in the collection. */
    void setFilesInCollection(Iterable<FileDesc> fileDescs, int collectionId, boolean contained) {
        lock.writeLock().lock();
        try {
            if(contained) {
                for(FileDesc fd : fileDescs) {
                    dirty |= addFileToCollection(fd.getFile(), collectionId);
                } 
            } else {
                for(FileDesc fd : fileDescs) {
                    dirty |= removeFileFromCollection(fd.getFile(), collectionId);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Returns true if the file was removed from the collection, false if it wasn't in the collection. */
    private boolean removeFileFromCollection(File file, int collectionId) {
        List<Integer> collections = fileData.get(createKey(file));
        if(collections == null || collections.isEmpty()) {
            return false;
        }
        
        // cast to ensure we use remove(Object) and not remove(int)
        return collections.remove((Integer)collectionId);
    }

    /** Returns true if file was added to the collection, false if it already was in the collection. */
    private boolean addFileToCollection(File file, int collectionId) {
        boolean changed = false;
        
        List<Integer> collections = fileData.get(createKey(file));
        if(collections == null || collections == Collections.<Integer>emptyList()) {
            collections = new ArrayList<Integer>(1);
            fileData.put(createKey(file), collections);
        }
        
        if(!collections.contains(collectionId)) {
            collections.add(collectionId);
            changed = true;
        }
        
        return changed;        
    }
    
    /** Returns true if the file is in the given collection. */
    boolean isFileInCollection(File file, int collectionId) {
        lock.readLock().lock();
        try {
            List<Integer> collections = fileData.get(createKey(file));
            if(collections != null) {
                return collections.contains(collectionId);
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }        
    }

    /** Returns the name of the given collection's id. */
    String getNameForCollection(int collectionId) {
        lock.readLock().lock();
        try {
            return collectionNames.get(collectionId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** Sets a new name for the collection of the given id. */
    boolean setNameForCollection(int collectionId, String name) {
        lock.writeLock().lock();
        try {
            String oldName = collectionNames.put(collectionId, name);
            boolean changed = oldName == null || !oldName.equals(name);
            dirty |= changed;
            return changed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns an ID that will be used for a new collection with the given name. */
    int createNewCollection(String name) {
        lock.writeLock().lock();
        try {
            int nextId = MIN_COLLECTION_ID;
            if(!collectionNames.isEmpty()) {
                nextId = collectionNames.lastKey() + 1;
            }
            collectionNames.put(nextId, name);
            dirty = true;
            return nextId;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Removes the collection's share data & name.  This assumes all files have already been dereferenced. */
    void removeCollection(int collectionId) {
        lock.writeLock().lock();
        try {
           dirty |= collectionNames.remove(collectionId) != null;
           dirty |= collectionShareData.remove(collectionId) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Adds a new shareId to the given collection's Id. */
    boolean addFriendToCollection(int collectionId, String friendId) {
        lock.writeLock().lock();
        try {
            List<String> ids = collectionShareData.get(collectionId);
            if(ids == null) {
                ids = Collections.emptyList();
            }
            
            if(!ids.contains(friendId)) {
                ids = new ArrayList<String>(ids);                
                ids.add(friendId);
                collectionShareData.put(collectionId, Collections.unmodifiableList(ids));
                dirty = true;
                return true;
            } else {
                return false;
                }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Removes a particular shareId from the given collection's Id. */
    boolean removeFriendFromCollection(int collectionId, String friendId) {
        lock.writeLock().lock();
        try {
            List<String> ids = collectionShareData.get(collectionId);
            if(ids != null && ids.contains(friendId)) {
                ids = new ArrayList<String>(ids);
                ids.remove(friendId);
                collectionShareData.put(collectionId, Collections.unmodifiableList(ids));
                dirty = true;
                return true;
            } else {
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }    
    
    /** Returns all shareIds for the given collection id. */
    List<String> getFriendsForCollection(int collectionId) {
        lock.readLock().lock();
        try {
            List<String> ids = collectionShareData.get(collectionId);
            if(ids != null) {
                return Collections.unmodifiableList(new ArrayList<String>(ids));
            } else {
                return Collections.emptyList();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets a new share id list for the given collection id. Returns null if no
     * change was performed because the lists were the same, otherwise returns
     * the list this replaced.
     */
    List<String> setFriendsForCollection(int collectionId, List<String> newIds) {
        lock.writeLock().lock();
        try {
            List<String> oldIds = collectionShareData.get(collectionId);
            if(oldIds == null) {
                oldIds = Collections.emptyList();
            }
            
            // See if old & new are the same -- if so, don't bother.
            // (use a HashSet so that equality isn't order based)
            if(new HashSet<String>(oldIds).equals(newIds)) {
                return null;
            } else {            
                if(newIds.isEmpty()) {
                    collectionShareData.remove(collectionId);
                } else {
                    newIds = Collections.unmodifiableList(new ArrayList<String>(newIds));            
                    collectionShareData.put(collectionId, newIds);
                }
                dirty = true;
                return oldIds;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean isProgramManagingAllowed() {
        return LibrarySettings.ALLOW_PROGRAMS.getValue();
    }
    
    boolean isGnutellaDocumentSharingAllowed() {
        return LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
    }
    
    public int peekPublicSharedListCount() {
        lock.readLock().lock();
        try {
            int count = 0;
            for ( List<Integer> listForFile : fileData.values() ) {
                if (listForFile.contains(DEFAULT_SHARED_COLLECTION_ID)) {
                    count++;
                }
            }
            return count;
        }
        finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Helper method for inspections since this class can not contain inspections due to crazy guice stuff.
     */
    int getChangeInNumPublicFiles() {
        if (originalNumPublicSharedFiles > -1) {
            return peekPublicSharedListCount() - originalNumPublicSharedFiles;
        } else {
            return -1;
        }
    }
    
    private static class FileProperties implements Serializable {
        private static final long serialVersionUID = 767248414812908206L;
        private boolean gnutella;
        private Set<String> friends;
        
        @Override
        public String toString() {
            return "FileProperties: gnutella: " + gnutella + ", friends: " + friends;
        }
    }
}
