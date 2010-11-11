package com.limegroup.gnutella.connection;

import java.net.Socket;

import org.limewire.net.SocketsManager.ConnectType;


public interface RoutedConnectionFactory {

    public RoutedConnection createRoutedConnection(String host, int port);

    public RoutedConnection createRoutedConnection(String host, int port,
            ConnectType type);

    public RoutedConnection createRoutedConnection(Socket socket);

}