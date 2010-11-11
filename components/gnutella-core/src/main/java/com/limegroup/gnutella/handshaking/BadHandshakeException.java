package com.limegroup.gnutella.handshaking;

import java.io.IOException;

public class BadHandshakeException extends IOException
{
    
    public BadHandshakeException(IOException originalCause)
    {
        super();
        initCause(originalCause);
    }
}

