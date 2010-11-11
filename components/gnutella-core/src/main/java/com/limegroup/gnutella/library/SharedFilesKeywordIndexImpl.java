package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.limewire.collection.Function;
import org.limewire.collection.IdentityHashSet;
import org.limewire.collection.IntSet;
import org.limewire.collection.MultiIterator;
import org.limewire.collection.StringTrie;
import org.limewire.core.api.Category;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.QueryCategoryFilterer;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

// TODO split this up further and remove query and response from here,
// or introduce a generic indexing class that can be used
@EagerSingleton
class SharedFilesKeywordIndexImpl implements SharedFilesKeywordIndex {

    /**
     * A trie mapping keywords in complete filenames to the indices in _files.
     * Keywords are the tokens when the filename is tokenized with the
     * characters from DELIMITERS as delimiters.
     * 
     * IncompleteFile keywords are NOT stored.
     * 
     * INVARIANT: For all keys k in _keywordTrie, for all i in the IntSet
     * _keywordTrie.get(k), _files[i]._path.substring(k)!=-1. Likewise for all
     * i, for all k in _files[i]._path where _files[i] is not an
     * IncompleteFileDesc, _keywordTrie.get(k) contains i.
     * 
     * Not threadsafe, hold lock on field.
     */
    private final StringTrie<IntSet> keywordTrie = new StringTrie<IntSet>(true);

    /**
     * A trie mapping keywords in complete filenames to the indices in _files.
     * Contains ONLY incomplete keywords.
     * 
     * Not threadsafe, hold lock on field.
     */
    private final StringTrie<IntSet> incompleteKeywordTrieV2 = new StringTrie<IntSet>(true);

    private final Provider<CreationTimeCache> creationTimeCache;

    private final Provider<ResponseFactory> responseFactory;

    private final Library library;
    private final FileView gnutellaFileView;
    private final FileView incompleteFileView;

    private final Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper;

    private final ActivityCallback activityCallback;

    private final Provider<LimeXMLSchemaRepository> schemaRepository;
    private final QueryCategoryFilterer mediaTypeAggregator;

    @Inject
    public SharedFilesKeywordIndexImpl(Library library,
            Provider<CreationTimeCache> creationTimeCache,
            Provider<ResponseFactory> responseFactory,
            Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper,
            ActivityCallback activityCallback, Provider<LimeXMLSchemaRepository> schemaRepository,
            @GnutellaFiles FileView gnutellaFileView,
            @IncompleteFiles FileView incompleteFileView,
            QueryCategoryFilterer mediaTypeAggregator) {
        this.library = library;
        this.creationTimeCache = creationTimeCache;
        this.responseFactory = responseFactory;
        this.schemaReplyCollectionMapper = schemaReplyCollectionMapper;
        this.activityCallback = activityCallback;
        this.schemaRepository = schemaRepository;
        this.incompleteFileView = incompleteFileView;
        this.gnutellaFileView = gnutellaFileView;
        this.mediaTypeAggregator = mediaTypeAggregator;
    }
    
    @Inject void register(ServiceRegistry registry, final ListenerSupport<FileDescChangeEvent> fileDescSupport) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return "P2P Network Keyword Library";
            }
            @Override
            public void initialize() {
                fileDescSupport.addListener(new EventListener<FileDescChangeEvent>() {
                    @Override
                    public void handleEvent(FileDescChangeEvent event) {
                        handleFileDescEvent(event);
                    }
                });                
                library.addManagedListStatusListener(new EventListener<LibraryStatusEvent>() {
                    @Override
                    public void handleEvent(LibraryStatusEvent event) {
                        handleManagedListStatusEvent(event);
                    }
                });
                gnutellaFileView.addListener(new EventListener<FileViewChangeEvent>() {
                    @Override
                    public void handleEvent(FileViewChangeEvent event) {
                        handleFileListEvent(event, true);
                    }
                });
                incompleteFileView.addListener(new EventListener<FileViewChangeEvent>() {
                    @Override
                    public void handleEvent(FileViewChangeEvent event) {
                        handleFileListEvent(event, false);
                    }
                });
            }
            @Override
            public void start() {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void stop() {
                // TODO Auto-generated method stub
                
            }
        });
    }

    @Override
    public Response[] query(QueryRequest request) {
        Set<Response> responses = QueryProcessor.processQuery(request, this);
        incrementHitCount(responses);
        return responses.toArray(new Response[responses.size()]);
    }

    /**
     * Increment hit counts for files which matched a query
     *
     * @param matches set of Response objects
     */
    private void incrementHitCount(Set<Response> matches) {
        for (Response resp : matches) {
            long index = resp.getIndex();

            // casting to int because Response originally created with positive int
            FileDesc desc = library.getFileDescForIndex((int)index);
            if(desc != null) {
                desc.incrementHitCount();
            }
        }
    }

    private Set<Response> queryMetaData(QueryRequest request) {
        Set<LimeXMLDocument> documents = Collections.emptySet();
        LimeXMLDocument doc = request.getRichQuery();
        if (doc != null) {
            documents = queryMetaDataWithRequestXml(doc);
        } else if (SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.getValue()) {
            // no xml query, look if any xml field of the matching mediatype
            // starts with the keywords of the request
            documents = queryMetaDataWithPlaintext(request);
        }
        return createResponses(documents, request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.FileManager#query(com.limegroup.gnutella.messages.QueryRequest)
     */
    private Set<Response> queryFileNames(QueryRequest request) {
        String str = request.getQuery();
        boolean includeXML = request.shouldIncludeXMLInResponse();

        // Normal case: query the index to find all matches. TODO: this
        // sometimes returns more results (>255) than we actually send out.
        // That's wasted work.
        // Trie requires that getPrefixedBy(String, int, int) passes
        // an already case-changed string. Both search & urnSearch
        // do this kind of match, so we canonicalize the case for them.
        str = keywordTrie.canonicalCase(str);
        IntSet matches = search(str, null, request.desiresPartialResults());
        if (request.getQueryUrns().size() > 0)
            matches = urnSearch(request.getQueryUrns(), matches);

        if (matches == null)
            return Collections.emptySet();

        Set<Response> responses = new HashSet<Response>();
        Predicate<String> filter = mediaTypeAggregator.getPredicateForQuery(request);
        LimeXMLDocument doc = request.getRichQuery();
        boolean includeNMs1Urn = request.desiresNMS1Urn();

        // Iterate through our hit indices to create a list of results.
        for (IntSet.IntSetIterator iter = matches.iterator(); iter.hasNext();) {
            int i = iter.next();
            FileDesc desc = gnutellaFileView.getFileDescForIndex(i);
            if(desc == null) {
                desc = incompleteFileView.getFileDescForIndex(i);
            }

            //desc can be null if items were removed after the IntSet matches were built
            if(desc != null) {
                if (!filter.apply(FileUtils.getFileExtension(desc.getFileName()))) {
                    continue;
                }

                activityCallback.handleSharedFileUpdate(desc.getFile());
    
                Response resp = responseFactory.get().createResponse(desc, includeNMs1Urn);
                if (includeXML) {
                    if (doc != null && resp.getDocument() != null && !isValidXMLMatch(resp, doc))
                        continue;
                } else {
                	//remove xml doc to save bandwidth
                    resp.setDocument(null);
                }
                responses.add(resp);
            }
        }
        if (responses.size() == 0)
            return Collections.emptySet();

        return responses;
    }

    private static boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return LimeXMLUtils.match(r.getDocument(), doc, true);
    }

    /**
     * Find all files with matching full URNs
     */
    private IntSet urnSearch(Iterable<URN> urnsIter, IntSet priors) {
        IntSet ret = priors;
        for (URN urn : urnsIter) {
            List<FileDesc> fds = gnutellaFileView.getFileDescsMatching(urn);
            for(FileDesc fd : fds) {
                if(ret == null) {
                    ret = new IntSet();
                }
                ret.add(fd.getIndex());
            }
        }
        return ret;
    }

    /**
     * Responds to a what is new request.
     */
    private Set<Response> queryWhatsNew(QueryRequest request) {
        boolean includeXML = request.shouldIncludeXMLInResponse();
        boolean includeNMs1Urn = request.desiresNMS1Urn();

        // see if there are any files to send....
        // NOTE: we only request up to 3 urns. we don't need to worry
        // about partial files because we don't add them to the cache.
        // NOTE: this doesn't return Store files. getNewestUrns only
        // returns the top 3 shared files
        Collection<URN> urnList = creationTimeCache.get().getFiles(request, 3);
        if (urnList.size() == 0)
            return Collections.emptySet();

        // get the appropriate responses
        Set<Response> resps = new HashSet<Response>(urnList.size());
        for (URN urn : urnList) {
            FileDesc desc = gnutellaFileView.getFileDesc(urn);

            // should never happen since we don't add times for IFDs and
            // we clear removed files...
            if ((desc == null) || (desc instanceof IncompleteFileDesc))
                throw new RuntimeException("Bad Rep - No IFDs allowed!");

            // Formulate the response
            Response r = responseFactory.get().createResponse(desc, includeNMs1Urn);
            if(!includeXML) {
                r.setDocument(null);
            }

            // Cache it
            resps.add(r);
        }
        return resps;
    }

    private void clear(boolean complete) {
        if(complete) {
            keywordTrie.clear();
        } else {
            incompleteKeywordTrieV2.clear();
        }
    }
    
    private void handleFileListEvent(FileViewChangeEvent evt, boolean complete) {
        switch(evt.getType()) {
        case FILE_ADDED:
            addFileDesc(evt.getFileDesc(), complete);
            break;
        case FILE_CHANGED:
            removeFileDesc(evt.getOldValue(), complete);
            addFileDesc(evt.getFileDesc(), complete);
            break;
        case FILE_REMOVED:
            removeFileDesc(evt.getFileDesc(), complete);
            break;
        case FILES_CLEARED:
            clear(complete);
            break;
        case FILE_META_CHANGED:
            // purposely do nothing!
            // meta change on a view means metadata changed,
            // and we don't process metadata here!
            break;
        }
    }

    private void handleManagedListStatusEvent(LibraryStatusEvent evt) {
        switch (evt.getType()) {
        case LOAD_COMPLETE:
            trim();
            break;
        }
    }
    
    private void handleFileDescEvent(FileDescChangeEvent evt) {
        FileDesc fd = evt.getSource();
        switch(evt.getType()) {
        case TT_ROOT_ADDED:
              if(fd instanceof IncompleteFileDesc) {
                  IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                  if (SharingSettings.ALLOW_PARTIAL_SHARING.getValue() &&
                          SharingSettings.LOAD_PARTIAL_KEYWORDS.getValue() &&
                          ifd.hasUrnsAndPartialData()) {
                      addFileDesc(fd, false);
                  }
              }
          break;
        }
    }

    private void removeFileDesc(FileDesc fileDesc, boolean complete) {
        if(complete) {
            removeKeywords(keywordTrie, fileDesc);
        } else {
            removeKeywords(incompleteKeywordTrieV2, fileDesc);
        }
    }

    private void addFileDesc(FileDesc fileDesc, boolean complete) {
        if(!complete) {
            boolean indexIncompleteFiles = SharingSettings.ALLOW_PARTIAL_SHARING.getValue()
                                                && SharingSettings.LOAD_PARTIAL_KEYWORDS.getValue();
            IncompleteFileDesc ifd = (IncompleteFileDesc) fileDesc;
            if (indexIncompleteFiles && ifd.hasUrnsAndPartialData()) {
                loadKeywords(incompleteKeywordTrieV2, fileDesc);
            }
        } else {
            loadKeywords(keywordTrie, fileDesc);
        }
    }

    /**
     * @param trie to update
     * @param fd to load keywords from
     */
    private void loadKeywords(StringTrie<IntSet> trie, FileDesc fd) {
        // Index the filename. For each keyword...
        String[] keywords = extractKeywords(fd);

        for (String keyword : keywords) {
            synchronized (trie) {
                // Ensure the _keywordTrie has a set of indices associated with
                // keyword.
                IntSet indices = trie.get(keyword);
                if (indices == null) {
                    indices = new IntSet();
                    trie.add(keyword, indices);
                }
                // Add fileIndex to the set.
                indices.add(fd.getIndex());
            }
        }
    }

    private void removeKeywords(StringTrie<IntSet> trie, FileDesc fd) {
        // Remove references to this from index.
        String[] keywords = extractKeywords(fd);
        for (String keyword : keywords) {
            synchronized (trie) {
                IntSet indices = trie.get(keyword);
                if (indices != null) {
                    indices.remove(fd.getIndex());
                    if (indices.size() == 0)
                        trie.remove(keyword);
                }
            }
        }
    }

    /**
     * Returns a set of indices of files matching <code>query</code>, or null
     * if there are no matches. Subclasses may override to provide different
     * notions of matching. The caller of this method must not mutate the
     * returned value.
     */
    protected IntSet search(String query, IntSet priors, boolean partial) {
        // As an optimization, we lazily allocate all sets in case there are no
        // matches. TODO2: we can avoid allocating sets when getPrefixedBy
        // returns an iterator of one element and there is only one keyword.
        IntSet ret = priors;

        // For each keyword in the query.... (Note that we avoid calling
        // StringUtils.split and take advantage of Trie's offset/limit feature.)
        for (int i = 0; i < query.length();) {
            if (QueryUtils.isDelimiter(query.charAt(i))) {
                i++;
                continue;
            }
            int j;
            for (j = i + 1; j < query.length(); j++) {
                if (QueryUtils.isDelimiter(query.charAt(j)))
                    break;
            }

            // Search for keyword, i.e., keywords[i...j-1].
            Iterator<IntSet> iter;
            synchronized (keywordTrie) {
                iter = keywordTrie.getPrefixedBy(query, i, j);
            }
            if (SharingSettings.ALLOW_PARTIAL_SHARING.getValue()
                    && SharingSettings.ALLOW_PARTIAL_RESPONSES.getValue() && partial) {
                Iterator<IntSet> incompleteIndices;
                synchronized (incompleteKeywordTrieV2) {
                    incompleteIndices = incompleteKeywordTrieV2.getPrefixedBy(query, i, j);
                }
                iter = new MultiIterator<IntSet>(iter, incompleteIndices);
            }

            synchronized (keywordTrie) {
                synchronized (incompleteKeywordTrieV2) {
                    if (iter.hasNext()) {
                        // Got match. Union contents of the iterator and store
                        // in
                        // matches. As an optimization, if this is the only
                        // keyword and
                        // there is only one set returned, return that set
                        // without
                        // copying.
                        IntSet matches = null;
                        while (iter.hasNext()) {
                            IntSet s = iter.next();
                            if (matches == null) {
                                if (i == 0 && j == query.length() && !(iter.hasNext()))
                                    return s;
                                matches = new IntSet();
                            }
                            matches.addAll(s);
                        }

                        // Intersect matches with ret. If ret isn't allocated,
                        // initialize to matches.
                        if (ret == null)
                            ret = matches;
                        else
                            ret.retainAll(matches);
                    } else {
                        // No match. Optimization: no matches for keyword =>
                        // failure
                        return null;
                    }

                    // Optimization: no matches after intersect => failure
                    if (ret.size() == 0)
                        return null;
                    i = j;
                }
            }
        }
        if (ret == null || ret.size() == 0)
            return null;
        return ret;
    }

    /**
     * Utility method to perform standardized keyword extraction for the given
     * <tt>FileDesc</tt>. This handles extracting keywords according to
     * locale-specific rules.
     * 
     * @param fd the <tt>FileDesc</tt> containing a file system path with
     *        keywords to extact
     * @return an array of keyword strings for the given file
     */
    private static String[] extractKeywords(FileDesc fd) {
        return StringUtils.split(I18NConvert.instance().getNorm(fd.getPath()),
                QueryUtils.DELIMITERS);
    }

    /**
     * Ensures that this's index takes the minimum amount of space. Only affects
     * performance, not correctness; hence no modifies clause.
     */
    private void trim() {
        for (StringTrie<IntSet> trie : new StringTrie[] { keywordTrie, incompleteKeywordTrieV2 }) {
            synchronized (trie) {
                trie.trim(new Function<IntSet, IntSet>() {
                    public IntSet apply(IntSet intSet) {
                        intSet.trim();
                        return intSet;
                    }
                });
            }
        }
    }

    /**
     * Returns an array of Responses that correspond to documents that have a
     * match given query document.
     */
    private Set<LimeXMLDocument> queryMetaDataWithRequestXml(LimeXMLDocument queryDoc) {
        String schema = queryDoc.getSchemaURI();
        LimeXMLReplyCollection replyCol = schemaReplyCollectionMapper.get().getReplyCollection(
                schema);
        if (replyCol == null)// no matching reply collection for schema
            return Collections.emptySet();

        return replyCol.getMatchingDocuments(queryDoc);
    }

    /**
     * Queries metadata of shared files. This will query certain media types,
     * depending on what is specified in the request.
     * 
     * If not specified in request, query all metadata.
     * 
     * @param request determines what to search, and which metadata is searched.
     * @return
     */
    private Set<LimeXMLDocument> queryMetaDataWithPlaintext(QueryRequest request) {

        Collection<LimeXMLReplyCollection> schemas = getReplyCollections(request);

        Set<LimeXMLDocument> documents = new IdentityHashSet<LimeXMLDocument>();
        for (LimeXMLReplyCollection schemaCol : schemas) {
            documents.addAll(schemaCol.getMatchingDocuments(request.getQuery()));
        }
        return documents;
    }

    private Collection<LimeXMLReplyCollection> getReplyCollections(QueryRequest request) {
        List<Category> categoriesRequested = mediaTypeAggregator.getRequestedCategories(request);
        SchemaReplyCollectionMapper mapper = schemaReplyCollectionMapper.get();
        if (categoriesRequested == null) {
            return mapper.getCollections();
        }
        List<LimeXMLReplyCollection> collections = new ArrayList<LimeXMLReplyCollection>(categoriesRequested.size());
        for (Category category : categoriesRequested) {

            // get schema uri from media type
            LimeXMLReplyCollection col = mapper.getReplyCollection(
                    getSchemaUriFromMimeType(category.getSchemaName()));

            if (col != null) {
                collections.add(col);
            }
        }
        return collections;
    }

    private String getSchemaUriFromMimeType(String mimeType) {
        Collection<LimeXMLSchema> schemas = schemaRepository.get().getAvailableSchemas();
        for (LimeXMLSchema schema : schemas) {
            if (schema.getDescription().equals(mimeType)) {
                return schema.getSchemaURI();
            }
        }
        return "";
    }

    private Set<Response> createResponses(Set<LimeXMLDocument> documents, QueryRequest request) {
        Set<Response> responses = new HashSet<Response>(documents.size());
        boolean includeNMs1Urn = request.desiresNMS1Urn();

        for (LimeXMLDocument currDoc : documents) {
            File file = currDoc.getIdentifier();// returns null if none
            Response res = null;
            assert(file != null);
            FileDesc fd = gnutellaFileView.getFileDesc(file);
            if (fd == null || fd.getSHA1Urn() == null) {
                // fd == null is bad -- would mean MetaFileManager is out of
                // sync.
                // fd incomplete should never happen, but apparently is
                // somehow...
                // fd is store file, shouldn't be returning query hits for
                // it then..
                continue;
            }

            // we found a file with the right name
            res = responseFactory.get().createResponse(fd, includeNMs1Urn);
            res.setDocument(null);
            activityCallback.handleSharedFileUpdate(fd.getFile());
            res.setDocument(currDoc);
            responses.add(res);
        }
        return responses;
    }

    /**
     * Enum type and context object to better organize the various steps that go
     * into processing a query
     * <p>
     * Each QueryProcessor enum represents 1 step in processing a query.
     * <p>
     * Purposes of the context object are:
     * <p>
     * 1. Keep track of whether query processing is done<br>
     * 2. Keep track of Responses found during searches so far.<br>
     * <p>
     * What query processing does:
     * <p>
     * For each QueryProcessor Enum type,
     * <p>
     * 1. Check context object to see if query processing is done. If so, stop
     * processing 2. Should this particular query type be done? 3. If it should,
     * perform the query, and add the Response objects in context object 4. If
     * query processing done, set status in context object
     * 
     */
    private enum QueryProcessor {

        /**
         * "What is new" search. Get up to 3 of your "youngest" files.
         */
        WHATS_NEW {
            @Override
            void processQueryStage(QueryRequest request, QueryProcessingContext context,
                    SharedFilesKeywordIndexImpl keywordIndex) {
                Set<Response> responses = keywordIndex.queryWhatsNew(request);
                context.addQueryResponses(responses);
                context.setFinishedProcessing();
            }

            @Override
            boolean shouldProcess(QueryRequest request) {
                return request.isWhatIsNewRequest();
            }
        },

        /**
         * Special case: return everything for Clip2 indexing query (" ") and
         * browse queries ("*.*"). If these messages had initial TTLs too high,
         * StandardMessageRouter will clip the number of results sent on the
         * network. Note that some initial TTLs are filterd by GreedyQuery
         * before they ever reach this point.
         */
        SPECIAL_CASE_EMPTY_RESPONSE {
            @Override
            void processQueryStage(QueryRequest request, QueryProcessingContext context,
                    SharedFilesKeywordIndexImpl keywordIndex) {
                context.setFinishedProcessing();
            }

            @Override
            boolean shouldProcess(QueryRequest request) {
                String str = request.getQuery();
                return str.equals(QueryRequest.INDEXING_QUERY)
                        || str.equals(QueryRequest.BROWSE_QUERY);
            }
        },

        /**
         * Search file name
         */
        FILE_SEARCH {
            @Override
            void processQueryStage(QueryRequest request, QueryProcessingContext context,
                    SharedFilesKeywordIndexImpl keywordIndex) {
                Set<Response> responses = keywordIndex.queryFileNames(request);
                context.addQueryResponses(responses);
            }

            @Override
            boolean shouldProcess(QueryRequest request) {
                return true;
            }

        },

        /**
         * Search meta data
         */
        METADATA_SEARCH {
            @Override
            void processQueryStage(QueryRequest request, QueryProcessingContext context,
                    SharedFilesKeywordIndexImpl keywordIndex) {
                Set<Response> responses = keywordIndex.queryMetaData(request);
                context.addQueryResponses(responses);
                context.setFinishedProcessing();
            }

            @Override
            boolean shouldProcess(QueryRequest request) {
                return request.shouldIncludeXMLInResponse();
            }
        };

        abstract void processQueryStage(QueryRequest request, QueryProcessingContext context,
                SharedFilesKeywordIndexImpl keywordIndex);

        abstract boolean shouldProcess(QueryRequest request);

        public static Set<Response> processQuery(QueryRequest request,
                SharedFilesKeywordIndexImpl keywordIndex) {

            QueryProcessingContext contextObj = new QueryProcessingContext();
            for (QueryProcessor queryProcessor : QueryProcessor.values()) {
                if (queryProcessor.shouldProcess(request)) {
                    queryProcessor.processQueryStage(request, contextObj, keywordIndex);

                    if (contextObj.isFinishedProcessing()) {
                        break;
                    }
                }
            }
            return contextObj.getResponses();
        }
    }

    private static class QueryProcessingContext {

        private boolean isTerminal;

        private final Set<Response> responses;

        QueryProcessingContext() {
            this.responses = new HashSet<Response>();
            this.isTerminal = false;
        }

        boolean isFinishedProcessing() {
            return isTerminal;
        }

        Set<Response> getResponses() {
            return responses;
        }

        void addQueryResponses(Set<Response> responses) {
            this.responses.addAll(responses);
        }

        void setFinishedProcessing() {
            isTerminal = true;
        }
    }
}
