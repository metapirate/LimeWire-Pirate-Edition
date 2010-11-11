package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.Serializable;

/**
 * Base class for deserializing old downloads, from before the download
 * serialization scheme moved to using mementos. (That is, LimeWire 4.17.0 and
 * before.)
 */
abstract class SerialRoot implements Serializable {
    private static final long serialVersionUID = -8297943148952763698l;

    protected SerialRoot() {
    }

}
