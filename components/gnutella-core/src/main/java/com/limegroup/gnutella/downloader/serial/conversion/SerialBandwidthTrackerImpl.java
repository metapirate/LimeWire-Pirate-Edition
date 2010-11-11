package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("unused")
class SerialBandwidthTrackerImpl implements Serializable {

    private static final long serialVersionUID = 7694080781117787305L;
        
    private long lastTime;
    private int lastAmountRead;
    private float measuredBandwidth;
    
    private void writeObject(ObjectOutputStream out) throws IOException {}
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException { 
        in.defaultReadObject();
    }
    
    
    
}
