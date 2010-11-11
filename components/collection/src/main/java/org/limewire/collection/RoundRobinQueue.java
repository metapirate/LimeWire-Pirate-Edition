package org.limewire.collection;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A round-robin queue. {@link #next()} returns an item on the queue and then
 * puts that item to the end of the queue.
 * 
 * <pre>
 * RoundRobinQueue&lt;Integer&gt; rrq = new RoundRobinQueue&lt;Integer&gt;();  
 *     rrq.enqueue(1); 
 *     rrq.enqueue(2);  
 *     
 *     for(Integer i = rrq.size() + 1; i &gt; 0; i--)
 *         System.out.println(rrq.next());
 *     
 *     Output:    
 *         1
 *         2
 *         1
 * </pre>
 * 
 */
public class RoundRobinQueue<T> {

    private LinkedList<T> _current;

    /**
     * Do not create the terminating elements.
     */
    public RoundRobinQueue() {
        _current = new LinkedList<T>();

    }

    /**
     * Enqueues the specified object in the round-robin queue.
     * 
     * @param value the object to add to the queue
     */
    public synchronized void enqueue(T value) {

        _current.addLast(value);

    }

    /**
     * @return the next object in the round robin queue
     */
    public synchronized T next() {
        T ret = _current.removeFirst();
        _current.addLast(ret);
        return ret;
    }

    /**
     * Removes the next occurrence of the specified object.
     * 
     * @param o the object to remove from the queue.
     */
    public synchronized void remove(T o) {
        _current.remove(o);
    }

    /**
     * Removes all occurrences of the given object in the list.
     * 
     * @param o the object to remove.
     */
    public synchronized void removeAllOccurences(T o) {
        Iterator iterator = _current.iterator();
        while (iterator.hasNext())
            if (iterator.next().equals(o))
                iterator.remove();

    }

    public synchronized int size() {
        return _current.size();
    }

    public synchronized void clear() {
        _current.clear();
    }

}
