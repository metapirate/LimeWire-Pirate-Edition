package com.limegroup.gnutella.filters;

import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.limewire.core.api.search.SearchResult;

import com.google.common.collect.ImmutableList;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.response.ResponseFilter;
import com.limegroup.gnutella.filters.response.SearchResultFilter;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/** 
 * A filter that blocks queries and responses matching certain banned phrases.
 */
public class PhraseFilter implements SpamFilter, ResponseFilter, SearchResultFilter {
    
    /** INVARIANT: strings in ban contain only lowercase */
    private final List<String> ban;

    PhraseFilter() {
        ban = createDefaultList();
    }
    
    PhraseFilter(String... phrases) {
        ban = ImmutableList.of(phrases);
    }
    
    private List<String> createDefaultList() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        // ADD PHRASES HERE
        return builder.build();
    }

    String canonical(String word) {
        return word.toLowerCase(Locale.US).intern();
    }

    @Override // SpamFilter
    public boolean allow(Message m) {
        if (m instanceof QueryRequest) 
            return !isBanned(((QueryRequest)m).getQuery());
        else
            return true;
    }

    @Override // ResponseFilter
    public boolean allow(QueryReply qr, Response response) {
        if(isBanned(response.getName())) {
            return false;
        } else {
            LimeXMLDocument doc = response.getDocument();
            return doc == null || allowDoc(doc);
        }
    }

    @Override
    public boolean allow(SearchResult result, LimeXMLDocument document) {
        if(isBanned(result.getFileNameWithoutExtension())) {
            return false;
        } else {
            return document == null || allowDoc(document);
        }
    }
    
    /** Returns true if input matches any of the banned phrases. */
    public boolean isBanned(String input) {
        String canonical = input.toLowerCase(Locale.US);
        for(String word : ban) {
            int idx = canonical.indexOf(word);
            if(idx != -1
              && (idx == 0 || canonical.charAt(idx - 1) == ' ') // start of word boundary
              && (word.length() + idx == canonical.length() || canonical.charAt(word.length() + idx) == ' ')) // end of word boundary
            {
                return true;
            }
        }
        return false;
    }
    
    /** Returns true if none of the filters matched. */
    private boolean allowDoc(LimeXMLDocument doc) {
        for(Entry<String, String> entry : doc.getNameValueSet()) {
            if(isBanned(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
