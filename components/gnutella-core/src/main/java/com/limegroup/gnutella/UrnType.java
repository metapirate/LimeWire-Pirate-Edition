package com.limegroup.gnutella;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A deprecated class that is used solely so serialized data can be deserialized.
 */
@Deprecated
public class UrnType implements Serializable {

	private static final long serialVersionUID = -8211681448456483713L;

	/** The type that was read from disk. */
	private transient String _urnType;
    
    private UrnType() {}

	/** Stores the type that was read from disk. */
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		s.defaultReadObject();
		_urnType = (String)s.readObject();
	}
    
	/** Returns the type as read from disk. */
    public String getType() {
        return _urnType;
    }
    
    @Override
    public String toString() {
        return "urn type: " + _urnType;
    }
}
