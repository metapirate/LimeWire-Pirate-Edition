package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.Vector;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.inject.GuiceUtils;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager.ConnectType;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * The command-line UI for the Gnutella servent.
 */
public class Main {
    
    @Inject private LifecycleManager lifecycleManager;
    @Inject private NetworkManager networkManager;
    @Inject private ConnectionServices connectionServices;
    @Inject private SearchServices searchServices;
    
    
    public static void main(String args[]) {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new LimeWireCoreModule(MainCallback.class));
        GuiceUtils.loadEagerSingletons(injector);
        Main main = injector.getInstance(Main.class);
        main.start();
    }
    
    private void start() {
        lifecycleManager.start();
        
        System.out.println("For a command list type help.");
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        for ( ; ;) {
            System.out.print("LimeRouter> ");
            try {
                String command=in.readLine();
                if (command==null)
                    break;
                else if (command.equals("help")) {
                    System.out.println("catcher                  "+
                                       "Print host catcher.");
                    System.out.println("connect <host> [<port>]  "+
                                       "Connect to a host[:port].");
                    System.out.println("help                     "+
                                       "Print this message.");
                    System.out.println("listen <port>            "+
                                       "Set the port you are listening on.");
                    //              System.out.println("push                     "+
                    //                "Print push routes.");
                    System.out.println("query <string>           "+
                                       "Send a query to the network.");
                    System.out.println("quit                     "+
                                       "Quit the application.");
                    //              System.out.println("route                    "+
                    //                "Print routing tables.");
                    //              System.out.println("stat                     "+
                    //                "Print statistics.");
                    System.out.println("update                   "+
                                       "Send pings to update the statistics.");
                }
                else if (command.equals("quit"))
                    break;
                //          //Print routing tables
                //          else if (command.equals("route"))
                //              RouterService.dumpRouteTable();
                //          //Print connections
                //          else if (command.equals("push"))
                //              RouterService.dumpPushRouteTable();
                //Print push route
            
                String[] commands=split(command);
                //Connect to remote host (establish outgoing connection)
                if (commands.length>=2 && commands[0].equals("connect")) {
                    try {
                        int port=6346;
                        if (commands.length>=3)
                            port=Integer.parseInt(commands[2]);
                        System.out.println("Connecting...");
                        connectionServices.connectToHostAsynchronously(commands[1], port, ConnectType.PLAIN);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    }
                } else if (commands.length>=2 && commands[0].equals("query")) {
                    //Get query string from command (possibly multiple words)
                    int i=command.indexOf(' ');
                    assert(i!=-1 && i<command.length());
                    String query=command.substring(i+1);
                    searchServices.query(searchServices.newQueryGUID(), query);
                } else if (commands.length==2 && commands[0].equals("listen")) {
                    try {
                        int port=Integer.parseInt(commands[1]);
                        networkManager.setListeningPort(port);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    } catch (IOException e) {
                        System.out.println("Couldn't change port.  Try another value.");
                    }
                }
            } catch (IOException e) {
                System.exit(1);
            }
        }
        System.out.println("Good bye.");
        lifecycleManager.shutdown(); //write gnutella.net
    }
    
    /** Returns an array of strings containing the words of s, where
     *  a word is any sequence of characters not containing a space.
     */
    public static String[] split(String s) {
        s=s.trim();
        int n=s.length();
        if (n==0)
            return new String[0];
        Vector<String> buf=new Vector<String>();

        //s[i] is the start of the word to add to buf
        //s[j] is just past the end of the word
        for (int i=0; i<n; ) {
            assert(s.charAt(i)!=' ');
            int j=s.indexOf(' ',i+1);
            if (j==-1)
                j=n;
            buf.add(s.substring(i,j));
            //Skip past whitespace (if any) following s[j]
            for (i=j+1; j<n ; ) {
                if (s.charAt(i)!=' ')
                    break;
                i++;
            }
        }
        String[] ret=new String[buf.size()];
        for (int i=0; i<ret.length; i++)
            ret[i]= buf.get(i);
        return ret;
    }

    
    
    @Singleton
    private static class MainCallback implements ActivityCallback {

        /////////////////////////// ActivityCallback methods //////////////////////
    
        /**
         *  Add a query string to the monitor screen
         */
        @Override
        public void handleQuery(QueryRequest query, String address, int port) {
        }
    
        ///////////////////////////////////////////////////////////////////////////

        @Override
        public void addDownload(Downloader mgr) {}
    
        @Override
        public void removeDownload(Downloader mgr) {}
    
        @Override
        public void addUpload(Uploader mgr) {}
    
        @Override
        public void uploadComplete(Uploader mgr) {}
        
        @Override
        public void handleSharedFileUpdate(File file) {}
    
        @Override
        public void downloadsComplete() {}    
        
        @Override
        public void uploadsComplete() {}
    
        @Override
        public void promptAboutUnscannedPreview(Downloader dloader) {
            dloader.discardUnscannedPreview(false);
        }
        
        @Override public void restoreApplication() {}
    
        @Override public boolean isQueryAlive(GUID guid) {
            return false;
        }
        
        @Override public void handleMagnets(final MagnetOptions[] magnets) {
        }
    
        @Override public void handleTorrent(File torrentFile){}
    	
        @Override public void installationCorrupted() {
            
        }
        @Override public String translate(String s) { return s;}

        @Override
        public void handleDownloadException(DownloadAction downLoadAction,
                DownloadException e, boolean supportsNewSaveDir) {
            
        }

        @Override
        public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            synchronized(System.out) {
                System.out.println("Query hit from "+rfd.getAddress() + ":");
                System.out.println("   "+rfd.getFileName());
            }
        }
        
        @Override
        public boolean promptTorrentFilePriorities(Torrent torrent) {
            return true;
        }

        @Override
        public boolean promptAboutTorrentWithBannedExtensions(Torrent torrent, Set<String> bannedExtensions) {
            return true;
        }

        @Override
        public boolean promptAboutTorrentDownloadWithFailedScan() {
            return true;
        }
    }
}
