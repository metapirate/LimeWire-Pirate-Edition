package org.limewire.collection;

/**
 * Thrown when there isn't any more space left in the
 * underlying data structure to store the new element which was attempted
 * to be added. For example, if you create a {@link FixedsizeHashMap} with
 * a size of 10 and attempt to add the 11th item, 
 * {@link FixedsizeHashMap#put(Object, Object)} throws 
 * <code>NoMoreStorageException</code>.
 * 
 * @author Anurag Singla 
 */

public class NoMoreStorageException extends RuntimeException
{
    public NoMoreStorageException()
    {
    }
    public NoMoreStorageException(String msg)
    { 
        super(msg); 
    }
}
