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

package org.limewire.mojito.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.settings.KademliaSettings;


public class MojitoUtils {
    
    private MojitoUtils() {
        
    }
    
    /**
     * A helper method to bootstrap a MojitoDHT instance.
     * <p>
     * It tries to ping the given SocketAddress (this blocks) and in
     * case of a success it will kick off a bootstrap process and returns
     * a DHTFuture for the process.
     */
    public static DHTFuture<BootstrapResult> bootstrap(MojitoDHT dht, SocketAddress addr) 
            throws ExecutionException, InterruptedException {
        PingResult pong = dht.ping(addr).get();
        return dht.bootstrap(pong.getContact());
    }
    
    /**
     * Creates <code>factor</code> * {@link KademliaSettings#REPLICATION_PARAMETER} bootstrapped DHTs
     * and stores each under its node id in a map.
     * <p>
     * Instances are bound from port 3000 onwards.
     * <p>
     * Make sure to close them in a try-finally block.
     */
    public static List<MojitoDHT> createBootStrappedDHTs(int factor) throws Exception {
        return createBootStrappedDHTs(factor, 3000);
    }
        
    /**
     * Creates <code>factor</code> * {@link KademliaSettings#REPLICATION_PARAMETER} bootstrapped DHTs.
     * <p>
     * Make sure to close them in a try-finally block.
     * 
     * @param port the port offset to start binding the instances on
     */
    public static List<MojitoDHT> createBootStrappedDHTs(int factor, int port) throws Exception {
        if (factor < 1) {
            throw new IllegalArgumentException("only values >= 1");
        }
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        
        for (int i = 0; i < factor * k; i++) {
            MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
            
            dht.bind(new InetSocketAddress(port + i));
            dht.start();
            
            if (i > 0) {
                dht.bootstrap(new InetSocketAddress("localhost", port)).get();
            }
            dhts.add(dht);
        }
        dhts.get(0).bootstrap(dhts.get(1).getContactAddress()).get();
        return dhts;
    }
    
    /**
     * Creates <code>factor</code> * {@link KademliaSettings#REPLICATION_PARAMETER} bootstrapped DHTs
     * and stores each under its node id in a map.
     * <p>
     * Instances are bound from port 3000 onwards.
     * <p>
     * Make sure to close them in a try-finally block.
     */
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(int factor) throws Exception {
        return createBootStrappedDHTsMap(factor, 3000);
    }
    
    /**
     * Creates <code>factor</code> * {@link KademliaSettings#REPLICATION_PARAMETER} bootstrapped DHTs
     * and stores each under its node id in a map.
     * <p>
     * Make sure to close them in a try-finally block.
     * 
     * @param port the port offset to start binding the instances on
     */
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(int factor, int port) throws Exception {
        if (factor < 1) {
            throw new IllegalArgumentException("only values >= 1");
        }
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        Map<KUID, MojitoDHT> dhts = new LinkedHashMap<KUID, MojitoDHT>();
        MojitoDHT first = null;
        
        for (int i = 0; i < factor * k; i++) {
            MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
            
            dht.bind(new InetSocketAddress(port + i));
            dht.start();
            
            if (i > 0) {
                dht.bootstrap(new InetSocketAddress("localhost", port)).get();
            } else {
                first = dht;
            }
            dhts.put(dht.getLocalNodeID(), dht);
        }
        if (first != null) { // unnecessary null check to satisfy compiler, although if k was null it would be necessary
            first.bootstrap(new InetSocketAddress("localhost", 3000 + 1)).get();
        }
        return dhts;
    }
}
