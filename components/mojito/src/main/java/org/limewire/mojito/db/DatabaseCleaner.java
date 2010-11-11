/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.db;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.DatabaseSettings;

/**
 * Removes expired values from the local database.
 */
public class DatabaseCleaner implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(DatabaseCleaner.class);
    
    private final Context context;
    
    private ScheduledFuture future;
    
    public DatabaseCleaner(Context context) {
        this.context = context;
    }
    
    /**
     * Starts the <code>DatabaseCleaner</code>.
     */
    public synchronized void start() {
        if (future == null) {
            long delay = DatabaseSettings.DATABASE_CLEANER_PERIOD.getValue();
            long initialDelay = delay;
            
            future = context.getDHTExecutorService()
                .scheduleWithFixedDelay(this, initialDelay, delay, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Stops the <code>DatabaseCleaner</code>.
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }
    
    /**
     * Removes all expired <code>DHTValueEntity</code> from the <code>Database</code>.
     */
    private void cleanupDatabase() {
        EvictorManager evictorManager = context.getEvictorManager();
        RouteTable routeTable = context.getRouteTable();
        Database database = context.getDatabase();
        synchronized (database) {
            for (DHTValueEntity entity : database.values()) {
                if (evictorManager.isExpired(routeTable, entity)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(entity + " is expired!");
                    }
                    
                    database.remove(entity.getPrimaryKey(), entity.getSecondaryKey());
                }
            }
        }
    }
    
    public void run() {
        cleanupDatabase();
    }
}
