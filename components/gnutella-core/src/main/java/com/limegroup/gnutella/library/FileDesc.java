package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.limewire.listener.ListenerSupport;
import org.limewire.util.RPNParser.StringLookup;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface FileDesc extends StringLookup, ListenerSupport<FileDescChangeEvent> {
    
    /** Returns true if this is considered a rare file. */
    boolean isRareFile();

    /**
     * Returns the index of this file in our file data structure.
     *
     * @return the index of this file in our file data structure
     */
    public int getIndex();

    /**
     * Returns the size of the file on disk, in bytes.
     *
     * @return the size of the file on disk, in bytes
     */
    public long getFileSize();

    /**
     * Returns the name of this file.
     * 
     * @return the name of this file
     */
    public String getFileName();

    /**
     * Returns the last modification time for the file according to this
     * <tt>FileDesc</tt> instance.
     *
     * @return the modification time for the file
     */
    public long lastModified();

    /**
     * @return the TTROOT URN from the set of urns.
     */
    public URN getTTROOTUrn();

    /**
     * Returns the <tt>File</tt> instance for this <tt>FileDesc</tt>.
     *
     * @return the <tt>File</tt> instance for this <tt>FileDesc</tt>
     */
    public File getFile();

    /**
     * @return the SHA1 for the file.
     */
    public URN getSHA1Urn();
    
    /**
     * @return the SHA1 not including metadata for an audio file. If the
     * NonMetaData hash cannot be calculated or the file is not an audio
     * file, returns null;
     */
    public URN getNMS1Urn();

    /**
     * Adds a new URN to this filedesc.
     */
    public void addUrn(URN urn);

    /**
     * Returns a new <tt>Set</tt> instance containing the <tt>URN</tt>s
     * for the this <tt>FileDesc</tt>.  The <tt>Set</tt> instance
     * returned is immutable.
     *
     * @return a new <tt>Set</tt> of <tt>URN</tt>s for this 
     *  <tt>FileDesc</tt>
     */
    public Set<URN> getUrns();

    /**
     * Returns the absolute path of the file represented wrapped by this
     * <tt>FileDesc</tt>.
     *
     * @return the absolute path of the file
     */
    public String getPath();

    /**
     * Adds a LimeXMLDocument to this FileDesc.
     */
    public void addLimeXMLDocument(LimeXMLDocument doc);

    /**
     * Replaces one LimeXMLDocument with another.
     */
    public boolean replaceLimeXMLDocument(LimeXMLDocument oldDoc, LimeXMLDocument newDoc);

    /**
     * Removes a LimeXMLDocument from the FileDesc.
     */
    public boolean removeLimeXMLDocument(LimeXMLDocument toRemove);

    /**
     * Returns the LimeXMLDocuments for this FileDesc.
     */
    public List<LimeXMLDocument> getLimeXMLDocuments();

    /**
     * Returns the first LimeXMLDocument or null if the 
     * document List is empty.
     */
    public LimeXMLDocument getXMLDocument();

    /**
     * Returns a LimeXMLDocument whose schema URI is equal to
     * the passed schema URI or null if no such LimeXMLDocument
     * exists.
     */
    public LimeXMLDocument getXMLDocument(String schemaURI);

    /**
     * Determines if a license exists on this FileDesc.
     */
    public boolean isLicensed();

    /**
     * Returns the license associated with this FileDesc.
     */
    public License getLicense();

    /**
     * Determine whether or not the given <tt>URN</tt> instance is 
     * contained in this <tt>FileDesc</tt>.
     *
     * @param urn the <tt>URN</tt> instance to check for
     * @return <tt>true</tt> if the <tt>URN</tt> is a valid <tt>URN</tt>
     *  for this file, <tt>false</tt> otherwise
     */
    public boolean containsUrn(URN urn);

    /**
     * Increase & return the new hit count.
     * @return the new hit count
     */
    public int incrementHitCount();

    /** 
     * @return the current hit count 
     */
    public int getHitCount();

    /**
     * Increase & return the new attempted uploads.
     * @return the new attempted upload count
     */
    public int incrementAttemptedUploads();

    /** 
     * @return the current attempted uploads
     */
    public int getAttemptedUploads();

    /**
     * Returns the time when the last upload attempt was made.
     */
    public long getLastAttemptedUploadTime();

    /**
     * Increase & return the new completed uploads.
     * @return the new completed upload count
     */
    public int incrementCompletedUploads();

    /** 
     * @return the current completed uploads
     */
    public int getCompletedUploads();

    /**
     * @return true if this file came from the LWS, false otherwise.
     */
    public boolean isStoreFile();
    
    /**
     * @return true if this file can be shared, false otherwise.
     */
    public boolean isShareable();
    
    /** Sets a new property for this FileDesc. */
    public void putClientProperty(String property, Object value);
    
    /** Retrieves a set property from this FileDesc. */
    public Object getClientProperty(String property);

}