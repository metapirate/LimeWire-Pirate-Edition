package org.limewire.io;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Exposes all information necessary for connecting to a host. */
// TODO push up into IpPort?
public interface Connectable extends IpPort, Address {
    
    public static final Comparator<Connectable> COMPARATOR = new Comparator<Connectable>() {

        @Override
        public int compare(Connectable o1, Connectable o2) {
            int ipPortdiff = IpPort.COMPARATOR.compare(o1, o2);
            if (ipPortdiff == 0) {
                if (o1.isTLSCapable() == o2.isTLSCapable()) {
                    return 0;
                }
                if (o1.isTLSCapable()) {
                    return 1;
                }
                return -1;
            }
            return ipPortdiff;
        }
    };
    
    /** Determines if the host is capable of receiving incoming TLS connections. */
    boolean isTLSCapable();
    
    /** An empty list, casted to an Connectable. */
    public static final List<Connectable> EMPTY_LIST = Collections.emptyList();
    /** An empty set, casted to an Connectable. */
    public static final Set<Connectable> EMPTY_SET = Collections.emptySet();
        
}
