package com.limegroup.gnutella.tigertree;

import java.io.IOException;

import org.limewire.nio.statemachine.IOState;


public interface ThexReader extends IOState {    
    public HashTree getHashTree() throws IOException;
}