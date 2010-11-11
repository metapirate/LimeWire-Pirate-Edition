package com.limegroup.gnutella.xml;

import java.util.Collection;

import com.limegroup.gnutella.library.FileDesc;

/** A simple interface for getting cached XML data or determining if XML data can be constructed. */
public interface XmlController {
    
    /**
     * Loads cached XML only.  Does not construct documents if there is nothing cached.
     * Returns true if this successfully loaded cached XML, false otherwise.  If the
     * pre-built XML list has any valid XML for this file, that XML is cached in preference
     * to any already-cached XML.
     */
    boolean loadCachedXml(FileDesc fd, Collection<? extends LimeXMLDocument> prebuilt);
    
    /** Determines whether or not XML can be constructed for this file. */
    boolean canConstructXml(FileDesc fd);
    
    /** Loads new XML for a file. Returns true if anything loaded. */
    boolean loadXml(FileDesc fd);

}
