package org.limewire.nio;

/** 
 * Defines an interface to mark some object as requiring the 
 * <code>SelectionKey</code> attachment. 
 */
public interface RequiresSelectionKeyAttachment {

    /** Sets the attachment that the SelectionKey has. */
    void setAttachment(Object o);
    
}
