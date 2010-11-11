package com.limegroup.gnutella.xml;

import java.io.File;
import java.net.URL;

import org.limewire.util.CommonUtils;

import com.google.inject.Singleton;


/**  Various utilities for LimeWire XML classes. */
@Singleton
public class LimeXMLProperties {

    /**
     * The default index for responses when there is no file and 
     * hence none to download. The value is set to 2^32 -1.
     */
    public static final long DEFAULT_NONFILE_INDEX = 0x00000000FFFFFFFFl;
    
    private String[] BUILT_IN_SCHEMAS = { "application", "audio", "document", "image", "video", "torrent" };

    /** Schema resource directory. */
    private static final String SCHEMA_RESOURCE_PATH = "org/limewire/xml/schema/";
    
    /** Place where serialized XML data is stored. */
    private static final String XML_DOCS_DIR = "xml/data";

    /**
     * Returns the name of the directory where the XML Documents are located.
     */
    public File getXMLDocsDir() {
        return new File(CommonUtils.getUserSettingsDir(), XML_DOCS_DIR);
    }

    /**
     * Returns the files pertaining to the XML Schemas used for 
     * querying/responding.
     */
    public URL[] getAllXmlSchemaUrls() {
        URL[] urls = new URL[BUILT_IN_SCHEMAS.length];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = LimeXMLProperties.class.getClassLoader().getResource(
                    SCHEMA_RESOURCE_PATH + BUILT_IN_SCHEMAS[i] + ".xsd");
        }
        return urls;
    }
}


