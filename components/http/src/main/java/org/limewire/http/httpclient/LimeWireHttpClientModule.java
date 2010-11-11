package org.limewire.http.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.net.SocketsManager;
import org.limewire.nio.NBSocket;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class LimeWireHttpClientModule extends AbstractModule {
    
    /**
     * The amount of time to wait while trying to connect to a specified
     * host via TCP.  If we exceed this value, an IOException is thrown
     * while trying to connect.
     */
    private static final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * The amount of time to wait while receiving data from a specified
     * host.  Used as an SO_TIMEOUT.
     */
    private static final int TIMEOUT = 8000;
    
    /**
     * The maximum number of times to allow redirects from hosts.
     */
    private static final int MAXIMUM_REDIRECTS = 10;
    
    @Override
    protected void configure() {
        // everything provided by provider methods
    }
    
    @Provides LimeHttpClient limeClient(@Named("nonBlockingConnectionManager") ReapingClientConnectionManager manager, @Named("defaults") Provider<HttpParams> defaultParams) {
        return new LimeHttpClientImpl(manager, defaultParams);
    }
    
    @Provides HttpClient simpleClient(Provider<LimeHttpClient> limeClient) {
        return limeClient.get();
    }
    
    @Provides @Named("limeSchemeRegistry") SchemeRegistry lsr(Provider<SocketsManager> socketsManager) {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.PLAIN), 80));
        registry.register(new Scheme("tls", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80));
        registry.register(new Scheme("https", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.SSL), 443)); 
        return registry;
    }
    
    @Provides @Singleton @Named("nonBlockingConnectionManager") ReapingClientConnectionManager nbcm(@Named("limeSchemeRegistry")Provider<SchemeRegistry> registry, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler, @Named("defaults") Provider<HttpParams> defaultParams) {
        return new ReapingClientConnectionManager(registry, scheduler, defaultParams);
    }
    
    @Provides @Named("defaults") HttpParams dp() {            
        BasicHttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClientParams.setRedirecting(params, true);
        params.setIntParameter(ClientPNames.MAX_REDIRECTS, MAXIMUM_REDIRECTS);
        return params;
    }
    
    private static class LimeSocketFactory implements SocketFactory {
        final Provider<SocketsManager> socketsManager;
        final SocketsManager.ConnectType type;
        
        public LimeSocketFactory(Provider<SocketsManager> socketsManager, SocketsManager.ConnectType type) {
            this.socketsManager = socketsManager;
            this.type = type;
        }

        public Socket createSocket() throws IOException {
            return socketsManager.get().create(type);
        }

        public Socket connectSocket(Socket socket, String targetHost, int targetPort, InetAddress localAddress, int localPort, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
            if(socket == null) {
                socket = createSocket();
            }
            InetSocketAddress localSocketAddr = null;
            if((localAddress != null && !localAddress.isAnyLocalAddress()) || localPort > 0) {
                localSocketAddr = new InetSocketAddress(localAddress, localPort);
            }
            return socketsManager.get().connect((NBSocket)socket, localSocketAddr, new InetSocketAddress(targetHost,targetPort), HttpConnectionParams.getConnectionTimeout(httpParams), type);
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false; // TODO type.equals(SocketsManager.ConnectType.TLS);  // TODO use socket instead?
        }        
    }
    
}
