package com.limegroup.gnutella.spam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

@EagerSingleton
public class RatingTable implements Service {
    private static final Log LOG = LogFactory.getLog(RatingTable.class);

    /**
     * Don't hold more than this many entries in memory or save more than
     * this many entries to disk. The size is a tradeoff - tokens should be
     * discarded when they become irrelevant, but not before.
     */
    private static final int MAX_SIZE = 5000;

    /**
     * Initial size of the rating table - should not be too large as many
     * users probably don't use the spam filter.
     */
    private static final int INITIAL_SIZE = 100;

    /**
     * The file for storing tokens between sessions.
     */
    private final File spamDat;

    /**
     * A map containing a limited number of tokens. We use a map rather than
     * a set so that we can retrieve a stored token by using an equivalent
     * token as a key. This allows us to use a token without rating data to
     * retrieve an equivalent token that has rating data.
     * <p>
     * The size of the map is limited. Entries are discarded in
     * least-recently-used order when the map is full, on the assumption that
     * the least-recently-used token is the least important to keep. Tokens
     * with zero ratings are not stored in the map.
     * <p>
     * LOCKING: this.
     */
    private final Map<Token, Token> tokenMap
    = new LinkedHashMap<Token, Token>(INITIAL_SIZE, 0.75f, true) {
        // This method will be called on every get(), put(), and putAll()
        @Override
        protected boolean removeEldestEntry(Map.Entry<Token, Token> e) {
            if(size() > MAX_SIZE) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Discarding token " + e.getValue());
                return true;
            }
            return false;
        }
    };

    /**
     * Tokens that the user has searched for during this session (could be
     * keywords, XML metadata, and maybe URNs in the future). They will not
     * contribute to the spam ratings of search results, because spammers
     * often echo the search terms.
     */
    private final HashSet<Token> searchTokens = new HashSet<Token>();

    /**
     * Whether the rating table needs to be saved. LOCKING: this.
     */
    private boolean dirty = false;

    private final Tokenizer tokenizer;
    private final TemplateHashTokenFactory templateHashTokenFactory;
    private final ScheduledExecutorService backgroundExecutor;

    @Inject
    RatingTable(Tokenizer tokenizer,
            TemplateHashTokenFactory templateHashTokenFactory,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.tokenizer = tokenizer;
        this.templateHashTokenFactory = templateHashTokenFactory;
        this.backgroundExecutor = backgroundExecutor;
        spamDat = new File(CommonUtils.getUserSettingsDir(), "spam.dat");
    }

    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Spam Management");
    }

    public void initialize() {
    }

    public synchronized void start() {
        load(spamDat);
        loadSpamTokensFromSettings();
        // Save the ratings every five minutes (if necessary)
        backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                save();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        save();
    }

    synchronized void loadSpamTokensFromSettings() {
        if(!FilterSettings.USE_NETWORK_FILTER.getValue())
            return;
        // Rate the received template hashes as spam
        for(String hash : FilterSettings.SPAM_TEMPLATES.get()) {
            setRatingIfUnrated(new TemplateHashToken(Base32.decode(hash)), 1f);
        }
        // Rate the received file sizes as spam
        for(String size : FilterSettings.SPAM_SIZES.get()) {
            try {
                setRatingIfUnrated(new ApproximateSizeToken(Long.parseLong(size)), 1f);
            } catch(NumberFormatException e) {
                LOG.debug("Error parsing file size", e);
                continue;
            }
        }
    }

    /**
     * Clears the filter data
     */
    synchronized void clear() {
        LOG.debug("Clearing ratings");
        tokenMap.clear();
        dirty = true;
    }

    /**
     * Returns the rating for a RemoteFileDesc
     * 
     * @param desc the RemoteFileDesc to rate
     * @return the rating for the RemoteFileDesc
     */
    synchronized float getRating(RemoteFileDesc desc) {
        float rating = getRating(lookup(tokenizer.getTokens(desc)));
        if(LOG.isDebugEnabled()) {
            String addr = desc.getAddress().getAddressDescription();
            LOG.debug("Result from " + addr + " rated " + rating);
        }
        return rating;
    }

    /**
     * Returns the combined rating for a set of tokens.
     * 
     * @param tokens a set of tokens to be rated
     * @return the combined rating for the tokens
     */
    private float getRating(Set<Token> tokens) {
        float rating = 1;
        for(Token t : tokens)
            rating *= 1 - t.getRating();
        return 1 - rating;
    }

    /**
     * Assigns the given rating to an array of RemoteFileDescs.
     * 
     * @param descs an array of RemoteFileDescs to be rated
     * @param rating a rating between 0 (not spam) and 1 (spam)
     */
    synchronized void rate(RemoteFileDesc[] descs, float rating) {
        rateInternal(lookup(tokenizer.getTokens(descs)), rating);
    }

    /**
     * Assigns the given rating to a QueryReply, ignoring keyword tokens.
     * 
     * @param qr a QueryReply to be rated
     * @param rating a rating between 0 (not spam) and 1 (spam)
     */
    synchronized void rate(QueryReply qr, float rating) {
        rateInternal(lookup(tokenizer.getNonKeywordTokens(qr)), rating);
    }

    /**
     * Assigns the given rating to the given token and stores it, unless the
     * token is already stored, in which case the existing rating is preserved.
     * LOCKING: this.
     */
    private void setRatingIfUnrated(Token t, float rating) {
        if(rating == 0f)
            return;
        Token stored = tokenMap.get(t);
        if(stored == null) {
            if(LOG.isDebugEnabled())
                LOG.debug("Setting rating of " + t + " to " + rating);
            t.setRating(rating);
            tokenMap.put(t, t);
            dirty = true;
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("Not replacing rating of " + t);
        }
    }

    /**
     * Clears the ratings of the tokens associated with a QueryRequest and
     * ignores them for the rest of the session.
     * 
     * @param qr the QueryRequest to clear
     */
    synchronized void clear(QueryRequest qr) {
        for(Token t : tokenizer.getTokens(qr)) {
            if(LOG.isDebugEnabled())
                LOG.debug("Clearing search token " + t);
            searchTokens.add(t); // Ignore the token for this session
            // Clear the rating for future sessions
            if(tokenMap.remove(t) != null)
                dirty = true;
        }
    }

    /**
     * Assigns the given rating to a set of tokens, storing any that have
     * non-zero ratings after being updated and removing from the map any that
     * have zero ratings after being updated. LOCKING: this.
     * 
     * @param tokens a set of tokens to be rated
     * @param rating a rating between 0 (not spam) and 1 (spam)
     */
    private void rateInternal(Set<Token> tokens, float rating) {
        for(Token t : tokens) {
            float before = t.getRating();
            t.updateRating(rating);
            float after = t.getRating();
            if(LOG.isDebugEnabled())
                LOG.debug(t + " was rated " + before + ", now rated " + after);
            if(after == 0f)
                tokenMap.remove(t);
            else
                tokenMap.put(t, t);
            dirty = true;
        }
    }

    /**
     * Replaces each token with an equivalent previously stored token, or
     * returns the token that was passed in if no equivalent exists. Tokens
     * that have been searched for during this session are not returned.
     * 
     * @param tokens a set of tokens to be replaced
     * @return a set of equivalent tokens, with search tokens removed
     */
    private Set<Token> lookup(Set<Token> tokens) {
        Set<Token> newTokens = new HashSet<Token>();
        for(Token t : tokens) {
            if(!searchTokens.contains(t))
                newTokens.add(lookup(t));
            else if(LOG.isDebugEnabled())
                LOG.debug("Ignoring search token " + t);
        }
        return newTokens;
    }

    /**
     * Returns an equivalent previously stored token if any such token exists,
     * otherwise returns the token that was passed in. LOCKING: this.
     * 
     * @param token the token to look up
     * @return the same token or a previously stored equivalent
     */
    private Token lookup(Token token) {
        Token stored = tokenMap.get(token);
        return stored == null ? token : stored;
    }

    /**
     * Looks up a single token and returns its rating (for testing).
     */
    synchronized float lookupAndGetRating(Token token) {
        return getRating(Collections.singleton(lookup(token)));
    }

    /**
     * Loads ratings from disk. Package access for testing.
     */
    void load(File file) {
        if(!file.exists()) {
            LOG.debug("No ratings to load");
            return;
        }
        Map<Token, Token> temporaryMap = new HashMap<Token, Token>();
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(file)));
            List<Token> list = GenericsUtils.scanForList(is.readObject(),
                    Token.class, GenericsUtils.ScanMode.REMOVE);
            int zeroes = 0, converted = 0;
            for(Token t : list) {
                // Convert old template tokens into template hash tokens
                if(t instanceof TemplateToken) {
                    t = templateHashTokenFactory.convert((TemplateToken) t);
                    converted++;
                }
                if(t.getRating() > 0f) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Loading " + t + ", rated " + t.getRating());
                    temporaryMap.put(t, t);
                } else {
                    zeroes++;
                }
            }
            synchronized(this) {
                tokenMap.clear();
                tokenMap.putAll(temporaryMap);
                if(zeroes > 0 || converted > 0)
                    dirty = true;
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Loaded " + tokenMap.size() +
                            " entries, converted " + converted + ", skipped " +
                            zeroes + " with zero scores");
                }
            }
        } catch(IOException e) {
            LOG.debug("Error loading spam ratings: ", e);
        } catch(ClassNotFoundException e) {
            LOG.debug("Error loading spam ratings: ", e);
        } catch(ClassCastException e) {
            LOG.debug("Error loading spam ratings: ", e);
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * Saves ratings to disk.
     */
    void save() {
        ArrayList<Token> list;
        synchronized(this) {
            if(!dirty) {
                LOG.debug("Ratings do not need to be saved");
                return;
            }
            dirty = false;
            list = new ArrayList<Token>(tokenMap.size());
            // The iterator returns the least-recently-used entry first
            for(Map.Entry<Token,Token> e : tokenMap.entrySet()) {
                Token t = e.getKey();
                if(LOG.isDebugEnabled())
                    LOG.debug("Saving " + t + ", rated " + t.getRating());
                list.add(t);
            }
        }
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(spamDat)));
            oos.writeObject(list);
            oos.flush();
            if(LOG.isDebugEnabled())
                LOG.debug("Saved " + list.size() + " entries");
        } catch (IOException iox) {
            LOG.debug("Error saving spam ratings: ", iox);
        } finally {
            IOUtils.close(oos);
        }
    }

    /**
     * @return the number of tokens in the rating table (for testing)
     */
    synchronized int size() {
        return tokenMap.size();
    }

    /**
     * @return the least-recently-used token in the table (for testing)
     */
    synchronized Token getLeastRecentlyUsed() {
        for(Map.Entry<Token,Token> e : tokenMap.entrySet())
            return e.getKey();
        return null; // Empty
    }
}