package com.limegroup.gnutella.connection;

public interface UDPConnectionChecker {

    /**
     * @return if we think that udp traffic is dead
     */
    boolean udpIsDead();
    
}
