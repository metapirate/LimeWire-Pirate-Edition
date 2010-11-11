package org.limewire.collection;

/**
 * Typed tuple that holds two objects of possibly different types.

 * @param <T1> the type of the first object
 * @param <T2> the type of the second object
 */
public class Tuple<T1, T2> {

    private final T1 obj1;
    
    private final T2 obj2;

    /**
     * Constructs a tuple for two objects.
     */
    public Tuple(T1 obj1, T2 obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    /**
     * Returns the first object.
     */
    public T1 getFirst() {
        return obj1;
    }
    
    /**
     * Returns the second object. 
     */
    public T2 getSecond() {
        return obj2;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("first: ");
        builder.append(obj1);
        builder.append(", second: ");
        builder.append(obj2);
        return builder.toString();
    }
    
}