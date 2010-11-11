package org.limewire.mojito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.BootstrapResult.ResultType;
import org.limewire.mojito.settings.BucketRefresherSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.MojitoUtils;

public class Main {    
    /**
     * Creates a localhost Mojito DHT. Once the localhost Mojito DHT's 
     * nodes are bootstrapped, you can execute various Mojito commands, 
     * like adding values to the DHT using a key, looking up keys, getting 
     * values using the key, etc.
     * 
     * @param args 'count port [host port]' either two or four arguments to 
     * setup a localhost Mojito DHT.
     * <ul>
     * <li>count: number of nodes in the example DHT; excludes the bootstrapped 
     * node.
     * <li>port: the base port for nodes added to the DHT. Each additional node (if 
     * count is greater than one) uses a port number which is an increment of the 
     * base port. * <li>host: host name/host number for the bootstrapped node.
     * <li>port: the port for the bootstrap node's port. 
     * </ul>
     * @throws Exception
     * <p>
     * See <a href="http://wiki.limewire.org/index.php?title=Mojito_Main">
     * Mojito Main</a> for more information on using this example code. 
     */
    public static void main(String[] args) throws Exception {
        
        int count = 0;
        int port = 0;
        
        //Allows for running locally       
        ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(false));        
        
        InetSocketAddress bootstrapHost = null;
        
        if (args.length != 2 && args.length != 4) {
            System.out.println("java Main count port");
            System.out.println("java Main count port host port");
            System.exit(-1);
        } else {
            count = Integer.parseInt(args[0]);
            port = Integer.parseInt(args[1]);
            
            if (args.length >= 4) {                
                bootstrapHost = new InetSocketAddress(args[2], Integer.parseInt(args[3]));
            }
        }
        
        RouteTableSettings.MAX_CONTACTS_PER_NETWORK_CLASS_RATIO.setValue(1.0f);
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.FILTER_CLASS_C.setValue(false);
        
        List<MojitoDHT> dhts = standalone(null, port, count);
        
        if (dhts.isEmpty()) {
            System.out.println("No Nodes to run");
            System.exit(0);
        }
        
        if (!NetworkSettings.LOCAL_IS_PRIVATE.getValue()) {
            System.out.println("WARNING: LOCAL_IS_PRIVATE is set to false!");
        }
        
        if (!NetworkSettings.FILTER_CLASS_C.getValue()) {
            System.out.println("WARNING: FILTER_CLASS_C is set to false!");
        }
        
        if (RouteTableSettings.MAX_CONTACTS_PER_NETWORK_CLASS_RATIO.getValue() >= 1.0f) {
            System.out.println("WARNING: MAX_CONTACTS_PER_NETWORK_CLASS_RATIO is set to >= 1.0f!");
        }
        
        run(port, dhts, bootstrapHost);
    }
    
    private static List<MojitoDHT> standalone(InetAddress addr, int port, int count) {
        if (count > 1) {
            BucketRefresherSettings.UNIFORM_BUCKET_REFRESH_DISTRIBUTION.setValue(true);
        }
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>(count);
        
        for(int i = 0; i < count; i++) {
            try {
                MojitoDHT dht = MojitoFactory.createDHT("DHT" + i);
                
                if (addr != null) {
                    dht.bind(new InetSocketAddress(addr, port+i));
                } else {
                    dht.bind(new InetSocketAddress(port+i));
                }
                
                dhts.add(dht);
                System.out.println(i + ": " + ((Context)dhts.get(dhts.size()-1)).getLocalNode());
            } catch (IOException err) {
                System.err.println("Failed to start/connect DHT #" + i);
                err.printStackTrace();
            }
        }
        
        return dhts;
    }

    private static void run(int port, List<MojitoDHT> dhts, SocketAddress bootstrapHost) throws Exception {
        long time = 0L;
                
        int start = 0;
        if (bootstrapHost == null) {
            dhts.get(0).start();
            
            start = 1;
            bootstrapHost = new InetSocketAddress("localhost", port);

        } 

        for(int i = start; i < dhts.size(); i++) {
            try {
                MojitoDHT dht = dhts.get(i);
                dht.start();
                
                BootstrapResult result = dht.bootstrap(bootstrapHost).get();
                
                time += result.getTime();
                
                if (result.getResultType().equals(ResultType.BOOTSTRAP_SUCCEEDED)) {    
                    System.out.println("Node #" + i + " finished bootstrapping from " 
                            + bootstrapHost + " in " + result.getTime() + "ms");
                } else {
                    System.out.println("Node #" + i + " failed to bootstrap from " 
                            + bootstrapHost + "\n" + result);
                }
            } catch (Exception err) {
                System.out.println("Node #" + i + " failed to bootstrap from " + bootstrapHost);
                err.printStackTrace();
            }
        }

        if (dhts.size() > 1) {
            BootstrapResult result = MojitoUtils.bootstrap(
                    dhts.get(0), dhts.get(1).getContactAddress()).get();
            time += result.getTime();
            
            if (result.getResultType().equals(ResultType.BOOTSTRAP_SUCCEEDED)) {    
                System.out.println("Node #0 finished bootstrapping from " 
                        + dhts.get(1).getContactAddress() + " in " + result.getTime() + "ms");
            } else {
                System.out.println("Node #0 failed to bootstrap from " 
                        + dhts.get(1).getContactAddress() + "\n" + result);
            }
        }
        
        System.out.println("All Nodes finished bootstrapping in " + time + "ms");
        
        int current = 0;
        MojitoDHT dht = dhts.get(current);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        
        while(true) {
            System.out.print("[" + current + "] $ ");
            String line = in.readLine();
            
            // Null if app gets interrupted by Eclipse
            if (line == null) {
                break;
            }
            
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            
            try {
                if (line.indexOf("quit") >= 0) {
                    for(int i = 0; i < dhts.size(); i++) {
                        dhts.get(i).close();
                    }
                    System.exit(0);
                } else if (line.indexOf("switch") >= 0) {
                    int index = Integer.parseInt(line.split(" ")[1]);
                    dht = dhts.get(index);
                    current = index;
                    CommandHandler.info(dht, null, out);
                } else if (line.indexOf("help") >= 0) {
                    out.println("quit");
                    out.println("switch <index>");
                    CommandHandler.handle(dht, line, out);
                } else if (line.indexOf("load") >= 0) {
                    MojitoDHT d = CommandHandler.load(dht, line.split(" "), out);
                    dht.close();
                    dhts.set(current, d);
                    d.getDHTExecutorService().setThreadFactory(
                            dht.getDHTExecutorService().getThreadFactory());
                    d.bind(dht.getLocalAddress());
                    d.start();
                    dht = d;
                    CommandHandler.info(dht, null, out);
                } else if (line.indexOf("keycount") >= 0) {
                    int keycount = 0;
                    for (MojitoDHT mojito : dhts) {
                        Database db = ((Context)mojito).getDatabase();
                        keycount += db.getKeyCount();
                    }
                    System.out.println("Key count: " + keycount);
                } else {
                    if(!CommandHandler.handle(dht, line, out))
                        System.out.println("Could not execute command: " + line);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                out.flush();
            }
        }
    }
}
