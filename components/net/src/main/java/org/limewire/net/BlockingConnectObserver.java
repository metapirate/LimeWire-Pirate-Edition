package org.limewire.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.OnewayExchanger;
import org.limewire.nio.observer.ConnectObserver;

/**
 * This class allows blocking access to socket connections as done by
 * {@link SocketsManager}.
 */
public class BlockingConnectObserver implements ConnectObserver {

    private final OnewayExchanger<Socket, IOException> exchanger = new OnewayExchanger<Socket, IOException>();
    
    @Override
    public void handleConnect(Socket socket) throws IOException {
        exchanger.setValue(socket);
    }

    @Override
    public void handleIOException(IOException iox) {
        exchanger.setException(iox);
    }

    @Override
    public void shutdown() {
        exchanger.setException(new IOException("shut down"));
    }
    
    public Socket getSocket() throws IOException {
        try {
            return exchanger.get();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }
    
    public Socket getSocket(long timeout, TimeUnit timeUnit) throws IOException, TimeoutException {
        try {
            return exchanger.get(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
