package org.limewire.rudp;

import java.lang.ref.WeakReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**<p>
 * An abstract class that provides a timed task to be repeated and rescheduled 
 * as needed. You must implement {@link #doActualEvent(UDPConnectionProcessor)}
 * to call an appropriate {@link UDPConnectionProcessor} method.
 * </p>
 * When done with the event, unregister it.
 */
public abstract class UDPTimerEvent implements Comparable<UDPTimerEvent> {

    private static final Log LOG =
        LogFactory.getLog(UDPTimerEvent.class);

    /** The currently scheduled time. */
    protected volatile long _eventTime;
    
    private volatile boolean _shouldUnregister;
    
    /** the UDPConnectionProcessor this event refers to. */
    protected final WeakReference<UDPConnectionProcessor> _udpCon;

   /**
    *  Create a timer event with a default time.
    */
    UDPTimerEvent(long eventTime, UDPConnectionProcessor conn) {
        _eventTime = eventTime;
        _udpCon= new WeakReference<UDPConnectionProcessor>(conn);
    }
    
    /**
     * Checks whether the UDPConnectionProcessor has been finalized and if so,
     * unregisters this event from the given scheduler
     * Also checks if this is event wants to unregister itself
     * @return whether the UDPConnectionProcessor was unregistered.
     */
    final boolean shouldUnregister() {

        if (_udpCon.get() == null || _shouldUnregister) {
            LOG.debug("Event decided to unregister itself");
            return  true;
        }

        return false;
    }
    
    protected final void unregister() {
        _shouldUnregister=true;
        _eventTime=1;
    }

   /**
    *  Change the time that an event is scheduled at.  Note to recall scheduler.
    */
    public void updateTime(long updatedEventTime) {
    	if (!_shouldUnregister)
    		_eventTime = updatedEventTime;
    }

   /**
    *  Return the time that an event should take place in milliseconds.
    */
    public long getEventTime() {
        return _eventTime;
    }

  
    public final void handleEvent(){
        UDPConnectionProcessor udpCon = _udpCon.get();
        
        if (udpCon==null)
            return;
        
        doActualEvent(udpCon);
    }
    
    /**
     *  Implementors should take their event actions here.
     */
    protected abstract void doActualEvent(UDPConnectionProcessor proc);

    /** 
     * Compares event times.
     */
    public int compareTo(UDPTimerEvent x) {
        long ret = x._eventTime - _eventTime;

        if ( ret > 0l )
            return 1;
        else if ( ret < 0l )
            return -1;
        else
            return 0;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + " [eventTime=" + _eventTime + "]";
    }
    
}
