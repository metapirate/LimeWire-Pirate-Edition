package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.collection.ForgetfulHashMap;
import org.limewire.collection.MultiIterable;
import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Records information about queries so that responses can be validated later.
 * Typical use is to call record(..) on an outgoing query request, and
 * score/matchesType/isMandragoreWorm on each incoming response.  
 */
@Singleton
public class ResponseVerifierImpl implements ResponseVerifier {
    
    private static final Log LOG = LogFactory.getLog(ResponseVerifierImpl.class);
    
    private static class RequestData {
        /** The original query. */
        final String query;
        /** The rich query. */
//        final LimeXMLDocument richQuery;
        /** The keywords of the original query, lowercased. */
        final List<String> queryWords;
        /** The type of the original query. */
        final SearchCategory type;
        /** Whether this is a what is new query */
        final boolean whatIsNew;

//        RequestData(String query, SearchCategory type) {
//            this(query, null, type, false);
//        }

        RequestData(String query, LimeXMLDocument richQuery, SearchCategory type, boolean whatIsNew) {
            this.query=query;
//            this.richQuery=richQuery;
            this.queryWords=getSearchTerms(query, richQuery);
            this.type=type;
            this.whatIsNew = whatIsNew;
        }

//        public boolean xmlQuery() {
//            return richQuery != null;
//        }

    }

    /**
     *  A mapping from GUIDs to the words of the search made with that GUID.
     */
    private ForgetfulHashMap<GUID, RequestData> mapper = new ForgetfulHashMap<GUID, RequestData>(15);
    /** The characters to use in stripping apart queries. */
    private static final String DELIMITERS="+ ";
    /** The size of a Mandragore worm response, i.e., 8KB. */
    private static final long Mandragore_SIZE=8*1024l;
    
    private final CategoryManager categoryManager;
    
    @Inject
    public ResponseVerifierImpl(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    /** Same as record(qr, null). */
    public synchronized void record(QueryRequest qr) {
        record(qr, null);
    }

    /**
     *  @modifies this
     *  @effects memorizes the query string for qr; this will be used to score
     *   responses later.  If type!=null, also memorizes that qr was for the given
     *   media type; otherwise, this is assumed to be for any type.
     */
    public synchronized void record(QueryRequest qr, SearchCategory type){
        byte[] guid = qr.getGUID();
        mapper.put(new GUID(guid),new RequestData(qr.getQuery(), 
                                                  qr.getRichQuery(),
                                                  type,
                                                  qr.isWhatIsNewRequest()));
    }

    public synchronized boolean matchesQuery(byte [] guid, Response response) {
        RequestData data = mapper.get(new GUID(guid));
        if (data == null || data.queryWords == null) {
            LOG.debugf("no match: {0}", response);
            return false;
        }
        
        if (data.whatIsNew) 
            return true;
        
        int minGood = FilterSettings.MIN_MATCHING_WORDS.getValue();
        if (score(data.queryWords, response.getName()) > minGood)
            return true;

        LimeXMLDocument doc = response.getDocument();
        if (doc != null) {
            for(String xmlWord : new MultiIterable<String>(doc.getKeyWords(), doc.getKeyWordsIndivisible()))
                if (score(data.queryWords,xmlWord) > minGood )
                    return true;
        }
        
        return false;
    }
    
    /**
     * Returns the score of the given response compared to the given query.
     *
     * @param query the query keyword string sent
     * @param richQuery the XML metadata string sent, or null if none
     * @param response the response to score, converted to RemoteFileDesc
     * @return the percentage of query keywords (0-100) matching
     */
    public static int score(String query, 
                            LimeXMLDocument richQuery, 
                            RemoteFileDesc response) {
        return score(getSearchTerms(query, richQuery), response.getFileName());
    }

    /** Actual implementation of scoring; called from both public versions. 
     *  @param queryWords the tokenized query keywords
     *  @param filename the name of the response*/
    public static int score(List<String> queryWords, String filename) {
        int numMatchingWords=0;
        int numQueryWords=queryWords.size();
        if (numQueryWords==0)
            return 100; // avoid divide-by-zero errors below

        //Count the number of regular expressions from the query that
        //match the result's name.  Ignore case in comparison.
        for (int i=0; i<numQueryWords; i++) {
            String pattern = queryWords.get(i);
            if (StringUtils.contains(filename, pattern, true)) {
                numMatchingWords++;
                continue;
            }
        }

        return (int)(100.0f * ((float)numMatchingWords/numQueryWords));
    }

    /**
     * Returns true if response has the same media type as the
     * corresponding query request the given GUID.  In the rare case
     * that guid is not known (because this' buffers overflowed),
     * conservatively returns true.
     */
    public boolean matchesType(byte[] guid, Response response) {
        RequestData request = mapper.get(new GUID(guid));
        if (request == null || request.type==null)
            return true;
        String reply = response.getName();
        Category category = request.type.getCategory();
        if(category != null) {
            LOG.debugf("Matching for type: {0} - {1}", category, response);
            return categoryManager.getCategoryForFilename(reply) == category;
        } else {
            return true;
        } 
    }

    /**
     * Returns true if the given response is an instance of the Mandragore
     * Worm.  This worm responds to the query "x" with a 8KB file named
     * "x.exe".  In the rare case that the query for guid can't be found
     * returns false.
     */
    public boolean isMandragoreWorm(byte[] guid, Response response) {
        RequestData request = mapper.get(new GUID(guid));
        if (request == null)
            return false;
        return response.getSize()==Mandragore_SIZE 
                   && response.getName().equals(request.query+".exe");
    }
    
    /**
     * Returns the query string corresponding to the given query GUID, or
     * null if the GUID is unknown or has expired from the cache.
     */
    @Override
    public synchronized String getQueryString(byte[] guid) {
        RequestData request = mapper.get(new GUID(guid));
        if(request == null)
            return null;
        return request.query;
    }

    @Override
    public String toString() {
        return mapper.toString();
    }

    private static List<String> getSearchTerms(String query,
                                           LimeXMLDocument richQuery) {
        String[] terms = null;
        // combine xml and standard keywords
        // ---------------------------------------
        Set<String> qWords=new HashSet<String>();
        terms = StringUtils.split(query.toLowerCase(), DELIMITERS);
        // add the standard query words..
        for (int i = 0; i < terms.length; i++)
            qWords.add(terms[i]);

        List<String> xmlWords=null;
        if (richQuery != null) {
            xmlWords = richQuery.getKeyWords();
            final int size = xmlWords.size();
            // add a lowercase version of the xml words...
            for (int i = 0; i < size; i++) {
                String currWord = xmlWords.remove(0);
                qWords.add(currWord.toLowerCase());
            }
        }
        
        return Collections.unmodifiableList(new ArrayList<String>(qWords));
    }
}

