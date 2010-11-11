package com.limegroup.gnutella.library;

import java.util.List;

import org.limewire.listener.DefaultSourceTypeEvent;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FileDescChangeEvent extends DefaultSourceTypeEvent<FileDesc, FileDescChangeEvent.Type> {
    
    public static enum Type { TT_ROOT_ADDED, NMS1_ADDED }
    
    private final List<? extends LimeXMLDocument> xmlDocs;
    private final URN urn;
    
    public FileDescChangeEvent(FileDesc fileDesc, Type type) {
        super(fileDesc, type);
        this.xmlDocs = null;
        this.urn = null;
    }
    
    public FileDescChangeEvent(FileDesc fileDesc, Type type, List<? extends LimeXMLDocument> xmlDocs) {
        super(fileDesc, type);
        this.xmlDocs = xmlDocs;
        this.urn = null;
    }
    
    public FileDescChangeEvent(FileDesc fileDesc, Type type, URN urn) {
        super(fileDesc, type);
        this.xmlDocs = null;
        this.urn = urn;
    }
    
    public List<? extends LimeXMLDocument> getXmlDocs() {
        return xmlDocs;
    }
    
    public URN getUrn() {
        return urn;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this) + ", super: " + super.toString();
    }

}
