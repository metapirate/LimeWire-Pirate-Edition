package org.limewire.core.api.mojito;

import java.beans.PropertyChangeListener;
import java.io.PrintWriter;

/**
 * Defines the manager interface for the Mojito DHT.
 */
public interface MojitoManager {
    /** Property name for DHT started value. */
    public static final String DHT_STARTED = "started";

    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes. 
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes. 
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Returns the name of the DHT.
     */
    public String getName();
    
    /**
     * Returns true if the Mojito DHT is running.
     */
    public boolean isRunning();
    
    /**
     * Invokes the specified command on the Mojito DHT, and forwards output
     * to the specified PrintWriter. 
     */
    public boolean handle(String command, PrintWriter out);
    
}
