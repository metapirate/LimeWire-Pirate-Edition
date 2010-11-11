package com.limegroup.gnutella.routing;

import org.limewire.collection.BitField;

/**
 * Something that stores the hash values contained in a routing table.
 */
interface QRTTableStorage extends BitField, Iterable<Integer> {

    /**
     * @return number of unused units, -1 if N/A
     */
    public int getUnusedUnits();
    
    /**
     * @return number of units with specified load, -1 if N/A
     */
    public int numUnitsWithLoad(int load);
    
    /**
     * @return number of 8-byte memory blocks in use or equivalent
     */
    public int getUnitsInUse();
    
    /**
     * @return % of units that are set.
     */
    public double getPercentFull();
    
    /**
     * sets the specified entry as present.
     */
    public void set(int hash);
    
    /**
     * clears the specified entry as absent
     */
    public void clear(int hash);
    
    /**
     * optional - compacts the memory representation of this.
     */
    public void compact();
    
    /**
     * @param newSize the new size we desire.
     * @return a new storage with the specified new size.  
     */
    public QRTTableStorage resize(int newSize);
    
    /**
     * @return a clone of this storage.
     */
    public QRTTableStorage clone() throws CloneNotSupportedException;
    
    /**
     * sets all entries present in other to be present in this as well.
     */
    public void or(QRTTableStorage other);
    
    /**
     * performs a xor with the other BitField.
     */
    public void xor(QRTTableStorage other);
    
    
}
