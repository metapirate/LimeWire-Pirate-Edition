package com.limegroup.gnutella.library;

import com.limegroup.gnutella.FileDetails;

public interface LocalFileDetailsFactory {

    public FileDetails create(final FileDesc fd);

}