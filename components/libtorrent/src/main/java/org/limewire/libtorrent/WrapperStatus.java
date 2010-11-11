package org.limewire.libtorrent;

import com.sun.jna.Structure;

public class WrapperStatus extends Structure {
    public int type;
    public String message;
}
