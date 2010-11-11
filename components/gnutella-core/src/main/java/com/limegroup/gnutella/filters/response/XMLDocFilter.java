package com.limegroup.gnutella.filters.response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.KeywordFilter;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.settings.FilterSettings;

public class XMLDocFilter extends KeywordFilter {

    private final Map<String, String> disallowedFieldValues;
    private final Map<String, List<String>> disallowedExactFieldValues;

    XMLDocFilter() {
        this(FilterSettings.FILTER_ADULT.getValue(), true);
    }

    XMLDocFilter(boolean banAdult, boolean banPersonal) {
        super(banAdult, banPersonal);
        if(banAdult) {
            ImmutableMap.Builder<String, String> fields =
                new ImmutableMap.Builder<String, String>();
            fields.put(LimeXMLNames.VIDEO_TYPE, "adult");
            fields.put(LimeXMLNames.VIDEO_RATING, "adult");
            disallowedFieldValues = fields.build();
            ImmutableMap.Builder<String, List<String>> exactFields =
                new ImmutableMap.Builder<String, List<String>>();
            exactFields.put(LimeXMLNames.VIDEO_RATING,
                    Arrays.asList("r", "nc-17"));
            disallowedExactFieldValues = exactFields.build();
        } else {
            disallowedFieldValues = Collections.emptyMap();
            disallowedExactFieldValues = Collections.emptyMap();
        }
    }

    @Override
    public boolean allow(QueryReply qr, Response response) {
        // don't check the super -- that's done by the separate KeywordFilter
        LimeXMLDocument doc = response.getDocument();
        return doc == null || allowDoc(doc);
    }
    
    @Override
    public boolean allow(SearchResult result, LimeXMLDocument document) {
        // don't check the super -- that's done by the separate KeywordFilter
        return document == null || allowDoc(document);
    }

    /**
     * Returns true if none of the filters matched. 
     */
    private boolean allowDoc(LimeXMLDocument doc) {
        for(Entry<String, String> entry : doc.getNameValueSet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if(matches(value))
                return false;
            value = value.toLowerCase(Locale.US);
            String dis = disallowedFieldValues.get(key);
            if(dis != null && value.contains(dis))
                return false;
            List<String> exact = disallowedExactFieldValues.get(key);
            if(exact != null) {
                for(String s : exact) {
                    if(s.equals(value))
                        return false;
                }
            }
        }
        return true;
    }
}
