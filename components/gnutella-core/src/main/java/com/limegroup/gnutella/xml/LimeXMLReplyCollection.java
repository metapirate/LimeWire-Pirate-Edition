package com.limegroup.gnutella.xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IdentityHashSet;
import org.limewire.collection.StringTrie;
import org.limewire.io.IOUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.NameValue;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;
import org.xml.sax.SAXException;

import com.google.inject.Provider;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.metadata.MetaDataWriter;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * Maps LimeXMLDocuments for FileDescs in a specific schema.
 */
public class LimeXMLReplyCollection {
    
    private static final Log LOG = LogFactory.getLog(LimeXMLReplyCollection.class);    

    /**
     * The schemaURI of this collection.
     */
    private final String schemaURI;
    
    /**
     * A map of File -> LimeXMLDocument for each shared file that contains XML.
     * <p>
     * SYNCHRONIZATION: Synchronize on LOCK when accessing, 
     *  adding or removing.
     */
    private final Map<FileAndUrn, LimeXMLDocument> mainMap;
    
    /**
     * The old map that was read off disk.
     * <p>
     * Used while initially processing FileDescs to add.
     */
    private final Map<?, LimeXMLDocument> oldMap;
    
    /**
     * A mapping of fields in the LimeXMLDocument to a Trie
     * that has a lookup table for the values of that field.
     * <p>
     * The Trie value is a mapping of keywords in LimeXMLDocuments
     * to the list of documents that have that keyword.
     * <p>
     * SYNCHRONIZATION: Synchronize on LOCK when accessing,
     *  adding or removing.
     */
    private final Map<String, StringTrie<List<LimeXMLDocument>>> trieMap;
    
    /**
     * Whether or not data became dirty after we last wrote to disk.
     */
    private boolean dirty = false;
    
    private final Object LOCK = new Object();

    public static enum MetaDataState {
        UNCHANGED,
        NORMAL,
        FILE_DEFECTIVE,
        RW_ERROR,
        BAD_ID3,
        FAILED_TITLE,
        FAILED_ARTIST,
        FAILED_ALBUM,
        FAILED_YEAR,
        FAILED_COMMENT,
        FAILED_TRACK,
        FAILED_GENRE,
        HASH_FAILED,
        INCORRECT_FILETYPE;
    }

    private final Provider<Library> library;

    private final Provider<LimeXMLDocumentFactory> limeXMLDocumentFactory;

    private final Provider<MetaDataFactory> metaDataFactory;

    private final Provider<MetaDataReader> metaDataReader;
    
    private final File savedDocsDir;

    /**
     * Creates a new LimeXMLReplyCollection.  The reply collection
     * will retain only those XMLDocs that match the given schema URI.
     *
     * @param URI this collection's schema URI
     * @param path directory where the xml documents are stored
     * @param library guice provider used for {@link Library}
     * @param limeXMLDocumentFactory factory object for {@link LimeXMLDocument}
     * @param metaDataReader also used to construct {@link LimeXMLDocument}
     * @param metaDataFactory the MetaDataFactory used in this class
     */
    LimeXMLReplyCollection(String URI, File path, Provider<Library> library,
            Provider<LimeXMLDocumentFactory> limeXMLDocumentFactory, Provider<MetaDataReader> metaDataReader,
            Provider<MetaDataFactory> metaDataFactory) {
        this.schemaURI = URI;
        this.library = library;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;
        this.metaDataFactory = metaDataFactory;
        this.trieMap = new HashMap<String, StringTrie<List<LimeXMLDocument>>>();
        this.mainMap = new HashMap<FileAndUrn, LimeXMLDocument>();
        this.savedDocsDir = path;
        this.oldMap = readMapFromDisk();
    }
    
    /**
     * Initializes the map using either LimeXMLDocuments in the list of potential
     * documents, or elements stored in oldMap.  Items in potential take priority.
     */
    LimeXMLDocument initialize(FileDesc fd, Collection<? extends LimeXMLDocument> potential) {
        LimeXMLDocument doc = null;
        
        // First try to get a doc from the potential list.
        for(LimeXMLDocument next : potential) {
            if(next.getSchemaURI().equals(schemaURI)) {
                doc = next;
                break;
            }
        }
        
        // Then try to get it from the old map.
        if(doc == null) {
            // oldMap can have a value of either FileAndUrn or SHA1.
            doc = oldMap.get(new FileAndUrn(fd));
            if(doc == null) {
                doc = oldMap.get(fd.getSHA1Urn());
            }
        }
        
        
        // Then try and see it, with validation and all.
        if(doc != null) {
            doc = validate(doc, fd);
            if(doc != null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Adding old document for file: " + fd.getFile() + ", doc: " + doc);
                addReply(fd, doc);
            }
        }
        
        return doc;
    }
    
    /**
     * Creates a LimeXMLDocument for the given FileDesc if no XML already exists
     * for it.
     */
    public LimeXMLDocument createIfNecessary(FileDesc fd) {
        LimeXMLDocument doc = null;

        boolean needsXml = false;
        File file = fd.getFile();
        FileAndUrn fileAndUrn = new FileAndUrn(fd);
        synchronized(LOCK) {
            if(!mainMap.containsKey(fileAndUrn)) {
                // If we have no documents for this FD attempt to parse the file.
                if(fd.getLimeXMLDocuments().size() == 0) {
                    needsXml = true;
                }
            }
        }
        
        if(needsXml) {
            // Create the doc outside of the lock.
            doc = constructDocument(file);
            if(doc != null) {
                synchronized(LOCK) {
                    // If we still need the doc, set it!
                    if(!mainMap.containsKey(fileAndUrn)) {
                        if(fd.getLimeXMLDocuments().size() == 0) {
                            if(LOG.isDebugEnabled())
                                LOG.debug("Adding newly constructed document for file: " + file + ", document: " + doc);
                            addReply(fd, doc);
                        }
                    }
                }
            }
        }
        
        return doc;
    }
    
    /**
     * Notification that initial loading is done.
     */
    void loadFinished() {
        synchronized(LOCK) {
            if(oldMap.equals(mainMap)) {
                dirty = false;
            }
            oldMap.clear();
        }
    
    }
    
    /**
     * Validates a LimeXMLDocument.
     * <pre>
     * This checks:
     * 1) If it's current (if not, it attempts to reparse.  If it can't, keeps the old one).
     * 2) If it's valid (if not, attempts to reparse it.  If it can't, drops it).
     * 3) If it's corrupted (if so, fixes & writes the fixed one to disk).
     * </pre>
     */
    private LimeXMLDocument validate(LimeXMLDocument doc, FileDesc fd) {
        if(!((GenericXmlDocument)doc).isCurrent()) {
            if(LOG.isDebugEnabled())
                LOG.debug("reconstructing old document: " + fd.getFile());
            LimeXMLDocument tempDoc = constructDocument(fd.getFile());
            if (tempDoc != null) {
                doc = update(doc, tempDoc);
            } else {
                ((GenericXmlDocument)doc).setCurrent();
            }
        }
            
        // check to see if it's corrupted and if so, fix it.
        if( AudioMetaData.isCorrupted(doc) ) {
            doc = AudioMetaData.fixCorruption(doc, limeXMLDocumentFactory.get());
            mediaFileToDisk(fd, doc);
        }
        
        return doc;
    }
    
    /**
     * Updates an existing old document to be a newer document, but retains all fields
     * that may have been in the old one that are not in the newer (for the case of
     * existing annotations).
     */
    private LimeXMLDocument update(LimeXMLDocument older, LimeXMLDocument newer) {
        Map<String, String> fields = new HashMap<String, String>();
        for(Map.Entry<String, String> next : newer.getNameValueSet()) {
            fields.put(next.getKey(), next.getValue());
        }
        
        for(Map.Entry<String, String> next : older.getNameValueSet()) {
            if(!fields.containsKey(next.getKey()))
                fields.put(next.getKey(), next.getValue());
        }

        List<NameValue<String>> nameValues = new ArrayList<NameValue<String>>(fields.size());
        for(Map.Entry<String, String> next : fields.entrySet())
            nameValues.add(new NameValue<String>(next.getKey(), next.getValue()));
        
        return limeXMLDocumentFactory.get().createLimeXMLDocument(nameValues, newer.getSchemaURI());
     }
    
    /** Returns true if a document can be created for this file. */
    boolean canCreateDocument(File file) {
        MetaDataFactory factory = metaDataFactory.get();
        return LimeXMLNames.AUDIO_SCHEMA.equals(schemaURI) && factory.containsAudioReader(file)
                || LimeXMLNames.VIDEO_SCHEMA.equals(schemaURI) && factory.containsVideoReader(file)
                || LimeXMLNames.TORRENT_SCHEMA.equals(schemaURI) && factory.containsReader(file);
    }
        
    
    /**
     * Creates a LimeXMLDocument from the file.  
     * @return null if the format is not supported or parsing fails,
     *  <tt>LimeXMLDocument</tt> otherwise.
     */
    private LimeXMLDocument constructDocument(File file) { 
        if(canCreateDocument(file)) {
            try {
                // Documents with multiple file formats may be the wrong type.
                LimeXMLDocument document = metaDataReader.get().readDocument(file);
                if(document.getSchemaURI().equals(schemaURI))
                    return document;
            } catch (IOException ignored) {
                LOG.warn("Error creating document", ignored);
            }
        }
        
        return null;
    }
    
    /**
     * Returns the schema URI of this collection.
     */
    public String getSchemaURI(){
        return schemaURI;
    }
    
    /**
     * Adds the keywords of this LimeXMLDocument into the correct Trie 
     * for the field of the value.
     */
    private void addKeywords(LimeXMLDocument doc) {
        synchronized(LOCK) {
            for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
                final String name = entry.getKey();
                final String value = I18NConvert.instance().getNorm(entry.getValue());
                StringTrie<List<LimeXMLDocument>> trie = trieMap.get(name);
                // if no lookup table created yet, create one & insert.
                if(trie == null) {
                    trie = new StringTrie<List<LimeXMLDocument>>(true); //ignore case.
                    trieMap.put(name, trie);
                }

                // extract document metadata attribute into keywords
                // and index document into trie based on every one of its keywords
                Set<String> valuesKeywords = QueryUtils.extractKeywords(value, true);

                for (String keyword : valuesKeywords) {
                    List<LimeXMLDocument> allDocs = trie.get(keyword);

                    // if no list of docs for this keyword created, create & insert.
                    if (allDocs == null) {
                        allDocs = new LinkedList<LimeXMLDocument>();
                        trie.add(keyword, allDocs);
                    }
                    allDocs.add(doc);
                }
            }
        }
    }
    
    /**
     * Removes the keywords of this LimeXMLDocument from the appropriate Trie.
     * If the list is emptied, it is removed from the Trie.
     */
    private void removeKeywords(LimeXMLDocument doc) {
        synchronized(LOCK) {
            for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
                final String name = entry.getKey();
                StringTrie<List<LimeXMLDocument>> trie = trieMap.get(name);
                // if no trie, ignore.
                if(trie == null)
                    continue;
                    
                final String value = I18NConvert.instance().getNorm(entry.getValue());

                // extract keywords from the metadata attribute value
                // and remove the LimeXMLDocument from the index for each keyword
                Set<String> keywords = QueryUtils.extractKeywords(value, true);

                for (String keyword : keywords) {
                    List<LimeXMLDocument> allDocs = trie.get(keyword);
                    // if no list, ignore.
                    if (allDocs == null) {
                        continue;
                    }
                    allDocs.remove(doc);
                    // if we emptied the doc, remove from trie...
                    if (allDocs.size() == 0) {
                        trie.remove(keyword);
                    }
                }
            }
        }
    }

    /**
     * Adds a reply into the mainMap of this collection.
     * Also adds this LimeXMLDocument to the list of documents the
     * FileDesc knows about.
     */
    public void addReply(FileDesc fd, LimeXMLDocument replyDoc) {
    	assert getSchemaURI().equals(replyDoc.getSchemaURI());

        synchronized(LOCK){
            dirty = true;
            mainMap.put(new FileAndUrn(fd),replyDoc);
            if(!isLWSDoc(replyDoc))
            	addKeywords(replyDoc);
        }
        
        fd.addLimeXMLDocument(replyDoc);
    }
    
    /**
     * Determines if the XMLDocument is from the LWS.
     * @return true if this document contains a LWS license, false otherwise
     */
    public boolean isLWSDoc(LimeXMLDocument doc) {
        if( doc != null && doc.getLicenseString() != null && doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.toString()))
            return true;
        return false;
    }

    /**
     * Returns the amount of items in this collection.
     */
    public int getCount(){
        synchronized(LOCK) {
            return mainMap.size();
        }
    }
        
    /**
     * Returns all documents that match the particular query.
     * If no documents match, this returns an empty list.
     * <p>
     * This goes through the following methodology:
     * <pre>
     * 1) Looks in the index trie to determine if ANY
     *    of the values in the query's document match.
     *    If they do, adds the document to a set of
     *    possible matches.  A set is used so the same
     *    document is not added multiple times.
     * 2) If no documents matched, returns an empty list.
     * 3) Iterates through the possible matching documents
     *    and does a fine-grained matchup, using XML-specific
     *    matching techniques.
     * 4) Returns an empty list if nothing matched or
     *    a list of the matching documents.
     * </pre>
     */    
    public Set<LimeXMLDocument> getMatchingDocuments(LimeXMLDocument query) {
        // First get a list of anything that could possibly match.
        // This uses a set so we don't add the same doc twice ...
        Set<LimeXMLDocument> matching = null;
        synchronized(LOCK) {
            
            for(Map.Entry<String, String> entry : query.getNameValueSet()) {
                // Get the name of the particular field being queried for.
                Set<String> metadata = Collections.singleton(entry.getKey());

                // Get the value of that field being queried for.
                final String value = entry.getValue();

                Set<LimeXMLDocument> repliesForMetadata = getMatchingDocumentsIntersectKeywords(metadata, value);

                if (!repliesForMetadata.isEmpty()) {

                    if (matching == null) {
                        matching = new IdentityHashSet<LimeXMLDocument>();
                    }
                    matching.addAll(repliesForMetadata);
                }
            }
        }
        
        // no matches?... exit.
        if( matching == null || matching.size() == 0) {
            return Collections.emptySet();
        }


        // Now filter that list using the real XML matching tool...
        Set<LimeXMLDocument> actualMatches = null;

        for(LimeXMLDocument currReplyDoc : matching) {
            if (LimeXMLUtils.match(currReplyDoc, query, false)) {
                if( actualMatches == null ) {
                    actualMatches = new IdentityHashSet<LimeXMLDocument>();
                }
                actualMatches.add(currReplyDoc);
            }
        }
        
        // No actual matches?... exit.
        if( actualMatches == null || actualMatches.size() == 0 )
            return Collections.emptySet();

        return actualMatches;
    }

    public Set<LimeXMLDocument> getMatchingDocuments(String query) {
        synchronized(LOCK) {
            return getMatchingDocumentsIntersectKeywords(trieMap.keySet(), query);
        }
    }

    /**
     * Returns a Set of matching {@link LimeXMLDocument}s for a passed in Set of metadata fields.
     * The query string is broken down into keywords, and only results common
     * to all keywords are returned.
     * <p/>
     * <ol>
     *    <li>Extract keywords from query</li>
     *    <li>For each keyword, search the metadata fields for matches (names of metadata fields are passed in)</li>
     *    <li>Return the matching LimeXMLDocuments common to all keywords</li>
     * </ol>
     * <p/>
     * NOTE: Caller of this method MUST SYNCHRONIZE on {@link #LOCK}
     *
     * @param metadataFields names of metadata fields to search for matches
     * @param query the query string to use for the search
     * @return LimeXMLDocuments
     */
    private Set<LimeXMLDocument> getMatchingDocumentsIntersectKeywords(Set<String> metadataFields, String query) {
        Set<LimeXMLDocument> matches = new IdentityHashSet<LimeXMLDocument>();
        Set<String> keywords = QueryUtils.extractKeywords(query, true);

        for (String keyword : keywords) {

            Set<LimeXMLDocument> allMatchedDocsForKeyword =
                    getMatchingDocumentsForMetadata(metadataFields, keyword);

            // matches contains all common lime xml docs that match
            // all keywords in the query
            if (matches.size() == 0) {
                matches.addAll(allMatchedDocsForKeyword);
            } else {
                matches.retainAll(allMatchedDocsForKeyword);
            }

            // if no docs in common, there is no chance of a match
            if (matches.size() == 0) {
                return Collections.emptySet();
            }
        }
        return matches;
    }

    /**
     * Returns a Set of matching {@link LimeXMLDocument}s for a passed in Set of metadata fields and a search term
     * This method does not break the search term into keywords.
     * <p/>
     * NOTE: Caller of this method MUST SYNCHRONIZE on {@link #mainMap}
     *
     * @param metadataFields names of metadata fields to search for matches
     * @param searchTerm the query string to use for the search
     * @return LimeXMLDocuments
     */
    private Set<LimeXMLDocument> getMatchingDocumentsForMetadata(Set<String> metadataFields, String searchTerm) {

        // first, add all lime xml doc matches from all Lists in Iterator
        Set<LimeXMLDocument> matches = new IdentityHashSet<LimeXMLDocument>();

        for (String metadataFieldName : metadataFields) {

            // get StringTrie associated with metadata field
            StringTrie<List<LimeXMLDocument>> trie = trieMap.get(metadataFieldName);
            if(trie == null) {
                continue;
            }
            Iterator<List<LimeXMLDocument>> iter = trie.getPrefixedBy(searchTerm);
            while (iter.hasNext()) {
                matches.addAll(iter.next());
            }
        }
        return matches;
    }

    /**
     * Replaces the document in the map with a newer LimeXMLDocument.
     * @return the older document, which is being replaced. Can be null.
     */
    public LimeXMLDocument replaceDoc(FileDesc fd, LimeXMLDocument newDoc) {
        assert getSchemaURI().equals(newDoc.getSchemaURI());

        if(LOG.isTraceEnabled())
            LOG.trace("Replacing doc in FD (" + fd + ") with new doc (" + newDoc + ")");
        
        LimeXMLDocument oldDoc = null;
        synchronized(LOCK) {
            dirty = true;
            oldDoc = mainMap.put(new FileAndUrn(fd),newDoc);
            assert oldDoc != null : "attempted to replace doc that did not exist!!";
            removeKeywords(oldDoc);
            if(!isLWSDoc(newDoc))
                addKeywords(newDoc);
        }
       
        boolean replaced = fd.replaceLimeXMLDocument(oldDoc, newDoc);
        assert replaced;
        
        return oldDoc;
    }

    /**
     * Removes the document associated with this FileDesc
     * from this collection, as well as removing it from
     * the FileDesc.
     */
    public boolean removeDoc(FileDesc fd) {
        LimeXMLDocument val;
        synchronized(LOCK) {
            val = mainMap.remove(new FileAndUrn(fd));
            if(val != null)
                dirty = true;
        }
        
        if(val != null) {
            fd.removeLimeXMLDocument(val);
            removeKeywords(val);
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("removed: " + val);
        
        return val != null;
    }
    
    /**
     * Writes this media file to disk, using the XML in the doc.
     */
    public MetaDataState mediaFileToDisk(FileDesc fd, LimeXMLDocument doc) {
        MetaDataState writeState = MetaDataState.UNCHANGED;
        
        if(LOG.isDebugEnabled())
            LOG.debug("writing: " + fd.getFile() + " to disk.");
        
        // see if you need to change a hash for a file due to a write...
        // if so, we need to commit the metadata to disk....
        MetaDataWriter writer = getEditorIfNeeded(fd.getFile(), doc);
        if (writer != null)  {
            writeState = commitMetaData(fd, writer);
        }
        assert writeState != MetaDataState.INCORRECT_FILETYPE : "trying to write data to unwritable file of type " + FileUtils.getFileExtension(fd.getFile());

        return writeState;
    }

    /**
     * Determines whether or not this LimeXMLDocument can or should be
     * committed to disk to replace the ID3 tags in the audioFile.
     * If the ID3 tags in the file are the same as those in document,
     * this returns null (indicating no changes required).
     * @return An Editor to use when committing or null if nothing 
     *  should be edited.
     */
    private MetaDataWriter getEditorIfNeeded(File file, LimeXMLDocument doc) {
        // check if an editor exists for this file, if no editor exists
        //  just store data in xml repository only
        if(!metaDataFactory.get().containsEditor(file.getName()))
            return null;
        
        //get the editor for this file and populate it with the XML doc info
        MetaDataWriter newValues = new MetaDataWriter(file.getPath(), metaDataFactory.get());
        newValues.populate(doc);
        
        
        // try reading the file off of disk
        MetaData existing = null;
        try {
            existing = metaDataFactory.get().parse(file);
        } catch (IOException e) {
            return null;
        }
        
        //We are supposed to pick and chose the better set of tags
        if(!newValues.needsToUpdate(existing)) {
            LOG.debug("tag read from disk is same as XML doc.");
            return null;
        }
            
        // Commit using this Meta data editor ... 
        return newValues;
    }


    /**
     * Commits the changes to disk.
     * If anything was changed on disk, notifies the FileManager of a change.
     */
    private MetaDataState commitMetaData(FileDesc fd, MetaDataWriter editor) {
        //write to mp3 file...
        MetaDataState retVal = editor.commitMetaData();
        if(LOG.isDebugEnabled())
            LOG.debug("wrote data: " + retVal);
        // any error where the file wasn't changed ... 
        if( retVal == MetaDataState.FILE_DEFECTIVE ||
            retVal == MetaDataState.RW_ERROR ||
            retVal == MetaDataState.BAD_ID3 ||
            retVal == MetaDataState.INCORRECT_FILETYPE)
            return retVal;
            
        // if a FileDesc for this file exists, write out the changes to disk
        // and update the FileDesc in the FileManager
        List<LimeXMLDocument> currentXmlDocs = fd.getLimeXMLDocuments();
        if(metaDataFactory.get().containsEditor(fd.getFile().getName())) {
            try {
                  //TODO: Disk IO being performed here!!
                  LimeXMLDocument newAudioXmlDoc = metaDataReader.get().readDocument(fd.getFile());
                  LimeXMLDocument oldAudioXmlDoc = getAudioDoc(currentXmlDocs);
                  
                  if(oldAudioXmlDoc == null || !oldAudioXmlDoc.equals(newAudioXmlDoc)) {
                      currentXmlDocs = mergeAudioDocs(currentXmlDocs, oldAudioXmlDoc, newAudioXmlDoc);
                  }
              } catch (IOException e) {
                  // if we were unable to read this document,
                  // then simply add the file without metadata.
                  currentXmlDocs = Collections.emptyList();
              }
        }
        //Since the hash of the file has changed, the metadata pertaining 
        //to other schemas will be lost unless we update those tables
        //with the new hashValue. 
        //NOTE:This is the only time the hash will change-(mp3 and audio)
        library.get().fileChanged(fd.getFile(), currentXmlDocs);
        
        return retVal;
    }
    
    /**
     * Returns the audio LimeXMLDocument from this list if one exists, null otherwise.
     */
    private LimeXMLDocument getAudioDoc(List<LimeXMLDocument> allDocs) {
        LimeXMLDocument audioDoc = null;
        
        for (LimeXMLDocument doc : allDocs) {
            if (doc.getSchema().getSchemaURI().equals(LimeXMLNames.AUDIO_SCHEMA)) {
                audioDoc = doc;
                break;
            }
        }
        return audioDoc;
    }
    
    /**
     * Merges the a new Audio LimeXMLDocument with a list of LimeXMLDocuments. 
     * If the list didn't already contain and audio LimeXMLDocument, the new 
     * one is added, else the new Audio LimeXMLDocument replaces the old one 
     * in the list.
     */
    private List<LimeXMLDocument> mergeAudioDocs(List<LimeXMLDocument> allDocs, LimeXMLDocument oldAudioDoc, 
            LimeXMLDocument newAudioDoc) {
        List<LimeXMLDocument> retList = new ArrayList<LimeXMLDocument>();
        retList.addAll(allDocs);

        if (oldAudioDoc == null) {// nothing to resolve
            retList.add(newAudioDoc);
        } else {
            // OK. audioDoc exists, remove it
            retList.remove(oldAudioDoc);
    
            // now add the non-id3 tags from audioDoc to id3doc
            List<NameValue<String>> oldAudioList = oldAudioDoc.getOrderedNameValueList();
            List<NameValue<String>> newAudioList = newAudioDoc.getOrderedNameValueList();
            
            for (int i = 0; i < oldAudioList.size(); i++) {
                NameValue<String> nameVal = oldAudioList.get(i);
                if (AudioMetaData.isNonLimeAudioField(nameVal.getName()))
                    newAudioList.add(nameVal);
            }
            oldAudioDoc = limeXMLDocumentFactory.get().createLimeXMLDocument(newAudioList, LimeXMLNames.AUDIO_SCHEMA);
            retList.add(oldAudioDoc);
        }
        return retList;
    }
    
    
    /** Serializes the current map to disk. */
    public boolean writeMapToDisk() {
        boolean wrote = false;
        Map<FileAndUrn, String> xmlMap;
        synchronized(LOCK) {
            if(!dirty) {
                LOG.debug("Not writing because not dirty.");
                return true;
            }
            
            xmlMap = new HashMap<FileAndUrn, String>(mainMap.size());
            for(Map.Entry<FileAndUrn, LimeXMLDocument> entry : mainMap.entrySet())
                xmlMap.put(entry.getKey(), ((GenericXmlDocument)entry.getValue()).getXmlWithVersion());
            
            dirty = false;
        }

        File dataFile = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml3");
        File parent = dataFile.getParentFile();
        if(parent != null)
            parent.mkdirs();
                
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dataFile)));
            out.writeObject(xmlMap);
            out.flush();
            wrote = true;
        } catch(IOException ignored) {
            LOG.trace("Unable to write", ignored);
        } finally {
            IOUtils.close(out);
        }
        
        return wrote;
    }
    
    /** Reads the map off of the disk. */
    private Map<?, LimeXMLDocument> readMapFromDisk() {
        File v3File = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI) + ".sxml3");
        Map<?, LimeXMLDocument> map = null;
        if(v3File.exists()) {
            map = readVersion3File(v3File);
        } else {
            File v2File = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml2");
            if(v2File.exists()) {
                map = readVersion2File(v2File);
            } else {
                File v1File = new File(savedDocsDir, LimeXMLSchema.getDisplayString(schemaURI)+ ".sxml");
                if(v1File.exists()) {
                    map = readVersion1File(v1File);
                    v1File.delete();
                }
            }
        }
        
        if(map == null) {
            return Collections.emptyMap();
        } else {
            return map;
        }
    }
    
    /** Reads a file in the new format off disk. */
    private Map<FileAndUrn, LimeXMLDocument> readVersion3File(File input) {
        if(LOG.isDebugEnabled())
            LOG.debug("Reading new format from file: " + input);
        
        ObjectInputStream in = null;
        Map<FileAndUrn, String> read = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input)));
            read = GenericsUtils.scanForMap(in.readObject(), FileAndUrn.class, String.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable t) {
            LOG.error("Unable to read LimeXMLCollection", t);
        } finally {
            IOUtils.close(in);
        }
        
        if(read == null)
            read = Collections.emptyMap();
        
        Map<FileAndUrn, LimeXMLDocument> docMap = new HashMap<FileAndUrn, LimeXMLDocument>(read.size());
        for(Map.Entry<FileAndUrn, String> entry : read.entrySet()) {
            try {
                docMap.put(entry.getKey(), limeXMLDocumentFactory.get().createLimeXMLDocument(entry.getValue()));
            } catch(IOException ignored) {
                LOG.warn("Error creating document for: " + entry.getValue(), ignored);
            } catch(SchemaNotFoundException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            } catch (SAXException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            }
        }
        
        return docMap;
    }    
    
    /** Reads a file in the new format off disk. */
    private Map<URN, LimeXMLDocument> readVersion2File(File input) {
        if(LOG.isDebugEnabled())
            LOG.debug("Reading new format from file: " + input);
        
        ObjectInputStream in = null;
        Map<URN, String> read = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input)));
            read = GenericsUtils.scanForMap(in.readObject(), URN.class, String.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable t) {
            LOG.error("Unable to read LimeXMLCollection", t);
        } finally {
            IOUtils.close(in);
        }
        
        if(read == null)
            read = Collections.emptyMap();
        
        Map<URN, LimeXMLDocument> docMap = new HashMap<URN, LimeXMLDocument>(read.size());
        for(Map.Entry<URN, String> entry : read.entrySet()) {
            try {
                docMap.put(entry.getKey(), limeXMLDocumentFactory.get().createLimeXMLDocument(entry.getValue()));
            } catch(IOException ignored) {
                LOG.warn("Error creating document for: " + entry.getValue(), ignored);
            } catch(SchemaNotFoundException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            } catch (SAXException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            }
        }
        
        return docMap;
    }
    
    /** Reads a file in the old format off disk. */
    private Map<URN, LimeXMLDocument> readVersion1File(File input) {
        if(LOG.isDebugEnabled())
            LOG.debug("Reading old format from file: " + input);
        ConverterObjectInputStream in = null;
        Map<URN, SerialXml> read = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(input)));
            in.addLookup("com.limegroup.gnutella.xml.LimeXMLDocument", SerialXml.class.getName());
            read = GenericsUtils.scanForMap(in.readObject(), URN.class, SerialXml.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable t) {
            LOG.error("Unable to read LimeXMLCollection", t);
        } finally {
            IOUtils.close(in);
        }
        
        if(read == null)
            read = Collections.emptyMap();
        
        Map<URN, LimeXMLDocument> docMap = new HashMap<URN, LimeXMLDocument>(read.size());
        for(Map.Entry<URN, SerialXml> entry : read.entrySet()) {
            try {
                docMap.put(entry.getKey(), limeXMLDocumentFactory.get().createLimeXMLDocument(entry.getValue().getXml(true)));
            } catch(IOException ignored) {
                LOG.warn("Error creating document for: " + entry.getValue(), ignored);
            } catch(SchemaNotFoundException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            } catch (SAXException ignored) {
                LOG.warn("Error creating document: " + entry.getValue(), ignored);
            }
        }
        
        return docMap;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    private static class FileAndUrn implements Serializable {        
        private static final long serialVersionUID = 6914168193085067395L;
        
        private final File file;
        private final URN urn;
        
        public FileAndUrn(FileDesc fd) {
            this.file = fd.getFile();
            this.urn = fd.getSHA1Urn();
        }
        
        @Override
        public int hashCode() {
            return file.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            } else if(obj instanceof FileAndUrn) {
                FileAndUrn o2 = (FileAndUrn)obj;
                return Objects.equalOrNull(urn, o2.urn) &&
                       o2.file.equals(file);
            } else {
                return false;
            }
        }
    }
}
