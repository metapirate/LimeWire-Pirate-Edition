package com.limegroup.gnutella;

import java.util.Set;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface FileDetails {

	/**
	 * Returns the file name.
	 * @return
	 */
	String getFileName();
	/**
	 * Returns the sha1 urn or <code>null</code> if there is none.
	 * @return
	 */
	URN getSHA1Urn();
	/**
	 * Returns the size of the file.
	 * @return
	 */
	long getSize();
	/**
	 * Returns the set of urns.
	 * @return
	 */
	Set<URN> getUrns();
	/**
	 * Returns the xml document or <code>null</code> if there is none for this
	 * file.
	 * @return
	 */
	LimeXMLDocument getXMLDocument();
	
    
    /**
     * Accessor for the index this file, which can be <tt>null</tt>.
     *
     * @return the file name for this file, which can be <tt>null</tt>
     */
    public long getIndex();
    
    /**
     * The creation time of this file.
     */
    public long getCreationTime();
}
