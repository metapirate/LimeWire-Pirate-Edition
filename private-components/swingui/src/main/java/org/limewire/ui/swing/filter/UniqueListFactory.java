package org.limewire.ui.swing.filter;

import java.util.Comparator;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.util.concurrent.Lock;

/**
 * A factory that converts a source list of values into a UniqueList of unique
 * values.  UniqueListFactory deals with a potential issue with UniqueList when 
 * it is preceded in the list chain by an ObservableList.  The issue is 
 * difficult to reproduce, but we occasionally get an NPE when UniqueList 
 * handles a listChanged event.  To prevent this, UniqueListFactory decouples 
 * list event processing between the source list and the unique list by 
 * creating a separate EventList chain, and installing its own ListEventListener
 * to forward list change events.  As a result, please note:
 * <ul>
 *   <li>The unique list uses a separate ListEventPublisher from the one used 
 *     by the source list.</li>
 *   <li>Applications should not attempt to directly modify the unique list
 *     since updates will not be forwarded back to the source list.</li>
 * </ul>
 * 
 * <p>UniqueListFactory is not thread-safe.  It is intended for use in 
 * single-threaded environments like the Swing UI thread. 
 */
public class UniqueListFactory<E> {

    /** Source list of values. */ 
    private final EventList<E> sourceList;
    /** List of values. */
    private final EventList<E> mirrorList;
    /** List of unique values. */
    private final UniqueList<E> uniqueList;
    /** Listener to handle changes to source list. */
    private final ListEventListener<E> sourceListener;
    /** Component name. */
    private String name;
    
    /**
     * Constructs a UniqueListFactory for the specified source list and 
     * comparator.
     */
    public UniqueListFactory(EventList<E> sourceList, Comparator<E> comparator) {
        // Create lists.
        this.sourceList = sourceList;
        mirrorList = new BasicEventList<E>();
        uniqueList = new UniqueList<E>(mirrorList, comparator);
        sourceListener = new SourceEventListener();
        
        // Copy values to mirror list, and add listener to source list.
        Lock lock = sourceList.getReadWriteLock().readLock();
        lock.lock();
        try {
            mirrorList.addAll(sourceList);
            sourceList.addListEventListener(sourceListener);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Disposes of the factory by removing the listener on the source list, and
     * disposing of the unique list.  The unique list can no longer be used
     * after this method is called.
     */
    public void dispose() {
        sourceList.removeListEventListener(sourceListener);
        uniqueList.dispose();
    }
    
    /**
     * Returns the name of the list factory.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of the list factory.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns a list of unique property values.
     */
    public UniqueList<E> getUniqueList() {
        return uniqueList;
    }

    /**
     * Listener that handles changes to the source list.
     */
    private class SourceEventListener implements ListEventListener<E> {

        /**
         * Handles list change event to update mirror list.
         */
        @Override
        public void listChanged(ListEvent<E> listChanges) {
            EventList<E> sourceList = listChanges.getSourceList();
            while (listChanges.next()) {
                int index = listChanges.getIndex();
                int type = listChanges.getType();
                try {
                    switch (type) {
                    case ListEvent.INSERT:
                        mirrorList.add(index, sourceList.get(index));
                        break;

                    case ListEvent.DELETE:
                        mirrorList.remove(index);
                        break;

                    case ListEvent.UPDATE:
                        mirrorList.set(index, sourceList.get(index));
                        break;
                    }
                    
                } catch (Throwable th) {
                    // Throw wrapper exception with message.
                    throw new RuntimeException(createExceptionMessage(type, index), th);
                }
            }
        }
        
        /**
         * Returns a detailed message using the specified ListEvent type and
         * index.
         */
        private String createExceptionMessage(int type, int index) {
            StringBuilder buf = new StringBuilder();
            
            buf.append(getName()).append(" list, unable to ");
            switch (type) {
            case ListEvent.INSERT:
                buf.append("insert");
                break;
            case ListEvent.DELETE:
                buf.append("delete");
                break;
            case ListEvent.UPDATE:
                buf.append("update");
                break;
            }
            buf.append(" at index ").append(index);
            
            return buf.toString();
        }
    }
}
