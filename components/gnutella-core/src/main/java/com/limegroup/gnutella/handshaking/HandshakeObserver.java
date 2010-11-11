package com.limegroup.gnutella.handshaking;

import org.limewire.nio.observer.Shutdownable;

public interface HandshakeObserver extends Shutdownable {
    public void handleNoGnutellaOk(int code, String msg);
    public void handleBadHandshake();
    public void handleHandshakeFinished(Handshaker shaker);

}
