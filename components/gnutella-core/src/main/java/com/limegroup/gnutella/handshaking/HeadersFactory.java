package com.limegroup.gnutella.handshaking;

import java.util.Properties;

public interface HeadersFactory {

    public Properties createLeafHeaders(String remoteIP);

    public Properties createUltrapeerHeaders(String remoteIP);

}