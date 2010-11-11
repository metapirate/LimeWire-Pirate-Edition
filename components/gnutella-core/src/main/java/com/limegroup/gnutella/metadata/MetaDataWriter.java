package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

/**
 *  A utility class that writes modified LimeXMLDocuments as Meta-data to 
 *  an audio or video file.
 */
public class MetaDataWriter {

    /**
     * File we're writing to.
     */
    private final String fileName;
    
    /**
     * The editor that we're using.
     */
    private final MetaWriter editor;
    
    /**
     * The audio data to be written to the file.
     */
    private final AudioMetaData audioData;
    
    /**
     * LimeXMLDocument that populated the MetaData.
     */
    protected LimeXMLDocument correctDocument= null;
    
    public MetaDataWriter(String fileName, MetaDataFactory metaDataFactory) {
        this.fileName = fileName;
        this.audioData = new AudioMetaData();
        editor = metaDataFactory.getEditorForFile(fileName);
    }
    
    public boolean needsToUpdate(MetaData data) {
        if(editor == null)
            return false;
        else if ( data == null )
            return true;
        return !audioData.equals(data);
    }
    
    /**
     * Performs the actual write of the metadata to disk.
     * @return status code as defined in LimeWireXMLReplyCollection
     */
    public MetaDataState commitMetaData(){
        return editor.commitMetaData(fileName, audioData);
    }
    
    /**
     * Populates the editor with the values from xmldocument.
     */
    public void populate(LimeXMLDocument doc) {
        if( editor == null )
            throw new NullPointerException("Editor not created");
        correctDocument = doc;
        audioData.populate(doc);
    }

    public LimeXMLDocument getCorrectDocument() {
        return correctDocument;
    }
    
    public MetaWriter getEditor(){
        return editor;
    }
}
