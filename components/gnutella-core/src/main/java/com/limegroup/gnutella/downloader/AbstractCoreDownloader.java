package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.download.DownloadException.ErrorCode;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.InvalidDataException;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.bittorrent.BTDownloader;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

/**
 * A basic implementation of CoreDownloader.
 * 
 * Subclasses still need to do the heavy-lifting.
 */
public abstract class AbstractCoreDownloader implements CoreDownloader {

	/**
	 * The current priority of this download -- only valid if inactive.
	 * Has no bearing on the download itself, and is used only so that the
	 * download doesn't have to be indexed in DownloadManager's inactive list
	 * every second, for GUI updates.
	 */
	private volatile int inactivePriority;
	
	/**
	 * A map of attributes associated with the download. The attributes
	 * may be used by GUI, to keep some additional information about
	 * the download.
	 */
	private Map<String, Attribute> attributes = new ConcurrentHashMap<String, Attribute>();
	
	/**
	 * The save file this download should be saved too.
	 * If null, the subclass should return a suggested location.
	 */
	private File saveFile;
	
	/** The default fileName this should use. */
	private String defaultFileName;

	private final SaveLocationManager saveLocationManager;
	private final CategoryManager categoryManager;
	
	protected AbstractCoreDownloader(SaveLocationManager saveLocationManager, CategoryManager categoryManager) {
	    this.saveLocationManager = Objects.nonNull(saveLocationManager, "saveLocationManager");
	    this.categoryManager = Objects.nonNull(categoryManager, "categoryManager");
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#setInactivePriority(int)
     */
	public void setInactivePriority(int priority) {
	    inactivePriority = priority;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#getInactivePriority()
     */
	public int getInactivePriority() {
	    return inactivePriority;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#setAttribute(java.lang.String, java.io.Serializable)
     */
	public Object setAttribute(String key, Object value, boolean serialize) {
	    return attributes.put( key, new Attribute(serialize, value) );
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#getAttribute(java.lang.String)
     */
	public Object getAttribute(String key) {
	    Attribute attr = attributes.get(key);
	    if(attr != null)
	        return attr.getObject();
	    else
	        return null;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#removeAttribute(java.lang.String)
     */
	public Object removeAttribute(String key) {
	    return attributes.remove( key );
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#conflicts(java.io.File)
     */
	public boolean conflictsSaveFile(File saveFile) {
		return getSaveFile().equals(saveFile);
	}
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#setSaveFile(java.io.File, java.lang.String, boolean)
     */
	public void setSaveFile(File saveDirectory, String fileName, boolean overwrite) throws DownloadException {
	    if (fileName == null) {
	        fileName = getDefaultFileName();
	    }
	    
	    Category category = null;
	    if(fileName != null) {
	        category = categoryManager.getCategoryForFilename(fileName);
	    }
	    
	    if (saveDirectory == null) {
	        saveDirectory = SharingSettings.getSaveDirectory(category);
	    }
	    
	    // Forcibly create the save directory, if it doesn't exist.
	    if(!saveDirectory.exists()) {
	        saveDirectory.mkdirs();
	    }
	    
	    try {
	        fileName = CommonUtils.convertFileName(saveDirectory, fileName);
	    }
	    catch (IOException ie) {
	        // if not a directory, give precedence to error messages below
	        if (saveDirectory.isDirectory()) {
	            throw new DownloadException(ErrorCode.PATH_NAME_TOO_LONG, saveDirectory);
	        }
	    }
	    
	    if (!saveDirectory.isDirectory()) {
	        if (saveDirectory.exists()) {
	            throw new DownloadException(ErrorCode.NOT_A_DIRECTORY, saveDirectory);
	        }
	        throw new DownloadException(ErrorCode.DIRECTORY_DOES_NOT_EXIST, saveDirectory);
	    }
	    
	    File candidateFile = new File(saveDirectory, fileName);
	    try {
	        if (!FileUtils.isReallyParent(saveDirectory, candidateFile))
	            throw new DownloadException(ErrorCode.SECURITY_VIOLATION, candidateFile);
	    } catch (IOException e) {
	        throw new DownloadException(ErrorCode.FILESYSTEM_ERROR, candidateFile);
	    }
		
	    if (! FileUtils.setWriteable(saveDirectory))    
	        throw new DownloadException(ErrorCode.DIRECTORY_NOT_WRITEABLE,saveDirectory);
		
	    if (candidateFile.exists()) {
	        if (!candidateFile.isFile() && !(this instanceof BTDownloader))
	            throw new DownloadException(ErrorCode.FILE_NOT_REGULAR, candidateFile);
	        if (!overwrite)
	            throw new DownloadException(ErrorCode.FILE_ALREADY_EXISTS, candidateFile);
	    }
		
		// check if another existing download is being saved to this download
		// we ignore the overwrite flag on purpose in this case
		if (saveLocationManager.isSaveLocationTaken(candidateFile)) {
			throw new DownloadException(ErrorCode.FILE_IS_ALREADY_DOWNLOADED_TO, candidateFile);
		}
	     
	    // Passed sanity checks, so save file
	    synchronized (this) {
	        if (!isRelocatable())
	            throw new DownloadException(ErrorCode.FILE_ALREADY_SAVED, candidateFile);
	        setSaveFileInternal(candidateFile);
	    }
	}
	
	protected synchronized void setSaveFileInternal(File saveFile) {
	    this.saveFile = saveFile;
	}
	
	public synchronized File getSaveFile() {
	    if(saveFile == null)
	        return getDefaultSaveFile();
	    else
	        return saveFile;
	}
	
	/** A default location where this file should be saved, if no explicit saveFile is set. */
	protected abstract File getDefaultSaveFile();
	
    /**
	 * Returns the value for the key {@link CoreDownloader#DEFAULT_FILENAME} from
	 * the properties map.
	 * <p>
	 * Subclasses should put the name into the map or override this
	 * method.
	 */
    protected synchronized String getDefaultFileName() {     
        assert defaultFileName != null : "no default filename initialized!";
		return CommonUtils.convertFileName(defaultFileName);
    }
    
    /** Returns true if a defaultFileName is set. */
    protected synchronized boolean hasDefaultFileName() {
        return defaultFileName != null;
    }
    
    /** Sets the default filename this will use. */
    protected synchronized void setDefaultFileName(String defaultFileName) {
        this.defaultFileName = defaultFileName;
    }
    
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        this.saveFile = memento.getSaveFile();
        setDefaultFileName(memento.getDefaultFileName());
        if(memento.getAttributes() != null) {
            for(Map.Entry<String, Object> entry : memento.getAttributes().entrySet()) {
                attributes.put(entry.getKey(), new Attribute(true, entry.getValue()));
            }
        }
    }
    
    public final synchronized DownloadMemento toMemento() {
        DownloadMemento memento = createMemento();
        fillInMemento(memento);
        return memento;
    }
    
    /** Constructs the correct type of memento. */
    protected abstract DownloadMemento createMemento();

    /** Fills in all data this class wants to store. */
    protected void fillInMemento(DownloadMemento memento) {
        memento.setDownloadType(getDownloadType());
        memento.setSaveFile(saveFile);
        memento.setDefaultFileName(defaultFileName);
        Map<String, Object> saveAttributes = new HashMap<String, Object>(attributes.size());
        for(Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            if(entry.getValue().isSerialize())
                saveAttributes.put(entry.getKey(), entry.getValue().getObject());
        }
        memento.setAttributes(saveAttributes);
    }
    
    /** A wrapper for an attribute. */
    private static class Attribute {
        private final boolean serialize;
        private final Object object;
        
        public Attribute(boolean serialize, Object object) {
            this.serialize = serialize;
            this.object = object;
        }

        public boolean isSerialize() {
            return serialize;
        }

        public Object getObject() {
            return object;
        }        
    }

    @Override
    public boolean isMementoSupported() {
        return true;
    }
    
    
}
