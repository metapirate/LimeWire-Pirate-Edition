package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.settings.DHTSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.I18NConvert;
import org.limewire.util.Objects;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.routing.HashFunction;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * This class contains data for an individual shared file.  It also provides
 * various utility methods for checking against the encapsulated data.<p>
 */

class FileDescImpl implements FileDesc {

	/**
	 * Constant for the index of this <tt>FileDesc</tt> instance in the 
	 * shared file data structure.
	 */
    private final int index;

    private volatile UrnSet urns;    

	/**
	 * Constant for the <tt>File</tt> instance.
	 */
	private final File file;
	
	/**
	 * The License, if one exists, for this FileDesc.
	 */
	private License _license;
	
	/**
	 * The LimeXMLDocs associated with this FileDesc.
	 */
	private final CopyOnWriteArrayList<LimeXMLDocument> _limeXMLDocs = new CopyOnWriteArrayList<LimeXMLDocument>();

	/**
	 * The size of the associated File.
	 */
	private final long fileSize;
	
	private final long lastModified;
	
	/**
	 * The number of hits this file has recieved.
	 */
	private int hits;
	
	/** 
	 * The number of times this file has had attempted uploads
	 */
	private int _attemptedUploads;
	
    /**
     * The time when the last attempt was made to upload this file
     */
    private long lastAttemptedUploadTime = System.currentTimeMillis();
    
	/** 
	 * The number of times this file has had completed uploads
	 */
	private int _completedUploads;
	
	/** True if this is a store file. */
    private volatile boolean storeFile;
    
    /** True if this can be shared. */
    private volatile boolean isShareable = true;
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster;
    private final RareFileStrategy rareFileStrategy;
    private final LicenseFactory licenseFactory;
    
    private final ConcurrentHashMap<String, Object> clientProperties =
        new ConcurrentHashMap<String, Object>(4, 0.75f, 1); // non-default initialCapacity,
                                                            // concurrencyLevel, saves
                                                            // ~1k memory / file.

                                                            // consider sizing even smaller,
                                                            // using other Map impls, or
                                                            // eliminating the use of a
                                                            // Map altogether

	    
    /**
	 * Constructs a new <tt>FileDesc</tt> instance from the specified 
	 * <tt>File</tt> class and the associated urns.
     * @param file the <tt>File</tt> instance to use for constructing the
	 *  <tt>FileDesc</tt>
     * @param urns the URNs to associate with this FileDesc
     * @param index the index in the FileManager
     */
    FileDescImpl(RareFileStrategy rareFileStrategy, LicenseFactory licenseFactory,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster,
            File file,
            Set<? extends URN> urns,
            int index) {
		
        if(index < 0) {
			throw new IndexOutOfBoundsException("negative index (" + index + ") not permitted in FileDesc");
		}
        
        fileSize = file.length();
        assert fileSize >= 0 && fileSize <= MAX_FILE_SIZE : "invalid size "+fileSize+" of file "+file;
        Objects.nonNull(urns, "urns");
        
		this.rareFileStrategy = rareFileStrategy;
		this.multicaster = multicaster;
		this.licenseFactory = licenseFactory;
		this.file = Objects.nonNull(file, "file");
        this.index = index;
        this.urns = UrnSet.unmodifiableSet(urns); 
        this.lastModified = file.lastModified();
        
        hits = 0; // Starts off with 0 hits
    }
    
    @Override
    public boolean isRareFile() {
        return rareFileStrategy.isRareFile(this);
    }

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#hasUrns()
     */
	public boolean hasUrns() {
		return !urns.isEmpty();
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getIndex()
     */
	public int getIndex() {
		return index;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getFileSize()
     */
	public long getFileSize() {
		return fileSize;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getFileName()
     */
	public String getFileName() {
		return I18NConvert.instance().compose(file.getName());
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#lastModified()
     */
	public long lastModified() {
		return lastModified;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getFile()
     */
    public File getFile() {
        return file;
    }
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getTTROOTUrn()
     */
    @Override
	public URN getTTROOTUrn() {
	    return urns.getTTRoot();
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getSHA1Urn()
     */
	@Override
    public URN getSHA1Urn() {
        return urns.getSHA1();
    }
    
    @Override
    public URN getNMS1Urn() {
        return urns.getNMS1();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#addUrn(com.limegroup.gnutella.URN)
     */
    public void addUrn(URN urn) {
        boolean contained = urns.contains(urn);
        if(!contained) {
            UrnSet newSet = UrnSet.modifiableSet(urns);
            newSet.add(urn);
            urns = UrnSet.unmodifiableSet(newSet);
            if(multicaster != null && urn.isTTRoot()) {
                multicaster.handleEvent(new FileDescChangeEvent(this, FileDescChangeEvent.Type.TT_ROOT_ADDED, urn));
            }
            if(multicaster != null && urn.isNMS1()) {
                multicaster.handleEvent(new FileDescChangeEvent(this, FileDescChangeEvent.Type.NMS1_ADDED, urn));
            }
        }
    }
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getUrns()
     */
	public Set<URN> getUrns() {
		return urns;
	}   

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getPath()
     */
	public String getPath() {
		return file.getAbsolutePath();
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#addLimeXMLDocument(com.limegroup.gnutella.xml.LimeXMLDocument)
     */
	public void addLimeXMLDocument(LimeXMLDocument doc) {
        _limeXMLDocs.add(doc);
        
	    doc.initIdentifier(file);
	    assignLicense(doc);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#replaceLimeXMLDocument(com.limegroup.gnutella.xml.LimeXMLDocument, com.limegroup.gnutella.xml.LimeXMLDocument)
     */
    public boolean replaceLimeXMLDocument(LimeXMLDocument oldDoc, 
                                          LimeXMLDocument newDoc) {
        synchronized(_limeXMLDocs) {
            int index = _limeXMLDocs.indexOf(oldDoc);
            if( index == -1 )
                return false;
            
            _limeXMLDocs.set(index, newDoc);
        }
        
        newDoc.initIdentifier(file);
        assignLicense(newDoc);
        return true;
    }
    
    private void assignLicense(LimeXMLDocument doc) {
        _license = null;
        if(doc.isLicenseAvailable()) {
            String license = doc.getLicenseString();
            if(license != null) {
                _license = licenseFactory.create(license);
            } else {
                _license = null;
            }
        } else {
            _license = null;
        }
        
        storeFile = doc.getLicenseString() != null &&
                    (doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name()) ||
                     doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_RESHAREABLE.name()));
        
        if(doc.getLicenseString() != null &&
                    doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name())) {
            isShareable = false;
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#removeLimeXMLDocument(com.limegroup.gnutella.xml.LimeXMLDocument)
     */
    public boolean removeLimeXMLDocument(LimeXMLDocument toRemove) {
        
        if (!_limeXMLDocs.remove(toRemove))
            return false;
        
        if(_license != null && toRemove.isLicenseAvailable())
            _license = null;
        
        return true;
    }   
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getLimeXMLDocuments()
     */
    public List<LimeXMLDocument> getLimeXMLDocuments() {
        return _limeXMLDocs;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getXMLDocument()
     */
    public LimeXMLDocument getXMLDocument() {
        List<LimeXMLDocument> docs = getLimeXMLDocuments();
        return docs.isEmpty() ? null : docs.get(0);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getXMLDocument(java.lang.String)
     */
    public LimeXMLDocument getXMLDocument(String schemaURI) {
        for(LimeXMLDocument doc : getLimeXMLDocuments()) {
            if (doc.getSchemaURI().equalsIgnoreCase(schemaURI))
                return doc;
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#isLicensed()
     */
    public boolean isLicensed() {
        return _license != null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getLicense()
     */
    public License getLicense() {
        return _license;
    }
	
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#containsUrn(com.limegroup.gnutella.URN)
     */
    public boolean containsUrn(URN urn) {
        return urns.contains(urn);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementHitCount()
     */    
    public int incrementHitCount() {
        return ++hits;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getHitCount()
     */
    public int getHitCount() {
        return hits;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementAttemptedUploads()
     */    
    public synchronized int incrementAttemptedUploads() {
        lastAttemptedUploadTime = System.currentTimeMillis();
        return ++_attemptedUploads;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getAttemptedUploads()
     */
    public synchronized int getAttemptedUploads() {
        return _attemptedUploads;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getLastAttemptedUploadTime()
     */
    public synchronized long getLastAttemptedUploadTime() {
        return lastAttemptedUploadTime;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#incrementCompletedUploads()
     */    
    public int incrementCompletedUploads() {
        return ++_completedUploads;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#getCompletedUploads()
     */
    public int getCompletedUploads() {
        return _completedUploads;
    }       
    
	// overrides Object.toString to provide a more useful description
	@Override
    public String toString() {
		return ("FileDesc:\r\n"+
				"name:     "+getFileName()+"\r\n"+
				"index:    "+index+"\r\n"+
				"path:     "+getPath()+"\r\n"+
				"size:     "+getFileSize()+"\r\n"+
				"modTime:  "+lastModified()+"\r\n"+
				"File:     "+file+"\r\n"+
				"urns:     "+urns+"\r\n"+
				"docs:     "+ _limeXMLDocs+"\r\n");
	}
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#lookup(java.lang.String)
     */
    public String lookup(String key) {
        if (key == null)
            return null;
        if ("hits".equals(key))
            return String.valueOf(getHitCount());
        else if ("ups".equals(key))
            return String.valueOf(getAttemptedUploads());
        else if ("cups".equals(key))
            return String.valueOf(getCompletedUploads());
        else if ("lastup".equals(key))
            return String.valueOf(System.currentTimeMillis() - getLastAttemptedUploadTime());
        else if ("licensed".equals(key))
            return String.valueOf(isLicensed());
        else if ("atUpSet".equals(key))
            return DHTSettings.RARE_FILE_ATTEMPTED_UPLOADS.getValueAsString();
        else if ("cUpSet".equals(key))
            return DHTSettings.RARE_FILE_COMPLETED_UPLOADS.getValueAsString();
        else if ("rftSet".equals(key))
            return DHTSettings.RARE_FILE_TIME.getValueAsString();
        else if ("hasXML".equals(key))
            return String.valueOf(getXMLDocument() != null);
        else if ("size".equals(key))
            return String.valueOf(getFileSize());
        else if ("lastM".equals(key))
            return String.valueOf(lastModified());
        else if ("numKW".equals(key))
            return String.valueOf(HashFunction.keywords(getPath()).length);
        else if ("numKWP".equals(key))
            return String.valueOf(HashFunction.getPrefixes(HashFunction.keywords(getPath())).length);
        else if (key.startsWith("xml_") && getXMLDocument() != null) {
            key = key.substring(4,key.length());
            return getXMLDocument().lookup(key);
            
        // Note: Removed 'firewalled' check -- might not be necessary, but
        // should see if other ways to re-add can be done.
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.library.FileDesc#isStoreFile()
     */
    public boolean isStoreFile() {
        return storeFile;
    }
    
    public boolean isShareable() {
        return isShareable;
    }
    
    @Override
    public void addListener(EventListener<FileDescChangeEvent> listener) {
        multicaster.addListener(this, listener);
    }
    
    @Override
    public boolean removeListener(EventListener<FileDescChangeEvent> listener) {
        return multicaster.removeListener(this, listener);
    }
    
    @Override
    public Object getClientProperty(String property) {
        return clientProperties.get(property);
    }
    
    @Override
    public void putClientProperty(String property, Object value) {
        clientProperties.put(property, value);
    }
}


