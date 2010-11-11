package org.limewire.core.impl.library;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResult;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.Stopwatch;
import org.limewire.util.StringUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@EagerSingleton
public class FriendLibraries {

    private static final Log LOG = LogFactory.getLog(FriendLibraries.class);
    
    private static final Stopwatch watch = new Stopwatch(LOG);
    /**
     * Keeps track of whether the database based index has already been
     * initialized.
     */
    private final AtomicBoolean databaseIndexInitialized = new AtomicBoolean(false);
    /**
     * An atomic count that identifies a presence library in the database. Every
     * {@link LibraryListener} requests a new unique id at construction which
     * is used in the database.
     */
    private final AtomicInteger uniquePresenceId = new AtomicInteger();
    /**
     * Lock object to synchronize access to {@link #index}.    
     */
    private final Object indexLock = new Object(); 
    /**
     * The index that is currently used. Initially, the index is the {@link EmptyIndex}
     * which doesn't have any results. Once the first presence library is added
     * the index will be switched to the {@link DatabaseIndex}.
     */
    private volatile Index index = new EmptyIndex();
    /**
     * The list map of {@link LibraryListener} indexed by the presence id of the
     * presence library they're listening to.
     */
    private final Map<String, LibraryListener> listeners = new ConcurrentHashMap<String, LibraryListener>();
    /**
     * Processing queue for writes into database to not hold up event dispatcher. An explicit
     * reference is needed to allow scheduling of calls to clear in order after inserts.
     * 
     * Non-private for testing purposes.
     */
    final Executor processingQueue = ExecutorsHelper.newProcessingQueue("friend-library-index-queue");
    
    /**
     * @param initializeDbIndex true to ensure the returned index is the {@link DatabaseIndex}
     * otherwise the return value could also be the {@link EmptyIndex}
     * @return the currently active index 
     */
    private Index getIndex(boolean initializeDbIndex) {
        if (initializeDbIndex && !databaseIndexInitialized.get()) {
            synchronized (indexLock) {
                if (!databaseIndexInitialized.get()) {
                    index = new DatabaseIndex(); 
                    databaseIndexInitialized.set(true);
                }
            }
        }
        return index;
    }
    
    @Inject
    void register(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(
                new ListEventListener<FriendLibrary>() {
                    @Override
                    public void listChanged(ListEvent<FriendLibrary> listChanges) {
                        while (listChanges.next()) {
                            int type = listChanges.getType();
                            if (type == ListEvent.INSERT) {
                                FriendLibrary friendLibrary = listChanges.getSourceList().get(
                                        listChanges.getIndex());
                                new AbstractListEventListener<PresenceLibrary>() {
       
                                    @Override
                                    protected void itemAdded(PresenceLibrary item, int idx,
                                            EventList<PresenceLibrary> source) {
                                        LOG.debugf("adding library for presence {0} to index", item);
                                        LibraryListener listener = new LibraryListener(item);
                                        listeners.put(item.getPresence().getPresenceId(), listener);
                                        item.addListener(listener);
                                    }

                                    @Override
                                    protected void itemRemoved(PresenceLibrary item, int idx,
                                            EventList<PresenceLibrary> source) {
                                        LOG.debugf("removing library for presence {0} from index",
                                                item.getPresence().getPresenceId());
                                        final LibraryListener listener = listeners.remove(item.getPresence().getPresenceId());
                                        item.removeListener(listener);
                                        processingQueue.execute(new Runnable() {
                                            public void run() {
                                                listener.clear();
                                            }
                                        });
                                    }

                                    @Override
                                    protected void itemUpdated(PresenceLibrary item,
                                            PresenceLibrary priorItem, int idx,
                                            EventList<PresenceLibrary> source) {
                                    }
                                }.install(friendLibrary.getPresenceLibraryList());
                            }
                        }
                    }
                });
    }

    private static String canonicalize(final String s) {
        return s.toUpperCase(Locale.US).toLowerCase(Locale.US);
    }

    /**
     * @return all keywords prefixed by <code>prefix</code> and of <code>category</code>
     * or empty collection if there are none
     */
    public Collection<String> getSuggestions(String prefix, SearchCategory category) {
        return getIndex(false).getSuggestions(prefix, category);
    }

    /**
     * @return all keywords prefixed by <code>prefix</code> and of <code>category</code>
     * and of <code>filePropertyKey</code> or empty collection if there are none
     */
    public Collection<String> getSuggestions(String prefix, SearchCategory category,
            FilePropertyKey filePropertyKey) {
        return getIndex(false).getSuggestions(prefix, category, filePropertyKey);
    }
    
    /**
     * @return all search results that match the given details 
     */
    public Collection<SearchResult> getMatchingItems(SearchDetails searchDetails) {
        return getIndex(false).getMatchingItems(searchDetails);
    }
    
    /**
     * Breaks <code>query</code> into a list of keywords removing empty
     * keywords. 
     */
    private List<String> extractKeywords(String query) {
        String[] keywords = query.split("\\s");
        List<String> results = new ArrayList<String>(keywords.length);
        for (String keyword : keywords) {
            if (!keyword.isEmpty()) {
                results.add(canonicalize(keyword));
            }
        }
        return results;
    }

    /**
     * Maps an integer presence id from the database back to a {@link PresenceLibrary}
     * and returns the search result at <code>index</code>. 
     */
    private SearchResult getSearchResult(int presenceId, int index) {
        for (LibraryListener libraryListener : listeners.values()) {
            if (presenceId == libraryListener.presenceId) {
                SearchResult result = libraryListener.presenceLibrary.get(index);
                return result;
            }
        }
        throw new IllegalArgumentException(presenceId + " " + index);
    }
    
    /**
     * Listens to events on a specific presence and updates the database index
     * based on these events.
     */
    private class LibraryListener implements EventListener<RemoteLibraryEvent> {
        
        /**
         * Unique id to identify this incarnation of presence library
         * in the database.
         */
        private final int presenceId = uniquePresenceId.incrementAndGet();
        private final PresenceLibrary presenceLibrary;

        LibraryListener(PresenceLibrary presenceLibrary) {
            this.presenceLibrary = presenceLibrary;
        }
        
        /**
         * Indexes the search result in the database index.
         * 
         * @param index the index of the search result in {@link PresenceLibrary}
         * @param result the search result to index 
         */
        private void index(int index, SearchResult result) {
            getIndex(true).index(presenceId, index, result);
        }

        /**
         * Clears all indexed keywords for this presence library from
         * the database index.
         */
        private void clear() {
            getIndex(true).clear(presenceId);
        }
        
        @Override
        public void handleEvent(final RemoteLibraryEvent event) {
            processingQueue.execute(new Runnable() {
                public void run() {
                    switch (event.getType()) {
                    case STATE_CHANGED:
                        break;
                    case RESULTS_ADDED:
                        Collection<SearchResult> results = event.getAddedResults();
                        int index = event.getStartIndex();
                        for (SearchResult result : results) {
                            index(index++, result);
                        }
                        break;
                    case RESULTS_CLEARED:
                        clear();
                        break;
                    }
                }
            });
        }
    }
          
    /**
     * Internal interface to delegate queries to an empty index if there are no
     * presence libraries, and otherwise delegate them to the database index.
     */
    interface Index {
        Collection<String> getSuggestions(String prefix, SearchCategory category);
        Collection<String> getSuggestions(String prefix, SearchCategory category, FilePropertyKey filePropertyKey);
        Collection<SearchResult> getMatchingItems(SearchDetails searchDetails);
        /**
         * Indexes the properties of <code>newFile</code> for suggestions and
         * retrieval. 
         * @param presenceId presence id internal to FriendLibraries
         * @param index of the result in its presence library
         */
        void index(int presenceId, int index, SearchResult newFile);
        void clear(int presenceId);
    }
    
    /**
     * Empty implementation of {@link Index} returning empty collections
     * for queries and throwing {@link UnsupportedOperationException} for
     * mutating methods.
     */
    private class EmptyIndex implements Index {
        @Override
        public Collection<SearchResult> getMatchingItems(SearchDetails searchDetails) {
            return Collections.emptySet();
        }
        @Override
        public Collection<String> getSuggestions(String prefix, SearchCategory category) {
            return Collections.emptySet();
        }
        @Override
        public Collection<String> getSuggestions(String prefix, SearchCategory category,
                FilePropertyKey filePropertyKey) {
            return Collections.emptySet();
        }
        @Override
        public void index(int presenceId, int index, SearchResult newFile) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void clear(int presenceId) {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * HSQLDB backed index implementation.
     */
    private class DatabaseIndex implements Index {
        
        /**
         * The connection to the database.
         */
        private final Connection connection;
        /**
         * The prepared statement to insert into the properties table.
         */
        private final PreparedStatement insertPropertiesStmt;
        /**
         * The prepared statetement to insert keywords into the suggestions table.
         */
        private final PreparedStatement insertSuggestionsStmt;
        /**
         * List of delete statements to execute to remove a presence
         * library from the index.
         */
        private final ImmutableList<PreparedStatement> deleteStmts;
        
        /**
         * Creates the database file and database tables and indices. This
         * call can block.
         */
        public DatabaseIndex() {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
            try {
                File folder = new File(CommonUtils.getUserSettingsDir(), "friend-indices");
                // delete all db files from a previous session
                FileUtils.deleteRecursive(folder);
                folder.mkdirs();
                
                String connectionUrl = "jdbc:hsqldb:file:" + folder.getAbsolutePath() + File.separator + "friend-indices";
                Connection con = DriverManager.getConnection(connectionUrl, "sa", "");
                Statement statement = con.createStatement();
                // set properties to make memory footprint small
                statement.execute("set property \"hsqldb.cache_scale\" 8");
                statement.execute("set property \"hsqldb.cache_size_scale\" 6");
                // close and reopen database, since property changes don't become effective otherwise
                con.close();
                
                connection = DriverManager.getConnection(connectionUrl, "sa", "");
                statement = connection.createStatement();

                // drop tables since HSQLDB doesn't get cleaned up properly in test environment
                statement.execute("drop table properties if exists");
                statement.execute("drop table suggestions if exists");
                
                // create tables and indices
                statement.execute("CREATE CACHED TABLE properties (keyword VARCHAR(200), i INT, presence INT, category INT, fileproperty INT)");
                statement.execute("CREATE INDEX propertieskeywordindex on properties (keyword)");
                statement.execute("CREATE INDEX propertiespresenceindex on properties (presence)");
                
                statement.execute("CREATE CACHED TABLE suggestions (keyword VARCHAR(200), presence INT, category INT, fileproperty INT)");
                statement.execute("CREATE INDEX suggestionskeywordindex on suggestions(keyword)");
                statement.execute("CREATE INDEX suggestionspresenceindex on suggestions (presence)");
                
                // create prepared statements
                insertPropertiesStmt = connection.prepareStatement("INSERT INTO properties (keyword, i, presence, category, fileproperty) VALUES (?,?,?,?,?)");
                insertSuggestionsStmt = connection.prepareStatement("INSERT INTO suggestions (keyword, presence, category, fileproperty) VALUES (?,?,?,?)");
                deleteStmts = ImmutableList.of(
                        connection.prepareStatement("delete from properties where presence = ?"),
                        connection.prepareStatement("delete from suggestions where presence = ?")
                );
                
                // delete files in folder on exit
                File[] files = FileUtils.getFilesRecursive(folder);
                for (File file : files) {
                    file.deleteOnExit();
                }
                // files do not exist yet, but add delete hook for it nevertheless
                new File(folder, "friend-indices.backup").deleteOnExit();
                new File(folder, "friend-indices.script").deleteOnExit();
            } catch (SQLException sql) {
                throw new RuntimeException(sql);
            }
        }

        /**
         * Uses an intersect query to link all keywords together.
         */
        @Override
        public Collection<SearchResult> getMatchingItems(SearchDetails searchDetails) {
            LOG.debugf("getMatchingItems for: {0}", searchDetails);
            SearchCategory category = searchDetails.getSearchCategory();
            StringBuilder sqlQuery = new StringBuilder();
            Map<FilePropertyKey, List<String>> details = new EnumMap<FilePropertyKey, List<String>>(FilePropertyKey.class);
            int totalKeywordCount = 0;
            for (Entry<FilePropertyKey, String> entry : searchDetails.getAdvancedDetails().entrySet()) {
                List<String> keywords = extractKeywords(entry.getValue());
                totalKeywordCount += keywords.size();
                details.put(entry.getKey(), keywords);
            }
            List<String> keywords = extractKeywords(searchDetails.getSearchQuery());
            final String criterion = category != SearchCategory.ALL ? " and category = ?" : "";
            if (!keywords.isEmpty()) {
                sqlQuery.append(StringUtils.explode("select distinct presence, i from properties where keyword like ?" + criterion, " intersect ", keywords.size()));
            }
            if (totalKeywordCount > 0) {
                if (!keywords.isEmpty()) {
                    sqlQuery.append(" intersect ");
                }
                sqlQuery.append(StringUtils.explode("select distinct presence, i from properties where keyword like ? and fileproperty = ?" + criterion, " intersect ", totalKeywordCount));
            }
            try {
                PreparedStatement statement = connection.prepareStatement(sqlQuery.toString());
                LOG.debugf("query statement: {0}", statement);
                int index = 1;
                for (String keyword : keywords) {
                    statement.setString(index++, keyword + "%");
                    if (!criterion.isEmpty()) {
                        statement.setInt(index++, category.getId());
                    }
                }
                for (Entry<FilePropertyKey, List<String>> entry : details.entrySet()) {
                    int fileProperty = entry.getKey().ordinal();
                    for (String keyword : entry.getValue()) {
                        statement.setString(index++, keyword + "%");
                        statement.setInt(index++, fileProperty);
                        if (!criterion.isEmpty()) {
                            statement.setInt(index++, category.getId());
                        }
                    }
                }
                LOG.debugf("filled in statement: {0}", statement);
                watch.reset();
                ResultSet resultSet = statement.executeQuery();
                watch.resetAndLog("query took ");
                List<SearchResult> results = new ArrayList<SearchResult>();
                while (resultSet.next()) {
                    results.add(getSearchResult(resultSet.getInt(1), resultSet.getInt(2)));
                }
                return results;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Returns the 8 most indexed keywords matching the prefix and category
         * of the query.
         */
        @Override
        public Collection<String> getSuggestions(String prefix, SearchCategory category) {
            prefix = canonicalize(prefix);
            LOG.debugf("get suggestions: {0}", prefix);
            watch.reset();
            try {
                PreparedStatement statement; 
                if (category == SearchCategory.ALL) {
                    statement = connection.prepareStatement("select keyword from suggestions where keyword LIKE ? group by keyword order by count(*) desc limit 8");
                    statement.setString(1, prefix + "%");
                } else {
                    statement = connection.prepareStatement("select keyword from suggestions where keyword LIKE ? and category = ? group by keyword order by count(*) desc limit 8");
                    statement.setString(1, prefix + "%");
                    statement.setInt(2, category.getId());
                }
                ResultSet result = statement.executeQuery();
                Set<String> suggestions = new HashSet<String>();
                while (result.next()) {
                    String suggestion = result.getString(1);
                    suggestions.add(suggestion);
                }
                if (LOG.isTraceEnabled())
                    watch.resetAndLog("query for " + prefix);
                return suggestions;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } 
        }

        /**
         * Returns the 8 most indexed keywords matching the prefix and category
         * of the query.
         */
        @Override
        public Collection<String> getSuggestions(String prefix, SearchCategory category,
                FilePropertyKey filePropertyKey) {
            prefix = canonicalize(prefix);
            watch.reset();
            try {
                PreparedStatement statement;
                if (category == SearchCategory.ALL) {
                    statement = connection.prepareStatement("select keyword from suggestions where keyword LIKE ? and fileproperty = ? group by keyword order by count(*) desc limit 8");
                    statement.setString(1, prefix + "%");
                    statement.setInt(2, filePropertyKey.ordinal());
                } else {
                    statement = connection.prepareStatement("select keyword from suggestions where keyword LIKE ? and fileproperty = ? and category = ? group by keyword order by count(*) desc limit 8");
                    statement.setString(1, prefix + "%");
                    statement.setInt(2, filePropertyKey.ordinal());
                    statement.setInt(3, category.getId());
                }
                ResultSet result = statement.executeQuery();
                Set<String> suggestions = new HashSet<String>();
                while (result.next()) {
                    String suggestion = result.getString(1);
                    suggestions.add(suggestion);
                }
                if (LOG.isTraceEnabled())
                    watch.resetAndLog("query for " + prefix);
                return suggestions;
            } catch (SQLException sql) {
                throw new RuntimeException(sql);
            }
        }

        @Override
        public void index(int presenceId, int index, SearchResult newFile) {
            watch.reset();
            for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                Object property = newFile.getProperty(filePropertyKey);
                if (property != null) {
                    String sentence = property.toString();
                    indexProperty(presenceId, index, newFile, filePropertyKey, sentence);
                }
            }
            if (LOG.isTraceEnabled())
                watch.resetAndLog("indexing " + newFile);
        }
        
        /**
         * Indexes properties in both the suggestions and properties indexes.
         * <p>
         * The suggestions index only indexes the phrase as a whole. While the
         * filename indexes the phrase by breaking it apart into all the words
         * within.
         */
        private void indexProperty(int presenceId, int index, SearchResult newFile, FilePropertyKey filePropertyKey, String phrase) {
            SearchCategory category = SearchCategory.forCategory(newFile.getCategory());
            try {
                Set<String> keywords = new HashSet<String>();
                keywords.add(canonicalize(phrase));
                for (String keyword : phrase.split("\\s")) {
                    keywords.add(canonicalize(keyword));
                }
                insertWordsIntoPropertiesIndex(insertPropertiesStmt, keywords, index, presenceId, category, filePropertyKey);
                insertWordIntoSuggestionsIndex(insertSuggestionsStmt, phrase, presenceId, category, filePropertyKey);
            } catch (SQLException sql) {
                throw new RuntimeException(sql);
            }
        }
        
        private void insertWordsIntoPropertiesIndex(PreparedStatement statement, Collection<String> keywords, int index, int presenceId, SearchCategory category, FilePropertyKey filePropertyKey) throws SQLException {
            for (String keyword : keywords) {
                int i = 1;
                statement.setString(i++, keyword);
                statement.setInt(i++, index);
                statement.setInt(i++, presenceId);
                statement.setInt(i++, category.getId());
                statement.setInt(i++, filePropertyKey.ordinal());
                statement.addBatch();
            }
            insertPropertiesStmt.executeBatch();
        
        }
        
        private void insertWordIntoSuggestionsIndex(PreparedStatement statement, String keyword, int presenceId,
                SearchCategory category, FilePropertyKey filePropertyKey) throws SQLException {
            statement.setString(1, canonicalize(keyword));
            statement.setInt(2, presenceId);
            statement.setInt(3, category.getId());
            statement.setInt(4, filePropertyKey.ordinal());
            statement.execute();
        }

        @Override
        public void clear(int presenceId) {
            watch.reset();
            try {
                for (PreparedStatement statement : deleteStmts) {
                    statement.setInt(1, presenceId);
                    statement.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            watch.resetAndLog("clearing");
        }
        
    }
     
}
