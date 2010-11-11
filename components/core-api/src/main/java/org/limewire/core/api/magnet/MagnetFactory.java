package org.limewire.core.api.magnet;

import java.net.URI;

public interface MagnetFactory {
    public boolean isMagnetLink(URI uri);
    public MagnetLink[] parseMagnetLink(URI uri);
}
