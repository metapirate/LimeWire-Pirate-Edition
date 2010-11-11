package org.limewire.core.impl.search.torrentweb;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implements {@link TorrentUriStore} and {@link TorrentRobotsTxtStore} and does
 * all persistent caching of data related to web torrent searches.
 * <p>
 * Uses hsqldb database and stores everything in ~/.limewire/torrent-uris.*
 */
// can be lazily singleton, uses Service only to be stopped properly, but if
// it's never started no need for service registration
@Singleton
public class TorrentUriDatabaseStore implements TorrentUriStore, TorrentRobotsTxtStore, Service {
    
    private static final Log LOG = LogFactory.getLog(TorrentUriDatabaseStore.class);
    
    /**
     * Maximum age of a torrent entry in store in milliseconds.
     */
    private static final long MAX_TORRENT_ENTRY_AGE = TimeUnit.DAYS.toMillis(90);
    /**
     * Maximum age of robots txt entry in milliseconds.
     */
    private static final long MAX_ROBOTS_ENTRY_AGE = TimeUnit.DAYS.toMillis(14); 
    
    /**
     * Invariant: once initialized <code>dbStore</code> will not become null.
     */
    private volatile DbStore dbStore = null;
    /**
     * Lock for construction of <code>dbStore</code>. 
     */
    private final Object lock = new Object();
    /**
     * Used for timestamps.
     */
    private final Clock clock;
    
    @Inject
    public TorrentUriDatabaseStore(Clock clock) {
        this.clock = clock;
    }
    
    @Override
    public String getServiceName() {
        return "torrent uri store";
    }

    @Override
    public void initialize() {
    }
    
    @Override
    public void start() {
    }
    
    @Override
    public void stop() {
        if (dbStore != null) {
            dbStore.stop();
        }
    }
    
    @Inject
    void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this).in(ServiceStage.VERY_LATE);
    }
    
    private DbStore getStore() {
        if (dbStore != null) {
            return dbStore;
        }
        synchronized (lock) {
            if (dbStore == null) {
                dbStore = new DbStore();
            }
            return dbStore;
        }
    }

    @Override
    public void addCanonicalTorrentUri(String host, URI uri) {
        getStore().addCanonicalTorrentUri(host, uri);
    }
    
    @Override
    public Set<URI> getTorrentUrisForHost(String host) {
        return getStore().getTorrentUrisForHost(host);
    }
    
    @Override
    public boolean isNotTorrentUri(URI uri) {
        return getStore().isNotTorrentUri(uri);
    }
    
    @Override
    public boolean isTorrentUri(URI uri) {
        return getStore().isTorrentUri(uri);
    }
    
    @Override
    public void setIsTorrentUri(URI uri, boolean isTorrent) {
        getStore().setIsTorrentUri(uri, isTorrent);
    }

    @Override
    public String getRobotsTxt(String host) {
        return getStore().getRobotsTxt(host);
    }
    
    @Override
    public void storeRobotsTxt(String host, String robotsTxt) {
        getStore().storeRobotsTxt(host, robotsTxt);
    }
        
    private class DbStore implements TorrentUriStore, TorrentRobotsTxtStore {
    
        private final Connection connection;

        private final PreparedStatement selectTorrentUris;

        private final PreparedStatement selectTorrentUrisByHost;

        private final PreparedStatement insertTorrentUri;

        private final PreparedStatement insertTorrentUriByHost;

        private final PreparedStatement updateTorrentUri;

        private final PreparedStatement selectRobotsTxt;

        private final PreparedStatement insertRobotsTxt;

        private final PreparedStatement selectTorrentUriByHostAndUri;

        private final PreparedStatement updateTorrentUriByHostTimestamp;
        
        
        public DbStore() {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException(e1);
            }
            try {
                // TODO maybe move into subfolder
                File dbFile = new File(CommonUtils.getUserSettingsDir(), "torrent-uris");
                String connectionUrl = "jdbc:hsqldb:file:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(connectionUrl, "sa", "");
                Statement statement = connection.createStatement();
                // set properties to make memory footprint small, will only take
                // effect after restart
                statement.execute("set property \"hsqldb.cache_scale\" 8");
                statement.execute("set property \"hsqldb.cache_size_scale\" 6");
                try {
                    statement.execute("create cached table torrent_uris (hash int, uri varchar(2048), is_torrent boolean, timestamp bigint, constraint unique_hash_uri unique(hash, uri))");
                    statement.execute("create index torrent_uris_index on torrent_uris(hash)");
                    statement.execute("create cached table torrent_uris_by_host(host varchar_ignorecase(255), uri varchar(2048), timestamp bigint, constraint unique_host_uri unique (host, uri))");
                    statement.execute("create index torrentindex on torrent_uris_by_host(host)");
                    statement.execute("create cached table torrent_robots_txt (host varchar_ignorecase(255) primary key, robots_txt varchar(5120), timestamp bigint)");
                } catch (SQLException se) {
                    LOG.debug("sql exception while creating", se);
                }
                selectTorrentUris = connection.prepareStatement("select uri, is_torrent from torrent_uris where hash = ?");
                insertTorrentUri = connection.prepareStatement("insert into torrent_uris values (?, ?, ?, ?)");
                updateTorrentUri = connection.prepareStatement("update torrent_uris set is_torrent = ?, timestamp = ? where hash = ? and uri = ?");
                selectTorrentUrisByHost = connection.prepareStatement("select uri from torrent_uris_by_host where host = ?");
                insertTorrentUriByHost = connection.prepareStatement("insert into torrent_uris_by_host values (?, ?, ?)");
                selectTorrentUriByHostAndUri = connection.prepareStatement("select uri from torrent_uris_by_host where host = ? and uri = ?");
                updateTorrentUriByHostTimestamp = connection.prepareStatement("update torrent_uris_by_host set timestamp = ? where host = ? and uri = ?");
                selectRobotsTxt = connection.prepareStatement("select robots_txt from torrent_robots_txt where host = ?");
                insertRobotsTxt = connection.prepareStatement("insert into torrent_robots_txt values (?, ?, ?)");
                purgeOldEntries();
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
        }
        
        @Override
        public synchronized Set<URI> getTorrentUrisForHost(String host) {
            Set<URI> uris = new HashSet<URI>();
            try {
                selectTorrentUrisByHost.setString(1, host);
                ResultSet resultSet = selectTorrentUrisByHost.executeQuery();
                while (resultSet.next()) {
                    try {
                        String uriString = resultSet.getString(1);
                        boolean added = uris.add(URIUtils.toURI(uriString));
                        assert added;
                    } catch (URISyntaxException e) {
                        LOG.debug("", e);
                    }
                }
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
            return uris;
        }
        
        @Override
        public boolean isNotTorrentUri(URI uri) {
            Boolean value = getTorrentUriValue(uri);
            return value == null ? false : !value.booleanValue();
        }
        
        private synchronized Boolean getTorrentUriValue(URI uri) {
            try {
                selectTorrentUris.setInt(1, uri.hashCode());
                ResultSet resultSet = selectTorrentUris.executeQuery();
                while (resultSet.next()) {
                    String uriString = resultSet.getString(1);
                    try {
                        URI otherUri = URIUtils.toURI(uriString);
                        if (uri.equals(otherUri)) {
                            return resultSet.getBoolean(2);
                        }
                    } catch (URISyntaxException e) {
                        LOG.debugf(e, "uri: {0}", uriString);
                    }
                }
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
            return null;
        }
        
        
        @Override
        public boolean isTorrentUri(URI uri) {
            Boolean value = getTorrentUriValue(uri);
            return value == null ? false : value.booleanValue();
        }
        
        @Override
        public synchronized void setIsTorrentUri(URI uri, boolean isTorrentUri) {
            try {
                Boolean value = getTorrentUriValue(uri);
                if (value != null) {
                    if (value.booleanValue() != isTorrentUri) {
                        updateTorrentUri.setBoolean(1, isTorrentUri);
                        updateTorrentUri.setLong(2, clock.now());
                        updateTorrentUri.setInt(3, uri.hashCode());
                        updateTorrentUri.setString(4, uri.toASCIIString());
                        updateTorrentUri.executeUpdate();
                    }
                } else {
                    insertTorrentUri.setInt(1, uri.hashCode());
                    insertTorrentUri.setString(2, uri.toASCIIString());
                    insertTorrentUri.setBoolean(3, isTorrentUri);
                    insertTorrentUri.setLong(4, clock.now());
                    insertTorrentUri.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public synchronized void addCanonicalTorrentUri(String host, URI uri) {
            try {
                selectTorrentUriByHostAndUri.setString(1, host);
                selectTorrentUriByHostAndUri.setString(2, uri.toASCIIString());
                ResultSet resultSet = selectTorrentUriByHostAndUri.executeQuery();
                if (resultSet.next()) {
                    updateTorrentUriByHostTimestamp.setLong(1, clock.now());
                    updateTorrentUriByHostTimestamp.setString(2, host);
                    updateTorrentUriByHostTimestamp.setString(3, uri.toASCIIString());
                    updateTorrentUriByHostTimestamp.execute();
                } else {
                    insertTorrentUriByHost.setString(1, host);
                    insertTorrentUriByHost.setString(2, uri.toASCIIString());
                    insertTorrentUriByHost.setLong(3, clock.now());
                    insertTorrentUriByHost.execute();
                }
            } catch (SQLException e) {
                LOG.debugf(e, "host {0}, uri {1}", host, uri);
            }
        }
        
        public synchronized void stop() {
            LOG.debug("shutting db down");
            try {
                Statement statement = connection.createStatement();
                statement.execute("SHUTDOWN");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized String getRobotsTxt(String host) {
            try {
                selectRobotsTxt.setString(1, host);
                ResultSet resultSet = selectRobotsTxt.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        public synchronized void storeRobotsTxt(String host, String robotsTxt) {
            if (robotsTxt.length() > TorrentRobotsTxtStore.MAX_ROBOTS_TXT_SIZE) {
                throw new IllegalArgumentException("robots txt too large: " + robotsTxt);
            }
            try {
                insertRobotsTxt.setString(1, host);
                insertRobotsTxt.setString(2, robotsTxt);
                insertRobotsTxt.setLong(3, clock.now());
                insertRobotsTxt.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        
        private void purgeOldEntries() {
            try {
                long threeMonthsAgo = clock.now() - MAX_TORRENT_ENTRY_AGE;
                PreparedStatement statement = connection.prepareStatement("delete from torrent_uris where timestamp < ?");
                statement.setLong(1, threeMonthsAgo);
                statement.execute();
                statement = connection.prepareStatement("delete from torrent_uris_by_host where timestamp < ?");
                statement.setLong(1, threeMonthsAgo);
                statement.execute();
                long twoWeeksAgo = clock.now() - MAX_ROBOTS_ENTRY_AGE;
                statement = connection.prepareStatement("delete from torrent_robots_txt where timestamp < ?");
                statement.setLong(1, twoWeeksAgo);
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}