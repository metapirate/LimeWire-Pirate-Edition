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
 
package org.limewire.mojito;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.concurrent.Future;

import org.limewire.io.SecureInputStream;
import org.limewire.io.SecureOutputStream;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindNodeResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.util.CollectionUtils;

/**
 * Executes various features of the Mojito DHT. <code>CommandHandler</code>
 * is useful in running the Mojito DHT from the command line.
 * Additionally, <code>CommandHandler</code> writes information about 
 * the {@link MojitoDHT} to a {@link PrintWriter}. 
 */
public class CommandHandler {
    
    private static final String[] COMMANDS = {
            "help",
            "info",
            "ping .+ \\d{1,5}",
            "bootstrap .+ \\d{1,5}",
            "put (key|kuid) (\\w|\\d)+ (value|file) .+",
            "remove (key|kuid) (\\w|\\d)+",
            "get (key|kuid) (\\w|\\d)+",
            "lookup (key|kuid) (\\w|\\d)+",
            "database",
            "publisher",
            "routetable",
            "store .+",
            "load .+",
            "kill",
            "stats",
            "restart",
            "firewalled",
            "exhaustive",
            "id .+",
            "select .+",
            "nextid",
            "rt_gui",
            "arcs_gui",
            "test",
            "bootstrapped"
    };
    
    public static boolean handle(MojitoDHT dht, String command, PrintWriter out) {
        try {
            command = command.trim();
            for (String c : COMMANDS) {
                if (command.matches(c)) {
                    String[] args = command.split(" ");
                    Method m = CommandHandler.class.getDeclaredMethod(args[0], MojitoDHT.class, String[].class, PrintWriter.class);
                    m.invoke(null, dht, args, out);
                    return true;
                }
            }
        } catch (NoSuchMethodException err) {
            err.printStackTrace(out);
        } catch (InvocationTargetException err) {
            err.printStackTrace(out);
        } catch (IllegalAccessException err) {
            err.printStackTrace(out);
        } finally {
            out.flush();
        }
        
        return false;
    }
    
    public static void help(MojitoDHT dht, String[] args, PrintWriter out) {
        for (String c : COMMANDS) {
            out.println(c);
        }
    }
    
    public static void info(MojitoDHT dht, String[] args, PrintWriter out) {
        out.println(dht.toString());
    }
    
    public static void exhaustive(MojitoDHT dht, String[] args, PrintWriter out) {
        boolean current = LookupSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue();
        LookupSettings.EXHAUSTIVE_VALUE_LOOKUP.setValue(
                !LookupSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
        
        out.println("Exhaustive: " + current + " -> " + LookupSettings.EXHAUSTIVE_VALUE_LOOKUP.getValue());
    }
    
    public static void firewalled(MojitoDHT dht, String[] args, PrintWriter out) {
        ((LocalContact)dht.getLocalNode()).setFirewalled(!dht.isFirewalled());
        out.println("Firewalled: " + dht.isFirewalled());
    }
    
    public static void database(MojitoDHT dht, String[] args, PrintWriter out) {
        StringBuilder buffer = new StringBuilder("\n");
        Database database = dht.getDatabase();
        buffer.append(database.toString());
        out.println(buffer);
    }
    
    public static void publisher(MojitoDHT dht, String[] args, PrintWriter out) {
        StringBuilder buffer = new StringBuilder("\n");
        StorableModelManager modelManager = dht.getStorableModelManager();
        buffer.append(modelManager.toString());
        out.println(buffer);
    }
    
    public static void routetable(MojitoDHT dht, String[] args, PrintWriter out) {
        StringBuilder buffer = new StringBuilder("\n");
        RouteTable routingTable = dht.getRouteTable();
        buffer.append(routingTable.toString());
        out.println(buffer);
    }
    
    public static Future<PingResult> ping(MojitoDHT dht, String[] args, final PrintWriter out) {
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        out.println("Pinging... " + addr);
        
        Future<PingResult> future = dht.ping(addr);
        try {
            PingResult result = future.get();
            out.println(result);
        } catch (Exception err) {
            err.printStackTrace(out);
        }
        out.flush();
        
        return future;
    }    
    
    public static void bootstrap(MojitoDHT dht, String[] args, final PrintWriter out) {
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        
        SocketAddress addr = new InetSocketAddress(host, port);
        
        out.println("Bootstrapping... " + addr);
        try {
            PingResult pong = dht.ping(addr).get();
            BootstrapResult result = dht.bootstrap(pong.getContact()).get();
            out.println("Bootstraping finished:\n" + result);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
    
    public static void put(MojitoDHT dht, String[] args, final PrintWriter out) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            byte[] value = null;
            
            if (args[1].equals("kuid")) {
                key = KUID.createWithHexString(args[2]);
            } else {
                key = KUID.createWithBytes(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            if (args[3].equals("value")) {
                value = args[4].getBytes("UTF-8");
            } else if (args[3].equals("file")) {
                File file = new File(args[4]);
                byte[] data = new byte[(int)Math.min(1024, file.length())];
                FileInputStream in = new FileInputStream(file);
                in.read(data, 0, data.length);
                in.close();
                value = data;
            }
            md.reset();
            
            out.println("Storing... " + key);
            
            StoreResult evt = dht.put(key, new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value)).get();
            StringBuilder buffer = new StringBuilder();
            buffer.append("STORE RESULT:\n");
            buffer.append(evt.toString());
            out.println(buffer.toString());
        } catch (Exception err) {
            err.printStackTrace(out);
        }
    }
    
    public static void remove(MojitoDHT dht, String[] args, final PrintWriter out) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            if (args[1].equals("kuid")) {
                key = KUID.createWithHexString(args[2]);
            } else {
                key = KUID.createWithBytes(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            out.println("Removing... " + key);
            
            StoreResult evt = dht.remove(key).get();
            StringBuilder buffer = new StringBuilder();
            buffer.append("REMOVE RESULT:\n");
            buffer.append(evt.toString());
            out.println(buffer.toString());
            
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
    
    public static void get(MojitoDHT dht, String[] args, PrintWriter out) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            if (args[1].equals("kuid")) {
                key = KUID.createWithHexString(args[2]);
            } else {
                key = KUID.createWithBytes(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            EntityKey lookupKey = EntityKey.createEntityKey(key, DHTValueType.ANY);
            FindValueResult evt = dht.get(lookupKey).get();
            out.println(evt.toString());
            
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
    
    public static void lookup(MojitoDHT dht, String[] args, PrintWriter out) {
    	try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            
            KUID key = null;
            if (args[1].equals("kuid")) {
                key = KUID.createWithHexString(args[2]);
            } else {
                key = KUID.createWithBytes(md.digest(args[2].getBytes("UTF-8")));
            }
            md.reset();
            
            FindNodeResult evt = ((Context)dht).lookup(key).get();
            out.println(evt.toString());
            
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
    
    public static MojitoDHT load(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        File file = new File(args[1]);
        out.println("Loading: " + file);
        ObjectInputStream ois = null;
        
        try {
            ois = new ObjectInputStream(
                    new BufferedInputStream(
                        new SecureInputStream(
                            new FileInputStream(file))));
            RouteTable routeTable = (RouteTable)ois.readObject();
            Database database = (Database)ois.readObject();
            
            MojitoDHT mojito = MojitoFactory.createDHT(dht.getName());
            synchronized (mojito) {
                mojito.setRouteTable(routeTable);
                mojito.setDatabase(database);
            }
            return mojito;
        } finally {
            try {
                if (ois != null) { ois.close(); }
            } catch (IOException ignore) {}
        }
    }
    
    public static void store(MojitoDHT dht, String[] args, PrintWriter out) throws IOException {
        File file = new File(args[1]);
        out.println("Storing: " + file);
        ObjectOutputStream oos = null;
        
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                        new SecureOutputStream(
                            new FileOutputStream(file))));
            synchronized (dht) {
                oos.writeObject(dht.getRouteTable());
                oos.writeObject(dht.getDatabase());
            }
            oos.flush();
        } finally {
            try {
                if (oos != null) { oos.close(); }
            } catch (IOException ignore) {}
        }
    }
    
    public static void id(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        KUID nodeId = KUID.createWithHexString(args[1]);
        out.println("Setting NodeID to: " + nodeId);
        Method m = dht.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
        m.setAccessible(true);
        m.invoke(dht, new Object[]{nodeId});
    }
    
    public static void select(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        KUID nodeId = KUID.createWithHexString(args[1]);
        out.println("Selecting: " + nodeId);
        
        RouteTable routeTable = ((Context)dht).getRouteTable();
        out.println(CollectionUtils.toString(routeTable.select(nodeId, 20, SelectMode.ALL)));
    }
    
    public static void nextid(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        ((Context)dht).getLocalNode().nextInstanceID();
    }
    
    public static void kill(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        if (dht.isRunning()) {
            out.println("Stopping " + dht.getName());
            dht.stop();
        }
    }
    
    public static void restart(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        if (!dht.isRunning()) {
            out.println("Starting " + dht.getName());
            dht.start();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void rt_gui(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        Class clazz = Class.forName("org.limewire.mojito.visual.RouteTableVisualizer");
        Method show = clazz.getDeclaredMethod("show", Context.class);
        show.invoke(null, dht);
    }
    
    @SuppressWarnings("unchecked")
    public static void arcs_gui(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
        Class clazz = Class.forName("org.limewire.mojito.visual.ArcsVisualizer");
        Method show = clazz.getDeclaredMethod("show", Context.class);
        show.invoke(null, dht);
    }
    
    public static void bootstrapped(MojitoDHT dht, String[] args, PrintWriter out) throws Exception {
    	Context context = (Context)dht;
    	
    	boolean bootstrapped = context.isBootstrapped();
    	context.setBootstrapped(!bootstrapped);
        out.println(bootstrapped + " -> " + (!bootstrapped));
    }
}
