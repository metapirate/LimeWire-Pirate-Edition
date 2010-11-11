package com.limegroup.gnutella.metadata;

import java.util.List;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Wrapper for MetaData from a file. When marshalling meta-data from disk to
 * LimeXMLDocument and from a LimeXMLDocument to disk, the data gets wrapped
 * inside this Object.
 */
public interface MetaData {

    /**
     * @return the type of schema this metadata represents
     */
    public String getSchemaURI();

    /**
     * @return a NameValue list of all the current data for this schema
     */
    public List<NameValue<String>> toNameValueList();

    /**
     * Fills in the saved metadata from reading the LimeXMLDocument.
     * 
     * @param doc a LimeXMLDocument that is used to populate the MetaData when
     *        it needs to be written out to disk
     */
    public void populate(LimeXMLDocument doc);
}