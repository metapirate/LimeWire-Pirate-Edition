package org.limewire.collection;


import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.limewire.service.ErrorService;

/**
 * Gives a sorted {@link Set} of elements with a maximum size. Elements are sorted
 * upon insertion to the list, but only a fixed number of unique items are kept. 
 * Therefore, a newly inserted element can push out items depending on its 
 * insertion location. All inserted elements must implement the 
 * {@link Comparable} interface. Additionally, <code>FixedSizeSortedSet</code> 
 * does not accept duplicate elements. 
 * <p>
 * {@link #hashCode()} and {@link #equals(Object)} are used to determine if an
 * existing item needs to be replaced, but <code>compareTo</code> is 
 * used to determine which items are ejected. <code>compareTo</code> CAN be 
 * different from <code>hashCode</code> and <code>equals</code> (which is 
 * explicitly against the strong suggestion of <code>compareTo</code> and 
 * <code>equals</code>, but is necessary for <code>FixedSizeSortedSet</code> to
 * function).
 * <p>
 * Note: <code>FixedSizeSortedSet</code> can use a class that has a natural
 * ordering that is inconsistent with equals.
 * <p>
 * <code>FixedSizeSortedSet<code> is not thread-safe.
 <pre>
    //Consistent with equals
    public class MyPersonName implements Comparable {
        private int age ;
        private String name;
        
        MyPersonName(String name, int age){
            this.name = name;
            this.age = age;
        }
        
        public int age(){
            return age;
        }
        
        public String name(){
            return name;
        }
        
        public int compareTo(Object o){
            MyPersonName p = (MyPersonName) o;
            return this.name.compareTo(p.name());
        }
        
        public boolean equals(Object o){
            MyPersonName p = (MyPersonName) o;
            return name.equals(p.name());
        }
        
        public int hashCode(){
            return 37*name.hashCode();
        }

        public String toString(){
            return name + "'s age: " + age;
        }
    }
    
    //Note: This class has a natural ordering that is inconsistent with equals
    public class MyPersonAge implements Comparable {
        private int age ;
        private String name;
        
        MyPersonAge(String name, int age){
            this.name = name;
            this.age = age;
        }
        public int age(){
            return age;
        }
        public String name(){
            return name;
        }
        
        public int compareTo(Object o){
            MyPersonAge p = (MyPersonAge) o;
            return this.age - p.age() ;
        }
        
        public boolean equals(Object o){
            MyPersonAge p = (MyPersonAge) o;
            return name.equals(p.name());
        }
        
        public int hashCode(){
            return 37*name.hashCode();
        }
        
        public String toString(){
            return name + "'s age: " + age;
        }
    }
    
    void sampleCodeFixedSizeSortedSet(){
        
        //compareTo by Age
        System.out.println("When max. size met, evict people by oldest person.");
        FixedSizeSortedSet&lt;MyPersonAge&gt; age = new FixedSizeSortedSet&lt;MyPersonAge&gt;(5);
        age.add(new MyPersonAge("Abby", 23));
        
        if(!age.add(new MyPersonAge("Abby", 27)))
            System.out.println("Abby already contained in the collection; 23 is replaced with 27");
        age.add(new MyPersonAge("Chris", 20));
        age.add(new MyPersonAge("Bob", 28));
        age.add(new MyPersonAge("Dan", 25));
        age.add(new MyPersonAge("Eric", 24));
        //max reached
        age.add(new MyPersonAge("Fred", 22));
        
        for(MyPersonAge o : age)
            System.out.println(o.toString());   

        System.out.println("");

        //compareTo by Name
        System.out.println("When max. size met, evict people by alphabetical name.");
        FixedSizeSortedSet&lt;MyPersonName&gt; name = new FixedSizeSortedSet&lt;MyPersonName&gt;(5);
        name.add(new MyPersonName("Abby", 23));
        
        if(!name.add(new MyPersonName("Abby", 27)))
            System.out.println("Abby already contained in the collection; 23 is replaced with 27");
        name.add(new MyPersonName("Chris", 20));
        name.add(new MyPersonName("Bob", 28));
        name.add(new MyPersonName("Dan", 25));
        name.add(new MyPersonName("Eric", 24));
        //max reached
        name.add(new MyPersonName("Fred", 22));
        
        for(MyPersonName o : name)
            System.out.println(o.toString());       
    }
    Output:
        When max. size met, evict people by oldest person.
        Abby already contained in the collection; 23 is replaced with 27
        Chris's age: 20
        Fred's age: 22
        Eric's age: 24
        Dan's age: 25
        Abby's age: 27
        
        When max. size met, evict people by alphabetical name.
        Abby already contained in the collection; 23 is replaced with 27
        Abby's age: 27
        Bob's age: 28
        Chris's age: 20
        Dan's age: 25
        Fred's age: 22
 </pre>

 */ 
 /* 
  * A simple fixed size sorted set. Uses two structures internally, a SortedSet
 * and a Map, in order to efficiently look things up and keep them sorted.
 * This class is NOT SYNCHRONIZED. Synchronization should be done externally.
 */
public class FixedSizeSortedSet<E> implements Iterable<E> {

    /**
     * The underlying set that efficiently
     * keeps this FixedSizeSortedSet sorted.
     * INVARIANT: The elements of this set must be mirrored
     *            by values in _map.
     * INVARIANT: The size of this set must be equal to the size
     *            of _map.
     */
    private SortedSet<E> _sortedSet;
    
    /**
     * The map that allows us to treat this FixedSizeSortedSet
     * with equality of equals() instead of compareTo().
     * INVARIANT: The values of this map must point to an element
     *            in the _sortedSet.
     * INVARIANT: The size of this map must be equal to the size
     *            of _sortedSet.
     */
    private Map<E, E> _map;
    
    /**
     *  The maximum size of this, defaults to 50
     */
    private int _maxSize;
    
    ///////////////////////////////constructors////////////////////
    
    /**
     * Constructs a FixedSizeSortedSet with a maximum size of 50.
     */ 
    public FixedSizeSortedSet() {
        this(50);
    }
    
    /**
     * Constructs a FixedSizeSortedSet with a specified maximum size.
     */
    public FixedSizeSortedSet(int size) {
        _maxSize = size;
        _sortedSet = new TreeSet<E>();
        _map = new HashMap<E, E>();
    }

    /**
     * Constructs a FixedSizeSortedSet with the specified comparator
     * for the SortedSet and a maximum size of 50.
     */
    public FixedSizeSortedSet(Comparator<? super E> c) {
        this(c,50);
    }

    /**
     * Constructs a FixedSizeSortedSet with the specified comparator
     * and maximum size.
     */
    public FixedSizeSortedSet(Comparator<? super E> c, int maxSize) {
        _maxSize = maxSize;
        _sortedSet = new TreeSet<E>(c);
        _map = new HashMap<E, E>();
    }

    
    ////////////////////////Sorted Set methods///////////////////////
    @Override
    @SuppressWarnings("unchecked")
    public Object  clone() throws CloneNotSupportedException {
        FixedSizeSortedSet<E> ret = new FixedSizeSortedSet<E>(_maxSize);
        ret._sortedSet = (SortedSet<E>)((TreeSet<E>)_sortedSet).clone();
        ret._map = (Map<E, E>)((HashMap<E, E>)_map).clone();
        return ret;
    }

    /////////////////////Set Interface methods ///////////////////

    /**
     * Adds the object to the set.  If the object is already present,
     * (as specified by the Map's equals comparison), then it is ejected
     * and this newer version is used.
     */ 
    public boolean add(E o) {
        if(o==null) 
            return false;
        E val = _map.get(o);
        if(val != null) {//we have the object
            boolean removed = _sortedSet.remove(val);
            if(!removed)
                invariantsBroken(o, val);
            _sortedSet.add(o);
            _map.put(o,o);//replace the old entry
            return false;
        }
        else {//we need to add it
            if(_map.size() >= _maxSize) { //need to remove highest element
                E highest = _sortedSet.last();
                boolean removed = (_map.remove(highest)!=null);
                if(!removed)
                    invariantsBroken(highest, highest);
                removed = _sortedSet.remove(highest);
                if(!removed)
                    invariantsBroken(highest, highest);
            }
            _map.put(o,o);
            boolean added = _sortedSet.add(o);
            if(!added)
                invariantsBroken(o, o);
            return true;
        }
    }

    /**
     * Adds all the elements of the specified collection to this set.
     */
    public boolean addAll(Collection<? extends E> c) {
        boolean ret = false;
        for(E e : c) {
            ret |= add(e);
        }
        return ret;
    }
    
    /**
     * Retrieves the element that has an equals comparison with this
     * object and is in this FixedSizeSortedSet.
     */
    public E get(E o) {
        return _map.get(o);
    }

    /**
     * Returns the last element in the sorted set.
     */
    public E last() {
        return _sortedSet.last();
    }

    /**
     * Returns the first element in the sorted set.
     */
    public E first() {
        return _sortedSet.first();
    }

    /**
     * Removes the specified object from this sorted set.
     * Equality is determined by equals, not compareTo.
     */
    public boolean remove(E o) {
        E obj = _map.remove(o);
        boolean b1 = (obj!=null);
        boolean b2 = _sortedSet.remove(obj);
        if(b1 != b2)
            invariantsBroken(o, obj);
        return b1;
    }

    /**
     * Clears this FixedSizeSortedSet.
     */
    public void clear() { 
        _sortedSet.clear();
        _map.clear();
    }
    
    /**
     * Determines if this set contains the specified object.
     * Equality is determined by equals, not compareTo.
     */
    @SuppressWarnings({"SuspiciousMethodCalls"})
    public boolean contains(Object o) {
        return (_map.get(o) != null); //some equal key exists in the map
    }

    @Override
    public boolean equals(Object o) {
        if(o==null)
            return false;
        if(o==this)
            return true;
        if(!( o instanceof FixedSizeSortedSet))
            return false;
        FixedSizeSortedSet other = (FixedSizeSortedSet)o;
        return (_sortedSet.equals(other._sortedSet) && _map.equals(other._map));
    }

    @Override
    public int hashCode() {
        return _sortedSet.hashCode() + 37*_map.hashCode(); 
    }
    
    public boolean isEmpty() { 
        assert _sortedSet.isEmpty() == _map.isEmpty();
        return _sortedSet.isEmpty(); 
    }
    
    public Iterator<E> iterator() { 
        return new FSSSIterator();
    }
    
    public int size() { 
        if( _sortedSet.size() != _map.size() )
            invariantsBroken(null, null);
        return _sortedSet.size(); 
    }
    
    /**
     * Notification that the invariants have broken, triggers an error.
     */
    private void invariantsBroken(E key, E value) {
        String mapBefore = _map.toString();
        String setBefore = _sortedSet.toString();
        String mapSizeBefore = "" + _map.size();
        String setSizeBefore = "" + _sortedSet.size();
        stabilize();
        String mapAfter = _map.toString();
        String setAfter = _sortedSet.toString();
        String mapSizeAfter = "" + _map.size();
        String setSizeAfter = "" + _sortedSet.size();
        ErrorService.error(new IllegalStateException(
            "key: " + key + ", value: " + value +
            "\nbefore stabilization: " +
            "\nsize of map: " + mapSizeBefore + ", set: " + setSizeBefore +
            "\nmap: " + mapBefore +
            "\nset: " + setBefore +
            "\nafter stabilization: " + 
            "\nsize of map " + mapSizeAfter + ", set: " + setSizeAfter +
            "\nmap: " + mapAfter +
            "\nset: " + setAfter));
    }
    
    /**
     * Stabilizes the two data structures so that the invariants of this 
     * class are consistent.  This should never normally be done, but until
     * we can find what is causing the data to go out of synch, we need
     * to clean up the structures to prevent errors from going out of control.
     */
     @SuppressWarnings({"SuspiciousMethodCalls"})
     private void stabilize() {
        // First clean up the map for any entries that may not be in the set.
        for(Iterator iter = _map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            // If the set does not contain the value of this entry, remove it
            // from the map.
            if( !_sortedSet.contains(entry.getValue()) )
                iter.remove();
        }
        
        // Then clean up the set for any entries that may not be in the map.
        Collection values = _map.values();
        for(Iterator iter = _sortedSet.iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            // If the values of the map do not contain this entry, remove it
            // from the set.
            if( !values.contains(o) )
                iter.remove();
        }
    }
     
     private class FSSSIterator implements Iterator<E> {
     	
     	private final Iterator<E> _setIterator;
     	private E  _current;
     	
     	public FSSSIterator() {
     		_setIterator=_sortedSet.iterator();

     	}
     	
     	public boolean hasNext() {
     		return _setIterator.hasNext();
     	}
     	
     	public E next() {
     		_current = _setIterator.next();
     		return _current;
     	}
     	
     	public void remove() {
     		_setIterator.remove();
     		_map.remove(_current);
     		_current=null;
     	}
     	
     }

}
