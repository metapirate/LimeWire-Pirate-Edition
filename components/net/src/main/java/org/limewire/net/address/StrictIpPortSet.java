package org.limewire.net.address;


import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import org.limewire.io.IpPort;



/**
 * A stricter generic version of IpPortSet that lets you
 * have IpPortSets restricted to IpPort subclasses. 
 */
public class StrictIpPortSet<T extends IpPort> extends TreeSet<T> {

    /** Constructs an empty set. */
    public StrictIpPortSet() {
        super(IpPort.COMPARATOR);
    }

    /** Constructs a set with the given initial IpPorts. */
    public StrictIpPortSet(Collection<? extends T> c) {
        this();
        addAll(c);
    }
    
    /** Constructs a set with the given initial IpPorts. */
    public StrictIpPortSet(T... ipps) {
        this(Arrays.asList(ipps));
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }
}