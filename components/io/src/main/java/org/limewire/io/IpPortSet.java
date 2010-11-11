package org.limewire.io;



import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;


/**
 * A utility class to easily construct a {@link TreeSet} with
 * {@link IpPort#COMPARATOR}.
 */
public class IpPortSet extends TreeSet<IpPort> {

    /** Constructs an empty set. */
    public IpPortSet() {
        super(IpPort.COMPARATOR);
    }

    /** Constructs a set with the given initial IpPorts. */
    public IpPortSet(Collection<? extends IpPort> c) {
        this();
        addAll(c);
    }
    
    /** Constructs a set with the given initial IpPorts. */
    public IpPortSet(IpPort... ipps) {
        this(Arrays.asList(ipps));
    }
    
    /**
     * Overridden to ensure {@link IpPort#COMPARATOR} is used for element
     * comparisons.
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }
}
