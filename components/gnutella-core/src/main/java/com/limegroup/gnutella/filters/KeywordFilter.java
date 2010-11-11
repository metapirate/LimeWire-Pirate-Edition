package com.limegroup.gnutella.filters;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.settings.FilterSettings;

import com.google.common.collect.ImmutableList;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.response.ResponseFilter;
import com.limegroup.gnutella.filters.response.SearchResultFilter;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/** 
 * A filter that blocks queries and responses matching certain banned keywords.
 */
public class KeywordFilter implements SpamFilter, ResponseFilter, SearchResultFilter {

    static final String[] ADULT_WORDS = {
        "adult", "anal", "anul", "ass", "boob", "blow", "bondage", "centerfold",
        "cock", "cum", "cunt", "dick", "dildo", "facial", "fuck", "gangbang",
        "hentai", "horny", "incest", "jenna", "masturbat", "milf", "nipple",
        "orgasm", "pedo", "penis", "playboy", "porn", "pussy", "rape", "sex",
        "slut", "squirt", "stripper", "suck", "tits", "tittie", "titty", "twat",
        "underage", "vagina", "whore", "xxx"
    };

    /** INVARIANT: strings in ban contain only lowercase */
    private final List<String> ban;

    KeywordFilter() {
        this(FilterSettings.FILTER_ADULT.getValue(), true);
    }

    protected KeywordFilter(boolean banAdult, boolean banPersonal) {
        ImmutableList.Builder<String> builder =
            new ImmutableList.Builder<String>();
        if(banAdult) {
            for(String word : ADULT_WORDS) {
                builder.add(word);
            }
        }
        if(banPersonal) {
            for(String word : FilterSettings.BANNED_WORDS.get()) {
                builder.add(word);
            }
            for(String ext : FilterSettings.BANNED_EXTENSIONS.get()) {
                builder.add(ext);
            }
        }
        ban = builder.build();
    }

    KeywordFilter(Collection<String> words) {
        ImmutableList.Builder<String> builder =
            new ImmutableList.Builder<String>();
        for(String word : words) {
            builder.add(word.toLowerCase(Locale.US));
        }
        ban = builder.build();
    }

    @Override // SpamFilter
    public boolean allow(Message m) {
        if (m instanceof QueryRequest) 
            return !matches(((QueryRequest)m).getQuery());
        else
            return true;
    }

    @Override // ResponseFilter
    public boolean allow(QueryReply qr, Response response) {
        return !matches(response.getName());
    }
    
    @Override // ResultFilter
    public boolean allow(SearchResult result, LimeXMLDocument document) {
        return !matches(result.getFileNameWithoutExtension());
    }
    
    /** 
     * Returns true if phrase matches any of the banned words.
     */
    protected boolean matches(String phrase) {
        String canonical = phrase.toLowerCase(Locale.US);
        for(String word : ban) {
            if(canonical.indexOf(word) != -1)
                return true;
        }
        return false;
    }
}
