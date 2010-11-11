package org.limewire.core.api.network;

import java.io.IOException;

public interface NetworkManager {

    public void setIncomingTLSEnabled(boolean value);
    
    public void setOutgoingTLSEnabled(boolean value);
    
    public boolean isIncomingTLSEnabled();
    
    public boolean isOutgoingTLSEnabled();
    
    public void setListeningPort(int port) throws IOException;
    
    public void portChanged();
    
    public boolean addressChanged();
    
    public void validateTLS();
    
    public byte[] getExternalAddress();
}
