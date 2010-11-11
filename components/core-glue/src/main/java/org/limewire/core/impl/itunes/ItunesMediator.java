package org.limewire.core.impl.itunes;

import java.io.File;

public interface ItunesMediator {

    /**
     * If running on OSX, iTunes integration is enabled and the downloaded file
     * is a supported type, send it to iTunes.
     */
    public abstract void addSong(File file);

}