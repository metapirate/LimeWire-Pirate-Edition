package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URIUtils;
import org.limewire.collection.Tuple;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Implements {@link TorrentUriPrioritizer} by performing the following steps
 * on a list of uris:
 * <ul>
 * <li>Remove duplicate uris</li>
 * <li>Remove uris that are known to not be torrent uris</li>
 * <li>Calculate a likelihood score for a uri being a torrent uri</li>
 * <li>Sort list by this score</li>
 * <li>Cap list to the top n elements in it</li>
 * </ul>
 */
public class TorrentUriPrioritizerImpl implements TorrentUriPrioritizer {
    
    private static final Log LOG = LogFactory.getLog(TorrentUriPrioritizerImpl.class);
    
    private final static Pattern numbers = Pattern.compile("[0-9]+");
    private final static int MAX_URIS = 20;
    
    private final String referrerHost;
    
    
    /**
     * List of predicates contributing to the likelihood score of a uri
     * being a torrent uri. They are ordered by weight.
     */
    @SuppressWarnings("unchecked")
    private final List<Predicate<URI>> predicates = ImmutableList.of(
            new IsTorrentUriPredicate(),
            new IsMagnetUriPredicate(),
            new UriSimilarToOtherTorrentUriPredicate(), 
            new UriEndsWithTorrentPredicate(), 
            new UriContainsQueryPredicate(),
            new UriOnSameHostAsReferrerPredicate());
            

    /**
     * Tokenized and lowercased query tokens.
     */
    private final String[] queryTokens;

    private final TorrentUriStore torrentUriStore;
    
    @Inject
    public TorrentUriPrioritizerImpl(@Assisted URI referrer, @Assisted String query,
            TorrentUriStore torrentUriStore) {
        this.torrentUriStore = torrentUriStore;
        this.queryTokens = toLowerCase(query.split("\\s"));
        String host = org.limewire.util.URIUtils.getCanonicalHost(referrer);
        this.referrerHost = host != null ? host : "";
    }
    
    @Override
    public List<URI> prioritize(List<URI> candidates) {
        // remove duplicates
        candidates = uniquify(candidates);
        // remove known non torrent uris
        int size = candidates.size();
        candidates = filter(candidates, new NotTorrentUriPredicate());
        LOG.debugf("removed non torrents: {0}, new size {1}", size - candidates.size(), candidates.size());
        // compute scores
        List<Tuple<URI, Integer>> scoredUris = transform(candidates, new TorrentUriLikelihoodFunction());
        // sort by how likely a candidate
        Collections.sort(scoredUris, new ScoreComparator());
        // only look at the top n uris
        scoredUris = scoredUris.subList(0, Math.min(scoredUris.size(), MAX_URIS));
        // transform back
        return transform(scoredUris, new UriExtractor());
    }
    
    /**
     * Applies <code>function</code> to elements of <code>list</code> and
     * returns a new {@link ArrayList} with results.
     */
    static <S, T> List<T> transform(List<S> list, Function<S, T> function) {
        List<T> transformed = new ArrayList<T>(list.size());
        for (S element : list) {
            transformed.add(function.apply(element));
        }
        return transformed;
    }

    /**
     * Filters elements of <code>list</code> using <code>predicate</code>
     * keeping elements that the predicated applies to and returns a new
     * {@link ArrayList} with those elements. 
     */
    <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        List<T> filtered = new ArrayList<T>(list.size());
        for (T element : list) {
            if (predicate.apply(element)) {
                filtered.add(element);
            }
        }
        return filtered;
    }

    /**
     * Uniquifies list by converting it to a hash set and then back to an
     * {@link ArrayList}.
     */
    private List<URI> uniquify(List<URI> candidates) {
        return new ArrayList<URI>(new HashSet<URI>(candidates));
    }

    @Override
    public void setIsTorrent(URI uri, boolean isTorrent) {
        torrentUriStore.setIsTorrentUri(uri, isTorrent);
        if (isTorrent) {
            String host = org.limewire.util.URIUtils.getCanonicalHost(uri);
            String path = uri.getPath();
            if (host == null || path == null) {
                LOG.debugf("host or path null {0}, {1}", host, path);
                return;
            }
            List<String> tokens = tokenize(path);
            String canonicalPath = "/" + StringUtils.explode(tokens, "/");
            uri = URIUtils.resolve(uri, canonicalPath);
            LOG.debugf("canonicalized uri: {0}", uri);
            torrentUriStore.addCanonicalTorrentUri(host, uri);
        }
    }
    
    Set<URI> getTorrentUrisForDomain(URI uri) {
        String host = org.limewire.util.URIUtils.getCanonicalHost(uri);
        if (host != null) {
           return torrentUriStore.getTorrentUrisForHost(host);
        }
        return Collections.emptySet();
    }
    
    /**
     * @return true if <code>uri</code> is structurally similar to any of the
     * uris in <code>torrentUris</code>
     */
    boolean isStructurallySimilar(URI uri, Iterable<URI> torrentUris) {
        for (URI torrentUri : torrentUris) {
            if (isStructurallySimilar(uri, torrentUri)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tokenizes a uri path, normalizing it and replacing the query and numerical
     * elements in the path with placeholders. 
     */
    List<String> tokenize(String path) {
        String[] tokens = path.split("[/?#]");
        List<String> canonicalized = new ArrayList<String>(tokens.length);
        for (String token : tokens) {
            if (containsQuery(token)) {
                canonicalized.add("*query*");
            } else if (numbers.matcher(token).matches()) {
                canonicalized.add("*numbers*");
            } else if (!token.isEmpty()) {
                canonicalized.add(token);
            }
        }
        return canonicalized;
    }
    
    /**
     * @return true if the two uris are structurally similar, for that their
     * paths are {@link #tokenize(String) tokenized} and then compared
     */
    boolean isStructurallySimilar(URI uri, URI torrentUri) {
        String path = uri.getPath();
        String torrentPath = torrentUri.getPath();
        if (path == null || torrentPath == null) {
            return false;
        }
        int score = 0;
        List<String> pathTokens = tokenize(path);
        List<String> torrentPathTokens = tokenize(torrentPath);
        if (pathTokens.size() == torrentPathTokens.size()) {
            score += 1;
        }
        for (Tuple<String, String> tuple : zip(pathTokens, torrentPathTokens)) {
            if (tuple.getFirst().equalsIgnoreCase(tuple.getSecond())) {
                score += 1;
            } else {
                score -= 1;
            }
        }
        return score > 3;
    }
    
    /**
     * @return true if <code>value</code> contains all query tokens
     */
    boolean containsQuery(String value) {
        value = value.toLowerCase();
        for (String token : queryTokens) {
            if (!value.contains(token)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * @return the likelihood score of a uri being a torrent uri
     */
    private int computeScore(URI uri) {
        int score = 0;
        for (Predicate<URI> predicate : predicates) {
            if (predicate.apply(uri)) {
                score += 1;
            }
            score <<= 1;
        }
        return score;
    }
    
    private static String[] toLowerCase(String...tokens) {
        List<String> results = new ArrayList<String>(tokens.length);
        for (String token : tokens) {
            results.add(token.toLowerCase());
        }
        return results.toArray(new String[results.size()]);
    }
    
    private class IsTorrentUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return torrentUriStore.isTorrentUri(uri);
        }
    }
    
    private class IsMagnetUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return "magnet".equalsIgnoreCase(uri.getScheme());
        }
    }
    
    private class UriSimilarToOtherTorrentUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            Set<URI> torrentUris = getTorrentUrisForDomain(uri);
            if (torrentUris.isEmpty()) {
                return false;
            }
            return isStructurallySimilar(uri, torrentUris);
        }
    }
    
    private class UriContainsQueryPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return containsQuery(uri.toString());
        }
    }
    
    private class UriEndsWithTorrentPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            String path = uri.getPath();
            if (path != null) {
                return FileUtils.getFileExtension(uri.getPath()).equalsIgnoreCase("torrent");
            }
            return false;
        }
    }
    
    private class UriOnSameHostAsReferrerPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return referrerHost.equals(org.limewire.util.URIUtils.getCanonicalHost(uri));
        }
    }
    
    private class NotTorrentUriPredicate implements Predicate<URI> {
        @Override
        public boolean apply(URI uri) {
            return !torrentUriStore.isNotTorrentUri(uri);
        }
    }
    
    private class TorrentUriLikelihoodFunction implements Function<URI, Tuple<URI, Integer>> {
        @Override
        public Tuple<URI, Integer> apply(URI uri) {
            return new Tuple<URI, Integer>(uri, computeScore(uri));
        }
    }
    
    private class ScoreComparator implements Comparator<Tuple<URI, Integer>> {
        @Override
        public int compare(Tuple<URI, Integer> o1, Tuple<URI, Integer> o2) {
            return o2.getSecond().compareTo(o1.getSecond());
        }
    }
    
    private class UriExtractor implements Function<Tuple<URI, Integer>, URI> {
        @Override
        public URI apply(Tuple<URI, Integer> tuple) {
            return tuple.getFirst();
        }
    }
    
    /**
     * @return an iterable of tuples from <code>iterableS</code> and <code>iterableT</code>
     */
    static <S, T> Iterable<Tuple<S, T>> zip(final Iterable<S> iterableS, final Iterable<T> iterableT) {
        return new Iterable<Tuple<S,T>>() {
            @Override
            public Iterator<Tuple<S, T>> iterator() {
                return new ZipIterator<S, T>(iterableS.iterator(), iterableT.iterator());
            }
        };
    }
    
    private static class ZipIterator<S, T> implements Iterator<Tuple<S, T>> {
        
        private final Iterator<S> iteratorS;
        private final Iterator<T> iteratorT;

        public ZipIterator(Iterator<S> iteratorS, Iterator<T> iteratorT) {
            this.iteratorS = iteratorS;
            this.iteratorT = iteratorT;
        }

        @Override
        public boolean hasNext() {
            return iteratorS.hasNext() && iteratorT.hasNext();
        }
        
        @Override
        public Tuple<S, T> next() {
            return new Tuple<S, T>(iteratorS.next(), iteratorT.next());
        }
        
        @Override
        public void remove() {
            iteratorS.remove();
            iteratorT.remove();
        }
        
    }
}
