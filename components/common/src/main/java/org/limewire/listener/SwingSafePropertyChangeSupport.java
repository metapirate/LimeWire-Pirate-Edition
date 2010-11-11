package org.limewire.listener;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A {@link PropertyChangeSupport} that automatically calls
 * {@link PropertyChangeListener#propertyChange(PropertyChangeEvent)} from
 * within the Swing thread if the listener is added on the Swing thread. 
 */
public class SwingSafePropertyChangeSupport extends PropertyChangeSupport {

    public SwingSafePropertyChangeSupport(Object sourceBean) {
        super(sourceBean);
    }

    @Override
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return unwrap(super.getPropertyChangeListeners());
    }

    @Override
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return unwrap(super.getPropertyChangeListeners(propertyName));
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if(EventQueue.isDispatchThread()) {
            listener = wrap(listener);
        }
        super.addPropertyChangeListener(listener);
    }

    @Override
    public synchronized void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        if(EventQueue.isDispatchThread()) {
            listener = wrap(listener);
        }        
        super.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        listener = wrap(listener, super.getPropertyChangeListeners());
        super.removePropertyChangeListener(listener);
    }

    @Override
    public synchronized void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        listener = wrap(listener, super.getPropertyChangeListeners(propertyName));
        super.removePropertyChangeListener(propertyName, listener);
    }

    /** Wraps toFind in the appropriate SwingSafePropertyChangeListener, finding it from the list of all. */
    private PropertyChangeListener wrap(PropertyChangeListener toFind, PropertyChangeListener[] all) {
        for(PropertyChangeListener listener : all) {
            if(listener == toFind) {
                return listener;
            } else if(listener instanceof SwingSafePropertyChangeListener) {
                if(((SwingSafePropertyChangeListener)listener).delegate == toFind) {
                    return listener;
                }
            }
        }
        return toFind;
    }

    /** Wraps the listener. */
    private PropertyChangeListener wrap(PropertyChangeListener listener) {
        return new SwingSafePropertyChangeListener(listener);
    }
    
    /** Unwraps all SwingSafePropertyChangeListeners. */
    private PropertyChangeListener[] unwrap(PropertyChangeListener[] propertyChangeListeners) {
        PropertyChangeListener[] newListeners = null;
        for(int i = 0; i < propertyChangeListeners.length; i++) {
            PropertyChangeListener listener = propertyChangeListeners[i];
            if(listener instanceof SwingSafePropertyChangeListener) {
                if(newListeners == null) {
                    newListeners = new PropertyChangeListener[propertyChangeListeners.length];
                    System.arraycopy(propertyChangeListeners, 0, newListeners, 0, newListeners.length);
                }
                newListeners[i] = ((SwingSafePropertyChangeListener)listener).delegate;
            }
        }
        return newListeners == null ? propertyChangeListeners : newListeners;
    }
    
    private static class SwingSafePropertyChangeListener implements PropertyChangeListener {
        private final PropertyChangeListener delegate;
        
        public SwingSafePropertyChangeListener(PropertyChangeListener delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if(EventQueue.isDispatchThread()) {
                delegate.propertyChange(evt);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        delegate.propertyChange(evt);
                    }
                });
            }
        }
    }

}
